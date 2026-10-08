// src/main/java/com/kn8/core/power/BodyParams.java
package com.kn8.core.power;

/**
 * Parametros dos atributos do corpo (0.5.0). Cada nivel custa {@code xpBase + xpPerLevel * nivel} de XP do proprio
 * atributo; o bonus cresce em linha reta ate {@code maxLevel}.
 *
 * <pre>
 * forca        dano corpo a corpo    x (1 + strengthDamagePerLevel * nivel)
 * velocidade   velocidade de andar   + speedPerLevel * nivel
 * resistencia  dano recebido         x (1 - resistancePerLevel * nivel)
 * agilidade    custo de esquiva/dash x (1 - agilityStaminaPerLevel * nivel)
 * </pre>
 */
public record BodyParams(int maxLevel, int xpBase, int xpPerLevel, double strengthDamagePerLevel,
        double speedPerLevel, double resistancePerLevel, double agilityStaminaPerLevel) {

    public int xpForNext(int level) {
        return xpBase + xpPerLevel * Math.max(0, level);
    }

    /** Converte XP em niveis ate o maximo; no maximo o XP guardado fica limitado ao custo de um nivel. */
    public PowerMath.Training convert(int level, int xp) {
        int current = Math.max(0, level);
        int left = Math.max(0, xp);
        while (current < maxLevel && left >= xpForNext(current)) {
            left -= xpForNext(current);
            current++;
        }
        if (current >= maxLevel) {
            left = Math.min(left, xpForNext(current));
        }
        return new PowerMath.Training(current, left);
    }
}
