package com.kn8.core.soldier;

/**
 * Fisica do salto tatico dos soldados (MOV-2, plano de mobilidade v3 §4 "saltos"): o salto e um impulso de verdade
 * (velocidade, colisao e gravidade do vanilla), entao as contas seguem a ordem do {@code LivingEntity#travel} de um
 * mob no ar sem andar: move pela velocidade, depois vertical = (vy - 0,08) x 0,98 e horizontal x 0,91. Sem
 * referencia ao Minecraft: testado no JUnit.
 */
public final class LeapMath {

    public static final double GRAVITY = 0.08;
    public static final double VERTICAL_DRAG = 0.98;
    public static final double AIR_DRAG = 0.91;
    /** Impulso do pulo vanilla: o salto nunca sai mais baixo que isto (arco visivel por cima de um vao). */
    public static final double MIN_VERTICAL = 0.42;
    /** Maior impulso horizontal por tick (a corrida vanilla e ~0,28): salto que precisaria mais nao sai. */
    public static final double MAX_HORIZONTAL = 0.7;
    /** Maior tempo no ar conferido. */
    public static final int MAX_TICKS = 60;

    private LeapMath() {
    }

    /** Altura maxima subida saindo com {@code vy}. */
    public static double peak(double vy) {
        double y = 0.0;
        double v = vy;
        while (v > 0.0) {
            y += v;
            v = (v - GRAVITY) * VERTICAL_DRAG;
        }
        return y;
    }

    /** Menor impulso vertical que passa de {@code height} + {@code margin} (e nunca menos que o pulo vanilla). */
    public static double verticalFor(double height, double margin) {
        double target = Math.max(0.0, height) + margin;
        double v = MIN_VERTICAL;
        while (peak(v) < target && v < 3.0) {
            v += 0.01;
        }
        return v;
    }

    /**
     * Ticks ate a caixa, saindo agora com velocidade vertical {@code vy}, estar descendo e chegar a {@code dy} blocos
     * da altura atual (negativo = abaixo). -1 se nunca chega (alto demais ou mais tempo que {@link #MAX_TICKS}).
     */
    public static int ticksToReach(double vy, double dy) {
        double y = 0.0;
        double v = vy;
        for (int t = 1; t <= MAX_TICKS; t++) {
            double before = y;
            y += v;
            boolean descending = v <= 0.0;
            v = (v - GRAVITY) * VERTICAL_DRAG;
            // So conta cruzando a altura de cima para baixo (pouso alto demais nunca cruza).
            if (descending && before > dy && y <= dy) {
                return t;
            }
        }
        return -1;
    }

    /** Distancia horizontal percorrida em {@code ticks} saindo com {@code speed} (freio do ar). */
    public static double covered(double speed, int ticks) {
        return speed * (1.0 - Math.pow(AIR_DRAG, ticks)) / (1.0 - AIR_DRAG);
    }

    /** Impulso horizontal que percorre {@code distance} em {@code ticks}; infinito se {@code ticks} <= 0. */
    public static double horizontalFor(double distance, int ticks) {
        if (ticks <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0.0, distance) * (1.0 - AIR_DRAG) / (1.0 - Math.pow(AIR_DRAG, ticks));
    }

    /**
     * Salto que da para fazer: subir {@code clearance} blocos acima da saida (0 em vao), pousar {@code dy} blocos
     * acima/abaixo dela a {@code distance} blocos. A parte horizontal sai so depois de passar da altura (salto em
     * duas fases, como o vault do jogador: nao bate na quina), entao o tempo que conta e o da descida ate o pouso.
     */
    public static boolean feasible(double clearance, double dy, double distance, double margin) {
        double vy = verticalFor(Math.max(clearance, dy), margin);
        double above = Math.max(0.0, clearance);
        // Velocidade e tempo restante no momento em que passa da altura de folga.
        double y = 0.0;
        double v = vy;
        int t = 0;
        while (above > 0.0 && y < above && t < MAX_TICKS) {
            y += v;
            v = (v - GRAVITY) * VERTICAL_DRAG;
            t++;
        }
        int rest = ticksToReach(v, dy - y);
        return rest > 0 && horizontalFor(distance, rest) <= MAX_HORIZONTAL;
    }
}
