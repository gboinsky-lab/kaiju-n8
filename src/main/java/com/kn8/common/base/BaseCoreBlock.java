package com.kn8.common.base;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Nucleo da Base (PM-8D): colocar funda a base do jogador (uma por dimensao); usar abre a aba Base do menu para
 * construir/demolir modulos nos lotes em volta; quebrar acaba com a base (os modulos ficam no mundo).
 */
public class BaseCoreBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<BaseCoreBlock> CODEC = simpleCodec(BaseCoreBlock::new);

    public BaseCoreBlock(Properties properties) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player
                && BaseBuilder.found(player, pos) == BaseBuilder.Result.ALREADY_HAS_BASE) {
            // Um nucleo por jogador: o segundo volta para o inventario.
            level.destroyBlock(pos, !player.getAbilities().instabuild);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            BaseBuilder.use(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            BaseBuilder.lost(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
