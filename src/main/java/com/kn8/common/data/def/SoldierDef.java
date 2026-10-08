// src/main/java/com/kn8/common/data/def/SoldierDef.java
package com.kn8.common.data.def;

import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Soldado da Forca de Defesa ({@code data/<ns>/kn8/soldier/<id>.json}, 0.1-B / Etapa F). Uma entidade so; variante
 * (arma) e nivel de potencia vem daqui.
 *
 * <ul>
 *   <li>{@code power_levels}: nome do nivel -> % de Release do traje (baixa, normal, alta, elite). O Release usa as
 *   MESMAS formulas do jogador (PowerMath): dano, velocidade e reducao de dano. Soldado elite continua muito abaixo
 *   de um kaiju forte (a forca vem do numero de soldados).</li>
 *   <li>{@code variants}: nome da variante -> {@link Variant} (arma, arma de apoio, peso no sorteio) ou so o item da
 *   arma ({@code minecraft:air} = sem arma). O dano e o ritmo vem do JSON da propria arma (weapon/), como no
 *   jogador. Soldados comuns usam so armas comuns; armas especiais (o machado) ficam para os soldados especiais.</li>
 *   <li>{@code kaiju_damage} (0.3, decisao do Miguel): nivel -> multiplicador do dano contra kaiju. Soldados comuns
 *   so derrubam kaiju em grupo; os de nivel alto/elite resolvem sozinhos. Sem a chave do nivel, 1,0.</li>
 * </ul>
 */
public record SoldierDef(float health, float armor, float speed, float followRange, float unarmedDamage,
        int unarmedIntervalTicks, float keepDistance, Map<String, Integer> powerLevels,
        Map<String, Variant> variants, Map<String, Float> kaijuDamage, float sidearmDistance,
        Map<String, LevelStats> levelStats) {

    /**
     * Balanceamento v1.0: vida, armadura e velocidade por nivel de forca (recruta/baixo 20, normal 24, alto 28,
     * elite 34). Nivel sem entrada usa os valores de cima do JSON.
     */
    public record LevelStats(float health, float armor, float speed) {
        public static final Codec<LevelStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(1.0F, 1000.0F).fieldOf("health").forGetter(LevelStats::health),
                Codec.floatRange(0.0F, 30.0F).fieldOf("armor").forGetter(LevelStats::armor),
                Codec.floatRange(0.01F, 2.0F).fieldOf("speed").forGetter(LevelStats::speed)
        ).apply(i, LevelStats::new));
    }

    /** Vida, armadura e velocidade do nivel (ou as de cima do JSON). */
    public LevelStats statsFor(String level) {
        return levelStats.getOrDefault(level, new LevelStats(health, armor, speed));
    }

    /**
     * Variante de soldado comum (0.4): arma principal, arma de apoio opcional (o atirador troca para ela quando o kaiju
     * chega perto, ver {@code sidearm_distance}) e peso no sorteio ({@code weight}; 0 = so quando pedida pelo nome).
     * Aceita tambem o formato antigo: so o id da arma.
     */
    public record Variant(ResourceLocation weapon, Optional<ResourceLocation> sidearm, int weight) {
        private static final Codec<Variant> FULL = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("weapon").forGetter(Variant::weapon),
                ResourceLocation.CODEC.optionalFieldOf("sidearm").forGetter(Variant::sidearm),
                Codec.intRange(0, 1000).optionalFieldOf("weight", 1).forGetter(Variant::weight)
        ).apply(i, Variant::new));

        public static final Codec<Variant> CODEC = Codec.either(ResourceLocation.CODEC, FULL).xmap(
                either -> either.map(id -> new Variant(id, Optional.empty(), 1), variant -> variant),
                Either::right);
    }

    public static final Codec<SoldierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(1.0F, 1000.0F).fieldOf("health").forGetter(SoldierDef::health),
            Codec.floatRange(0.0F, 30.0F).optionalFieldOf("armor", 0.0F).forGetter(SoldierDef::armor),
            Codec.floatRange(0.01F, 2.0F).fieldOf("speed").forGetter(SoldierDef::speed),
            Codec.floatRange(1.0F, 128.0F).optionalFieldOf("follow_range", 32.0F).forGetter(SoldierDef::followRange),
            Codec.floatRange(0.0F, 100.0F).optionalFieldOf("unarmed_damage", 2.0F)
                    .forGetter(SoldierDef::unarmedDamage),
            DefCodecs.TICKS.optionalFieldOf("unarmed_interval_ticks", 12).forGetter(SoldierDef::unarmedIntervalTicks),
            Codec.floatRange(0.0F, 64.0F).optionalFieldOf("keep_distance", 12.0F)
                    .forGetter(SoldierDef::keepDistance),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(0, 100)).fieldOf("power_levels")
                    .forGetter(SoldierDef::powerLevels),
            Codec.unboundedMap(Codec.STRING, Variant.CODEC).fieldOf("variants").forGetter(SoldierDef::variants),
            Codec.unboundedMap(Codec.STRING, Codec.floatRange(0.0F, 10.0F)).optionalFieldOf("kaiju_damage", Map.of())
                    .forGetter(SoldierDef::kaijuDamage),
            Codec.floatRange(0.0F, 32.0F).optionalFieldOf("sidearm_distance", 3.5F)
                    .forGetter(SoldierDef::sidearmDistance),
            Codec.unboundedMap(Codec.STRING, LevelStats.CODEC).optionalFieldOf("level_stats", Map.of())
                    .forGetter(SoldierDef::levelStats)
    ).apply(i, SoldierDef::new));

    /** Sorteia uma variante pelo {@code weight} (soldados comuns sem variante pedida: ovo, invasao "random"). */
    public String randomVariant(RandomSource random, String fallback) {
        int total = variants.values().stream().mapToInt(Variant::weight).sum();
        if (total <= 0) {
            return fallback;
        }
        int roll = random.nextInt(total);
        for (Map.Entry<String, Variant> entry : variants.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            roll -= entry.getValue().weight();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return fallback;
    }
}
