package com.kn8.common.boss;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

/**
 * Estado de chefe de um kaiju (0.2, Etapa 6). Fica no proprio kaiju (nada global): id do chefe nos dados, fase
 * atual, barra de chefe, invulnerabilidade da troca de fase e quem participou da luta (recompensa).
 */
public final class BossState {

    private final ResourceLocation id;
    private int phase;
    private long invulnerableUntil = Long.MIN_VALUE;
    private final Set<UUID> participants = new HashSet<>();
    private ServerBossEvent bar;

    public BossState(ResourceLocation id, int phase) {
        this.id = id;
        this.phase = phase;
    }

    public ResourceLocation id() {
        return id;
    }

    public int phase() {
        return phase;
    }

    void setPhase(int value) {
        phase = value;
    }

    public boolean isInvulnerable(long now) {
        return now < invulnerableUntil;
    }

    void setInvulnerableUntil(long tick) {
        invulnerableUntil = tick;
    }

    public Set<UUID> participants() {
        return participants;
    }

    ServerBossEvent bar(Component name) {
        if (bar == null) {
            bar = new ServerBossEvent(name, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setDarkenScreen(true);
        }
        return bar;
    }

    /** Some com a barra para todos (morte, remocao, descarregar o chunk). */
    public void clearBar() {
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }
}
