package com.kn8.common.data.def;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Comportamento extra de um kaiju numerado ({@code data/<ns>/kn8/numbered/<id do kaiju>.json}, 0.2 Etapa 8). O
 * Kaiju No. 9 revive carcacas ({@code revive}: especie da carcaca -> especie revivida), comanda os kaiju por perto
 * (passa o seu alvo) e foge quando a vida cai a {@code flee_health} (fracao), dando {@code flee_merit} a quem
 * estava lutando por perto. 0.3: {@code revive_boss} (especie da carcaca -> chefe de {@code boss/*.json}: o Honju
 * volta como chefe com barra e fases) e a ressurreicao em massa das ondas {@code mass_revive} (raio, gesto mais longo
 * e intervalo entre um kaiju e o proximo, para o exercito levantar em sequencia).
 */
public record NumberedDef(Map<ResourceLocation, ResourceLocation> revive, float reviveRadius, int reviveCastTicks,
        int reviveCooldownTicks, int maxRevivedAlive, float commandRadius, float fleeHealth, int fleeMerit,
        Map<ResourceLocation, ResourceLocation> reviveBoss, float massReviveRadius, int massReviveCastTicks,
        int massReviveIntervalTicks) {

    public static final Codec<NumberedDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).optionalFieldOf("revive", Map.of())
                    .forGetter(NumberedDef::revive),
            Codec.floatRange(1.0F, 128.0F).optionalFieldOf("revive_radius", 24.0F).forGetter(NumberedDef::reviveRadius),
            DefCodecs.TICKS.optionalFieldOf("revive_cast_ticks", 60).forGetter(NumberedDef::reviveCastTicks),
            DefCodecs.TICKS.optionalFieldOf("revive_cooldown_ticks", 300).forGetter(NumberedDef::reviveCooldownTicks),
            Codec.intRange(0, 32).optionalFieldOf("max_revived_alive", 3).forGetter(NumberedDef::maxRevivedAlive),
            Codec.floatRange(0.0F, 128.0F).optionalFieldOf("command_radius", 32.0F)
                    .forGetter(NumberedDef::commandRadius),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flee_health", 0.3F).forGetter(NumberedDef::fleeHealth),
            Codec.intRange(0, 1_000_000).optionalFieldOf("flee_merit", 0).forGetter(NumberedDef::fleeMerit),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).optionalFieldOf("revive_boss", Map.of())
                    .forGetter(NumberedDef::reviveBoss),
            Codec.floatRange(1.0F, 256.0F).optionalFieldOf("mass_revive_radius", 72.0F)
                    .forGetter(NumberedDef::massReviveRadius),
            DefCodecs.TICKS.optionalFieldOf("mass_revive_cast_ticks", 100).forGetter(NumberedDef::massReviveCastTicks),
            DefCodecs.TICKS.optionalFieldOf("mass_revive_interval_ticks", 4)
                    .forGetter(NumberedDef::massReviveIntervalTicks)
    ).apply(i, NumberedDef::new));
}
