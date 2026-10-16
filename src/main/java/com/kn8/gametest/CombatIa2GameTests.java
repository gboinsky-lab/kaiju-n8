// src/main/java/com/kn8/gametest/CombatIa2GameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-2: ataque de kaiju passa por construcoes (quebra o que esta no ponto do golpe), so especial pesado
 * quebra com golpe pesado, Anti-Giant da Mina com 4 usos e rajada em que cada tiro conta.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatIa2GameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 2;

    private CombatIa2GameTests() {
    }

    private static int countStone(GameTestHelper helper, int x0, int x1, int z, int y0, int y1) {
        int stone = 0;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE)) {
                    stone++;
                }
            }
        }
        return stone;
    }

    /** Miguel: ataques de kaiju passam por construcoes; o golpe quebra a parede entre ele e o alvo. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void kaijuStrikeBreaksTheWallInFront(GameTestHelper helper) {
        for (int x = 5; x <= 9; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
            }
        }
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 3));
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 8));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> helper.assertTrue(
                kaiju.startAbility(KN8Constants.id("hooved_strike"), golem), "O golpe deveria comecar"));
        helper.runAfterDelay(SETTLE + 60, () -> {
            helper.assertTrue(countStone(helper, 5, 9, 6, 1, 3) < 15, "O golpe do kaiju deveria quebrar a parede");
            kaiju.discard();
            golem.discard();
            helper.succeed();
        });
    }

    /** Miguel: so os especiais pesados quebram, com golpe pesado (Kikoru, golpe pesado do machado). */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void heavySpecialBreaksBlocksAroundTheTarget(GameTestHelper helper) {
        for (int x = 6; x <= 8; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 9), Blocks.STONE);
            }
        }
        HoshinaEntity kikoru = helper.spawn(KN8Entities.KIKORU.get(), new BlockPos(7, 1, 5));
        kikoru.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 7));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> helper.assertTrue(kikoru.startTechnique("heavy_swing", golem),
                "O golpe pesado deveria comecar"));
        helper.runAfterDelay(SETTLE + 80, () -> {
            helper.assertTrue(countStone(helper, 6, 8, 9, 1, 3) < 9,
                    "O golpe pesado da Kikoru deveria quebrar os blocos perto do alvo");
            kikoru.discard();
            golem.discard();
            helper.succeed();
        });
    }

    /** Miguel: o Anti-Giant da Mina serve contra qualquer kaiju, mas so tem 4 tiros. */
    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void minaAntiGiantHasFourShots(GameTestHelper helper) {
        HoshinaEntity mina = helper.spawn(KN8Entities.MINA.get(), new BlockPos(7, 1, 1));
        mina.setNoAi(true);
        boolean[] results = new boolean[5];
        for (int i = 0; i < 5; i++) {
            int shot = i;
            helper.runAfterDelay(SETTLE + shot * 80L, () -> {
                ArmorStand target = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(7, 1, 13));
                results[shot] = mina.startTechnique("anti_giant_shot", target);
            });
        }
        helper.runAfterDelay(SETTLE + 4 * 80L + 10, () -> {
            for (int i = 0; i < 4; i++) {
                helper.assertTrue(results[i], "O tiro " + (i + 1) + " do Anti-Giant deveria sair");
            }
            helper.assertTrue(!results[4], "O quinto tiro do Anti-Giant nao deveria sair");
            mina.discard();
            helper.succeed();
        });
    }

    /** Bug da Fase 0: a rajada de varios tiros no mesmo tick somava um so; cada tiro conta. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyShotOfABurstCounts(GameTestHelper helper) {
        KaijuEntity no9 = helper.spawn(KN8Entities.KAIJU_NO9_BLACK.get(), new BlockPos(7, 1, 3));
        no9.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 9));
        golem.setNoAi(true);
        float[] before = new float[1];
        ResourceLocation id = KN8Constants.id("no9_black_multi_finger_gun");
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            Vec3 from = no9.position().add(0, 1.5, 0);
            Vec3 aim = golem.getBoundingBox().getCenter().subtract(from);
            KN8Data.ABILITY.get(id, false).ifPresent(ability -> {
                KaijuProjectile.shoot(no9, id, ability, from, aim, 3.0F);
                KaijuProjectile.shoot(no9, id, ability, from.add(0.3, 0, 0), aim, 3.0F);
                KaijuProjectile.shoot(no9, id, ability, from.add(-0.3, 0, 0), aim, 3.0F);
            });
        });
        helper.runAfterDelay(SETTLE + 40, () -> {
            float lost = before[0] - golem.getHealth();
            helper.assertTrue(lost >= 8.5F, "Os 3 tiros de 3 deveriam tirar 9 de vida (tiraram " + lost + ")");
            no9.discard();
            golem.discard();
            helper.succeed();
        });
    }
}
