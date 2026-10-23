package com.kn8.common.support;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.server.KN8Server;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Apoio militar (PM-10B, Plano Mestre secao 15; Miguel 2026-10-23: "so em missao e invasao, 3 tipos: pequeno, medio e
 * grande"). Chamado pelo Radio de Campo, decidido inteiro no servidor:
 * <ul>
 *   <li><b>pequeno</b> = reconhecimento: kaiju perto do ponto marcado brilham (vistos atraves das paredes) por um
 *   tempo e a lista vai ao chat; sem kaiju, nada e gasto;</li>
 *   <li><b>medio</b> = suprimentos: caixa no ponto depois de um atraso (municao e resfriador do config) e
 *   Regeneracao II para jogadores, soldados e civis em volta;</li>
 *   <li><b>grande</b> = bombardeio limitado: area marcada no chao durante o atraso, depois dano so em kaiju (fracao da
 *   vida maxima com teto; nunca resolve a luta sozinho), sem quebrar blocos nem ferir aliados.</li>
 * </ul>
 * Requisitos: operacao ativa (missao de campo ou invasao com o jogador na area), patente, usos por operacao, espera e
 * ponto valido (bloco olhado a ate {@code callRange}). Chamada recusada nao gasta nada. Estado do jogador nos dados
 * persistentes que sobrevivem a morte ({@link Player#PERSISTED_NBT_TAG}).
 */
public final class SupportService {

    private static final String TAG = "kn8_support";
    private static final String OPERATION = "op";
    private static final String SELECTED = "sel";
    private static final String LAST_CALL = "last";
    private static final String STRIKE_COLOR = "#ff4030";

    private SupportService() {
    }

    /** Os tres tamanhos; a ordem e a salva ({@code sel}) e a do radio. */
    public enum Tier {
        RECON, SUPPLY, STRIKE;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        int uses() {
            return switch (this) {
                case RECON -> ServerConfig.SUPPORT_RECON_USES.get();
                case SUPPLY -> ServerConfig.SUPPORT_SUPPLY_USES.get();
                case STRIKE -> ServerConfig.SUPPORT_STRIKE_USES.get();
            };
        }

        int cooldown() {
            return switch (this) {
                case RECON -> ServerConfig.SUPPORT_RECON_COOLDOWN.get();
                case SUPPLY -> ServerConfig.SUPPORT_SUPPLY_COOLDOWN.get();
                case STRIKE -> ServerConfig.SUPPORT_STRIKE_COOLDOWN.get();
            };
        }

        String rank() {
            return switch (this) {
                case RECON -> ServerConfig.SUPPORT_RECON_RANK.get();
                case SUPPLY -> ServerConfig.SUPPORT_SUPPLY_RANK.get();
                case STRIKE -> ServerConfig.SUPPORT_STRIKE_RANK.get();
            };
        }
    }

    public enum Result {
        OK, NO_OPERATION, RANK_TOO_LOW, NO_USES, COOLDOWN, NO_TARGET, NO_CONTACT, TOO_FAST
    }

    // --- operacao --------------------------------------------------------------------------------------------------

    /**
     * Chave da operacao em que o jogador esta: a missao de campo ativa mais antiga ("m:id@aceite") ou a invasao da
     * dimensao com o jogador dentro da area ("i:id@inicio"). Vazio fora de operacao (provas nao contam).
     */
    public static Optional<String> operation(ServerPlayer player) {
        CareerData data = CareerService.data(player);
        for (Map.Entry<ResourceLocation, CareerData.MissionProgress> entry : data.activeSorted()) {
            Optional<MissionDef> def = KN8Data.MISSION.get(entry.getKey(), false);
            if (def.isPresent() && !MissionService.isExam(def.get())) {
                return Optional.of("m:" + entry.getKey() + "@" + entry.getValue().acceptedTick());
            }
        }
        Optional<Invasion> invasion = InvasionService.active(player.serverLevel());
        if (invasion.isPresent() && InvasionService.inArea(player, invasion.get())) {
            return Optional.of("i:" + invasion.get().id() + "@" + invasion.get().startTick());
        }
        return Optional.empty();
    }

    private static CompoundTag state(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = persisted.getCompound(TAG);
        persisted.put(TAG, tag);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return tag;
    }

    /** Usos ja feitos do tamanho na operacao atual (operacao nova zera os usos, nao as esperas). */
    public static int used(ServerPlayer player, Tier tier, String operation) {
        CompoundTag tag = state(player);
        return operation.equals(tag.getString(OPERATION)) ? tag.getInt("u" + tier.ordinal()) : 0;
    }

    public static long cooldownLeft(ServerPlayer player, Tier tier) {
        return Math.max(0, state(player).getLong("c" + tier.ordinal()) - player.level().getGameTime());
    }

    public static Tier selected(ServerPlayer player) {
        int index = state(player).getInt(SELECTED);
        return Tier.values()[Math.floorMod(index, Tier.values().length)];
    }

    /** Troca o tamanho selecionado no radio e mostra o que ele tem disponivel. */
    public static Tier cycle(ServerPlayer player) {
        Tier next = Tier.values()[(selected(player).ordinal() + 1) % Tier.values().length];
        state(player).putInt(SELECTED, next.ordinal());
        showStatus(player, next);
        return next;
    }

    public static void showStatus(ServerPlayer player, Tier tier) {
        Optional<String> op = operation(player);
        int left = op.map(key -> tier.uses() - used(player, tier, key)).orElse(tier.uses());
        long wait = cooldownLeft(player, tier);
        Component text = Component.translatable("kn8.support.selected", Component.translatable("kn8.support.tier."
                + tier.key()), Math.max(0, left), tier.uses());
        if (wait > 0) {
            text = text.copy().append(Component.translatable("kn8.support.wait", (wait + 19) / 20));
        }
        player.displayClientMessage(text.copy().withStyle(ChatFormatting.AQUA), true);
    }

    // --- chamada ---------------------------------------------------------------------------------------------------

    /** Confere tudo sem gastar nada (o ponto e o contato do reconhecimento sao conferidos no {@link #call}). */
    public static Result check(ServerPlayer player, Tier tier) {
        Optional<String> op = operation(player);
        if (op.isEmpty()) {
            return Result.NO_OPERATION;
        }
        ResourceLocation rank = ResourceLocation.tryParse(tier.rank());
        if (rank != null && !CareerService.hasRank(player, rank) && !player.isCreative()) {
            return Result.RANK_TOO_LOW;
        }
        if (used(player, tier, op.get()) >= tier.uses()) {
            return Result.NO_USES;
        }
        if (cooldownLeft(player, tier) > 0) {
            return Result.COOLDOWN;
        }
        return Result.OK;
    }

    /** Chamada pelo radio: o ponto e o bloco olhado. */
    public static Result call(ServerPlayer player, Tier tier) {
        long now = player.level().getGameTime();
        CompoundTag tag = state(player);
        if (now - tag.getLong(LAST_CALL) < ServerConfig.SUPPORT_CALL_GAP_TICKS.get() && tag.contains(LAST_CALL)) {
            return refuse(player, Result.TOO_FAST);
        }
        tag.putLong(LAST_CALL, now);
        Optional<BlockPos> point = aimed(player);
        if (point.isEmpty()) {
            return refuse(player, Result.NO_TARGET);
        }
        return callAt(player, tier, point.get());
    }

    /** Chamada num ponto (tambem usada pelos GameTests). */
    public static Result callAt(ServerPlayer player, Tier tier, BlockPos point) {
        Result result = check(player, tier);
        if (result != Result.OK) {
            return refuse(player, result);
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = Vec3.atBottomCenterOf(point.above());
        if (tier == Tier.RECON && !recon(level, player, at)) {
            return refuse(player, Result.NO_CONTACT);
        }
        String op = operation(player).orElseThrow();
        CompoundTag tag = state(player);
        if (!op.equals(tag.getString(OPERATION))) {
            tag.putString(OPERATION, op);
            for (Tier each : Tier.values()) {
                tag.remove("u" + each.ordinal());
            }
        }
        tag.putInt("u" + tier.ordinal(), tag.getInt("u" + tier.ordinal()) + 1);
        tag.putLong("c" + tier.ordinal(), level.getGameTime() + tier.cooldown());
        switch (tier) {
            case SUPPLY -> supply(level, player, at);
            case STRIKE -> strike(level, player, at);
            default -> {
            }
        }
        player.sendSystemMessage(Component.translatable("kn8.support.called." + tier.key(),
                point.getX(), point.getY(), point.getZ()).withStyle(ChatFormatting.GOLD));
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.8F,
                1.6F);
        return Result.OK;
    }

    private static Result refuse(ServerPlayer player, Result result) {
        player.displayClientMessage(Component.translatable("kn8.support.refused." + result.name()
                .toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.RED), true);
        return result;
    }

    private static Optional<BlockPos> aimed(ServerPlayer player) {
        HitResult hit = player.pick(ServerConfig.SUPPORT_CALL_RANGE.get(), 0.0F, false);
        return hit.getType() == HitResult.Type.BLOCK ? Optional.of(((BlockHitResult) hit).getBlockPos())
                : Optional.empty();
    }

    // --- efeitos ---------------------------------------------------------------------------------------------------

    /** Reconhecimento: marca os kaiju perto do ponto; falso sem contato (nada gasto). */
    static boolean recon(ServerLevel level, ServerPlayer player, Vec3 at) {
        double radius = ServerConfig.SUPPORT_RECON_RADIUS.get();
        List<KaijuEntity> kaiju = level.getEntitiesOfClass(KaijuEntity.class, new AABB(at, at).inflate(radius),
                one -> one.isAlive() && one.position().distanceTo(at) <= radius);
        if (kaiju.isEmpty()) {
            return false;
        }
        int ticks = ServerConfig.SUPPORT_RECON_MARK_TICKS.get();
        for (KaijuEntity one : kaiju) {
            one.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
            player.sendSystemMessage(Component.translatable("kn8.support.contact", one.getType().getDescription(),
                    Math.round(one.distanceTo(player)), Math.round(100 * one.getHealth() / one.getMaxHealth()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        VfxService.keyed(level, VfxService.TELEGRAPH, at.add(0, 0.05, 0), Vec3.ZERO, 2.0F, "disc#40c0ff@40", 0.0F);
        return true;
    }

    /** Suprimentos: caixa no ponto depois do atraso, com cura em volta. */
    static void supply(ServerLevel level, ServerPlayer player, Vec3 at) {
        int delay = ServerConfig.SUPPORT_SUPPLY_DELAY.get();
        VfxService.keyed(level, VfxService.TELEGRAPH, at.add(0, 0.05, 0), Vec3.ZERO, 2.0F, "disc#60ff90@" + (delay
                + 10), 0.0F);
        KN8Server.get(level.getServer()).schedule(delay, () -> dropSupply(level, at));
    }

    /** Pouso da caixa (publico para os GameTests). */
    public static void dropSupply(ServerLevel level, Vec3 at) {
        for (String entry : ServerConfig.SUPPORT_SUPPLY_ITEMS.get()) {
            String[] parts = entry.split("\\*");
            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            int count = parts.length > 1 ? parseCount(parts[1]) : 1;
            if (item == Items.AIR || count <= 0) {
                continue;
            }
            while (count > 0) {
                int size = Math.min(count, item.getDefaultMaxStackSize());
                ItemEntity drop = new ItemEntity(level, at.x, at.y + 0.5, at.z, new ItemStack(item, size));
                drop.setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(drop);
                count -= size;
            }
        }
        double radius = ServerConfig.SUPPORT_SUPPLY_RADIUS.get();
        int heal = ServerConfig.SUPPORT_SUPPLY_HEAL_TICKS.get();
        if (heal > 0) {
            for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                    e -> e.isAlive() && e.position().distanceTo(at) <= radius && (e instanceof SoldierEntity
                            || e instanceof CivilianEntity || e instanceof Player player && !player.isSpectator()))) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, heal, 1));
            }
        }
        VfxService.keyed(level, VfxService.SMOKE, at.add(0, 0.6, 0), Vec3.ZERO, 1.2F, "#9fe8b0", 0.0F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    private static int parseCount(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Bombardeio: area marcada durante o atraso, depois o dano. */
    static void strike(ServerLevel level, ServerPlayer player, Vec3 at) {
        int delay = ServerConfig.SUPPORT_STRIKE_DELAY.get();
        float radius = ServerConfig.SUPPORT_STRIKE_RADIUS.get().floatValue();
        VfxService.keyed(level, VfxService.TELEGRAPH, at.add(0, 0.05, 0), Vec3.ZERO, radius, "disc" + STRIKE_COLOR
                + "@" + delay, 0.0F);
        java.util.UUID caller = player.getUUID();
        KN8Server.get(level.getServer()).schedule(delay, () -> {
            int hit = detonate(level, at);
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(caller);
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("kn8.support.strike_result", hit)
                        .withStyle(ChatFormatting.GOLD));
            }
        });
    }

    /** Impacto do bombardeio: so kaiju, uma vez cada, fracao da vida com teto. Devolve quantos pegou. */
    public static int detonate(ServerLevel level, Vec3 at) {
        double radius = ServerConfig.SUPPORT_STRIKE_RADIUS.get();
        int hit = 0;
        for (KaijuEntity kaiju : level.getEntitiesOfClass(KaijuEntity.class, new AABB(at, at).inflate(radius + 8),
                one -> one.isAlive() && one.getBoundingBox().distanceToSqr(at) <= radius * radius)) {
            float damage = (float) Math.min(ServerConfig.SUPPORT_STRIKE_MAX_DAMAGE.get(),
                    kaiju.getMaxHealth() * ServerConfig.SUPPORT_STRIKE_FRACTION.get());
            kaiju.invulnerableTime = 0;
            if (damage > 0 && kaiju.hurt(level.damageSources().explosion(null, null), damage)) {
                hit++;
            }
        }
        VfxService.play(level, VfxService.EXPLOSION, at.add(0, 1.0, 0), Vec3.ZERO, (float) radius, 0.6F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0F,
                0.8F);
        return hit;
    }
}
