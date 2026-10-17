package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.MinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5D (perfil da Mina, plano de mobilidade v3 §7 e "ciclo de vida" das habilidades): o Anti-Giant
 * (4 usos) fica guardado para alvo grande; tiro de tecnica sem linha de tiro no disparo e cancelado e o uso volta;
 * atiradora cancela o tiro se o alvo chega mais perto que o alcance minimo durante o preparo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MinaProfileGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    private static final int TRIES = 20;
    private static final String ANTI_GIANT = "anti_giant_shot";

    private MinaProfileGameTests() {
    }

    private static MinaEntity mina(GameTestHelper helper) {
        MinaEntity mina = helper.spawn(KN8Entities.MINA.get(), new BlockPos(1, 1, 7));
        mina.setNoAi(true);
        return mina;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160, batch = "kn8_mov5d_mina_a")
    public static void minaSavesAntiGiantForBigTargets(GameTestHelper helper) {
        MinaEntity mina = mina(helper);
        KaijuEntity yoju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(13, 1, 7));
        yoju.setNoAi(true);
        boolean[] started = {false};
        helper.runAfterDelay(SETTLE, () -> {
            mina.lookAt(yoju, 360.0F, 90.0F);
            // O canhao carregado (prioridade 3) em recarga: canhao e Anti-Giant empatam (2) contra o Yoju.
            helper.assertTrue(mina.startTechnique("charged_cannon", yoju), "Canhao carregado comecou");
            started[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(started[0] && !mina.isUsingTechnique(), "Tecnica em andamento");
            for (int i = 0; i < TRIES; i++) {
                helper.assertTrue(!mina.chooseTechnique(yoju).filter(ANTI_GIANT::equals).isPresent(),
                        "Anti-Giant gasto num Yoju");
            }
            yoju.discard();
            mina.discard();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "kn8_mov5d_mina_b")
    public static void antiGiantChosenAgainstHonju(GameTestHelper helper) {
        MinaEntity mina = mina(helper);
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(12, 1, 7));
        honju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(mina.chooseTechnique(honju).filter(ANTI_GIANT::equals).isPresent(),
                    "Contra o Honju: Anti-Giant, veio " + mina.chooseTechnique(honju));
            honju.discard();
            mina.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140, batch = "kn8_mov5d_mina_c")
    public static void shotWithoutLineOfFireIsCancelledAndRefunded(GameTestHelper helper) {
        MinaEntity mina = mina(helper);
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(12, 1, 7));
        honju.setNoAi(true);
        float[] health = new float[1];
        boolean[] started = {false};
        helper.runAfterDelay(SETTLE, () -> {
            health[0] = honju.getHealth();
            mina.lookAt(honju, 360.0F, 90.0F);
            helper.assertTrue(mina.startTechnique(ANTI_GIANT, honju), "Anti-Giant comecou");
            started[0] = true;
            // Muro entre as duas durante o preparo (60 ticks).
            for (int y = 1; y <= 10; y++) {
                for (int z = 0; z <= 14; z++) {
                    helper.setBlock(new BlockPos(5, y, z), Blocks.BEDROCK);
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(started[0] && !mina.isUsingTechnique(), "Tecnica em andamento");
            helper.assertTrue(honju.getHealth() >= health[0], "O tiro nao deveria sair atraves do muro");
            helper.assertTrue(mina.profile().map(def -> mina.usesLeft(def, ANTI_GIANT)).orElse(-1) == 4,
                    "O uso do Anti-Giant deveria voltar");
            honju.discard();
            mina.discard();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "kn8_mov5d_mina_d")
    public static void shooterCancelsWhenTargetGetsTooClose(GameTestHelper helper) {
        MinaEntity mina = mina(helper);
        KaijuEntity yoju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(13, 1, 7));
        yoju.setNoAi(true);
        boolean[] started = {false};
        helper.runAfterDelay(SETTLE, () -> {
            mina.lookAt(yoju, 360.0F, 90.0F);
            helper.assertTrue(mina.startTechnique("charged_cannon", yoju), "Canhao carregado comecou");
            started[0] = true;
            // O kaiju chega colado (alcance minimo do canhao carregado: 6).
            yoju.teleportTo(mina.getX() + 3.0, mina.getY(), mina.getZ());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(started[0] && !mina.isUsingTechnique(), "O tiro deveria ser cancelado no preparo");
            helper.assertTrue(helper.getLevel().getGameTime() < mina.techniqueReadyAt("charged_cannon") + 1
                    && mina.techniqueReadyAt("charged_cannon") < helper.getLevel().getGameTime() + 60,
                    "Recarga curta depois do cancelamento: " + mina.techniqueReadyAt("charged_cannon"));
            yoju.discard();
            mina.discard();
        });
    }
}
