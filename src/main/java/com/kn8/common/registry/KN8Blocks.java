// src/main/java/com/kn8/common/registry/KN8Blocks.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;
import com.kn8.common.arena.TrainingConsoleBlock;
import com.kn8.common.base.BaseCoreBlock;
import com.kn8.common.base.SquadStationBlock;
import com.kn8.common.craft.DefenseWorkbenchBlock;
import com.kn8.common.research.ResearchTerminalBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocos do mod. */
public final class KN8Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KN8Constants.MOD_ID);

    /** 0.2 (Etapa 3): bancada da Forca de Defesa. */
    public static final DeferredBlock<DefenseWorkbenchBlock> DEFENSE_WORKBENCH = BLOCKS.register("defense_workbench",
            () -> new DefenseWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** Construcoes C2: painel da arena de treino (solta kaiju de treino na arena da base grande). */
    public static final DeferredBlock<TrainingConsoleBlock> TRAINING_CONSOLE = BLOCKS.register("training_console",
            () -> new TrainingConsoleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** PM-8: estacao de preparacao do esquadrao (cura e reabastece o esquadrao do jogador). */
    public static final DeferredBlock<SquadStationBlock> SQUAD_STATION = BLOCKS.register("squad_station",
            () -> new SquadStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** PM-8C: terminal de pesquisa do laboratorio (material de kaiju vira receitas e bonus contra kaiju). */
    public static final DeferredBlock<ResearchTerminalBlock> RESEARCH_TERMINAL = BLOCKS.register("research_terminal",
            () -> new ResearchTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** PM-8D: Nucleo da Base (funda a base do jogador e abre a construcao de modulos). */
    public static final DeferredBlock<BaseCoreBlock> BASE_CORE = BLOCKS.register("base_core",
            () -> new BaseCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 8)));

    private KN8Blocks() {
    }
}
