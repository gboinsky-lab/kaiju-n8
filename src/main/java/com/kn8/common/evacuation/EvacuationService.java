package com.kn8.common.evacuation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.site.RegionService;
import com.kn8.common.vfx.VfxService;
import com.kn8.common.world.KaijuSpawner;
import com.kn8.core.world.RegionAlertMath;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/**
 * Evacuacao (PM-10A, Plano Mestre secao 14). A missao com o objetivo {@code evacuate} faz surgir os civis uma vez so
 * (ao virar o objetivo atual; o mundo salva os civis, recarregar nao duplica), marca o ponto de extracao (HUD e zona
 * desenhada no chao) e, se o JSON pedir, o kaiju que ameaca. Cada civil que chega ao ponto conta para a missao e some;
 * cada um que morre conta como perda; perdas acima de {@code max_losses} fracassam a missao. No fim da missao (qualquer
 * motivo) os civis que sobraram somem. Enquanto dura, a regiao do ponto fica em evacuacao (um estado so, o mesmo da
 * HUD e da aba Alertas: {@link RegionService}).
 */
public final class EvacuationService {

    private static final int CHECK_TICKS = 10;
    private static final int ORPHAN_TICKS = 100;
    private static final double SPREAD = 3.0;
    private static final int LEVEL_TRIES = 12;
    private static final String ZONE_COLOR = "#40ff80";

    private EvacuationService() {
    }

    /** Objetivo {@code evacuate} virou o atual: civis, ponto de extracao e ameaca. Devolve o ponto de extracao. */
    public static BlockPos start(ServerPlayer player, ResourceLocation mission, MissionDef.Objective objective,
            int alreadyEvacuated, BlockPos center) {
        ServerLevel level = player.serverLevel();
        // Na cidade a superficie pode ser um telhado: os pontos ficam na altura do chao de quem aceitou.
        BlockPos area = sameLevel(level, center, ServerConfig.EVAC_SPAWN_MIN.get(), ServerConfig.EVAC_SPAWN_MAX.get(),
                center.getY(), player);
        BlockPos extraction = sameLevel(level, area, ServerConfig.EVAC_EXTRACTION_MIN.get(),
                ServerConfig.EVAC_EXTRACTION_MAX.get(), area.getY(), player);
        int count = Math.max(0, objective.count() + objective.maxLosses() - alreadyEvacuated);
        for (int i = 0; i < count; i++) {
            BlockPos spot = sameLevel(level, area, 0, SPREAD, area.getY(), player);
            CivilianEntity civilian = KN8Entities.CIVILIAN.get().create(level);
            if (civilian == null) {
                continue;
            }
            civilian.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getRandom().nextFloat() * 360,
                    0);
            civilian.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
            civilian.assign(player.getUUID(), mission, extraction);
            level.addFreshEntity(civilian);
        }
        objective.target().ifPresent(species -> {
            double distance = ServerConfig.EVAC_THREAT_DISTANCE.get();
            BlockPos spot = KaijuSpawner.surfaceAround(level, area, distance, distance + 4, player.getRandom());
            KaijuSpawner.spawn(level, species, spot);
            player.sendSystemMessage(Component.translatable("kn8.evacuation.threat", Component.translatable("entity."
                    + species.getNamespace() + "." + species.getPath())).withStyle(ChatFormatting.RED));
        });
        player.sendSystemMessage(Component.translatable("kn8.evacuation.briefing", count,
                objective.maxLosses()).withStyle(ChatFormatting.GOLD));
        drawZone(level, extraction);
        return extraction;
    }

    /** Ponto na superficie entre {@code min} e {@code max} do centro com a altura mais perto de {@code y}. */
    private static BlockPos sameLevel(ServerLevel level, BlockPos center, double min, double max, int y,
            ServerPlayer player) {
        BlockPos best = null;
        for (int i = 0; i < LEVEL_TRIES; i++) {
            BlockPos spot = KaijuSpawner.surfaceAround(level, center, min, max, player.getRandom());
            if (best == null || Math.abs(spot.getY() - y) < Math.abs(best.getY() - y)) {
                best = spot;
            }
            if (Math.abs(best.getY() - y) <= 1) {
                break;
            }
        }
        return best;
    }

    /** Uma vez por segundo pela missao: regiao em evacuacao e a zona de extracao desenhada no chao. */
    public static void missionTick(ServerPlayer player, ResourceLocation mission, BlockPos extraction) {
        ServerLevel level = player.serverLevel();
        String cause = RegionService.EVACUATION_CAUSE + mission;
        RegionService.report(level, extraction, RegionAlertMath.Event.EVACUATING, cause);
        RegionService.report(level, player.blockPosition(), RegionAlertMath.Event.EVACUATING, cause);
        int interval = ServerConfig.EVAC_MARK_INTERVAL_TICKS.get();
        if (level.getGameTime() % interval < 20) {
            drawZone(level, extraction);
        }
    }

    /** Zona de extracao: disco verde do tamanho do raio de extracao (formas desenhadas do aviso). */
    private static void drawZone(ServerLevel level, BlockPos point) {
        int ticks = ServerConfig.EVAC_MARK_INTERVAL_TICKS.get() + 10;
        float radius = ServerConfig.EVAC_EXTRACTION_RADIUS.get().floatValue();
        VfxService.keyed(level, VfxService.TELEGRAPH, new Vec3(point.getX() + 0.5, point.getY() + 0.05,
                point.getZ() + 0.5), Vec3.ZERO, radius, "disc" + ZONE_COLOR + "@" + ticks, 0.0F);
    }

    /** Tick do civil: chegou ao ponto (evacuado) e civil orfao (missao do dono acabou enquanto ele estava longe). */
    static void civilianTick(CivilianEntity civilian) {
        if (civilian.tickCount % CHECK_TICKS != 0 || civilian.extraction() == null || civilian.owner() == null
                || civilian.mission() == null) {
            return;
        }
        ServerLevel level = (ServerLevel) civilian.level();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(civilian.owner());
        if (owner == null) {
            return;
        }
        if (civilian.tickCount % ORPHAN_TICKS == 0 && !MissionService.isActive(owner, civilian.mission())) {
            vanish(level, civilian);
            return;
        }
        BlockPos point = civilian.extraction();
        double dx = civilian.getX() - (point.getX() + 0.5);
        double dz = civilian.getZ() - (point.getZ() + 0.5);
        double radius = ServerConfig.EVAC_EXTRACTION_RADIUS.get();
        if (dx * dx + dz * dz <= radius * radius && owner.level() == level) {
            // Sai do mundo antes de contar: a ultima chegada conclui a missao e limpa os civis que sobraram.
            if (MissionService.evacuating(owner, civilian.mission())) {
                VfxService.keyed(level, VfxService.SMOKE, civilian.position().add(0, 1.0, 0), Vec3.ZERO, 0.6F,
                        "#a0ffc0", 0.0F);
                civilian.discard();
                MissionService.onCivilianEvacuated(owner, civilian.mission());
            }
        }
    }

    /** Civil morto: perda para a missao do dono (se ele estiver no servidor). */
    static void lost(CivilianEntity civilian) {
        if (civilian.owner() == null || civilian.mission() == null) {
            return;
        }
        ServerPlayer owner = ((ServerLevel) civilian.level()).getServer().getPlayerList()
                .getPlayer(civilian.owner());
        if (owner != null) {
            MissionService.onCivilianLost(owner, civilian.mission());
        }
    }

    /** Fim da missao (concluida, fracassada, abandonada): somem os civis dela carregados no mundo. */
    public static void clear(ServerPlayer player, ResourceLocation mission) {
        ServerLevel level = player.serverLevel();
        UUID owner = player.getUUID();
        List<CivilianEntity> list = new ArrayList<>();
        level.getEntities(EntityTypeTest.forClass(CivilianEntity.class), civilian -> owner.equals(civilian.owner())
                && mission.equals(civilian.mission()), list);
        list.forEach(civilian -> vanish(level, civilian));
    }

    /** Civis vivos da missao carregados no mundo (GameTests e comando). */
    public static List<CivilianEntity> civilians(ServerLevel level, UUID owner, ResourceLocation mission) {
        List<CivilianEntity> list = new ArrayList<>();
        level.getEntities(EntityTypeTest.forClass(CivilianEntity.class), civilian -> civilian.isAlive()
                && owner.equals(civilian.owner()) && mission.equals(civilian.mission()), list);
        return list;
    }

    private static void vanish(ServerLevel level, CivilianEntity civilian) {
        VfxService.keyed(level, VfxService.SMOKE, civilian.position().add(0, 1.0, 0), Vec3.ZERO, 0.6F, "#d8d8d8",
                0.0F);
        civilian.discard();
    }

    /** Linha de progresso no chat (evacuado ou perdido). */
    public static void announce(ServerPlayer player, String key, int value, int total) {
        player.displayClientMessage(Component.translatable(key, value, total).withStyle(
                key.endsWith("saved") ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        CareerService.changed(player);
    }
}
