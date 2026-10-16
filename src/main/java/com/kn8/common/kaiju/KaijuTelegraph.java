// src/main/java/com/kn8/common/kaiju/KaijuTelegraph.java
package com.kn8.common.kaiju;

import java.util.Optional;

import org.joml.Vector3f;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.core.kaiju.AbilityGeometry;
import com.kn8.core.kaiju.DangerZone;
import com.kn8.core.kaiju.TelegraphShapes;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Aviso de golpe pesado (IA-6, Parte 12 do plano de IA): durante o preparo de toda habilidade {@code heavy}, o
 * servidor marca no chao a area ameacada (pisao: anel no ponto do impacto; soco e varredura: arco a frente;
 * investida e tiro: linha ate o alvo; salto: anel onde o alvo esta), em pulsos de {@code [kaijuAi]
 * telegraphIntervalTicks}, amarelo no comeco e vermelho perto do impacto. Particulas comuns: todo cliente ve, sem
 * pacote novo. Um som grave sai no inicio do preparo.
 */
final class KaijuTelegraph {

    private static final float DUST_SIZE = 1.6F;
    /** Altura dos pontos acima do pe do kaiju (sobre o chao). */
    private static final double GROUND_OFFSET = 0.15;
    /** Largura do arco do soco (radianos, 90 graus). */
    private static final double PUNCH_ARC = Math.PI * 0.5;
    /** Alcance da linha quando o JSON nao da {@code max_range} (investida). */
    private static final double DEFAULT_LINE = 14.0;
    private static final double LEAP_RING_EXTRA = 1.0;
    /** MOV-4: meia largura da faixa de um tiro (blocos). */
    private static final double LANE_HALF_WIDTH = 1.0;

    private KaijuTelegraph() {
    }

    static boolean applies(AbilityDef ability) {
        return ability.heavy() && ServerConfig.SPEC.isLoaded() && ServerConfig.AI_TELEGRAPH_HEAVY.get();
    }

    /** Inicio do preparo: som de aviso. */
    static void start(KaijuEntity kaiju, AbilityDef ability) {
        if (applies(ability) && kaiju.level() instanceof ServerLevel level) {
            level.playSound(null, kaiju.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE,
                    2.0F, 0.6F);
        }
    }

    /**
     * MOV-4: a area do golpe em preparo como area de verdade (a mesma forma que o aviso desenha), para os soldados
     * sairem dela. Vazio sem area (golpe sem aviso, salto sem alvo).
     */
    static Optional<DangerZone> zone(KaijuEntity kaiju, AbilityDef ability, LivingEntity target) {
        Vec3 forward = kaiju.bodyForward();
        double facing = Math.atan2(forward.z, forward.x);
        switch (ability.type().getPath()) {
            case KaijuEntity.TYPE_AREA_MELEE -> {
                Vec3 center = kaiju.position().add(forward.scale(AbilityGeometry.slamCenterForward(
                        kaiju.getBbWidth())));
                return Optional.of(DangerZone.disc(center.x, center.z, ability.radius()));
            }
            case KaijuEntity.TYPE_CHARGE, KaijuAbilities.TYPE_PROJECTILE -> {
                double length = ability.behavior().maxRange() >= 0 ? ability.behavior().maxRange() : DEFAULT_LINE;
                Vec3 to = target != null ? new Vec3(target.getX() - kaiju.getX(), 0, target.getZ() - kaiju.getZ())
                        : forward;
                Vec3 dir = to.lengthSqr() < 1.0E-4 ? forward : to.normalize();
                double start = kaiju.getBbWidth() * 0.5;
                // Investida: o corpo inteiro passa; tiro: a faixa do projetil.
                double half = KaijuEntity.TYPE_CHARGE.equals(ability.type().getPath())
                        ? Math.max(LANE_HALF_WIDTH, kaiju.getBbWidth() * 0.5) : LANE_HALF_WIDTH;
                return Optional.of(DangerZone.lane(kaiju.getX() + dir.x * start, kaiju.getZ() + dir.z * start,
                        kaiju.getX() + dir.x * (start + length), kaiju.getZ() + dir.z * (start + length), half));
            }
            case KaijuAbilities.TYPE_LEAP -> {
                return target == null ? Optional.empty() : Optional.of(DangerZone.disc(target.getX(), target.getZ(),
                        ability.radius() + LEAP_RING_EXTRA));
            }
            case KaijuAbilities.TYPE_SWEEP -> {
                AbilityDef.Behavior behavior = ability.behavior();
                double reach = kaiju.getBbWidth() * 0.5 + (behavior.maxRange() >= 0 ? behavior.maxRange()
                        : kaiju.reachOf(ability));
                return Optional.of(DangerZone.sector(kaiju.getX(), kaiju.getZ(), reach,
                        facing + Math.toRadians(behavior.arcCenter()), Math.toRadians(behavior.arcDegrees())));
            }
            default -> {
                return Optional.of(DangerZone.sector(kaiju.getX(), kaiju.getZ(),
                        kaiju.getBbWidth() * 0.5 + kaiju.reachOf(ability), facing, PUNCH_ARC));
            }
        }
    }

    /** Um pulso do aviso; {@code ticksLeft} ate o impacto. */
    static void pulse(KaijuEntity kaiju, AbilityDef ability, LivingEntity target, int ticksLeft, int windup) {
        if (!applies(ability) || !(kaiju.level() instanceof ServerLevel level)
                || ticksLeft % ServerConfig.AI_TELEGRAPH_INTERVAL.get() != 0) {
            return;
        }
        int points = ServerConfig.AI_TELEGRAPH_POINTS.get();
        Vec3 forward = kaiju.bodyForward();
        double facing = Math.atan2(forward.z, forward.x);
        double[][] shape;
        double y = kaiju.getY();
        switch (ability.type().getPath()) {
            case KaijuEntity.TYPE_AREA_MELEE -> {
                Vec3 center = kaiju.position().add(forward.scale(AbilityGeometry.slamCenterForward(
                        kaiju.getBbWidth())));
                shape = TelegraphShapes.ring(center.x, center.z, ability.radius(), points);
            }
            case KaijuEntity.TYPE_CHARGE, KaijuAbilities.TYPE_PROJECTILE -> {
                double length = ability.behavior().maxRange() >= 0 ? ability.behavior().maxRange() : DEFAULT_LINE;
                Vec3 to = target != null ? new Vec3(target.getX() - kaiju.getX(), 0, target.getZ() - kaiju.getZ())
                        : forward;
                Vec3 dir = to.lengthSqr() < 1.0E-4 ? forward : to.normalize();
                double start = kaiju.getBbWidth() * 0.5;
                shape = TelegraphShapes.line(kaiju.getX() + dir.x * start, kaiju.getZ() + dir.z * start,
                        kaiju.getX() + dir.x * (start + length), kaiju.getZ() + dir.z * (start + length), points);
            }
            case KaijuAbilities.TYPE_LEAP -> {
                if (target == null) {
                    return;
                }
                shape = TelegraphShapes.ring(target.getX(), target.getZ(), ability.radius() + LEAP_RING_EXTRA,
                        points);
                y = target.getY();
            }
            case KaijuAbilities.TYPE_SWEEP -> {
                AbilityDef.Behavior behavior = ability.behavior();
                double reach = kaiju.getBbWidth() * 0.5 + (behavior.maxRange() >= 0 ? behavior.maxRange()
                        : kaiju.reachOf(ability));
                // arc_center: graus a partir da frente, no sentido do yaw do Minecraft (o mesmo do inSector); no
                // angulo atan2(z, x) o yaw so soma.
                shape = TelegraphShapes.arc(kaiju.getX(), kaiju.getZ(), reach,
                        facing + Math.toRadians(behavior.arcCenter()), Math.toRadians(behavior.arcDegrees()), points);
            }
            default -> shape = TelegraphShapes.arc(kaiju.getX(), kaiju.getZ(),
                    kaiju.getBbWidth() * 0.5 + kaiju.reachOf(ability), facing, PUNCH_ARC, points);
        }
        float[] rgb = TelegraphShapes.color(ticksLeft, windup);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(rgb[0], rgb[1], rgb[2]), DUST_SIZE);
        for (double[] point : shape) {
            level.sendParticles(dust, point[0], y + GROUND_OFFSET, point[1], 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
