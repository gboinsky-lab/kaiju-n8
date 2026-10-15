// src/main/java/com/kn8/common/arena/TrainingConsoleBlock.java
package com.kn8.common.arena;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Painel da arena de treino (construcoes C2, Miguel: "zona de treino com a possibilidade de enfrentar kaijus"). A
 * frente aponta para a arena: o centro fica {@code [trainingArena] distance} blocos a frente. Usar solta o kaiju
 * escolhido (ou encerra o treino em andamento); Shift + usar troca o kaiju. A escolha fica no proprio estado do bloco
 * ({@link #SELECTION}), sem block entity (assim o painel gira junto com a estrutura).
 */
public class TrainingConsoleBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<TrainingConsoleBlock> CODEC = simpleCodec(TrainingConsoleBlock::new);
    /** Indice na lista {@code [trainingArena] kaiju} (ate 16 especies). */
    public static final IntegerProperty SELECTION = IntegerProperty.create("selection", 0, 15);

    public TrainingConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SELECTION, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SELECTION);
    }

    /** Quem coloca olha para a arena: a frente do painel aponta para onde o jogador olha. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                TrainingArenaService.cycle(serverLevel, pos, state, serverPlayer);
            } else {
                TrainingArenaService.use(serverLevel, pos, state, serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
