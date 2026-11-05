package com.kn8.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.InvasionDef;
import com.kn8.common.data.def.MissionDef;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da B-1 (onde cada um aparece): todo kaiju e todo soldado especial dos dados aparecem no jogo por alguma
 * via que nao seja o {@code /summon}: onda de invasao, alvo de missao, chefe, guarnicao ou defensor. Os que so nascem
 * por transicao (forma gigante, fusoes, absorcao, forma do Kafka, larva do hospedeiro) ficam na lista de excecoes.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldPresenceGameTests {

    /**
     * Kaiju que nascem de outro (forma gigante do No. 10) ou pelo servico da larva. As formas do No. 9 tambem surgem
     * nas invasoes grandes desde a AJUSTES-2 (Miguel 2026-11-05).
     */
    private static final List<String> BY_TRANSITION_KAIJU = List.of("kaiju_no10_giant", "kaiju_larva");
    /** AJUSTES-2/3: as formas do No. 9 surgem de surpresa (1%, Miguel 2026-11-05) nas invasoes grandes (nivel 4+). */
    private static final List<String> BIG_INVASION_SURPRISES = List.of("kaiju_no9_black", "kaiju_no9_fusion",
            "kaiju_no9_fusion_giant", "kaiju_no9_camponotus", "kaiju_no9_isao");
    /** AJUSTES-3: o No. 8 sem controle entra em qualquer invasao com {@code [invasion] rampageNo8Chance}. */
    private static final List<String> ANY_INVASION_SURPRISES = List.of("kaiju_no8_rampage",
            "kaiju_no8_rampage_giant");
    private static final float FORM_SURPRISE_CHANCE = 0.01F;
    private static final int BIG_INVASION_LEVEL = 4;
    /** Especiais que nascem pela troca de forma de outro (Kafka -> No. 8). */
    private static final List<String> BY_TRANSITION_SPECIAL = List.of("kaiju_no8");

    private WorldPresenceGameTests() {
    }

    @GameTest(template = "empty_3x3")
    public static void everyKaijuAndSpecialAppearsInTheWorld(GameTestHelper helper) {
        Set<String> kaiju = new HashSet<>();
        Set<String> specials = new HashSet<>();
        for (InvasionDef invasion : KN8Data.INVASION.server().values()) {
            for (InvasionDef.Wave wave : invasion.waves()) {
                wave.kaiju().forEach(spawn -> kaiju.add(spawn.species().getPath()));
                wave.boss().flatMap(boss -> KN8Data.BOSS.get(boss, false))
                        .ifPresent(boss -> kaiju.add(boss.kaiju().getPath()));
            }
            invasion.defenders().forEach(defender -> specials.add(defender.variant()));
        }
        for (MissionDef mission : KN8Data.MISSION.server().values()) {
            for (MissionDef.Objective objective : mission.objectives()) {
                objective.target().ifPresent(target -> {
                    kaiju.add(target.getPath());
                    KN8Data.BOSS.get(target, false).ifPresent(boss -> kaiju.add(boss.kaiju().getPath()));
                });
            }
        }
        KN8Data.SITE.server().values().forEach(site -> site.garrison().forEach(member -> specials.add(
                member.variant())));
        for (ResourceLocation id : KN8Data.KAIJU.server().keySet()) {
            helper.assertTrue(kaiju.contains(id.getPath()) || BY_TRANSITION_KAIJU.contains(id.getPath())
                    || ANY_INVASION_SURPRISES.contains(id.getPath()),
                    "Kaiju sem lugar no mundo (so /summon): " + id);
        }
        for (ResourceLocation id : KN8Data.SPECIAL_SOLDIER.server().keySet()) {
            helper.assertTrue(specials.contains(id.getPath()) || BY_TRANSITION_SPECIAL.contains(id.getPath()),
                    "Soldado especial sem lugar no mundo (so ovo): " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3")
    public static void no9FormsAndRampageNo8SurpriseBigInvasions(GameTestHelper helper) {
        Set<String> surprises = new HashSet<>();
        for (InvasionDef invasion : KN8Data.INVASION.server().values()) {
            if (invasion.level() < BIG_INVASION_LEVEL) {
                continue;
            }
            invasion.waves().forEach(wave -> wave.kaiju().stream().filter(spawn -> spawn.chance() < 1.0F)
                    .forEach(spawn -> {
                        surprises.add(spawn.species().getPath());
                        if (BIG_INVASION_SURPRISES.contains(spawn.species().getPath())) {
                            helper.assertTrue(spawn.chance() <= FORM_SURPRISE_CHANCE, "Forma do No. 9 com mais de"
                                    + " 1%: " + spawn.species() + " " + spawn.chance());
                        }
                    }));
        }
        for (String id : BIG_INVASION_SURPRISES) {
            helper.assertTrue(surprises.contains(id), "Sem surpresa numa invasao grande: " + id);
            helper.assertTrue(KN8Data.KAIJU.server().containsKey(KN8Constants.id(id)), "Kaiju sem dados: " + id);
        }
        helper.assertTrue(ServerConfig.INVASION_RAMPAGE_NO8_CHANCE.get() > 0.0, "No. 8 sem controle sem chance");
        ANY_INVASION_SURPRISES.forEach(id -> helper.assertTrue(KN8Data.KAIJU.server().containsKey(KN8Constants.id(id)),
                "Kaiju sem dados: " + id));
        helper.succeed();
    }
}
