// src/main/java/com/kn8/common/data/def/VfxProfileDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.core.vfx.VfxColors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Perfil visual de um personagem ou especie ({@code data/<ns>/kn8/vfx_profile/<id>.json}, VFX-1, diretriz KaijuVFX
 * do Miguel). O id e o do kaiju ({@code kaiju/<id>}), o do perfil de soldado especial ({@code special_soldier/<id>})
 * ou {@code soldier}/{@code player}. Paleta tirada da textura do modelo e das decisoes do Miguel
 * ({@code docs/VFX_FICHAS_IDENTIDADE.md}).
 *
 * <ul>
 *   <li>{@code palette}: {@code primary} (corpo), {@code secondary}, {@code accent} (destaque: linhas, nucleo,
 *   lamina) e {@code core} (miolo da energia), em "#RRGGBB";</li>
 *   <li>{@code material}: como o corpo reage no golpe (lascas de quitina, esporos do fungo, vapor do revivido...),
 *   usado pelos efeitos genericos quando nao ha receita;</li>
 *   <li>{@code effects}: receita por habilidade, tecnica ou efeito ({@code bite}, {@code kuuchi}, {@code impact}):
 *   camadas de particulas proprias com forma, cor da paleta, padrao e quantidade. Com receita, ela substitui o
 *   efeito generico daquele id.</li>
 * </ul>
 */
public record VfxProfileDef(Palette palette, String material, Map<String, List<Layer>> effects) {

    public static final String GENERIC = "generic";

    public static final List<String> MATERIALS =
            List.of("flesh", "chitin", "fungus", "revived", "energy", "ice", "suit", "metal", "scales");

    public record Palette(String primary, String secondary, String accent, String core) {
        public static final Codec<Palette> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("primary").forGetter(Palette::primary),
                Codec.STRING.fieldOf("secondary").forGetter(Palette::secondary),
                Codec.STRING.fieldOf("accent").forGetter(Palette::accent),
                Codec.STRING.optionalFieldOf("core", "#FFFFFF").forGetter(Palette::core)
        ).apply(i, Palette::new));

        /** Cor de um nome da paleta ({@code primary}...) ou de um "#RRGGBB" direto; invalido = -1. */
        public int resolve(String color) {
            return switch (color) {
                case "primary" -> VfxColors.parse(primary);
                case "secondary" -> VfxColors.parse(secondary);
                case "accent" -> VfxColors.parse(accent);
                case "core" -> VfxColors.parse(core);
                default -> VfxColors.parse(color);
            };
        }
    }

    /**
     * Uma camada da receita: {@code shape} = particula ({@code arc}, {@code ring}, {@code shard}, {@code streak},
     * {@code glow}, {@code spore}); {@code pattern} = {@code burst}/{@code ring}/{@code line}/{@code arc}/
     * {@code converge}/{@code rise} ({@code VfxPatterns}); {@code count} antes da qualidade; {@code size} em blocos;
     * {@code spread} = raio/comprimento do padrao; {@code forward}/{@code up} = deslocamento da origem;
     * {@code delay} = ticks depois do evento (sequencia: rachar, fumaca, onda). VFX-2: os numeros sao para um corpo
     * da altura de referencia ({@code [fx] referenceHeight}, um humano) e crescem com o corpo de quem faz o efeito.
     * {@code shape} {@code generic} toca o efeito comum do evento junto da receita.
     */
    public record Layer(String shape, String color, String pattern, int count, float size, float speed, float spread,
            int lifetime, float forward, float up, int delay) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("shape").forGetter(Layer::shape),
                Codec.STRING.optionalFieldOf("color", "primary").forGetter(Layer::color),
                Codec.STRING.optionalFieldOf("pattern", "burst").forGetter(Layer::pattern),
                Codec.intRange(0, 400).optionalFieldOf("count", 8).forGetter(Layer::count),
                Codec.floatRange(0.01F, 16.0F).optionalFieldOf("size", 0.3F).forGetter(Layer::size),
                Codec.floatRange(0.0F, 4.0F).optionalFieldOf("speed", 0.2F).forGetter(Layer::speed),
                Codec.floatRange(0.0F, 32.0F).optionalFieldOf("spread", 1.0F).forGetter(Layer::spread),
                Codec.intRange(1, 400).optionalFieldOf("lifetime", 10).forGetter(Layer::lifetime),
                Codec.floatRange(-16.0F, 16.0F).optionalFieldOf("forward", 0.0F).forGetter(Layer::forward),
                Codec.floatRange(-16.0F, 32.0F).optionalFieldOf("up", 0.0F).forGetter(Layer::up),
                Codec.intRange(0, 200).optionalFieldOf("delay", 0).forGetter(Layer::delay)
        ).apply(i, Layer::new));

        /** Camada que toca o efeito comum do evento ({@code size} = multiplicador da intensidade). */
        public boolean generic() {
            return GENERIC.equals(shape);
        }
    }

    public static final Codec<VfxProfileDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Palette.CODEC.fieldOf("palette").forGetter(VfxProfileDef::palette),
            Codec.STRING.optionalFieldOf("material", "flesh").forGetter(VfxProfileDef::material),
            Codec.unboundedMap(Codec.STRING, Layer.CODEC.listOf()).optionalFieldOf("effects", Map.of())
                    .forGetter(VfxProfileDef::effects)
    ).apply(i, VfxProfileDef::new));

    public Optional<List<Layer>> recipe(String key) {
        return Optional.ofNullable(effects.get(key));
    }
}
