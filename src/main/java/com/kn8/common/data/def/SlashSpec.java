// src/main/java/com/kn8/common/data/def/SlashSpec.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Corte a distancia (0.6-D, Kuuchi/Kosa-uchi do Hoshina; tambem o especial da espada do Hoshina para o jogador):
 * {@code count} cortes que voam retos ate {@code range} blocos a {@code speed} blocos/tick, atravessando entidades
 * (cada alvo leva uma vez por corte). {@code spread_degrees} abre os cortes para os lados (0 = todos na mesma
 * linha); {@code roll_degrees} inclina cada corte (o segundo corte usa o sinal oposto: o "X" do Kosa-uchi).
 * {@code width} e a meia-largura do corte; {@code color} e a cor do rastro (RGB).
 *
 * <p>0.7-C (tiros da Mina e do Reno, baioneta do Narumi): {@code bullet} desenha um rastro reto de bala no lugar do
 * arco; {@code explosion_radius} (maior que 0) faz o projetil explodir no primeiro alvo, no primeiro bloco ou no fim
 * do alcance, ferindo quem estiver no raio (aliados de fora, sem quebrar blocos). {@code slow_ticks}/
 * {@code slow_level}: Lentidao em quem for atingido (municao congelante do Reno).</p>
 */
public record SlashSpec(int count, float spreadDegrees, float rollDegrees, float speed, float range, float width,
        int color, boolean bullet, float explosionRadius, int slowTicks, int slowLevel) {

    public static final Codec<SlashSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 8).optionalFieldOf("count", 1).forGetter(SlashSpec::count),
            Codec.floatRange(0.0F, 90.0F).optionalFieldOf("spread_degrees", 0.0F).forGetter(SlashSpec::spreadDegrees),
            Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("roll_degrees", 0.0F).forGetter(SlashSpec::rollDegrees),
            Codec.floatRange(0.1F, 6.0F).optionalFieldOf("speed", 1.6F).forGetter(SlashSpec::speed),
            Codec.floatRange(1.0F, 64.0F).optionalFieldOf("range", 10.0F).forGetter(SlashSpec::range),
            Codec.floatRange(0.1F, 8.0F).optionalFieldOf("width", 1.2F).forGetter(SlashSpec::width),
            Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color", 0xB070FF).forGetter(SlashSpec::color),
            Codec.BOOL.optionalFieldOf("bullet", false).forGetter(SlashSpec::bullet),
            Codec.floatRange(0.0F, 16.0F).optionalFieldOf("explosion_radius", 0.0F)
                    .forGetter(SlashSpec::explosionRadius),
            Codec.intRange(0, 600).optionalFieldOf("slow_ticks", 0).forGetter(SlashSpec::slowTicks),
            Codec.intRange(1, 5).optionalFieldOf("slow_level", 1).forGetter(SlashSpec::slowLevel)
    ).apply(i, SlashSpec::new));

    public static final SlashSpec DEFAULT = new SlashSpec(1, 0.0F, 0.0F, 1.6F, 10.0F, 1.2F, 0xB070FF, false, 0.0F, 0, 1);
}
