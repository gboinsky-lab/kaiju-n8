// src/main/java/com/kn8/common/command/DuelCommands.java
package com.kn8.common.command;

import com.kn8.common.soldier.SoldierEntity;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * {@code /kn8 duel <a> <b>} e {@code /kn8 duel stop}: duelo de teste entre dois soldados (Miguel, 2026-10-16: "em um
 * combate dos soldados especiais quem ganharia?"). Marca um com o UUID do outro ({@link SoldierEntity#DUEL_TAG}):
 * so os dois marcados podem se ferir e cada um mira so no outro; no jogo normal continuam aliados.
 */
final class DuelCommands {

    private DuelCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("duel")
                .then(Commands.literal("stop").executes(DuelCommands::stop))
                .then(Commands.argument("a", EntityArgument.entity())
                        .then(Commands.argument("b", EntityArgument.entity()).executes(DuelCommands::start)));
    }

    private static int start(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity a = EntityArgument.getEntity(ctx, "a");
        Entity b = EntityArgument.getEntity(ctx, "b");
        if (!(a instanceof SoldierEntity first) || !(b instanceof SoldierEntity second) || a == b) {
            ctx.getSource().sendFailure(Component.translatable("kn8.command.duel.invalid"));
            return 0;
        }
        first.getPersistentData().putUUID(SoldierEntity.DUEL_TAG, second.getUUID());
        second.getPersistentData().putUUID(SoldierEntity.DUEL_TAG, first.getUUID());
        first.setTarget(second);
        second.setTarget(first);
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.duel.started", a.getDisplayName(),
                b.getDisplayName()), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        int count = 0;
        for (Entity entity : ctx.getSource().getLevel().getAllEntities()) {
            if (entity instanceof SoldierEntity soldier && soldier.duelOpponent().isPresent()) {
                soldier.getPersistentData().remove(SoldierEntity.DUEL_TAG);
                soldier.setTarget(null);
                count++;
            }
        }
        int stopped = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.duel.stopped", stopped), true);
        return stopped;
    }
}
