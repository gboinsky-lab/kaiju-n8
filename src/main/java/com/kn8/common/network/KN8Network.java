// src/main/java/com/kn8/common/network/KN8Network.java
package com.kn8.common.network;

import com.kn8.common.anim.AnimTriggerS2C;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.base.BaseBuildC2S;
import com.kn8.common.base.OpenBaseS2C;
import com.kn8.common.base.OpenMissionBoardS2C;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.CareerSyncS2C;
import com.kn8.common.career.MissionActionC2S;
import com.kn8.common.craft.CraftC2S;
import com.kn8.common.soldier.SquadCommandC2S;
import com.kn8.common.craft.OpenWorkbenchS2C;
import com.kn8.common.research.OpenResearchS2C;
import com.kn8.common.research.ResearchC2S;
import com.kn8.common.site.RegionAlertS2C;
import com.kn8.common.invasion.InvasionStateS2C;
import com.kn8.common.combat.CombatInputC2S;
import com.kn8.common.combat.CombatNetwork;
import com.kn8.common.combat.AmmoS2C;
import com.kn8.common.combat.CombatStateS2C;
import com.kn8.common.attribute.PowerSyncS2C;
import com.kn8.common.attribute.ReleaseInputC2S;
import com.kn8.common.host.KaijuFormC2S;
import com.kn8.common.data.DataSyncS2C;
import com.kn8.common.network.debug.NetPingC2S;
import com.kn8.common.vfx.VfxS2C;
import com.kn8.common.network.debug.NetPongS2C;
import com.kn8.common.parkour.ParkourInputC2S;
import com.kn8.common.parkour.ParkourStateS2C;
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
    // 6 = 0.1-B: VfxS2C (efeitos). 7/8 = 0.2 (PowerView.winded, sons no JSON da arma). 9 = 0.2: CareerSyncS2C,
    // MissionActionC2S, Active.point, CraftC2S, OpenWorkbenchS2C, InvasionStateS2C.
    // 10 = 0.3: InvasionStateS2C.level (niveis de invasao). 11 = 0.5: CombatAction.SPECIAL e weapon.special.
    // 12 = 0.6-D: weapon.special.slash (corte a distancia) no registro de armas sincronizado.
    // 13 = 0.5.0-A: ReleaseInputC2S e PowerView novo. 14 = 0.5.0-C: registro sincronizado "locomotion".
    // 15 = 0.5.0-D: AmmoS2C, CombatResult.RELOADING e registro sincronizado "weapon_profile".
    // 16 = 0.5.0-D3: CombatResult.DENIED_NEEDS_DUAL. 17 = 0.5.0-D7: AnimTriggerS2C.speed (golpes com o Release).
    // 18 = 0.5.0-E: ParkourInputC2S e ParkourStateS2C (parkour do jogador). 19 = IA-5: SquadCommandC2S (esquadrao).
    // 20 = VFX-1: VfxS2C.source (perfil visual e receita) e registro sincronizado "vfx_profile".
    // 21 = VFX-2: VfxS2C.source.height (o efeito cresce com o corpo de quem o faz).
    // 22 = PM-1: CombatResult.PERFECT_DODGE e especiais "lunge"/"flurry" no registro de armas sincronizado.
    // 25 = PM-8B: categoria "exam" e "rewards.body_xp" no registro de missoes sincronizado.
    // 26 = PM-8C: OpenResearchS2C, ResearchC2S, CareerView.research e registro sincronizado "research".
    // 27 = PM-8D: OpenBaseS2C, BaseBuildC2S e registro sincronizado "base_module".
    // 28 = PM-9: RegionAlertS2C (alerta por regiao).
    // 30 = PM-11A: registro sincronizado "numbered_gear", CareerView.Research.mastery e DENIED_INCOMPATIBLE.
    // 31 = PM-11B: blocos "tail" e "wings" no registro "numbered_gear".
    // 32 = T-2: KaijuFormC2S (tecla da forma do Kaiju No. 8).
    // 33 = T-3: kn8:kaiju_form virou inteiro (humano, forma, sem controle) e o punho do No. 8 ganhou especial.
    // 34 = COMBATE-1: CombatAction.NEXT_TECHNIQUE (tecla X) e weapon.techniques no registro de armas sincronizado.
    // 35 = COMBATE-3: CombatResult.DENIED_NEEDS_HEIGHT (Falling Thunder vem de cima).
    private static final String PROTOCOL_VERSION = "35";

    /** Diagnostico: 20 por segundo, rajada de 40. */
    private static final C2SGuard.Limit DEBUG_LIMIT = new C2SGuard.Limit(20, 40);
    /** Combate: 20 por segundo, rajada de 20 (bloqueio segurar/soltar + golpes + esquiva com folga). */
    private static final C2SGuard.Limit COMBAT_LIMIT = new C2SGuard.Limit(20, 20);
    /** Canal de teste do M3 sem intervalo minimo entre envios. */
    private static final int PROBE_MIN_INTERVAL_TICKS = 0;
    /** Poder do jogador: no maximo 4 envios por segundo (stamina e calor mudam todo tick). */
    private static final int POWER_MIN_INTERVAL_TICKS = 5;
    /** Carreira: muda pouco (merito, missoes); no maximo 2 envios por segundo. */
    private static final int CAREER_MIN_INTERVAL_TICKS = 10;
    /** Menu: 4 por segundo, rajada de 4 (aceitar/abandonar missao). */
    private static final C2SGuard.Limit MENU_LIMIT = new C2SGuard.Limit(4, 4);
    /** Tecla de Release: pressionar/soltar, ate 10 por segundo com rajada de 10. */
    private static final C2SGuard.Limit RELEASE_LIMIT = new C2SGuard.Limit(10, 10);

    /** T-2: tecla da forma do No. 8, ate 2 por segundo com rajada de 2. */
    private static final C2SGuard.Limit FORM_LIMIT = new C2SGuard.Limit(2, 2);

    /** Parkour: intencoes pontuais (pulo, borda, slide), ate 10 por segundo com rajada de 10. */
    private static final C2SGuard.Limit PARKOUR_LIMIT = new C2SGuard.Limit(10, 10);

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
        // 0.2: carreira do proprio jogador (privado) e acoes de missao vindas do menu.
        registrar.playToClient(CareerSyncS2C.TYPE, CareerSyncS2C.STREAM_CODEC, CareerSyncS2C::handleOnClient);
        toServer(registrar, MissionActionC2S.TYPE, MissionActionC2S.STREAM_CODEC, MENU_LIMIT, MissionActionC2S::handle);
        toServer(registrar, CraftC2S.TYPE, CraftC2S.STREAM_CODEC, MENU_LIMIT, CraftC2S::handle);
        // IA-5: esquadrao do jogador (recrutar, dispensar, ordens) pela aba Esquadrao.
        toServer(registrar, SquadCommandC2S.TYPE, SquadCommandC2S.STREAM_CODEC, MENU_LIMIT, SquadCommandC2S::handle);
        toClientHook(registrar, OpenWorkbenchS2C.TYPE, OpenWorkbenchS2C.STREAM_CODEC);
        // PM-8: oficial de missoes abre o quadro de missoes dele.
        toClientHook(registrar, OpenMissionBoardS2C.TYPE, OpenMissionBoardS2C.STREAM_CODEC);
        // PM-8C: terminal de pesquisa (abre a secao Pesquisa) e pedido de pesquisar.
        toClientHook(registrar, OpenResearchS2C.TYPE, OpenResearchS2C.STREAM_CODEC);
        toServer(registrar, ResearchC2S.TYPE, ResearchC2S.STREAM_CODEC, MENU_LIMIT, ResearchC2S::handle);
        // PM-8D: base do jogador (estado ao usar o nucleo e depois de cada obra) e pedido de construir/demolir.
        toClientHook(registrar, OpenBaseS2C.TYPE, OpenBaseS2C.STREAM_CODEC);
        toServer(registrar, BaseBuildC2S.TYPE, BaseBuildC2S.STREAM_CODEC, MENU_LIMIT, BaseBuildC2S::handle);
        // PM-9: estado de alerta da regiao onde o jogador esta (so na mudanca).
        toClientHook(registrar, RegionAlertS2C.TYPE, RegionAlertS2C.STREAM_CODEC);
        toClientHook(registrar, InvasionStateS2C.TYPE, InvasionStateS2C.STREAM_CODEC);
        // 0.5.0: tecla de Release (so na mudanca; o servidor sobe/desce a % no tick).
        toServer(registrar, ReleaseInputC2S.TYPE, ReleaseInputC2S.STREAM_CODEC, RELEASE_LIMIT,
                ReleaseInputC2S::handle);
        // T-2: tecla da forma do Kaiju No. 8 (o servidor decide transformar ou voltar).
        toServer(registrar, KaijuFormC2S.TYPE, KaijuFormC2S.STREAM_CODEC, FORM_LIMIT, KaijuFormC2S::handle);
        // M10: intencao de combate (o servidor valida tudo).
        toServer(registrar, CombatInputC2S.TYPE, CombatInputC2S.STREAM_CODEC, COMBAT_LIMIT, CombatNetwork::handle);
        // 0.1-B: efeitos visuais (desenhados no cliente).
        toClientHook(registrar, VfxS2C.TYPE, VfxS2C.STREAM_CODEC);
        // M10b: resposta privada do combate (HUD do dono).
        toClientHook(registrar, CombatStateS2C.TYPE, CombatStateS2C.STREAM_CODEC);
        // 0.5.0-D: pente da arma de fogo (HUD do dono).
        toClientHook(registrar, AmmoS2C.TYPE, AmmoS2C.STREAM_CODEC);
        // 0.5.0-E: parkour (intencao do cliente; estado continuo aprovado so para o dono).
        toServer(registrar, ParkourInputC2S.TYPE, ParkourInputC2S.STREAM_CODEC, PARKOUR_LIMIT,
                ParkourInputC2S::handle);
        toClientHook(registrar, ParkourStateS2C.TYPE, ParkourStateS2C.STREAM_CODEC);
        // M9: animacao PAL do jogador (tratada por codigo de cliente via KN8ClientHooks).
        toClientHook(registrar, AnimTriggerS2C.TYPE, AnimTriggerS2C.STREAM_CODEC);
    }

    /** Canais de sync privado. Chamado uma vez pelo construtor de {@code KN8}. */
    public static void registerPrivateChannels() {
        NetworkSync.registerPrivate(NetProbeSyncS2C.CHANNEL, PROBE_MIN_INTERVAL_TICKS,
                player -> new NetProbeSyncS2C(player.getData(KN8Attachments.NET_PROBE)));
        NetworkSync.registerPrivate(PowerSyncS2C.CHANNEL, POWER_MIN_INTERVAL_TICKS,
                player -> new PowerSyncS2C(PowerService.view(player)));
        NetworkSync.registerPrivate(CareerSyncS2C.CHANNEL, CAREER_MIN_INTERVAL_TICKS,
                player -> new CareerSyncS2C(CareerService.view(player)));
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
