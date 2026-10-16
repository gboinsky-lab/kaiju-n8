// src/main/java/com/kn8/common/world/KN8Worldgen.java
package com.kn8.common.world;

import com.kn8.KN8Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tipos de worldgen do mod (construcoes C3): estrutura com checagem de terreno e longe de vilas. */
public final class KN8Worldgen {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, KN8Constants.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<CheckedJigsawStructure>> CHECKED_JIGSAW =
            STRUCTURE_TYPES.register("checked_jigsaw", () -> () -> CheckedJigsawStructure.CODEC);

    private KN8Worldgen() {
    }
}
