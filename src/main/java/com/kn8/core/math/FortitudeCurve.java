// src/main/java/com/kn8/core/math/FortitudeCurve.java
package com.kn8.core.math;

/**
 * Curvas do GDD (secao 10) que transformam a fortitude canonica de um kaiju em atributos (Java puro).
 *
 * <pre>
 * vida      = healthBase * healthExponentBase ^ (f - 2)
 * dano      = damageBase * damageExponentBase ^ (f - 2)
 * armadura  = min(armorPerFortitude * f, armorCap)
 * </pre>
 *
 * <p>Todos os parametros vem do {@code kn8-server.toml} (regra 2: nenhum numero de balanceamento no Java); os
 * valores padrao do GDD ficam so na definicao do config.</p>
 */
public record FortitudeCurve(double healthBase, double healthExponentBase, double damageBase,
        double damageExponentBase, double armorPerFortitude, double armorCap) {

    /** Fortitude de referencia da curva: um kaiju de fortitude 2 tem exatamente a vida e o dano base. */
    private static final double REFERENCE_FORTITUDE = 2.0;

    public FortitudeCurve {
        if (healthBase <= 0 || healthExponentBase <= 1 || damageBase <= 0 || damageExponentBase <= 1
                || armorPerFortitude < 0 || armorCap < 0) {
            throw new IllegalArgumentException("Parametros invalidos da curva de fortitude");
        }
    }

    public double health(double fortitude) {
        return healthBase * Math.pow(healthExponentBase, fortitude - REFERENCE_FORTITUDE);
    }

    public double damage(double fortitude) {
        return damageBase * Math.pow(damageExponentBase, fortitude - REFERENCE_FORTITUDE);
    }

    public double armor(double fortitude) {
        return Math.min(armorPerFortitude * fortitude, armorCap);
    }
}
