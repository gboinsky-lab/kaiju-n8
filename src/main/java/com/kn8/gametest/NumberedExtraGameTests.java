package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NUMERADOS-C: o que faltava dos numerados do anime. O No. 12 se adapta ao golpe repetido e regenera; a forma Kikoru
 * do No. 15 revela a forma kaiju com a vida baixa; o No. 11 bate mais forte com agua perto e mais fraco seco.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NumberedExtraGameTests {

    private static final String TEMPLATE = "empty_15x12x15";

    private NumberedExtraGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_no12_adapta", timeoutTicks = 200)
    public static void no12AdaptsToRepeatedHitsAndRegenerates(GameTestHelper helper) {
        KaijuEntity no12 = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("kaiju_no12"),
                helper.absolutePos(new BlockPos(4, 1, 7))).orElseThrow();
        no12.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(11, 1, 7));
        golem.setNoAi(true);
        float[] health = new float[1];
        helper.runAfterDelay(3, () -> {
            no12.setHealth(no12.getMaxHealth() * 0.4F);
            DamageSource source = helper.getLevel().damageSources().mobAttack(golem);
            float first = hit(no12, source);
            for (int i = 0; i < 8; i++) {
                hit(no12, source);
            }
            float last = hit(no12, source);
            helper.assertTrue(first > 0.0F && last < first * 0.85F,
                    "Golpe repetido tira menos: primeiro " + first + ", decimo " + last);
            health[0] = no12.getHealth();
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(no12.getHealth() > health[0] + 1.0F,
                    "Regenera abaixo de 50%: " + health[0] + " -> " + no12.getHealth());
            no12.discard();
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_no15_disfarce", timeoutTicks = 100)
    public static void no15DisguiseRevealsTheTrueForm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(7, 1, 7));
        KaijuEntity disguise = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no15_kikoru"), center)
                .orElseThrow();
        disguise.setNoAi(true);
        helper.runAfterDelay(3, () -> disguise.setHealth(disguise.getMaxHealth() * 0.4F));
        helper.succeedWhen(() -> {
            List<KaijuEntity> revealed = level.getEntitiesOfClass(KaijuEntity.class, new AABB(center).inflate(12.0),
                    kaiju -> kaiju.isAlive() && kaiju.kaijuId().equals(KN8Constants.id("kaiju_no15")));
            helper.assertTrue(revealed.size() == 1, "A forma kaiju deveria surgir, vieram " + revealed.size());
            helper.assertTrue(!disguise.isAlive() || disguise.isRemoved(), "O disfarce some");
            KaijuEntity no15 = revealed.get(0);
            helper.assertTrue(Math.abs(no15.getHealth() / no15.getMaxHealth() - 0.5F) < 0.02F,
                    "Forma kaiju com metade da vida: " + no15.getHealth() + "/" + no15.getMaxHealth());
            no15.discard();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_no11_agua", timeoutTicks = 100)
    public static void no11HitsHarderNearWater(GameTestHelper helper) {
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        KaijuEntity no11 = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("kaiju_no11"),
                helper.absolutePos(new BlockPos(4, 1, 7))).orElseThrow();
        no11.setNoAi(true);
        float[] dry = new float[1];
        helper.runAfterDelay(3, () -> {
            dry[0] = no11.waterDamageFactor();
            helper.assertTrue(Math.abs(dry[0] - 0.85F) < 0.01F, "Seco: x0,85, veio " + dry[0]);
            for (int x = 7; x <= 9; x++) {
                helper.setBlock(new BlockPos(x, 1, 7), Blocks.WATER);
            }
        });
        helper.runAfterDelay(30, () -> {
            float wet = no11.waterDamageFactor();
            helper.assertTrue(Math.abs(wet - 1.3F) < 0.01F, "Com agua a 3 blocos: x1,3, veio " + wet + " em " + no11.blockPosition() + ", agua em "
                    + helper.absolutePos(new BlockPos(7, 1, 7)) + " "
                    + helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(7, 1, 7))).getType());
            no11.discard();
            helper.succeed();
        });
    }

    private static float hit(KaijuEntity kaiju, DamageSource source) {
        float before = kaiju.getHealth();
        kaiju.invulnerableTime = 0;
        kaiju.hurt(source, 30.0F);
        return before - kaiju.getHealth();
    }
}
