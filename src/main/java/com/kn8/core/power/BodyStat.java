// src/main/java/com/kn8/core/power/BodyStat.java
package com.kn8.core.power;

/**
 * Atributos do corpo do jogador (0.5.0, Biblioteca v21 Prioridade 1), treinados pelo uso e independentes do
 * Release: forca (dano corpo a corpo), velocidade (corrida), resistencia (dano recebido) e agilidade (esquiva,
 * dash e parry).
 */
public enum BodyStat {
    STRENGTH("strength"),
    SPEED("speed"),
    RESISTANCE("resistance"),
    AGILITY("agility");

    private final String key;

    BodyStat(String key) {
        this.key = key;
    }

    /** Chave no save, no lang ({@code kn8.body.<chave>}) e no config. */
    public String key() {
        return key;
    }
}
