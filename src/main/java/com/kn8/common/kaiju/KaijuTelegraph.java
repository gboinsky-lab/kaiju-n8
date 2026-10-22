// src/main/java/com/kn8/common/kaiju/KaijuTelegraph.java
package com.kn8.common.kaiju;

import java.util.Locale;
import java.util.Optional;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.kaiju.AbilityGeometry;
import com.kn8.core.kaiju.DangerZone;
import com.kn8.core.kaiju.TelegraphShapes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Aviso de golpe pesado (IA-6, Parte 12 do plano de IA): durante o preparo de toda habilidade {@code heavy}, o
 * servidor marca no chao a area ameacada (pisao: anel no ponto do impacto; soco e varredura: arco a frente;
 * investida e tiro: linha ate o alvo; salto: anel onde o alvo esta), em pulsos de {@code [kaijuAi]
 * telegraphIntervalTicks}, amarelo no comeco e vermelho perto do impacto. VFX-R2: desenhado no cliente (um evento
 * por pulso, sem particulas). Um som grave sai no inicio do preparo.
 */
final class KaijuTelegraph {

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

    /**
     * Um pulso do aviso; {@code ticksLeft} ate o impacto. VFX-R2 (Miguel: sem particulas): a area do golpe vai ao
     * cliente como um evento ({@code kn8:telegraph}) e e desenhada inteira no chao (preenchida de leve, borda forte),
     * na cor do momento; cada pulso dura ate o seguinte.
     */
    static void pulse(KaijuEntity kaiju, AbilityDef ability, LivingEntity target, int ticksLeft, int windup) {
        int interval = ServerConfig.AI_TELEGRAPH_INTERVAL.get();
        if (!applies(ability) || !(kaiju.level() instanceof ServerLevel level) || ticksLeft % interval != 0) {
            return;
        }
        Optional<DangerZone> found = zone(kaiju, ability, target);
        if (found.isEmpty()) {
            return;
        }
        DangerZone zone = found.get();
        double y = (KaijuAbilities.TYPE_LEAP.equals(ability.type().getPath()) && target != null ? target.getY()
                : kaiju.getY()) + GROUND_OFFSET;
        float[] rgb = TelegraphShapes.color(ticksLeft, windup);
        String color = String.format(Locale.ROOT, "#%02x%02x%02x", Math.round(rgb[0] * 255), Math.round(rgb[1] * 255),
                Math.round(rgb[2] * 255));
        String kind;
        Vec3 shape;
        switch (zone.kind()) {
            case SECTOR -> {
                kind = "sector";
                shape = new Vec3(zone.center(), zone.width(), 0.0);
            }
            case LANE -> {
                kind = "lane";
                shape = new Vec3(zone.x1(), zone.z1(), zone.halfWidth());
            }
            default -> {
                kind = "disc";
                shape = Vec3.ZERO;
            }
        }
        VfxService.keyed(level, VfxService.TELEGRAPH, new Vec3(zone.cx(), y, zone.cz()), shape,
                (float) zone.radius(), kind + color + "@" + interval, 0.0F);
    }
}
