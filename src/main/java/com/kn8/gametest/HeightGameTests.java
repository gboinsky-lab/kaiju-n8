// src/main/java/com/kn8/gametest/HeightGameTests.java
package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tabela mestra de alturas (Miguel, 2026-10-10): a altura do JSON vira o atributo {@code scale}, que muda a hitbox
 * (e o desenho) junto; partes do kaiju acompanham; a fusao de 5 m vira a gigante de 20 m perdendo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeightGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final float TOLERANCE = 0.02F;
    private static final double AREA = 24.0;

    private HeightGameTests() {
    }

    /** Primigenius: modelo de 6 m com escala 0,917 = 5,5 m; a parte mais alta desce junto. */
    @GameTest(template = TEMPLATE)
    public static void primigeniusIsFiveAndAHalfTall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity kaiju = KaijuSpawner.spawn(level, KN8Constants.id("primigenius"), center).orElseThrow();
        kaiju.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(kaiju.getBbHeight() - 5.5F) < TOLERANCE,
                    "Primigenius deveria ter 5,5 de altura, tem " + kaiju.getBbHeight());
            double top = 0.0;
            for (KaijuPart part : kaiju.kaijuParts()) {
                top = Math.max(top, part.getBoundingBox().maxY - kaiju.getY());
            }
            helper.assertTrue(top <= 5.5 + 0.6, "As partes deveriam encolher com o corpo (topo " + top + ")");
            kaiju.discard();
            helper.succeed();
        });
    }

    /** Hoshina: modelo de 1,85 m, altura da tabela 1,72 (height no special_soldier/hoshina.json). */
    @GameTest(template = TEMPLATE)
    public static void hoshinaIsOneSeventyTwo(GameTestHelper helper) {
        HoshinaEntity hoshina = helper.spawn(KN8Entities.HOSHINA.get(), new BlockPos(4, 1, 4));
        hoshina.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(hoshina.getBbHeight() - 1.72F) < TOLERANCE,
                    "Hoshina deveria ter 1,72, tem " + hoshina.getBbHeight());
            hoshina.discard();
            helper.succeed();
        });
    }

    /** Soldado comum: a altura segue o nivel (recruta 1,70 ... elite 1,85). */
    @GameTest(template = TEMPLATE)
    public static void soldierHeightFollowsTheLevel(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 4));
        soldier.setNoAi(true);
        soldier.setPowerLevel("low");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(soldier.getBbHeight() - 1.70F) < TOLERANCE,
                    "Recruta deveria ter 1,70, tem " + soldier.getBbHeight());
            soldier.setPowerLevel("elite");
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(Math.abs(soldier.getBbHeight() - 1.85F) < TOLERANCE,
                    "Elite deveria ter 1,85, tem " + soldier.getBbHeight());
            soldier.discard();
            helper.succeed();
        });
    }

    /** A fusao de 5 m abaixo de 50% da vida vira a fusao gigante de 20 m, com a vida cheia. */
    @GameTest(template = TEMPLATE, batch = "kn8_fusion_giant", timeoutTicks = 100)
    public static void fusionTurnsGiantWhenLosing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity fusion = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9_fusion"), center).orElseThrow();
        fusion.setNoAi(true);
        helper.runAfterDelay(5, () -> fusion.setHealth(fusion.getMaxHealth() * 0.4F));
        helper.runAfterDelay(15, () -> {
            List<KaijuEntity> giants = level.getEntitiesOfClass(KaijuEntity.class, new AABB(center).inflate(AREA),
                    kaiju -> kaiju.isAlive() && kaiju.kaijuId().equals(KN8Constants.id("kaiju_no9_fusion_giant")));
            helper.assertTrue(fusion.isRemoved(), "A fusao deveria sumir ao crescer");
            helper.assertTrue(giants.size() == 1, "Deveria surgir 1 fusao gigante, vieram " + giants.size());
            KaijuEntity giant = giants.get(0);
            helper.assertTrue(Math.abs(giant.getBbHeight() - 20.0F) < 0.1F,
                    "A fusao gigante deveria ter 20 de altura, tem " + giant.getBbHeight());
            helper.assertTrue(giant.getHealth() == giant.getMaxHealth(), "A fusao gigante nasce com a vida cheia");
            level.getEntitiesOfClass(Entity.class, new AABB(center).inflate(AREA),
                    entity -> entity instanceof KaijuEntity).forEach(Entity::discard);
            helper.succeed();
        });
    }
}
