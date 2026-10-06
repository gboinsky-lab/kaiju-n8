// src/main/java/com/kn8/common/network/PrivateSyncState.java
package com.kn8.common.network;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * Estado por servidor do sync privado: canais marcados como "sujos" por jogador e o tick do ultimo envio de cada
 * canal (para respeitar o intervalo minimo, ex.: stamina no maximo a cada 5 ticks). Vive no {@code KN8Server}.
 */
public final class PrivateSyncState {

    private final Map<UUID, Set<ResourceLocation>> dirty = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Long>> lastSent = new HashMap<>();

    void markDirty(UUID player, ResourceLocation channel) {
        dirty.computeIfAbsent(player, p -> new LinkedHashSet<>()).add(channel);
    }

    Map<UUID, Set<ResourceLocation>> dirty() {
        return dirty;
    }

    long lastSent(UUID player, ResourceLocation channel) {
        return lastSent.getOrDefault(player, Map.of()).getOrDefault(channel, Long.MIN_VALUE);
    }

    void recordSent(UUID player, ResourceLocation channel, long tick) {
        lastSent.computeIfAbsent(player, p -> new HashMap<>()).put(channel, tick);
    }

    public void forget(UUID player) {
        dirty.remove(player);
        lastSent.remove(player);
    }
}
