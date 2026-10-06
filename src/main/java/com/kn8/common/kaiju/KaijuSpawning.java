// src/main/java/com/kn8/common/kaiju/KaijuSpawning.java
package com.kn8.common.kaiju;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.server.KN8Server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Spawn natural de kaiju (M8). ONDE e com que peso: biome modifiers do datapack
 * ({@code data/kn8/neoforge/biome_modifier/*.json}, registry de worldgen, como decidido no PT2). SE pode nascer:
 * este predicado, com as regras do config e do JSON da especie:
 * <ul>
 *   <li>{@code spawn.natural} no config E {@code spawn.natural} no JSON da especie;</li>
 *   <li>nunca no pacifico;</li>
 *   <li>superficie ({@code spawn.surfaceOnly}) e luz de monstro vanilla ({@code spawn.requireDarkness}): regras
 *   da Fase 4, secao 8.3 (o M8 nao checava luz e nascia de dia; corrigido no M9);</li>
 *   <li>limites {@code spawn.maxKaijuPerChunk} e {@code spawn.maxKaijuPerLevel};</li>
 *   <li>regras vanilla de chao valido.</li>
 * </ul>
 * Spawn por comando, ovo ou evento ignora os limites naturais (so precisa de chao valido).
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class KaijuSpawning {

    private KaijuSpawning() {
    }

    /** Mod bus: substitui o predicado de cada especie. */
    public static void registerPlacements(RegisterSpawnPlacementsEvent event) {
        KN8Entities.KAIJU.forEach(type -> event.register(type.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, KaijuSpawning::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    static boolean canSpawn(EntityType<KaijuEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (!ServerConfig.SPEC.isLoaded() || level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        boolean natural = spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
        if (natural && !naturalAllowed(type, level, pos)) {
            return false;
        }
        return Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    private static boolean naturalAllowed(EntityType<KaijuEntity> type, ServerLevelAccessor level, BlockPos pos) {
        if (!ServerConfig.NATURAL_SPAWN.get()) {
            return false;
        }
        boolean speciesNatural = KN8Data.KAIJU.get(BuiltInRegistries.ENTITY_TYPE.getKey(type), false)
                .map(KaijuDef::spawn).map(KaijuDef.Spawn::natural).orElse(false);
        if (!speciesNatural) {
            return false;
        }
        if (ServerConfig.SPAWN_SURFACE_ONLY.get() && !level.canSeeSky(pos)) {
            return false;
        }
        if (ServerConfig.SPAWN_REQUIRE_DARKNESS.get() && !Monster.isDarkEnoughToSpawn(level, pos, level.getRandom())) {
            return false;
        }
        ServerLevel server = level.getLevel();
        KN8Server services = KN8Server.getOrNull(server.getServer());
        if (services != null && services.kaijuCount(server.dimension()) >= ServerConfig.MAX_KAIJU_PER_LEVEL.get()) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(pos);
        AABB column = new AABB(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunk.getMaxBlockZ() + 1);
        return level.getEntitiesOfClass(KaijuEntity.class, column).size() < ServerConfig.MAX_KAIJU_PER_CHUNK.get();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof KaijuEntity && event.getLevel() instanceof ServerLevel level) {
            KN8Server services = KN8Server.getOrNull(level.getServer());
            if (services != null) {
                services.adjustKaijuCount(level.dimension(), 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof KaijuEntity && event.getLevel() instanceof ServerLevel level) {
            KN8Server services = KN8Server.getOrNull(level.getServer());
            if (services != null) {
                services.adjustKaijuCount(level.dimension(), -1);
            }
        }
    }
}
