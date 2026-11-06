// src/main/java/com/kn8/common/combat/HitEffects.java
package com.kn8.common.combat;

import com.kn8.common.data.def.HitEffect;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Etapa 7-B: aplica o {@link HitEffect} de uma arma ou tecnica em quem levou o golpe (servidor, depois do dano
 * aceito). Serve jogador ({@code CombatService}), golpe dos especiais ({@code HoshinaEntity.strike}) e tiro deles
 * ({@code SlashProjectile}).
 */
public final class HitEffects {

    /** Cor da marca no alvo (laranja de aviso: a marca e para os aliados verem). */
    private static final String MARK_COLOR = "#FFB040";

    private HitEffects() {
    }

    public static void apply(LivingEntity target, HitEffect effect, Entity attacker) {
        if (effect == null || effect.isEmpty() || target.level().isClientSide() || !target.isAlive()) {
            return;
        }
        if (effect.slowTicks() > 0) {
            // Sem particulas (decisao do Miguel, 2026-10-22: nada de particula vanilla).
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effect.slowTicks(),
                    effect.slowLevel() - 1, false, false, true), attacker);
        }
        if (effect.poisonTicks() > 0 && effect.poisonPerSecond() > 0.0F) {
            if (target instanceof KaijuEntity kaiju) {
                kaiju.poison(effect.poisonPerSecond(), effect.poisonTicks(), attacker);
                if (attacker instanceof net.minecraft.world.entity.Mob mob) {
                    com.kn8.common.ai.AiTuning.logMove(mob, "veneno no kaiju: " + effect.poisonPerSecond() + "/s");
                }
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, effect.poisonTicks(), 0, false, false, true),
                        attacker);
            }
        }
        if (target instanceof KaijuEntity kaiju) {
            boolean marked = false;
            if (effect.markTicks() > 0 && effect.markBonus() > 0.0F) {
                kaiju.markVulnerable(effect.markTicks(), 1.0F + effect.markBonus());
                marked = true;
            }
            if (effect.nextHitBonus() > 0.0F) {
                kaiju.markNextHit(effect.nextHitBonus(), effect.nextHitTicks());
                marked = true;
            }
            if (marked && attacker instanceof net.minecraft.world.entity.Mob mob) {
                com.kn8.common.ai.AiTuning.logMove(mob, "marca no kaiju: x" + kaiju.vulnerability()
                        + (kaiju.hasNextHitMark() ? " e proximo golpe" : ""));
            }
            if (marked) {
                // Anel no chao do kaiju marcado (o mesmo desenho do aviso, na cor da marca), por um segundo.
                VfxService.keyed((ServerLevel) target.level(), VfxService.TELEGRAPH, target.position(), Vec3.ZERO,
                        Math.max(1.0F, target.getBbWidth() * 0.6F), "disc" + MARK_COLOR + "@20", 0.0F);
            }
        }
    }
}
