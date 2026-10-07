package com.kn8.common.command;

import com.kn8.common.data.KN8Data;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionService;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Comandos de invasao (0.2, Etapa 7; operador): {@code /kn8 invasion start <invasao> [pos]} (centro = quem executa),
 * {@code /kn8 invasion next} (pula o aviso/intervalo), {@code /kn8 invasion stop} e {@code /kn8 invasion status}.
 */
final class InvasionCommands {

    private static final SuggestionProvider<CommandSourceStack> INVASIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(KN8Data.INVASION.serverIds(), builder);

    private InvasionCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> invasion() {
        return Commands.literal("invasion")
                .then(Commands.literal("start").then(Commands.argument("invasion", ResourceLocationArgument.id())
                        .suggests(INVASIONS)
                        .executes(ctx -> start(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> start(ctx, BlockPosArgument.getBlockPos(ctx, "pos"))))))
                .then(Commands.literal("stop").executes(ctx -> {
                    boolean stopped = InvasionService.stop(ctx.getSource().getLevel());
                    ctx.getSource().sendSuccess(() -> Component.translatable(stopped ? "kn8.command.invasion.stopped"
                            : "kn8.command.invasion.none"), true);
                    return stopped ? 1 : 0;
                }))
                .then(Commands.literal("next").executes(ctx -> {
                    boolean skipped = InvasionService.skip(ctx.getSource().getLevel());
                    ctx.getSource().sendSuccess(() -> Component.translatable(skipped ? "kn8.command.invasion.next"
                            : "kn8.command.invasion.none"), true);
                    return skipped ? 1 : 0;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    var active = InvasionService.active(ctx.getSource().getLevel());
                    if (active.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.invasion.none"), false);
                        return 0;
                    }
                    Invasion invasion = active.get();
                    ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.invasion.status",
                            InvasionService.name(invasion.id()), invasion.phase().name(), invasion.wave() + 1,
                            invasion.def().waves().size(), invasion.alive().size(), invasion.killed(),
                            invasion.center().toShortString()), false);
                    return 1;
                }));
    }

    private static int start(CommandContext<CommandSourceStack> ctx, BlockPos center) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "invasion");
        InvasionService.Result result = InvasionService.start(ctx.getSource().getLevel(), id, center);
        if (result != InvasionService.Result.OK) {
            ctx.getSource().sendFailure(Component.translatable("kn8.command.invasion." + result.name()
                    .toLowerCase(java.util.Locale.ROOT), id.toString()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.invasion.started", id.toString(),
                center.toShortString()), true);
        return 1;
    }
}
