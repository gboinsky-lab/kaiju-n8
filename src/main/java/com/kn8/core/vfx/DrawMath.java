package com.kn8.core.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * VFX-R1 (Miguel, 2026-10-22: "nao quero que nenhum tenha particulas"): geometria dos efeitos desenhados, so contas
 * (JUnit). Pontos como {x, y, z}; o cliente transforma em faixas, aneis e discos.
 */
public final class DrawMath {

    private DrawMath() {
    }

    /**
     * Caminho de um raio: de {@code start} na direcao {@code dir} (unitaria) por {@code length}, em {@code segments}
     * trechos, cada no desviado para os lados ate {@code amplitude} (zigue-zague). O primeiro no e o inicio e o ultimo
     * fica no fim da reta (o raio chega onde deve).
     */
    public static List<double[]> boltPath(double[] start, double[] dir, double length, int segments, double amplitude,
            DoubleSupplier random) {
        double[] side = perpendicular(dir);
        double[] up = cross(side, dir);
        List<double[]> nodes = new ArrayList<>();
        nodes.add(start.clone());
        for (int s = 1; s <= segments; s++) {
            double t = s / (double) segments;
            double a = s == segments ? 0.0 : (random.getAsDouble() - 0.5) * 2.0 * amplitude;
            double b = s == segments ? 0.0 : (random.getAsDouble() - 0.5) * 2.0 * amplitude;
            nodes.add(new double[] {start[0] + dir[0] * length * t + side[0] * a + up[0] * b,
                    start[1] + dir[1] * length * t + side[1] * a + up[1] * b,
                    start[2] + dir[2] * length * t + side[2] * a + up[2] * b});
        }
        return nodes;
    }

    /**
     * Ponto de um arco de corte em volta de {@code center}: frente {@code (fx, fz)} na horizontal, plano girado por
     * {@code tilt} graus em volta da frente (0 = horizontal, 90 = vertical), {@code angle} em graus a partir da frente.
     */
    public static double[] arcPoint(double[] center, double fx, double fz, double tilt, double radius, double angle) {
        double flat = Math.sqrt(fx * fx + fz * fz);
        double ux = flat < 1.0E-6 ? 0.0 : fx / flat;
        double uz = flat < 1.0E-6 ? 1.0 : fz / flat;
        double a = Math.toRadians(angle);
        double roll = Math.toRadians(tilt);
        double lateral = Math.sin(a) * Math.cos(roll);
        return new double[] {center[0] + (ux * Math.cos(a) - uz * lateral) * radius,
                center[1] + Math.sin(a) * Math.sin(roll) * radius,
                center[2] + (uz * Math.cos(a) + ux * lateral) * radius};
    }

    /** Fracao do efeito visivel no tempo: aparece em {@code in} da vida e some no ultimo {@code out} (0-1). */
    public static float fade(float life, float in, float out) {
        if (life < 0.0F || life > 1.0F) {
            return 0.0F;
        }
        float show = in <= 0.0F ? 1.0F : Math.min(1.0F, life / in);
        float hide = out <= 0.0F ? 1.0F : Math.min(1.0F, (1.0F - life) / out);
        return Math.min(show, hide);
    }

    static double[] perpendicular(double[] dir) {
        double[] side = cross(dir, new double[] {0, 1, 0});
        double len = Math.sqrt(side[0] * side[0] + side[1] * side[1] + side[2] * side[2]);
        if (len < 1.0E-6) {
            return new double[] {1, 0, 0};
        }
        return new double[] {side[0] / len, side[1] / len, side[2] / len};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
