// src/main/java/com/kn8/common/kaiju/KaijuAbilities.java
package com.kn8.common.kaiju;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.vfx.AbilityEffects;
import com.kn8.core.kaiju.AbilitySelection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Habilidades novas dos kaiju (0.6, especificacao do Miguel): escolha por distancia/prioridade/vida e os tipos
 * {@code kn8:sweep}, {@code kn8:projectile}, {@code kn8:leap} e {@code kn8:multi_hit}. Fica fora do
 * {@link KaijuEntity} (secao 18 da especificacao: logica de habilidade em classe propria); o kaiju guarda so o estado
 * do golpe em andamento.
 */
final class KaijuAbilities {

    static final String TYPE_SWEEP = "sweep";
    static final String TYPE_PROJECTILE = "projectile";
    static final String TYPE_LEAP = "leap";
    static final String TYPE_MULTI_HIT = "multi_hit";
    /** Tempo de voo do salto (ticks): define a parabola ate o alvo. */
    private static final int LEAP_FLIGHT_TICKS = 16;
    /** Fisica vanilla no ar: gravidade e freio vertical/horizontal por tick. */
    private static final double GRAVITY = 0.08;
    private static final double VERTICAL_DRAG = 0.98;
    private static final double AIR_DRAG = 0.91;
    /** Folga no alcance dos golpes seguintes do combo (o alvo recua um pouco entre eles). */
    private static final double MULTI_HIT_REACH_TOLERANCE = 1.5;

    private KaijuAbilities() {
    }

    /** Tipos que agem a distancia: precisam de linha de visao e usam {@code min_range}/{@code max_range}. */
    static boolean isRanged(AbilityDef ability) {
        String type = ability.type().getPath();
        return TYPE_PROJECTILE.equals(type) || TYPE_LEAP.equals(type);
    }

    /**
     * Escolhe a habilidade a usar agora: prontas, com a vida dentro de {@code health_below}, ao alcance (corpo a
     * corpo: entre as bordas; a distancia: entre {@code min_range} e {@code max_range} com linha de visao). A de maior
     * prioridade vence; empate, sorteio.
     */
    static Optional<ResourceLocation> choose(KaijuEntity kaiju, LivingEntity target, List<ResourceLocation> ids,
            Map<ResourceLocation, Long> readyAt, long now) {
        double edge = kaiju.edgeDistance(target);
        float health = kaiju.getHealth() / Math.max(1.0F, kaiju.getMaxHealth());
        List<ResourceLocation> candidates = new ArrayList<>();
        List<Integer> priorities = new ArrayList<>();
        Boolean sight = null;
        for (ResourceLocation id : ids) {
            Optional<AbilityDef> found = KN8Data.ABILITY.get(id, false);
            if (found.isEmpty() || now < readyAt.getOrDefault(id, Long.MIN_VALUE)) {
                continue;
            }
            AbilityDef ability = found.get();
            AbilityDef.Behavior behavior = ability.behavior();
            if (health > behavior.healthBelow()) {
                continue;
            }
            double max = behavior.maxRange() >= 0 ? behavior.maxRange() : kaiju.reachOf(ability);
            if (!AbilitySelection.inRange(edge, behavior.minRange(), max)) {
                continue;
            }
            if (isRanged(ability)) {
                if (sight == null) {
                    sight = kaiju.hasLineOfSight(target);
                }
                if (!sight) {
                    continue;
                }
            } else if (!kaiju.isInReach(target, max)) {
                continue;
            } else if (TYPE_SWEEP.equals(ability.type().getPath()) && !inSector(kaiju, target, behavior)) {
                // Rabada/varredura so com o alvo no setor dela (senao o golpe sairia no vazio).
                continue;
            }
            candidates.add(id);
            priorities.add(behavior.priority());
        }
        int index = AbilitySelection.pick(priorities, kaiju.getRandom()::nextInt);
        return index < 0 ? Optional.empty() : Optional.of(candidates.get(index));
    }

    // --- tipos ---------------------------------------------------------------------------------------------------

    /**
     * {@code kn8:sweep} (cauda, pernas): todos ao alcance dentro do setor {@code arc_degrees} centrado em
     * {@code arc_center} graus da frente do corpo; empurra para longe do kaiju.
     */
    static void sweep(KaijuEntity kaiju, AbilityDef ability) {
        AbilityDef.Behavior behavior = ability.behavior();
        double reach = behavior.maxRange() >= 0 ? behavior.maxRange() : kaiju.reachOf(ability);
        Vec3 forward = kaiju.bodyForward();
        for (LivingEntity target : kaiju.level().getEntitiesOfClass(LivingEntity.class,
                kaiju.getBoundingBox().inflate(reach, reach * 0.5, reach))) {
            if (target == kaiju || target instanceof KaijuEntity || !target.isAlive()
                    || !kaiju.isInReach(target, reach) || !inSector(kaiju, target, behavior)) {
                continue;
            }
            kaiju.dealAbilityDamage(target, ability, new Vec3(target.getX() - kaiju.getX(), 0,
                    target.getZ() - kaiju.getZ()));
        }
        double behind = Math.toRadians(behavior.arcCenter());
        Vec3 side = new Vec3(forward.x * Math.cos(behind) - forward.z * Math.sin(behind), 0,
                forward.z * Math.cos(behind) + forward.x * Math.sin(behind));
        AbilityEffects.play(kaiju, ability, kaiju.position().add(side.scale(kaiju.getBbWidth() * 0.5 + 1.0))
                .add(0, kaiju.getBbHeight() * 0.3, 0));
    }

    /** O alvo esta no setor ({@code arc_degrees} em volta de {@code arc_center}) da frente do corpo? */
    static boolean inSector(KaijuEntity kaiju, LivingEntity target, AbilityDef.Behavior behavior) {
        Vec3 forward = kaiju.bodyForward();
        float bodyYaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float toTarget = (float) Math.toDegrees(Math.atan2(-(target.getX() - kaiju.getX()),
                target.getZ() - kaiju.getZ()));
        return AbilitySelection.inArc(Mth.wrapDegrees(toTarget - bodyYaw), behavior.arcCenter(),
                behavior.arcDegrees());
    }

    /** {@code kn8:projectile}: dispara da boca (frente do corpo, 3/4 da altura) no centro do alvo. */
    static void fire(KaijuEntity kaiju, ResourceLocation id, AbilityDef ability, LivingEntity target) {
        Vec3 mouth = mouth(kaiju);
        Vec3 aim = target != null && target.isAlive()
                ? target.getBoundingBox().getCenter().subtract(mouth) : kaiju.bodyForward();
        float damage = (float) (kaiju.getAttributeValue(Attributes.ATTACK_DAMAGE) * ability.damageMultiplier());
        KaijuProjectile.shoot(kaiju, id, ability, mouth, aim, damage);
    }

    /** Ponto de onde saem projeteis e avisos: borda da frente do corpo, a 3/4 da altura. */
    static Vec3 mouth(KaijuEntity kaiju) {
        return kaiju.position().add(kaiju.bodyForward().scale(kaiju.getBbWidth() * 0.5))
                .add(0, kaiju.getBbHeight() * 0.75, 0);
    }

    /** {@code kn8:leap}: pula numa parabola ate o alvo; o dano em area sai na aterrissagem ({@link #land}). */
    static void leap(KaijuEntity kaiju, LivingEntity target) {
        Vec3 to = target != null && target.isAlive() ? target.position()
                : kaiju.position().add(kaiju.bodyForward().scale(6.0));
        // Cai com a borda da frente encostando no alvo (nao em cima do centro dele).
        Vec3 flat = new Vec3(to.x - kaiju.getX(), 0, to.z - kaiju.getZ());
        double distance = Math.max(0.0, flat.length() - kaiju.getBbWidth() * 0.5);
        Vec3 dir = flat.lengthSqr() < 1.0E-4 ? kaiju.bodyForward() : flat.normalize();
        double speed = AbilitySelection.leapHorizontalSpeed(distance, LEAP_FLIGHT_TICKS, AIR_DRAG);
        double vy = AbilitySelection.leapVerticalSpeed(to.y - kaiju.getY(), LEAP_FLIGHT_TICKS, GRAVITY,
                VERTICAL_DRAG);
        kaiju.setDeltaMovement(dir.x * speed, Math.max(0.4, vy), dir.z * speed);
        kaiju.hurtMarked = true;
    }

    /** Aterrissagem do salto: golpe em area (raio = {@code radius}) em volta do kaiju. */
    static void land(KaijuEntity kaiju, AbilityDef ability) {
        double radius = kaiju.reachOf(ability);
        for (LivingEntity target : kaiju.level().getEntitiesOfClass(LivingEntity.class,
                kaiju.getBoundingBox().inflate(radius, 1.0, radius))) {
            if (target != kaiju && !(target instanceof KaijuEntity) && target.isAlive()
                    && kaiju.isInReach(target, radius)) {
                kaiju.dealAbilityDamage(target, ability, new Vec3(target.getX() - kaiju.getX(), 0,
                        target.getZ() - kaiju.getZ()));
            }
        }
        AbilityEffects.play(kaiju, ability, kaiju.position());
    }

    /** Um golpe do {@code kn8:multi_hit} (o alvo do golpe, se ainda estiver ao alcance). */
    static void multiHit(KaijuEntity kaiju, AbilityDef ability, LivingEntity target) {
        if (target == null || !target.isAlive()
                || !kaiju.isInReach(target, kaiju.reachOf(ability) * MULTI_HIT_REACH_TOLERANCE)) {
            return;
        }
        // Golpes a cada poucos ticks: sem isso a invulnerabilidade vanilla (10 ticks) engolia o combo.
        target.invulnerableTime = 0;
        kaiju.dealAbilityDamage(target, ability, null);
        AbilityEffects.play(kaiju, ability, target.position().add(0, target.getBbHeight() / 2, 0));
    }
}
