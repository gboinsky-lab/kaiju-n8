// src/main/java/com/kn8/common/registry/KN8Blocks.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;

import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocos do mod. Vazio no M1; o primeiro bloco (kn8:marker) entra no M17. */
public final class KN8Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KN8Constants.MOD_ID);

    private KN8Blocks() {
    }
}
