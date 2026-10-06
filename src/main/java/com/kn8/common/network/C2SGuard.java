// src/main/java/com/kn8/common/network/C2SGuard.java
package com.kn8.common.network;

import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.server.KN8Server;
import com.kn8.core.net.RateLimiter;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

/**
 * Porta de entrada de TODO payload C2S (Fase 4, secao 5.3). Ordem, sempre na thread principal do servidor:
 * <ol>
 *   <li>so aceita pacote de um {@link ServerPlayer} com o servidor do kn8 ativo;</li>
 *   <li>rate limit por jogador e por tipo de payload (excesso e descartado e contado, nunca derruba ninguem);</li>
 *   <li>atraso artificial de debug, se ligado ({@code /kn8 debug latency});</li>
 *   <li>handler do sistema, que ainda valida estado, recarga, alcance etc.</li>
 * </ol>
 */
public final class C2SGuard {

    /** Descartes sao logados no primeiro e depois a cada N, para nao inundar o log durante um spam. */
    private static final long DROP_LOG_EVERY = 100;

    /** Handler de payload ja com o jogador validado. */
    @FunctionalInterface
    public interface ServerHandler<T> {
        void handle(T payload, ServerPlayer player);
    }

    /** Limite de um tipo de payload: pedidos por segundo sustentados e rajada maxima. */
    public record Limit(double perSecond, double burst) {
    }

    private C2SGuard() {
    }

    static <T extends CustomPacketPayload> IPayloadHandler<T> wrap(CustomPacketPayload.Type<T> type, Limit limit,
            ServerHandler<T> handler) {
        String channel = type.id().toString();
        return (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            KN8Server services = KN8Server.getOrNull(player.getServer());
            if (services == null) {
                return;
            }
            double multiplier = rateLimitMultiplier();
            long now = services.server().getTickCount();
            RateLimiter<UUID> limiter = services.rateLimiter();
            if (!limiter.tryAcquire(player.getUUID(), channel, limit.perSecond() * multiplier,
                    limit.burst() * multiplier, now)) {
                long dropped = limiter.stats(player.getUUID()).get(channel).dropped();
                if (dropped == 1 || dropped % DROP_LOG_EVERY == 0) {
                    KN8Constants.LOGGER.warn("[kn8] Rate limit: {} descartou {} pacote(s) de {} ate agora",
                            player.getGameProfile().getName(), dropped, channel);
                }
                return;
            }
            int delay = services.debugInboundDelayTicks();
            if (delay > 0) {
                services.schedule(delay, () -> {
                    if (!player.isRemoved()) {
                        handler.handle(payload, player);
                    }
                });
                return;
            }
            handler.handle(payload, player);
        });
    }

    private static double rateLimitMultiplier() {
        // Durante o arranque o config de servidor pode ainda nao estar carregado; 1.0 = limites do protocolo.
        return ServerConfig.SPEC.isLoaded() ? ServerConfig.RATE_LIMIT_MULTIPLIER.get() : 1.0;
    }
}
