// src/main/java/com/kn8/common/data/def/Rarity.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Raridade de aparicao de um kaiju (GDD secao 10). */
public enum Rarity implements StringRepresentable {
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    EVENT("event");

    public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);

    private final String serializedName;

    Rarity(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
