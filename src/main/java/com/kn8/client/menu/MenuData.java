package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.career.CareerView;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Dados e desenhos compartilhados pelas abas do menu (so leitura do que o cliente ja tem). */
final class MenuData {

    /** Nivel de ameaca mostrado no menu (pela categoria e porte do kaiju). */
    enum Threat {
        LOW(MenuStyle.GREEN), MEDIUM(MenuStyle.YELLOW), HIGH(MenuStyle.ORANGE), EXTREME(MenuStyle.RED);

        final int color;

        Threat(int color) {
            this.color = color;
        }

        Component label() {
            return Component.translatable("kn8.menu.threat." + name().toLowerCase(Locale.ROOT));
        }
    }

    private static final Map<EntityType<?>, LivingEntity> PREVIEWS = new HashMap<>();
    private static final float PREVIEW_FILL = 0.78F;

    private MenuData() {
    }

    static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    // --- patentes ----------------------------------------------------------------------------------------------

    /** Patentes em ordem (do JSON sincronizado). */
    static List<Map.Entry<ResourceLocation, RankDef>> ranks() {
        List<Map.Entry<ResourceLocation, RankDef>> list = new ArrayList<>(KN8Data.RANK.client().entrySet());
        list.sort(Comparator.comparingInt(entry -> entry.getValue().order()));
        return list;
    }

    /** Carreira do jogador local (enviada pelo servidor; vazia ate o primeiro envio). */
    static CareerView career() {
        LocalPlayer player = player();
        return player == null ? CareerView.EMPTY : player.getData(KN8Attachments.CAREER_VIEW);
    }

    /** Patente atual (da carreira; antes do primeiro envio, a mais baixa dos dados). */
    static Optional<Map.Entry<ResourceLocation, RankDef>> currentRank() {
        return rankEntry(career().rank());
    }

    /** Proxima patente; vazio no topo. */
    static Optional<Map.Entry<ResourceLocation, RankDef>> nextRank() {
        CareerView view = career();
        if (view.rank().equals(CareerView.NONE)) {
            List<Map.Entry<ResourceLocation, RankDef>> list = ranks();
            return list.size() < 2 ? Optional.empty() : Optional.of(list.get(1));
        }
        return view.isTopRank() ? Optional.empty() : rankEntry(view.nextRank());
    }

    private static Optional<Map.Entry<ResourceLocation, RankDef>> rankEntry(ResourceLocation id) {
        List<Map.Entry<ResourceLocation, RankDef>> list = ranks();
        for (Map.Entry<ResourceLocation, RankDef> entry : list) {
            if (entry.getKey().equals(id)) {
                return Optional.of(entry);
            }
        }
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Ordem da patente atual (0 = mais baixa). */
    static int rankOrder() {
        return currentRank().map(entry -> entry.getValue().order()).orElse(0);
    }

    /** Mesma regra do servidor ({@code CareerService.isUnlocked}): livre se nenhuma patente prende o id. */
    static boolean unlocked(ResourceLocation id) {
        int order = rankOrder();
        boolean lockedSomewhere = false;
        for (Map.Entry<ResourceLocation, RankDef> entry : ranks()) {
            if (entry.getValue().unlocks().contains(id)) {
                if (entry.getValue().order() <= order) {
                    return true;
                }
                lockedSomewhere = true;
            }
        }
        return !lockedSomewhere;
    }

    /** Patente mais baixa que libera o id (para o aviso de item travado). */
    static ResourceLocation unlockingRank(ResourceLocation id) {
        for (Map.Entry<ResourceLocation, RankDef> entry : ranks()) {
            if (entry.getValue().unlocks().contains(id)) {
                return entry.getKey();
            }
        }
        return CareerView.NONE;
    }

    static Component rankName(ResourceLocation id) {
        return Component.translatable("kn8.rank." + id.getPath());
    }

    // --- kaiju -------------------------------------------------------------------------------------------------

    static Optional<KaijuDef> kaijuDef(ResourceLocation species) {
        return KN8Data.KAIJU.get(species, true);
    }

    static Component speciesName(ResourceLocation species) {
        return Component.translatable("entity." + species.getNamespace() + "." + species.getPath());
    }

    static Threat threat(KaijuDef def) {
        return switch (def.kaijuClass()) {
            case YOJU -> def.dimensions().height() >= 5.0F ? Threat.MEDIUM : Threat.LOW;
            case HONJU -> Threat.HIGH;
            case DAIKAIJU, NUMBERED -> Threat.EXTREME;
        };
    }

    static Component kaijuClass(KaijuDef def) {
        return Component.translatable("kn8.kaiju_class." + def.kaijuClass().getSerializedName());
    }

    /** Kaiju abatidos (estatistica vanilla, pedida ao servidor quando o menu abre). */
    static int kills(EntityType<?> type) {
        LocalPlayer player = player();
        return player == null ? 0 : player.getStats().getValue(Stats.ENTITY_KILLED.get(type));
    }

    static int totalKaijuKills() {
        return KN8Entities.KAIJU.stream().mapToInt(type -> kills(type.get())).sum();
    }

    /** Especie registrada no bestiario: ja abatida (no criativo, todas, para conferir o conteudo). */
    static boolean registered(EntityType<?> type) {
        LocalPlayer player = player();
        return player != null && (player.isCreative() || kills(type) > 0);
    }

    static ResourceLocation id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    // --- entidades por perto -----------------------------------------------------------------------------------

    static List<KaijuEntity> nearbyKaiju(double radius) {
        return nearby(KaijuEntity.class, radius);
    }

    static List<SoldierEntity> nearbySoldiers(double radius) {
        return nearby(SoldierEntity.class, radius);
    }

    private static <T extends Entity> List<T> nearby(Class<T> type, double radius) {
        LocalPlayer player = player();
        List<T> list = new ArrayList<>();
        if (player == null || Minecraft.getInstance().level == null) {
            return list;
        }
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (type.isInstance(entity) && entity.isAlive() && entity.distanceTo(player) <= radius) {
                list.add(type.cast(entity));
            }
        }
        list.sort(Comparator.comparingDouble(entity -> entity.distanceTo(player)));
        return list;
    }

    /** Direcao do alvo vista pelo jogador (N, NE, L...), pelo angulo no plano. */
    static Component direction(Entity target) {
        LocalPlayer player = player();
        double angle = Mth.wrapDegrees(Math.toDegrees(Math.atan2(target.getX() - player.getX(),
                player.getZ() - target.getZ())));
        String[] names = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        int index = Math.floorMod(Math.round((float) (angle / 45.0)), 8);
        return Component.translatable("kn8.menu.dir." + names[index]);
    }

    // --- modelos 3D --------------------------------------------------------------------------------------------

    /** Entidade so para desenho (nao entra no mundo), uma por tipo. */
    static LivingEntity preview(EntityType<?> type) {
        LivingEntity cached = PREVIEWS.get(type);
        if (cached != null && cached.level() != Minecraft.getInstance().level) {
            // Mundo trocado (relog, outra dimensao): os modelos antigos apontam para o mundo velho.
            PREVIEWS.clear();
        }
        return PREVIEWS.computeIfAbsent(type, key -> {
            Entity entity = key.create(Minecraft.getInstance().level);
            return entity instanceof LivingEntity living ? living : null;
        });
    }

    /** Avanca o relogio dos modelos (as animacoes GeckoLib usam o tickCount da entidade). */
    static void tickPreviews() {
        PREVIEWS.values().forEach(entity -> {
            if (entity != null) {
                entity.tickCount++;
            }
        });
    }

    /**
     * Desenha a entidade na caixa, enquadrada pela maior medida da hitbox. {@code silhouette} = so a forma escura
     * (especie ainda nao registrada).
     */
    static void renderEntity(GuiGraphics g, LivingEntity entity, int x1, int y1, int x2, int y2, float yaw,
            float pitch, boolean silhouette) {
        if (entity == null) {
            return;
        }
        float size = Math.max(entity.getBbHeight(), entity.getBbWidth() * 0.9F);
        int scale = Math.max(1, Math.round(Math.min(y2 - y1, x2 - x1) * PREVIEW_FILL / size));
        if (silhouette) {
            RenderSystem.setShaderColor(0.06F, 0.09F, 0.14F, 1.0F);
        }
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x1, y1, x2, y2, scale, 0.0625F, yaw, pitch, entity);
        MenuStyle.resetColor();
    }
}
