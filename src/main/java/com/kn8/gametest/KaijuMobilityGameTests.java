package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-6 (kaiju): golpe nao atravessa parede (e o golpe barrado nao conta como falha), habilidade que
 * falha varias vezes seguidas descansa, e o salto so sai com o arco livre (teto barra).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuMobilityGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final ResourceLocation BITE = KN8Constants.id("bite");
    private static final int SETTLE = 2;
    /** Preparo + ativo da mordida (6 + 2) com folga. */
    private static final int BITE_TICKS = 10;

    private KaijuMobilityGameTests() {
    }

    private static KaijuEntity ant(GameTestHelper helper, BlockPos pos) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.CAMPONOTUS.get(), pos);
        kaiju.setNoAi(true);
        kaiju.setYRot(270.0F);
        kaiju.setYBodyRot(270.0F);
        kaiju.yBodyRotO = 270.0F;
        return kaiju;
    }

    private static Cow cow(GameTestHelper helper, Vec3 at) {
        Cow cow = helper.spawn(EntityType.COW, at);
        cow.setNoAi(true);
        return cow;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_mov6_a")
    public static void wallStopsTheBiteAndDoesNotCountAsFailure(GameTestHelper helper) {
        KaijuEntity kaiju = ant(helper, new BlockPos(3, 1, 7));
        // Grade de ferro (parede fina) no bloco x = 8, a borda do kaiju logo antes e a vaca logo depois: ao alcance
        // pela distancia (0,2 bloco), so a grade no meio.
        double half = kaiju.getBbWidth() / 2.0;
        Vec3 at = helper.absoluteVec(new Vec3(8.4 - half, 1, 7.5));
        kaiju.moveTo(at.x, at.y, at.z, 270.0F, 0.0F);
        int wallX = 8;
        for (int y = 1; y <= 4; y++) {
            for (int z = 5; z <= 9; z++) {
                helper.setBlock(new BlockPos(wallX, y, z), Blocks.IRON_BARS);
            }
        }
        Cow cow = cow(helper, new Vec3(9.06, 1, 7.5));
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kaiju.inMeleeReach(cow), "A vaca deveria estar ao alcance pela distancia");
            helper.assertFalse(kaiju.strikeClear(cow), "Parede entre o kaiju e a vaca");
            before[0] = cow.getHealth();
            helper.assertTrue(kaiju.startAbility(BITE, cow), "A mordida deveria comecar");
        });
        helper.runAfterDelay(SETTLE + BITE_TICKS, () -> {
            helper.assertTrue(cow.getHealth() == before[0], "A mordida atravessou a parede: " + cow.getHealth());
            helper.assertTrue(kaiju.failureStreak(BITE) == 0, "Golpe barrado nao conta como falha");
            for (int y = 1; y <= 4; y++) {
                for (int z = 5; z <= 9; z++) {
                    helper.setBlock(new BlockPos(wallX, y, z), Blocks.AIR);
                }
            }
            helper.assertTrue(kaiju.strikeClear(cow), "Sem a parede o caminho fica livre");
            helper.assertTrue(kaiju.startAbility(BITE, cow), "Segunda mordida");
        });
        helper.runAfterDelay(SETTLE + BITE_TICKS * 2, () -> {
            helper.assertTrue(cow.getHealth() < before[0], "Sem parede a mordida fere");
            kaiju.discard();
            cow.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "kn8_mov6_b")
    public static void abilityRestsAfterRepeatedMisses(GameTestHelper helper) {
        KaijuEntity kaiju = ant(helper, new BlockPos(3, 1, 7));
        // Vaca longe: a mordida sai no vazio.
        Cow cow = cow(helper, new Vec3(13.5, 1, 7.5));
        long[] start = new long[1];
        for (int i = 0; i < 3; i++) {
            int use = i;
            helper.runAfterDelay(SETTLE + use * BITE_TICKS, () -> {
                if (use == 0) {
                    start[0] = helper.getLevel().getGameTime();
                }
                helper.assertTrue(kaiju.startAbility(BITE, cow), "Mordida " + (use + 1) + " deveria comecar");
            });
        }
        helper.runAfterDelay(SETTLE + 2 * BITE_TICKS + BITE_TICKS - 1, () -> {
            helper.assertTrue(kaiju.failureStreak(BITE) == 0, "Contagem zera ao descansar");
            long rest = kaiju.abilityReadyTick(BITE) - helper.getLevel().getGameTime();
            helper.assertTrue(rest > 100, "Depois de 3 falhas seguidas a mordida descansa: " + rest + " ticks");
            kaiju.discard();
            cow.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov6_c")
    public static void ceilingBlocksTheLeap(GameTestHelper helper) {
        KaijuEntity kaiju = ant(helper, new BlockPos(3, 1, 7));
        Cow cow = cow(helper, new Vec3(12.5, 1, 7.5));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kaiju.canLeapAt(cow), "Sem teto o salto tem arco livre");
            int roof = Mth.ceil(helper.relativeVec(new Vec3(0, kaiju.getBoundingBox().maxY, 0)).y) + 1;
            for (int x = 0; x < 15; x++) {
                for (int z = 3; z <= 11; z++) {
                    helper.setBlock(new BlockPos(x, roof, z), Blocks.BEDROCK);
                }
            }
            helper.assertFalse(kaiju.canLeapAt(cow), "Teto logo acima: o salto nao sai");
            kaiju.discard();
            cow.discard();
            helper.succeed();
        });
    }
}
