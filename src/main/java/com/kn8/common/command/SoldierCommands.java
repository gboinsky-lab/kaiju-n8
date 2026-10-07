// src/main/java/com/kn8/common/command/SoldierCommands.java
package com.kn8.common.command;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SoldierDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MobSpawnType;

/**
 * {@code /kn8 soldier spawn <variante> [nivel]}: invoca um Soldado 1 na posicao de quem executa. Variantes e niveis
 * vem do JSON ({@code data/kn8/kn8/soldier/soldier_1.json}), com sugestao no autocompletar.
 */
final class SoldierCommands {

    private static final String RANDOM = "random";

    private SoldierCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("soldier").then(Commands.literal("spawn")
                .then(Commands.argument("variant", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(variants(), builder))
                        .executes(ctx -> spawn(ctx, SoldierEntity.DEFAULT_LEVEL))
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(levels(), builder))
                                .executes(ctx -> spawn(ctx, StringArgumentType.getString(ctx, "level"))))));
    }

    /** Variantes do JSON + "random" (0.4: sorteia pelo peso, como o ovo). */
    private static Set<String> variants() {
        Set<String> names = new TreeSet<>(definition().map(def -> def.variants().keySet()).orElse(Set.of()));
        names.add(RANDOM);
        return names;
    }

    private static Set<String> levels() {
        return definition().map(def -> def.powerLevels().keySet()).orElse(Set.of());
    }

    private static Optional<SoldierDef> definition() {
        return KN8Data.SOLDIER.get(SoldierEntity.DEFINITION, false);
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, String level) {
        CommandSourceStack source = ctx.getSource();
        String variant = StringArgumentType.getString(ctx, "variant");
        if (!variants().contains(variant) || !levels().contains(level)) {
            source.sendFailure(Component.translatable("kn8.command.soldier.unknown", variant, level));
            return 0;
        }
        SoldierEntity soldier = KN8Entities.SOLDIER.get().create(source.getLevel());
        if (soldier == null) {
            return 0;
        }
        soldier.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z,
                source.getRotation().y, 0.0F);
        if (!RANDOM.equals(variant)) {
            soldier.setVariant(variant);
        }
        soldier.setPowerLevel(level);
        soldier.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(soldier.blockPosition()),
                MobSpawnType.COMMAND, null);
        source.getLevel().addFreshEntity(soldier);
        source.sendSuccess(() -> Component.translatable("kn8.command.soldier.spawned", soldier.variant(), level,
                soldier.release()), true);
        return 1;
    }
}
