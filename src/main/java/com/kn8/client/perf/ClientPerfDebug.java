// src/main/java/com/kn8/client/perf/ClientPerfDebug.java
package com.kn8.client.perf;

import com.kn8.KN8Constants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Medidor de FPS do cliente (M8): {@code /kn8client fps [segundos]} amostra o FPS a cada tick durante N segundos e
 * mostra media, minimo e maximo. Serve para fechar o guia de arte (20 kaiju a vista) com numeros comparaveis.
 * O estado abaixo e so da medicao em andamento neste cliente.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ClientPerfDebug {

    private static final int DEFAULT_SECONDS = 10;
    private static final int MAX_SECONDS = 120;
    private static final int TICKS_PER_SECOND = 20;

    private static int ticksLeft;
    private static long sum;
    private static int samples;
    private static int min;
    private static int max;

    private ClientPerfDebug() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kn8client").then(Commands.literal("fps")
                .executes(ctx -> start(ctx, DEFAULT_SECONDS))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MAX_SECONDS))
                        .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "seconds"))))));
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int seconds) {
        ticksLeft = seconds * TICKS_PER_SECOND;
        sum = 0;
        samples = 0;
        min = Integer.MAX_VALUE;
        max = 0;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.fps.started", seconds), false);
        return 1;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ticksLeft <= 0) {
            return;
        }
        int fps = Minecraft.getInstance().getFps();
        sum += fps;
        samples++;
        min = Math.min(min, fps);
        max = Math.max(max, fps);
        if (--ticksLeft == 0) {
            Player player = Minecraft.getInstance().player;
            if (player != null && samples > 0) {
                player.displayClientMessage(Component.translatable("kn8.client.fps.result",
                        String.format("%.1f", sum / (double) samples), min, max, samples), false);
            }
        }
    }
}
