// src/main/java/com/kn8/common/combat/FreezeSpread.java
package com.kn8.common.combat;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.server.KN8Server;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.combat.FreezeStages;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * COMBATE-7 (obra, Reno e Numbers 6: o gelo progride pelo corpo do kaiju e bloqueia o movimento, desenhado em
 * etapas): o tiro congelante nao trava o alvo de uma vez. A cada {@code [combat] freezeStageTicks} a Lentidao sobe ate
 * a do tiro ({@link FreezeStages}) e o gelo anda do ponto atingido ao centro do corpo, com a receita
 * {@code freeze/<etapa>} do perfil visual de quem atirou no tamanho do alvo. A paralisia (brecha) so chega na ultima
 * etapa. Tiro com Lentidao abaixo de {@code freezeMinLevel} (supressao) ou de quem nao tem o gelo no perfil visual
 * continua de uma vez.
 */
public final class FreezeSpread {

    /** Receita das etapas no perfil visual de quem atirou ("freeze/1", "freeze/2"...). */
    public static final String RECIPE = "freeze";
    /** Fracao da largura do corpo do centro ate a pele onde o gelo termina. */
    private static final double SKIN = 0.45;

    private FreezeSpread() {
    }

    /** Aplica a Lentidao do tiro em {@code target}, em etapas se for congelante; {@code hit} = onde o tiro bateu. */
    public static void apply(LivingEntity target, Entity owner, Vec3 hit, int slowTicks, int slowLevel) {
        if (slowTicks <= 0) {
            return;
        }
        // So e gelo o tiro de quem tem o gelo no perfil visual (Reno, Numbers 6; o rugido do No. 8 tambem deixa
        // lento e nao congela).
        if (!(target.level() instanceof ServerLevel level) || slowLevel < ServerConfig.FREEZE_MIN_LEVEL.get()
                || owner == null || !VfxService.hasRecipe(owner, RECIPE + "/1")) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowLevel - 1));
            return;
        }
        int stages = FreezeStages.stages(ServerConfig.FREEZE_STAGES.get(), slowLevel);
        int stageTicks = ServerConfig.FREEZE_STAGE_TICKS.get();
        // Ponto atingido guardado em relacao ao alvo: o gelo segue o kaiju que anda.
        Vec3 offset = nearestInside(target.getBoundingBox(), hit).subtract(target.position());
        stage(level, target, owner, offset, 0, stages, stageTicks, slowTicks, slowLevel);
        KN8Server server = KN8Server.get(level.getServer());
        for (int i = 1; i < stages; i++) {
            int index = i;
            server.schedule(i * stageTicks, () -> {
                if (target.isAlive() && !target.isRemoved()) {
                    stage(level, target, owner, offset, index, stages, stageTicks, slowTicks, slowLevel);
                }
            });
        }
    }

    private static void stage(ServerLevel level, LivingEntity target, Entity owner, Vec3 offset, int stage,
            int stages, int stageTicks, int slowTicks, int slowLevel) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                FreezeStages.duration(stage, stageTicks, slowTicks), FreezeStages.level(stage, stages, slowLevel) - 1));
        // O gelo cresce no tamanho de quem congela (um kaiju de 8 m fica coberto, nao so o ponto do tiro).
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 start = target.position().add(offset);
        // Fim do caminho na pele voltada para o ponto atingido, na altura do centro (o centro fica dentro do modelo e
        // esconderia o gelo).
        Vec3 side = new Vec3(start.x - center.x, 0, start.z - center.z);
        Vec3 skin = side.lengthSqr() < 1.0E-6 ? center
                : center.add(side.normalize().scale(target.getBbWidth() * SKIN));
        Vec3 at = start.lerp(skin, FreezeStages.reach(stage, stages));
        float grown = (float) (target.getBbHeight() * (0.35 + 0.65 * (stage + 1) / (double) stages));
        VfxService.play(level, VfxService.TECHNIQUE, at, skin.subtract(start), 1.0F, 0.0F,
                VfxService.profileOf(owner), RECIPE + "/" + Math.min(stage + 1, 3), Math.max(1.0F, grown));
    }

    private static Vec3 nearestInside(AABB box, Vec3 point) {
        return new Vec3(Math.max(box.minX, Math.min(box.maxX, point.x)),
                Math.max(box.minY, Math.min(box.maxY, point.y)), Math.max(box.minZ, Math.min(box.maxZ, point.z)));
    }
}
