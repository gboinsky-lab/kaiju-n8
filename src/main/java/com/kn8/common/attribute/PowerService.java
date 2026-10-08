// src/main/java/com/kn8/common/attribute/PowerService.java
package com.kn8.common.attribute;

import com.kn8.KN8Constants;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.craft.SuitEvents;
import com.kn8.common.data.def.SuitDef;
import com.kn8.common.network.NetworkSync;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.power.BodyParams;
import com.kn8.core.power.BodyStat;
import com.kn8.core.power.HeatStage;
import com.kn8.core.power.PowerMath;
import com.kn8.core.power.PowerParams;
import com.kn8.core.power.TalentParams;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
 * <p>0.5.0 (Biblioteca v21 Prioridade 1, decisoes do Miguel de 2026-10-08):</p>
 * <ul>
 *   <li>o Release <b>so funciona com o traje</b> da Forca de Defesa vestido;</li>
 *   <li>o jogador <b>sobe</b> a % segurando a tecla de Release (e desce com Shift); a aura aparece junto;</li>
 *   <li>o "treinado" virou o <b>limite pessoal</b>: sorteado uma vez (talento comum 5-10%, raro 15-30%) e subido
 *   treinando, ate {@code career.releaseMax};</li>
 *   <li>passar do limite <b>nao baixa a %</b>: aquece o traje e, com o traje sobrecarregado, desgasta o corpo (dano
 *   por segundo so enquanto estiver acima do limite). Dentro do limite o uso so cansa (calor ate WARM).</li>
 * </ul>
 */
public final class PowerService {

    public static final ResourceKey<DamageType> SUIT_OVERHEAT =
            ResourceKey.create(Registries.DAMAGE_TYPE, KN8Constants.id("suit_overheat"));

    private static final ResourceLocation DAMAGE_MODIFIER = KN8Constants.id("release_damage");
    private static final ResourceLocation SPEED_MODIFIER = KN8Constants.id("release_speed");
    private static final ResourceLocation KNOCKBACK_MODIFIER = KN8Constants.id("release_knockback");
    private static final ResourceLocation BODY_SPEED_MODIFIER = KN8Constants.id("body_speed");
    /** Intervalo do alarme do traje no calor maximo (so aviso sonoro, nao e balanceamento). */
    private static final int ALARM_INTERVAL_TICKS = 40;
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

    /**
     * Teto atual: forcado por comando ou {@code career.releaseMax} (100). 0.2, decisao do Miguel: a patente nao limita
     * o Release; o teto e o mesmo para todos e se chega nele treinando.
     */
    public static int cap(ServerPlayer player) {
        int override = data(player).capOverride();
        if (override != PowerData.NO_CAP_OVERRIDE) {
            return override;
        }
        return ServerConfig.RELEASE_MAX.get();
    }

    /** Calor no maximo (estagio PANIC): so aviso; a % nao cai mais (0.5.0). */
    public static boolean inPanic(ServerPlayer player) {
        return data(player).heat() >= params().heatMax();
    }

    /** Traje da Forca de Defesa vestido? Sem ele nao ha Release (0.5.0, "igual ao anime"). */
    public static boolean suitWorn(ServerPlayer player) {
        return SuitEvents.worn(player).isPresent();
    }

    /** Limite pessoal: o treinado, limitado pelo teto. */
    public static int limit(ServerPlayer player) {
        return Math.min(data(player).trainedRelease(), cap(player));
    }

    /** % liberada na tecla (0 sem traje). */
    public static int activeRelease(ServerPlayer player) {
        return suitWorn(player) ? (int) Math.round(data(player).active()) : 0;
    }

    /** Pontos acima do limite pessoal. */
    public static int excess(ServerPlayer player) {
        return PowerMath.excess(activeRelease(player), limit(player));
    }

    /** % efetiva usada por todas as regras: a ativa mais o desespero, e so com o traje vestido. */
    public static int effectiveRelease(ServerPlayer player) {
        if (!suitWorn(player)) {
            return 0;
        }
        // 0.5: com a vida baixa a % sobe sozinha (desperationHealth/desperationMaxPoints), sem calor.
        int desperation = PowerMath.desperationBonus(player.getHealth() / Math.max(1.0F, player.getMaxHealth()),
                ServerConfig.DESPERATION_HEALTH.get(), ServerConfig.DESPERATION_MAX_POINTS.get());
        return PowerMath.effectiveRelease(activeRelease(player), desperation);
    }

    // --- atributos do corpo (0.5.0) --------------------------------------------------------------------------

    public static BodyParams bodyParams() {
        return ServerConfig.bodyParams();
    }

    public static int bodyLevel(ServerPlayer player, BodyStat stat) {
        return data(player).bodyLevel(stat);
    }

    /** Multiplicador de dano corpo a corpo pela forca. */
    public static float strengthMultiplier(ServerPlayer player) {
        return (float) (1.0 + bodyParams().strengthDamagePerLevel() * bodyLevel(player, BodyStat.STRENGTH));
    }

    /** Fator do dano recebido pela resistencia (1 = sem reducao). */
    public static float resistanceFactor(ServerPlayer player) {
        return (float) Math.max(0.0, 1.0 - bodyParams().resistancePerLevel() * bodyLevel(player,
                BodyStat.RESISTANCE));
    }

    /** Fator do custo de stamina da esquiva e do dash pela agilidade. */
    public static double agilityCostFactor(ServerPlayer player) {
        return Math.max(0.0, 1.0 - bodyParams().agilityStaminaPerLevel() * bodyLevel(player, BodyStat.AGILITY));
    }

    /** Soma XP a um atributo do corpo (fracoes acumulam) e converte em niveis. */
    public static void addBodyXp(ServerPlayer player, BodyStat stat, double amount) {
        PowerData data = data(player);
        int whole = data.takeBodyXp(stat, amount);
        if (whole <= 0) {
            return;
        }
        PowerMath.Training result = bodyParams().convert(data.bodyLevel(stat), data.bodyXp(stat) + whole);
        boolean levelUp = result.trained() > data.bodyLevel(stat);
        data.setBody(stat, result.trained(), result.xp());
        if (levelUp) {
            data.setLastAppliedRelease(-1);
        }
        changed(player);
    }

    /** Fixa o nivel de um atributo (comando e testes). */
    public static void setBodyLevel(ServerPlayer player, BodyStat stat, int level) {
        PowerData data = data(player);
        data.setBody(stat, Math.min(level, bodyParams().maxLevel()), 0);
        data.setLastAppliedRelease(-1);
        changed(player);
    }

    /** 0.5: aura de poder do jogador: a do comando, senao a do traje vestido, senao a padrao. */
    public static ResourceLocation auraOf(ServerPlayer player) {
        return player.getData(KN8Attachments.AURA_OVERRIDE)
                .or(() -> SuitEvents.worn(player).flatMap(SuitDef::aura))
                .orElse(KN8Attachments.DEFAULT_AURA);
    }

    public static HeatStage heatStage(ServerPlayer player) {
        return PowerMath.heatStage(data(player).heat(), params());
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

    /** Fixa a % ativa (comando e testes); sem traje fica em 0 no proximo tick. */
    public static void setActive(ServerPlayer player, int value) {
        data(player).setActive(value);
        changed(player);
    }

    /**
     * Tecla de Release (payload {@code kn8:release_input}): +1 segura para subir, -1 para descer, 0 soltou. Subir sem
     * traje avisa e nao faz nada.
     */
    public static void releaseInput(ServerPlayer player, int direction) {
        if (direction > 0 && !suitWorn(player)) {
            player.displayClientMessage(Component.translatable("kn8.release.need_suit"), true);
            data(player).setReleaseInput(0);
            return;
        }
        data(player).setReleaseInput(direction);
    }

    /**
     * Sorteia o limite pessoal uma vez por jogador (0.5.0): talento comum ou raro. Quem ja tinha treinado mais que o
     * sorteio fica com o treinado.
     */
    private static void ensureTalent(ServerPlayer player, PowerData data) {
        if (data.talentRolled()) {
            return;
        }
        TalentParams.Talent talent = ServerConfig.talentParams().roll(player.getRandom().nextDouble(),
                player.getRandom().nextDouble());
        data.setTalent(talent.rare());
        data.setTrainedRelease(Math.max(data.trainedRelease(), talent.limit()));
        player.displayClientMessage(Component.translatable(talent.rare() ? "kn8.release.talent_rare"
                : "kn8.release.talent", data.trainedRelease()), false);
        KN8Constants.LOGGER.info("[kn8] Talento de Release de {}: {}% ({})", player.getGameProfile().getName(),
                data.trainedRelease(), talent.rare() ? "raro" : "comum");
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

    /**
     * Um tick do servidor para este jogador: talento, % ativa, calor, stamina, energia, efeitos e envio ao dono.
     * Publico para os GameTests (o FakePlayer nao recebe o PlayerTickEvent).
     */
    public static void tick(ServerPlayer player) {
        PowerData data = data(player);
        PowerParams params = params();
        long now = now(player);
        ensureInitialized(player, data);

        ensureTalent(player, data);
        boolean suit = suitWorn(player);
        if (suit) {
            data.setActive(PowerMath.releaseAfterInput(data.active(), data.releaseInput(), params));
        } else {
            // Sem traje nao ha Release: a % zera e a tecla e ignorada ate vestir de novo.
            data.setActive(0);
            data.setReleaseInput(0);
        }
        boolean inCombat = !PowerData.never(data.lastCombatTick())
                && now - data.lastCombatTick() <= ServerConfig.COMBAT_GRACE_TICKS.get();
        double heatBefore = data.heat();
        double heatAfter = PowerMath.heatAfterTick(heatBefore, activeRelease(player), limit(player), inCombat,
                params);
        if (heatAfter > heatBefore) {
            // 0.2 (Etapa 3): o traje vestido corta parte do calor que sobe (heat_resistance do suit/*.json).
            double resistance = SuitEvents.worn(player).map(s -> (double) s.heatResistance())
                    .orElse(0.0);
            heatAfter = heatBefore + (heatAfter - heatBefore) * (1.0 - resistance);
        }
        data.setHeat(heatAfter);

        sprint(player, data, now);
        trainSpeed(player, data);
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
            setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED_MODIFIER,
                    bodyParams().speedPerLevel() * data.bodyLevel(BodyStat.SPEED),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            data.setLastAppliedRelease(effective);
            player.setData(KN8Attachments.RELEASE_VISUAL, effective);
        }
        ResourceLocation aura = auraOf(player);
        if (!aura.equals(player.getData(KN8Attachments.AURA))) {
            // Publico e so na mudanca (sync nativo): todos os clientes desenham a aura com a % de RELEASE_VISUAL.
            player.setData(KN8Attachments.AURA, aura);
        }
        suitEffects(player, stage, now);
        PowerView view = view(player);
        if (!view.equals(data.lastSentView())) {
            data.setLastSentView(view);
            NetworkSync.markDirty(player, PowerSyncS2C.CHANNEL);
        }
    }

    /**
     * Etapa D: brilho/fumaca de sobrecarga pelo calor, em pulsos, para quem esta perto. A aura de poder saiu daqui
     * na 0.5: cada cliente desenha a partir da % publica e do {@code aura/<id>.json} (AuraRenderer), sem pulsos.
     */
    private static void suitEffects(ServerPlayer player, HeatStage stage, long now) {
        if (now % ServerConfig.SUIT_VFX_INTERVAL_TICKS.get() != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
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
        return new PowerView(data.trainedRelease(), effectiveRelease(player), cap(player), excess(player),
                (float) Math.max(0, data.stamina()), (float) maxStamina(player), (float) data.heat(),
                params.heatMax(), heatStage(player).ordinal(), (float) Math.max(0, data.energy()), data.control(),
                data.releaseXp(), xpToNext, inPanic(player), data.winded(), activeRelease(player), suitWorn(player),
                data.talentRare(), data.bodyLevel(BodyStat.STRENGTH), data.bodyLevel(BodyStat.SPEED),
                data.bodyLevel(BodyStat.RESISTANCE), data.bodyLevel(BodyStat.AGILITY));
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

    /**
     * 0.5.0: o corpo so desgasta enquanto a % estiver ACIMA do limite pessoal (decisao do Miguel), e mais quanto mais
     * quente o traje; no calor maximo o desgaste e pesado e o alarme toca, mas a % nao cai.
     */
    private static void applyHeatEffects(ServerPlayer player, HeatStage stage, long now) {
        if (excess(player) <= 0) {
            return;
        }
        if (stage == HeatStage.PANIC) {
            PowerData data = data(player);
            if (PowerData.never(data.lastAlarmTick()) || now - data.lastAlarmTick() >= ALARM_INTERVAL_TICKS) {
                data.setLastAlarmTick(now);
                player.level().playSound(null, player.blockPosition(), KN8Sounds.OVERHEAT_ALARM.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        if (now % TICKS_PER_SECOND != 0) {
            return;
        }
        double drain = switch (stage) {
            case OVERLOAD -> ServerConfig.OVERLOAD_DRAIN_PER_SECOND.get();
            case CRITICAL -> ServerConfig.CRITICAL_DRAIN_PER_SECOND.get();
            case PANIC -> ServerConfig.MAX_HEAT_DRAIN_PER_SECOND.get();
            default -> 0.0;
        };
        if (drain > 0) {
            player.hurt(overheatDamage(player), (float) drain);
        }
        if (stage == HeatStage.CRITICAL || stage == HeatStage.PANIC) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CRITICAL_SLOWNESS_TICKS,
                    stage == HeatStage.PANIC ? SLOWNESS_II : SLOWNESS_I));
        }
    }

    /** Velocidade treina correndo: XP por bloco corrido no chao (0.5.0). */
    private static void trainSpeed(ServerPlayer player, PowerData data) {
        double x = player.getX();
        double z = player.getZ();
        if (!Double.isNaN(data.lastX()) && player.isSprinting() && player.onGround() && !player.isCreative()
                && !player.isSpectator()) {
            double moved = Math.hypot(x - data.lastX(), z - data.lastZ());
            // Teleporte ou montaria nao contam: um passo de corrida nunca passa de ~1 bloco por tick.
            if (moved < 1.0) {
                addBodyXp(player, BodyStat.SPEED, moved * ServerConfig.BODY_XP_SPEED_PER_BLOCK.get());
            }
        }
        data.setLastPosition(x, z);
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
