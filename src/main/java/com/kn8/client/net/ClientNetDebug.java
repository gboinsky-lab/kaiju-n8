// src/main/java/com/kn8/client/net/ClientNetDebug.java
package com.kn8.client.net;

import java.util.HashMap;
import java.util.Map;

import com.kn8.KN8Constants;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.common.network.debug.NetPingC2S;
import com.kn8.common.network.debug.NetPongS2C;
import com.kn8.common.registry.KN8Attachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Diagnostico de rede no cliente (M3), por comandos que rodam SO no cliente:
 * <ul>
 *   <li>{@code /kn8client ping [n]}: envia n pings no mesmo tick (n &gt; 40 testa o rate limit do servidor);</li>
 *   <li>{@code /kn8client ping stats}: enviados, respondidos, tempo medio e maximo de ida e volta;</li>
 *   <li>{@code /kn8client probe [jogador]}: valor da sonda privada que ESTE cliente recebeu ("-" = nunca chegou).</li>
 * </ul>
 * O estado abaixo e so de diagnostico deste cliente (nao e estado de jogo) e e zerado a cada nova rodada de ping.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ClientNetDebug {

    private static final int MAX_PINGS = 200;
    private static final int PONGS_SHOWN = 5;
    private static final double NANOS_PER_MS = 1_000_000.0;

    private static final Map<Integer, Long> PENDING = new HashMap<>();
    private static int nextNonce;
    private static int sent;
    private static int received;
    private static double totalMs;
    private static double maxMs;

    private ClientNetDebug() {
    }

    /** Instala o tratamento do pong na ponte comum. Chamado pelo {@code KN8Client}. */
    public static void install() {
        KN8ClientHooks.register(NetPongS2C.TYPE, ClientNetDebug::onPong);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> ping = Commands.literal("ping")
                .executes(ctx -> ping(ctx, 1))
                .then(Commands.literal("stats").executes(ClientNetDebug::stats))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_PINGS))
                        .executes(ctx -> ping(ctx, IntegerArgumentType.getInteger(ctx, "count"))));
        LiteralArgumentBuilder<CommandSourceStack> probe = Commands.literal("probe")
                .executes(ctx -> probe(ctx, null))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> probe(ctx, StringArgumentType.getString(ctx, "player"))));
        event.getDispatcher().register(Commands.literal("kn8client").then(ping).then(probe));
    }

    private static int ping(CommandContext<CommandSourceStack> ctx, int count) {
        PENDING.clear();
        sent = 0;
        received = 0;
        totalMs = 0;
        maxMs = 0;
        for (int i = 0; i < count; i++) {
            int nonce = nextNonce++;
            PENDING.put(nonce, System.nanoTime());
            PacketDistributor.sendToServer(new NetPingC2S(nonce));
            sent++;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.net.ping_sent", count), false);
        return count;
    }

    private static void onPong(NetPongS2C pong) {
        Long sentAt = PENDING.remove(pong.nonce());
        if (sentAt == null) {
            return;
        }
        double ms = (System.nanoTime() - sentAt) / NANOS_PER_MS;
        received++;
        totalMs += ms;
        maxMs = Math.max(maxMs, ms);
        Player player = Minecraft.getInstance().player;
        if (player != null && received <= PONGS_SHOWN) {
            player.displayClientMessage(Component.translatable("kn8.client.net.pong", pong.nonce(),
                    String.format("%.1f", ms), pong.serverTick()), false);
        }
    }

    private static int stats(CommandContext<CommandSourceStack> ctx) {
        double average = received == 0 ? 0 : totalMs / received;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.net.stats", sent, received,
                String.format("%.1f", average), String.format("%.1f", maxMs)), false);
        return received;
    }

    private static int probe(CommandContext<CommandSourceStack> ctx, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return 0;
        }
        Player target = minecraft.player;
        if (name != null) {
            target = null;
            for (AbstractClientPlayer candidate : minecraft.level.players()) {
                if (candidate.getGameProfile().getName().equalsIgnoreCase(name)) {
                    target = candidate;
                }
            }
            if (target == null) {
                ctx.getSource().sendFailure(Component.translatable("kn8.client.net.player_not_found", name));
                return 0;
            }
        }
        // getExistingDataOrNull nao cria o valor no cliente: "-" prova que nada foi recebido.
        Integer value = target.getExistingDataOrNull(KN8Attachments.NET_PROBE);
        String shown = value == null ? "-" : value.toString();
        Player shownPlayer = target;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.net.probe", shownPlayer.getName(),
                shown), false);
        return 1;
    }
}
