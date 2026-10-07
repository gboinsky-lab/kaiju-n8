package com.kn8.gametest;

import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.2 (Etapa 7): a invasao comeca em aviso, nao deixa outra comecar na mesma dimensao, solta a onda com
 * a quantidade do JSON e passa ao intervalo quando a onda morre. Lote proprio: os kaiju surgem a 36-56 blocos e
 * atrapalhariam os testes vizinhos.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InvasionGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation SWARM = KN8Constants.id("spider_swarm");
    private static final double CLEANUP_RADIUS = 96.0;
    private static final int FORCED_CHUNK_RADIUS = 4;

    private InvasionGameTests() {
    }

    private static void forceChunks(ServerLevel level, BlockPos center, boolean forced) {
        int radius = FORCED_CHUNK_RADIUS;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setChunkForced((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, forced);
            }
        }
    }

    private static void killAlive(ServerLevel level, Invasion invasion) {
        for (UUID uuid : List.copyOf(invasion.alive())) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                entity.kill();
            }
        }
    }

    @GameTest(template = TEMPLATE, batch = "kn8_invasion", timeoutTicks = 200)
    public static void invasionRunsWavesAndBreaks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        // Sem jogador, os chunks a 36-56 blocos descarregam e o kaiju some de level.getEntity: forca o carregamento.
        forceChunks(level, center, true);
        helper.assertTrue(InvasionService.start(level, SWARM, center) == InvasionService.Result.OK,
                "A invasao deveria comecar");
        helper.assertTrue(InvasionService.start(level, SWARM, center) == InvasionService.Result.ALREADY_ACTIVE,
                "Uma segunda invasao na mesma dimensao deveria ser recusada");
        helper.assertTrue(InvasionService.active(level).map(Invasion::phase).orElse(null) == Invasion.Phase.WARNING,
                "A invasao comeca em aviso (sirene)");
        InvasionService.skip(level);

        helper.runAfterDelay(30, () -> {
            Invasion invasion = InvasionService.active(level).orElseThrow();
            helper.assertTrue(invasion.phase() == Invasion.Phase.FIGHT, "Depois do aviso vem a onda");
            helper.assertTrue(invasion.alive().size() == 3, "A primeira onda tem 3 Trichonephila, vieram "
                    + invasion.alive().size());
            killAlive(level, invasion);
        });
        // Um kaiju pode terminar de carregar depois do primeiro abate: segunda passada antes de conferir.
        helper.runAfterDelay(50, () -> InvasionService.active(level).ifPresent(invasion -> killAlive(level,
                invasion)));
        helper.runAfterDelay(85, () -> {
            Invasion invasion = InvasionService.active(level).orElseThrow();
            helper.assertTrue(invasion.phase() == Invasion.Phase.BREAK, "Onda eliminada: intervalo antes da proxima"
                    + " (fase " + invasion.phase() + ", vivos " + invasion.alive().size() + ", abatidos "
                    + invasion.killed() + ", achados " + invasion.alive().stream().filter(id -> level.getEntity(id)
                    != null).count() + ")");
            helper.assertTrue(invasion.killed() == 3, "Deveria contar 3 abates, contou " + invasion.killed());
            helper.assertTrue(invasion.wave() == 1, "A proxima onda e a 2");
            InvasionService.stop(level);
            helper.assertTrue(InvasionService.active(level).isEmpty(), "stop deveria encerrar a invasao");
            AABB area = new AABB(center).inflate(CLEANUP_RADIUS);
            level.getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity
                    || entity instanceof CarcassEntity || entity instanceof SoldierEntity).forEach(Entity::discard);
            forceChunks(level, center, false);
            helper.succeed();
        });
    }
}
