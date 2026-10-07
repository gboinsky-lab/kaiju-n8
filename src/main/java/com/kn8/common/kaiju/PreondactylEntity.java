// src/main/java/com/kn8/common/kaiju/PreondactylEntity.java
package com.kn8.common.kaiju;

import java.util.Optional;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.FlyerDef;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Preondactyl (0.6-E, especificacao do Miguel secao 21.6): kaiju voador. Os golpes sao os do {@code kaiju/*.json}
 * (raio de energia, mordida, garra, cauda, golpe do mergulho), escolhidos como em qualquer kaiju; o voo e as
 * mecanicas proprias vem do {@code flyer/preondactyl.json}:
 *
 * <ul>
 *   <li>com alvo: decola, circula acima dele (raio de cima) e de tempos em tempos mergulha para os golpes de perto e
 *   volta a subir; sem alvo por um tempo, pousa;</li>
 *   <li>frente blindada (dano x {@code front_damage_multiplier}) e costas fracas: posicionamento conta;</li>
 *   <li>autodestruicao com a vida baixa: aviso com contagem, depois explosao em area.</li>
 * </ul>
 *
 * <p>Comandado pelo No. 10: o No10Service passa o alvo dele aos kaiju por perto. Estado de voo sincronizado (so a
 * animacao no cliente); o resto e so do servidor.</p>
 */
public class PreondactylEntity extends KaijuEntity {

    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(PreondactylEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_TURN_DEGREES = 20;
    private static final double CIRCLE_STEP = 0.035;
    private static final double MIN_GROUND_CLEARANCE = 3.0;
    private static final int MAX_DIVE_TICKS = 60;
    private static final int CLIMB_TICKS = 30;
    private static final double CLIMB_HEIGHT = 7.0;
    private static final double DIVE_REACH_SLACK = 1.5;
    private static final double LAND_SPEED = 0.6;
    private static final double ANNOUNCE_RADIUS = 48.0;
    private static final int COUNTDOWN_STEP_TICKS = 20;
    private static final int SELF_DESTRUCT_POWER = 3;
    private static final double BACK_COSINE = -0.5;

    private enum Phase { CIRCLE, DIVE, CLIMB }

    private Phase phase = Phase.CIRCLE;
    private double circleAngle;
    private long nextDiveAt;
    private long phaseEndsAt;
    private int idleTicks;
    private long selfDestructAt = -1;

    public PreondactylEntity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, MAX_TURN_DEGREES, true);
    }

    public Optional<FlyerDef> flyer() {
        return KN8Data.FLYER.get(kaijuId(), false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    private void setFlying(boolean flying) {
        entityData.set(FLYING, flying);
        setNoGravity(flying);
    }

    public boolean isSelfDestructing() {
        return selfDestructAt >= 0;
    }

    @Override
    protected void registerGoals() {
        // O voo e os golpes ficam no customServerAiStep (abaixo); aqui so olhar e escolher alvo.
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, true));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // --- voo -----------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Optional<FlyerDef> found = flyer();
        if (found.isEmpty()) {
            return;
        }
        FlyerDef def = found.get();
        getAttribute(Attributes.FLYING_SPEED).setBaseValue(def.flySpeed());
        long now = level().getGameTime();
        if (isSelfDestructing()) {
            hover();
            tickSelfDestruct(def, now);
            return;
        }
        def.selfDestruct().filter(destruct -> getHealth() < getMaxHealth() * destruct.healthBelow())
                .ifPresent(destruct -> startSelfDestruct(destruct, now));
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            idleTicks = 0;
            if (!isFlying()) {
                setFlying(true);
                nextDiveAt = now + def.diveCooldownTicks() / 2;
            }
            fight(def, target, now);
        } else if (isFlying()) {
            land(def);
        }
    }

    private void fight(FlyerDef def, LivingEntity target, long now) {
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (isUsingAbility()) {
            hover();
            return;
        }
        switch (phase) {
            case CIRCLE -> {
                circleAngle += CIRCLE_STEP;
                double x = target.getX() + Math.cos(circleAngle) * def.circleRadius();
                double z = target.getZ() + Math.sin(circleAngle) * def.circleRadius();
                double y = Math.max(target.getY() + def.cruiseHeight(), groundY(x, z) + MIN_GROUND_CLEARANCE);
                moveControl.setWantedPosition(x, y, z, 1.0);
                tryAttack(target);
                if (now >= nextDiveAt) {
                    phase = Phase.DIVE;
                    phaseEndsAt = now + MAX_DIVE_TICKS;
                }
            }
            case DIVE -> {
                moveControl.setWantedPosition(target.getX(), target.getY() + target.getBbHeight() * 0.5,
                        target.getZ(), def.diveSpeed());
                boolean close = edgeDistance(target) <= meleeReach() + DIVE_REACH_SLACK
                        && Math.abs(target.getY() - getY()) <= getBbHeight() + DIVE_REACH_SLACK;
                if ((close && tryAttack(target)) || now >= phaseEndsAt) {
                    phase = Phase.CLIMB;
                    phaseEndsAt = now + CLIMB_TICKS;
                    nextDiveAt = now + def.diveCooldownTicks();
                }
            }
            case CLIMB -> {
                Vec3 away = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(4.0);
                moveControl.setWantedPosition(getX() + away.x, getY() + CLIMB_HEIGHT, getZ() + away.z, 1.0);
                if (now >= phaseEndsAt) {
                    phase = Phase.CIRCLE;
                }
            }
        }
    }

    /** Sem alvo: plana um tempo e depois desce ate o chao (pousado, a gravidade volta). */
    private void land(FlyerDef def) {
        phase = Phase.CIRCLE;
        if (++idleTicks < def.landAfterIdleTicks()) {
            hover();
            return;
        }
        moveControl.setWantedPosition(getX(), groundY(getX(), getZ()), getZ(), LAND_SPEED);
        if (onGround()) {
            setFlying(false);
        }
    }

    private void hover() {
        moveControl.setWantedPosition(getX(), getY(), getZ(), 0.0);
        setDeltaMovement(getDeltaMovement().scale(0.8));
    }

    private double groundY(double x, double z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }

    // --- couraca -------------------------------------------------------------------------------------------------

    /** Frente blindada, costas fracas: o dano depende de onde veio o golpe em relacao ao corpo. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.getSourcePosition() != null) {
            amount *= flyer().map(def -> facingMultiplier(def, source.getSourcePosition())).orElse(1.0F);
        }
        return super.hurt(source, amount);
    }

    public float facingMultiplier(FlyerDef def, Vec3 from) {
        Vec3 to = new Vec3(from.x - getX(), 0, from.z - getZ());
        if (to.lengthSqr() < 1.0E-4) {
            return 1.0F;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        double cosine = forward.dot(to.normalize());
        if (cosine >= Math.cos(Math.toRadians(def.frontArcDegrees() / 2.0))) {
            return def.frontDamageMultiplier();
        }
        return cosine <= BACK_COSINE ? def.backDamageMultiplier() : 1.0F;
    }

    // --- autodestruicao ------------------------------------------------------------------------------------------

    private void startSelfDestruct(FlyerDef.SelfDestruct destruct, long now) {
        selfDestructAt = now + destruct.warningTicks();
        triggerAnim("special", "self_destruct");
        level().playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 4.0F, 0.5F);
        announce(Component.translatable("kn8.preondactyl.self_destruct").withStyle(ChatFormatting.RED,
                ChatFormatting.BOLD), false);
    }

    private void tickSelfDestruct(FlyerDef def, long now) {
        ServerLevel level = (ServerLevel) level();
        long left = selfDestructAt - now;
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5, getZ(), 4,
                getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.02);
        if (left > 0 && left % COUNTDOWN_STEP_TICKS == 0) {
            announce(Component.literal(String.valueOf(left / COUNTDOWN_STEP_TICKS)).withStyle(ChatFormatting.RED,
                    ChatFormatting.BOLD), true);
        }
        if (left > 0) {
            return;
        }
        def.selfDestruct().ifPresent(destruct -> explode(level, destruct));
        selfDestructAt = -1;
        kill();
    }

    private void explode(ServerLevel level, FlyerDef.SelfDestruct destruct) {
        Vec3 center = position().add(0, getBbHeight() * 0.5, 0);
        double radius = destruct.radius();
        float damage = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * destruct.damageMultiplier());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center)
                .inflate(radius), living -> living.isAlive() && !(living instanceof KaijuEntity))) {
            if (target.getBoundingBox().distanceToSqr(center) > radius * radius) {
                continue;
            }
            target.hurt(damageSources().explosion(this, this), damage);
            Vec3 push = target.position().subtract(center).normalize();
            target.knockback(2.0, -push.x, -push.z);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 1.0, 1.0, 1.0, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.7F);
        if (destruct.destructionRadius() > 0) {
            DestructionService.request(level, center, new AbilityDef.Destruction(destruct.destructionRadius(),
                    SELF_DESTRUCT_POWER, true, 1.5F), this);
        }
    }

    private void announce(Component message, boolean actionBar) {
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(ANNOUNCE_RADIUS))) {
            player.displayClientMessage(message, actionBar);
        }
    }

    // --- animacao ------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation movementAnimation(boolean moving, RawAnimation idle, RawAnimation walk) {
        return isFlying() ? loop("movement", "fly") : super.movementAnimation(moving, idle, walk);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "special", 0, state -> PlayState.STOP)
                .triggerableAnim("self_destruct", RawAnimation.begin().thenLoop(kaijuId().getPath()
                        + ".action.self_destruct")));
    }
}
