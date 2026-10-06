// src/main/java/com/kn8/common/data/def/DefCodecs.java
package com.kn8.common.data.def;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

/** Codecs reaproveitados pelas definicoes de dados. */
final class DefCodecs {

    static final Codec<List<ResourceLocation>> IDS = ResourceLocation.CODEC.listOf();
    static final Codec<List<String>> STRINGS = Codec.STRING.listOf();
    /** Tempo em ticks: ate 1 hora de jogo. */
    static final Codec<Integer> TICKS = Codec.intRange(0, 72_000);
    static final Codec<Float> MULTIPLIER = Codec.floatRange(0.0F, 100.0F);

    private DefCodecs() {
    }
}
