package com.kn8.common.craft;

import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Suprimentos fabricados na bancada (0.2, Etapa 3): resfriador do traje (tira calor), estimulante (stamina cheia)
 * e catalisador de Release (XP de treino). Os valores ficam no config ({@code supply.*}).
 */
public class SupplyItem extends Item {

    public enum Kind {
        COOLANT, STIM, CATALYST
    }

    private static final int USE_COOLDOWN_TICKS = 40;

    private final Kind kind;

    public SupplyItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        switch (kind) {
            case COOLANT -> PowerService.setHeat(server, Math.max(0.0, PowerService.data(server).heat()
                    - ServerConfig.SUPPLY_COOLANT_HEAT.get()));
            case STIM -> PowerService.setStamina(server, PowerService.maxStamina(server));
            case CATALYST -> PowerService.addTrainingXp(server, ServerConfig.SUPPLY_CATALYST_XP.get());
        }
        server.displayClientMessage(Component.translatable("kn8.supply.used." + kind.name().toLowerCase())
                .withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, player.blockPosition(), kind == Kind.COOLANT ? SoundEvents.FIRE_EXTINGUISH
                : SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.7F, 1.3F);
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
