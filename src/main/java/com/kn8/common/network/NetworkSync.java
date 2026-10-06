// src/main/java/com/kn8/common/network/NetworkSync.java
package com.kn8.common.network;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.kn8.common.server.KN8Server;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sync de dados PRIVADOS (so para o dono), padrao validado no PT1. Dados publicos nao passam por aqui: usam o sync
 * nativo do NeoForge ({@code AttachmentType.Builder#sync}).
 *
 * <ul>
 *   <li>Quem muda um dado privado chama {@link #markDirty}; o envio acontece no fim do tick (agrupado), respeitando
 *   o intervalo minimo do canal.</li>
 *   <li>Login, respawn e troca de dimensao reenviam todos os canais ({@link #sendAll}); o cliente recria o jogador
 *   local nesses momentos e perde os dados.</li>
 *   <li>Start tracking NUNCA envia: outros jogadores nao recebem dado privado.</li>
 * </ul>
 *
 * <p>Os canais sao definicoes registradas na construcao do mod (como DeferredRegisters), nao estado de jogo; o
 * estado de envio fica no {@code KN8Server}.</p>
 */
public final class NetworkSync {

    /** Cria o payload com o valor atual de um canal para um jogador. */
    @FunctionalInterface
    public interface Snapshot {
        CustomPacketPayload create(ServerPlayer player);
    }

    private record Channel(ResourceLocation id, int minIntervalTicks, Snapshot snapshot) {
    }

    private static final Map<ResourceLocation, Channel> CHANNELS = new LinkedHashMap<>();

    private NetworkSync() {
    }

    /** Registra um canal privado. Chamado uma vez, na construcao do mod. */
    public static void registerPrivate(ResourceLocation id, int minIntervalTicks, Snapshot snapshot) {
        if (CHANNELS.putIfAbsent(id, new Channel(id, Math.max(0, minIntervalTicks), snapshot)) != null) {
            throw new IllegalStateException("Canal de sync duplicado: " + id);
        }
    }

    /** Marca um canal como alterado para o jogador; o envio sai no fim do tick. */
    public static void markDirty(ServerPlayer player, ResourceLocation channel) {
        KN8Server services = KN8Server.getOrNull(player.getServer());
        if (services != null && CHANNELS.containsKey(channel)) {
            services.privateSync().markDirty(player.getUUID(), channel);
        }
    }

    /** Envia todos os canais ao jogador agora (login, respawn, dimensao, comando de diagnostico). */
    public static void sendAll(ServerPlayer player) {
        KN8Server services = KN8Server.getOrNull(player.getServer());
        long now = services == null ? 0 : services.server().getTickCount();
        for (Channel channel : CHANNELS.values()) {
            send(player, channel, now, services);
        }
    }

    /** Fim de tick: envia os canais sujos cujo intervalo minimo ja passou; os demais esperam o proximo tick. */
    public static void flush(KN8Server services) {
        PrivateSyncState state = services.privateSync();
        long now = services.server().getTickCount();
        Iterator<Map.Entry<UUID, Set<ResourceLocation>>> players = state.dirty().entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Set<ResourceLocation>> entry = players.next();
            ServerPlayer player = services.server().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                players.remove();
                continue;
            }
            entry.getValue().removeIf(id -> {
                Channel channel = CHANNELS.get(id);
                if (channel == null) {
                    return true;
                }
                long last = state.lastSent(player.getUUID(), id);
                if (last != Long.MIN_VALUE && now - last < channel.minIntervalTicks()) {
                    return false;
                }
                send(player, channel, now, services);
                return true;
            });
            if (entry.getValue().isEmpty()) {
                players.remove();
            }
        }
    }

    private static void send(ServerPlayer player, Channel channel, long now, KN8Server services) {
        PacketDistributor.sendToPlayer(player, channel.snapshot().create(player));
        if (services != null) {
            services.privateSync().recordSent(player.getUUID(), channel.id(), now);
        }
    }
}
