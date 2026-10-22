package com.kn8.core.kaiju;

import java.util.ArrayList;
import java.util.List;

/**
 * MOV-6 (Preondactyl e voadores): pontos de desvio quando a linha reta ate o destino bate em obstaculo. Em ordem de
 * preferencia: reto, subindo (por cima do predio/morro), subindo pelos lados e so pelos lados (contornar); quem chama
 * fica com o primeiro cujo caminho esta livre. Contas so de vetor (JUnit).
 */
public final class FlightSteer {

    /** Angulos (graus) dos desvios laterais, a partir da direcao do destino. */
    private static final double[] SIDE_DEGREES = {35.0, 70.0};

    private FlightSteer() {
    }

    /**
     * Deslocamentos candidatos {x, y, z} a partir da posicao atual, com {@code ahead} de alcance na horizontal. A
     * direcao do destino e ({@code dx}, {@code dz}) na horizontal e {@code dy} na vertical (limitado ao alcance).
     * {@code climb} e quanto sobe nos desvios por cima.
     */
    public static List<double[]> candidates(double dx, double dy, double dz, double ahead, double climb) {
        List<double[]> out = new ArrayList<>();
        double flat = Math.sqrt(dx * dx + dz * dz);
        double ux = flat < 1.0E-6 ? 0.0 : dx / flat;
        double uz = flat < 1.0E-6 ? 1.0 : dz / flat;
        double step = Math.min(ahead, Math.max(flat, 1.0));
        double rise = Math.max(-step, Math.min(step, dy * step / Math.max(flat, step)));
        out.add(new double[] {ux * step, rise, uz * step});
        out.add(new double[] {ux * step * 0.5, climb, uz * step * 0.5});
        for (double degrees : SIDE_DEGREES) {
            for (int sign = -1; sign <= 1; sign += 2) {
                double a = Math.toRadians(degrees * sign);
                double sx = ux * Math.cos(a) - uz * Math.sin(a);
                double sz = uz * Math.cos(a) + ux * Math.sin(a);
                out.add(new double[] {sx * ahead, climb, sz * ahead});
            }
        }
        for (double degrees : SIDE_DEGREES) {
            for (int sign = -1; sign <= 1; sign += 2) {
                double a = Math.toRadians(degrees * sign);
                double sx = ux * Math.cos(a) - uz * Math.sin(a);
                double sz = uz * Math.cos(a) + ux * Math.sin(a);
                out.add(new double[] {sx * ahead, 0.0, sz * ahead});
            }
        }
        out.add(new double[] {0.0, climb, 0.0});
        return out;
    }
}
