package com.kn8.common.training;

import com.kn8.common.config.ServerConfig;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Suporte de pesos (TREINO, Miguel 2026-10-30: forca por exercicio): cada uso e uma repeticao que gasta stamina e
 * treina a forca ({@link WeightTraining}). Fica no patio de treino das bases e sai na bancada.
 */
public class WeightRackBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<WeightRackBlock> CODEC = simpleCodec(WeightRackBlock::new);

    public WeightRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && ServerConfig.SPEC.isLoaded()) {
            WeightTraining.lift(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
