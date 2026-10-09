// src/main/java/com/kn8/common/command/AnimCommands.java
package com.kn8.common.command;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import com.kn8.common.anim.AnimationBridge;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Comandos de teste de animacao (M9; o combate que dispara as animacoes de verdade chega no M10):
 * <ul>
 *   <li>{@code /kn8 anim play <jogadores> <animacao>}: toca uma animacao PAL (ex.: kn8:player.action.heavy);</li>
 *   <li>{@code /kn8 anim stop <jogadores>}: para a camada de combate (ex.: soltar o bloqueio);</li>
 *   <li>{@code /kn8 anim kaiju <controller> <nome>}: dispara uma animacao no kaiju mais proximo;</li>
 *   <li>{@code /kn8 anim soldier <controller> <nome>}: idem no soldado (ou Hoshina) mais proximo (0.5.0-D3, conferir
 *   as tecnicas com o soldado parado; nome = animacao registrada no controller, ex.: action kuuchi).</li>
 * </ul>
 */
final class AnimCommands {

    private static final double KAIJU_RADIUS = 32.0;

    private AnimCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("anim")
                .then(Commands.literal("play").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("animation", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        AnimationBridge.PLAYER_ANIMATIONS, builder))
                                .executes(ctx -> play(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                        ResourceLocationArgument.getId(ctx, "animation"))))))
                .then(Commands.literal("stop").then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> stop(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("kaiju").then(Commands.argument("controller", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> kaiju(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "controller"),
                                        StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("soldier").then(Commands.argument("controller", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> soldier(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "controller"),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static int soldier(CommandSourceStack source, String controller, String name) {
        AABB area = AABB.ofSize(source.getPosition(), KAIJU_RADIUS * 2, KAIJU_RADIUS * 2, KAIJU_RADIUS * 2);
        Optional<SoldierEntity> nearest = source.getLevel().getEntitiesOfClass(SoldierEntity.class, area).stream()
                .min(Comparator.comparingDouble(soldier -> soldier.distanceToSqr(source.getPosition())));
        if (nearest.isEmpty()) {
            source.sendFailure(Component.translatable("kn8.command.anim.no_soldier"));
            return 0;
        }
        nearest.get().triggerAnim(controller, name);
        source.sendSuccess(() -> Component.translatable("kn8.command.anim.soldier", controller, name), false);
        return 1;
    }

    private static int play(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation animation) {
        targets.forEach(player -> AnimationBridge.playPlayer(player, animation));
        source.sendSuccess(() -> Component.translatable("kn8.command.anim.played", animation.toString(),
                targets.size()), false);
        return targets.size();
    }

    private static int stop(CommandSourceStack source, Collection<ServerPlayer> targets) {
        targets.forEach(AnimationBridge::stopPlayer);
        source.sendSuccess(() -> Component.translatable("kn8.command.anim.stopped", targets.size()), false);
        return targets.size();
    }

    private static int kaiju(CommandSourceStack source, String controller, String name) {
        AABB area = AABB.ofSize(source.getPosition(), KAIJU_RADIUS * 2, KAIJU_RADIUS * 2, KAIJU_RADIUS * 2);
        Optional<KaijuEntity> nearest = source.getLevel().getEntitiesOfClass(KaijuEntity.class, area).stream()
                .min(Comparator.comparingDouble(kaiju -> kaiju.distanceToSqr(source.getPosition())));
        if (nearest.isEmpty()) {
            source.sendFailure(Component.translatable("kn8.command.kaiju.none"));
            return 0;
        }
        AnimationBridge.playKaiju(nearest.get(), controller, name);
        source.sendSuccess(() -> Component.translatable("kn8.command.anim.kaiju", controller, name), false);
        return 1;
    }
}
