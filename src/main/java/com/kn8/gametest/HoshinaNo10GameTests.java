// src/main/java/com/kn8/gametest/HoshinaNo10GameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.soldier.special.HoshinaNo10Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do Hoshina com o traje numerado 10 (0.6-F): sincronizacao ate 100% so neste traje (o Hoshina comum para
 * em 92%), cauda que corta sozinha o kaiju das costas, guarda da cauda contra projetil de tras e o Juni-hitoe, que so
 * sai em Full Release e expoe o nucleo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoshinaNo10GameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final int SETTLE = 3;
    private static final float ARROW_DAMAGE = 10.0F;

    private HoshinaNo10GameTests() {
    }

    private static HoshinaNo10Entity no10(GameTestHelper helper, BlockPos pos) {
        HoshinaNo10Entity hoshina = helper.spawn(KN8Entities.HOSHINA_NO10.get(), pos);
        hoshina.setNoAi(true);
        return hoshina;
    }

    private static KaijuEntity kaiju(GameTestHelper helper, BlockPos pos) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), pos);
        kaiju.setNoAi(true);
        return kaiju;
    }

    @GameTest(template = TEMPLATE)
    public static void onlyTheNumbers10SuitReachesFullRelease(GameTestHelper helper) {
        HoshinaNo10Entity suit = no10(helper, new BlockPos(2, 1, 4));
        HoshinaEntity plain = helper.spawn(KN8Entities.HOSHINA.get(), new BlockPos(6, 1, 4));
        plain.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(suit.getMainHandItem().is(KN8Items.HOSHINA_SWORD.get())
                    && suit.getOffhandItem().is(KN8Items.HOSHINA_SWORD.get()), "Duas espadas nas maos");
            helper.assertFalse(suit.isFullRelease(), "Comeca abaixo do Full Release: " + suit.sync());
            double damage = suit.damageMultiplier();
            suit.setEscalation(100);
            plain.setEscalation(100);
            helper.assertTrue(suit.sync() == 100 && suit.isFullRelease(), "Traje 10: 100%, agora " + suit.sync());
            helper.assertTrue(suit.damageMultiplier() > damage, "Full Release deveria dar mais dano");
            helper.assertTrue(plain.release() == 92, "Hoshina comum para em 92%: " + plain.release());
            helper.assertFalse(plain.isFullRelease(), "Hoshina comum nunca entra em Full Release");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void tailCutsTheKaijuBehind(GameTestHelper helper) {
        // O Hoshina olha para +Z; o kaiju fica atras dele, fora do alcance das espadas.
        HoshinaNo10Entity hoshina = no10(helper, new BlockPos(4, 1, 6));
        KaijuEntity kaiju = kaiju(helper, new BlockPos(4, 1, 2));
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            hoshina.setYRot(0.0F);
            hoshina.setYBodyRot(0.0F);
            before[0] = kaiju.getHealth();
        });
        helper.runAfterDelay(SETTLE + 40, () -> {
            helper.assertTrue(hoshina.tailAttacks() > 0, "A cauda deveria cortar o kaiju das costas");
            helper.assertTrue(kaiju.getHealth() < before[0], "O corte da cauda deveria tirar vida");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void tailGuardsAProjectileFromBehind(GameTestHelper helper) {
        HoshinaNo10Entity front = no10(helper, new BlockPos(2, 1, 4));
        HoshinaNo10Entity back = no10(helper, new BlockPos(6, 1, 4));
        float[] lost = new float[2];
        helper.runAfterDelay(SETTLE, () -> {
            HoshinaNo10Entity[] targets = {front, back};
            for (int i = 0; i < targets.length; i++) {
                HoshinaNo10Entity hoshina = targets[i];
                hoshina.setYRot(0.0F);
                hoshina.setYBodyRot(0.0F);
                // Flecha 3 blocos a frente (+Z) do primeiro e 3 blocos atras (-Z) do segundo.
                Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
                Vec3 at = hoshina.position().add(0, 1, i == 0 ? 3 : -3);
                arrow.setPos(at.x, at.y, at.z);
                float health = hoshina.getHealth();
                hoshina.invulnerableTime = 0;
                hoshina.hurt(helper.getLevel().damageSources().arrow(arrow, null), ARROW_DAMAGE);
                lost[i] = health - hoshina.getHealth();
            }
            helper.assertTrue(back.guards() == 1 && front.guards() == 0, "So a flecha de tras ativa a guarda: "
                    + front.guards() + "/" + back.guards());
            helper.assertTrue(lost[1] < lost[0] * 0.5F, "A guarda da cauda deveria reduzir o dano: frente "
                    + lost[0] + ", costas " + lost[1]);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void juniHitoeNeedsFullReleaseAndExposesTheCore(GameTestHelper helper) {
        HoshinaNo10Entity hoshina = no10(helper, new BlockPos(1, 1, 4));
        KaijuEntity kaiju = kaiju(helper, new BlockPos(5, 1, 4));
        helper.runAfterDelay(SETTLE, () -> {
            hoshina.setTarget(kaiju);
            helper.assertFalse(hoshina.startTechnique("juni_hitoe", kaiju), "Sem Full Release nao ha Juni-hitoe");
            hoshina.setEscalation(100);
            helper.assertTrue(hoshina.startTechnique("juni_hitoe", kaiju), "Em Full Release o Juni-hitoe sai");
        });
        // 12 golpes: preparo de 10 ticks e um golpe a cada 2 (o ultimo no tick 32).
        helper.runAfterDelay(SETTLE + 40, () -> {
            helper.assertTrue(kaiju.isCoreExposed() || !kaiju.isAlive(),
                    "O ultimo golpe do Juni-hitoe deveria expor o nucleo");
            helper.succeed();
        });
    }
}
