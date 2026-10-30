package com.kn8.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kn8.KN8Constants;
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

    /** Kaiju que nascem de outro (transform/absorb do numbered) ou pelo servico da larva. */
    private static final List<String> BY_TRANSITION_KAIJU = List.of("kaiju_no10_giant", "kaiju_no9_fusion",
            "kaiju_no9_fusion_giant", "kaiju_no9_isao", "kaiju_larva");
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
            helper.assertTrue(kaiju.contains(id.getPath()) || BY_TRANSITION_KAIJU.contains(id.getPath()),
                    "Kaiju sem lugar no mundo (so /summon): " + id);
        }
        for (ResourceLocation id : KN8Data.SPECIAL_SOLDIER.server().keySet()) {
            helper.assertTrue(specials.contains(id.getPath()) || BY_TRANSITION_SPECIAL.contains(id.getPath()),
                    "Soldado especial sem lugar no mundo (so ovo): " + id);
        }
        helper.succeed();
    }
}
