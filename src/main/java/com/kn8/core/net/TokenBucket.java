// src/main/java/com/kn8/core/net/TokenBucket.java
package com.kn8.core.net;

/**
 * Balde de fichas medido em ticks do servidor (Java puro). Permite rajadas curtas ate {@code capacity} pedidos e
 * recarrega {@code refillPerTick} fichas por tick. Base do rate limit dos payloads C2S (Fase 4, secao 5.3).
 */
public final class TokenBucket {

    private final double capacity;
    private final double refillPerTick;
    private double tokens;
    private long lastTick;

    public TokenBucket(double capacity, double refillPerTick, long nowTick) {
        if (capacity < 1 || refillPerTick <= 0) {
            throw new IllegalArgumentException("Balde invalido: capacidade " + capacity + ", recarga " + refillPerTick);
        }
        this.capacity = capacity;
        this.refillPerTick = refillPerTick;
        this.tokens = capacity;
        this.lastTick = nowTick;
    }

    /** Consome uma ficha se houver; retorna false (pedido descartado) se o balde estiver vazio. */
    public boolean tryAcquire(long nowTick) {
        if (nowTick > lastTick) {
            tokens = Math.min(capacity, tokens + (nowTick - lastTick) * refillPerTick);
            lastTick = nowTick;
        }
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }
}
