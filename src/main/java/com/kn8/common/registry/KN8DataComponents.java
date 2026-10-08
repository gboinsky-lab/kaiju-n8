// src/main/java/com/kn8/common/registry/KN8DataComponents.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data Components (estado de item). */
public final class KN8DataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, KN8Constants.MOD_ID);

    /**
     * 0.5.0-D2: tiros no item (no pente avulso e no pente que esta dentro da arma de fogo). Salvo com o item e
     * enviado ao cliente (HUD e barra do pente). Arma sem o componente = veio de fabrica com o pente cheio.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROUNDS =
            DATA_COMPONENTS.registerComponentType("rounds", builder -> builder
                    .persistent(Codec.intRange(0, 10_000)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private KN8DataComponents() {
    }
}
