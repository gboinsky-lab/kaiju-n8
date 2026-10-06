// src/main/java/com/kn8/common/network/KN8Network.java
package com.kn8.common.network;

import com.kn8.common.anim.AnimTriggerS2C;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatInputC2S;
import com.kn8.common.combat.CombatNetwork;
import com.kn8.common.combat.CombatStateS2C;
import com.kn8.common.attribute.PowerSyncS2C;
import com.kn8.common.data.DataSyncS2C;
import com.kn8.common.network.debug.NetPingC2S;
import com.kn8.common.vfx.VfxS2C;
import com.kn8.common.network.debug.NetPongS2C;
import com.kn8.common.network.debug.NetProbeSyncS2C;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro de todos os payloads e canais de sync privado do mod (Fase 4, secao 5).
 *
 * <p>Tres formas de registrar:</p>
 * <ul>
 *   <li>{@link #toServer}: C2S, sempre atras do {@link C2SGuard} (rate limit + atraso de debug);</li>
 *   <li>{@link #toClientHook}: S2C tratado por codigo de cliente via {@link KN8ClientHooks};</li>
 *   <li>{@code registrar.playToClient} direto: S2C cujo handler so usa classes comuns (ex.: grava attachment no
 *   jogador local).</li>
 * </ul>
 *
 * <p>Os limites abaixo sao limites do PROTOCOLO (Fase 4, secao 5.1), nao balanceamento; o servidor pode
 * escala-los com {@code network.rateLimitMultiplier}.</p>
 */
public final class KN8Network {

    // Mudar quando o formato de algum payload mudar, para recusar clientes incompativeis com mensagem clara.
    // 2 = M6: PowerView ganhou heatMax. 3 = M9: AnimTriggerS2C. 4 = M10a: CombatInputC2S. 5 = M10b: CombatStateS2C.
    // 6 = 0.1-B: VfxS2C (efeitos).
    private static final String PROTOCOL_VERSION = "6";

    /** Diagnostico: 20 por segundo, rajada de 40. */
    private static final C2SGuard.Limit DEBUG_LIMIT = new C2SGuard.Limit(20, 40);
    /** Combate: 20 por segundo, rajada de 20 (bloqueio segurar/soltar + golpes + esquiva com folga). */
    private static final C2SGuard.Limit COMBAT_LIMIT = new C2SGuard.Limit(20, 20);
    /** Canal de teste do M3 sem intervalo minimo entre envios. */
    private static final int PROBE_MIN_INTERVAL_TICKS = 0;
    /** Poder do jogador: no maximo 4 envios por segundo (stamina e calor mudam todo tick). */
    private static final int POWER_MIN_INTERVAL_TICKS = 5;

    private KN8Network() {
    }

    /** Mod bus. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        toServer(registrar, NetPingC2S.TYPE, NetPingC2S.STREAM_CODEC, DEBUG_LIMIT, NetPingC2S::handle);
        toClientHook(registrar, NetPongS2C.TYPE, NetPongS2C.STREAM_CODEC);
        registrar.playToClient(NetProbeSyncS2C.TYPE, NetProbeSyncS2C.STREAM_CODEC, NetProbeSyncS2C::handleOnClient);
        // M4: definicoes de dados (estado completo por tipo, no login e apos /reload).
        registrar.playToClient(DataSyncS2C.TYPE, DataSyncS2C.STREAM_CODEC, DataSyncS2C::handleOnClient);
        // M5: poder do proprio jogador (privado).
        registrar.playToClient(PowerSyncS2C.TYPE, PowerSyncS2C.STREAM_CODEC, PowerSyncS2C::handleOnClient);
        // M10: intencao de combate (o servidor valida tudo).
        toServer(registrar, CombatInputC2S.TYPE, CombatInputC2S.STREAM_CODEC, COMBAT_LIMIT, CombatNetwork::handle);
        // 0.1-B: efeitos visuais (desenhados no cliente).
        toClientHook(registrar, VfxS2C.TYPE, VfxS2C.STREAM_CODEC);
        // M10b: resposta privada do combate (HUD do dono).
        toClientHook(registrar, CombatStateS2C.TYPE, CombatStateS2C.STREAM_CODEC);
        // M9: animacao PAL do jogador (tratada por codigo de cliente via KN8ClientHooks).
        toClientHook(registrar, AnimTriggerS2C.TYPE, AnimTriggerS2C.STREAM_CODEC);
    }

    /** Canais de sync privado. Chamado uma vez pelo construtor de {@code KN8}. */
    public static void registerPrivateChannels() {
        NetworkSync.registerPrivate(NetProbeSyncS2C.CHANNEL, PROBE_MIN_INTERVAL_TICKS,
                player -> new NetProbeSyncS2C(player.getData(KN8Attachments.NET_PROBE)));
        NetworkSync.registerPrivate(PowerSyncS2C.CHANNEL, POWER_MIN_INTERVAL_TICKS,
                player -> new PowerSyncS2C(PowerService.view(player)));
    }

    private static <T extends CustomPacketPayload> void toServer(PayloadRegistrar registrar,
            CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            C2SGuard.Limit limit, C2SGuard.ServerHandler<T> handler) {
        registrar.playToServer(type, codec, C2SGuard.wrap(type, limit, handler));
    }

    private static <T extends CustomPacketPayload> void toClientHook(PayloadRegistrar registrar,
            CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        registrar.playToClient(type, codec, (payload, context) -> context.enqueueWork(
                () -> KN8ClientHooks.dispatch(payload)));
    }
}
