package com.kn8.common.career;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.network.NetworkSync;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Carreira no servidor (0.2, Etapa 2): patente, merito, promocao e o que a patente da (vida maxima; esquadrao e
 * desbloqueios sao lidos por quem precisa). Tambem centraliza as recompensas de treino/merito por eventos de jogo
 * (abate, dano em kaiju, desmonte), para as regras ficarem num lugar so.
 *
 * <p>Decisao do Miguel: a patente NAO limita o Release; o teto e {@code career.releaseMax} para todos.</p>
 */
public final class CareerService {

    private static final ResourceLocation HEALTH_MODIFIER = KN8Constants.id("rank_health");
    private static final float VANILLA_MAX_HEALTH = 20.0F;
    private static final long DUMMY_WINDOW_TICKS = 20L * 60L;

    private CareerService() {
    }

    public static CareerData data(ServerPlayer player) {
        return player.getData(KN8Attachments.CAREER);
    }

    // --- patentes ------------------------------------------------------------------------------------------------

    /** Patentes dos dados em ordem (0 = mais baixa). */
    public static List<Map.Entry<ResourceLocation, RankDef>> ranks() {
        List<Map.Entry<ResourceLocation, RankDef>> list = new ArrayList<>(KN8Data.RANK.server().entrySet());
        list.sort(Comparator.comparingInt(entry -> entry.getValue().order()));
        return list;
    }

    /** Patente atual (a gravada, se ainda existir nos dados; senao a mais baixa). */
    public static Optional<Map.Entry<ResourceLocation, RankDef>> rank(ServerPlayer player) {
        List<Map.Entry<ResourceLocation, RankDef>> all = ranks();
        Optional<ResourceLocation> saved = data(player).rank();
        if (saved.isPresent()) {
            for (Map.Entry<ResourceLocation, RankDef> entry : all) {
                if (entry.getKey().equals(saved.get())) {
                    return Optional.of(entry);
                }
            }
        }
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    public static Optional<Map.Entry<ResourceLocation, RankDef>> nextRank(ServerPlayer player) {
        List<Map.Entry<ResourceLocation, RankDef>> all = ranks();
        Optional<Map.Entry<ResourceLocation, RankDef>> current = rank(player);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        int index = all.indexOf(current.get());
        return index + 1 < all.size() ? Optional.of(all.get(index + 1)) : Optional.empty();
    }

    /** O jogador ja tem esta patente ou uma acima (receitas, armas e missoes presas a patente). */
    public static boolean hasRank(ServerPlayer player, ResourceLocation required) {
        Optional<RankDef> needed = KN8Data.RANK.get(required, false);
        return needed.isEmpty() || rank(player).map(entry -> entry.getValue().order() >= needed.get().order())
                .orElse(false);
    }

    /** Algo foi liberado por alguma patente ate a atual ({@code unlocks}), ou nenhuma patente o prende. */
    public static boolean isUnlocked(ServerPlayer player, ResourceLocation id) {
        int order = rank(player).map(entry -> entry.getValue().order()).orElse(0);
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

    public static void setRank(ServerPlayer player, ResourceLocation id) {
        data(player).setRank(id);
        applyRankEffects(player);
        changed(player);
    }

    /** Merito (soma ou tira) e promocao automatica quando a proxima patente fica liberada. */
    public static void addMerit(ServerPlayer player, int amount) {
        CareerData data = data(player);
        data.setMerit(data.merit() + amount);
        tryPromote(player);
        changed(player);
    }

    /**
     * Promove enquanto der: merito suficiente e, se a proxima patente tiver missao de avaliacao, ela concluida
     * (ex.: Candidato -> Oficial pelo Exame de Admissao).
     */
    public static void tryPromote(ServerPlayer player) {
        CareerData data = data(player);
        for (int guard = 0; guard < 16; guard++) {
            Optional<Map.Entry<ResourceLocation, RankDef>> next = nextRank(player);
            if (next.isEmpty() || !canPromote(data, next.get().getValue())) {
                return;
            }
            data.setRank(next.get().getKey());
            applyRankEffects(player);
            announcePromotion(player, next.get().getKey());
        }
    }

    private static boolean canPromote(CareerData data, RankDef next) {
        return data.merit() >= next.meritRequired()
                && next.promotionMission().map(data.completed()::contains).orElse(true);
    }

    /** Promocao direta (missao com {@code promote_to}): mesma patente, efeitos e aviso da promocao por merito. */
    public static void promote(ServerPlayer player, ResourceLocation rank) {
        setRank(player, rank);
        announcePromotion(player, rank);
    }

    private static void announcePromotion(ServerPlayer player, ResourceLocation rank) {
        Component name = Component.translatable("kn8.rank." + rank.getPath()).withStyle(ChatFormatting.AQUA);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("kn8.career.promoted")
                .withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(name));
        player.sendSystemMessage(Component.translatable("kn8.career.promoted_chat", name));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                1.0F, 0.8F);
    }

    /** Vida maxima da patente (o resto do que ela da e lido sob demanda). Login, respawn e mudanca de patente. */
    public static void applyRankEffects(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(HEALTH_MODIFIER);
        float bonus = rank(player).map(entry -> entry.getValue().maxHealth()).orElse(VANILLA_MAX_HEALTH)
                - VANILLA_MAX_HEALTH;
        if (bonus != 0) {
            health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER, bonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // --- recompensas por eventos ----------------------------------------------------------------------------------

    public static void onKaijuKilled(ServerPlayer player, ResourceLocation species, KaijuClass kaijuClass) {
        data(player).addKaijuKill();
        boolean yoju = kaijuClass == KaijuClass.YOJU;
        PowerService.addTrainingXp(player, yoju ? ServerConfig.XP_KILL_YOJU.get() : ServerConfig.XP_KILL_HONJU.get());
        addMerit(player, yoju ? ServerConfig.MERIT_KILL_YOJU.get() : ServerConfig.MERIT_KILL_HONJU.get());
        MissionService.progress(player, MissionDef.ObjectiveType.KILL_KAIJU, species);
    }

    public static void onKaijuDamaged(ServerPlayer player, float damage) {
        int xp = (int) Math.round(damage * ServerConfig.XP_PER_KAIJU_DAMAGE.get());
        if (xp > 0) {
            PowerService.addTrainingXp(player, xp);
        }
    }

    public static void onDismantleStep(ServerPlayer player, ResourceLocation species, boolean finished) {
        PowerService.addTrainingXp(player, ServerConfig.XP_DISMANTLE_STEP.get());
        if (finished) {
            data(player).addDismantled();
            addMerit(player, ServerConfig.MERIT_DISMANTLE.get());
            MissionService.progress(player, MissionDef.ObjectiveType.DISMANTLE, species);
        }
    }

    /**
     * Golpe no boneco de treino: XP com limite por minuto ({@code career.dummyXpPerMinute}). Devolve o XP dado
     * (0 = limite do minuto atingido).
     */
    public static int onDummyHit(ServerPlayer player) {
        CareerData data = data(player);
        long now = player.level().getGameTime();
        long start = data.dummyWindowStart();
        int used = data.dummyXpInWindow();
        if (start == Long.MIN_VALUE || now - start >= DUMMY_WINDOW_TICKS) {
            start = now;
            used = 0;
        }
        int xp = Math.min(ServerConfig.DUMMY_XP_PER_HIT.get(), ServerConfig.DUMMY_XP_PER_MINUTE.get() - used);
        data.setDummyWindow(start, used + Math.max(0, xp));
        if (xp > 0) {
            PowerService.addTrainingXp(player, xp);
        }
        return Math.max(0, xp);
    }

    // --- visao do dono ------------------------------------------------------------------------------------------

    public static CareerView view(ServerPlayer player) {
        CareerData data = data(player);
        ResourceLocation rank = rank(player).map(Map.Entry::getKey).orElse(CareerView.NONE);
        Optional<Map.Entry<ResourceLocation, RankDef>> next = nextRank(player);
        ResourceLocation nextId = next.map(Map.Entry::getKey).orElse(rank);
        int nextMerit = next.map(entry -> entry.getValue().meritRequired()).orElse(0);
        String nextMission = next.flatMap(entry -> entry.getValue().promotionMission())
                .filter(id -> !data.completed().contains(id)).map(ResourceLocation::toString).orElse("");
        return new CareerView(rank, nextId, data.merit(), nextMerit, nextMission,
                new CareerView.Stats(data.kaijuKills(), data.dismantled(), data.missionsDone(),
                        data.invasionsDefended()),
                MissionService.activeView(player), List.copyOf(data.completed()));
    }

    /** Marca o canal privado da carreira para envio no fim do tick. */
    public static void changed(ServerPlayer player) {
        NetworkSync.markDirty(player, CareerSyncS2C.CHANNEL);
    }
}
