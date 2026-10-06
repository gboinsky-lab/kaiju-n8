package com.kn8.common.world;

import java.util.Optional;

import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Faz kaiju surgirem por eventos (0.2): missoes, alertas e invasoes. Decisao do Miguel: kaiju NAO nascem sozinhos no
 * mundo, so por esses eventos; todo spawn de jogo passa por aqui (os comandos de teste tem o seu).
 */
public final class KaijuSpawner {

    private static final int GROUND_SEARCH_BLOCKS = 24;

    private KaijuSpawner() {
    }

    public static Optional<EntityType<KaijuEntity>> type(ResourceLocation species) {
        return KN8Entities.KAIJU.stream().filter(holder -> holder.getId().equals(species))
                .map(holder -> holder.get()).findFirst();
    }

    /** Ponto na superficie a uma distancia aleatoria entre {@code min} e {@code max} do centro. */
    public static BlockPos surfaceAround(ServerLevel level, BlockPos center, double min, double max,
            RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = min + random.nextDouble() * Math.max(0, max - min);
        int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
        int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
        return surface(level, x, z);
    }

    public static BlockPos surface(ServerLevel level, int x, int z) {
        // Carrega o chunk: o ponto pode estar fora da distancia de simulacao do jogador.
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Faz surgir um kaiju da especie no ponto (no chao livre mais proximo). Vazio se a especie nao existir. */
    public static Optional<KaijuEntity> spawn(ServerLevel level, ResourceLocation species, BlockPos pos) {
        Optional<EntityType<KaijuEntity>> type = type(species);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        KaijuEntity kaiju = type.get().create(level);
        if (kaiju == null) {
            return Optional.empty();
        }
        kaiju.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        placeOnFreeGround(level, kaiju);
        kaiju.finalizeSpawn(level, level.getCurrentDifficultyAt(kaiju.blockPosition()), MobSpawnType.EVENT, null);
        kaiju.setPersistenceRequired();
        return level.addFreshEntity(kaiju) ? Optional.of(kaiju) : Optional.empty();
    }

    /** Sobe ate a hitbox nao colidir e desce ate encostar no chao (kaiju grande nascia dentro do terreno). */
    public static void placeOnFreeGround(ServerLevel level, KaijuEntity kaiju) {
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
}
