// src/main/java/com/kn8/common/combat/CombatResult.java
package com.kn8.common.combat;

/** Resposta do servidor a um pedido de combate (Fase 4, secao 5.1: OK ou NEGADO + motivo) e eventos de defesa. */
public enum CombatResult {
    OK,
    DENIED_NO_WEAPON,
    DENIED_BUSY,
    DENIED_NO_STAMINA,
    SLOWED_NO_STAMINA,
    PARRY,
    GUARD_BROKEN,
    CRITICAL;

    public static CombatResult byIndex(int index) {
        CombatResult[] values = values();
        return index >= 0 && index < values.length ? values[index] : OK;
    }
}
