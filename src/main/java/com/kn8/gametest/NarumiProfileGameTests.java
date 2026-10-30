package com.kn8.gametest;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.NarumiEntity;
import com.kn8.common.soldier.special.NarumiNo1Entity;
import com.kn8.core.soldier.TechniqueMath;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
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

    /**
     * COMBATE-6 (Miguel: Narumi com o Numbers 1 "deve acertar de qualquer jeito o alvo pois o numero 1 preve o
     * futuro"): as tecnicas da obra (Eruption Slash, Slash Salvo, Seven-Branched Sword) estao nas duas formas, as do
     * No. 1 mais fortes; parado a ~7 blocos do alvo, o Slash Salvo do Narumi erra e o do No. 1 acerta.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_combate6_narumi")
    public static void narumiNo1TechniquesAlwaysHit(GameTestHelper helper) {
        NarumiEntity narumi = helper.spawn(KN8Entities.NARUMI.get(), new BlockPos(2, 1, 3));
        narumi.setNoAi(true);
        NarumiNo1Entity no1 = helper.spawn(KN8Entities.NARUMI_NO1.get(), new BlockPos(2, 1, 11));
        no1.setNoAi(true);
        IronGolem plain = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(10, 1, 3));
        plain.setNoAi(true);
        IronGolem foreseen = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(10, 1, 11));
        foreseen.setNoAi(true);
        float[] before = new float[2];
        helper.runAfterDelay(SETTLE, () -> {
            SpecialSoldierDef base = narumi.profile().orElseThrow();
            SpecialSoldierDef enhanced = no1.profile().orElseThrow();
            for (String id : new String[] {"eruption_slash", "slash_salvo", "seven_branched_sword"}) {
                helper.assertTrue(base.techniques().containsKey(id) && enhanced.techniques().containsKey(id),
                        "Tecnica da obra nas duas formas: " + id);
                double plainTotal = base.techniques().get(id).hits().stream().mapToDouble(Float::doubleValue).sum();
                double foreseenTotal = enhanced.techniques().get(id).hits().stream()
                        .mapToDouble(Float::doubleValue).sum();
                helper.assertTrue(foreseenTotal > plainTotal, "No. 1 aprimorado: " + id);
            }
            helper.assertTrue(enhanced.sureHit() && !base.sureHit(), "So o No. 1 acerta sempre");
            before[0] = plain.getHealth();
            before[1] = foreseen.getHealth();
            helper.assertTrue(narumi.startTechnique("slash_salvo", plain), "Slash Salvo do Narumi");
            helper.assertTrue(no1.startTechnique("slash_salvo", foreseen), "Slash Salvo do No. 1");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            helper.assertTrue(plain.getHealth() >= before[0], "Parado longe, o Narumi sem o Numbers 1 erra");
            helper.assertTrue(foreseen.getHealth() < before[1], "O Numbers 1 acerta mesmo longe: "
                    + foreseen.getHealth());
            narumi.discard();
            no1.discard();
            plain.discard();
            foreseen.discard();
            helper.succeed();
        });
    }
}
