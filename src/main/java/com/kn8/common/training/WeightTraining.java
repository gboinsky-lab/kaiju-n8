package com.kn8.common.training;

import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.core.power.BodyStat;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

/**
 * Repeticao no suporte de pesos (TREINO): gasta {@code [exercise] liftStaminaCost} de stamina e da
 * {@code liftXp} de forca; uma a cada {@code liftIntervalTicks}, no maximo {@code liftRepsPerMinute} com XP por minuto
 * (depois o musculo cansa). Sem stamina nao levanta. Estado curto nos dados persistentes do jogador.
 */
public final class WeightTraining {

    public static final String TAG = "kn8_lift";
    private static final String NEXT = "next";
    private static final String WINDOW = "window";
    private static final String REPS = "reps";
    private static final int WINDOW_TICKS = 1200;

    private WeightTraining() {
    }

    /** Resultado de uma tentativa (para os testes). */
    public enum Result { LIFTED, TOO_FAST, NO_STAMINA, TIRED }

    public static Result lift(ServerPlayer player) {
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData().getCompound(TAG);
        if (now < data.getLong(NEXT)) {
            return Result.TOO_FAST;
        }
        if (!data.contains(WINDOW) || now - data.getLong(WINDOW) >= WINDOW_TICKS) {
            data.putLong(WINDOW, now);
            data.putInt(REPS, 0);
        }
        int reps = data.getInt(REPS);
        Result result;
        if (reps >= ServerConfig.LIFT_REPS_PER_MINUTE.get()) {
            result = Result.TIRED;
            player.displayClientMessage(Component.translatable("kn8.lift.tired").withStyle(ChatFormatting.GRAY), true);
        } else if (!player.isCreative()
                && !PowerService.tryConsumeStamina(player, ServerConfig.LIFT_STAMINA_COST.get())) {
            result = Result.NO_STAMINA;
            player.displayClientMessage(Component.translatable("kn8.lift.no_stamina")
                    .withStyle(ChatFormatting.RED), true);
        } else {
            result = Result.LIFTED;
            reps++;
            data.putInt(REPS, reps);
            PowerService.addBodyXp(player, BodyStat.STRENGTH, ServerConfig.LIFT_XP.get());
            player.swing(InteractionHand.MAIN_HAND, true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.PLAYERS, 0.7F, 0.7F + player.getRandom().nextFloat() * 0.2F);
            player.displayClientMessage(Component.translatable("kn8.lift.rep", reps,
                    ServerConfig.LIFT_REPS_PER_MINUTE.get(), String.format("%.0f", ServerConfig.LIFT_XP.get()))
                    .withStyle(ChatFormatting.AQUA), true);
        }
        data.putLong(NEXT, now + ServerConfig.LIFT_INTERVAL_TICKS.get());
        player.getPersistentData().put(TAG, data);
        return result;
    }
}
