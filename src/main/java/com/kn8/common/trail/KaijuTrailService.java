package com.kn8.common.trail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.kn8.common.arena.TrainingArenaService;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.ExamTargets;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.host.LarvaService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Rastro de destruicao e investigacao (PM-9, Plano Mestre secao 16). Pistas nascem de eventos reais: o kaiju pediu
 * para quebrar o caminho/alvo (andando, preso, golpe) ou morreu. Uma pista por kaiju a cada {@code [trail]
 * pointSpacing} blocos; teto e prazo no config, sem tick global (a limpeza acontece ao gravar e ao investigar).
 *
 * <p>O Rastreador de Kaiju investiga as pistas perto: mostra no chao o caminho de cada rastro (do mais velho ao mais
 * novo) e diz no chat a especie, quantas pistas, ha quanto tempo, para onde ia e se o kaiju ainda esta no mundo
 * (nunca aponta para kaiju que nao existe: sem o kaiju, o rastro e "frio"; sem kaiju conhecido, "inconclusivo").
 * Cada pista da merito uma vez so, para quem investiga primeiro.</p>
 */
public final class KaijuTrailService {

    private static final String[] DIRECTIONS = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
    private static final int MAX_TRAILS_SHOWN = 3;

    private KaijuTrailService() {
    }

    /** Kaiju quebrou (ou pediu para quebrar) algo onde esta, ou morreu. */
    public static void note(KaijuEntity kaiju, KaijuTrail.Kind kind) {
        if (!(kaiju.level() instanceof ServerLevel level) || !ServerConfig.TRAIL_ENABLED.get()
                || TrainingArenaService.isTraining(kaiju) || ExamTargets.isExam(kaiju)
                || LarvaService.isHostLarva(kaiju)) {
            return;
        }
        KaijuTrail trail = KaijuTrail.get(level);
        long now = level.getGameTime();
        trail.expire(now, ServerConfig.TRAIL_LIFE_TICKS.get());
        BlockPos pos = kaiju.blockPosition();
        double spacing = ServerConfig.TRAIL_POINT_SPACING.get();
        if (kind == KaijuTrail.Kind.DESTRUCTION) {
            List<KaijuTrail.Point> points = trail.points();
            for (int i = points.size() - 1; i >= 0 && i >= points.size() - 64; i--) {
                KaijuTrail.Point last = points.get(i);
                if (kaiju.getUUID().equals(last.kaiju)) {
                    if (last.pos.distSqr(pos) < spacing * spacing) {
                        return;
                    }
                    break;
                }
            }
        }
        trail.add(pos, kaiju.kaijuId(), kaiju.getUUID(), now, kind, ServerConfig.TRAIL_MAX_POINTS.get());
    }

    /** Resultado da investigacao (para os GameTests): rastros achados e pistas novas (com merito). */
    public record Investigation(int trails, int clues, int fresh) {
    }

    public static Investigation investigate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        KaijuTrail trail = KaijuTrail.get(level);
        long now = level.getGameTime();
        trail.expire(now, ServerConfig.TRAIL_LIFE_TICKS.get());
        double radius = ServerConfig.TRAIL_SCAN_RADIUS.get();
        // Agrupa por kaiju (sem kaiju: um grupo por especie, inconclusivo).
        Map<String, List<KaijuTrail.Point>> groups = new LinkedHashMap<>();
        for (KaijuTrail.Point point : trail.points()) {
            if (point.pos.distToCenterSqr(player.position()) <= radius * radius) {
                String key = point.kaiju != null ? point.kaiju.toString() : "?" + point.species;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(point);
            }
        }
        if (groups.isEmpty()) {
            player.sendSystemMessage(Component.translatable("kn8.trail.none", (int) radius)
                    .withStyle(ChatFormatting.GRAY));
            return new Investigation(0, 0, 0);
        }
        List<List<KaijuTrail.Point>> sorted = new ArrayList<>(groups.values());
        // O rastro mais recente primeiro.
        sorted.sort(Comparator.comparingLong((List<KaijuTrail.Point> list) -> -list.get(list.size() - 1).tick));
        int clues = 0;
        int fresh = 0;
        for (int g = 0; g < sorted.size() && g < MAX_TRAILS_SHOWN; g++) {
            List<KaijuTrail.Point> list = sorted.get(g);
            clues += list.size();
            for (KaijuTrail.Point point : list) {
                if (!point.investigated) {
                    point.investigated = true;
                    fresh++;
                }
            }
            describe(player, level, list, now);
            draw(level, list);
        }
        if (fresh > 0) {
            trail.setDirty();
            int merit = fresh * ServerConfig.TRAIL_MERIT_PER_CLUE.get();
            if (merit > 0) {
                CareerService.addMerit(player, merit);
                player.sendSystemMessage(Component.translatable("kn8.trail.merit", fresh, merit)
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return new Investigation(Math.min(sorted.size(), MAX_TRAILS_SHOWN), clues, fresh);
    }

    private static void describe(ServerPlayer player, ServerLevel level, List<KaijuTrail.Point> list, long now) {
        KaijuTrail.Point first = list.get(0);
        KaijuTrail.Point last = list.get(list.size() - 1);
        Component species = Component.translatable("entity." + last.species.getNamespace() + "."
                + last.species.getPath());
        long seconds = (now - last.tick + 19) / 20;
        String ago = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        Component heading;
        if (list.size() < 2 || first.pos.distSqr(last.pos) < 4) {
            heading = Component.translatable("kn8.trail.dir.here");
        } else {
            double angle = Math.toDegrees(Math.atan2(-(last.pos.getX() - first.pos.getX()),
                    last.pos.getZ() - first.pos.getZ()));
            int index = Math.floorMod(Math.round((float) (angle / 45.0)), 8);
            heading = Component.translatable("kn8.trail.dir." + DIRECTIONS[index]);
        }
        Component status;
        ChatFormatting color;
        if (last.kaiju == null) {
            status = Component.translatable("kn8.trail.status.inconclusive");
            color = ChatFormatting.GRAY;
        } else if (last.kind == KaijuTrail.Kind.DEATH) {
            status = Component.translatable("kn8.trail.status.dead");
            color = ChatFormatting.GRAY;
        } else {
            Entity entity = level.getEntity(last.kaiju);
            if (entity instanceof KaijuEntity kaiju && kaiju.isAlive()) {
                status = Component.translatable("kn8.trail.status.active",
                        (int) Math.round(Math.sqrt(kaiju.distanceToSqr(player))));
                color = ChatFormatting.RED;
            } else {
                status = Component.translatable("kn8.trail.status.cold");
                color = ChatFormatting.YELLOW;
            }
        }
        player.sendSystemMessage(Component.translatable("kn8.trail.found", species, list.size(), ago, heading,
                status).withStyle(color));
    }

    /** Caminho do rastro no chao (formas desenhadas da faixa do aviso), do mais velho ao mais novo. */
    private static void draw(ServerLevel level, List<KaijuTrail.Point> list) {
        String color = "#ffb030";
        int ticks = ServerConfig.TRAIL_MARK_TICKS.get();
        for (int i = 0; i < list.size(); i++) {
            BlockPos pos = list.get(i).pos;
            Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
            VfxService.keyed(level, VfxService.TELEGRAPH, at, Vec3.ZERO, 0.7F, "disc" + color + "@" + ticks, 0.0F);
            if (i + 1 < list.size()) {
                BlockPos next = list.get(i + 1).pos;
                VfxService.keyed(level, VfxService.TELEGRAPH, at, new Vec3(next.getX() + 0.5, next.getZ() + 0.5,
                        0.25), (float) Math.sqrt(pos.distSqr(next)), "lane" + color + "@" + ticks, 0.0F);
            }
        }
    }

    /** Pistas de um kaiju (GameTests). */
    public static long count(ServerLevel level, UUID kaiju) {
        return KaijuTrail.get(level).points().stream().filter(point -> kaiju.equals(point.kaiju)).count();
    }
}
