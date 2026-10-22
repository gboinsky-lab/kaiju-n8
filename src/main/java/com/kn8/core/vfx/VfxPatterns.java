package com.kn8.core.vfx;

import java.util.Locale;
import java.util.function.DoubleSupplier;

/**
 * KaijuVFX (VFX-1, diretriz do Miguel): onde nasce e para onde vai cada particula de uma camada de efeito, pelo
 * padrao da camada. So contas (JUnit); quem desenha e o cliente. Direcao = frente do golpe na horizontal.
 *
 * <ul>
 *   <li>{@code burst}: espalha para todos os lados a partir do ponto;</li>
 *   <li>{@code ring}: anel horizontal saindo do ponto (onda de choque no chao);</li>
 *   <li>{@code line}: ao longo da direcao, do ponto ate {@code spread} a frente (rastro, feixe);</li>
 *   <li>{@code arc}: meia-lua a frente, de lado a lado (corte);</li>
 *   <li>{@code converge}: nasce numa esfera de raio {@code spread} e vai para o ponto (carga, regeneracao);</li>
 *   <li>{@code rise}: sobe devagar do chao em volta do ponto (vapor, esporos, poeira).</li>
 * </ul>
 */
public final class VfxPatterns {

    public enum Pattern {
        BURST, RING, LINE, ARC, CONVERGE, RISE;

        /** Nome do JSON (minusculas); desconhecido = burst. */
        public static Pattern of(String name) {
            for (Pattern pattern : values()) {
                if (pattern.name().equals(name.toUpperCase(Locale.ROOT))) {
                    return pattern;
                }
            }
            return BURST;
        }

        public static boolean known(String name) {
            for (Pattern pattern : values()) {
                if (pattern.name().equals(name.toUpperCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Posicao (deslocamento do ponto) e velocidade de uma particula: {px, py, pz, vx, vy, vz}. */
    public static double[] spawn(Pattern pattern, int index, int count, double dirX, double dirZ, double spread,
            double speed, DoubleSupplier random) {
        return spawn(pattern, index, count, dirX, dirZ, spread, speed, 0.0, random);
    }

    /**
     * VFX-3: {@code tilt} (graus) gira o plano do arco em volta da frente: 0 = corte horizontal, 90 = vertical,
     * +-40 = as diagonais de um X, passos de 22,5 = a estrela do Yae-uchi. So vale para {@code arc}.
     */
    public static double[] spawn(Pattern pattern, int index, int count, double dirX, double dirZ, double spread,
            double speed, double tilt, DoubleSupplier random) {
        double flat = Math.sqrt(dirX * dirX + dirZ * dirZ);
        double fx = flat < 1.0E-6 ? 0.0 : dirX / flat;
        double fz = flat < 1.0E-6 ? 1.0 : dirZ / flat;
        double t = count <= 1 ? 0.5 : index / (double) (count - 1);
        switch (pattern) {
            case RING -> {
                double angle = 2.0 * Math.PI * index / Math.max(1, count);
                double cx = Math.cos(angle);
                double cz = Math.sin(angle);
                return new double[] {cx * spread * 0.2, 0.1, cz * spread * 0.2, cx * speed, 0.0, cz * speed};
            }
            case LINE -> {
                double along = t * spread;
                return new double[] {fx * along, jitter(random, 0.1), fz * along, fx * speed * 0.2, 0.0,
                        fz * speed * 0.2};
            }
            case ARC -> {
                // De -75 a +75 graus em volta da frente, no raio spread.
                double angle = Math.toRadians(-75.0 + 150.0 * t);
                // Lado = (-fz, 0, fx) girado em volta da frente pelo tilt, em direcao ao alto.
                double roll = Math.toRadians(tilt);
                double lateral = Math.sin(angle) * Math.cos(roll);
                double ax = fx * Math.cos(angle) - fz * lateral;
                double az = fz * Math.cos(angle) + fx * lateral;
                double ay = Math.sin(angle) * Math.sin(roll);
                double py = tilt == 0.0 ? jitter(random, 0.15) : ay * spread;
                return new double[] {ax * spread, py, az * spread, ax * speed * 0.3, ay * speed * 0.3,
                        az * speed * 0.3};
            }
            case CONVERGE -> {
                double[] d = sphere(random);
                return new double[] {d[0] * spread, d[1] * spread, d[2] * spread, -d[0] * speed, -d[1] * speed,
                        -d[2] * speed};
            }
            case RISE -> {
                double angle = random.getAsDouble() * 2.0 * Math.PI;
                double radius = Math.sqrt(random.getAsDouble()) * spread;
                return new double[] {Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius, 0.0, speed, 0.0};
            }
            default -> {
                double[] d = sphere(random);
                return new double[] {d[0] * spread * 0.3, d[1] * spread * 0.3, d[2] * spread * 0.3, d[0] * speed,
                        Math.abs(d[1]) * speed, d[2] * speed};
            }
        }
    }

    private static double jitter(DoubleSupplier random, double amount) {
        return (random.getAsDouble() - 0.5) * 2.0 * amount;
    }

    /** Direcao aleatoria na esfera (rejeicao no cubo). */
    private static double[] sphere(DoubleSupplier random) {
        for (int i = 0; i < 16; i++) {
            double x = random.getAsDouble() * 2.0 - 1.0;
            double y = random.getAsDouble() * 2.0 - 1.0;
            double z = random.getAsDouble() * 2.0 - 1.0;
            double length = Math.sqrt(x * x + y * y + z * z);
            if (length > 1.0E-3 && length <= 1.0) {
                return new double[] {x / length, y / length, z / length};
            }
        }
        return new double[] {0.0, 1.0, 0.0};
    }

    private VfxPatterns() {
    }
}
