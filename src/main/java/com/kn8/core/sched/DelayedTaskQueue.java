// src/main/java/com/kn8/core/sched/DelayedTaskQueue.java
package com.kn8.core.sched;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila de tarefas para executar num tick futuro (Java puro). Primeiro uso: o atraso artificial de debug, que
 * segura os pedidos dos clientes por alguns ticks para simular latencia (validado no PT8).
 *
 * <p>Tarefas com o mesmo tick rodam na ordem em que foram agendadas.</p>
 */
public final class DelayedTaskQueue {

    private record Entry(long dueTick, Runnable task) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public void schedule(long dueTick, Runnable task) {
        entries.add(new Entry(dueTick, task));
    }

    /** Executa e remove todas as tarefas com {@code dueTick <= now}. Retorna quantas rodaram. */
    public int runDue(long now) {
        List<Entry> due = new ArrayList<>();
        entries.removeIf(entry -> {
            if (entry.dueTick() <= now) {
                due.add(entry);
                return true;
            }
            return false;
        });
        due.forEach(entry -> entry.task().run());
        return due.size();
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}
