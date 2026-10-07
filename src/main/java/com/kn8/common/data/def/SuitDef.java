// src/main/java/com/kn8/common/data/def/SuitDef.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Traje de combate ({@code data/<ns>/kn8/suit/<id do item>.json}), GDD secoes 26 e 34. A patente que libera o traje
 * fica so nos {@code unlocks} das patentes (0.3).
 */
public record SuitDef(float armor, float toughness, int releaseCapBonus, float heatResistance) {

    public static final Codec<SuitDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.0F, 30.0F).fieldOf("armor").forGetter(SuitDef::armor),
            Codec.floatRange(0.0F, 20.0F).optionalFieldOf("toughness", 0.0F).forGetter(SuitDef::toughness),
            Codec.intRange(0, 100).optionalFieldOf("release_cap_bonus", 0).forGetter(SuitDef::releaseCapBonus),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("heat_resistance", 0.0F).forGetter(SuitDef::heatResistance)
    ).apply(i, SuitDef::new));
}
