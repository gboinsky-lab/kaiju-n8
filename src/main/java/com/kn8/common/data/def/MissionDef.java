// src/main/java/com/kn8/common/data/def/MissionDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Missao ({@code data/<ns>/kn8/mission/<id>.json}), GDD secao 20 e Fase 4 secao 4.4. Objetivos sao tipados por
 * {@code type}; os tipos da 0.1 e o campo que cada um exige estao em {@link ObjectiveType}.
 */
public record MissionDef(Category category, Optional<ResourceLocation> giver, Requires requires,
        List<Objective> objectives, Fail fail, Rewards rewards, boolean repeatable, int cooldownTicks,
        Optional<Grading> grading, Optional<Arena> arena) {

    /**
     * EXAME (Miguel, 2026-10-30: "exame de admissao numa area propria, com armas, armadura, municao e itens entregues
     * no inicio"): prova que so pode ser aceita dentro do local {@code site} (registro {@code site}, ex. o Campo do
     * Exame de Admissao); os alvos surgem entre {@code min_distance} e {@code max_distance} do centro do local; o
     * {@code kit} e entregue ao aceitar (armadura vestida se o slot estiver livre, armas de fogo e pentes cheios); o
     * aprovado fica com ele (Miguel, 2026-10-30) e, com {@code return_kit}, quem reprova, abandona ou cai devolve.
     * {@code candidates}: soldados que fazem a prova junto com o jogador (Miguel: "implemente alguns soldados para fazer
     * o exame com o jogador").
     */
    public record Arena(Optional<ResourceLocation> site, double minDistance, double maxDistance, List<KitItem> kit,
            boolean returnKit, int candidates) {
        public static final Codec<Arena> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("site").forGetter(Arena::site),
                Codec.doubleRange(0.0, 256.0).optionalFieldOf("min_distance", 6.0).forGetter(Arena::minDistance),
                Codec.doubleRange(1.0, 256.0).optionalFieldOf("max_distance", 14.0).forGetter(Arena::maxDistance),
                KitItem.CODEC.listOf().optionalFieldOf("kit", List.of()).forGetter(Arena::kit),
                Codec.BOOL.optionalFieldOf("return_kit", true).forGetter(Arena::returnKit),
                Codec.intRange(0, 16).optionalFieldOf("candidates", 0).forGetter(Arena::candidates)
        ).apply(i, Arena::new));
    }

    /** Item do kit do exame: id e quantidade. */
    public record KitItem(ResourceLocation item, int count) {
        public static final Codec<KitItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(KitItem::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(KitItem::count)
        ).apply(i, KitItem::new));
    }

    /**
     * PM-7: prova de promocao. Requisitos medidos durante a missao (todos opcionais): precisao minima com um minimo
     * de tiros, dano recebido maximo (fracao da vida maxima), aliados do esquadrao perdidos, tempo ideal (so na nota),
     * nota minima e esquadrao minimo para aceitar. Reprovada: nao conclui, nao da recompensa e so pode tentar de novo
     * depois de {@code retry_cooldown_ticks}.
     */
    public record Grading(Optional<Double> minAccuracy, int minShots, Optional<Double> maxDamageTaken,
            Optional<Integer> maxAlliesLost, Optional<Integer> parTicks, String minGrade, int minSquad,
            int retryCooldownTicks) {
        public static final Codec<Grading> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.0, 1.0).optionalFieldOf("min_accuracy").forGetter(Grading::minAccuracy),
                Codec.intRange(1, 10_000).optionalFieldOf("min_shots", 10).forGetter(Grading::minShots),
                Codec.doubleRange(0.0, 100.0).optionalFieldOf("max_damage_taken").forGetter(Grading::maxDamageTaken),
                Codec.intRange(0, 64).optionalFieldOf("max_allies_lost").forGetter(Grading::maxAlliesLost),
                Codec.intRange(1, 720_000).optionalFieldOf("par_ticks").forGetter(Grading::parTicks),
                Codec.STRING.validate(s -> s.length() == 1 && "sabcd".contains(s.toLowerCase())
                        ? DataResult.success(s) : DataResult.error(() -> "min_grade deve ser s, a, b, c ou d: " + s))
                        .optionalFieldOf("min_grade", "d").forGetter(Grading::minGrade),
                Codec.intRange(0, 64).optionalFieldOf("min_squad", 0).forGetter(Grading::minSquad),
                DefCodecs.TICKS.optionalFieldOf("retry_cooldown_ticks", 6000).forGetter(Grading::retryCooldownTicks)
        ).apply(i, Grading::new));
    }

    /** Categorias do GDD (secao 20). */
    public enum Category implements StringRepresentable {
        STORY("story"),
        PATROL("patrol"),
        INVESTIGATION("investigation"),
        EXTERMINATION("extermination"),
        DEFENSE("defense"),
        RESCUE("rescue"),
        ESCORT("escort"),
        BOSS("boss"),
        EMERGENCY("emergency"),
        DISMANTLE("dismantle"),
        /**
         * PM-8B (Miguel: "prova acontece dentro da base e serve como um curso ou para fortificar o corpo"): aceita so
         * com o Oficial de Provas perto, alvos dentro da base, sem merito por abate; da XP do corpo.
         */
        EXAM("exam");

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        private final String serializedName;

        Category(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** Tipos de objetivo da 0.1 e o que cada um referencia. */
    public enum ObjectiveType implements StringRepresentable {
        /** Matar {@code count} kaiju da especie {@code target} (registry kaiju). */
        KILL_KAIJU("kill_kaiju"),
        /** Desmontar {@code count} carcacas da especie {@code target} (registry kaiju). */
        DISMANTLE("dismantle"),
        /** Derrotar o chefe {@code target} (registry boss). */
        DEFEAT_BOSS("defeat_boss"),
        /** Chegar ao marcador {@code marker} (estrutura opcional em {@code structure}). */
        REACH_AREA("reach_area"),
        /** Visitar {@code count} pontos de patrulha. */
        PATROL("patrol"),
        /** 0.2 (Etapa 7): vencer a invasao {@code target} (registry invasion), que comeca perto do jogador. */
        DEFEND_INVASION("defend_invasion"),
        /**
         * PM-10A: levar {@code count} civis ao ponto de extracao. Surgem {@code count + max_losses} civis perto do
         * jogador; {@code target} (opcional) e o kaiju que ameaca a evacuacao. Mais perdas que {@code max_losses}
         * fracassam a missao.
         */
        EVACUATE("evacuate");

        public static final Codec<ObjectiveType> CODEC = StringRepresentable.fromEnum(ObjectiveType::values);

        private final String serializedName;

        ObjectiveType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** {@code max_losses} (PM-10A, so {@code evacuate}): civis que podem morrer sem fracassar a missao. */
    public record Objective(ObjectiveType type, Optional<ResourceLocation> target, int count,
            Optional<String> marker, Optional<ResourceLocation> structure, int maxLosses) {
        public static final Codec<Objective> CODEC = RecordCodecBuilder.create(i -> i.group(
                ObjectiveType.CODEC.fieldOf("type").forGetter(Objective::type),
                ResourceLocation.CODEC.optionalFieldOf("target").forGetter(Objective::target),
                Codec.intRange(1, 10_000).optionalFieldOf("count", 1).forGetter(Objective::count),
                Codec.STRING.optionalFieldOf("marker").forGetter(Objective::marker),
                ResourceLocation.CODEC.optionalFieldOf("structure").forGetter(Objective::structure),
                Codec.intRange(0, 64).optionalFieldOf("max_losses", 0).forGetter(Objective::maxLosses)
        ).apply(i, Objective::new));
    }

    public record Requires(Optional<ResourceLocation> rank, List<ResourceLocation> completed) {
        public static final Requires NONE = new Requires(Optional.empty(), List.of());
        public static final Codec<Requires> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("rank").forGetter(Requires::rank),
                DefCodecs.IDS.optionalFieldOf("completed", List.of()).forGetter(Requires::completed)
        ).apply(i, Requires::new));
    }

    public record Fail(Optional<Integer> timeTicks, boolean onDeath) {
        public static final Fail NEVER = new Fail(Optional.empty(), false);
        public static final Codec<Fail> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 720_000).optionalFieldOf("time_ticks").forGetter(Fail::timeTicks),
                Codec.BOOL.optionalFieldOf("on_death", false).forGetter(Fail::onDeath)
        ).apply(i, Fail::new));
    }

    /**
     * {@code training_xp} (0.2): XP de treino de Release dado ao concluir. {@code body_xp} (PM-8B): XP dos atributos
     * do corpo por chave ({@code strength}, {@code speed}, {@code resistance}, {@code agility}).
     */
    public record Rewards(int merit, Optional<ResourceLocation> promoteTo, List<ResourceLocation> items,
            int trainingXp, Map<String, Double> bodyXp) {
        public static final Rewards NONE = new Rewards(0, Optional.empty(), List.of(), 0, Map.of());
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1_000_000).optionalFieldOf("merit", 0).forGetter(Rewards::merit),
                ResourceLocation.CODEC.optionalFieldOf("promote_to").forGetter(Rewards::promoteTo),
                DefCodecs.IDS.optionalFieldOf("items", List.of()).forGetter(Rewards::items),
                Codec.intRange(0, 1_000_000).optionalFieldOf("training_xp", 0).forGetter(Rewards::trainingXp),
                Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0.0, 1_000_000.0)).optionalFieldOf("body_xp",
                        Map.of()).forGetter(Rewards::bodyXp)
        ).apply(i, Rewards::new));
    }

    public static final Codec<MissionDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Category.CODEC.fieldOf("category").forGetter(MissionDef::category),
            ResourceLocation.CODEC.optionalFieldOf("giver").forGetter(MissionDef::giver),
            Requires.CODEC.optionalFieldOf("requires", Requires.NONE).forGetter(MissionDef::requires),
            Objective.CODEC.listOf().fieldOf("objectives").forGetter(MissionDef::objectives),
            Fail.CODEC.optionalFieldOf("fail", Fail.NEVER).forGetter(MissionDef::fail),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(MissionDef::rewards),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(MissionDef::repeatable),
            DefCodecs.TICKS.optionalFieldOf("cooldown_ticks", 0).forGetter(MissionDef::cooldownTicks),
            Grading.CODEC.optionalFieldOf("grading").forGetter(MissionDef::grading),
            Arena.CODEC.optionalFieldOf("arena").forGetter(MissionDef::arena)
    ).apply(i, MissionDef::new));
}
