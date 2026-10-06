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
 */
public record AbilityDef(ResourceLocation type, float damageMultiplier, float radius, int windupTicks,
        int activeTicks, int cooldownTicks, Cost cost, String animation, Optional<String> telegraph,
        Optional<ResourceLocation> particles, boolean heavy, Optional<Destruction> destruction, List<Vfx> vfx,
        float cameraShake, Optional<ResourceLocation> sound) {

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
            ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(AbilityDef::sound)
    ).apply(i, AbilityDef::new));
}
