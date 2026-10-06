// src/main/java/com/kn8/common/command/DebugCommands.java
package com.kn8.common.command;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.server.KN8Server;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Comandos de debug, so com {@code debug.allowCommands = true} no {@code kn8-server.toml}:
 * {@code /kn8 debug latency <ticks>} segura todo pacote C2S por N ticks antes de processa-lo (simula rede; 0
 * desliga). Fica so em memoria, nunca no config, para nao ir ligado para um servidor por engano (decisao do M2).
 */
final class DebugCommands {

    private static final int MAX_LATENCY_TICKS = 40;
    private static final int MILLIS_PER_TICK = 50;

    private DebugCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("debug")
                // O config pode ainda nao estar carregado quando a arvore de comandos e montada.
                .requires(source -> ServerConfig.SPEC.isLoaded() && ServerConfig.DEBUG_COMMANDS.get())
                .then(Commands.literal("latency")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, MAX_LATENCY_TICKS))
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    KN8Server.get(ctx.getSource().getServer()).setDebugInboundDelayTicks(ticks);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "kn8.command.debug.latency", ticks, ticks * MILLIS_PER_TICK), true);
                                    return 1;
                                })));
    }
}
