// src/main/java/com/kn8/common/combat/CombatService.java
package com.kn8.common.combat;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.anim.AnimationBridge;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.combat.CombatMath;
import com.kn8.core.power.BodyStat;
import com.kn8.core.power.PowerMath;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Combate do jogador no servidor (M10a + M10b). Fluxo do PT7: o cliente so pede ({@link CombatInputC2S}); aqui se
 * valida arma, estado e stamina; a acao entra na {@code ActionTimeline}; a animacao vai para todos (M9); o dano sai
 * no tick de impacto do JSON da arma, por raycast, uma unica vez. Cada pedido recebe um {@link CombatStateS2C} so
 * para o dono (OK ou NEGADO + motivo).
 *
 * <ul>
 *   <li>Lamina ({@code blade}): leve, pesado, combo; pesado expoe o nucleo do kaiju.</li>
 *   <li>Arma de fogo ({@code firearm}, M10b): tiro instantaneo (raycast) ate o alcance do JSON; desde a 0.5.0-D com
 *   pente e recarga por etapas do perfil da arma ({@link WeaponHandling}; reserva infinita).</li>
 *   <li>Parry e critico (M10b): o primeiro golpe depois de um parry, dentro da janela, e critico.</li>
 * </ul>
 */
public final class CombatService {

    private static final String LIGHT = "light";
    private static final String HEAVY = "heavy";
    private static final float HIT_SOUND_VOLUME = 0.8F;
    private static final float SHOT_SOUND_VOLUME = 1.0F;
    private static final float SWING_SOUND_VOLUME = 0.7F;
    private static final double TRACER_STEP = 1.5;
    private static final double MUZZLE_DISTANCE = 1.2;

    /**
     * 0.5.0: tipo de dano do golpe especial de arma (tecla R). E o unico golpe do jogador que pode passar do teto por
     * golpe em kaiju ({@code combat.maxLightHitFraction}/{@code maxHeavyHitFraction}): golpe comum nunca mata
     * kaiju de uma vez.
     */
    public static final ResourceKey<DamageType> WEAPON_SPECIAL =
            ResourceKey.create(Registries.DAMAGE_TYPE, KN8Constants.id("weapon_special"));

    private CombatService() {
    }

    /** Fonte de dano do golpe especial do jogador (conta como ataque dele). */
    public static DamageSource specialDamage(Player player) {
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(WEAPON_SPECIAL);
        return new DamageSource(type, player);
    }

    public static CombatState state(ServerPlayer player) {
        return player.getData(KN8Attachments.COMBAT);
    }

    public static boolean isBlocking(ServerPlayer player) {
        return state(player).blocking;
    }

    public static boolean isInvulnerable(ServerPlayer player) {
        CombatState state = state(player);
        return state.invulnerableUntilTick != CombatState.NEVER && now(player) < state.invulnerableUntilTick;
    }

    /** Ha um critico pos-parry esperando o proximo acerto? */
    public static boolean hasCriticalReady(ServerPlayer player) {
        CombatState state = state(player);
        return state.criticalUntilTick != CombatState.NEVER && now(player) < state.criticalUntilTick;
    }

    /** Arma do kn8 na mao principal, se houver. */
    public static Optional<WeaponDef> heldWeapon(Player player) {
        return WeaponIndex.find(player.getMainHandItem(), player.level().isClientSide());
    }

    /** Entrada do pacote C2S (ja passou pelo rate limit do C2SGuard). */
    static void handle(CombatInputC2S payload, ServerPlayer player) {
        CombatAction action = CombatAction.byIndex(payload.action());
        if (action == null || !player.isAlive() || player.isSpectator() || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        switch (action) {
            case LIGHT -> attack(player, LIGHT);
            case HEAVY -> attack(player, HEAVY);
            case BLOCK -> setBlocking(player, payload.pressed());
            case DODGE -> dodge(player, payload.dirX(), payload.dirZ());
            case DASH -> dash(player, payload.dirX(), payload.dirZ());
            case CHARGE_START -> startCharge(player);
            case CHARGE_RELEASE -> releaseCharge(player);
            case SPECIAL -> special(player);
        }
    }

    /** Resposta privada ao dono (HUD). */
    static void reply(ServerPlayer player, CombatAction action, CombatResult result) {
        CombatState state = state(player);
        int comboLength = heldWeapon(player).map(weapon -> weapon.combo().size()).orElse(0);
        PacketDistributor.sendToPlayer(player, new CombatStateS2C(action.ordinal(), result.ordinal(),
                Math.max(-1, state.comboStep), comboLength));
    }

    // --- golpes e tiros ---------------------------------------------------------------------------------------

    /** Golpe ou tiro (publico para os GameTests; em jogo vem do pacote C2S). */
    public static boolean attack(ServerPlayer player, String actionName) {
        return attack(player, actionName, 1.0F, false);
    }

    /** Golpe com multiplicador extra (ataque carregado) e opcao de critico garantido (carga completa). */
    static boolean attack(ServerPlayer player, String actionName, float extraMultiplier, boolean forceCritical) {
        CombatState state = state(player);
        CombatAction request = LIGHT.equals(actionName) ? CombatAction.LIGHT : CombatAction.HEAVY;
        Optional<WeaponDef> weapon = heldWeapon(player);
        long now = now(player);
        if (weapon.isEmpty()) {
            reply(player, request, CombatResult.DENIED_NO_WEAPON);
            return false;
        }
        WeaponDef.Action action = weapon.get().actions().get(actionName);
        if (action == null || action.impactTick() < 0) {
            // Ex.: o rifle nao tem golpe pesado. Nao e erro: so nao ha acao para esse botao.
            return false;
        }
        if (state.blocking || state.timeline.isActive(now)) {
            reply(player, request, WeaponHandling.RELOAD.equals(state.timeline.actionId())
                    ? CombatResult.RELOADING : CombatResult.DENIED_BUSY);
            return false;
        }
        boolean firearm = weapon.get().style() == WeaponDef.Style.FIREARM;
        CombatResult blocked = firearm ? WeaponHandling.canFire(player, state, weapon.get(), now) : null;
        if (blocked != null) {
            reply(player, request, blocked);
            return false;
        }
        double cost = LIGHT.equals(actionName) ? ServerConfig.LIGHT_STAMINA_COST.get()
                : extraMultiplier > 1.0F ? ServerConfig.CHARGED_STAMINA_COST.get()
                : ServerConfig.HEAVY_STAMINA_COST.get();
        boolean hasStamina = PowerService.tryConsumeStamina(player, cost);
        double slowdown = ServerConfig.NO_STAMINA_SLOWDOWN.get();
        // 0.5.0-D7 (Miguel): com mais Release o golpe fica mais rapido (duracao e impacto); a animacao acompanha.
        double speed = releaseSpeed(player);
        int duration = CombatMath.faster(CombatMath.slowed(action.durationTicks(), hasStamina, slowdown), speed);
        int impact = Math.min(duration - 1,
                CombatMath.faster(CombatMath.slowed(action.impactTick(), hasStamina, slowdown), speed));
        if (!state.timeline.tryStart(now, actionName, duration, impact)) {
            reply(player, request, CombatResult.DENIED_BUSY);
            return false;
        }
        if (firearm) {
            WeaponHandling.spendRound(player, state, weapon.get(), now);
        }
        float combo = 1.0F;
        if (LIGHT.equals(actionName) && !firearm) {
            state.comboStep = CombatMath.nextComboStep(state.comboStep, state.lastLightEndTick, now,
                    ServerConfig.COMBO_WINDOW_TICKS.get(), weapon.get().combo().size());
            combo = CombatMath.comboMultiplier(weapon.get().combo(), state.comboStep);
            state.lastLightEndTick = now + duration;
        } else {
            state.comboStep = -1;
        }
        state.currentAction = request;
        state.currentWeapon = weapon.get();
        state.currentMultiplier = action.multiplier() * combo * extraMultiplier;
        if (forceCritical) {
            // GDD secao 7: carga completa = critico (o primeiro acerto deste golpe).
            state.criticalUntilTick = now + duration + 1;
        }
        // Animacao da propria arma (pico no tick de impacto do JSON dela); armas de fogo usam "shoot". 0.5.0-D2:
        // com perfil, cada passo do combo tem o seu golpe (player.<perfil>.light_<n>) e o pesado e o da familia.
        AnimationBridge.playPlayer(player, firearm || weapon.get().profile().isEmpty()
                ? AnimationBridge.weaponAction(weapon.get().item(), firearm ? "shoot" : actionName)
                : AnimationBridge.profileAction(weapon.get().profile().get(), LIGHT.equals(actionName)
                        ? "light_" + (Math.max(0, state.comboStep) + 1) : HEAVY),
                (float) action.durationTicks() / duration);
        if (!firearm) {
            // Etapa 1 (0.2): o "vush" sai no inicio do golpe; o impacto tem som proprio no tick do JSON.
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                    request == CombatAction.HEAVY ? weaponSound(weapon.get(), "heavy", KN8Sounds.BLADE_HEAVY.get())
                            : weaponSound(weapon.get(), "swing", KN8Sounds.BLADE_SWING.get()),
                    SoundSource.PLAYERS, SWING_SOUND_VOLUME, 1.0F);
        }
        reply(player, request, hasStamina ? CombatResult.OK : CombatResult.SLOWED_NO_STAMINA);
        return true;
    }

    /** Todo tick do servidor: resolve o impacto no tick exato (publico para os GameTests). */
    public static void tick(ServerPlayer player) {
        CombatState state = state(player);
        WeaponHandling.tick(player, state, now(player));
        if (state.currentWeapon == null || !state.timeline.consumeImpact(now(player))) {
            return;
        }
        WeaponDef weapon = state.currentWeapon;
        boolean heavy = state.currentAction == CombatAction.HEAVY;
        state.currentWeapon = null;
        if (state.currentAction == CombatAction.SPECIAL && weapon.special().isPresent()) {
            double release = PowerMath.damageMultiplier(PowerService.effectiveRelease(player), PowerService.params());
            // 0.5.0: a forca do corpo vale tambem no especial (golpe da arma).
            float damage = CombatMath.damage(weapon.baseDamage(), state.currentMultiplier, 1.0F, release)
                    * PowerService.strengthMultiplier(player);
            SpecialAttacks.resolve(player, weapon.special().get(), damage, specialDamage(player));
            return;
        }
        strike(player, weapon, state.currentMultiplier, heavy);
    }

    // --- ataque especial (0.5) ---------------------------------------------------------------------------------

    /**
     * Ataque especial da arma na mao (tecla R; publico para os GameTests). Precisa de stamina inteira (nao ha versao
     * lenta) e respeita a recarga do JSON; o calor do {@code heat_cost} sobe na hora. O dano sai no tick de impacto.
     */
    public static boolean special(ServerPlayer player) {
        CombatState state = state(player);
        Optional<WeaponDef> weapon = heldWeapon(player);
        long now = now(player);
        if (weapon.isEmpty()) {
            replySpecial(player, CombatResult.DENIED_NO_WEAPON);
            return false;
        }
        Optional<WeaponDef.Special> special = weapon.get().special();
        if (special.isEmpty() && weapon.get().style() == WeaponDef.Style.FIREARM && !state.blocking
                && WeaponHandling.manualReload(player, state, weapon.get(), now)) {
            // 0.5.0-D: na arma de fogo sem especial, a tecla R recarrega.
            return true;
        }
        if (special.isEmpty()) {
            replySpecial(player, CombatResult.DENIED_NO_SPECIAL);
            return false;
        }
        if (!WeaponHandling.dualReady(player, weapon.get(), false)) {
            // 0.5.0-D3 (Miguel): tecnica de arma de par so com uma arma em cada mao (como o Hoshina NPC).
            replySpecial(player, CombatResult.DENIED_NEEDS_DUAL);
            return false;
        }
        if (state.blocking || state.timeline.isActive(now)) {
            replySpecial(player, CombatResult.DENIED_BUSY);
            return false;
        }
        if (specialCooldown(player) > 0) {
            replySpecial(player, CombatResult.DENIED_COOLDOWN);
            return false;
        }
        WeaponDef.Special def = special.get();
        if (!PowerService.tryConsumeStamina(player, def.staminaCost())) {
            replySpecial(player, CombatResult.DENIED_NO_STAMINA);
            return false;
        }
        double speed = releaseSpeed(player);
        int duration = CombatMath.faster(def.durationTicks(), speed);
        if (!state.timeline.tryStart(now, "special", duration,
                Math.min(duration - 1, CombatMath.faster(def.impactTick(), speed)))) {
            replySpecial(player, CombatResult.DENIED_BUSY);
            return false;
        }
        if (def.heatCost() > 0) {
            PowerService.setHeat(player, PowerService.data(player).heat() + def.heatCost());
        }
        state.chargeStartTick = CombatState.NEVER;
        state.comboStep = -1;
        state.currentAction = CombatAction.SPECIAL;
        state.currentWeapon = weapon.get();
        state.currentMultiplier = def.multiplier();
        state.specialCooldownTicks = def.cooldownTicks();
        state.specialReadyTick = now + def.cooldownTicks();
        AnimationBridge.playPlayer(player, AnimationBridge.weaponAction(weapon.get().item(), "special"),
                (float) def.durationTicks() / duration);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                weaponSound(weapon.get(), "heavy", KN8Sounds.BLADE_HEAVY.get()), SoundSource.PLAYERS,
                SWING_SOUND_VOLUME, 0.8F);
        replySpecial(player, CombatResult.OK);
        return true;
    }

    /** 0.5.0-D7: fator de velocidade dos golpes pelo Release efetivo do jogador ({@code [combat]}). */
    public static double releaseSpeed(ServerPlayer player) {
        return CombatMath.releaseSpeed(PowerService.effectiveRelease(player),
                ServerConfig.ATTACK_SPEED_AT_FULL_RELEASE.get());
    }

    /** Balanceamento v1.0: o golpe que esta acertando agora e pesado (ou carregado)? Decide o teto por golpe. */
    public static boolean isHeavyStrike(ServerPlayer player) {
        return state(player).currentAction == CombatAction.HEAVY;
    }

    /** Ticks que faltam para o ataque especial ficar pronto (0 = pronto). */
    public static long specialCooldown(ServerPlayer player) {
        CombatState state = state(player);
        return state.specialReadyTick == CombatState.NEVER ? 0 : Math.max(0, state.specialReadyTick - now(player));
    }

    /** Resposta do especial: leva a recarga restante e a total (ver {@link CombatStateS2C}). */
    private static void replySpecial(ServerPlayer player, CombatResult result) {
        PacketDistributor.sendToPlayer(player, new CombatStateS2C(CombatAction.SPECIAL.ordinal(), result.ordinal(),
                (int) specialCooldown(player), state(player).specialCooldownTicks));
    }

    private static void strike(ServerPlayer player, WeaponDef weapon, float multiplier, boolean heavy) {
        boolean firearm = weapon.style() == WeaponDef.Style.FIREARM;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Optional<Entity> found = MeleeRaycast.findTarget(player.level(), player, eye, look, weapon.reach());
        if (firearm) {
            shotEffects(player, weapon, eye, found.map(Entity::getBoundingBox).map(box -> box.getCenter())
                    .orElse(eye.add(look.scale(weapon.reach()))));
            // 0.1-B: clarao e fumaca na boca do cano.
            VfxService.play((ServerLevel) player.level(), VfxService.WEAPON_FIRE, eye.add(look.scale(MUZZLE_DISTANCE)),
                    look, 1.0F, 0.0F);
        }
        if (found.isEmpty()) {
            return;
        }
        Entity target = found.get();
        Entity root = target instanceof PartEntity<?> part ? part.getParent() : target;
        if (root instanceof Player && !ServerConfig.PVP_ENABLED.get()) {
            return;
        }
        CombatState state = state(player);
        boolean critical = state.criticalUntilTick != CombatState.NEVER && now(player) < state.criticalUntilTick;
        double release = PowerMath.damageMultiplier(PowerService.effectiveRelease(player), PowerService.params());
        // 0.5.0: a forca do corpo so vale no corpo a corpo (arma de fogo nao depende do braco).
        float strength = firearm ? 1.0F : PowerService.strengthMultiplier(player);
        float damage = CombatMath.withCritical(CombatMath.damage(weapon.baseDamage(), multiplier, 1.0F, release)
                * strength, critical, ServerConfig.CRITICAL_MULTIPLIER.get());
        if (heavy && !firearm && root instanceof KaijuEntity kaiju) {
            // GDD secao 12: golpe pesado expoe o nucleo (multiplicador extra por alguns segundos).
            kaiju.exposeCore(ServerConfig.CORE_EXPOSED_TICKS.get());
        }
        if (target.hurt(player.damageSources().playerAttack(player), damage)) {
            // 0.1-B: impacto no alvo; corte da lamina (mais forte no pesado e no critico).
            Vec3 hitPoint = target.getBoundingBox().getCenter();
            VfxService.play((ServerLevel) player.level(), firearm ? VfxService.IMPACT : VfxService.SLASH, hitPoint,
                    look, heavy || critical ? 1.5F : 0.8F, heavy ? 0.15F : 0.0F);
            if (critical) {
                state.criticalUntilTick = CombatState.NEVER;
                reply(player, state.currentAction == null ? CombatAction.LIGHT : state.currentAction,
                        CombatResult.CRITICAL);
            }
            if (!firearm) {
                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        weaponSound(weapon, "hit", heavy ? SoundEvents.PLAYER_ATTACK_STRONG
                                : SoundEvents.PLAYER_ATTACK_SWEEP),
                        SoundSource.PLAYERS, HIT_SOUND_VOLUME, 1.0F);
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }

    /** Disparo: o {@code shot} do JSON da arma; sem ele, pistola tem o seco e as outras o do rifle. */
    public static SoundEvent shotSound(WeaponDef weapon) {
        return weaponSound(weapon, "shot", weapon.item().getPath().contains("pistol") ? KN8Sounds.PISTOL_SHOT.get()
                : KN8Sounds.RIFLE_SHOT.get());
    }

    /**
     * Som da arma no JSON ({@code sounds.<chave>}: swing, heavy, hit, shot), ou o generico. Um id que nao esta no
     * registro vira um evento direto: o cliente toca se algum sounds.json (do mod ou de resource pack) o definir.
     */
    public static SoundEvent weaponSound(WeaponDef weapon, String key, SoundEvent fallback) {
        ResourceLocation id = weapon.sounds().get(key);
        if (id == null) {
            return fallback;
        }
        return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
    }

    /** Som do disparo e rastro de particulas ate o ponto atingido (placeholder ate a arte de efeitos). */
    private static void shotEffects(ServerPlayer player, WeaponDef weapon, Vec3 from, Vec3 to) {
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), shotSound(weapon),
                SoundSource.PLAYERS, SHOT_SOUND_VOLUME, 1.0F);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 path = to.subtract(from);
        double length = path.length();
        Vec3 step = path.normalize().scale(TRACER_STEP);
        Vec3 point = from.add(step);
        for (double travelled = TRACER_STEP; travelled < length; travelled += TRACER_STEP) {
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            point = point.add(step);
        }
    }

    // --- bloqueio, parry e esquiva -----------------------------------------------------------------------------

    /** Segurar/soltar o bloqueio (publico para os GameTests; em jogo vem do pacote C2S). */
    public static void setBlocking(ServerPlayer player, boolean pressed) {
        CombatState state = state(player);
        boolean allowed = pressed && heldWeapon(player).isPresent() && !state.timeline.isActive(now(player));
        if (allowed == state.blocking) {
            return;
        }
        state.blocking = allowed;
        if (allowed) {
            state.blockStartTick = now(player);
            AnimationBridge.playPlayer(player, WeaponHandling.guardAnimation(heldWeapon(player)));
        } else {
            AnimationBridge.stopPlayer(player);
        }
    }

    /** O golpe que chegou agora cai na janela de parry do bloqueio atual? */
    public static boolean isParry(ServerPlayer player) {
        CombatState state = state(player);
        return state.blocking && CombatMath.isParry(state.blockStartTick, now(player),
                PowerService.effectiveRelease(player), ServerConfig.PARRY_WINDOW_TICKS.get(),
                ServerConfig.PARRY_WINDOW_TICKS_HIGH.get(), ServerConfig.PARRY_HIGH_RELEASE.get());
    }

    /**
     * Parry bem-sucedido (GDD secao 7): sem dano, devolve stamina e abre a janela de critico. Kaiju Yoju sempre fica
     * atordoado; Honju so a partir de {@code combat.parryHighRelease} (GDD secao 6, faixa 60-69%).
     */
    public static void parry(ServerPlayer player, Entity attacker) {
        CombatState state = state(player);
        state.criticalUntilTick = now(player) + ServerConfig.CRITICAL_WINDOW_TICKS.get();
        PowerService.restoreStamina(player, ServerConfig.PARRY_STAMINA_REFUND.get());
        PowerService.addBodyXp(player, BodyStat.AGILITY, ServerConfig.BODY_XP_AGILITY_PER_ACTION.get());
        if (attacker instanceof KaijuEntity kaiju) {
            boolean honju = kaiju.def().map(def -> def.kaijuClass() != KaijuClass.YOJU).orElse(true);
            if (!honju || PowerService.effectiveRelease(player) >= ServerConfig.PARRY_HIGH_RELEASE.get()) {
                kaiju.stagger(ServerConfig.PARRY_STAGGER_TICKS.get());
            }
        }
        player.level().playSound(null, player.blockPosition(), KN8Sounds.PARRY.get(), SoundSource.PLAYERS, 1.0F,
                1.0F);
        reply(player, CombatAction.BLOCK, CombatResult.PARRY);
    }

    /** Guarda quebrada (sem stamina para segurar o golpe): solta o bloqueio. */
    static void breakGuard(ServerPlayer player) {
        state(player).blocking = false;
        AnimationBridge.stopPlayer(player);
        player.level().playSound(null, player.blockPosition(), KN8Sounds.GUARD_BREAK.get(), SoundSource.PLAYERS, 1.0F,
                1.0F);
        reply(player, CombatAction.BLOCK, CombatResult.GUARD_BROKEN);
    }

    static boolean dodge(ServerPlayer player, float dirX, float dirZ) {
        CombatState state = state(player);
        long now = now(player);
        if (state.blocking || state.timeline.isActive(now)) {
            reply(player, CombatAction.DODGE, CombatResult.DENIED_BUSY);
            return false;
        }
        // GDD secao 7: sem stamina nao esquiva.
        // 0.5.0: a agilidade barateia a esquiva.
        if (!PowerService.tryConsumeStamina(player, ServerConfig.DODGE_STAMINA_COST.get()
                * PowerService.agilityCostFactor(player))) {
            reply(player, CombatAction.DODGE, CombatResult.DENIED_NO_STAMINA);
            return false;
        }
        int duration = ServerConfig.DODGE_DURATION_TICKS.get();
        state.timeline.tryStart(now, "dodge", duration, -1);
        state.currentAction = CombatAction.DODGE;
        state.currentWeapon = null;
        state.invulnerableUntilTick = now + ServerConfig.DODGE_INVULNERABLE_TICKS.get();
        Vec3 direction = new Vec3(dirX, 0, dirZ);
        if (direction.lengthSqr() < 1.0E-4) {
            // Sem direcao: esquiva para tras.
            Vec3 look = player.getLookAngle();
            direction = new Vec3(-look.x, 0, -look.z);
        }
        Vec3 burst = direction.normalize().scale(ServerConfig.DODGE_SPEED.get());
        player.setDeltaMovement(burst.x, Math.max(player.getDeltaMovement().y, 0.1), burst.z);
        // O movimento do jogador e do cliente: hurtMarked manda a velocidade nova para ele.
        player.hurtMarked = true;
        AnimationBridge.playPlayer(player, AnimationBridge.PLAYER_DODGE);
        PowerService.addBodyXp(player, BodyStat.AGILITY, ServerConfig.BODY_XP_AGILITY_PER_ACTION.get());
        reply(player, CombatAction.DODGE, CombatResult.OK);
        return true;
    }

    // --- dash e ataque carregado (0.1-B, GDD secao 7) ---------------------------------------------------------

    static boolean dash(ServerPlayer player, float dirX, float dirZ) {
        CombatState state = state(player);
        long now = now(player);
        if (state.blocking || state.timeline.isActive(now)) {
            reply(player, CombatAction.DASH, CombatResult.DENIED_BUSY);
            return false;
        }
        if (!PowerService.tryConsumeStamina(player, ServerConfig.DASH_STAMINA_COST.get()
                * PowerService.agilityCostFactor(player))) {
            reply(player, CombatAction.DASH, CombatResult.DENIED_NO_STAMINA);
            return false;
        }
        state.timeline.tryStart(now, "dash", ServerConfig.DASH_DURATION_TICKS.get(), -1);
        state.currentAction = CombatAction.DASH;
        state.currentWeapon = null;
        Vec3 direction = new Vec3(dirX, 0, dirZ);
        if (direction.lengthSqr() < 1.0E-4) {
            // Sem direcao: dash para a frente (a esquiva e que vai para tras).
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x, 0, look.z);
        }
        Vec3 burst = direction.normalize().scale(ServerConfig.DASH_SPEED.get());
        player.setDeltaMovement(burst.x, Math.max(player.getDeltaMovement().y, 0.05), burst.z);
        player.hurtMarked = true;
        AnimationBridge.playPlayer(player, AnimationBridge.PLAYER_DASH);
        player.level().playSound(null, player.blockPosition(), KN8Sounds.DASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        VfxService.play((ServerLevel) player.level(), VfxService.DUST, player.position(), direction, 0.4F, 0.0F);
        PowerService.addBodyXp(player, BodyStat.AGILITY, ServerConfig.BODY_XP_AGILITY_PER_ACTION.get());
        reply(player, CombatAction.DASH, CombatResult.OK);
        return true;
    }

    /** Lamina e arma pesada (machado) carregam golpe; arma de fogo e canhao nao. */
    private static boolean isMelee(WeaponDef weapon) {
        return weapon.style() == WeaponDef.Style.BLADE || weapon.style() == WeaponDef.Style.HEAVY;
    }

    /** Clique direito pressionado com lamina ou arma pesada: comeca a carregar (so arma com golpe pesado). */
    static void startCharge(ServerPlayer player) {
        CombatState state = state(player);
        Optional<WeaponDef> weapon = heldWeapon(player);
        long now = now(player);
        if (weapon.isEmpty() || !isMelee(weapon.get())
                || !weapon.get().actions().containsKey(HEAVY) || state.blocking || state.timeline.isActive(now)) {
            state.chargeStartTick = CombatState.NEVER;
            return;
        }
        state.chargeStartTick = now;
        AnimationBridge.playPlayer(player, AnimationBridge.PLAYER_CHARGE);
    }

    /**
     * Clique direito solto: abaixo de {@code combat.chargeMinTicks}, golpe pesado comum; acima, ataque carregado
     * (custo maior, dano escala com a carga, carga completa = critico e nucleo exposto pelo golpe pesado).
     */
    static void releaseCharge(ServerPlayer player) {
        CombatState state = state(player);
        if (state.chargeStartTick == CombatState.NEVER) {
            return;
        }
        long held = now(player) - state.chargeStartTick;
        state.chargeStartTick = CombatState.NEVER;
        float fraction = CombatMath.chargeFraction(held, ServerConfig.CHARGE_MIN_TICKS.get(),
                ServerConfig.CHARGE_MAX_TICKS.get());
        if (fraction < 0) {
            attack(player, HEAVY);
            return;
        }
        attack(player, HEAVY, CombatMath.chargedMultiplier(fraction, ServerConfig.CHARGE_FULL_MULTIPLIER.get()),
                fraction >= 1.0F);
    }

    /** Fracao de carga atual para os GameTests (-1 = nao carregando ou abaixo do minimo). */
    public static float chargeFraction(ServerPlayer player) {
        CombatState state = state(player);
        return state.chargeStartTick == CombatState.NEVER ? -1.0F : CombatMath.chargeFraction(
                now(player) - state.chargeStartTick, ServerConfig.CHARGE_MIN_TICKS.get(),
                ServerConfig.CHARGE_MAX_TICKS.get());
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }
}
