// src/main/java/com/kn8/common/combat/CombatState.java
package com.kn8.common.combat;

import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.data.def.WeaponProfileDef;
import com.kn8.core.combat.ActionTimeline;

import net.minecraft.resources.ResourceLocation;

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
    /** 0.5: tick em que o ataque especial volta a ficar pronto (por jogador, vale para qualquer arma). */
    long specialReadyTick = NEVER;
    /** 0.5: recarga total do ultimo ataque especial (para a HUD mostrar a fracao). */
    int specialCooldownTicks;
    /** 0.5.0-D: item da mao principal no ultimo tick (saque ao trocar). */
    ResourceLocation lastHeldItem;
    long reloadStartTick = NEVER;
    WeaponProfileDef.Reload reloadProfile;
    ResourceLocation reloadItem;
    int reloadStage = -1;
    /** Pente esvaziou: recarrega quando o tiro atual terminar. */
    boolean pendingReload;

    public boolean blocking() {
        return blocking;
    }

    public int comboStep() {
        return comboStep;
    }
}
