// src/main/java/com/kn8/common/data/def/KaijuClass.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Classe de ameaca do GDD (secao 11). */
public enum KaijuClass implements StringRepresentable {
    YOJU("yoju"),
    HONJU("honju"),
    DAIKAIJU("daikaiju"),
    NUMBERED("numbered");

    public static final Codec<KaijuClass> CODEC = StringRepresentable.fromEnum(KaijuClass::values);

    private final String serializedName;

    KaijuClass(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
