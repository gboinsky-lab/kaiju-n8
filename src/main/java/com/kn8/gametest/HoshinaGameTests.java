// src/main/java/com/kn8/gametest/HoshinaGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do Hoshina (0.6-D): perfil (duas espadas, vida, aura e Release do JSON), cortes a distancia (Kuuchi
 * acerta longe e nao fere aliados; Kosa-uchi solta dois cortes), combos (Ran-uchi com varios golpes, Yae-uchi expoe o
 * nucleo) e as reacoes a golpes de kaiju (esquiva e Kaeshi-uchi contra golpe "heavy").
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoshinaGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String LONG_TEMPLATE = "empty_9x7x33";
    private static final int SETTLE = 3;
    private static final int TIMEOUT = 160;

    private HoshinaGameTests() {
    }

    private static HoshinaEntity hoshina(GameTestHelper helper, BlockPos pos, boolean ai) {
        HoshinaEntity hoshina = helper.spawn(KN8Entities.HOSHINA.get(), pos);
        hoshina.setNoAi(!ai);
        return hoshina;
    }

    private static IronGolem golem(GameTestHelper helper, BlockPos pos) {
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, pos);
        golem.setNoAi(true);
        return golem;
    }

    @GameTest(template = TEMPLATE)
    public static void profileGivesTwoSwordsAndAura(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(4, 1, 4), false);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(hoshina.getMainHandItem().is(KN8Items.HOSHINA_SWORD.get()), "Espada na mao direita");
            helper.assertTrue(hoshina.getOffhandItem().is(KN8Items.HOSHINA_SWORD.get()), "Espada na mao esquerda");
            helper.assertTrue(hoshina.getAttributeValue(Attributes.MAX_HEALTH) == 80.0,
                    "Vida do perfil (80): " + hoshina.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(hoshina.getData(KN8Attachments.AURA).equals(KN8Constants.id("violet_lightning")),
                    "Aura roxa: " + hoshina.getData(KN8Attachments.AURA));
            helper.assertTrue(hoshina.getData(KN8Attachments.RELEASE_VISUAL) == 40,
                    "Release 40%: " + hoshina.getData(KN8Attachments.RELEASE_VISUAL));
            helper.succeed();
        });
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = TIMEOUT)
    public static void kuuchiHitsFarAndSparesAllies(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(4, 1, 2), false);
        SoldierEntity ally = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 6));
        ally.setNoAi(true);
        IronGolem golem = golem(helper, new BlockPos(4, 1, 12));
        float[] before = new float[2];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            before[1] = ally.getHealth();
            helper.assertTrue(hoshina.startTechnique("kuuchi", golem), "O Kuuchi deveria comecar");
            helper.succeedWhen(() -> {
                helper.assertTrue(golem.getHealth() < before[0], "O corte deveria acertar o golem a 10 blocos");
                helper.assertTrue(ally.getHealth() == before[1], "O soldado no caminho nao deveria levar o corte");
            });
        });
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = TIMEOUT)
    public static void kosaUchiFiresTwoSlashes(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(4, 1, 2), false);
        IronGolem golem = golem(helper, new BlockPos(4, 1, 14));
        helper.runAfterDelay(SETTLE, () ->
                helper.assertTrue(hoshina.startTechnique("kosa_uchi", golem), "O Kosa-uchi deveria comecar"));
        // Cortes saem no tick 7 do JSON; no tick 9 os dois estao no ar.
        helper.runAfterDelay(SETTLE + 9, () -> {
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40);
            int slashes = helper.getLevel().getEntitiesOfClass(SlashProjectile.class, area).size();
            helper.assertTrue(slashes == 2, "O Kosa-uchi deveria soltar 2 cortes, soltou " + slashes);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void ranUchiHitsManyTimes(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(4, 1, 2), false);
        IronGolem golem = golem(helper, new BlockPos(4, 1, 4));
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(hoshina.startTechnique("ran_uchi", golem), "O Ran-uchi deveria comecar");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            float lost = before[0] - golem.getHealth();
            float oneHit = (float) (7 * 0.35 * hoshina.damageMultiplier());
            helper.assertTrue(lost > oneHit * 5, "Varios golpes do Ran-uchi deveriam acertar: perdeu " + lost
                    + ", um golpe = " + oneHit);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void yaeUchiExposesTheCore(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(1, 1, 4), false);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(5, 1, 4));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () ->
                helper.assertTrue(hoshina.startTechnique("yae_uchi", kaiju), "O Yae-uchi deveria comecar"));
        helper.runAfterDelay(SETTLE + 16, () -> {
            helper.assertTrue(kaiju.isCoreExposed(), "O ultimo golpe do Yae-uchi deveria expor o nucleo");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void dodgesAKaijuStrike(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(1, 1, 4), true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(5, 1, 4));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> helper.assertTrue(kaiju.startAbility(KN8Constants.id("hooved_strike"),
                hoshina), "O casco deveria comecar"));
        helper.runAfterDelay(SETTLE + 12, () -> {
            helper.assertTrue(hoshina.dodges() > 0, "O Hoshina deveria esquivar do golpe que vinha nele");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void countersAHeavyStrike(GameTestHelper helper) {
        HoshinaEntity hoshina = hoshina(helper, new BlockPos(1, 1, 4), true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(5, 1, 4));
        kaiju.setNoAi(true);
        float[] before = new float[2];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = hoshina.getHealth();
            before[1] = kaiju.getHealth();
            helper.assertTrue(kaiju.startAbility(KN8Constants.id("heavy_punch"), hoshina),
                    "O soco pesado deveria comecar");
        });
        helper.runAfterDelay(SETTLE + 24, () -> {
            helper.assertTrue(hoshina.counters() > 0, "Contra golpe heavy o Hoshina deveria usar o Kaeshi-uchi");
            helper.assertTrue(hoshina.getHealth() == before[0], "Invulneravel no Kaeshi-uchi: nao deveria levar dano");
            helper.assertTrue(kaiju.getHealth() < before[1], "O contra-ataque deveria acertar o kaiju");
            helper.succeed();
        });
    }
}
