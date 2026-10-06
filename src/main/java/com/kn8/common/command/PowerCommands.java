// src/main/java/com/kn8/common/command/PowerCommands.java
package com.kn8.common.command;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.kn8.common.attribute.PowerService;
import com.kn8.common.attribute.PowerView;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos de atributos (Fase 4, secao 10.2), todos com jogador opcional no fim:
 * <ul>
 *   <li>{@code /kn8 power [jogador]}: valores do SERVIDOR (comparar com {@code /kn8client power});</li>
 *   <li>{@code /kn8 release set|xp|surge <n>}, {@code /kn8 release cap <n>|clear};</li>
 *   <li>{@code /kn8 heat set <n>}, {@code /kn8 stamina set|consume <n>}, {@code /kn8 energy set <n>}.</li>
 * </ul>
 */
final class PowerCommands {

    private static final int MAX_XP = 1_000_000;
    private static final int MAX_SURGE_ARGUMENT = 50;
    private static final double MAX_RESOURCE = 10_000.0;

    private PowerCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> power() {
        return Commands.literal("power")
                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> release() {
        return Commands.literal("release")
                .then(Commands.literal("set").then(intAction(0, 100, PowerService::setTrainedRelease)))
                .then(Commands.literal("xp").then(intAction(0, MAX_XP, PowerService::addTrainingXp)))
                .then(Commands.literal("surge").then(intAction(0, MAX_SURGE_ARGUMENT, PowerService::setSurge)))
                .then(Commands.literal("cap")
                        .then(Commands.literal("clear")
                                .executes(ctx -> apply(ctx, self(ctx), player -> PowerService.setCapOverride(player,
                                        -1)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> apply(ctx, EntityArgument.getPlayer(ctx, "target"),
                                                player -> PowerService.setCapOverride(player, -1)))))
                        .then(intAction(0, 100, PowerService::setCapOverride)));
    }

    static LiteralArgumentBuilder<CommandSourceStack> heat() {
        return Commands.literal("heat").then(Commands.literal("set")
                .then(doubleAction(PowerService::setHeat)));
    }

    static LiteralArgumentBuilder<CommandSourceStack> stamina() {
        return Commands.literal("stamina")
                .then(Commands.literal("set").then(doubleAction(PowerService::setStamina)))
                .then(Commands.literal("consume").then(doubleAction((player, amount) -> {
                    if (!PowerService.tryConsumeStamina(player, amount)) {
                        player.displayClientMessage(Component.translatable("kn8.command.power.no_stamina"), true);
                    }
                })));
    }

    static LiteralArgumentBuilder<CommandSourceStack> energy() {
        return Commands.literal("energy").then(Commands.literal("set").then(doubleAction(PowerService::setEnergy)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> intAction(int min, int max,
            BiConsumer<ServerPlayer, Integer> action) {
        return Commands.argument("value", IntegerArgumentType.integer(min, max))
                .executes(ctx -> apply(ctx, self(ctx),
                        player -> action.accept(player, IntegerArgumentType.getInteger(ctx, "value"))))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> apply(ctx, EntityArgument.getPlayer(ctx, "target"),
                                player -> action.accept(player, IntegerArgumentType.getInteger(ctx, "value")))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> doubleAction(BiConsumer<ServerPlayer, Double> action) {
        return Commands.argument("value", DoubleArgumentType.doubleArg(0.0, MAX_RESOURCE))
                .executes(ctx -> apply(ctx, self(ctx),
                        player -> action.accept(player, DoubleArgumentType.getDouble(ctx, "value"))))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> apply(ctx, EntityArgument.getPlayer(ctx, "target"),
                                player -> action.accept(player, DoubleArgumentType.getDouble(ctx, "value")))));
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, ServerPlayer target,
            Consumer<ServerPlayer> change) {
        change.accept(target);
        return show(ctx.getSource(), target);
    }

    static int show(CommandSourceStack source, ServerPlayer target) {
        PowerView view = PowerService.view(target);
        source.sendSuccess(() -> Component.translatable("kn8.command.power.show", target.getDisplayName(),
                view.trained(), view.effective(), view.cap(), view.surge(), view.releaseXp(), view.xpToNext()),
                false);
        source.sendSuccess(() -> Component.translatable("kn8.command.power.resources",
                String.format("%.1f/%.1f", view.stamina(), view.maxStamina()), String.format("%.1f", view.heat()),
                Component.translatable("kn8.heat_stage." + view.heatStage()), String.format("%.1f", view.energy()),
                view.control()), false);
        return view.effective();
    }
}
