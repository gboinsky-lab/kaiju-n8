// src/main/java/com/kn8/gametest/DataGameTests.java
package com.kn8.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonParser;
import com.kn8.KN8Constants;
import com.kn8.common.data.DataRegistry;
import com.kn8.common.data.DataReport;
import com.kn8.common.data.DataValidation;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.data.def.MissionDef;
import com.mojang.serialization.DataResult;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M4: os JSON embutidos carregam e validam sem erro; JSON invalido e rejeitado com motivo (regra
 * 10); referencias quebradas tiram a entrada sem derrubar nada.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataGameTests {

    private static final String TEMPLATE = "empty_3x3";
    private static final ResourceLocation PRIMIGENIUS = KN8Constants.id("primigenius");
    private static final float PRIMIGENIUS_FORTITUDE = 5.4F;
    private static final int RANK_COUNT = 6;

    private DataGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void bundledDataLoadsWithoutErrors(GameTestHelper helper) {
        DataReport report = DataValidation.lastReport();
        helper.assertTrue(report.errors().isEmpty(), "Dados embutidos com erro: " + report.errors());
        KaijuDef primigenius = KN8Data.KAIJU.server().get(PRIMIGENIUS);
        helper.assertTrue(primigenius != null, "kn8:primigenius nao carregou");
        helper.assertTrue(primigenius.fortitude() == PRIMIGENIUS_FORTITUDE, "Fortitude do Primigenius errada");
        helper.assertTrue(primigenius.parts().stream().filter(KaijuDef.Part::core).count() == 1,
                "Primigenius deveria ter exatamente uma parte nucleo");
        helper.assertTrue(KN8Data.RANK.server().size() == RANK_COUNT, "Deveriam existir 6 patentes");
        helper.assertTrue(KN8Data.MISSION.server().containsKey(KN8Constants.id("exam")), "Missao do exame ausente");
        helper.assertTrue(KN8Data.BOSS.server().containsKey(KN8Constants.id("revived_honju")), "Chefe ausente");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidJsonIsRejectedWithReason(GameTestHelper helper) {
        String json = "{\"class\":\"yoju\",\"fortitude\":-2,\"size\":\"medium\","
                + "\"dimensions\":{\"width\":1,\"height\":1},\"speed\":0.2}";
        DataResult<KaijuDef> result = KN8Data.KAIJU.parse(JsonParser.parseString(json));
        helper.assertTrue(result.error().isPresent(), "Fortitude negativa deveria ser rejeitada");
        String message = DataRegistry.cleanMessage(result.error().get().message());
        helper.assertTrue(message.contains("outside of range"), "Motivo inesperado: " + message);
        helper.assertTrue(!message.contains("missed input"), "Mensagem nao foi limpa: " + message);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void kaijuWithMissingAbilityIsRemoved(GameTestHelper helper) {
        KaijuDef base = KN8Data.KAIJU.server().get(PRIMIGENIUS);
        helper.assertTrue(base != null, "kn8:primigenius nao carregou");
        KaijuDef broken = new KaijuDef(base.kaijuClass(), base.fortitude(), base.size(), base.dimensions(),
                base.overrides(), base.speed(), base.intelligence(), base.parts(), base.core(),
                List.of(KN8Constants.id("does_not_exist")), base.weaknesses(), Optional.empty(), base.spawn(),
                base.rarity(), base.tags(), base.rage());
        DataReport report = new DataReport();
        Map<ResourceLocation, KaijuDef> valid = DataValidation.validateKaiju(Map.of(PRIMIGENIUS, broken),
                Set.of(), Set.of(), report);
        helper.assertTrue(valid.isEmpty(), "Kaiju com habilidade inexistente deveria sair");
        helper.assertTrue(report.errors().size() == 1, "Esperava 1 erro, veio " + report.errors());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missionChainWithMissingRequirementIsRemoved(GameTestHelper helper) {
        MissionDef exam = KN8Data.MISSION.server().get(KN8Constants.id("exam"));
        helper.assertTrue(exam != null, "kn8:exam nao carregou");
        // O exame exige kn8:first_dismantle; validando o exame sozinho, a dependencia nao existe.
        DataReport report = new DataReport();
        Map<ResourceLocation, MissionDef> valid = DataValidation.validateMissions(
                Map.of(KN8Constants.id("exam"), exam), KN8Data.RANK.server().keySet(),
                KN8Data.KAIJU.server().keySet(), KN8Data.BOSS.server().keySet(), report);
        helper.assertTrue(valid.isEmpty(), "Missao com requires.completed inexistente deveria sair");
        helper.assertTrue(!report.errors().isEmpty(), "Deveria haver erro de dependencia");
        helper.succeed();
    }
}
