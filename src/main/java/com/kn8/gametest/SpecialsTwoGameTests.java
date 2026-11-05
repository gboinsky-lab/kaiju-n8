package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.soldier.special.KonomiEntity;
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
 * ESPECIAIS-2 (modelos do Miguel de 2026-11-05): Aoi, Hakua, Kota, Tae e Ryo como soldados especiais com perfil,
 * aura, arma, receita desenhada por tecnica e lugar nas invasoes; a Postura inabalavel do Aoi da Resistencia e o
 * Incentivo da Hakua da Regeneracao aos aliados; Konomi nao luta e vale como terminal de pesquisa.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialsTwoGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    private static final List<String> GROUP = List.of("aoi", "hakua", "kota", "tae", "ryo");

    private SpecialsTwoGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_especiais2_perfis")
    public static void groupTwoSpecialsHaveProfileAuraAndEffects(GameTestHelper helper) {
        List<HoshinaEntity> spawned = List.of(
                helper.spawn(KN8Entities.AOI.get(), new BlockPos(2, 1, 2)),
                helper.spawn(KN8Entities.HAKUA.get(), new BlockPos(5, 1, 2)),
                helper.spawn(KN8Entities.KOTA.get(), new BlockPos(8, 1, 2)),
                helper.spawn(KN8Entities.TAE.get(), new BlockPos(11, 1, 2)),
                helper.spawn(KN8Entities.RYO.get(), new BlockPos(2, 1, 6)));
        spawned.forEach(special -> special.setNoAi(true));
        helper.runAfterDelay(SETTLE, () -> {
            for (int i = 0; i < GROUP.size(); i++) {
                String id = GROUP.get(i);
                HoshinaEntity special = spawned.get(i);
                ResourceLocation profileId = KN8Constants.id(id);
                SpecialSoldierDef def = KN8Data.SPECIAL_SOLDIER.server().get(profileId);
                helper.assertTrue(def != null, "Sem perfil: " + id);
                helper.assertTrue(special.activeProfileId().equals(profileId), "Perfil ativo de " + id);
                helper.assertTrue(KN8Data.AURA.server().containsKey(def.aura()), "Aura sem dados: " + def.aura());
                helper.assertTrue(KN8Data.WEAPON.server().containsKey(def.weapon()), "Arma sem dados: " + id);
                helper.assertTrue(!special.getMainHandItem().isEmpty(), "Sem arma na mao: " + id);
                def.techniques().keySet().forEach(technique -> helper.assertTrue(
                        VfxService.hasRecipe(special, technique), "Tecnica sem efeito: " + id + " " + technique));
                helper.assertTrue(Math.abs(special.getBbHeight() - def.height()) < 0.05F,
                        "Altura de " + id + ": " + special.getBbHeight());
                helper.assertTrue(KN8Data.INVASION.server().values().stream().anyMatch(invasion -> invasion
                        .defenders().stream().anyMatch(defender -> defender.variant().equals(id))),
                        "Fora das invasoes: " + id);
            }
            spawned.forEach(HoshinaEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "kn8_especiais2_apoio")
    public static void aoiShieldsAndHakuaHealsAllies(GameTestHelper helper) {
        HoshinaEntity aoi = helper.spawn(KN8Entities.AOI.get(), new BlockPos(3, 1, 7));
        HoshinaEntity hakua = helper.spawn(KN8Entities.HAKUA.get(), new BlockPos(5, 1, 3));
        SoldierEntity ally = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 4));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(5, 1, 7));
        List.of(aoi, hakua, ally).forEach(e -> e.setNoAi(true));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(aoi.startTechnique("stalwart_stance", golem), "Postura inabalavel comecou");
            helper.assertTrue(hakua.startTechnique("direct_encouragement", golem), "Incentivo comecou");
            helper.assertTrue(ally.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                    && aoi.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Aliados com Resistencia");
            helper.assertTrue(ally.hasEffect(MobEffects.REGENERATION) && hakua.hasEffect(MobEffects.REGENERATION),
                    "Aliados com Regeneracao");
            List.of(aoi, hakua, ally, golem).forEach(e -> e.discard());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_especiais2_konomi")
    public static void konomiDoesNotFight(GameTestHelper helper) {
        KonomiEntity konomi = helper.spawn(KN8Entities.KONOMI.get(), new BlockPos(4, 1, 4));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(konomi.isInvulnerable() && !konomi.canJoinSquad(), "Konomi invulneravel e fora do"
                    + " esquadrao");
            helper.assertTrue(KN8Data.SPECIAL_SOLDIER.server().get(KonomiEntity.PROFILE).techniques().isEmpty(),
                    "Konomi sem tecnicas");
            konomi.discard();
            helper.succeed();
        });
    }
}
