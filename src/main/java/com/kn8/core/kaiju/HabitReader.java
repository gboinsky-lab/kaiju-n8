package com.kn8.core.kaiju;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * PM-3 (Plano Mestre, secao 3: IA que le padroes do jogador): memoria curta dos habitos do alvo de um kaiju
 * inteligente. Sinais simples e limitados a uma janela: distancia (fica longe / fica colado), golpes do kaiju que nao
 * pegam (esquiva) e o mesmo tipo de ataque repetido pelo alvo. Quando um sinal passa do limite, vira um habito com
 * resposta por um tempo e uma espera antes do proximo (nao oscila); o sinal usado e apagado (tem de se repetir para
 * voltar). Troca de alvo ou fim do combate zeram tudo. So contas, sem Minecraft (JUnit).
 */
public final class HabitReader {

    /** Habitos lidos, em ordem de prioridade (o primeiro que passa vale). */
    public enum Habit {
        REPEATS("repeats"), EVADES("evades"), KEEPS_DISTANCE("keeps_distance"), STAYS_CLOSE("stays_close");

        private final String key;

        Habit(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /**
     * Limites: janela em ticks; distancia (entre bordas) longe/perto e a fracao das amostras que precisa estar nela,
     * com um minimo de amostras; esquivas na janela; repeticoes seguidas; tempo da resposta e espera depois dela.
     */
    public record Params(int windowTicks, double farDistance, double closeDistance, double share, int minSamples,
            int evadeCount, int repeatCount, int responseTicks, int cooldownTicks) {
    }

    private record Sample(long tick, double distance) {
    }

    private final Deque<Sample> samples = new ArrayDeque<>();
    private final Deque<Long> misses = new ArrayDeque<>();
    private String lastKind;
    private int kindStreak;
    private long lastKindTick = Long.MIN_VALUE;
    private Habit active;
    private long activeUntil = Long.MIN_VALUE;
    private long cooldownUntil = Long.MIN_VALUE;

    public void reset() {
        samples.clear();
        misses.clear();
        lastKind = null;
        kindStreak = 0;
        lastKindTick = Long.MIN_VALUE;
        active = null;
        activeUntil = Long.MIN_VALUE;
        cooldownUntil = Long.MIN_VALUE;
    }

    /** Distancia entre bordas ate o alvo agora (amostrada a cada poucos ticks por quem chama). */
    public void sampleDistance(long now, double distance, Params params) {
        prune(now, params.windowTicks());
        samples.addLast(new Sample(now, distance));
    }

    /** Um golpe do kaiju contra o alvo terminou: {@code landed} false = o alvo saiu (esquiva). */
    public void attackResult(long now, boolean landed, Params params) {
        prune(now, params.windowTicks());
        if (!landed) {
            misses.addLast(now);
        }
    }

    /** O alvo acertou o kaiju com um ataque do tipo {@code kind} (ex.: "melee", "heavy", "ranged"). */
    public void hitBy(long now, String kind, Params params) {
        if (kind.equals(lastKind) && now - lastKindTick <= params.windowTicks()) {
            kindStreak++;
        } else {
            lastKind = kind;
            kindStreak = 1;
        }
        lastKindTick = now;
    }

    /** Habito com resposta agora, se houver (sem decidir outro). */
    public Optional<Habit> active(long now) {
        return active != null && now < activeUntil ? Optional.of(active) : Optional.empty();
    }

    /**
     * Le os sinais e devolve o habito novo quando um comeca agora (vazio se nada mudou). Enquanto uma resposta
     * dura ou a espera nao passou, nao le nada.
     */
    public Optional<Habit> update(long now, Params params) {
        prune(now, params.windowTicks());
        if (now < activeUntil || now < cooldownUntil) {
            return Optional.empty();
        }
        active = null;
        Habit found = read(params);
        if (found == null) {
            return Optional.empty();
        }
        active = found;
        activeUntil = now + params.responseTicks();
        cooldownUntil = activeUntil + params.cooldownTicks();
        // O sinal usado se apaga: para a resposta voltar o alvo tem de repetir o habito.
        switch (found) {
            case REPEATS -> kindStreak = 0;
            case EVADES -> misses.clear();
            default -> samples.clear();
        }
        return Optional.of(found);
    }

    private Habit read(Params params) {
        if (kindStreak >= params.repeatCount()) {
            return Habit.REPEATS;
        }
        if (misses.size() >= params.evadeCount()) {
            return Habit.EVADES;
        }
        if (samples.size() >= params.minSamples()) {
            long far = samples.stream().filter(s -> s.distance() >= params.farDistance()).count();
            long close = samples.stream().filter(s -> s.distance() <= params.closeDistance()).count();
            if (far >= params.share() * samples.size()) {
                return Habit.KEEPS_DISTANCE;
            }
            if (close >= params.share() * samples.size()) {
                return Habit.STAYS_CLOSE;
            }
        }
        return null;
    }

    private void prune(long now, int window) {
        while (!samples.isEmpty() && now - samples.peekFirst().tick() > window) {
            samples.removeFirst();
        }
        while (!misses.isEmpty() && now - misses.peekFirst() > window) {
            misses.removeFirst();
        }
        if (lastKind != null && now - lastKindTick > window) {
            lastKind = null;
            kindStreak = 0;
        }
    }

    /** Amostras guardadas (teste: a memoria e limitada pela janela). */
    public int sampleCount() {
        return samples.size();
    }
}
