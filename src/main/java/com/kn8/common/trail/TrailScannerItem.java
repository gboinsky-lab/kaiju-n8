package com.kn8.common.trail;

import com.kn8.common.config.ServerConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rastreador de Kaiju (PM-9): usar investiga as pistas de destruicao perto ({@link KaijuTrailService}); o servidor
 * decide tudo e a espera entre usos e a do proprio item.
 */
public class TrailScannerItem extends Item {

    public TrailScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            KaijuTrailService.investigate(serverPlayer);
            player.getCooldowns().addCooldown(this, ServerConfig.TRAIL_SCAN_COOLDOWN_TICKS.get());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
