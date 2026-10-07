// src/main/java/com/kn8/common/data/def/FlyerDef.java
package com.kn8.common.data.def;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Kaiju voador ({@code data/<ns>/kn8/flyer/<id do kaiju>.json}, 0.6-E; primeiro: Preondactyl, especificacao do
 * Miguel secao 21.6). Os golpes vem do {@code kaiju/<id>.json} como em qualquer kaiju; aqui fica o voo:
 *
 * <ul>
 *   <li>{@code fly_speed}: velocidade de voo; {@code cruise_height}/{@code circle_radius}: com alvo, circula acima
 *   dele a essa altura e raio (atirando o raio de energia de cima);</li>
 *   <li>{@code dive_speed}/{@code dive_cooldown_ticks}: de tempos em tempos mergulha no alvo (golpes de perto) e
 *   volta a subir;</li>
 *   <li>{@code front_damage_multiplier}: dano recebido pela frente (couraca, padrao do texto: 0,35) e
 *   {@code back_damage_multiplier} pelas costas (ponto fraco); {@code front_arc_degrees}: largura da frente;</li>
 *   <li>{@code land_after_idle_ticks}: sem alvo por esse tempo, pousa;</li>
 *   <li>{@code self_destruct}: com a vida abaixo de {@code health_below}, aviso de {@code warning_ticks} e
 *   explosao de raio {@code radius} ({@code damage_multiplier} x dano dele; quebra blocos ate
 *   {@code destruction_radius}, se a destruicao estiver ligada).</li>
 * </ul>
 */
public record FlyerDef(float flySpeed, float cruiseHeight, float circleRadius, float diveSpeed, int diveCooldownTicks,
        float frontDamageMultiplier, float backDamageMultiplier, float frontArcDegrees, int landAfterIdleTicks,
        Optional<SelfDestruct> selfDestruct) {

    public record SelfDestruct(float healthBelow, int warningTicks, float radius, float damageMultiplier,
            float destructionRadius) {
        public static final Codec<SelfDestruct> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("health_below").forGetter(SelfDestruct::healthBelow),
                Codec.intRange(1, 400).fieldOf("warning_ticks").forGetter(SelfDestruct::warningTicks),
                Codec.floatRange(0.5F, 32.0F).fieldOf("radius").forGetter(SelfDestruct::radius),
                DefCodecs.MULTIPLIER.fieldOf("damage_multiplier").forGetter(SelfDestruct::damageMultiplier),
                Codec.floatRange(0.0F, 32.0F).optionalFieldOf("destruction_radius", 0.0F)
                        .forGetter(SelfDestruct::destructionRadius)
        ).apply(i, SelfDestruct::new));
    }

    public static final Codec<FlyerDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.05F, 3.0F).fieldOf("fly_speed").forGetter(FlyerDef::flySpeed),
            Codec.floatRange(2.0F, 64.0F).optionalFieldOf("cruise_height", 9.0F).forGetter(FlyerDef::cruiseHeight),
            Codec.floatRange(2.0F, 64.0F).optionalFieldOf("circle_radius", 12.0F).forGetter(FlyerDef::circleRadius),
            Codec.floatRange(0.1F, 6.0F).optionalFieldOf("dive_speed", 2.0F).forGetter(FlyerDef::diveSpeed),
            DefCodecs.TICKS.optionalFieldOf("dive_cooldown_ticks", 160).forGetter(FlyerDef::diveCooldownTicks),
            Codec.floatRange(0.0F, 10.0F).optionalFieldOf("front_damage_multiplier", 1.0F)
                    .forGetter(FlyerDef::frontDamageMultiplier),
            Codec.floatRange(0.0F, 10.0F).optionalFieldOf("back_damage_multiplier", 1.0F)
                    .forGetter(FlyerDef::backDamageMultiplier),
            Codec.floatRange(0.0F, 360.0F).optionalFieldOf("front_arc_degrees", 120.0F)
                    .forGetter(FlyerDef::frontArcDegrees),
            DefCodecs.TICKS.optionalFieldOf("land_after_idle_ticks", 200).forGetter(FlyerDef::landAfterIdleTicks),
            SelfDestruct.CODEC.optionalFieldOf("self_destruct").forGetter(FlyerDef::selfDestruct)
    ).apply(i, FlyerDef::new));
}
