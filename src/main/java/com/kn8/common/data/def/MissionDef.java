// src/main/java/com/kn8/common/data/def/MissionDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Missao ({@code data/<ns>/kn8/mission/<id>.json}), GDD secao 20 e Fase 4 secao 4.4. Objetivos sao tipados por
 * {@code type}; os tipos da 0.1 e o campo que cada um exige estao em {@link ObjectiveType}.
 */
public record MissionDef(Category category, Optional<ResourceLocation> giver, Requires requires,
        List<Objective> objectives, Fail fail, Rewards rewards, boolean repeatable, int cooldownTicks) {

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
        DISMANTLE("dismantle");

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
        PATROL("patrol");

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

    public record Objective(ObjectiveType type, Optional<ResourceLocation> target, int count,
            Optional<String> marker, Optional<ResourceLocation> structure) {
        public static final Codec<Objective> CODEC = RecordCodecBuilder.create(i -> i.group(
                ObjectiveType.CODEC.fieldOf("type").forGetter(Objective::type),
                ResourceLocation.CODEC.optionalFieldOf("target").forGetter(Objective::target),
                Codec.intRange(1, 10_000).optionalFieldOf("count", 1).forGetter(Objective::count),
                Codec.STRING.optionalFieldOf("marker").forGetter(Objective::marker),
                ResourceLocation.CODEC.optionalFieldOf("structure").forGetter(Objective::structure)
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

    public record Rewards(int merit, Optional<ResourceLocation> promoteTo, List<ResourceLocation> items) {
        public static final Rewards NONE = new Rewards(0, Optional.empty(), List.of());
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1_000_000).optionalFieldOf("merit", 0).forGetter(Rewards::merit),
                ResourceLocation.CODEC.optionalFieldOf("promote_to").forGetter(Rewards::promoteTo),
                DefCodecs.IDS.optionalFieldOf("items", List.of()).forGetter(Rewards::items)
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
            DefCodecs.TICKS.optionalFieldOf("cooldown_ticks", 0).forGetter(MissionDef::cooldownTicks)
    ).apply(i, MissionDef::new));
}
