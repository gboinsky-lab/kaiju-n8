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
    CRITICAL,
    /** 0.5: ataque especial ainda recarregando. */
    DENIED_COOLDOWN,
    /** 0.5: a arma na mao nao tem ataque especial. */
    DENIED_NO_SPECIAL,
    /** 0.5.0-D: pente vazio (a recarga comecou) ou recarregando. */
    RELOADING,
    /** 0.5.0-D2: pente vazio e nenhum pente carregado na mochila. */
    DENIED_NO_MAGAZINE;

    public static CombatResult byIndex(int index) {
        CombatResult[] values = values();
        return index >= 0 && index < values.length ? values[index] : OK;
    }
}
