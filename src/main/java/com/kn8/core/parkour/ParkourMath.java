// src/main/java/com/kn8/core/parkour/ParkourMath.java
package com.kn8.core.parkour;

/**
 * Contas puras do parkour do jogador (0.5.0-E, Biblioteca v22 Prioridade 4 "Parkour e mobilidade"), sem classes do
 * Minecraft: servem ao servidor (que decide o movimento) e ao cliente (que aplica os estados continuos), e sao
 * testadas por JUnit. Todos os numeros vem do config {@code [parkour]}.
 */
public final class ParkourMath {

    /** Obstaculo a frente: nada, baixo o bastante para pular por cima (vault) ou alto para a escalada curta. */
    public enum Obstacle {
        NONE, VAULT, CLIMB
    }

    private ParkourMath() {
    }

    /**
     * Classifica a altura do obstaculo a frente (blocos acima dos pes): ate {@code stepHeight} o vanilla ja sobe
     * sozinho; ate {@code vaultMax} e vault; ate {@code climbMax} e escalada curta; acima disso, nada.
     */
    public static Obstacle classify(double height, double stepHeight, double vaultMax, double climbMax) {
        if (height <= stepHeight) {
            return Obstacle.NONE;
        }
        if (height <= vaultMax) {
            return Obstacle.VAULT;
        }
        return height <= climbMax ? Obstacle.CLIMB : Obstacle.NONE;
    }

    /**
     * Velocidade vertical para subir {@code height} blocos com a gravidade do jogador (0,08 por tick, freio 0,98):
     * o menor impulso que passa da altura, mais {@code margin}.
     */
    public static double launchSpeedFor(double height, double margin) {
        double target = height + margin;
        double speed = 0.0;
        while (peakHeight(speed) < target && speed < 3.0) {
            speed += 0.01;
        }
        return speed;
    }

    /** Altura maxima alcancada saindo do chao com velocidade vertical {@code speed} (fisica do jogador). */
    public static double peakHeight(double speed) {
        double y = 0.0;
        double velocity = speed;
        while (velocity > 0.0) {
            y += velocity;
            velocity = (velocity - 0.08) * 0.98;
        }
        return y;
    }

    /**
     * Wall-run: velocidade vertical no tick {@code tick} de {@code duration}. Comeca subindo {@code lift} e cai em
     * linha ate {@code -lift} no fim (arco suave ao longo da parede, sem flutuar parado).
     */
    public static double wallRunVertical(int tick, int duration, double lift) {
        if (duration <= 0) {
            return 0.0;
        }
        double t = Math.min(1.0, Math.max(0.0, tick / (double) duration));
        return lift * (1.0 - 2.0 * t);
    }

    /** Wall-slide: a queda nunca passa de {@code maxFall} blocos por tick. */
    public static double wallSlideVertical(double current, double maxFall) {
        return Math.max(current, -maxFall);
    }

    /** Slide: velocidade no tick {@code tick} saindo de {@code start}, com atrito {@code friction} por tick. */
    public static double slideSpeed(double start, int tick, double friction) {
        return start * Math.pow(friction, Math.max(0, tick));
    }

    /**
     * Rolamento: multiplicador do dano da queda. Abaixo de {@code safeFall} blocos nao ha dano nenhum; acima, o dano
     * cai {@code reduction} (0,6 = 60% menos).
     */
    public static float rollDamageMultiplier(float fallDistance, float safeFall, float reduction) {
        if (fallDistance <= safeFall) {
            return 0.0F;
        }
        return Math.max(0.0F, 1.0F - reduction);
    }

    /**
     * Forca do parkour pelo nivel do jogador (Miguel, 2026-10-15: "quanto mais forte o jogador, mais forte, rapido e
     * mais alto ele pula"): 1 + {@code perAgility} x agilidade/maximo + {@code perRelease} x Release/100. Multiplica
     * velocidades e alturas alcancaveis; o custo ja cai pela agilidade e a stamina maxima sobe com o Release.
     */
    public static double levelFactor(int agilityLevel, int maxLevel, int release, double perAgility,
            double perRelease) {
        double agility = maxLevel <= 0 ? 0.0 : Math.max(0, Math.min(agilityLevel, maxLevel)) / (double) maxLevel;
        double power = Math.max(0, Math.min(release, 100)) / 100.0;
        return 1.0 + perAgility * agility + perRelease * power;
    }

    /** Custo de stamina do movimento com o fator de agilidade do corpo (treinado gasta menos). */
    public static double cost(double base, double agilityFactor) {
        return Math.max(0.0, base * agilityFactor);
    }

    /**
     * FIX-3: borda agarrada sem a tecla: a altura da borda acima dos pes esta entre {@code min} e {@code max}, o
     * jogador anda para a frente e nao esta subindo rapido (no alto do pulo ou caindo; subindo, o pulo ainda passa
     * por cima sozinho).
     */
    public static boolean autoLedge(double ledgeHeight, double verticalSpeed, boolean forward, double min,
            double max, double maxRise) {
        return forward && verticalSpeed <= maxRise && ledgeHeight >= min && ledgeHeight <= max;
    }

    /**
     * FIX-3: velocidade vertical na escalada da parede: sobe com a tecla para a frente, fica parado sem ela e desce
     * devagar com a tecla para tras.
     */
    public static double climbVertical(float forwardInput, double speed) {
        if (forwardInput > 0.0F) {
            return speed;
        }
        return forwardInput < 0.0F ? -speed : 0.0;
    }
}
