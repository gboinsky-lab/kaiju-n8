// src/main/java/com/kn8/common/command/NetCommands.java
package com.kn8.common.command;

import java.util.Map;

import com.kn8.common.network.NetworkSync;
import com.kn8.common.network.debug.NetProbeSyncS2C;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.server.KN8Server;
import com.kn8.core.net.RateLimiter;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos de rede (Fase 4, secao 10.2):
 * <ul>
 *   <li>{@code /kn8 net stats [jogador]}: pacotes aceitos e descartados pelo rate limit, por tipo;</li>
 *   <li>{@code /kn8 net probe <valor> [jogador]}: altera a sonda privada e marca o canal para envio;</li>
 *   <li>{@code /kn8 net resend [jogador]}: reenvia todos os canais privados (diagnostico).</li>
 * </ul>
 */
final class NetCommands {

    // Limite so da sonda de teste; nao e valor de gameplay.
    private static final int MAX_PROBE_VALUE = 1_000_000;

    private NetCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("net")
                .then(Commands.literal("stats")
                        .executes(ctx -> stats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> stats(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("probe")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_PROBE_VALUE))
                                .executes(ctx -> probe(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "value")))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> probe(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                                                IntegerArgumentType.getInteger(ctx, "value"))))))
                .then(Commands.literal("resend")
                        .executes(ctx -> resend(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> resend(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
    }

    private static int stats(CommandSourceStack source, ServerPlayer target) {
        Map<String, RateLimiter.Counters> stats = KN8Server.get(source.getServer()).rateLimiter()
                .stats(target.getUUID());
        if (stats.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("kn8.command.net.stats_empty", target.getDisplayName()),
                    false);
            return 0;
        }
        for (Map.Entry<String, RateLimiter.Counters> entry : stats.entrySet()) {
            source.sendSuccess(() -> Component.translatable("kn8.command.net.stats", target.getDisplayName(),
                    entry.getKey(), entry.getValue().accepted(), entry.getValue().dropped()), false);
        }
        return stats.size();
    }

    private static int probe(CommandSourceStack source, ServerPlayer target, int value) {
        target.setData(KN8Attachments.NET_PROBE, value);
        NetworkSync.markDirty(target, NetProbeSyncS2C.CHANNEL);
        source.sendSuccess(() -> Component.translatable("kn8.command.net.probe", target.getDisplayName(), value),
                true);
        return 1;
    }

    private static int resend(CommandSourceStack source, ServerPlayer target) {
        NetworkSync.sendAll(target);
        source.sendSuccess(() -> Component.translatable("kn8.command.net.resend", target.getDisplayName()), true);
        return 1;
    }
}
