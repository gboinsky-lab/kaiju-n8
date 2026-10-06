// src/main/java/com/kn8/common/combat/CombatState.java
package com.kn8.common.combat;

import com.kn8.common.data.def.WeaponDef;
import com.kn8.core.combat.ActionTimeline;

/**
 * Estado de combate de UM jogador no servidor (attachment {@code kn8:combat}, nunca salvo: some no relog e na
 * morte, como deve). A logica fica no {@link CombatService}.
 */
public final class CombatState {

    static final long NEVER = Long.MIN_VALUE;

    final ActionTimeline timeline = new ActionTimeline();
    CombatAction currentAction;
    WeaponDef currentWeapon;
    float currentMultiplier = 1.0F;
    int comboStep = -1;
    long lastLightEndTick = NEVER;
    boolean blocking;
    long blockStartTick = NEVER;
    long invulnerableUntilTick = NEVER;
    long criticalUntilTick = NEVER;
    /** Inicio do ataque carregado em andamento (NEVER = nao esta carregando). */
    long chargeStartTick = NEVER;

    public boolean blocking() {
        return blocking;
    }

    public int comboStep() {
        return comboStep;
    }
}
