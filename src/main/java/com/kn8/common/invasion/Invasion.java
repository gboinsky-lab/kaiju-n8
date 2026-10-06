package com.kn8.common.invasion;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.kn8.common.data.def.InvasionDef;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;

/**
 * Uma invasao em andamento numa dimensao (0.2, Etapa 7). Vive no {@code KN8Server}; nao e salva (ao desligar, a
 * invasao acaba e os kaiju que ja chegaram ficam no mundo) [SUPOSICAO].
 */
public final class Invasion {

    /** Fase atual. A ordem e o indice do payload (InvasionStateS2C). */
    public enum Phase {
        WARNING, FIGHT, BREAK, VICTORY, DEFEAT
    }

    final ResourceLocation id;
    final InvasionDef def;
    final BlockPos center;
    final long startTick;
    final long deadline;
    final ServerBossEvent bar;
    final Set<UUID> alive = new HashSet<>();
    final Set<UUID> participants = new HashSet<>();
    Phase phase = Phase.WARNING;
    /** Indice da onda atual (FIGHT) ou da proxima (WARNING/BREAK). */
    int wave;
    long phaseEndsTick;
    int killed;

    Invasion(ResourceLocation id, InvasionDef def, BlockPos center, long now, ServerBossEvent bar) {
        this.id = id;
        this.def = def;
        this.center = center;
        this.startTick = now;
        this.deadline = now + def.warningTicks() + def.timeLimitTicks();
        this.bar = bar;
        this.phaseEndsTick = now + def.warningTicks();
    }

    public ResourceLocation id() {
        return id;
    }

    public InvasionDef def() {
        return def;
    }

    public BlockPos center() {
        return center;
    }

    public Phase phase() {
        return phase;
    }

    public int wave() {
        return wave;
    }

    public int killed() {
        return killed;
    }

    public Set<UUID> alive() {
        return alive;
    }

    public Set<UUID> participants() {
        return participants;
    }

    /** Fim do aviso/intervalo ou, na luta, o prazo da invasao (para o relogio do cliente). */
    public long timerEnd() {
        return phase == Phase.FIGHT ? deadline : phaseEndsTick;
    }

    boolean ended() {
        return phase == Phase.VICTORY || phase == Phase.DEFEAT;
    }
}
