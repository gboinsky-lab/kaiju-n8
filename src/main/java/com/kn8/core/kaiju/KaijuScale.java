// src/main/java/com/kn8/core/kaiju/KaijuScale.java
package com.kn8.core.kaiju;

import java.util.Locale;
import java.util.Optional;

/**
 * Escala oficial dos kaiju (Etapa C, aprovada): 1 bloco ~ 1 metro. Java puro, testavel com JUnit.
 *
 * <pre>
 * YOJU      4-8 blocos   (4-5 pequeno, 5-6,5 medio, 6,5-8 grande)
 * HONJU     6-15 blocos  (6-8 pequeno, 8-11 medio, 11-15 grande)
 * DAIKAIJU  20-30 blocos (20-23 pequeno, 23-27 medio, 27-30 grande)
 * </pre>
 * O "tamanho" de um kaiju e a maior medida da hitbox (largura ou altura). A validacao aceita uma folga
 * ({@link #TOLERANCE}) porque a hitbox cobre o corpo e o modelo pode ir alem (patas abertas, cauda).
 */
public final class KaijuScale {

    /** Folga da validacao: 15% abaixo do minimo / acima do maximo. */
    public static final double TOLERANCE = 0.15;

    /** Faixa de uma categoria, com os limites das subfaixas pequeno/medio/grande. */
    public record Band(double min, double smallMax, double mediumMax, double max) {
    }

    public enum SizeClass {
        SMALL,
        MEDIUM,
        LARGE
    }

    private KaijuScale() {
    }

    /** Faixa da categoria do JSON ({@code yoju}, {@code honju}, {@code daikaiju}); outras categorias nao tem. */
    public static Optional<Band> band(String kaijuClass) {
        return switch (kaijuClass.toLowerCase(Locale.ROOT)) {
            case "yoju" -> Optional.of(new Band(4, 5, 6.5, 8));
            case "honju" -> Optional.of(new Band(6, 8, 11, 15));
            case "daikaiju" -> Optional.of(new Band(20, 23, 27, 30));
            default -> Optional.empty();
        };
    }

    public static double size(double width, double height) {
        return Math.max(width, height);
    }

    /** O tamanho esta dentro da faixa da categoria (com a folga)? Categoria sem faixa = sempre aceito. */
    public static boolean fits(String kaijuClass, double width, double height) {
        return band(kaijuClass).map(band -> {
            double size = size(width, height);
            return size >= band.min() * (1 - TOLERANCE) && size <= band.max() * (1 + TOLERANCE);
        }).orElse(true);
    }

    /** Subfaixa (pequeno/medio/grande) dentro da categoria; vazio se a categoria nao tem faixa. */
    public static Optional<SizeClass> sizeClass(String kaijuClass, double width, double height) {
        return band(kaijuClass).map(band -> {
            double size = size(width, height);
            if (size <= band.smallMax()) {
                return SizeClass.SMALL;
            }
            return size <= band.mediumMax() ? SizeClass.MEDIUM : SizeClass.LARGE;
        });
    }
}
