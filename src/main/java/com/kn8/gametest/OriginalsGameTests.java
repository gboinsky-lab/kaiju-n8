package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * MIGUEL-CAMILLY: os personagens originais do Miguel como soldados especiais, com perfil, aura, arma propria (foice
 * e rifle rosa), receita desenhada por tecnica, lugar nas invasoes e as armas fabricaveis na bancada.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OriginalsGameTests {

    private OriginalsGameTests() {
    }

    @GameTest(template = "empty_15x12x15", batch = "kn8_originais")
    public static void miguelAndCamillyHaveTheirKits(GameTestHelper helper) {
        List<HoshinaEntity> spawned = List.of(
                helper.spawn(KN8Entities.MIGUEL.get(), new BlockPos(3, 1, 3)),
                helper.spawn(KN8Entities.CAMILLY.get(), new BlockPos(8, 1, 3)));
        List<String> ids = List.of("miguel", "camilly");
        spawned.forEach(special -> special.setNoAi(true));
        helper.runAfterDelay(3, () -> {
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                HoshinaEntity special = spawned.get(i);
                ResourceLocation profileId = KN8Constants.id(id);
                SpecialSoldierDef def = KN8Data.SPECIAL_SOLDIER.server().get(profileId);
                helper.assertTrue(def != null && def.techniques().size() == 8, "Kit de " + id);
                helper.assertTrue(KN8Data.AURA.server().containsKey(def.aura()), "Aura de " + id);
                def.techniques().keySet().forEach(technique -> helper.assertTrue(
                        VfxService.hasRecipe(special, technique), "Tecnica sem efeito: " + id + " " + technique));
                helper.assertTrue(KN8Data.INVASION.server().values().stream().anyMatch(invasion -> invasion
                        .defenders().stream().anyMatch(defender -> defender.variant().equals(id))),
                        "Fora das invasoes: " + id);
            }
            helper.assertTrue(spawned.get(0).getMainHandItem().is(KN8Items.MIGUEL_SCYTHE.get())
                    && spawned.get(1).getMainHandItem().is(KN8Items.CAMILLY_RIFLE.get()), "Armas proprias na mao");
            for (ResourceLocation weapon : List.of(KN8Constants.id("miguel_scythe"), KN8Constants.id("camilly_rifle"))) {
                helper.assertTrue(KN8Data.WEAPON.server().containsKey(weapon), "Arma sem dados: " + weapon);
                helper.assertTrue(KN8Data.WORKBENCH.server().values().stream()
                        .anyMatch(r -> r.result().equals(weapon)), "Sem receita: " + weapon);
            }
            spawned.forEach(HoshinaEntity::discard);
            helper.succeed();
        });
    }
}
