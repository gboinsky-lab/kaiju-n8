// src/main/java/com/kn8/common/data/def/LocomotionDef.java
package com.kn8.common.data.def;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Perfil de locomocao de uma criatura ({@code data/<ns>/kn8/locomotion/<id>.json}, 0.5.0-C, Biblioteca v21
 * Prioridade 2: "locomotion profiles diferentes por personagem/criatura"). O id e o do kaiju ({@code kaiju/<id>})
 * ou o do tipo de entidade (soldados).
 *
 * <ul>
 *   <li>{@code stride}: blocos andados em uma volta inteira da animacao de andar (dois passos). A animacao toca na
 *   velocidade em que os pes acompanham o chao: criatura grande de passada longa anda devagar e pesada, a pequena
 *   mexe as pernas mais rapido; entre {@code min_animation_speed} e {@code max_animation_speed};</li>
 *   <li>{@code step_distance}: blocos entre um som de passo e outro (padrao: metade da passada, um por pe);</li>
 *   <li>{@code step_dust}: particulas do bloco do chao a cada passo (massa: so nos grandes).</li>
 * </ul>
 */
public record LocomotionDef(float stride, float minAnimationSpeed, float maxAnimationSpeed,
        Optional<Float> stepDistance, int stepDust) {

    public static final Codec<LocomotionDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.1F, 64.0F).fieldOf("stride").forGetter(LocomotionDef::stride),
            Codec.floatRange(0.05F, 4.0F).optionalFieldOf("min_animation_speed", 0.35F)
                    .forGetter(LocomotionDef::minAnimationSpeed),
            Codec.floatRange(0.05F, 8.0F).optionalFieldOf("max_animation_speed", 2.5F)
                    .forGetter(LocomotionDef::maxAnimationSpeed),
            Codec.floatRange(0.1F, 64.0F).optionalFieldOf("step_distance").forGetter(LocomotionDef::stepDistance),
            Codec.intRange(0, 64).optionalFieldOf("step_dust", 0).forGetter(LocomotionDef::stepDust)
    ).apply(i, LocomotionDef::new));

    /** Blocos entre dois passos (um por pe). */
    public float stepEvery() {
        return stepDistance.orElse(stride / 2.0F);
    }
}
