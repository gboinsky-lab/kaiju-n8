// src/main/java/com/kn8/gametest/MobilityGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.MoveSafety;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SoldierLeap;
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
 * teleporte. MOV-2: o salto tatico sobe em plataforma de 2, atravessa vao de 3 e recusa muro alto demais.
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

    private static final String BIG = "empty_15x12x15";
    private static final String LEAP_BATCH = "kn8_mov2_leap";

    /** Soldado com IA (mob sem IA nao anda pela velocidade) e sem alvo, para o salto ser so o do teste. */
    private static SoldierEntity leaper(GameTestHelper helper, BlockPos pos) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), pos);
        soldier.goalSelector.removeAllGoals(goal -> true);
        soldier.targetSelector.removeAllGoals(goal -> true);
        return soldier;
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /** Salta, segura o controle no ar como o goal de combate e confere o pouso. */
    private static void leapAndCheck(GameTestHelper helper, SoldierEntity soldier, Vec3 toward, double minY,
            double minX, String what) {
        SoldierLeap leap = new SoldierLeap();
        boolean[] launched = new boolean[1];
        double[] startY = new double[1];
        helper.runAfterDelay(10, () -> {
            startY[0] = soldier.getY();
            launched[0] = leap.tryLeap(soldier, helper.absoluteVec(toward));
            helper.assertTrue(launched[0], "Deveria saltar " + what);
        });
        helper.onEachTick(() -> {
            if (launched[0] && leap.active()) {
                leap.tickAir(soldier);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(launched[0] && !leap.active(), "Ainda no ar");
            Vec3 at = helper.relativeVec(soldier.position());
            helper.assertTrue(soldier.onGround() && at.y >= minY - 0.01 && at.x >= minX,
                    "Deveria pousar " + what + ": " + at);
            helper.assertTrue(soldier.getHealth() >= soldier.getMaxHealth(), "O salto nao pode ferir");
        });
    }

    /** Plataforma de 2 blocos colada a leste: sobe em cima dela. */
    @GameTest(template = BIG, batch = LEAP_BATCH, timeoutTicks = 100)
    public static void soldierLeapsOntoATwoBlockLedge(GameTestHelper helper) {
        fill(helper, 5, 1, 0, 14, 2, 14);
        SoldierEntity soldier = leaper(helper, new BlockPos(4, 1, 7));
        leapAndCheck(helper, soldier, new Vec3(12.5, 3, 7.5), 3.0, 5.0, "em cima da plataforma");
    }

    /** Vao de 3 blocos e 5 de fundo (mais que a queda maxima): salta para o outro lado. */
    @GameTest(template = BIG, batch = LEAP_BATCH, timeoutTicks = 100)
    public static void soldierLeapsAcrossAThreeBlockGap(GameTestHelper helper) {
        fill(helper, 0, 1, 0, 5, 5, 14);
        fill(helper, 9, 1, 0, 14, 5, 14);
        SoldierEntity soldier = leaper(helper, new BlockPos(5, 6, 7));
        leapAndCheck(helper, soldier, new Vec3(12.5, 6, 7.5), 6.0, 8.7, "do outro lado do vao");
    }

    /** Muro de 5 (acima de [soldierAi] leapMaxHeight 3): nao salta. */
    @GameTest(template = BIG, batch = LEAP_BATCH)
    public static void soldierDoesNotLeapATooHighWall(GameTestHelper helper) {
        fill(helper, 5, 1, 0, 14, 5, 14);
        SoldierEntity soldier = leaper(helper, new BlockPos(4, 1, 7));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(ServerConfig.SOLDIER_LEAP_MAX_HEIGHT.get() < 5.0, "Teste supoe salto maximo < 5");
            helper.assertFalse(new SoldierLeap().tryLeap(soldier, helper.absoluteVec(new Vec3(12.5, 6, 7.5))),
                    "Muro de 5 nao deveria ter salto");
            helper.succeed();
        });
    }
}
