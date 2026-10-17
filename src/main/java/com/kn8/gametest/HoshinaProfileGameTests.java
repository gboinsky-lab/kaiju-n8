package com.kn8.gametest;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5B (perfil do Hoshina, plano de mobilidade v3 §7): explora a brecha do kaiju com a tecnica que mais
 * tira, nao comeca combo com o alvo saindo do alcance, um golpe por tick nas sequencias rapidas e espera extra depois
 * de um combo que nao acertou nada.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoshinaProfileGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    /** Igual ao {@code miss_cooldown_ticks} do hoshina.json. */
    private static final int MISS_COOLDOWN = 40;

    private HoshinaProfileGameTests() {
    }

    private static HoshinaEntity hoshina(GameTestHelper helper, BlockPos pos) {
        HoshinaEntity hoshina = helper.spawn(KN8Entities.HOSHINA.get(), pos);
        hoshina.setNoAi(true);
        return hoshina;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5b_hoshina")
    public static void hoshinaExploitsAnOpening(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(5, 1, 7));
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(9, 1, 7));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            Optional<String> closed = hoshina.chooseTechnique(kaiju);
            helper.assertTrue(closed.equals(Optional.of("ran_uchi")), "Guarda fechada: Ran-uchi, veio " + closed);
            kaiju.stagger(40);
            Optional<String> open = hoshina.chooseTechnique(kaiju);
            helper.assertTrue(open.equals(Optional.of("yae_uchi")),
                    "Guarda aberta: a tecnica que mais tira (Yae-uchi), veio " + open);
            kaiju.discard();
            hoshina.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5b_hoshina")
    public static void hoshinaSkipsComboOnAFleeingTarget(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(5, 1, 7));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 7));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(hoshina.chooseTechnique(golem).isPresent(), "Alvo parado: escolhe um combo");
            // O golem "andou" 3 blocos para longe no ultimo tick: nenhum combo chega a tempo.
            golem.xo = golem.getX() - 3.0;
            golem.zo = golem.getZ();
            Optional<String> chosen = hoshina.chooseTechnique(golem);
            helper.assertTrue(chosen.isEmpty(), "Alvo fugindo: nenhum combo, veio " + chosen);
            golem.discard();
            hoshina.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_mov5b_hoshina")
    public static void hoshinaSequenceLandsOneHitPerTick(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(5, 1, 7));
        // Kaiju de 220 de vida: aguenta os 8 golpes (o golem morria no setimo).
        KaijuEntity golem = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(9, 1, 7));
        golem.setNoAi(true);
        int[] last = {0};
        boolean[] started = {false};
        helper.runAfterDelay(SETTLE, () -> {
            // Escalada no maximo: a tecnica corre mais rapido que um golpe por tick do JSON (Yae-uchi a cada 1 tick).
            hoshina.setEscalation(1000.0F);
            hoshina.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(hoshina.startTechnique("yae_uchi", golem), "Yae-uchi comecou");
            started[0] = true;
        });
        helper.onEachTick(() -> {
            if (!started[0]) {
                return;
            }
            int landed = hoshina.landedHits();
            helper.assertTrue(landed - last[0] <= 1, "Mais de um golpe no mesmo tick: " + last[0] + " -> " + landed);
            last[0] = landed;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(started[0] && !hoshina.isUsingTechnique(), "Tecnica ainda em andamento");
            helper.assertTrue(last[0] == 8, "Os 8 golpes do Yae-uchi deveriam acertar: " + last[0]);
            golem.discard();
            hoshina.discard();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_mov5b_hoshina")
    public static void hoshinaWaitsAfterAMissedCombo(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(5, 1, 7));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 7));
        golem.setNoAi(true);
        boolean[] started = {false};
        long[] normal = {0};
        helper.runAfterDelay(SETTLE, () -> {
            hoshina.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(hoshina.startTechnique("kasumi_uchi", golem), "Kasumi-uchi comecou");
            started[0] = true;
            normal[0] = hoshina.techniqueReadyAt("kasumi_uchi");
            // O alvo sai do alcance antes do primeiro corte.
            golem.teleportTo(golem.getX() + 6.0, golem.getY(), golem.getZ());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(started[0] && !hoshina.isUsingTechnique(), "Tecnica ainda em andamento");
            helper.assertTrue(hoshina.landedHits() == 0, "Nenhum golpe deveria acertar");
            helper.assertTrue(hoshina.techniqueReadyAt("kasumi_uchi") >= normal[0] + MISS_COOLDOWN,
                    "Combo que errou espera mais: pronto em " + hoshina.techniqueReadyAt("kasumi_uchi")
                            + ", sem errar " + normal[0]);
            golem.discard();
            hoshina.discard();
        });
    }
}
