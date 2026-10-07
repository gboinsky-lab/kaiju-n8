// src/main/java/com/kn8/common/data/def/AbilityDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Habilidade de kaiju ou jogador ({@code data/<ns>/kn8/ability/<id>.json}). O dano acontece no servidor no tick
 * {@code windup_ticks} (fim do telegraph), nunca pela animacao (regra 7).
 *
 * <p>0.1-B: no mesmo tick de impacto tambem saem a destruicao ({@code destruction}), os efeitos visuais
 * ({@code vfx}), o tremor de camera ({@code camera_shake}) e o som ({@code sound}) — todos opcionais.</p>
 *
 * <p>0.6: {@code particles} e o efeito de aviso (telegraph) tocado no INICIO do preparo, para o jogador reagir
 * (ex.: {@code kn8:energy_charge} na boca antes do raio). {@code behavior} traz quando usar (distancia, prioridade,
 * vida) e os numeros dos tipos novos: {@code kn8:sweep}, {@code kn8:projectile}, {@code kn8:leap},
 * {@code kn8:multi_hit}.</p>
 */
public record AbilityDef(ResourceLocation type, float damageMultiplier, float radius, int windupTicks,
        int activeTicks, int cooldownTicks, Cost cost, String animation, Optional<String> telegraph,
        Optional<ResourceLocation> particles, boolean heavy, Optional<Destruction> destruction, List<Vfx> vfx,
        float cameraShake, Optional<ResourceLocation> sound, Behavior behavior) {

    /**
     * Quando e como a habilidade e usada (0.6, especificacao do Miguel secoes 9 e 10).
     *
     * <ul>
     *   <li>{@code min_range}/{@code max_range}: distancia entre as bordas em que pode comecar ({@code max_range}
     *   negativo = alcance de corpo a corpo + {@code radius}); tipos a distancia exigem linha de visao.</li>
     *   <li>{@code priority}: entre as prontas e ao alcance, a maior vence (empate: sorteio).</li>
     *   <li>{@code health_below}: so com a vida abaixo dessa fracao (1 = sempre).</li>
     *   <li>{@code arc_degrees}/{@code arc_center}: setor do {@code kn8:sweep} (0 = frente, 180 = atras).</li>
     *   <li>{@code hits}/{@code hit_interval}: golpes do {@code kn8:multi_hit}.</li>
     *   <li>{@code projectile_speed}/{@code projectile_range}/{@code explosion_radius}/{@code color}: projetil.</li>
     *   <li>{@code slow_ticks}/{@code slow_level}: lentidao em quem for atingido (teia).</li>
     * </ul>
     */
    public record Behavior(float minRange, float maxRange, int priority, float healthBelow, float arcDegrees,
            float arcCenter, int hits, int hitInterval, float projectileSpeed, float projectileRange,
            float explosionRadius, int color, int slowTicks, int slowLevel) {
        public static final Behavior DEFAULT = new Behavior(0.0F, -1.0F, 0, 1.0F, 360.0F, 0.0F, 1, 4, 1.2F, 32.0F,
                0.0F, 0xFFFFFF, 0, 0);
        public static final Codec<Behavior> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 128.0F).optionalFieldOf("min_range", 0.0F).forGetter(Behavior::minRange),
                Codec.floatRange(-1.0F, 128.0F).optionalFieldOf("max_range", -1.0F).forGetter(Behavior::maxRange),
                Codec.intRange(-100, 100).optionalFieldOf("priority", 0).forGetter(Behavior::priority),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("health_below", 1.0F).forGetter(Behavior::healthBelow),
                Codec.floatRange(1.0F, 360.0F).optionalFieldOf("arc_degrees", 360.0F).forGetter(Behavior::arcDegrees),
                Codec.floatRange(-180.0F, 180.0F).optionalFieldOf("arc_center", 0.0F).forGetter(Behavior::arcCenter),
                Codec.intRange(1, 32).optionalFieldOf("hits", 1).forGetter(Behavior::hits),
                Codec.intRange(1, 40).optionalFieldOf("hit_interval", 4).forGetter(Behavior::hitInterval),
                Codec.floatRange(0.1F, 5.0F).optionalFieldOf("projectile_speed", 1.2F)
                        .forGetter(Behavior::projectileSpeed),
                Codec.floatRange(1.0F, 128.0F).optionalFieldOf("projectile_range", 32.0F)
                        .forGetter(Behavior::projectileRange),
                Codec.floatRange(0.0F, 16.0F).optionalFieldOf("explosion_radius", 0.0F)
                        .forGetter(Behavior::explosionRadius),
                AuraDef.COLOR.optionalFieldOf("color", 0xFFFFFF).forGetter(Behavior::color),
                Codec.intRange(0, 1200).optionalFieldOf("slow_ticks", 0).forGetter(Behavior::slowTicks),
                Codec.intRange(0, 5).optionalFieldOf("slow_level", 0).forGetter(Behavior::slowLevel)
        ).apply(i, Behavior::new));
    }

    /** Destruicao do ambiente no impacto (Etapa E): raio, forca 1-4, se abre cratera e a profundidade dela. */
    public record Destruction(float radius, int power, boolean crater, float depth) {
        public static final Codec<Destruction> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 32.0F).fieldOf("radius").forGetter(Destruction::radius),
                Codec.intRange(1, 4).fieldOf("power").forGetter(Destruction::power),
                Codec.BOOL.optionalFieldOf("crater", false).forGetter(Destruction::crater),
                Codec.floatRange(0.0F, 16.0F).optionalFieldOf("depth", 1.0F).forGetter(Destruction::depth)
        ).apply(i, Destruction::new));
    }

    /** Efeito visual no impacto (Etapa D): id do efeito ({@code kn8:shockwave}...) e intensidade. */
    public record Vfx(ResourceLocation effect, float intensity) {
        public static final Codec<Vfx> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Vfx::effect),
                Codec.floatRange(0.0F, 10.0F).optionalFieldOf("intensity", 1.0F).forGetter(Vfx::intensity)
        ).apply(i, Vfx::new));
    }

    /** Custo para quem usa (jogador ou kaiju com recursos). */
    public record Cost(float stamina, float energy, float heat) {
        public static final Cost FREE = new Cost(0, 0, 0);
        public static final Codec<Cost> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("stamina", 0.0F).forGetter(Cost::stamina),
                Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("energy", 0.0F).forGetter(Cost::energy),
                Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("heat", 0.0F).forGetter(Cost::heat)
        ).apply(i, Cost::new));
    }

    public static final Codec<AbilityDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(AbilityDef::type),
            DefCodecs.MULTIPLIER.fieldOf("damage_multiplier").forGetter(AbilityDef::damageMultiplier),
            Codec.floatRange(0.0F, 64.0F).optionalFieldOf("radius", 0.0F).forGetter(AbilityDef::radius),
            DefCodecs.TICKS.fieldOf("windup_ticks").forGetter(AbilityDef::windupTicks),
            DefCodecs.TICKS.optionalFieldOf("active_ticks", 1).forGetter(AbilityDef::activeTicks),
            DefCodecs.TICKS.fieldOf("cooldown_ticks").forGetter(AbilityDef::cooldownTicks),
            Cost.CODEC.optionalFieldOf("cost", Cost.FREE).forGetter(AbilityDef::cost),
            Codec.STRING.fieldOf("animation").forGetter(AbilityDef::animation),
            Codec.STRING.optionalFieldOf("telegraph").forGetter(AbilityDef::telegraph),
            ResourceLocation.CODEC.optionalFieldOf("particles").forGetter(AbilityDef::particles),
            Codec.BOOL.optionalFieldOf("heavy", false).forGetter(AbilityDef::heavy),
            Destruction.CODEC.optionalFieldOf("destruction").forGetter(AbilityDef::destruction),
            Vfx.CODEC.listOf().optionalFieldOf("vfx", List.of()).forGetter(AbilityDef::vfx),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("camera_shake", 0.0F).forGetter(AbilityDef::cameraShake),
            ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(AbilityDef::sound),
            Behavior.CODEC.optionalFieldOf("behavior", Behavior.DEFAULT).forGetter(AbilityDef::behavior)
    ).apply(i, AbilityDef::new));
}
