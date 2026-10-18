package com.kn8.gametest;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.NarumiNo1Entity;
import com.kn8.core.soldier.TechniqueMath;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5F (perfil do Narumi com o Numbers 1, que le o movimento antes de acontecer): com o kaiju
 * preparando um golpe contra ele, so escolhe tecnica que acerta antes do impacto; com o impacto em cima, nao comeca
 * nenhuma (fica a esquiva e o contra-ataque).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NarumiProfileGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    private static final int TRIES = 20;
    /** Folga padrao do config ({@code [soldierAi] readWindupMargin}). */
    private static final int MARGIN = 2;

    private NarumiProfileGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_mov5f_narumi_a")
    public static void narumiNo1HitsBeforeTheImpact(GameTestHelper helper) {
        NarumiNo1Entity narumi = helper.spawn(KN8Entities.NARUMI_NO1.get(), new BlockPos(7, 1, 2));
        narumi.setNoAi(true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 8));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kaiju.startAbility(KN8Constants.id("slam"), narumi), "Pisao comecou");
            int impact = kaiju.ticksToImpact().orElse(-1);
            helper.assertTrue(impact > 0, "Pisao com impacto a frente");
            for (int i = 0; i < TRIES; i++) {
                Optional<String> chosen = narumi.chooseTechnique(kaiju);
                helper.assertTrue(chosen.isPresent(), "Deveria escolher uma tecnica rapida");
                int hit = narumi.profile().orElseThrow().techniques().get(chosen.get()).hitTick(0);
                helper.assertTrue(TechniqueMath.landsBefore(hit, 1.0, impact, MARGIN) || hit <= impact - MARGIN,
                        "A tecnica " + chosen.get() + " (golpe no tick " + hit + ") sai depois do impacto " + impact);
            }
            kaiju.discard();
            narumi.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_mov5f_narumi_b")
    public static void narumiNo1WaitsWhenTheImpactIsImminent(GameTestHelper helper) {
        NarumiNo1Entity narumi = helper.spawn(KN8Entities.NARUMI_NO1.get(), new BlockPos(7, 1, 2));
        narumi.setNoAi(true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 8));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> helper.assertTrue(kaiju.startAbility(KN8Constants.id("slam"), narumi),
                "Pisao comecou"));
        helper.succeedWhen(() -> {
            int impact = kaiju.ticksToImpact().orElse(-1);
            helper.assertTrue(impact >= 0 && impact <= MARGIN + 1, "Esperando o impacto chegar perto: " + impact);
            helper.assertTrue(narumi.chooseTechnique(kaiju).isEmpty(),
                    "Com o impacto em " + impact + " ticks nenhuma tecnica deveria comecar");
            kaiju.discard();
            narumi.discard();
        });
    }
}
