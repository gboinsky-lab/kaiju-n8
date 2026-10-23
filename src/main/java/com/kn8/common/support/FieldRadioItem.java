package com.kn8.common.support;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Radio de Campo (PM-10B): usar chama o apoio selecionado no bloco olhado; agachado, troca entre reconhecimento,
 * suprimentos e bombardeio. O servidor decide tudo ({@link SupportService}).
 */
public class FieldRadioItem extends Item {

    public FieldRadioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                SupportService.cycle(serverPlayer);
            } else {
                SupportService.call(serverPlayer, SupportService.selected(serverPlayer));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
