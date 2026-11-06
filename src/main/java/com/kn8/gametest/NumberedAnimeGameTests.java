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
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NUMERADOS-A e B (modelos do Miguel de 2026-11-05): No. 1, 2, 6, 11, 12, 14 e 15 (duas formas) com os dados, as
 * habilidades do dossie (todas com receita visual quando atiram) e lugar nas invasoes; o No. 1 na altura da tabela
 * mestra (20 m). No. 14: campo de forca de 35% que alterna com a janela exposta e teleporte curto de verdade.
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

    @GameTest(template = "empty_15x12x15", batch = "kn8_no14_escudo")
    public static void no14ShieldAlternatesAndTeleports(GameTestHelper helper) {
        KaijuEntity no14 = helper.spawn(KN8Entities.KAIJU_NO14.get(), new BlockPos(7, 1, 7));
        no14.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 3));
        golem.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            // Inicio do ciclo: escudo ligado (200 ticks), depois 60 exposto.
            no14.tickCount = 5;
            helper.assertTrue(no14.isShielded() && !no14.isOpen(), "Escudo ligado no comeco");
            float before = no14.getHealth();
            no14.invulnerableTime = 0;
            no14.hurt(helper.getLevel().damageSources().mobAttack(golem), 100.0F);
            float shielded = before - no14.getHealth();
            no14.tickCount = 220;
            helper.assertTrue(no14.isShieldDown() && no14.isOpen(), "Janela exposta e brecha");
            float middle = no14.getHealth();
            no14.invulnerableTime = 0;
            no14.hurt(helper.getLevel().damageSources().mobAttack(golem), 100.0F);
            float exposed = middle - no14.getHealth();
            helper.assertTrue(Math.abs(shielded - exposed * 0.65F) < 0.5F,
                    "Escudo tira 35%: com " + shielded + ", sem " + exposed);
            helper.assertTrue(no14.startAbility(KN8Constants.id("no14_blink"), golem), "Teleporte deveria comecar");
        });
        double startX = helper.absolutePos(new BlockPos(7, 1, 7)).getX() + 0.5;
        double startZ = helper.absolutePos(new BlockPos(7, 1, 7)).getZ() + 0.5;
        helper.runAfterDelay(20, () -> {
            double moved = Math.hypot(no14.getX() - startX, no14.getZ() - startZ);
            helper.assertTrue(moved > 4.0, "No. 14 teleportou: andou " + moved);
            helper.assertTrue(Math.abs(no14.distanceTo(golem) - 12.0) < 4.0,
                    "Reapareceu a ~12 blocos do alvo: " + no14.distanceTo(golem));
            no14.discard();
            golem.discard();
            helper.succeed();
        });
    }
}
