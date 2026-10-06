// src/main/java/com/kn8/KN8Constants.java
package com.kn8;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

/**
 * Constantes compartilhadas por todo o mod. Nao guarda estado de jogo: o estado vive em {@code KN8Server}.
 */
public final class KN8Constants {
    public static final String MOD_ID = "kn8";
    public static final Logger LOGGER = LogUtils.getLogger();

    private KN8Constants() {
    }

    /** Cria um ResourceLocation no namespace do mod, ex.: {@code id("primigenius")} -> {@code kn8:primigenius}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
