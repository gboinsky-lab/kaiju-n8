package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.IsaoBaseEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5G (perfil do Isao): o Isao sem o Numbers 2 nasce com o proprio perfil (v1.2: 520 de vida, so
 * golpes de perto, punhos sem as manoplas); nucleo exposto (Thunder Emperor, gelo do Reno) conta como brecha.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IsaoProfileGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;

    private IsaoProfileGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5g_isao_a")
    public static void isaoWithoutNumbers2HasItsOwnProfile(GameTestHelper helper) {
        IsaoBaseEntity isao = helper.spawn(KN8Entities.ISAO_BASE.get(), new BlockPos(7, 1, 7));
        isao.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            SpecialSoldierDef def = isao.profile().orElseThrow();
            helper.assertTrue(def.health() == 520.0F, "Vida do v1.2 (Isao normal): " + def.health());
            helper.assertTrue(isao.getAttributeValue(Attributes.MAX_HEALTH) == def.health(),
                    "Vida aplicada: " + isao.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(isao.getMainHandItem().is(KN8Items.ISAO_FIST.get()),
                    "Punhos sem as manoplas: " + isao.getMainHandItem());
            helper.assertTrue(def.techniques().values().stream()
                    .noneMatch(t -> t.type() == SpecialSoldierDef.TechniqueType.SLASH),
                    "Sem o Numbers 2 nao ha ondas de choque");
            isao.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_mov5g_isao_b")
    public static void exposedCoreIsAnOpening(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 7));
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertFalse(kaiju.isOpen(), "Sem brecha no comeco");
            kaiju.exposeCore(60);
            helper.assertTrue(kaiju.isOpen(), "Nucleo exposto conta como brecha");
            kaiju.discard();
            helper.succeed();
        });
    }

    /**
     * Miguel 2026-10-22 (o Isao deve ser dos mais fortes) e v1.2 (expor o nucleo interrompe a regeneracao avancada):
     * o No. 9 nao se cura com o nucleo exposto e volta a se curar depois.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 160, batch = "kn8_mov5g_isao_c")
    public static void exposedCoreStopsRegeneration(GameTestHelper helper) {
        KaijuEntity no9 = helper.spawn(KN8Entities.KAIJU_NO9.get(), new BlockPos(7, 1, 7));
        no9.setNoAi(true);
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            no9.setHealth(no9.getMaxHealth() * 0.45F);
            start[0] = no9.getHealth();
            no9.exposeCore(60);
        });
        helper.runAfterDelay(SETTLE + 40, () -> helper.assertTrue(no9.getHealth() <= start[0] + 0.01F,
                "Com o nucleo exposto nao deveria curar: " + start[0] + " -> " + no9.getHealth()));
        helper.runAfterDelay(SETTLE + 100, () -> {
            helper.assertTrue(no9.getHealth() > start[0] + 1.0F,
                    "Sem o nucleo exposto volta a curar: " + start[0] + " -> " + no9.getHealth());
            no9.discard();
            helper.succeed();
        });
    }
}
