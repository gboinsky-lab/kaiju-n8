// src/main/java/com/kn8/common/command/AuraCommands.java
package com.kn8.common.command;

import java.util.Collection;
import java.util.Optional;

import com.kn8.common.data.KN8Data;
import com.kn8.common.registry.KN8Attachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Comandos da aura de poder (0.5; operador). {@code /kn8 aura set <alvos> <aura>} troca a aura (jogador: por cima da
 * do traje, ate relogar; outra entidade: direto), {@code /kn8 aura clear <alvos>} volta a do traje/padrao e
 * {@code /kn8 aura release <alvos> <%>} mostra a aura numa entidade que nao e jogador (soldado, para previa; o
 * jogador usa {@code /kn8 release}).
 */
final class AuraCommands {

    private static final SuggestionProvider<CommandSourceStack> AURAS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(KN8Data.AURA.serverIds(), builder);

    private AuraCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> aura() {
        return Commands.literal("aura")
                .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("aura", ResourceLocationArgument.id()).suggests(AURAS)
                                .executes(AuraCommands::set))))
                .then(Commands.literal("clear").then(Commands.argument("targets", EntityArgument.entities())
                        .executes(AuraCommands::clear)))
                .then(Commands.literal("release").then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(AuraCommands::release))));
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation aura = ResourceLocationArgument.getId(ctx, "aura");
        if (KN8Data.AURA.get(aura, false).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("kn8.command.aura.unknown", aura.toString()));
            return 0;
        }
        int count = 0;
        for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
            if (entity instanceof ServerPlayer player) {
                player.setData(KN8Attachments.AURA_OVERRIDE, Optional.of(aura));
                count++;
            } else if (entity instanceof LivingEntity living) {
                living.setData(KN8Attachments.AURA, aura);
                count++;
            }
        }
        int changed = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.aura.set", aura.toString(), changed),
                true);
        return changed;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        for (Entity entity : targets) {
            if (entity instanceof ServerPlayer player) {
                player.setData(KN8Attachments.AURA_OVERRIDE, Optional.empty());
            } else if (entity instanceof LivingEntity living) {
                living.removeData(KN8Attachments.AURA);
                living.removeData(KN8Attachments.RELEASE_VISUAL);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.aura.cleared", targets.size()), true);
        return targets.size();
    }

    private static int release(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int percent = IntegerArgumentType.getInteger(ctx, "percent");
        int count = 0;
        for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
            // No jogador a % e do PowerService (seria sobrescrita no proximo tick): so outras entidades.
            if (entity instanceof LivingEntity living && !(entity instanceof ServerPlayer)) {
                living.setData(KN8Attachments.RELEASE_VISUAL, percent);
                count++;
            }
        }
        int changed = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.aura.release", percent, changed), true);
        return changed;
    }
}
