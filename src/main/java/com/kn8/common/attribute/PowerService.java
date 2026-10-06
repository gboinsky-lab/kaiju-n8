// src/main/java/com/kn8/common/attribute/PowerService.java
package com.kn8.common.attribute;

import java.util.Comparator;

import com.kn8.KN8Constants;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.network.NetworkSync;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.power.HeatStage;
import com.kn8.core.power.PowerMath;
import com.kn8.core.power.PowerParams;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Regras de poder do jogador no servidor (Fase 4: ReleaseService + StaminaService + HeatService + AttributeApplier,
 * reunidos aqui porque nao guardam estado proprio: todo estado esta no attachment {@code kn8:power}).
 *
 * <p>Ate o M14 nao existe item de traje nem carreira: o traje conta como vestido e o teto de liberacao e o da
 * patente de menor {@code order} nos dados (Candidato = 10), a menos que um comando force outro teto.</p>
 */
public final class PowerService {

    public static final ResourceKey<DamageType> SUIT_OVERHEAT =
            ResourceKey.create(Registries.DAMAGE_TYPE, KN8Constants.id("suit_overheat"));

    private static final ResourceLocation DAMAGE_MODIFIER = KN8Constants.id("release_damage");
    private static final ResourceLocation SPEED_MODIFIER = KN8Constants.id("release_speed");
    private static final ResourceLocation KNOCKBACK_MODIFIER = KN8Constants.id("release_knockback");
    private static final int TICKS_PER_SECOND = 20;
    private static final int CRITICAL_SLOWNESS_TICKS = 40;
    private static final int SLOWNESS_I = 0;
    private static final int SLOWNESS_II = 1;

    private PowerService() {
    }

    public static PowerData data(ServerPlayer player) {
        return player.getData(KN8Attachments.POWER);
    }

    public static PowerParams params() {
        return ServerConfig.powerParams();
    }

    /** Teto atual: forcado por comando ou, ate o M14, o da patente mais baixa dos dados. */
    public static int cap(ServerPlayer player) {
        int override = data(player).capOverride();
        if (override != PowerData.NO_CAP_OVERRIDE) {
            return override;
        }
        return KN8Data.RANK.server().values().stream()
                .min(Comparator.comparingInt(RankDef::order))
                .map(RankDef::releaseCap)
                .orElse(0);
    }

    public static boolean inPanic(ServerPlayer player) {
        long until = data(player).panicUntilTick();
        return !PowerData.never(until) && now(player) < until;
    }

    /** % efetiva usada por todas as regras (durante o panico do traje, cai para o valor do config). */
    public static int effectiveRelease(ServerPlayer player) {
        if (inPanic(player)) {
            return ServerConfig.PANIC_RELEASE.get();
        }
        PowerData data = data(player);
        return PowerMath.effectiveRelease(data.trainedRelease(), cap(player), data.surge(), params());
    }

    public static HeatStage heatStage(ServerPlayer player) {
        return inPanic(player) ? HeatStage.PANIC : PowerMath.heatStage(data(player).heat(), params());
    }

    // --- alteracoes (sempre marcam o canal privado para envio) -----------------------------------------------

    public static void setTrainedRelease(ServerPlayer player, int value) {
        data(player).setTrainedRelease(value);
        changed(player);
    }

    /** Soma XP de treino e converte em pontos enquanto der (ver {@link #convertTrainingXp}). */
    public static void addTrainingXp(ServerPlayer player, int xp) {
        PowerData data = data(player);
        data.setReleaseXp(data.releaseXp() + Math.max(0, xp));
        convertTrainingXp(player);
        changed(player);
    }

    public static void setCapOverride(ServerPlayer player, int cap) {
        data(player).setCapOverride(cap);
        onCapChanged(player);
    }

    /**
     * Chamar sempre que o teto mudar (comando hoje; promocao de patente no M14): converte na hora o XP guardado.
     */
    public static void onCapChanged(ServerPlayer player) {
        convertTrainingXp(player);
        changed(player);
    }

    /** Converte o XP guardado em pontos ate o teto atual (regra em {@link PowerMath#convertTraining}). */
    private static void convertTrainingXp(ServerPlayer player) {
        PowerData data = data(player);
        PowerMath.Training result = PowerMath.convertTraining(data.trainedRelease(), data.releaseXp(), cap(player),
                params());
        data.setTrainedRelease(result.trained());
        data.setReleaseXp(result.xp());
    }

    /** Surto (GDD secao 6): ate {@code power.surgeMax} pontos acima do treinado, gerando calor. */
    public static void setSurge(ServerPlayer player, int points) {
        data(player).setSurge(Math.min(points, params().surgeMax()));
        changed(player);
    }

    public static void setHeat(ServerPlayer player, double heat) {
        data(player).setHeat(Math.min(heat, params().heatMax()));
        changed(player);
    }

    public static void setEnergy(ServerPlayer player, double energy) {
        data(player).setEnergy(Math.max(0, Math.min(energy, params().energyMax())));
        changed(player);
    }

    public static void setStamina(ServerPlayer player, double stamina) {
        data(player).setStamina(Math.max(0, Math.min(stamina, maxStamina(player))));
        changed(player);
    }

    /** Gasta stamina se houver; false = acao recusada (GDD secao 7). */
    public static boolean tryConsumeStamina(ServerPlayer player, double amount) {
        PowerData data = data(player);
        ensureInitialized(player, data);
        if (data.stamina() < amount) {
            return false;
        }
        data.setStamina(data.stamina() - amount);
        data.setLastStaminaSpendTick(now(player));
        changed(player);
        return true;
    }

    public static double maxStamina(ServerPlayer player) {
        return PowerMath.maxStamina(effectiveRelease(player), params());
    }

    /** Golpe dado ou recebido: o traje passa a esfriar no ritmo de combate. */
    public static void markCombat(ServerPlayer player) {
        data(player).setLastCombatTick(now(player));
    }

    public static DamageSource overheatDamage(ServerPlayer player) {
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SUIT_OVERHEAT);
        return new DamageSource(type);
    }

    // --- tick ----------------------------------------------------------------------------------------------------

    /** Um tick do servidor para este jogador: stamina, calor, energia, panico, efeitos e envio ao dono. */
    static void tick(ServerPlayer player) {
        PowerData data = data(player);
        PowerParams params = params();
        long now = now(player);
        ensureInitialized(player, data);

        boolean inCombat = !PowerData.never(data.lastCombatTick())
                && now - data.lastCombatTick() <= ServerConfig.COMBAT_GRACE_TICKS.get();
        data.setHeat(PowerMath.heatAfterTick(data.heat(), inPanic(player) ? 0 : data.surge(), inCombat, params));
        if (!inPanic(player) && data.heat() >= params.heatMax()) {
            startPanic(player, data, now);
        }

        sprint(player, data, now);
        HeatStage stage = heatStage(player);
        long sinceSpent = PowerData.never(data.lastStaminaSpendTick()) ? Long.MAX_VALUE
                : now - data.lastStaminaSpendTick();
        double maxStamina = maxStamina(player);
        if (CombatService.isBlocking(player)) {
            // GDD secao 7: bloqueando, regenera devagar (5/s) e sem o atraso normal.
            double regen = ServerConfig.BLOCK_STAMINA_REGEN_PER_SECOND.get() / TICKS_PER_SECOND;
            data.setStamina(Math.min(maxStamina, data.stamina() + regen));
        } else {
            data.setStamina(PowerMath.staminaAfterTick(data.stamina(), maxStamina, sinceSpent, stage, params));
        }
        data.setEnergy(PowerMath.energyAfterTick(data.energy(), params));
        applyHeatEffects(player, stage, now);

        int effective = effectiveRelease(player);
        if (effective != data.lastAppliedRelease()) {
            applyModifiers(player, effective, params);
            data.setLastAppliedRelease(effective);
            player.setData(KN8Attachments.RELEASE_VISUAL, effective);
        }
        suitEffects(player, effective, stage, now);
        PowerView view = view(player);
        if (!view.equals(data.lastSentView())) {
            data.setLastSentView(view);
            NetworkSync.markDirty(player, PowerSyncS2C.CHANNEL);
        }
    }

    /**
     * Etapa D: aura do traje pela % liberada (fraca a maxima, intensidade = R/100) e brilho/fumaca de sobrecarga pelo
     * calor; em pulsos, para quem esta perto. Usa o Release e o Heat que ja existem (nenhum sistema novo).
     */
    private static void suitEffects(ServerPlayer player, int effective, HeatStage stage, long now) {
        if (now % ServerConfig.SUIT_VFX_INTERVAL_TICKS.get() != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (effective >= ServerConfig.RELEASE_AURA_MIN.get()) {
            VfxService.play(level, VfxService.SUIT_RELEASE, player.position(), effective / 100.0F);
        }
        if (stage.ordinal() >= HeatStage.OVERLOAD.ordinal()) {
            float intensity = stage == HeatStage.OVERLOAD ? 0.4F : stage == HeatStage.CRITICAL ? 0.7F : 1.0F;
            VfxService.play(level, VfxService.OVERHEAT, player.position(), intensity);
        }
    }

    /** Login e respawn: modificadores sao transitorios e o jogador pode ser um objeto novo. */
    static void reapply(ServerPlayer player) {
        PowerData data = data(player);
        data.setLastAppliedRelease(-1);
        data.setLastSentView(null);
    }

    /** Visao do dono (HUD); calculada sempre no servidor. */
    public static PowerView view(ServerPlayer player) {
        PowerData data = data(player);
        PowerParams params = params();
        int xpToNext = data.trainedRelease() >= cap(player) ? 0 : PowerMath.xpForNextPoint(data.trainedRelease(),
                params);
        return new PowerView(data.trainedRelease(), effectiveRelease(player), cap(player), data.surge(),
                (float) Math.max(0, data.stamina()), (float) maxStamina(player), (float) data.heat(),
                params.heatMax(), heatStage(player).ordinal(), (float) Math.max(0, data.energy()), data.control(),
                data.releaseXp(), xpToNext, inPanic(player), data.winded());
    }

    private static void ensureInitialized(ServerPlayer player, PowerData data) {
        if (data.stamina() < 0) {
            data.setStamina(maxStamina(player));
        }
        if (data.energy() < 0) {
            data.setEnergy(params().energyMax());
        }
    }

    /**
     * Corrida gasta stamina (GDD secao 7; Etapa 1 da 0.2). O cliente decide se corre; o servidor cobra e, sem
     * folego, corta a corrida (o cliente do dono tambem para ao ver {@code winded} na HUD sincronizada).
     */
    private static void sprint(ServerPlayer player, PowerData data, long now) {
        double cost = ServerConfig.SPRINT_STAMINA_PER_SECOND.get() / TICKS_PER_SECOND;
        boolean free = player.isCreative() || player.isSpectator();
        data.setWinded(!free && PowerMath.windedAfterTick(data.winded(), data.stamina(), cost,
                ServerConfig.SPRINT_MIN_STAMINA.get()));
        if (!player.isSprinting() || free) {
            return;
        }
        if (data.winded()) {
            player.setSprinting(false);
            return;
        }
        data.setStamina(data.stamina() - cost);
        data.setLastStaminaSpendTick(now);
    }

    private static void startPanic(ServerPlayer player, PowerData data, long now) {
        int ticks = ServerConfig.PANIC_TICKS.get();
        data.setPanicUntilTick(now + ticks);
        data.setSurge(0);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, SLOWNESS_II));
        player.hurt(overheatDamage(player), ServerConfig.PANIC_DAMAGE.get().floatValue());
        player.level().playSound(null, player.blockPosition(), KN8Sounds.OVERHEAT_ALARM.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
        KN8Constants.LOGGER.info("[kn8] Pane do traje: {} por {} ticks.", player.getGameProfile().getName(), ticks);
    }

    private static void applyHeatEffects(ServerPlayer player, HeatStage stage, long now) {
        if (now % TICKS_PER_SECOND != 0) {
            return;
        }
        double drain = switch (stage) {
            case OVERLOAD -> ServerConfig.OVERLOAD_DRAIN_PER_SECOND.get();
            case CRITICAL -> ServerConfig.CRITICAL_DRAIN_PER_SECOND.get();
            default -> 0.0;
        };
        if (drain > 0) {
            player.hurt(overheatDamage(player), (float) drain);
        }
        if (stage == HeatStage.CRITICAL) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CRITICAL_SLOWNESS_TICKS, SLOWNESS_I));
        }
    }

    private static void applyModifiers(ServerPlayer player, int release, PowerParams params) {
        setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, release / params.releaseDamageDivisor(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, PowerMath.speedBonus(release, params),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER,
                PowerMath.knockbackResistance(release, params), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            // Transitorio: nao vai para o save; e reaplicado no login, no respawn e quando a % muda.
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /**
     * Toda alteracao passa por aqui: limita a stamina ao maximo atual (bug do M10a: depois de
     * {@code /kn8 release set 0} a stamina ficava 130/100 ate o proximo tick) e marca o envio ao dono.
     */
    private static void changed(ServerPlayer player) {
        PowerData data = data(player);
        if (data.stamina() > maxStamina(player)) {
            data.setStamina(maxStamina(player));
        }
        NetworkSync.markDirty(player, PowerSyncS2C.CHANNEL);
    }

    /** Devolve stamina (ex.: parry bem-sucedido, GDD secao 7), sem passar do maximo. */
    public static void restoreStamina(ServerPlayer player, double amount) {
        PowerData data = data(player);
        ensureInitialized(player, data);
        data.setStamina(Math.min(maxStamina(player), data.stamina() + Math.max(0, amount)));
        changed(player);
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }
}
