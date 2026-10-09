// src/main/java/com/kn8/common/combat/SlashProjectile.java
package com.kn8.common.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import com.kn8.common.data.def.SlashSpec;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Corte a distancia (0.6-D): Kuuchi e Kosa-uchi do Hoshina e o especial da espada dele para o jogador. Voa reto, sem
 * gravidade, ate {@code range} blocos; atravessa entidades (cada uma leva uma vez) e para no primeiro bloco solido.
 * Aliados de quem cortou ficam de fora ({@link SpecialAttacks#isAlly}); kaiju levam no corpo, com o fator de dano
 * contra kaiju de quem cortou ({@code kaijuFactor}: soldado especial pelo JSON, jogador 1,0). Nao e salvo no mundo.
 *
 * <p>Cor, inclinacao e largura vao ao cliente como dados sincronizados; o arco do corte e desenhado la com
 * particulas (sem pacote por tick).</p>
 */
public class SlashProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.FLOAT);
    /** 0.7-C: rastro reto de bala no lugar do arco (tiros do Reno, da Mina e da baioneta do Narumi). */
    private static final EntityDataAccessor<Boolean> BULLET =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.BOOLEAN);
    /** Pontos do rastro de bala por tick (atras da ponta, ao longo do voo). */
    private static final int BULLET_POINTS = 4;
    /** Pontos do arco desenhados por tick e abertura do arco (graus para cada lado). */
    private static final int ARC_POINTS = 9;
    private static final float ARC_DEGREES = 70.0F;
    private static final float DUST_SCALE = 1.1F;
    private static final double FORWARD_SPAWN = 0.8;

    private final Set<UUID> struck = new HashSet<>();
    private float damage;
    private float kaijuFactor = 1.0F;
    private double range;
    private double travelled;
    private float explosionRadius;
    private int slowTicks;
    private int slowLevel = 1;

    public SlashProjectile(EntityType<? extends SlashProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * Dispara os cortes do {@code spec} a partir dos olhos de quem corta, na direcao {@code direction}; devolve
     * quantos cortes sairam. O segundo corte de um par inclina para o outro lado (o "X").
     */
    public static int fire(LivingEntity owner, Vec3 direction, SlashSpec spec, float damage, float kaijuFactor) {
        Vec3 dir = direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        for (int index = 0; index < spec.count(); index++) {
            float offset = spec.count() == 1 ? 0.0F
                    : spec.spreadDegrees() * (index / (float) (spec.count() - 1) * 2.0F - 1.0F);
            Vec3 heading = Vec3.directionFromRotation(pitch, yaw + offset);
            SlashProjectile slash = new SlashProjectile(KN8Entities.SLASH_PROJECTILE.get(), owner.level());
            slash.setOwner(owner);
            slash.damage = damage;
            slash.kaijuFactor = kaijuFactor;
            slash.range = spec.range();
            slash.entityData.set(COLOR, spec.color());
            slash.entityData.set(ROLL, index % 2 == 0 ? spec.rollDegrees() : -spec.rollDegrees());
            slash.entityData.set(WIDTH, spec.width());
            slash.entityData.set(BULLET, spec.bullet());
            slash.explosionRadius = spec.explosionRadius();
            slash.slowTicks = spec.slowTicks();
            slash.slowLevel = spec.slowLevel();
            slash.setPos(owner.getEyePosition().subtract(0, 0.3, 0).add(heading.scale(FORWARD_SPAWN)));
            slash.setDeltaMovement(heading.scale(spec.speed()));
            slash.setYRot(yaw + offset);
            slash.setXRot(pitch);
            owner.level().addFreshEntity(slash);
        }
        return spec.count();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xB070FF);
        builder.define(ROLL, 0.0F);
        builder.define(WIDTH, 1.2F);
        builder.define(BULLET, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (level().isClientSide()) {
            drawArc(motion);
            setPos(position().add(motion));
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(motion);
        HitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this));
        if (block.getType() != HitResult.Type.MISS) {
            to = block.getLocation();
        }
        Vec3 hit = strikeAlong(from, to);
        if (hit != null && explosionRadius > 0.0F) {
            explode(hit);
            return;
        }
        if (block.getType() != HitResult.Type.MISS) {
            if (explosionRadius > 0.0F) {
                explode(to);
                return;
            }
            ((ServerLevel) level()).sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 8, 0.2, 0.2, 0.2, 0.2);
            discard();
            return;
        }
        setPos(to);
        travelled += motion.length();
        if (travelled > range) {
            if (explosionRadius > 0.0F) {
                explode(to);
                return;
            }
            discard();
        }
    }

    /**
     * 0.7-C: explosao do tiro do canhao. Fere uma vez quem estiver no raio (menos aliados e quem o tiro ja acertou),
     * com o mesmo dano e fator contra kaiju; nao quebra blocos (o dano a construcoes fica com os kaiju).
     */
    private void explode(Vec3 at) {
        ServerLevel server = (ServerLevel) level();
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, explosionRadius * 0.3,
                explosionRadius * 0.3, explosionRadius * 0.3, 0.02);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 1.1F);
        if (getOwner() instanceof LivingEntity source) {
            DamageSource damageSource = damageSources().explosion(this, source);
            AABB area = new AABB(at, at).inflate(explosionRadius);
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area,
                    living -> living != source && living.isAlive() && !SpecialAttacks.isAlly(source, living)
                            && !struck.contains(living.getUUID()))) {
                if (target.getBoundingBox().distanceToSqr(at) > explosionRadius * explosionRadius) {
                    continue;
                }
                struck.add(target.getUUID());
                target.invulnerableTime = 0;
                if (target.hurt(damageSource, target instanceof KaijuEntity ? damage * kaijuFactor : damage)) {
                    slow(target);
                }
            }
        }
        discard();
    }

    /** 0.7-C: municao congelante (Lentidao do JSON) em quem o tiro acertou. */
    private void slow(LivingEntity target) {
        if (slowTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowLevel - 1));
        }
    }

    /**
     * Fere quem a faixa do corte (largura {@code width}) cruzou neste tick, uma vez por alvo. Devolve o centro do
     * primeiro alvo atingido (onde o tiro explosivo explode) ou {@code null}.
     */
    private Vec3 strikeAlong(Vec3 from, Vec3 to) {
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity source)) {
            return null;
        }
        Vec3 first = null;
        double width = entityData.get(WIDTH);
        AABB swept = new AABB(from, to).inflate(width);
        DamageSource damageSource = damageSources().mobProjectile(this, source);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, swept,
                living -> living != source && living.isAlive() && !SpecialAttacks.isAlly(source, living))) {
            if (!struck.add(target.getUUID())) {
                continue;
            }
            float amount = target instanceof KaijuEntity ? damage * kaijuFactor : damage;
            // Os dois cortes do Kosa-uchi chegam quase juntos: a invulnerabilidade vanilla engoliria o segundo.
            target.invulnerableTime = 0;
            if (target.hurt(damageSource, amount)) {
                slow(target);
                Vec3 center = target.getBoundingBox().getCenter();
                ((ServerLevel) level()).sendParticles(entityData.get(BULLET) ? ParticleTypes.CRIT
                        : ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0, 0, 0, 0);
                if (first == null) {
                    first = center;
                }
            }
        }
        return first;
    }

    /** Arco do corte, perpendicular ao voo e inclinado por {@code roll} (cliente); bala: rastro reto. */
    private void drawArc(Vec3 motion) {
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        if (entityData.get(BULLET)) {
            drawBullet(motion);
            return;
        }
        Vec3 forward = motion.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        float roll = entityData.get(ROLL) * Mth.DEG_TO_RAD;
        Vec3 side = right.scale(Mth.cos(roll)).add(up.scale(Mth.sin(roll)));
        double width = entityData.get(WIDTH);
        int color = entityData.get(COLOR);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F), DUST_SCALE);
        for (int i = 0; i < ARC_POINTS; i++) {
            float angle = (i / (float) (ARC_POINTS - 1) * 2.0F - 1.0F) * ARC_DEGREES * Mth.DEG_TO_RAD;
            // Meia-lua: as pontas ficam um pouco para tras do centro.
            Vec3 at = position().add(side.scale(Mth.sin(angle) * width))
                    .add(forward.scale((Mth.cos(angle) - 1.0F) * width * 0.6));
            level().addParticle(dust, at.x, at.y, at.z, 0, 0, 0);
        }
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SWEEP_ATTACK, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    /** 0.7-C: rastro de bala (pontos na cor do tiro ao longo do ultimo trecho do voo e um pouco de fumaca). */
    private void drawBullet(Vec3 motion) {
        int color = entityData.get(COLOR);
        float scale = Math.max(0.6F, Math.min(2.5F, entityData.get(WIDTH) * 2.0F));
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F), scale);
        for (int i = 0; i < BULLET_POINTS; i++) {
            Vec3 at = position().subtract(motion.scale(i / (double) BULLET_POINTS));
            level().addParticle(dust, at.x, at.y, at.z, 0, 0, 0);
        }
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        // O voo e resolvido em tick() (atravessa entidades, para em bloco).
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public float damage() {
        return damage;
    }
}
