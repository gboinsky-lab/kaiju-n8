// src/main/java/com/kn8/core/kaiju/KaijuStats.java
package com.kn8.core.kaiju;

import java.util.OptionalDouble;

import com.kn8.core.math.FortitudeCurve;

/**
 * Atributos finais de um kaiju (Java puro). Regras:
 * <ul>
 *   <li>vida e dano saem da curva de fortitude, a nao ser que o JSON fixe um valor ({@code overrides});</li>
 *   <li>os multiplicadores globais e de dificuldade do config valem SEMPRE, inclusive sobre valores fixos (o
 *   servidor em dificuldade dificil deve sentir a diferenca em todo kaiju);</li>
 *   <li>armadura: curva ou valor fixo, sem multiplicador (ja tem teto proprio).</li>
 * </ul>
 */
public record KaijuStats(double health, double damage, double armor) {

    /** Multiplicadores do config: global e da dificuldade atual, para vida e para dano. */
    public record Multipliers(double health, double damage, double difficultyHealth, double difficultyDamage) {
        public static final Multipliers NONE = new Multipliers(1, 1, 1, 1);
    }

    public static KaijuStats of(double fortitude, FortitudeCurve curve, Multipliers multipliers,
            OptionalDouble healthOverride, OptionalDouble damageOverride, OptionalDouble armorOverride) {
        double baseHealth = healthOverride.orElse(curve.health(fortitude));
        double baseDamage = damageOverride.orElse(curve.damage(fortitude));
        double armor = armorOverride.orElse(curve.armor(fortitude));
        return new KaijuStats(baseHealth * multipliers.health() * multipliers.difficultyHealth(),
                baseDamage * multipliers.damage() * multipliers.difficultyDamage(), armor);
    }
}
