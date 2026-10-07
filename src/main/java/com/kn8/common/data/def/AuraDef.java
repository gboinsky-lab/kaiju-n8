// src/main/java/com/kn8/common/data/def/AuraDef.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

/**
 * Aura de poder ({@code data/<ns>/kn8/aura/<id>.json}, 0.5, ideia do Miguel): aparece em volta de quem libera
 * potencia no meio do combate (jogador e soldados especiais), a partir de {@code min_release}% e mais forte quanto
 * maior a %. Cada personagem tem a sua (cor, estilo); o jogador usa a do traje ou a padrao. O cliente desenha
 * sozinho a partir da % publica ({@code kn8:release_visual}) e do id publico da aura ({@code kn8:aura}).
 *
 * <p>{@code color} e a cor principal (pó/brilho); {@code secondary} a dos raios e faiscas (mais clara). {@code size}
 * multiplica o raio em volta do corpo.</p>
 */
public record AuraDef(int color, int secondary, Style style, int minRelease, float size) {

    /** Formas de aura. Cada uma ganha um desenho no cliente ({@code client/vfx/AuraRenderer}). */
    public enum Style implements StringRepresentable {
        /** Faiscas eletricas subindo (aura padrao do traje). */
        SPARKS("sparks"),
        /** Raios em arco em volta do corpo + po e faiscas subindo (referencia do Miguel, aura roxa). */
        LIGHTNING("lightning"),
        /** Labaredas de po subindo do chao. */
        FLAME("flame");

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

        private final String serializedName;

        Style(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** Cor em texto {@code "#RRGGBB"}. */
    public static final Codec<Integer> COLOR = Codec.STRING.comapFlatMap(AuraDef::parseColor,
            color -> String.format("#%06X", color & 0xFFFFFF));

    public static final Codec<AuraDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            COLOR.fieldOf("color").forGetter(AuraDef::color),
            COLOR.optionalFieldOf("secondary", 0xFFFFFF).forGetter(AuraDef::secondary),
            Style.CODEC.optionalFieldOf("style", Style.SPARKS).forGetter(AuraDef::style),
            Codec.intRange(0, 100).optionalFieldOf("min_release", 40).forGetter(AuraDef::minRelease),
            Codec.floatRange(0.25F, 4.0F).optionalFieldOf("size", 1.0F).forGetter(AuraDef::size)
    ).apply(i, AuraDef::new));

    private static DataResult<Integer> parseColor(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) {
            return DataResult.error(() -> "cor precisa ser #RRGGBB, veio " + text);
        }
        try {
            return DataResult.success(Integer.parseInt(hex, 16));
        } catch (NumberFormatException exception) {
            return DataResult.error(() -> "cor precisa ser #RRGGBB, veio " + text);
        }
    }

    /**
     * Forca da aura de 0 a 1 pela % efetiva: 0 abaixo de {@code min_release}, 1 em 100%. Abaixo do minimo nao ha
     * aura (o chamador confere {@code release >= minRelease}).
     */
    public float intensity(int release) {
        if (release < minRelease) {
            return 0.0F;
        }
        if (minRelease >= 100) {
            return 1.0F;
        }
        return Math.max(0.05F, Math.min(1.0F, (release - minRelease) / (float) (100 - minRelease)));
    }
}
