// src/main/java/com/kn8/common/combat/MeleeRaycast.java
package com.kn8.common.combat;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Raycast de golpe corpo a corpo no servidor (padrao do PT7): do olho do atacante na direcao do olhar, ate o alcance
 * da arma, parando em blocos. Enxerga as partes de kaiju (sao entidades miraveis), entao o golpe acerta a parte para
 * onde o jogador olha e o multiplicador dela vale.
 */
public final class MeleeRaycast {

    private MeleeRaycast() {
    }

    public static Optional<Entity> findTarget(Level level, Entity attacker, Vec3 from, Vec3 direction, double reach) {
        return findTarget(level, attacker, from, direction, reach, entity -> true);
    }

    /** Com filtro extra (ex.: soldados nao acertam aliados). */
    public static Optional<Entity> findTarget(Level level, Entity attacker, Vec3 from, Vec3 direction, double reach,
            Predicate<Entity> filter) {
        Vec3 end = from.add(direction.normalize().scale(reach));
        BlockHitResult block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, attacker));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB area = new AABB(from, end).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(attacker, from, end, area,
                entity -> !entity.isSpectator() && entity.isPickable() && !entity.is(attacker) && filter.test(entity),
                from.distanceToSqr(end));
        return hit == null ? Optional.empty() : Optional.of(hit.getEntity());
    }
}
