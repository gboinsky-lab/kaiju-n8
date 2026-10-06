// src/main/java/com/kn8/common/data/def/KaijuSize.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Classe de tamanho do GDD (secao 10) e da Fase 4 (secao 7.1). */
public enum KaijuSize implements StringRepresentable {
    MINIATURE("miniature"),
    MEDIUM("medium"),
    LARGE("large"),
    GIANT("giant"),
    SUPERGIANT("supergiant");

    public static final Codec<KaijuSize> CODEC = StringRepresentable.fromEnum(KaijuSize::values);

    private final String serializedName;

    KaijuSize(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
