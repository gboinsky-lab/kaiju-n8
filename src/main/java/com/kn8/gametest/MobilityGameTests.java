// src/main/java/com/kn8/gametest/MobilityGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.MoveSafety;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.StuckWatch;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-1 (plano de mobilidade v3): o pouso de um impulso e conferido pelas colisoes (parede logo a
 * frente e queda grande recusam; chao livre aceita; degrau de 1 bloco sobe) e o soldado travado sai para o lado sem
 * teleporte.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobilityGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final int SETTLE = 3;

    private MobilityGameTests() {
    }

    private static SoldierEntity soldier(GameTestHelper helper, BlockPos pos) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), pos);
        soldier.setNoAi(true);
        return soldier;
    }

    /** Parede colada a leste: o impulso para leste nao tem pouso; para oeste (chao livre) tem. */
    @GameTest(template = TEMPLATE)
    public static void landingRefusesAWallAhead(GameTestHelper helper) {
        for (int y = 1; y <= 4; y++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        SoldierEntity soldier = soldier(helper, new BlockPos(4, 1, 4));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(MoveSafety.landing(soldier, new Vec3(1, 0, 0), 3.0).isEmpty(),
                    "Contra a parede o impulso nao deveria ter pouso");
            helper.assertTrue(MoveSafety.landing(soldier, new Vec3(-1, 0, 0), 3.0).isPresent(),
                    "Para o lado livre deveria ter pouso");
            helper.succeed();
        });
    }

    /** Em cima de um pilar de 5 blocos: sair dele cairia mais que a queda maxima (padrao 3). */
    @GameTest(template = TEMPLATE)
    public static void landingRefusesABigDrop(GameTestHelper helper) {
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(4, y, 4), Blocks.STONE);
        }
        SoldierEntity soldier = soldier(helper, new BlockPos(4, 6, 4));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(5 > ServerConfig.SOLDIER_LANDING_MAX_DROP.get(), "Teste supoe queda maxima < 5");
            helper.assertTrue(MoveSafety.landing(soldier, new Vec3(1, 0, 0), 3.0).isEmpty(),
                    "Pular do pilar nao deveria ter pouso");
            helper.succeed();
        });
    }

    /** Degrau de 1 bloco a frente: o impulso sobe e pousa em cima. */
    @GameTest(template = TEMPLATE)
    public static void landingClimbsAOneBlockStep(GameTestHelper helper) {
        for (int x = 5; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        SoldierEntity soldier = soldier(helper, new BlockPos(4, 1, 4));
        helper.runAfterDelay(SETTLE, () -> {
            Vec3 land = MoveSafety.landing(soldier, new Vec3(1, 0, 0), 3.0).orElse(null);
            helper.assertTrue(land != null && land.y >= soldier.getY() + 0.9, "Deveria pousar em cima do degrau: "
                    + land);
            helper.succeed();
        });
    }

    /** Querendo andar e sem sair do lugar pelo tempo de travamento, o soldado anda para o lado (sem teleporte). */
    @GameTest(template = "empty_15x12x15", timeoutTicks = 200)
    public static void stuckSoldierStepsAside(GameTestHelper helper) {
        // Area maior: o ponto ao lado fica a [soldierAi] unstuckStep (5) blocos.
        SoldierEntity soldier = soldier(helper, new BlockPos(7, 1, 7));
        StuckWatch watch = new StuckWatch();
        Vec3[] start = new Vec3[1];
        boolean[] out = new boolean[1];
        helper.runAfterDelay(SETTLE, () -> start[0] = soldier.position());
        helper.onEachTick(() -> {
            if (start[0] != null && !out[0]) {
                // Mob sem IA nao atualiza o "no chao" e a navegacao vanilla so faz caminho no chao.
                soldier.setOnGround(true);
                out[0] = watch.tick(soldier, true, soldier.position().add(0, 0, 6));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(out[0], "Deveria detectar o travamento");
            helper.assertTrue(soldier.position().distanceTo(start[0]) < 0.01,
                    "Nao pode teleportar: so a navegacao move o soldado");
            helper.assertTrue(soldier.getNavigation().getPath() != null, "Deveria ter caminho para o lado");
        });
    }
}
