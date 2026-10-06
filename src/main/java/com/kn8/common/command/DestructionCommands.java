// src/main/java/com/kn8/common/command/DestructionCommands.java
package com.kn8.common.command;

import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.destruction.ProtectedAreas;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Comandos da destruicao (Etapa E):
 * <ul>
 *   <li>{@code /kn8 destruction protect <de> <ate> <nome>} / {@code unprotect <nome>} / {@code list};</li>
 *   <li>{@code /kn8 destruction test <raio> <forca> [cratera]}: impacto de teste onde o comando e executado;</li>
 *   <li>{@code /kn8 destruction restore}: restaura aos poucos tudo o que foi destruido na dimensao.</li>
 * </ul>
 */
final class DestructionCommands {

    private static final float MAX_TEST_RADIUS = 16.0F;
    private static final float TEST_DEPTH_FRACTION = 0.4F;

    private DestructionCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("destruction")
                .then(Commands.literal("protect").then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> protect(ctx.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "to"),
                                                StringArgumentType.getString(ctx, "name")))))))
                .then(Commands.literal("unprotect").then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> unprotect(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("test").then(Commands.argument("radius",
                                FloatArgumentType.floatArg(0.5F, MAX_TEST_RADIUS))
                        .then(Commands.argument("power", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> test(ctx.getSource(), FloatArgumentType.getFloat(ctx, "radius"),
                                        IntegerArgumentType.getInteger(ctx, "power"), false))
                                .then(Commands.argument("crater", BoolArgumentType.bool())
                                        .executes(ctx -> test(ctx.getSource(),
                                                FloatArgumentType.getFloat(ctx, "radius"),
                                                IntegerArgumentType.getInteger(ctx, "power"),
                                                BoolArgumentType.getBool(ctx, "crater")))))))
                .then(Commands.literal("restore").executes(ctx -> restore(ctx.getSource())));
    }

    private static int protect(CommandSourceStack source, BlockPos from, BlockPos to, String name) {
        ProtectedAreas.get(source.getLevel()).add(name, from, to);
        source.sendSuccess(() -> Component.translatable("kn8.command.destruction.protected", name), true);
        return 1;
    }

    private static int unprotect(CommandSourceStack source, String name) {
        boolean removed = ProtectedAreas.get(source.getLevel()).remove(name);
        source.sendSuccess(() -> Component.translatable(removed ? "kn8.command.destruction.unprotected"
                : "kn8.command.destruction.unknown", name), true);
        return removed ? 1 : 0;
    }

    private static int list(CommandSourceStack source) {
        var areas = ProtectedAreas.get(source.getLevel()).areas();
        source.sendSuccess(() -> Component.translatable("kn8.command.destruction.list", areas.size()), false);
        areas.forEach((name, box) -> source.sendSuccess(() -> Component.literal(" - " + name + ": " + box), false));
        return areas.size();
    }

    private static int test(CommandSourceStack source, float radius, int power, boolean crater) {
        Vec3 center = source.getPosition();
        DestructionService.request(source.getLevel(), center,
                new AbilityDef.Destruction(radius, power, crater, radius * TEST_DEPTH_FRACTION), source.getEntity());
        source.sendSuccess(() -> Component.translatable("kn8.command.destruction.test", radius, power), false);
        return 1;
    }

    private static int restore(CommandSourceStack source) {
        int count = DestructionService.startRestore(source.getLevel());
        source.sendSuccess(() -> Component.translatable("kn8.command.destruction.restore", count), true);
        return count;
    }
}
