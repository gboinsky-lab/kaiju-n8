// src/main/java/com/kn8/common/combat/CombatAction.java
package com.kn8.common.combat;

/** Acoes de combate que o cliente pode pedir (so a intencao; o servidor decide). Ordem = indice no pacote. */
public enum CombatAction {
    LIGHT,
    HEAVY,
    BLOCK,
    DODGE,
    /** 0.1-B (GDD secao 7): impulso de 25 de stamina, sem invulnerabilidade. */
    DASH,
    /** 0.1-B: comeca a carregar o ataque (clique direito pressionado com lamina). */
    CHARGE_START,
    /** 0.1-B: solta o ataque carregado (clique direito solto). */
    CHARGE_RELEASE;

    public static CombatAction byIndex(int index) {
        CombatAction[] values = values();
        return index >= 0 && index < values.length ? values[index] : null;
    }
}
