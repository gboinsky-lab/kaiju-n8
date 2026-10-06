package com.kn8.common.command;

import java.util.Map;
import java.util.Optional;

import com.kn8.common.boss.BossService;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.RankDef;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Comandos de carreira, missao e chefe (0.2; operador):
 * <ul>
 *   <li>{@code /kn8 rank [jogador]} mostra; {@code /kn8 rank set <patente> [jogador]};</li>
 *   <li>{@code /kn8 merit add <n> [jogador]} (negativo tira);</li>
 *   <li>{@code /kn8 mission accept|abandon <missao> [jogador]}, {@code /kn8 mission list [jogador]},
 *   {@code /kn8 mission reset [jogador]} (limpa missoes ativas, concluidas e esperas);</li>
 *   <li>{@code /kn8 boss spawn <chefe>}: o chefe surge 12 blocos a frente.</li>
 * </ul>
 */
final class CareerCommands {

    private static final double BOSS_DISTANCE = 12.0;

    private static final SuggestionProvider<CommandSourceStack> RANKS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(KN8Data.RANK.serverIds(), builder);
    private static final SuggestionProvider<CommandSourceStack> MISSIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(KN8Data.MISSION.serverIds(), builder);
    private static final SuggestionProvider<CommandSourceStack> BOSSES = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(KN8Data.BOSS.serverIds(), builder);

    private CareerCommands() {
    }

    interface PlayerAction {
        int run(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException;
    }

    /** {@code <acao>} e {@code <acao> <jogador>}. */
    private static <T extends com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, T>> T withTarget(
            T builder, PlayerAction action) {
        return builder.executes(ctx -> action.run(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> action.run(ctx, EntityArgument.getPlayer(ctx, "target"))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> rank() {
        return withTarget(Commands.literal("rank"), CareerCommands::show)
                .then(Commands.literal("set").then(withTarget(Commands.argument("rank",
                        ResourceLocationArgument.id()).suggests(RANKS), (ctx, player) -> {
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "rank");
                            if (KN8Data.RANK.get(id, false).isEmpty()) {
                                ctx.getSource().sendFailure(Component.translatable("kn8.command.unknown", id));
                                return 0;
                            }
                            CareerService.setRank(player, id);
                            return show(ctx, player);
                        })));
    }

    static LiteralArgumentBuilder<CommandSourceStack> merit() {
        return Commands.literal("merit").then(Commands.literal("add").then(withTarget(
                Commands.argument("amount", IntegerArgumentType.integer(-1_000_000, 1_000_000)), (ctx, player) -> {
                    CareerService.addMerit(player, IntegerArgumentType.getInteger(ctx, "amount"));
                    return show(ctx, player);
                })));
    }

    static LiteralArgumentBuilder<CommandSourceStack> mission() {
        return Commands.literal("mission")
                .then(Commands.literal("accept").then(withTarget(Commands.argument("mission",
                        ResourceLocationArgument.id()).suggests(MISSIONS), (ctx, player) -> {
                            MissionService.Result result = MissionService.accept(player,
                                    ResourceLocationArgument.getId(ctx, "mission"));
                            ctx.getSource().sendSuccess(() -> Component.literal(result.name()), false);
                            return result == MissionService.Result.OK ? 1 : 0;
                        })))
                .then(Commands.literal("abandon").then(withTarget(Commands.argument("mission",
                        ResourceLocationArgument.id()).suggests(MISSIONS), (ctx, player) ->
                        MissionService.abandon(player, ResourceLocationArgument.getId(ctx, "mission")) ? 1 : 0)))
                .then(withTarget(Commands.literal("list"), CareerCommands::listMissions))
                .then(withTarget(Commands.literal("reset"), (ctx, player) -> {
                    CareerData data = CareerService.data(player);
                    data.active().clear();
                    data.completed().clear();
                    data.cooldowns().clear();
                    CareerService.changed(player);
                    return listMissions(ctx, player);
                }));
    }

    static LiteralArgumentBuilder<CommandSourceStack> boss() {
        return Commands.literal("boss").then(Commands.literal("spawn").then(Commands.argument("boss",
                ResourceLocationArgument.id()).suggests(BOSSES).executes(ctx -> {
                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "boss");
                    CommandSourceStack source = ctx.getSource();
                    Vec3 look = source.getRotation() == null ? Vec3.ZERO
                            : Vec3.directionFromRotation(0, source.getRotation().y);
                    Vec3 at = source.getPosition().add(look.scale(BOSS_DISTANCE));
                    boolean ok = BossService.spawn(source.getLevel(), id, BlockPos.containing(at)).isPresent();
                    if (!ok) {
                        source.sendFailure(Component.translatable("kn8.command.unknown", id));
                    }
                    return ok ? 1 : 0;
                })));
    }

    private static int show(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        Optional<Map.Entry<ResourceLocation, RankDef>> rank = CareerService.rank(player);
        CareerData data = CareerService.data(player);
        Component rankName = rank.<Component>map(entry -> Component.translatable("kn8.rank." + entry.getKey()
                .getPath())).orElse(Component.literal("-"));
        Component next = CareerService.nextRank(player).<Component>map(entry -> Component.translatable(
                "kn8.command.career.next", Component.translatable("kn8.rank." + entry.getKey().getPath()),
                entry.getValue().meritRequired())).orElse(Component.literal("-"));
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.command.career.show", player.getName(),
                rankName, data.merit(), next, data.kaijuKills(), data.dismantled(), data.missionsDone()), false);
        return 1;
    }

    private static int listMissions(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CareerData data = CareerService.data(player);
        StringBuilder text = new StringBuilder();
        for (ResourceLocation id : KN8Data.MISSION.serverIds()) {
            String state = data.active().containsKey(id) ? "ATIVA " + data.active().get(id).progress()
                    : data.completed().contains(id) ? "concluida" : MissionService.canAccept(player, id).name();
            text.append("\n ").append(id).append(": ").append(state);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + text), false);
        return 1;
    }
}
