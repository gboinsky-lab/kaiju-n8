// src/main/java/com/kn8/common/soldier/special/HoshinaNo10Entity.java
package com.kn8.common.soldier.special;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Hoshina com o traje numerado 10 (0.6-F, especificacao do Miguel secao 3). Tudo o que o Hoshina faz, mais:
 *
 * <ul>
 *   <li><b>cauda independente</b> (TailController da especificacao): a cauda segura a terceira espada (osso
 *   {@code item_tail}) e corta sozinha, mesmo com o Hoshina lutando com outro alvo. Prioridade: quem esta atacando o
 *   Hoshina, depois quem esta atras ou do lado, depois o alvo dele;</li>
 *   <li><b>guarda da cauda</b>: golpe pesado ou projetil vindo de fora da frente cai para {@code guard_factor};</li>
 *   <li><b>sincronizacao</b> ({@code numbers10_sync}) = a % de Release, que sobe com a escalada de combate ate 100%
 *   ({@code max_release} 100, so neste traje); a aura mostra;</li>
 *   <li><b>Full Release</b> a 100%: dano, velocidade e recargas melhores e as tecnicas com
 *   {@code requires_full_release} liberadas (Juni-hitoe: 12 golpes, o ultimo forte, ignora a armadura).</li>
 * </ul>
 * Numeros em {@code special_soldier/hoshina_no10.json}. Estado da cauda so no servidor.
 */
public class HoshinaNo10Entity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_NO10 = KN8Constants.id("hoshina_no10");
    public static final String VARIANT_NO10 = "hoshina_no10";
    private static final double TAIL_SEARCH_MARGIN = 3.0;
    private static final double SIDE_COSINE = 0.5;
    private static final int FULL_RELEASE = 100;

    private long tailReadyAt;
    private long guardReadyAt;
    private int tailAttacks;
    private int guards;

    public HoshinaNo10Entity(EntityType<? extends HoshinaNo10Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_NO10;
    }

    @Override
    protected String variantName() {
        return VARIANT_NO10;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_NO10;
    }

    public Optional<SpecialSoldierDef.Numbers10> numbers10() {
        return profile().flatMap(SpecialSoldierDef::numbers10);
    }

    /** Sincronizacao com o traje (0-100%): a % de Release. */
    public int sync() {
        return release();
    }

    @Override
    public boolean isFullRelease() {
        return release() >= FULL_RELEASE;
    }

    @Override
    public double damageMultiplier() {
        double factor = isFullRelease() ? numbers10().map(n10 -> n10.fullRelease().damageMultiplier()).orElse(1.0F)
                : 1.0;
        return super.damageMultiplier() * factor;
    }

    @Override
    protected double cooldownFactor() {
        return isFullRelease() ? numbers10().map(n10 -> n10.fullRelease().cooldownMultiplier()).orElse(1.0F) : 1.0;
    }

    @Override
    protected double speedFactor() {
        return isFullRelease() ? numbers10().map(n10 -> n10.fullRelease().speedMultiplier()).orElse(1.0F) : 1.0;
    }

    public int tailAttacks() {
        return tailAttacks;
    }

    public int guards() {
        return guards;
    }

    // --- cauda ---------------------------------------------------------------------------------------------------

    @Override
    protected void tickExtra(long now) {
        Optional<SpecialSoldierDef.Numbers10> n10 = numbers10();
        Optional<WeaponDef> weapon = weapon();
        if (n10.isEmpty() || weapon.isEmpty() || now < tailReadyAt) {
            return;
        }
        SpecialSoldierDef.Tail tail = n10.get().tail();
        Optional<LivingEntity> target = tailTarget(tail.reach());
        if (target.isEmpty()) {
            return;
        }
        float slack = Math.max(0.0F, tail.reach() - weapon.get().reach());
        if (strike(target.get(), tail.multiplier(), 0.0F, weapon.get(), slack)) {
            tailAttacks++;
        }
        triggerAnim("tail", "slash");
        double factor = isFullRelease() ? n10.get().fullRelease().tailCooldownMultiplier() : 1.0;
        tailReadyAt = now + Math.round(tail.cooldownTicks() * factor);
    }

    /**
     * Alvo da cauda: kaiju a ate {@code reach} (borda da hitbox). Primeiro quem esta com um golpe vindo no Hoshina
     * (proteger), depois quem esta atras ou do lado (a cauda cobre as costas), depois o mais perto.
     */
    Optional<LivingEntity> tailTarget(double reach) {
        List<KaijuEntity> near = level().getEntitiesOfClass(KaijuEntity.class,
                getBoundingBox().inflate(reach + TAIL_SEARCH_MARGIN),
                kaiju -> kaiju.isAlive() && edgeTo(kaiju) <= reach);
        if (near.isEmpty()) {
            return Optional.empty();
        }
        return near.stream().min(Comparator
                .comparing((KaijuEntity kaiju) -> !kaiju.abilityTarget().map(target -> target == this).orElse(false))
                .thenComparing(kaiju -> !isBehindOrSide(kaiju.position()))
                .thenComparingDouble(this::edgeTo)).map(kaiju -> kaiju);
    }

    private boolean isBehindOrSide(Vec3 point) {
        return facingCosine(point) < SIDE_COSINE;
    }

    /** Cosseno entre a frente do corpo e a direcao ate o ponto (1 = bem na frente, -1 = bem atras). */
    private double facingCosine(Vec3 point) {
        Vec3 to = new Vec3(point.x - getX(), 0, point.z - getZ());
        if (to.lengthSqr() < 1.0E-4) {
            return 1.0;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).dot(to.normalize());
    }

    /** Guarda da cauda: golpe pesado ou projetil vindo de fora da frente; o resto segue o Hoshina (parry etc.). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || source.getSourcePosition() == null) {
            return super.hurt(source, amount);
        }
        Optional<SpecialSoldierDef.Numbers10> n10 = numbers10();
        long now = level().getGameTime();
        boolean heavy = source.getEntity() instanceof KaijuEntity kaiju && kaiju.isDealingHeavyHit();
        boolean threat = heavy || source.is(DamageTypeTags.IS_PROJECTILE);
        if (n10.isPresent() && threat && now >= guardReadyAt && !isInvulnerableNow()) {
            SpecialSoldierDef.Tail tail = n10.get().tail();
            double half = Math.toRadians(tail.frontArcDegrees() / 2.0);
            if (facingCosine(source.getSourcePosition()) < Math.cos(half)) {
                amount *= tail.guardFactor();
                guardReadyAt = now + tail.guardCooldownTicks();
                guards++;
                triggerAnim("tail", "guard");
                Vec3 at = position().add(source.getSourcePosition().subtract(position()).normalize())
                        .add(0, getBbHeight() * 0.6, 0);
                ((ServerLevel) level()).sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25,
                        0.3);
                level().playSound(null, getX(), getEyeY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.6F,
                        1.6F);
            }
        }
        return super.hurt(source, amount);
    }

    // --- animacao ------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        String prefix = animPrefix() + ".tail.";
        RawAnimation idle = RawAnimation.begin().thenLoop(prefix + "idle");
        controllers.add(new AnimationController<>(this, "tail", 3, state -> state.setAndContinue(idle))
                .triggerableAnim("slash", RawAnimation.begin().thenPlay(prefix + "slash"))
                .triggerableAnim("guard", RawAnimation.begin().thenPlay(prefix + "guard")));
    }
}
