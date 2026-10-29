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
import net.minecraft.world.phys.Vec3;
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
    /** Lista "heavy" do kikoru.json (COMBATE-3: Falling Thunder e Half-Moon entraram). */
    private static final Set<String> HEAVY = Set.of("heavy_swing", "shockwave", "ground_smash", "guard_break",
            "falling_thunder", "half_moon");

    private KikoruProfileGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "kn8_mov5c_kikoru")
    public static void kikoruAlternatesFastAndHeavy(GameTestHelper helper) {
        KikoruEntity kikoru = helper.spawn(KN8Entities.KIKORU.get(), new BlockPos(5, 1, 7));
        kikoru.setNoAi(true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(9, 1, 7));
        kaiju.setNoAi(true);
        // COMBATE-3: -1 = esperando o pesado comecar (o succeedWhen roda desde o tick 0).
        int[] step = {-1};
        helper.runAfterDelay(SETTLE, () -> {
            kikoru.lookAt(kaiju, 360.0F, 90.0F);
            helper.assertTrue(kikoru.startTechnique("ground_smash", kaiju), "Golpe no chao (pesado) comecou");
            step[0] = 0;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(step[0] >= 0, "Esperando o pesado comecar");
            helper.assertTrue(!kikoru.isUsingTechnique(), "Tecnica em andamento");
            if (step[0] == 0) {
                // Depois do pesado: o golpe rapido ganha o bonus de alternar e passa os pesados empatados.
                Optional<String> next = kikoru.chooseTechnique(kaiju);
                // COMBATE-3: o rapido agora e uma tecnica da obra (Twin Swallows/Daruma Dropper), nao o axe_slash.
                helper.assertTrue(next.isPresent() && !HEAVY.contains(next.get()),
                        "Depois do pesado, o rapido: " + next);
                helper.assertTrue(kikoru.startTechnique(next.get(), kaiju), "Corte rapido comecou");
                step[0] = 1;
                helper.fail("esperando o corte rapido terminar");
            }
            Optional<String> next = kikoru.chooseTechnique(kaiju);
            helper.assertTrue(next.isPresent() && HEAVY.contains(next.get()), "Depois do rapido, um pesado: " + next);
            kaiju.discard();
            kikoru.discard();
        });
    }

    /**
     * COMBATE-3 (Miguel: o Falling Thunder vem de cima): a Kikoru NPC salta para cima e na direcao do kaiju no comeco
     * da tecnica e cai em cima dele.
     */
    // Lote proprio: nenhum kaiju de outro teste por perto para a IA dela.
    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "kn8_combate3_kikoru_leap")
    public static void kikoruFallingThunderLeapsOntoTheKaiju(GameTestHelper helper) {
        // Com IA: mob sem IA nao segue a velocidade (0.6-A); o teste comeca a tecnica antes de a IA escolher alvo.
        KikoruEntity kikoru = helper.spawn(KN8Entities.KIKORU.get(), new BlockPos(3, 1, 7));
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(10, 1, 7));
        kaiju.setNoAi(true);
        double[] startX = new double[1];
        double[] peak = {Double.NEGATIVE_INFINITY};
        double[] farthest = {Double.NEGATIVE_INFINITY};
        long[] started = {-1};
        double ground = helper.absoluteVec(new Vec3(0, 1, 0)).y;
        // Tenta a cada tick ate a Kikoru estar livre (ao nascer ela saca a arma) e no chao.
        helper.succeedWhen(() -> {
            long now = helper.getTick();
            if (started[0] < 0) {
                helper.assertTrue(kikoru.onGround() && !kikoru.isUsingTechnique() && !kikoru.isActing(),
                        "Esperando a Kikoru ficar livre no chao");
                // Com IA ela anda ate o kaiju enquanto saca a arma; encostada nele o salto e quase so vertical
                // (certo no jogo). O teste a poe de volta a 7 blocos para medir o avanco.
                Vec3 back = helper.absoluteVec(new Vec3(3.5, 1.0, 7.5));
                kikoru.moveTo(back.x, back.y, back.z);
                kikoru.getNavigation().stop();
                kikoru.lookAt(kaiju, 360.0F, 90.0F);
                startX[0] = kikoru.getX();
                helper.assertTrue(kikoru.startTechnique("falling_thunder", kaiju), "Falling Thunder comecou");
                helper.assertTrue(kikoru.getDeltaMovement().y > 0.5, "A Kikoru deveria saltar alto: "
                        + kikoru.getDeltaMovement());
                started[0] = now;
                helper.fail("saltando");
            }
            // Pico do salto (medir num tick fixo falhava quando ela ja tinha descido ou ainda subia).
            peak[0] = Math.max(peak[0], kikoru.getY());
            farthest[0] = Math.max(farthest[0], kikoru.getX());
            helper.assertTrue(now >= started[0] + 8, "No ar");
            helper.assertTrue(peak[0] > ground + 1.5, "No meio do Falling Thunder deveria estar no alto: pico "
                    + (peak[0] - ground));
            helper.assertTrue(farthest[0] > startX[0] + 0.5, "A Kikoru deveria ir na direcao do kaiju: "
                    + (farthest[0] - startX[0]));
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
