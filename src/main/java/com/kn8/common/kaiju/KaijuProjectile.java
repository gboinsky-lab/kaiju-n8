// src/main/java/com/kn8/common/kaiju/KaijuProjectile.java
package com.kn8.common.kaiju;

import java.util.Optional;

import org.joml.Vector3f;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.vfx.AbilityEffects;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Projetil de habilidade de kaiju (0.6, tipo {@code kn8:projectile}): raio de energia do Honju, teia da
 * Trichonephila, Finger Gun do No. 9. Voa reto, sem gravidade, ate {@code projectile_range}; ao acertar uma entidade
 * ou um bloco, causa o dano (direto ou em area com {@code explosion_radius}), aplica a lentidao do JSON e toca os
 * efeitos da habilidade. Nao atinge kaiju, partes nem carcacas. Nao e salvo no mundo.
 *
 * <p>So o id da habilidade vai ao cliente (dado sincronizado): o rastro e desenhado la, na cor {@code color} do
 * JSON (o registro de habilidades e sincronizado).</p>
 */
public class KaijuProjectile extends Projectile {

    private static final EntityDataAccessor<String> ABILITY =
            SynchedEntityData.defineId(KaijuProjectile.class, EntityDataSerializers.STRING);
    private static final float TRAIL_SCALE = 1.4F;
    private static final double FORWARD_SPAWN = 0.6;

    private float damage;
    private double travelled;

    public KaijuProjectile(EntityType<? extends KaijuProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** Dispara do ponto {@code from} na direcao {@code direction} (normalizada aqui). */
    public static KaijuProjectile shoot(KaijuEntity owner, ResourceLocation abilityId, AbilityDef ability, Vec3 from,
            Vec3 direction, float damage) {
        KaijuProjectile projectile = new KaijuProjectile(KN8Entities.KAIJU_PROJECTILE.get(), owner.level());
        Vec3 dir = direction.normalize();
        projectile.setOwner(owner);
        projectile.entityData.set(ABILITY, abilityId.toString());
        projectile.damage = damage;
        projectile.setPos(from.add(dir.scale(FORWARD_SPAWN)));
        projectile.setDeltaMovement(dir.scale(ability.behavior().projectileSpeed()));
        owner.level().addFreshEntity(projectile);
        return projectile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ABILITY, "");
    }

    private Optional<AbilityDef> ability() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(ABILITY));
        return id == null ? Optional.empty() : KN8Data.ABILITY.get(id, level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (level().isClientSide()) {
            trail(motion);
            setPos(position().add(motion));
            return;
        }
        Optional<AbilityDef> ability = ability();
        if (ability.isEmpty()) {
            discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            return;
        }
        setPos(position().add(motion));
        travelled += motion.length();
        if (travelled > ability.get().behavior().projectileRange()) {
            discard();
        }
    }

    private void trail(Vec3 motion) {
        int color = ability().map(def -> def.behavior().color()).orElse(0xFFFFFF);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F), TRAIL_SCALE);
        for (int i = 0; i < 3; i++) {
            Vec3 at = position().subtract(motion.scale(i / 3.0));
            level().addParticle(dust, at.x, at.y, at.z, 0, 0, 0);
        }
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof KaijuEntity) && !(entity instanceof KaijuPart)
                && !(entity instanceof CarcassEntity) && !(entity instanceof KaijuProjectile);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Optional<AbilityDef> ability = ability();
        if (ability.isEmpty()) {
            return;
        }
        if (ability.get().behavior().explosionRadius() <= 0 && result.getEntity() instanceof LivingEntity target) {
            hurt(target, ability.get());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide()) {
            return;
        }
        ability().ifPresent(def -> {
            Vec3 where = result.getLocation();
            if (def.behavior().explosionRadius() > 0) {
                explode(def, where);
            }
            if (getOwner() != null) {
                AbilityEffects.play(getOwner(), def, where);
            }
        });
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // O dano em area (se houver) sai em onHit; bater no bloco so encerra o voo.
    }

    /** Dano em area: todos os vivos (menos kaiju) com a hitbox a ate {@code explosion_radius} do ponto. */
    private void explode(AbilityDef ability, Vec3 where) {
        double radius = ability.behavior().explosionRadius();
        AABB area = new AABB(where, where).inflate(radius);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && !(living instanceof KaijuEntity))) {
            if (target.getBoundingBox().distanceToSqr(where) <= radius * radius) {
                hurt(target, ability);
            }
        }
    }

    private void hurt(LivingEntity target, AbilityDef ability) {
        Entity owner = getOwner();
        boolean hurt = target.hurt(damageSources().mobProjectile(this,
                owner instanceof LivingEntity living ? living : null), damage);
        if (hurt && ability.behavior().slowTicks() > 0) {
            // Teia: lentidao (que tambem corta a corrida) pelo tempo do JSON.
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ability.behavior().slowTicks(),
                    ability.behavior().slowLevel()));
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
