package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NUMERADOS-A e B (modelos do Miguel de 2026-11-05): No. 1, 2, 6, 11, 12, 14 e 15 (duas formas) com os dados, as
 * habilidades do dossie (todas com receita visual quando atiram) e lugar nas invasoes; o No. 1 na altura da tabela
 * mestra (20 m).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NumberedAnimeGameTests {

    private NumberedAnimeGameTests() {
    }

    @GameTest(template = "empty_15x12x15", batch = "kn8_numerados_a")
    public static void animeNumberedKaijuHaveDataAndAppear(GameTestHelper helper) {
        List<EntityType<KaijuEntity>> types = List.of(KN8Entities.KAIJU_NO1.get(), KN8Entities.KAIJU_NO11.get(),
                KN8Entities.KAIJU_NO12.get(), KN8Entities.KAIJU_NO15.get(), KN8Entities.KAIJU_NO15_KIKORU.get(),
                KN8Entities.KAIJU_NO2.get(), KN8Entities.KAIJU_NO6.get(), KN8Entities.KAIJU_NO14.get());
        List<String> ids = List.of("kaiju_no1", "kaiju_no11", "kaiju_no12", "kaiju_no15", "kaiju_no15_kikoru",
                "kaiju_no2", "kaiju_no6", "kaiju_no14");
        for (int i = 0; i < ids.size(); i++) {
            ResourceLocation id = KN8Constants.id(ids.get(i));
            KaijuDef def = KN8Data.KAIJU.server().get(id);
            helper.assertTrue(def != null && def.abilities().size() >= 5, "Sem dados/habilidades: " + id);
            def.abilities().forEach(ability -> helper.assertTrue(KN8Data.ABILITY.server().containsKey(ability),
                    "Habilidade inexistente: " + ability));
            helper.assertTrue(KN8Data.VFX_PROFILE.server().containsKey(id), "Sem perfil visual: " + id);
            helper.assertTrue(KN8Data.INVASION.server().values().stream().anyMatch(invasion -> invasion.waves()
                    .stream().anyMatch(wave -> wave.kaiju().stream().anyMatch(k -> k.species().equals(id)))),
                    "Fora das invasoes: " + id);
        }
        KaijuEntity no1 = helper.spawn(types.get(0), new BlockPos(7, 1, 7));
        no1.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(no1.getBbHeight() - 20.0F) < 0.5F, "No. 1 com 20 m: " + no1.getBbHeight());
            no1.discard();
            helper.succeed();
        });
    }
}
