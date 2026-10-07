// src/main/java/com/kn8/common/data/def/KaijuDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Definicao de uma especie de kaiju ({@code data/<ns>/kn8/kaiju/<id>.json}), Fase 4 secao 4.4.
 * Vida, dano e armadura saem da curva de fortitude do config, a nao ser que {@code overrides} os fixe.
 * Medidas de partes e hitbox: blocos; offset das partes: [direita, cima, frente] relativo aos pes do kaiju.
 */
public record KaijuDef(KaijuClass kaijuClass, float fortitude, KaijuSize size, Dimensions dimensions,
        Overrides overrides, double speed, int intelligence, List<Part> parts, Core core,
        List<ResourceLocation> abilities, List<String> weaknesses, Optional<ResourceLocation> dismantle, Spawn spawn,
        Rarity rarity, List<String> tags, Optional<Rage> rage) {

    /**
     * 0.6 (especificacao do Miguel, "Revived Rage"/"Berserk"): com a vida abaixo de {@code health_below}, o kaiju
     * fica enfurecido de vez: dano e velocidade multiplicados e recargas das habilidades encurtadas.
     */
    public record Rage(float healthBelow, float damageMultiplier, float speedMultiplier, float cooldownMultiplier) {
        public static final Codec<Rage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("health_below").forGetter(Rage::healthBelow),
                Codec.floatRange(0.1F, 10.0F).optionalFieldOf("damage_multiplier", 1.0F)
                        .forGetter(Rage::damageMultiplier),
                Codec.floatRange(0.1F, 5.0F).optionalFieldOf("speed_multiplier", 1.0F).forGetter(Rage::speedMultiplier),
                Codec.floatRange(0.05F, 1.0F).optionalFieldOf("cooldown_multiplier", 1.0F)
                        .forGetter(Rage::cooldownMultiplier)
        ).apply(i, Rage::new));
    }

    /** Hitbox do corpo principal. */
    public record Dimensions(float width, float height) {
        public static final Codec<Dimensions> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.1F, 64.0F).fieldOf("width").forGetter(Dimensions::width),
                Codec.floatRange(0.1F, 64.0F).fieldOf("height").forGetter(Dimensions::height)
        ).apply(i, Dimensions::new));
    }

    /** Valores fixos que substituem a curva de fortitude (null no JSON = usar a curva). */
    public record Overrides(Optional<Double> health, Optional<Double> damage, Optional<Double> armor) {
        public static final Overrides NONE = new Overrides(Optional.empty(), Optional.empty(), Optional.empty());
        public static final Codec<Overrides> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1.0, 1_000_000.0).optionalFieldOf("health").forGetter(Overrides::health),
                Codec.doubleRange(0.0, 10_000.0).optionalFieldOf("damage").forGetter(Overrides::damage),
                Codec.doubleRange(0.0, 30.0).optionalFieldOf("armor").forGetter(Overrides::armor)
        ).apply(i, Overrides::new));
    }

    /** Uma hitbox de parte (PT4). No maximo uma parte pode ser o nucleo. */
    public record Part(String name, Vec3 offset, float width, float height, float multiplier, boolean core) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Part::name),
                Vec3.CODEC.fieldOf("offset").forGetter(Part::offset),
                Codec.floatRange(0.1F, 32.0F).fieldOf("width").forGetter(Part::width),
                Codec.floatRange(0.1F, 32.0F).fieldOf("height").forGetter(Part::height),
                DefCodecs.MULTIPLIER.fieldOf("multiplier").forGetter(Part::multiplier),
                Codec.BOOL.optionalFieldOf("core", false).forGetter(Part::core)
        ).apply(i, Part::new));
    }

    /** Regras do nucleo (GDD secao 12). */
    public record Core(float healthFraction, float shieldFraction, boolean fake, List<String> exposedOn) {
        public static final Core DEFAULT = new Core(0.25F, 0.0F, false, List.of());
        public static final Codec<Core> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.01F, 1.0F).optionalFieldOf("health_fraction", 0.25F).forGetter(Core::healthFraction),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("shield_fraction", 0.0F).forGetter(Core::shieldFraction),
                Codec.BOOL.optionalFieldOf("fake", false).forGetter(Core::fake),
                DefCodecs.STRINGS.optionalFieldOf("exposed_on", List.of()).forGetter(Core::exposedOn)
        ).apply(i, Core::new));
    }

    /** Onde o kaiju aparece. */
    public record Spawn(boolean natural, List<ResourceLocation> events) {
        public static final Spawn NONE = new Spawn(false, List.of());
        public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("natural", false).forGetter(Spawn::natural),
                DefCodecs.IDS.optionalFieldOf("events", List.of()).forGetter(Spawn::events)
        ).apply(i, Spawn::new));
    }

    public static final Codec<KaijuDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            KaijuClass.CODEC.fieldOf("class").forGetter(KaijuDef::kaijuClass),
            Codec.floatRange(0.1F, 12.0F).fieldOf("fortitude").forGetter(KaijuDef::fortitude),
            KaijuSize.CODEC.fieldOf("size").forGetter(KaijuDef::size),
            Dimensions.CODEC.fieldOf("dimensions").forGetter(KaijuDef::dimensions),
            Overrides.CODEC.optionalFieldOf("overrides", Overrides.NONE).forGetter(KaijuDef::overrides),
            Codec.doubleRange(0.0, 2.0).fieldOf("speed").forGetter(KaijuDef::speed),
            Codec.intRange(1, 5).optionalFieldOf("intelligence", 1).forGetter(KaijuDef::intelligence),
            Part.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(KaijuDef::parts),
            Core.CODEC.optionalFieldOf("core", Core.DEFAULT).forGetter(KaijuDef::core),
            DefCodecs.IDS.optionalFieldOf("abilities", List.of()).forGetter(KaijuDef::abilities),
            DefCodecs.STRINGS.optionalFieldOf("weaknesses", List.of()).forGetter(KaijuDef::weaknesses),
            ResourceLocation.CODEC.optionalFieldOf("dismantle").forGetter(KaijuDef::dismantle),
            Spawn.CODEC.optionalFieldOf("spawn", Spawn.NONE).forGetter(KaijuDef::spawn),
            Rarity.CODEC.optionalFieldOf("rarity", Rarity.COMMON).forGetter(KaijuDef::rarity),
            DefCodecs.STRINGS.optionalFieldOf("tags", List.of()).forGetter(KaijuDef::tags),
            Rage.CODEC.optionalFieldOf("rage").forGetter(KaijuDef::rage)
    ).apply(i, KaijuDef::new));
}
