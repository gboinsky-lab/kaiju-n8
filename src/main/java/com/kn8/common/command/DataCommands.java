// src/main/java/com/kn8/common/command/DataCommands.java
package com.kn8.common.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.kn8.common.data.DataRegistry;
import com.kn8.common.data.DataReport;
import com.kn8.common.data.DataValidation;
import com.kn8.common.data.KN8Data;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Comandos de dados (Fase 4, secao 9.3):
 * <ul>
 *   <li>{@code /kn8 data validate}: refaz a validacao cruzada e mostra o resumo e as primeiras mensagens;</li>
 *   <li>{@code /kn8 data dump <tipo>}: lista os ids validados de um tipo (kaiju, rank, mission...).</li>
 * </ul>
 */
final class DataCommands {

    private static final int MAX_MESSAGES = 10;

    private DataCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("data")
                .then(Commands.literal("validate").executes(ctx -> validate(ctx.getSource())))
                .then(Commands.literal("dump")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        KN8Data.ALL.stream().map(registry -> registry.id().getPath()), builder))
                                .executes(ctx -> dump(ctx.getSource(), StringArgumentType.getString(ctx, "type")))));
    }

    private static int validate(CommandSourceStack source) {
        DataReport report = DataValidation.run();
        source.sendSuccess(() -> Component.translatable("kn8.command.data.summary", report.summary()), false);
        List<String> messages = new ArrayList<>(report.errors());
        messages.addAll(report.warnings());
        messages.stream().limit(MAX_MESSAGES).forEach(message ->
                source.sendSuccess(() -> Component.literal("  " + message), false));
        return report.errors().isEmpty() ? 1 : 0;
    }

    private static int dump(CommandSourceStack source, String type) {
        Optional<DataRegistry<?>> registry = KN8Data.byName(type);
        if (registry.isEmpty()) {
            source.sendFailure(Component.translatable("kn8.command.data.unknown_type", type));
            return 0;
        }
        List<ResourceLocation> ids = registry.get().serverIds();
        source.sendSuccess(() -> Component.translatable("kn8.command.data.dump", type, ids.size(),
                String.join(", ", ids.stream().map(ResourceLocation::toString).toList())), false);
        return ids.size();
    }
}
