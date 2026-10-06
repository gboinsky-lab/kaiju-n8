// src/main/java/com/kn8/common/data/def/DismantleDef.java
package com.kn8.common.data.def;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Tabela de desmonte de carcaca ({@code data/<ns>/kn8/dismantle/<id>.json}), GDD secao 29. O drop pode depender
 * do estado do nucleo registrado na carcaca (PT7): matar pelo nucleo e mais rapido, mas destroi o nucleo.
 */
public record DismantleDef(int steps, String tool, int ticksPerStep, List<Drop> drops) {

    /** Quando um drop vale. */
    public enum Condition implements StringRepresentable {
        ALWAYS("always"),
        CORE_INTACT("core_intact"),
        CORE_DESTROYED("core_destroyed");

        public static final Codec<Condition> CODEC = StringRepresentable.fromEnum(Condition::values);

        private final String serializedName;

        Condition(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public record Drop(ResourceLocation item, int minCount, int maxCount, Condition condition) {
        public static final Codec<Drop> CODEC = RecordCodecBuilder.<Drop>create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Drop::item),
                Codec.intRange(0, 64).fieldOf("min_count").forGetter(Drop::minCount),
                Codec.intRange(0, 64).fieldOf("max_count").forGetter(Drop::maxCount),
                Condition.CODEC.optionalFieldOf("condition", Condition.ALWAYS).forGetter(Drop::condition)
        ).apply(i, Drop::new)).validate(drop -> drop.minCount() <= drop.maxCount()
                ? DataResult.success(drop)
                : DataResult.error(() -> "min_count maior que max_count em " + drop.item()));
    }

    public static final Codec<DismantleDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 100).fieldOf("steps").forGetter(DismantleDef::steps),
            Codec.STRING.fieldOf("tool").forGetter(DismantleDef::tool),
            Codec.intRange(1, 1200).fieldOf("ticks_per_step").forGetter(DismantleDef::ticksPerStep),
            Drop.CODEC.listOf().fieldOf("drops").forGetter(DismantleDef::drops)
    ).apply(i, DismantleDef::new));
}
