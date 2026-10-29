// src/main/java/com/kn8/common/combat/CombatState.java
package com.kn8.common.combat;

import java.util.HashMap;
import java.util.Map;

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
    /** PM-1: inicio da esquiva atual e se ela ja deu a esquiva perfeita (uma por esquiva). */
    long dodgeStartTick = NEVER;
    boolean perfectDodgeUsed;
    long criticalUntilTick = NEVER;
    /** Inicio do ataque carregado em andamento (NEVER = nao esta carregando). */
    long chargeStartTick = NEVER;
    /** COMBATE-1: tick em que cada tecnica (id) volta a ficar pronta; recarga propria por tecnica. */
    final Map<String, Long> techniqueReadyTick = new HashMap<>();
    /** COMBATE-1: recarga total de cada tecnica usada (para a HUD mostrar a fracao). */
    final Map<String, Integer> techniqueCooldownTicks = new HashMap<>();
    /** COMBATE-1: tecnica escolhida por arma (item); sem entrada = a primeira (o {@code special}). */
    final Map<ResourceLocation, Integer> selectedTechnique = new HashMap<>();
    /** COMBATE-1: tecnica em andamento (o dano sai dela no tick de impacto). */
    WeaponDef.Special currentTechnique;
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
