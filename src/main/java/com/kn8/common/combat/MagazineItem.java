// src/main/java/com/kn8/common/combat/MagazineItem.java
package com.kn8.common.combat;

import java.util.List;

import com.kn8.common.registry.KN8DataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Pente de arma de fogo (0.5.0-D2, Miguel: sem municao infinita). Guarda os tiros no componente
 * {@code kn8:rounds}; a capacidade vem do {@code weapon_profile} que aponta para este item ({@code magazine_item}).
 * Usar o pente (botao direito) carrega com a municao da mochila; a arma troca o pente dela por um carregado na
 * recarga ({@link WeaponHandling}).
 */
public class MagazineItem extends Item {

    private static final int LOAD_COOLDOWN_TICKS = 20;
    private static final int BAR_COLOR = 0xFFE0B040;

    public MagazineItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int rounds(ItemStack stack) {
        return stack.getOrDefault(KN8DataComponents.ROUNDS.get(), 0);
    }

    public static void setRounds(ItemStack stack, int rounds) {
        stack.set(KN8DataComponents.ROUNDS.get(), Math.max(0, rounds));
    }

    private static int capacity(ItemStack stack, boolean clientSide) {
        return WeaponHandling.magazineCapacity(stack.getItem(), clientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int capacity = capacity(stack, level.isClientSide());
        if (capacity <= 0 || rounds(stack) >= capacity) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            int loaded = WeaponHandling.loadMagazine(player, stack, capacity);
            if (loaded <= 0) {
                player.displayClientMessage(Component.translatable("kn8.magazine.no_ammo"), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE,
                    SoundSource.PLAYERS, 0.7F, 1.2F);
            player.getCooldowns().addCooldown(this, LOAD_COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, capacity(stack, true));
        return Mth.clamp(Math.round(13.0F * rounds(stack) / capacity), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("kn8.magazine.rounds", rounds(stack), capacity(stack, true)));
        tooltip.add(Component.translatable("kn8.magazine.hint"));
    }
}
