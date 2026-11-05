package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * ESPECIAIS-1 (modelos do Miguel de 2026-11-05 e o dossie de habilidades): Soichiro Hoshina, Jura Igarashi, Rin
 * Shinonome, Iharu Furuhashi e Haruichi Izumo como soldados especiais, com perfil, aura, receita desenhada para cada
 * tecnica e lugar nas invasoes; a bola em chamas da Jura incendeia e o Hot-Blooded fortalece os aliados; a municao
 * eletrificada do Iharu da choque (Lentidao).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialsOneGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    private static final List<String> GROUP = List.of("soichiro", "jura", "rin", "iharu", "haruichi");

    private SpecialsOneGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_especiais1_perfis")
    public static void groupOneSpecialsHaveProfileAuraAndEffects(GameTestHelper helper) {
        List<HoshinaEntity> spawned = List.of(
                helper.spawn(KN8Entities.SOICHIRO.get(), new BlockPos(2, 1, 2)),
                helper.spawn(KN8Entities.JURA.get(), new BlockPos(5, 1, 2)),
                helper.spawn(KN8Entities.RIN.get(), new BlockPos(8, 1, 2)),
                helper.spawn(KN8Entities.IHARU.get(), new BlockPos(11, 1, 2)),
                helper.spawn(KN8Entities.HARUICHI.get(), new BlockPos(2, 1, 6)));
        spawned.forEach(special -> special.setNoAi(true));
        helper.runAfterDelay(SETTLE, () -> {
            for (int i = 0; i < GROUP.size(); i++) {
                String id = GROUP.get(i);
                HoshinaEntity special = spawned.get(i);
                ResourceLocation profileId = KN8Constants.id(id);
                SpecialSoldierDef def = KN8Data.SPECIAL_SOLDIER.server().get(profileId);
                helper.assertTrue(def != null, "Sem perfil: " + id);
                helper.assertTrue(special.activeProfileId().equals(profileId), "Perfil ativo de " + id + ": "
                        + special.activeProfileId());
                helper.assertTrue(KN8Data.AURA.server().containsKey(def.aura()), "Aura sem dados: " + def.aura());
                helper.assertTrue(!special.getMainHandItem().isEmpty(), "Sem arma na mao: " + id);
                def.techniques().keySet().forEach(technique -> helper.assertTrue(
                        VfxService.hasRecipe(special, technique), "Tecnica sem efeito: " + id + " " + technique));
                helper.assertTrue(Math.abs(special.getBbHeight() - def.height()) < 0.05F,
                        "Altura de " + id + ": " + special.getBbHeight());
            }
            // Todos nascem no jogo (invasoes), sem /summon.
            for (String id : GROUP) {
                helper.assertTrue(KN8Data.INVASION.server().values().stream().anyMatch(invasion -> invasion
                        .defenders().stream().anyMatch(defender -> defender.variant().equals(id))),
                        "Fora das invasoes: " + id);
            }
            spawned.forEach(HoshinaEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_especiais1_jura")
    public static void juraBallBurnsAndHotBloodedRalliesAllies(GameTestHelper helper) {
        HoshinaEntity jura = helper.spawn(KN8Entities.JURA.get(), new BlockPos(4, 1, 7));
        jura.setNoAi(true);
        SoldierEntity ally = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 4));
        ally.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(6, 1, 7));
        golem.setNoAi(true);
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(jura.startTechnique("hot_blooded_impact", golem), "Hot-Blooded comecou");
            helper.assertTrue(jura.hasEffect(MobEffects.DAMAGE_BOOST) && ally.hasEffect(MobEffects.DAMAGE_BOOST)
                    && ally.hasEffect(MobEffects.MOVEMENT_SPEED), "Aliados com Forca e Velocidade");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            helper.assertTrue(golem.getHealth() < before[0], "A bola acertou: " + golem.getHealth());
            helper.assertTrue(golem.getRemainingFireTicks() > 0, "Alvo em chamas");
            List.of(jura, ally, golem).forEach(e -> e.discard());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_especiais1_iharu")
    public static void iharuElectricRoundShocks(GameTestHelper helper) {
        HoshinaEntity iharu = helper.spawn(KN8Entities.IHARU.get(), new BlockPos(2, 1, 7));
        iharu.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(10, 1, 7));
        golem.setNoAi(true);
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(iharu.startTechnique("thunderbolt_blast", golem), "Explosao trovao comecou");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            helper.assertTrue(golem.getHealth() < before[0], "O tiro acertou: " + golem.getHealth());
            helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Choque (Lentidao) no alvo");
            iharu.discard();
            golem.discard();
            helper.succeed();
        });
    }
}
