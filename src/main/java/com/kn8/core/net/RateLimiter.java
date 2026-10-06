// src/main/java/com/kn8/core/net/RateLimiter.java
package com.kn8.core.net;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate limit por dono (jogador) e por canal (tipo de payload), com contadores de aceitos e descartados
 * (Java puro). Um balde por par dono/canal, criado no primeiro pedido.
 *
 * @param <K> tipo da chave do dono (UUID do jogador no jogo; qualquer coisa nos testes)
 */
public final class RateLimiter<K> {

    private static final double TICKS_PER_SECOND = 20.0;

    /** Contadores de um canal de um dono. */
    public static final class Counters {
        private long accepted;
        private long dropped;

        public long accepted() {
            return accepted;
        }

        public long dropped() {
            return dropped;
        }
    }

    private final Map<K, Map<String, TokenBucket>> buckets = new HashMap<>();
    private final Map<K, Map<String, Counters>> counters = new HashMap<>();

    /**
     * @param perSecond pedidos por segundo sustentados
     * @param burst     rajada maxima (capacidade do balde)
     * @return true se o pedido deve ser processado
     */
    public boolean tryAcquire(K owner, String channel, double perSecond, double burst, long nowTick) {
        TokenBucket bucket = buckets.computeIfAbsent(owner, k -> new HashMap<>())
                .computeIfAbsent(channel, c -> new TokenBucket(burst, perSecond / TICKS_PER_SECOND, nowTick));
        boolean ok = bucket.tryAcquire(nowTick);
        Counters count = counters.computeIfAbsent(owner, k -> new LinkedHashMap<>())
                .computeIfAbsent(channel, c -> new Counters());
        if (ok) {
            count.accepted++;
        } else {
            count.dropped++;
        }
        return ok;
    }

    /** Contadores de um dono (vazio se nunca enviou nada). */
    public Map<String, Counters> stats(K owner) {
        return counters.getOrDefault(owner, Map.of());
    }

    /** Descarta tudo de um dono (ex.: jogador saiu do servidor). */
    public void forget(K owner) {
        buckets.remove(owner);
        counters.remove(owner);
    }
}
