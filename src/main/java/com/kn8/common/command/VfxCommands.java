// src/main/java/com/kn8/common/command/VfxCommands.java
package com.kn8.common.command;

import java.util.Optional;

import com.kn8.common.data.KN8Data;
import com.kn8.common.vfx.VfxService;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * VFX-1 (operador): {@code /kn8 vfx play <efeito> <perfil> [intensidade] [receita]} toca um efeito na posicao de
 * quem executa (use {@code execute positioned}), pintado com o perfil visual {@code vfx_profile/<perfil>}; serve para
 * comparar a mesma habilidade em especies diferentes e conferir as receitas das proximas etapas sem esperar a luta.
 */
final class VfxCommands {

    private static final SuggestionProvider<CommandSourceStack> PROFILES = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(KN8Data.VFX_PROFILE.serverIds(), builder);

    private VfxCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> vfx() {
        return Commands.literal("vfx").then(Commands.literal("play")
                .then(Commands.argument("effect", ResourceLocationArgument.id())
                        .then(Commands.argument("profile", ResourceLocationArgument.id()).suggests(PROFILES)
                                .executes(ctx -> play(ctx, 1.0F, ""))
                                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.1F, 10.0F))
                                        .executes(ctx -> play(ctx, FloatArgumentType.getFloat(ctx, "intensity"), ""))
                                        .then(Commands.argument("recipe", StringArgumentType.greedyString())
                                                .executes(ctx -> play(ctx, FloatArgumentType.getFloat(ctx,
                                                        "intensity"), StringArgumentType.getString(ctx,
                                                        "recipe"))))))));
    }

    /**
     * Altura do corpo para a previa (VFX-2: o efeito cresce com o corpo): kaiju pela altura da tabela de alturas,
     * especial pela altura do perfil, senao a do tipo de entidade com o mesmo id; sem nada, a de referencia.
     */
    private static float heightOf(ResourceLocation profile) {
        return KN8Data.KAIJU.get(profile, false).map(def -> def.dimensions().scaledHeight())
                .or(() -> KN8Data.SPECIAL_SOLDIER.get(profile, false).map(def -> def.height())
                        .filter(height -> height > 0.0F))
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(profile).map(type -> type.getHeight())
                        .orElse(0.0F));
    }

    private static int play(CommandContext<CommandSourceStack> ctx, float intensity, String recipe) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation effect = ResourceLocationArgument.getId(ctx, "effect");
        ResourceLocation profile = ResourceLocationArgument.getId(ctx, "profile");
        float yaw = source.getRotation().y;
        Vec3 forward = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
        VfxService.play(source.getLevel(), effect, source.getPosition(), forward, intensity, 0.0F,
                Optional.of(profile), recipe, KN8Data.VFX_PROFILE.get(profile, false).isPresent()
                        ? heightOf(profile) : 0.0F);
        source.sendSuccess(() -> Component.translatable("kn8.command.vfx.play", effect.toString(),
                profile.toString()), false);
        return 1;
    }
}
