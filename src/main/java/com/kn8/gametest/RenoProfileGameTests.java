package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.RenoNo6Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5E (perfil do Reno, "congela, paralisa e expoe pontos fracos para os aliados"): kaiju paralisado
 * (Lentidao alta) conta como brecha; a explosao congelante do Reno No. 6 o paralisa; com o alvo paralisado o Reno nao
 * se esconde (pressiona).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RenoProfileGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;

    private RenoProfileGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5e_reno_a")
    public static void paralyzedKaijuCountsAsOpening(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 7));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            kaiju.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            helper.assertFalse(kaiju.isOpen(), "Lentidao II nao e paralisia");
            kaiju.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 6));
            helper.assertTrue(kaiju.isParalyzed() && kaiju.isOpen(), "Lentidao VII: paralisado e aberto");
            kaiju.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160, batch = "kn8_mov5e_reno_b")
    public static void freezeBurstParalyzes(GameTestHelper helper) {
        RenoNo6Entity reno = helper.spawn(KN8Entities.RENO_NO6.get(), new BlockPos(1, 1, 7));
        reno.setNoAi(true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(11, 1, 7));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            reno.lookAt(kaiju, 360.0F, 90.0F);
            helper.assertTrue(reno.startTechnique("freeze_burst", kaiju), "Explosao congelante comecou");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(kaiju.isParalyzed(), "O kaiju deveria ficar paralisado");
            helper.assertTrue(kaiju.isOpen(), "Paralisado conta como brecha para os aliados");
            kaiju.discard();
            reno.discard();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5e_reno_c")
    public static void renoPressesAParalyzedShooter(GameTestHelper helper) {
        RenoNo6Entity reno = helper.spawn(KN8Entities.RENO_NO6.get(), new BlockPos(7, 1, 12));
        reno.setNoAi(true);
        KaijuEntity ant = helper.spawn(KN8Entities.CAMPONOTUS.get(), new BlockPos(7, 1, 2));
        ant.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            reno.setHealth(reno.getMaxHealth() * 0.3F);
            helper.assertTrue(reno.wantsCoverFrom(ant), "Sob pressao contra a formiga: quer cobertura");
            ant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 6));
            helper.assertFalse(reno.wantsCoverFrom(ant), "Formiga paralisada: pressiona em vez de se esconder");
            ant.discard();
            reno.discard();
            helper.succeed();
        });
    }
}
