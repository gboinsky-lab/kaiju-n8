package com.kn8.common.training;

import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Coloca o boneco de treino no bloco clicado, virado para o jogador. */
public class TrainingDummyItem extends Item {

    public TrainingDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        TrainingDummyEntity dummy = KN8Entities.TRAINING_DUMMY.get().create(level);
        if (dummy == null || !level.noCollision(dummy, new AABB(pos).expandTowards(0, 1, 0))) {
            return InteractionResult.FAIL;
        }
        float yaw = context.getPlayer() == null ? 0.0F
                : Mth.wrapDegrees(context.getPlayer().getYRot() + 180.0F);
        dummy.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Math.round(yaw / 45.0F) * 45.0F, 0.0F);
        level.addFreshEntity(dummy);
        level.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
