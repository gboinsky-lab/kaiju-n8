// src/main/java/com/kn8/common/data/def/SoldierDef.java
package com.kn8.common.data.def;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Soldado da Forca de Defesa ({@code data/<ns>/kn8/soldier/<id>.json}, 0.1-B / Etapa F). Uma entidade so; variante
 * (arma) e nivel de potencia vem daqui.
 *
 * <ul>
 *   <li>{@code power_levels}: nome do nivel -> % de Release do traje (baixa, normal, alta, elite). O Release usa as
 *   MESMAS formulas do jogador (PowerMath): dano, velocidade e reducao de dano. Soldado elite continua muito abaixo
 *   de um kaiju forte (a forca vem do numero de soldados).</li>
 *   <li>{@code variants}: nome da variante -> item da arma ({@code minecraft:air} = sem arma). O dano e o ritmo vem do
 *   JSON da propria arma (weapon/), como no jogador.</li>
 *   <li>{@code kaiju_damage} (0.3, decisao do Miguel): nivel -> multiplicador do dano contra kaiju. Soldados comuns
 *   so derrubam kaiju em grupo; os de nivel alto/elite resolvem sozinhos. Sem a chave do nivel, 1,0.</li>
 * </ul>
 */
public record SoldierDef(float health, float armor, float speed, float followRange, float unarmedDamage,
        int unarmedIntervalTicks, float keepDistance, Map<String, Integer> powerLevels,
        Map<String, ResourceLocation> variants, Map<String, Float> kaijuDamage) {

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
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("variants")
                    .forGetter(SoldierDef::variants),
            Codec.unboundedMap(Codec.STRING, Codec.floatRange(0.0F, 10.0F)).optionalFieldOf("kaiju_damage", Map.of())
                    .forGetter(SoldierDef::kaijuDamage)
    ).apply(i, SoldierDef::new));
}
