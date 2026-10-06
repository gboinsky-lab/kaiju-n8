// src/main/java/com/kn8/common/server/KN8Server.java
package com.kn8.common.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.kn8.KN8Constants;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.network.NetworkSync;
import com.kn8.common.network.PrivateSyncState;
import com.kn8.core.net.RateLimiter;
import com.kn8.core.sched.DelayedTaskQueue;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * Dono dos servicos do mod para UM servidor em execucao (dedicado ou integrado do singleplayer).
 *
 * <p>Por que existe: a Fase 4 proibe estado estatico global. Todo estado de jogo mora nesta instancia, criada em
 * {@code ServerStartingEvent} e descartada em {@code ServerStoppedEvent}. Assim, sair de um mundo e abrir outro no
 * singleplayer nunca herda estado do anterior. A referencia estatica abaixo aponta so para o servidor atual e e
 * validada em {@link #get(MinecraftServer)}.</p>
 *
 * <p>M3: rate limiter dos pacotes C2S, estado do sync privado, fila de tarefas atrasadas e atraso de debug.
 * M8: contagem de kaiju por dimensao (limite do spawn natural).</p>
 */
public final class KN8Server {

    // Referencia ao servidor em execucao; nula quando nenhum servidor esta rodando.
    private static volatile KN8Server current;

    private final MinecraftServer server;
    private final RateLimiter<UUID> rateLimiter = new RateLimiter<>();
    private final PrivateSyncState privateSync = new PrivateSyncState();
    private final DelayedTaskQueue delayedTasks = new DelayedTaskQueue();
    private int debugInboundDelayTicks;
    private final Map<ResourceKey<Level>, Integer> kaijuCounts = new HashMap<>();
    // 0.1-B (destruicao): fila de pedidos por dimensao e dimensoes em restauracao. Nao salvo: pedidos em andamento
    // se perdem ao desligar (o que ja quebrou fica no DestructionLog, que e salvo).
    private final Map<ResourceKey<Level>, Deque<DestructionService.Job>> destructionJobs = new HashMap<>();
    private final Set<ResourceKey<Level>> restoring = new HashSet<>();
    // 0.2 (Etapa 7): invasao ativa por dimensao (nao salva).
    private final Map<ResourceKey<Level>, Invasion> invasions = new HashMap<>();

    private KN8Server(MinecraftServer server) {
        this.server = server;
    }

    static void start(MinecraftServer server) {
        KN8Server previous = current;
        if (previous != null) {
            // Nao deveria acontecer: o servidor anterior nao disparou ServerStoppedEvent.
            KN8Constants.LOGGER.warn("[kn8] KN8Server anterior nao foi encerrado; substituindo.");
        }
        current = new KN8Server(server);
        KN8Constants.LOGGER.info("[kn8] KN8Server iniciado (dedicado: {}).", server.isDedicatedServer());
    }

    static void stop(MinecraftServer server) {
        KN8Server instance = current;
        if (instance != null && instance.server == server) {
            current = null;
            KN8Constants.LOGGER.info("[kn8] KN8Server encerrado.");
        }
    }

    /**
     * Retorna os servicos do servidor informado.
     *
     * @throws IllegalStateException se o servidor informado nao for o servidor em execucao
     */
    public static KN8Server get(MinecraftServer server) {
        KN8Server instance = current;
        if (instance == null || instance.server != server) {
            throw new IllegalStateException("[kn8] KN8Server nao esta ativo para este servidor.");
        }
        return instance;
    }

    /** Versao tolerante de {@link #get}: null fora de um servidor ativo (ex.: durante o arranque). */
    public static @Nullable KN8Server getOrNull(@Nullable MinecraftServer server) {
        KN8Server instance = current;
        return instance != null && instance.server == server ? instance : null;
    }

    /** Chamado no fim de cada tick do servidor. */
    void endTick() {
        delayedTasks.runDue(server.getTickCount());
        NetworkSync.flush(this);
    }

    public MinecraftServer server() {
        return server;
    }

    public RateLimiter<UUID> rateLimiter() {
        return rateLimiter;
    }

    public PrivateSyncState privateSync() {
        return privateSync;
    }

    /** Agenda uma tarefa para daqui a {@code ticks} ticks do servidor. */
    public void schedule(int ticks, Runnable task) {
        delayedTasks.schedule(server.getTickCount() + (long) ticks, task);
    }

    /** Kaiju carregados agora na dimensao (entra/sai do nivel); usado pelo limite {@code spawn.maxKaijuPerLevel}. */
    public int kaijuCount(ResourceKey<Level> dimension) {
        return kaijuCounts.getOrDefault(dimension, 0);
    }

    public void adjustKaijuCount(ResourceKey<Level> dimension, int delta) {
        kaijuCounts.merge(dimension, delta, (current, change) -> Math.max(0, current + change));
    }

    public Deque<DestructionService.Job> destructionJobs(ResourceKey<Level> dimension) {
        return destructionJobs.computeIfAbsent(dimension, key -> new ArrayDeque<>());
    }

    public Map<ResourceKey<Level>, Invasion> invasions() {
        return invasions;
    }

    public boolean isRestoring(ResourceKey<Level> dimension) {
        return restoring.contains(dimension);
    }

    public void setRestoring(ResourceKey<Level> dimension, boolean value) {
        if (value) {
            restoring.add(dimension);
        } else {
            restoring.remove(dimension);
        }
    }

    /** Debug: atraso artificial aplicado aos pacotes C2S (simula latencia). 0 = desligado. Nunca salvo. */
    public int debugInboundDelayTicks() {
        return debugInboundDelayTicks;
    }

    public void setDebugInboundDelayTicks(int ticks) {
        this.debugInboundDelayTicks = Math.max(0, ticks);
    }
}
