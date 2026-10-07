// src/main/java/com/kn8/common/combat/SpecialAttacks.java
package com.kn8.common.combat;

import java.util.List;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.combat.SpecialGeometry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Resolvedor dos ataques especiais das armas (0.5), no tick de impacto do JSON. Nao conhece jogador nem soldado:
 * quem chama passa o dano ja calculado (jogador: Release; soldado especial: o nivel dele) e a fonte de dano. Assim o
 * mesmo ataque serve ao jogador e, depois, aos soldados especiais (docs/PLANO_SOLDADOS_ESPECIAIS.md).
 */
public final class SpecialAttacks {

    private static final float SOUND_VOLUME = 2.0F;
    /** Folga da busca: a hitbox de um kaiju largo pode estar com o centro bem fora do raio. */
    private static final double SEARCH_MARGIN = 8.0;
    /** Empurrao vertical da onda (sai do chao). */
    private static final double KNOCK_UP = 0.35;

    private SpecialAttacks() {
    }

    /** Centro do golpe: {@code forward} blocos a frente, no chao (pes de quem ataca). */
    public static Vec3 center(LivingEntity source, WeaponDef.Special special) {
        Vec3 look = source.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 forward = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(special.forward());
        return source.position().add(forward);
    }

    /**
     * Resolve o ataque especial; devolve quantos alvos levaram dano. Aliados de quem ataca (soldados, jogadores sem
     * PvP) ficam de fora; kaiju levam no corpo (sem nucleo, como os outros golpes de area).
     */
    public static int resolve(LivingEntity source, WeaponDef.Special special, float damage, DamageSource damageSource) {
        if (!(source.level() instanceof ServerLevel level)) {
            return 0;
        }
        return switch (special.type()) {
            case GROUND_SLAM -> groundSlam(level, source, special, damage, damageSource);
            case SLASH_WAVE -> slashWave(level, source, special, damage);
        };
    }

    /**
     * 0.6-D: corte que voa reto pelo olhar de quem ataca ({@link SlashProjectile}); o dano sai quando o corte cruza
     * cada alvo, nao neste tick. Devolve quantos cortes sairam.
     */
    private static int slashWave(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage) {
        int fired = SlashProjectile.fire(source, source.getLookAngle(), special.slash(), damage, 1.0F);
        effects(level, special, source.getEyePosition().add(source.getLookAngle()), source);
        return fired;
    }

    private static int groundSlam(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource) {
        Vec3 center = center(source, special);
        double reach = special.radius() + SEARCH_MARGIN;
        AABB search = new AABB(center.x - reach, center.y - SpecialGeometry.SLAM_BELOW, center.z - reach,
                center.x + reach, center.y + SpecialGeometry.SLAM_HEIGHT, center.z + reach);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target != source && target.isAlive() && !isAlly(source, target));
        int hits = 0;
        for (LivingEntity target : targets) {
            AABB box = target.getBoundingBox();
            if (!SpecialGeometry.inGroundSlam(center.x, center.y, center.z, special.radius(), box.minX, box.minY,
                    box.minZ, box.maxX, box.maxY, box.maxZ)) {
                continue;
            }
            if (!target.hurt(damageSource, damage)) {
                continue;
            }
            hits++;
            Vec3 away = new Vec3(target.getX() - center.x, 0, target.getZ() - center.z);
            if (special.knockback() > 0 && away.lengthSqr() > 1.0E-4) {
                // knockback() aplica a resistencia do alvo (kaiju grande quase nao sai do lugar).
                target.knockback(special.knockback(), -away.x, -away.z);
                target.setDeltaMovement(target.getDeltaMovement().add(0, KNOCK_UP * special.knockback()
                        * (1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)), 0));
                target.hurtMarked = true;
            }
            if (special.staggerTicks() > 0 && target instanceof KaijuEntity kaiju
                    && kaiju.def().map(def -> def.kaijuClass() == KaijuClass.YOJU).orElse(false)) {
                // Como o parry: so Yoju fica atordoado; Honju e numerados aguentam o tranco.
                kaiju.stagger(special.staggerTicks());
            }
        }
        effects(level, special, center, source);
        return hits;
    }

    /** Aliado de quem ataca (soldados, suportes de armadura, jogadores sem PvP): fica fora do golpe. */
    public static boolean isAlly(LivingEntity source, LivingEntity target) {
        if (target instanceof ArmorStand || target instanceof SoldierEntity) {
            return true;
        }
        if (target instanceof Player) {
            return !(source instanceof Player) || !ServerConfig.PVP_ENABLED.get();
        }
        return false;
    }

    private static void effects(ServerLevel level, WeaponDef.Special special, Vec3 center, LivingEntity source) {
        Vec3 look = source.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0, look.z);
        boolean first = true;
        for (AbilityDef.Vfx vfx : special.vfx()) {
            VfxService.play(level, vfx.effect(), center, direction, vfx.intensity(),
                    first ? special.cameraShake() : 0.0F);
            first = false;
        }
        SoundEvent sound = special.sound()
                .map(id -> BuiltInRegistries.SOUND_EVENT.getOptional(id)
                        .orElseGet(() -> SoundEvent.createVariableRangeEvent(id)))
                .orElse(KN8Sounds.AXE_SPECIAL.get());
        level.playSound(null, center.x, center.y, center.z, sound, source instanceof Player ? SoundSource.PLAYERS
                : SoundSource.NEUTRAL, SOUND_VOLUME, 1.0F);
    }
}
