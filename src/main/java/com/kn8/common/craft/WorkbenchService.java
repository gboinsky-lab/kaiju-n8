package com.kn8.common.craft;

import java.util.Optional;

import com.kn8.common.career.CareerService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WorkbenchRecipeDef;
import com.kn8.common.registry.KN8Blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Fabricacao na bancada da Forca de Defesa (0.2, Etapa 3). O cliente so pede "fabricar X"; aqui o servidor confere
 * a bancada perto, a patente e os ingredientes no inventario, tira os ingredientes e entrega o resultado.
 */
public final class WorkbenchService {

    /** Distancia (blocos) da bancada para poder fabricar. */
    private static final int REACH = 6;

    private WorkbenchService() {
    }

    public enum Result {
        OK, UNKNOWN, NO_WORKBENCH, LOCKED, MISSING
    }

    public static Result craft(ServerPlayer player, ResourceLocation recipeId) {
        Optional<WorkbenchRecipeDef> recipe = KN8Data.WORKBENCH.get(recipeId, false);
        Result result = recipe.isEmpty() ? Result.UNKNOWN : check(player, recipe.get());
        if (result != Result.OK) {
            player.displayClientMessage(Component.translatable("kn8.workbench.refused." + result.name()
                    .toLowerCase()).withStyle(ChatFormatting.RED), true);
            return result;
        }
        WorkbenchRecipeDef def = recipe.get();
        for (WorkbenchRecipeDef.Ingredient ingredient : def.ingredients()) {
            remove(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item()), ingredient.count());
        }
        ItemStack made = new ItemStack(BuiltInRegistries.ITEM.get(def.result()), def.count());
        Component name = made.getHoverName();
        if (!player.getInventory().add(made)) {
            player.drop(made, false);
        }
        player.displayClientMessage(Component.translatable("kn8.workbench.crafted", def.count(), name)
                .withStyle(ChatFormatting.GREEN), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS,
                1.0F, 1.0F);
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getEyeY(),
                player.getZ(), 12, 0.4, 0.3, 0.4, 0.05);
        return Result.OK;
    }

    static Result check(ServerPlayer player, WorkbenchRecipeDef recipe) {
        if (!nearWorkbench(player)) {
            return Result.NO_WORKBENCH;
        }
        if (!CareerService.isUnlocked(player, recipe.unlockId())) {
            return Result.LOCKED;
        }
        for (WorkbenchRecipeDef.Ingredient ingredient : recipe.ingredients()) {
            if (count(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item())) < ingredient.count()) {
                return Result.MISSING;
            }
        }
        return Result.OK;
    }

    private static boolean nearWorkbench(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-REACH, -REACH, -REACH),
                center.offset(REACH, REACH, REACH))) {
            if (player.level().getBlockState(pos).is(KN8Blocks.DEFENSE_WORKBENCH.get())) {
                return true;
            }
        }
        return false;
    }

    public static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void remove(Inventory inventory, Item item, int amount) {
        int left = amount;
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
        inventory.setChanged();
    }
}
