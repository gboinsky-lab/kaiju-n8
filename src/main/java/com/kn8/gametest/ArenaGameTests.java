// src/main/java/com/kn8/gametest/ArenaGameTests.java
package com.kn8.gametest;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.arena.TrainingArenaService;
import com.kn8.common.destruction.ProtectedAreas;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Arena de treino (construcoes C2): o painel solta um kaiju de treino no centro, protege a arena, o segura dentro
 * dela e, na morte, ele nao vira carcaca. Geometria pequena so para caber no template do teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArenaGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final TrainingArenaService.Geometry SMALL = new TrainingArenaService.Geometry(6, 5, 5, 8, 1);
    private static final BlockPos CONSOLE = new BlockPos(7, 1, 1);

    private ArenaGameTests() {
    }

    private static KaijuEntity start(GameTestHelper helper) {
        Optional<KaijuEntity> kaiju = TrainingArenaService.start(helper.getLevel(), helper.absolutePos(CONSOLE),
                Direction.SOUTH, KN8Constants.id("camponotus"), SMALL);
        helper.assertTrue(kaiju.isPresent(), "O painel deveria soltar o kaiju de treino");
        kaiju.get().setNoAi(true);
        return kaiju.get();
    }

    private static void cleanup(GameTestHelper helper) {
        ProtectedAreas.get(helper.getLevel()).remove(TrainingArenaService.areaName(helper.absolutePos(CONSOLE)));
    }

    /** Solta no centro (6 blocos a frente), marca como treino, protege a arena; encerrar tira o kaiju. */
    @GameTest(template = TEMPLATE, batch = "kn8_arena")
    public static void consoleStartsAndEndsTraining(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuEntity kaiju = start(helper);
        BlockPos center = helper.absolutePos(CONSOLE).relative(Direction.SOUTH, 6);
        helper.assertTrue(TrainingArenaService.isTraining(kaiju), "Kaiju deveria estar marcado como de treino");
        helper.assertTrue(kaiju.blockPosition().closerThan(center, 3.0), "Kaiju deveria nascer no centro");
        helper.assertTrue(ProtectedAreas.get(level).isProtected(center), "A arena deveria ficar protegida");
        List<KaijuEntity> active = TrainingArenaService.active(level, helper.absolutePos(CONSOLE), Direction.SOUTH,
                SMALL);
        helper.assertTrue(active.size() == 1, "Um treino ativo, tem " + active.size());
        active.forEach(k -> TrainingArenaService.vanish(level, k));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(TrainingArenaService.active(level, helper.absolutePos(CONSOLE), Direction.SOUTH, SMALL)
                    .isEmpty(), "Encerrar deveria tirar o kaiju");
            cleanup(helper);
            helper.succeed();
        });
    }

    /** Kaiju de treino fora da arena volta ao centro. */
    @GameTest(template = TEMPLATE, batch = "kn8_arena")
    public static void trainingKaijuIsLeashed(GameTestHelper helper) {
        KaijuEntity kaiju = start(helper);
        BoundingBox box = TrainingArenaService.boxOf(kaiju).orElseThrow();
        kaiju.teleportTo(box.maxX() + 6.0, kaiju.getY(), box.maxZ() + 6.0);
        helper.assertFalse(box.isInside(kaiju.blockPosition()), "O kaiju deveria estar fora da arena");
        TrainingArenaService.leash(kaiju, box);
        helper.assertTrue(box.isInside(kaiju.blockPosition()), "O kaiju deveria voltar para a arena");
        kaiju.discard();
        cleanup(helper);
        helper.succeed();
    }

    /** Kaiju de treino morto some sem carcaca (nem desmonte, nem material). */
    @GameTest(template = TEMPLATE, batch = "kn8_arena")
    public static void trainingKaijuLeavesNoCarcass(GameTestHelper helper) {
        KaijuEntity kaiju = start(helper);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20.0);
        // Mata depois do primeiro tick (a definicao do kaiju, aplicada no tick, devolveria a vida), como no teste de
        // carcaca.
        helper.runAfterDelay(2, kaiju::kill);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(CarcassEntity.class, area).isEmpty(),
                    "Kaiju de treino nao deixa carcaca");
            helper.assertTrue(kaiju.isRemoved(), "O kaiju deveria sumir");
            cleanup(helper);
            helper.succeed();
        });
    }
}
