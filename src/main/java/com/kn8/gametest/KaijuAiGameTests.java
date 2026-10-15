// src/main/java/com/kn8/gametest/KaijuAiGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-1A: escolha de alvo (comum = mais perto, sem ficar preso ao primeiro; inteligente = atirador
 * primeiro) e atordoado parado. Soldados sem IA: so servem de alvo (invulneravel nao conta
 * como inimigo no vanilla, {@code canBeSeenAsEnemy}).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuAiGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int STAGGER_TICKS = 60;
    private static final double STILL_DISTANCE = 0.3;

    private KaijuAiGameTests() {
    }

    private static SoldierEntity dummy(GameTestHelper helper, BlockPos pos, String variant) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), pos);
        soldier.setVariant(variant);
        soldier.setNoAi(true);
        return soldier;
    }

    /** Miguel: o kaiju ataca quem esta perto, nao corre atras de um so para sempre. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void commonKaijuSwitchesToTheNearestSoldier(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 3));
        SoldierEntity far = dummy(helper, new BlockPos(7, 1, 13), "rifle");
        SoldierEntity near = dummy(helper, new BlockPos(7, 1, 7), "sword");
        kaiju.setTarget(far);
        helper.succeedWhen(() -> {
            helper.assertTrue(kaiju.getTarget() == near, "O kaiju comum deveria trocar para o soldado mais perto");
            kaiju.discard();
            far.discard();
            near.discard();
        });
    }

    /** Miguel: o inteligente elimina por funcao (atirador de longe mesmo com alguem de espada perto). */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void smartKaijuGoesForTheShooter(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.KAIJU_NO10_SMALL.get(), new BlockPos(7, 1, 3));
        SoldierEntity swordsman = dummy(helper, new BlockPos(7, 1, 6), "sword");
        SoldierEntity shooter = dummy(helper, new BlockPos(7, 1, 13), "rifle");
        kaiju.setTarget(swordsman);
        helper.succeedWhen(() -> {
            helper.assertTrue(kaiju.isSmart(), "O No. 10 (inteligencia 4) deveria ser inteligente");
            helper.assertTrue(kaiju.getTarget() == shooter, "O No. 10 deveria priorizar o atirador");
            kaiju.discard();
            swordsman.discard();
            shooter.discard();
        });
    }

    /** Miguel: atordoado fica parado alguns segundos e depois volta. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void staggeredKaijuStandsStillThenResumes(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 3));
        SoldierEntity target = dummy(helper, new BlockPos(7, 1, 13), "rifle");
        kaiju.setTarget(target);
        Vec3[] start = new Vec3[1];
        helper.runAfterDelay(5, () -> {
            kaiju.stagger(STAGGER_TICKS);
            start[0] = kaiju.position();
        });
        helper.runAfterDelay(5 + STAGGER_TICKS - 5, () -> helper.assertTrue(
                horizontal(kaiju.position(), start[0]) < STILL_DISTANCE, "Atordoado deveria ficar parado"));
        helper.runAfterDelay(5 + STAGGER_TICKS + 40, () -> {
            helper.assertTrue(horizontal(kaiju.position(), start[0]) > 1.0,
                    "Depois do atordoamento o kaiju deveria voltar a perseguir");
            kaiju.discard();
            target.discard();
            helper.succeed();
        });
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
