// src/main/java/com/kn8/common/data/DataValidation.java
package com.kn8.common.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.kn8.KN8Constants;
import com.kn8.core.kaiju.KaijuScale;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.BossDef;
import com.kn8.common.data.def.DismantleDef;
import com.kn8.common.data.def.InvasionDef;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.data.def.NumberedDef;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.data.def.SoldierDef;
import com.kn8.common.data.def.SuitDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.data.def.WorkbenchRecipeDef;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Validacao de referencias cruzadas, rodada quando TODOS os dados terminam de carregar (abertura do mundo e
 * {@code /reload}). Segue a ordem de {@link KN8Data#ALL}: cada tipo so referencia tipos ja validados, entao uma
 * entrada removida aqui some tambem para quem depende dela, sem varias passadas.
 *
 * <ul>
 *   <li><b>Arquivo ignorado</b>: o Codec recusou o JSON no carregamento; entra no relatorio como erro.</li>
 *   <li><b>Erro</b>: a entrada sai da foto do servidor (ex.: kaiju com habilidade inexistente). Nunca crash.</li>
 *   <li><b>Aviso</b>: a entrada fica (ex.: item de arma que so sera registrado num modulo futuro).</li>
 * </ul>
 */
public final class DataValidation {

    // Ultimo relatorio; mesmo ciclo de vida das fotos de dados (limpo ao parar o servidor).
    private static volatile DataReport lastReport = new DataReport();

    private DataValidation() {
    }

    public static DataReport lastReport() {
        return lastReport;
    }

    /** Valida tudo e publica as fotos do servidor. */
    public static DataReport run() {
        DataReport report = new DataReport();
        // Arquivos que o Codec ja recusou no carregamento tambem sao erros do relatorio (antes so iam para o log).
        KN8Data.ALL.forEach(registry -> registry.parseErrors().forEach(report::error));
        Map<ResourceLocation, RankDef> ranks = publish(KN8Data.RANK, validateRanks(KN8Data.RANK.loaded(), report),
                report);
        Map<ResourceLocation, AbilityDef> abilities = publish(KN8Data.ABILITY, KN8Data.ABILITY.loaded(), report);
        Map<ResourceLocation, DismantleDef> dismantles = publish(KN8Data.DISMANTLE,
                validateDismantles(KN8Data.DISMANTLE.loaded(), report), report);
        Map<ResourceLocation, KaijuDef> kaiju = publish(KN8Data.KAIJU,
                validateKaiju(KN8Data.KAIJU.loaded(), abilities.keySet(), dismantles.keySet(), report), report);
        Map<ResourceLocation, BossDef> bosses = publish(KN8Data.BOSS,
                validateBosses(KN8Data.BOSS.loaded(), kaiju.keySet(), abilities.keySet(), report), report);
        publish(KN8Data.WEAPON, validateWeapons(KN8Data.WEAPON.loaded(), ranks.keySet(), report), report);
        publish(KN8Data.SUIT, validateSuits(KN8Data.SUIT.loaded(), ranks.keySet(), report), report);
        Map<ResourceLocation, MissionDef> missions = publish(KN8Data.MISSION,
                validateMissions(KN8Data.MISSION.loaded(), ranks.keySet(), kaiju.keySet(), bosses.keySet(), report),
                report);
        publish(KN8Data.SOLDIER, validateSoldiers(KN8Data.SOLDIER.loaded(), report), report);
        publish(KN8Data.WORKBENCH, validateWorkbench(KN8Data.WORKBENCH.loaded(), report), report);
        publish(KN8Data.NUMBERED, validateNumbered(KN8Data.NUMBERED.loaded(), kaiju.keySet(),
                bosses.keySet(), report), report);
        publish(KN8Data.INVASION, validateInvasions(KN8Data.INVASION.loaded(), kaiju.keySet(), bosses.keySet(), report),
                report);
        // Patentes apontam para missoes de avaliacao, que so foram validadas agora: confere no fim (so aviso).
        ranks.forEach((id, rank) -> rank.promotionMission().filter(mission -> !missions.containsKey(mission))
                .ifPresent(mission -> report.warning("rank " + id + ": promotion_mission inexistente " + mission)));

        report.errors().forEach(message -> KN8Constants.LOGGER.error("[kn8] Dados: {}", message));
        report.warnings().forEach(message -> KN8Constants.LOGGER.warn("[kn8] Dados: {}", message));
        KN8Constants.LOGGER.info("[kn8] Dados validados: {}", report.summary());
        lastReport = report;
        return report;
    }

    static void clear() {
        lastReport = new DataReport();
    }

    private static <T> Map<ResourceLocation, T> publish(DataRegistry<T> registry, Map<ResourceLocation, T> valid,
            DataReport report) {
        registry.publishValidated(valid);
        report.count(registry.id().getPath(), valid.size());
        return registry.server();
    }

    /** 0.2 (Etapa 3): resultado e ingredientes precisam ser itens registrados (senao a receita sai). */
    public static Map<ResourceLocation, WorkbenchRecipeDef> validateWorkbench(
            Map<ResourceLocation, WorkbenchRecipeDef> input, DataReport report) {
        Map<ResourceLocation, WorkbenchRecipeDef> valid = new LinkedHashMap<>();
        input.forEach((id, def) -> {
            String where = "workbench " + id;
            if (!BuiltInRegistries.ITEM.containsKey(def.result())) {
                report.error(where + ": result nao registrado " + def.result());
                return;
            }
            for (WorkbenchRecipeDef.Ingredient ingredient : def.ingredients()) {
                if (!BuiltInRegistries.ITEM.containsKey(ingredient.item())) {
                    report.error(where + ": ingrediente nao registrado " + ingredient.item());
                    return;
                }
            }
            valid.put(id, def);
        });
        return valid;
    }

    /** 0.2 (Etapa 8): o numerado e as especies de {@code revive} (carcaca e revivida) precisam existir. */
    public static Map<ResourceLocation, NumberedDef> validateNumbered(Map<ResourceLocation, NumberedDef> input,
            Set<ResourceLocation> kaiju, Set<ResourceLocation> bosses, DataReport report) {
        Map<ResourceLocation, NumberedDef> valid = new LinkedHashMap<>();
        input.forEach((id, def) -> {
            String where = "numbered " + id;
            if (!kaiju.contains(id)) {
                report.error(where + ": o id precisa ser um kaiju existente");
                return;
            }
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : def.revive().entrySet()) {
                if (!kaiju.contains(entry.getKey()) || !kaiju.contains(entry.getValue())) {
                    report.error(where + ": revive com especie inexistente " + entry);
                    return;
                }
            }
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : def.reviveBoss().entrySet()) {
                if (!kaiju.contains(entry.getKey()) || !bosses.contains(entry.getValue())) {
                    report.error(where + ": revive_boss com especie ou chefe inexistente " + entry);
                    return;
                }
            }
            valid.put(id, def);
        });
        return valid;
    }

    /** 0.2 (Etapa 7): especies e chefes das ondas precisam existir; invasao sem kaiju sai. */
    public static Map<ResourceLocation, InvasionDef> validateInvasions(Map<ResourceLocation, InvasionDef> input,
            Set<ResourceLocation> kaiju, Set<ResourceLocation> bosses, DataReport report) {
        Map<ResourceLocation, InvasionDef> valid = new LinkedHashMap<>();
        input.forEach((id, def) -> {
            String where = "invasion " + id;
            for (InvasionDef.Wave wave : def.waves()) {
                for (InvasionDef.Spawn spawn : wave.kaiju()) {
                    if (!kaiju.contains(spawn.species())) {
                        report.error(where + ": especie inexistente " + spawn.species());
                        return;
                    }
                }
                if (wave.boss().isPresent() && !bosses.contains(wave.boss().get())) {
                    report.error(where + ": chefe inexistente " + wave.boss().get());
                    return;
                }
            }
            if (def.totalKaiju() == 0) {
                report.error(where + ": nenhuma onda com kaiju");
                return;
            }
            valid.put(id, def);
        });
        return valid;
    }

    /** 0.1-B: soldado precisa de ao menos um nivel e uma variante; arma inexistente vira aviso (variante sem arma). */
    public static Map<ResourceLocation, SoldierDef> validateSoldiers(Map<ResourceLocation, SoldierDef> input,
            DataReport report) {
        Map<ResourceLocation, SoldierDef> valid = new LinkedHashMap<>();
        input.forEach((id, def) -> {
            String where = "soldier " + id;
            if (def.powerLevels().isEmpty() || def.variants().isEmpty()) {
                report.error(where + ": precisa de power_levels e variants");
                return;
            }
            def.variants().forEach((name, item) -> {
                if (!BuiltInRegistries.ITEM.containsKey(item)) {
                    report.warning(where + ": arma da variante " + name + " nao registrada " + item);
                }
            });
            valid.put(id, def);
        });
        return valid;
    }

    public static Map<ResourceLocation, RankDef> validateRanks(Map<ResourceLocation, RankDef> input,
            DataReport report) {
        Map<ResourceLocation, RankDef> valid = new HashMap<>();
        Set<Integer> orders = new HashSet<>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (!orders.add(entry.getValue().order())) {
                report.error("rank " + entry.getKey() + ": order " + entry.getValue().order() + " repetido");
            } else {
                valid.put(entry.getKey(), entry.getValue());
            }
        });
        return valid;
    }

    public static Map<ResourceLocation, DismantleDef> validateDismantles(Map<ResourceLocation, DismantleDef> input,
            DataReport report) {
        input.forEach((id, def) -> def.drops().forEach(drop -> warnMissingItem("dismantle " + id, drop.item(),
                report)));
        return input;
    }

    public static Map<ResourceLocation, KaijuDef> validateKaiju(Map<ResourceLocation, KaijuDef> input,
            Set<ResourceLocation> abilities, Set<ResourceLocation> dismantles, DataReport report) {
        Map<ResourceLocation, KaijuDef> valid = new HashMap<>();
        input.forEach((id, def) -> {
            String where = "kaiju " + id;
            boolean ok = true;
            if (!KaijuScale.fits(def.kaijuClass().getSerializedName(), def.dimensions().width(),
                    def.dimensions().height())) {
                // Etapa C: so aviso (a especie continua valendo), para escalas fora da faixa da categoria.
                report.warning(where + ": tamanho " + KaijuScale.size(def.dimensions().width(),
                        def.dimensions().height()) + " fora da faixa da categoria " + def.kaijuClass()
                        .getSerializedName());
            }
            Set<String> names = new HashSet<>();
            long cores = 0;
            for (KaijuDef.Part part : def.parts()) {
                if (!names.add(part.name())) {
                    ok = error(report, where + ": parte repetida '" + part.name() + "'");
                }
                if (part.core()) {
                    cores++;
                }
            }
            if (cores > 1) {
                ok = error(report, where + ": mais de uma parte marcada como nucleo");
            }
            for (ResourceLocation ability : def.abilities()) {
                if (!abilities.contains(ability)) {
                    ok = error(report, where + ": habilidade inexistente " + ability);
                }
            }
            Optional<ResourceLocation> dismantle = def.dismantle();
            if (dismantle.isPresent() && !dismantles.contains(dismantle.get())) {
                ok = error(report, where + ": tabela de desmonte inexistente " + dismantle.get());
            }
            if (ok) {
                valid.put(id, def);
            }
        });
        return valid;
    }

    public static Map<ResourceLocation, BossDef> validateBosses(Map<ResourceLocation, BossDef> input,
            Set<ResourceLocation> kaiju, Set<ResourceLocation> abilities, DataReport report) {
        Map<ResourceLocation, BossDef> valid = new HashMap<>();
        input.forEach((id, def) -> {
            String where = "boss " + id;
            boolean ok = true;
            if (!kaiju.contains(def.kaiju())) {
                ok = error(report, where + ": kaiju inexistente " + def.kaiju());
            }
            if (def.phases().isEmpty()) {
                ok = error(report, where + ": precisa de pelo menos uma fase");
            }
            for (BossDef.Phase phase : def.phases()) {
                if (phase.attacks().isEmpty()) {
                    ok = error(report, where + ": fase sem ataques");
                }
                for (BossDef.Attack attack : phase.attacks()) {
                    if (!abilities.contains(attack.ability())) {
                        ok = error(report, where + ": habilidade inexistente " + attack.ability());
                    }
                }
            }
            if (ok) {
                valid.put(id, def);
            }
        });
        return valid;
    }

    public static Map<ResourceLocation, WeaponDef> validateWeapons(Map<ResourceLocation, WeaponDef> input,
            Set<ResourceLocation> ranks, DataReport report) {
        Map<ResourceLocation, WeaponDef> valid = new HashMap<>();
        input.forEach((id, def) -> {
            String where = "weapon " + id;
            if (!ranks.contains(def.requiredRank())) {
                error(report, where + ": required_rank inexistente " + def.requiredRank());
                return;
            }
            if (def.actions().isEmpty()) {
                error(report, where + ": precisa de pelo menos uma acao");
                return;
            }
            warnMissingItem(where, def.item(), report);
            valid.put(id, def);
        });
        return valid;
    }

    public static Map<ResourceLocation, SuitDef> validateSuits(Map<ResourceLocation, SuitDef> input,
            Set<ResourceLocation> ranks, DataReport report) {
        Map<ResourceLocation, SuitDef> valid = new HashMap<>();
        input.forEach((id, def) -> {
            if (ranks.contains(def.requiredRank())) {
                valid.put(id, def);
            } else {
                error(report, "suit " + id + ": required_rank inexistente " + def.requiredRank());
            }
        });
        return valid;
    }

    public static Map<ResourceLocation, MissionDef> validateMissions(Map<ResourceLocation, MissionDef> input,
            Set<ResourceLocation> ranks, Set<ResourceLocation> kaiju, Set<ResourceLocation> bosses,
            DataReport report) {
        Map<ResourceLocation, MissionDef> valid = new HashMap<>();
        input.forEach((id, def) -> {
            String where = "mission " + id;
            boolean ok = def.objectives().isEmpty() ? error(report, where + ": sem objetivos") : true;
            if (def.requires().rank().isPresent() && !ranks.contains(def.requires().rank().get())) {
                ok = error(report, where + ": requires.rank inexistente " + def.requires().rank().get());
            }
            if (def.rewards().promoteTo().isPresent() && !ranks.contains(def.rewards().promoteTo().get())) {
                ok = error(report, where + ": rewards.promote_to inexistente " + def.rewards().promoteTo().get());
            }
            for (MissionDef.Objective objective : def.objectives()) {
                ok &= validateObjective(where, objective, kaiju, bosses, report);
            }
            for (ResourceLocation item : def.rewards().items()) {
                warnMissingItem(where, item, report);
            }
            if (ok) {
                valid.put(id, def);
            }
        });
        // requires.completed aponta para outras missoes: confere depois que todas foram vistas, repetindo ate
        // estabilizar (se A depende de B e B saiu, A sai tambem).
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<Map.Entry<ResourceLocation, MissionDef>> iterator = valid.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ResourceLocation, MissionDef> entry = iterator.next();
                Optional<ResourceLocation> missing = entry.getValue().requires().completed().stream()
                        .filter(required -> !valid.containsKey(required))
                        .findFirst();
                if (missing.isPresent()) {
                    report.error("mission " + entry.getKey() + ": requires.completed inexistente " + missing.get());
                    iterator.remove();
                    changed = true;
                }
            }
        }
        return valid;
    }

    private static boolean validateObjective(String where, MissionDef.Objective objective,
            Set<ResourceLocation> kaiju, Set<ResourceLocation> bosses, DataReport report) {
        String label = where + ", objetivo " + objective.type().getSerializedName();
        return switch (objective.type()) {
            case KILL_KAIJU, DISMANTLE -> objective.target().filter(kaiju::contains).isPresent()
                    || error(report, label + ": target precisa ser um kaiju existente");
            case DEFEAT_BOSS -> objective.target().filter(bosses::contains).isPresent()
                    || error(report, label + ": target precisa ser um chefe existente");
            case REACH_AREA -> objective.marker().isPresent()
                    || error(report, label + ": precisa de marker");
            case PATROL -> true;
            // Invasoes sao validadas depois das missoes: aqui basta o arquivo ter carregado.
            case DEFEND_INVASION -> objective.target().filter(KN8Data.INVASION.loaded()::containsKey).isPresent()
                    || error(report, label + ": target precisa ser uma invasao existente");
        };
    }

    /** Itens chegam em modulos futuros (M10, M11): ausencia e aviso, nao erro. */
    private static void warnMissingItem(String where, ResourceLocation item, DataReport report) {
        if (!BuiltInRegistries.ITEM.containsKey(item)) {
            report.warning(where + ": item ainda nao registrado " + item);
        }
    }

    /** Registra o erro e devolve false, para encadear em {@code ok = error(...)}. */
    private static boolean error(DataReport report, String message) {
        report.error(message);
        return false;
    }
}
