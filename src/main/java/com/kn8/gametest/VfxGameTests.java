// src/main/java/com/kn8/gametest/VfxGameTests.java
package com.kn8.gametest;

import java.util.ArrayList;
import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.vfx.VfxColors;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * VFX-1 (diretriz KaijuVFX): todo kaiju e personagem tem perfil visual, com as cores decididas pelo Miguel; o efeito
 * sai com o perfil da forma ativa; efeitos desligados nao mudam o dano (diretriz §7).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VfxGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation BITE = KN8Constants.id("bite");
    private static final float TOLERANCE = 0.05F;

    private VfxGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void everyKaijuAndCharacterHasAVisualProfile(GameTestHelper helper) {
        List<ResourceLocation> ids = new ArrayList<>(KN8Data.KAIJU.server().keySet());
        ids.addAll(KN8Data.SPECIAL_SOLDIER.server().keySet());
        ids.add(KN8Constants.id("soldier"));
        ids.add(KN8Constants.id("player"));
        ids.forEach(id -> helper.assertTrue(KN8Data.VFX_PROFILE.server().containsKey(id),
                "Sem perfil visual: " + id));
        // Cores do Miguel (2026-10-22): Mina verde, Narumi rosa, Isao amarelo/laranja, Hoshina violeta.
        int mina = accent("mina");
        helper.assertTrue(green(mina) > red(mina) && green(mina) > blue(mina), "Mina deveria ser verde");
        int narumi = accent("narumi");
        helper.assertTrue(red(narumi) > green(narumi) && blue(narumi) > green(narumi), "Narumi deveria ser rosa");
        int isao = accent("isao");
        helper.assertTrue(red(isao) > green(isao) && green(isao) > blue(isao), "Isao deveria ser laranja");
        int hoshina = accent("hoshina");
        helper.assertTrue(blue(hoshina) > green(hoshina) && red(hoshina) > green(hoshina),
                "Hoshina deveria ser violeta");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void effectsCarryTheProfileOfTheActiveForm(GameTestHelper helper) {
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(2, 1, 2));
        HoshinaEntity no10 = helper.spawn(KN8Entities.HOSHINA_NO10.get(), new BlockPos(6, 1, 2));
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 6));
        helper.assertTrue(VfxService.profileOf(spider).orElseThrow().equals(KN8Constants.id("trichonephila")),
                "Perfil da aranha");
        helper.assertTrue(VfxService.profileOf(no10).orElseThrow().equals(KN8Constants.id("hoshina_no10")),
                "Perfil do Hoshina com o Numbers 10");
        helper.assertTrue(VfxService.profileOf(soldier).orElseThrow().equals(KN8Constants.id("soldier")),
                "Perfil do soldado");
        spider.discard();
        no10.discard();
        soldier.discard();
        helper.succeed();
    }

    /** O mesmo golpe com os efeitos desligados ([vfx] enabled false) tira a mesma vida no mesmo tick. */
    @GameTest(template = TEMPLATE)
    public static void effectsOffKeepTheDamage(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(3, 1, 4));
        kaiju.setNoAi(true);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(5, 1, 4));
        cow.setNoAi(true);
        float[] state = new float[2];
        helper.runAfterDelay(2, () -> {
            ServerConfig.VFX_ENABLED.set(false);
            state[0] = cow.getHealth();
            state[1] = (float) kaiju.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(kaiju.startAbility(BITE, cow), "A mordida deveria comecar");
        });
        helper.runAfterDelay(11, () -> {
            ServerConfig.VFX_ENABLED.set(true);
            helper.assertTrue(Math.abs(cow.getHealth() - (state[0] - state[1])) < TOLERANCE,
                    "Esperava " + (state[0] - state[1]) + " de vida sem efeitos, veio " + cow.getHealth());
            kaiju.discard();
            cow.discard();
            helper.succeed();
        });
    }

    private static int accent(String id) {
        VfxProfileDef def = KN8Data.VFX_PROFILE.server().get(KN8Constants.id(id));
        return def == null ? 0 : def.palette().resolve("accent");
    }

    private static float red(int rgb) {
        return VfxColors.red(rgb);
    }

    private static float green(int rgb) {
        return VfxColors.green(rgb);
    }

    private static float blue(int rgb) {
        return VfxColors.blue(rgb);
    }
}
