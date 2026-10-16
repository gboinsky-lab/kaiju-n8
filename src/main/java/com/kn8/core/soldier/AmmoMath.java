// src/main/java/com/kn8/core/soldier/AmmoMath.java
package com.kn8.core.soldier;

/**
 * Municao dos soldados (IA-2, Miguel: "use a segunda opcao"): a reserva e contada em balas, nao em pentes. A recarga
 * completa o pente com o que falta e as balas que sobravam no pente velho nao se perdem (voltam para a reserva). O
 * total de tiros de uma vida e sempre pente cheio + pentes reserva x pente.
 */
public final class AmmoMath {

    /** Resultado de uma recarga: balas no pente e na reserva depois dela. */
    public record Reload(int rounds, int reserve) {
    }

    private AmmoMath() {
    }

    /** Reserva inicial em balas. */
    public static int initialReserve(int spareMagazines, int magazine) {
        return Math.max(0, spareMagazines) * Math.max(0, magazine);
    }

    /** Completa o pente com a reserva (sem passar do tamanho do pente e sem perder o que ainda estava nele). */
    public static Reload topUp(int rounds, int magazine, int reserve) {
        int current = Math.max(0, rounds);
        int taken = Math.min(Math.max(0, magazine - current), Math.max(0, reserve));
        return new Reload(current + taken, Math.max(0, reserve) - taken);
    }

    /** Pentes reserva arredondados para cima (para mostrar e para os testes). */
    public static int magazinesLeft(int reserve, int magazine) {
        return magazine <= 0 ? 0 : (Math.max(0, reserve) + magazine - 1) / magazine;
    }
}
