// src/main/java/com/kn8/common/command/KaijuCommands.java
package com.kn8.common.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Comandos de kaiju (Fase 4, secao 10.2):
 * <ul>
 *   <li>{@code /kn8 kaiju spawn <especie> [quantidade]}: invoca na frente de quem executa;</li>
 *   <li>{@code /kn8 kaiju info}: o kaiju mais proximo (32 blocos) com os atributos aplicados do JSON, vida do nucleo
 *   e partes (multiplicador; * = nucleo);</li>
 *   <li>{@code /kn8 kaiju stagger <ticks>}: atordoa o mais proximo (teste do estado STAGGER);</li>
 *   <li>{@code /kn8 kaiju clear}: remove todos os kaiju da dimensao.</li>
 * </ul>
 */
final class KaijuCommands {

    private static final int MAX_SPAWN = 50;
    private static final double SPAWN_DISTANCE = 6.0;
    private static final double INFO_RADIUS = 32.0;
    private static final int MAX_STAGGER = 1200;
    private static final int GROUND_SEARCH_BLOCKS = 16;

    private KaijuCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("kaiju")
                .then(Commands.literal("spawn")
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        KN8Entities.KAIJU.stream().map(type -> type.getId().getPath()), builder))
                                .executes(ctx -> spawn(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "species"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SPAWN))
                                        .executes(ctx -> spawn(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "species"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("stagger")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, MAX_STAGGER))
                                .executes(ctx -> stagger(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "ticks")))))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())));
    }

    private static int spawn(CommandSourceStack source, String species, int count) {
        Optional<EntityType<KaijuEntity>> type = KN8Entities.KAIJU.stream()
                .filter(holder -> holder.getId().getPath().equals(species))
                .map(holder -> holder.get())
                .findFirst();
        if (type.isEmpty()) {
            source.sendFailure(Component.translatable("kn8.command.kaiju.unknown", species));
            return 0;
        }
        ServerLevel level = source.getLevel();
        Vec3 look = source.getRotation() == null ? Vec3.ZERO
                : Vec3.directionFromRotation(0, source.getRotation().y);
        Vec3 position = source.getPosition().add(look.scale(SPAWN_DISTANCE));
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            KaijuEntity kaiju = type.get().create(level);
            if (kaiju != null) {
                kaiju.moveTo(position.x + i, position.y, position.z, level.random.nextFloat() * 360.0F, 0.0F);
                placeOnFreeGround(level, kaiju);
                if (level.addFreshEntity(kaiju)) {
                    spawned++;
                }
            }
        }
        int result = spawned;
        source.sendSuccess(() -> Component.translatable("kn8.command.kaiju.spawned", result, species), true);
        return result;
    }

    /**
     * Chao livre mais proximo (observacao do M7a: kaiju grande nascia dentro do terreno ou caia alguns blocos):
     * sobe ate a hitbox nao colidir com blocos e depois desce ate encostar no chao, dentro de um limite curto.
     */
    private static void placeOnFreeGround(ServerLevel level, KaijuEntity kaiju) {
        int steps = 0;
        while (!level.noCollision(kaiju) && steps++ < GROUND_SEARCH_BLOCKS) {
            kaiju.setPos(kaiju.getX(), kaiju.getY() + 1.0, kaiju.getZ());
        }
        steps = 0;
        while (steps++ < GROUND_SEARCH_BLOCKS && kaiju.getY() > level.getMinBuildHeight()
                && level.noCollision(kaiju, kaiju.getBoundingBox().move(0, -1.0, 0))) {
            kaiju.setPos(kaiju.getX(), kaiju.getY() - 1.0, kaiju.getZ());
        }
    }

    private static Optional<KaijuEntity> nearest(CommandSourceStack source) {
        AABB area = AABB.ofSize(source.getPosition(), INFO_RADIUS * 2, INFO_RADIUS * 2, INFO_RADIUS * 2);
        return source.getLevel().getEntitiesOfClass(KaijuEntity.class, area).stream()
                .min(Comparator.comparingDouble(kaiju -> kaiju.distanceToSqr(source.getPosition())));
    }

    private static int info(CommandSourceStack source) {
        Optional<KaijuEntity> found = nearest(source);
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("kn8.command.kaiju.none"));
            return 0;
        }
        KaijuEntity kaiju = found.get();
        String fortitude = kaiju.def().map(KaijuDef::fortitude).map(String::valueOf).orElse("-");
        source.sendSuccess(() -> Component.translatable("kn8.command.kaiju.info", kaiju.kaijuId().toString(),
                fortitude, String.format("%.1f/%.1f", kaiju.getHealth(), kaiju.getMaxHealth()),
                String.format("%.2f", kaiju.getAttributeValue(Attributes.ATTACK_DAMAGE)),
                String.format("%.1f", kaiju.getAttributeValue(Attributes.ARMOR)),
                String.format("%.2f", kaiju.getAttributeValue(Attributes.MOVEMENT_SPEED)),
                String.format("%.1f x %.1f", kaiju.getBbWidth(), kaiju.getBbHeight()),
                kaiju.state().name()), false);
        if (kaiju.kaijuParts().length > 0) {
            String parts = String.join(", ", Arrays.stream(kaiju.kaijuParts())
                    .map(part -> part.partName() + " x" + part.multiplier() + (part.isCore() ? "*" : ""))
                    .toList());
            source.sendSuccess(() -> Component.translatable("kn8.command.kaiju.parts",
                    String.format("%.1f/%.1f", kaiju.coreHealth(), kaiju.maxCoreHealth()), parts), false);
        }
        return 1;
    }

    private static int stagger(CommandSourceStack source, int ticks) {
        Optional<KaijuEntity> found = nearest(source);
        found.ifPresent(kaiju -> kaiju.stagger(ticks));
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("kn8.command.kaiju.none"));
            return 0;
        }
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : source.getLevel().getAllEntities()) {
            if (entity instanceof KaijuEntity) {
                toRemove.add(entity);
            }
        }
        // Remove fora da iteracao para nao alterar a colecao do nivel enquanto ela e percorrida.
        toRemove.forEach(Entity::discard);
        int removed = toRemove.size();
        source.sendSuccess(() -> Component.translatable("kn8.command.kaiju.cleared", removed), true);
        return removed;
    }
}
