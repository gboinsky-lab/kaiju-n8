package com.kn8.common.site;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.arena.TrainingArenaService;
import com.kn8.common.career.ExamTargets;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.core.world.RegionAlertMath;
import com.kn8.core.world.RegionAlertMath.Event;
import com.kn8.core.world.RegionAlertMath.State;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Alerta por regiao (PM-9, Plano Mestre secao 13): cada local da obra gerado no mundo tem um estado (normal, alerta,
 * combate, recuperacao) que so sobe por causa identificavel (kaiju dentro dele, kaiju lutando, invasao com o centro
 * nele) e so desce pelo tempo ({@link RegionAlertMath}). Sem varredura global: cada kaiju se reporta a cada
 * {@code [region] kaijuCheckTicks}; as regioes fora do normal sao conferidas uma vez por segundo.
 *
 * <p>O que o estado muda (so sistemas que existem): em combate a estacao de preparacao, o terminal de pesquisa e o
 * aceite de provas da regiao ficam fechados; fora do normal a invasao natural do local nao e sorteada (nao empilha
 * ameaca). HUD e aba Alertas mostram o mesmo estado.</p>
 */
public final class RegionService {

    /** Prefixo da causa quando e uma invasao (sem ele, a causa e o id do kaiju). */
    public static final String INVASION_CAUSE = "!";
    /** PM-10A: prefixo da causa quando e uma evacuacao (id da missao). */
    public static final String EVACUATION_CAUSE = "+";

    private RegionService() {
    }

    public static RegionAlertMath.Params params() {
        return new RegionAlertMath.Params(ServerConfig.REGION_ALERT_TICKS.get(), ServerConfig.REGION_COMBAT_TICKS
                .get(), ServerConfig.REGION_RECOVERY_TICKS.get(), ServerConfig.EVAC_REGION_TICKS.get());
    }

    private static String key(SiteService.Hit hit) {
        return hit.id() + "@" + hit.start().getChunkPos().x + "," + hit.start().getChunkPos().z;
    }

    /** Evento real numa posicao: se ela esta num local, sobe o estado da regiao. */
    public static void report(ServerLevel level, BlockPos pos, Event event, String cause) {
        if (!ServerConfig.REGION_ALERTS.get()) {
            return;
        }
        Optional<SiteService.Hit> hit = SiteService.siteAt(level, pos);
        if (hit.isEmpty()) {
            return;
        }
        reportRegion(level, key(hit.get()), hit.get().id(), hit.get().start().getBoundingBox(), event, cause);
    }

    /** Evento numa regiao ja identificada (tambem usado pelos GameTests, que nao geram o local inteiro). */
    public static void reportRegion(ServerLevel level, String key, ResourceLocation site, BoundingBox box,
            Event event, String cause) {
        RegionAlerts data = RegionAlerts.get(level);
        RegionAlerts.Region region = data.getOrCreate(key, site, box);
        RegionAlertMath.Status before = region.status();
        RegionAlertMath.Status after = RegionAlertMath.apply(before, event, level.getGameTime(), params());
        if (after.equals(before)) {
            return;
        }
        boolean raised = after.state().rank() > before.state().rank() && after.state() != State.RECOVERY;
        region.set(after, raised || region.cause().isEmpty() ? cause : region.cause());
        data.setDirty();
        if (after.state() != before.state()) {
            changed(level, region, before.state());
        } else if (after.state() != State.EVACUATION) {
            // Prazo renovado: a faixa de quem esta no local recebe o tempo novo (sem aviso no chat).
            send(level, region);
        }
    }

    /** Kaiju dentro de um local (chamado pelo tick do kaiju); kaiju de treino e de prova nao alarmam a regiao. */
    public static void kaijuTick(KaijuEntity kaiju) {
        if (!(kaiju.level() instanceof ServerLevel level) || !kaiju.isAlive()
                || kaiju.tickCount % ServerConfig.REGION_KAIJU_CHECK_TICKS.get() != 0
                || TrainingArenaService.isTraining(kaiju) || ExamTargets.isExam(kaiju)) {
            return;
        }
        report(level, kaiju.blockPosition(), kaiju.getTarget() != null ? Event.FIGHTING : Event.KAIJU_SEEN,
                kaiju.kaijuId().toString());
    }

    /** Invasao comecou ou terminou: o centro dela decide a regiao. */
    public static void onInvasion(ServerLevel level, BlockPos center, ResourceLocation invasion, boolean ended) {
        report(level, center, ended ? Event.ENDED : Event.INVASION, INVASION_CAUSE + invasion);
    }

    /** Uma vez por segundo: invasao em andamento renova o combate; prazos vencidos descem um degrau. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0 || !ServerConfig.REGION_ALERTS.get()) {
            return;
        }
        RegionAlerts data = RegionAlerts.get(level);
        if (data.regions().isEmpty()) {
            return;
        }
        InvasionService.active(level).ifPresent(invasion -> report(level, invasion.center(), Event.INVASION,
                INVASION_CAUSE + invasion.id()));
        expire(level, level.getGameTime());
    }

    /** Desce as regioes com prazo vencido em {@code now} (publico para os GameTests). */
    public static void expire(ServerLevel level, long now) {
        RegionAlerts data = RegionAlerts.get(level);
        Iterator<Map.Entry<String, RegionAlerts.Region>> it = data.regions().entrySet().iterator();
        while (it.hasNext()) {
            RegionAlerts.Region region = it.next().getValue();
            RegionAlertMath.Status before = region.status();
            RegionAlertMath.Status after = RegionAlertMath.expire(before, now, params());
            if (after.equals(before)) {
                continue;
            }
            region.set(after, region.cause());
            data.setDirty();
            changed(level, region, before.state());
            if (after.state() == State.NORMAL) {
                it.remove();
            }
        }
    }

    /** Estado da regiao na posicao (normal fora de qualquer regiao conhecida). */
    public static State stateAt(ServerLevel level, BlockPos pos) {
        for (RegionAlerts.Region region : RegionAlerts.get(level).regions().values()) {
            if (region.box().isInside(pos)) {
                return region.status().state();
            }
        }
        return State.NORMAL;
    }

    /** Servicos da base fechados durante o combate na regiao. */
    public static boolean servicesClosed(ServerLevel level, BlockPos pos) {
        return ServerConfig.REGION_CLOSE_SERVICES.get() && stateAt(level, pos) == State.COMBAT;
    }

    public static void refuseClosed(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("kn8.region.closed").withStyle(ChatFormatting.RED), true);
    }

    /** Manda ao jogador o estado da regiao em que ele esta (entrada/saida do local, login). */
    public static void syncTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (RegionAlerts.Region region : RegionAlerts.get(level).regions().values()) {
            if (region.box().isInside(player.blockPosition())) {
                PacketDistributor.sendToPlayer(player, payload(region));
                return;
            }
        }
        Optional<SiteService.Hit> hit = SiteService.siteAt(level, player.blockPosition());
        PacketDistributor.sendToPlayer(player, hit.map(value -> new RegionAlertS2C(value.id().toString(),
                State.NORMAL.ordinal(), "", 0L)).orElse(RegionAlertS2C.NONE));
    }

    private static RegionAlertS2C payload(RegionAlerts.Region region) {
        return new RegionAlertS2C(region.site().toString(), region.status().state().ordinal(), region.cause(),
                region.status().until());
    }

    private static void changed(ServerLevel level, RegionAlerts.Region region, State before) {
        State now = region.status().state();
        KN8Constants.LOGGER.info("[kn8] Regiao {} {}: {} -> {} ({})", region.site(), region.box().getCenter()
                .toShortString(), before, now, region.cause());
        BoundingBox box = region.box().inflatedBy(ServerConfig.REGION_NOTIFY_MARGIN.get());
        RegionAlertS2C payload = payload(region);
        for (ServerPlayer player : level.players()) {
            if (!box.isInside(player.blockPosition())) {
                continue;
            }
            PacketDistributor.sendToPlayer(player, payload);
            ChatFormatting color = switch (now) {
                case COMBAT -> ChatFormatting.RED;
                case ALERT -> ChatFormatting.YELLOW;
                case RECOVERY -> ChatFormatting.AQUA;
                case EVACUATION -> ChatFormatting.GOLD;
                case NORMAL -> ChatFormatting.GREEN;
            };
            player.sendSystemMessage(Component.translatable("kn8.region.changed", SiteService.name(region.site()),
                    Component.translatable("kn8.region.state." + now.name().toLowerCase(java.util.Locale.ROOT)),
                    causeName(region.cause())).withStyle(color));
        }
    }

    private static void send(ServerLevel level, RegionAlerts.Region region) {
        BoundingBox box = region.box().inflatedBy(ServerConfig.REGION_NOTIFY_MARGIN.get());
        RegionAlertS2C payload = payload(region);
        for (ServerPlayer player : level.players()) {
            if (box.isInside(player.blockPosition())) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** Nome da causa: invasao (com o prefixo) ou kaiju (entidade); serve tambem ao cliente. */
    public static Component causeName(String cause) {
        boolean invasion = cause.startsWith(INVASION_CAUSE);
        boolean evacuation = cause.startsWith(EVACUATION_CAUSE);
        ResourceLocation id = ResourceLocation.tryParse(invasion || evacuation ? cause.substring(1) : cause);
        if (id == null || cause.isEmpty()) {
            return Component.translatable("kn8.region.cause.unknown");
        }
        if (evacuation) {
            return Component.translatable("kn8.mission." + id.getPath() + ".name");
        }
        return invasion ? Component.translatable("kn8.invasion." + id.getPath())
                : Component.translatable("entity." + id.getNamespace() + "." + id.getPath());
    }
}
