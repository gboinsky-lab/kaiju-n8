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
 */
public record SlashSpec(int count, float spreadDegrees, float rollDegrees, float speed, float range, float width,
        int color) {

    public static final Codec<SlashSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 8).optionalFieldOf("count", 1).forGetter(SlashSpec::count),
            Codec.floatRange(0.0F, 90.0F).optionalFieldOf("spread_degrees", 0.0F).forGetter(SlashSpec::spreadDegrees),
            Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("roll_degrees", 0.0F).forGetter(SlashSpec::rollDegrees),
            Codec.floatRange(0.1F, 6.0F).optionalFieldOf("speed", 1.6F).forGetter(SlashSpec::speed),
            Codec.floatRange(1.0F, 64.0F).optionalFieldOf("range", 10.0F).forGetter(SlashSpec::range),
            Codec.floatRange(0.1F, 8.0F).optionalFieldOf("width", 1.2F).forGetter(SlashSpec::width),
            Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color", 0xB070FF).forGetter(SlashSpec::color)
    ).apply(i, SlashSpec::new));

    public static final SlashSpec DEFAULT = new SlashSpec(1, 0.0F, 0.0F, 1.6F, 10.0F, 1.2F, 0xB070FF);
}
