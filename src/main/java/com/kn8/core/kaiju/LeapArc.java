package com.kn8.core.kaiju;

/**
 * MOV-6: o arco do salto do kaiju ({@code kn8:leap}) com a fisica vanilla do ar (anda {@code v}, depois a vertical
 * perde a gravidade e as duas freiam), para conferir antes de saltar se o caminho e o pouso estao livres. Mesmas
 * contas do {@link AbilitySelection#leapHorizontalSpeed}/{@link AbilitySelection#leapVerticalSpeed}.
 */
public final class LeapArc {

    private LeapArc() {
    }

    /**
     * Posicoes (deslocamento do ponto de partida) ao fim de cada tick do voo: {@code ticks} linhas de {x, y, z}.
     * {@code vx}/{@code vz} perdem {@code airDrag} por tick; {@code vy} perde a gravidade e depois o freio vertical.
     */
    public static double[][] points(double vx, double vy, double vz, int ticks, double gravity, double verticalDrag,
            double airDrag) {
        double[][] out = new double[Math.max(0, ticks)][];
        double x = 0;
        double y = 0;
        double z = 0;
        for (int i = 0; i < out.length; i++) {
            x += vx;
            y += vy;
            z += vz;
            out[i] = new double[] {x, y, z};
            vx *= airDrag;
            vz *= airDrag;
            vy = (vy - gravity) * verticalDrag;
        }
        return out;
    }

    /** Altura maxima (acima da partida) entre os pontos do arco. */
    public static double apex(double[][] points) {
        double top = 0;
        for (double[] p : points) {
            top = Math.max(top, p[1]);
        }
        return top;
    }
}
