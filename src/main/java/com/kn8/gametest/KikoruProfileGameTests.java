package com.kn8.gametest;

import java.util.Optional;
import java.util.Set;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.No9Service;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.KikoruEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5C (perfil da Kikoru, plano de mobilidade v3 §7: "alternar golpes rapidos e comprometidos"):
 * depois de um golpe pesado vem um rapido e depois de um rapido vem um pesado; o No. 9 abaixo da metade da vida conta
 * como regenerando (a Kikoru pressiona).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KikoruProfileGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    /** Tecnicas pesadas do kikoru.json (bloco heavy). */
    private static final Set<String> HEAVY = Set.of("heavy_swing", "shockwave", "ground_smash", "guard_break");

    private KikoruProfileGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "kn8_mov5c_kikoru")
    public static void kikoruAlternatesFastAndHeavy(GameTestHelper helper) {
        KikoruEntity kikoru = helper.spawn(KN8Entities.KIKORU.get(), new BlockPos(5, 1, 7));
        kikoru.setNoAi(true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(9, 1, 7));
        kaiju.setNoAi(true);
        int[] step = {0};
        helper.runAfterDelay(SETTLE, () -> {
            kikoru.lookAt(kaiju, 360.0F, 90.0F);
            helper.assertTrue(kikoru.startTechnique("ground_smash", kaiju), "Golpe no chao (pesado) comecou");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!kikoru.isUsingTechnique(), "Tecnica em andamento");
            if (step[0] == 0) {
                // Depois do pesado: o golpe rapido ganha o bonus de alternar e passa os pesados empatados.
                Optional<String> next = kikoru.chooseTechnique(kaiju);
                helper.assertTrue(next.equals(Optional.of("axe_slash")), "Depois do pesado, o rapido: " + next);
                helper.assertTrue(kikoru.startTechnique("axe_slash", kaiju), "Corte rapido comecou");
                step[0] = 1;
                helper.fail("esperando o corte rapido terminar");
            }
            Optional<String> next = kikoru.chooseTechnique(kaiju);
            helper.assertTrue(next.isPresent() && HEAVY.contains(next.get()), "Depois do rapido, um pesado: " + next);
            kaiju.discard();
            kikoru.discard();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5c_kikoru")
    public static void no9BelowHalfCountsAsRegenerating(GameTestHelper helper) {
        KaijuEntity no9 = helper.spawn(KN8Entities.KAIJU_NO9.get(), new BlockPos(7, 1, 7));
        no9.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(!No9Service.regenerating(no9), "Vida cheia: nao regenera");
            no9.setHealth(no9.getMaxHealth() * 0.4F);
            helper.assertTrue(No9Service.regenerating(no9), "Abaixo de 50%: regenera");
            no9.discard();
            helper.succeed();
        });
    }
}
