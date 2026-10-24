package com.kn8.common.command;

import java.util.UUID;

import com.kn8.common.host.KaijuHostData;
import com.kn8.common.host.LarvaService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * T-1: comandos do hospedeiro do Kaiju No. 8 (operador). {@code /kn8 host status} diz quem e o hospedeiro do mundo e
 * se ha larva voando; {@code spawn <jogador>} faz a larva aparecer para ele agora (sem sorteio nem lugar, mas com a
 * regra de um hospedeiro por mundo); {@code infect}/{@code clear <jogador>} infecta direto ou desfaz.
 */
final class HostCommands {

    private HostCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> host() {
        return Commands.literal("host")
                .then(Commands.literal("status").executes(HostCommands::status))
                .then(Commands.literal("spawn").then(Commands.argument("player", EntityArgument.player())
                        .executes(HostCommands::spawn)))
                .then(Commands.literal("infect").then(Commands.argument("player", EntityArgument.player())
                        .executes(HostCommands::infect)))
                .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player())
                        .executes(HostCommands::clear)));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        KaijuHostData data = KaijuHostData.get(ctx.getSource().getServer());
        UUID host = data.host();
        String name = "-";
        if (host != null) {
            ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayer(host);
            name = online != null ? online.getGameProfile().getName() : host.toString();
        }
        boolean larva = LarvaService.larvaAlive(ctx.getSource().getServer(), data) != null;
        String shown = name;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.host.status", shown,
                Component.translatable(larva ? "kn8.command.host.larva.yes" : "kn8.command.host.larva.no")), false);
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (!LarvaService.canAppearFor(player)) {
            ctx.getSource().sendFailure(Component.translatable("kn8.command.host.taken"));
            return 0;
        }
        if (LarvaService.spawnFor(player).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("kn8.command.host.spawn_failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.host.spawned",
                player.getGameProfile().getName()), true);
        return 1;
    }

    private static int infect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (!LarvaService.infect(player)) {
            ctx.getSource().sendFailure(Component.translatable("kn8.command.host.taken"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.host.infected",
                player.getGameProfile().getName()), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        LarvaService.clear(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.host.cleared",
                player.getGameProfile().getName()), true);
        return 1;
    }
}
