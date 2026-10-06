// src/main/java/com/kn8/common/registry/KN8DataComponents.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data Components (estado de item). Vazio no M1. */
public final class KN8DataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KN8Constants.MOD_ID);

    private KN8DataComponents() {
    }
}
