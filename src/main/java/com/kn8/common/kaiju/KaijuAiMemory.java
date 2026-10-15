// src/main/java/com/kn8/common/kaiju/KaijuAiMemory.java
package com.kn8.common.kaiju;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.world.phys.Vec3;

/**
 * Memoria da IA de um kaiju (IA-1A), por entidade e so no servidor (nao salva: some ao recarregar o mundo, como o
 * alvo vanilla): quem o feriu e quanto (ameaca), alvos marcados como inalcancaveis, ultima posicao conhecida do
 * alvo (para a busca) e ate quando vale o alvo dado por um numerado.
 */
final class KaijuAiMemory {

    private record Threat(float damage, long lastTick) {
    }

    private final Map<UUID, Threat> threats = new HashMap<>();
    private final Map<UUID, Long> unreachableUntil = new HashMap<>();
    private Vec3 lastKnown;
    private long lastKnownTick = Long.MIN_VALUE;
    private long commandedUntil = Long.MIN_VALUE;

    /** Registra um golpe (dano 0 = alerta do bando: sabe quem foi, sem somar ameaca). */
    void hurtBy(UUID attacker, float damage, long now, int memoryTicks) {
        Threat old = threats.get(attacker);
        float kept = old != null && now - old.lastTick() <= memoryTicks ? old.damage() : 0.0F;
        threats.put(attacker, new Threat(kept + damage, now));
    }

    float recentDamage(UUID id, long now, int memoryTicks) {
        Threat threat = threats.get(id);
        return threat != null && now - threat.lastTick() <= memoryTicks ? threat.damage() : 0.0F;
    }

    boolean recentAttacker(UUID id, long now, int memoryTicks) {
        Threat threat = threats.get(id);
        return threat != null && now - threat.lastTick() <= memoryTicks;
    }

    void markUnreachable(UUID id, long until) {
        unreachableUntil.put(id, until);
    }

    boolean unreachable(UUID id, long now) {
        Long until = unreachableUntil.get(id);
        return until != null && now < until;
    }

    void see(Vec3 position, long now) {
        lastKnown = position;
        lastKnownTick = now;
    }

    Optional<Vec3> lastKnown(long now, int searchTicks) {
        return lastKnown != null && now - lastKnownTick <= searchTicks ? Optional.of(lastKnown) : Optional.empty();
    }

    void forgetLastKnown() {
        lastKnown = null;
    }

    void command(long until) {
        commandedUntil = until;
    }

    boolean commanded(long now) {
        return now < commandedUntil;
    }

    /** Limpeza periodica: entradas vencidas nao ficam crescendo em batalhas longas. */
    void prune(long now, int memoryTicks) {
        threats.values().removeIf(threat -> now - threat.lastTick() > memoryTicks);
        unreachableUntil.values().removeIf(until -> now >= until);
    }
}
