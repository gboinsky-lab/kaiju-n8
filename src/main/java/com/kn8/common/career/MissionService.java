package com.kn8.common.career;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.attribute.PowerService;
import com.kn8.common.boss.BossService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Missoes (0.2, Etapa 5): aceitar, acompanhar e recompensar. Os objetivos sao feitos EM ORDEM; quando um objetivo
 * vira o atual, a missao prepara o mundo para ele (decisao do Miguel: kaiju so aparecem por missoes e alertas):
 * <ul>
 *   <li>{@code kill_kaiju}/{@code dismantle}: faz surgir os kaiju numa area perto do jogador;</li>
 *   <li>{@code defeat_boss}: faz surgir o chefe ({@link BossService});</li>
 *   <li>{@code reach_area}: marca um ponto a alcancar; {@code patrol}: um ponto de cada vez, {@code count} vezes.</li>
 * </ul>
 * O ponto do objetivo atual vai para a HUD (marcador com distancia e direcao).
 */
public final class MissionService {

    private MissionService() {
    }

    public enum Result {
        OK, UNKNOWN, ALREADY_ACTIVE, ALREADY_DONE, COOLDOWN, RANK_TOO_LOW, MISSING_PREREQUISITE, TOO_MANY
    }

    private static final int MAX_ACTIVE = 3;

    // --- aceitar e abandonar -------------------------------------------------------------------------------------

    public static Result canAccept(ServerPlayer player, ResourceLocation id) {
        Optional<MissionDef> def = KN8Data.MISSION.get(id, false);
        if (def.isEmpty()) {
            return Result.UNKNOWN;
        }
        CareerData data = CareerService.data(player);
        if (data.active().containsKey(id)) {
            return Result.ALREADY_ACTIVE;
        }
        if (data.active().size() >= MAX_ACTIVE) {
            return Result.TOO_MANY;
        }
        if (data.completed().contains(id) && !def.get().repeatable()) {
            return Result.ALREADY_DONE;
        }
        Long cooldown = data.cooldowns().get(id);
        if (cooldown != null && player.level().getGameTime() < cooldown) {
            return Result.COOLDOWN;
        }
        if (def.get().requires().rank().map(rank -> !CareerService.hasRank(player, rank)).orElse(false)) {
            return Result.RANK_TOO_LOW;
        }
        if (!data.completed().containsAll(def.get().requires().completed())) {
            return Result.MISSING_PREREQUISITE;
        }
        return Result.OK;
    }

    public static Result accept(ServerPlayer player, ResourceLocation id) {
        Result result = canAccept(player, id);
        if (result != Result.OK) {
            player.displayClientMessage(Component.translatable("kn8.mission.refused." + result.name()
                    .toLowerCase()).withStyle(ChatFormatting.RED), true);
            return result;
        }
        MissionDef def = KN8Data.MISSION.get(id, false).orElseThrow();
        int count = def.objectives().size();
        CareerData.MissionProgress progress = new CareerData.MissionProgress(
                new ArrayList<>(Collections.nCopies(count, 0)), player.level().getGameTime(),
                new ArrayList<>(Collections.nCopies(count, CareerView.NO_POINT)));
        CareerService.data(player).active().put(id, progress);
        player.sendSystemMessage(Component.translatable("kn8.mission.accepted", name(id))
                .withStyle(ChatFormatting.AQUA));
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.PLAYERS, 0.8F, 1.2F);
        activateObjective(player, id, def, progress, 0);
        CareerService.changed(player);
        return Result.OK;
    }

    public static boolean abandon(ServerPlayer player, ResourceLocation id) {
        if (CareerService.data(player).active().remove(id) == null) {
            return false;
        }
        player.sendSystemMessage(Component.translatable("kn8.mission.abandoned", name(id))
                .withStyle(ChatFormatting.GRAY));
        CareerService.changed(player);
        return true;
    }

    // --- progresso -----------------------------------------------------------------------------------------------

    /** Um evento de jogo (abate, desmonte, chefe derrotado) conta para o objetivo ATUAL das missoes ativas. */
    public static void progress(ServerPlayer player, MissionDef.ObjectiveType type, ResourceLocation target) {
        CareerData data = CareerService.data(player);
        for (Map.Entry<ResourceLocation, CareerData.MissionProgress> entry : List.copyOf(data.active().entrySet())) {
            Optional<MissionDef> def = KN8Data.MISSION.get(entry.getKey(), false);
            if (def.isEmpty()) {
                continue;
            }
            int index = currentObjective(def.get(), entry.getValue());
            if (index < 0) {
                continue;
            }
            MissionDef.Objective objective = def.get().objectives().get(index);
            if (objective.type() == type && objective.target().map(target::equals).orElse(true)) {
                advance(player, entry.getKey(), def.get(), entry.getValue(), index);
            }
        }
    }

    /** Um por segundo: chegada a pontos (area/patrulha) e prazo. */
    public static void tick(ServerPlayer player) {
        CareerData data = CareerService.data(player);
        long now = player.level().getGameTime();
        for (Map.Entry<ResourceLocation, CareerData.MissionProgress> entry : List.copyOf(data.active().entrySet())) {
            Optional<MissionDef> def = KN8Data.MISSION.get(entry.getKey(), false);
            if (def.isEmpty()) {
                data.active().remove(entry.getKey());
                CareerService.changed(player);
                continue;
            }
            Optional<Integer> limit = def.get().fail().timeTicks();
            if (limit.isPresent() && now - entry.getValue().acceptedTick() > limit.get()) {
                fail(player, entry.getKey(), "time");
                continue;
            }
            int index = currentObjective(def.get(), entry.getValue());
            if (index < 0) {
                continue;
            }
            MissionDef.ObjectiveType type = def.get().objectives().get(index).type();
            BlockPos point = point(entry.getValue(), index);
            if ((type == MissionDef.ObjectiveType.REACH_AREA || type == MissionDef.ObjectiveType.PATROL)
                    && !point.equals(CareerView.NO_POINT)
                    && horizontalDistance(player, point) <= ServerConfig.MISSION_REACH_RADIUS.get()) {
                advance(player, entry.getKey(), def.get(), entry.getValue(), index);
            }
        }
    }

    public static void onDeath(ServerPlayer player) {
        CareerData data = CareerService.data(player);
        for (ResourceLocation id : List.copyOf(data.active().keySet())) {
            KN8Data.MISSION.get(id, false).filter(def -> def.fail().onDeath()).ifPresent(def -> fail(player, id,
                    "death"));
        }
    }

    private static void advance(ServerPlayer player, ResourceLocation id, MissionDef def,
            CareerData.MissionProgress progress, int index) {
        MissionDef.Objective objective = def.objectives().get(index);
        int value = Math.min(objective.count(), progress.progress().get(index) + 1);
        progress.progress().set(index, value);
        if (value >= objective.count()) {
            int next = currentObjective(def, progress);
            if (next < 0) {
                complete(player, id, def);
                return;
            }
            player.displayClientMessage(Component.translatable("kn8.mission.objective_done", name(id))
                    .withStyle(ChatFormatting.GREEN), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.7F, 1.4F);
            activateObjective(player, id, def, progress, next);
        } else if (objective.type() == MissionDef.ObjectiveType.PATROL) {
            // Proximo ponto da patrulha.
            setPoint(progress, index, KaijuSpawner.surfaceAround(player.serverLevel(), player.blockPosition(),
                    ServerConfig.MISSION_PATROL_MIN.get(), ServerConfig.MISSION_PATROL_MAX.get(), player.getRandom()));
            player.displayClientMessage(Component.translatable("kn8.mission.patrol_point", value, objective.count())
                    .withStyle(ChatFormatting.GREEN), true);
        }
        CareerService.changed(player);
    }

    /** Prepara o mundo para o objetivo que acabou de virar o atual. */
    private static void activateObjective(ServerPlayer player, ResourceLocation id, MissionDef def,
            CareerData.MissionProgress progress, int index) {
        MissionDef.Objective objective = def.objectives().get(index);
        ServerLevel level = player.serverLevel();
        BlockPos area = KaijuSpawner.surfaceAround(level, player.blockPosition(), ServerConfig.MISSION_AREA_MIN.get(),
                ServerConfig.MISSION_AREA_MAX.get(), player.getRandom());
        switch (objective.type()) {
            case KILL_KAIJU, DISMANTLE -> {
                objective.target().ifPresent(species -> {
                    for (int i = 0; i < objective.count() - progress.progress().get(index); i++) {
                        BlockPos spot = KaijuSpawner.surfaceAround(level, area, 0, 8, player.getRandom());
                        KaijuSpawner.spawn(level, species, spot);
                    }
                });
                setPoint(progress, index, area);
            }
            case DEFEAT_BOSS -> {
                objective.target().ifPresent(boss -> BossService.spawn(level, boss, area));
                setPoint(progress, index, area);
            }
            case REACH_AREA -> setPoint(progress, index, area);
            case PATROL -> setPoint(progress, index, KaijuSpawner.surfaceAround(level, player.blockPosition(),
                    ServerConfig.MISSION_PATROL_MIN.get(), ServerConfig.MISSION_PATROL_MAX.get(),
                    player.getRandom()));
        }
        player.sendSystemMessage(Component.translatable("kn8.mission.objective." + objective.type()
                .getSerializedName(), objective.target().map(MissionService::targetName)
                .orElse(Component.empty()), objective.count()).withStyle(ChatFormatting.YELLOW));
        CareerService.changed(player);
    }

    private static void complete(ServerPlayer player, ResourceLocation id, MissionDef def) {
        CareerData data = CareerService.data(player);
        data.active().remove(id);
        data.completed().add(id);
        data.addMissionDone();
        if (def.repeatable() && def.cooldownTicks() > 0) {
            data.cooldowns().put(id, player.level().getGameTime() + def.cooldownTicks());
        }
        MissionDef.Rewards rewards = def.rewards();
        for (ResourceLocation itemId : rewards.items()) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        if (rewards.trainingXp() > 0) {
            PowerService.addTrainingXp(player, rewards.trainingXp());
        }
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("kn8.mission.complete")
                .withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(name(id)));
        player.sendSystemMessage(Component.translatable("kn8.mission.complete_chat", name(id), rewards.merit(),
                rewards.trainingXp()).withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 0.8F, 1.0F);
        rewards.promoteTo().ifPresent(rank -> {
            if (!CareerService.hasRank(player, rank)) {
                CareerService.setRank(player, rank);
            }
        });
        // O merito tambem tenta promover (a missao concluida pode ser a de avaliacao da proxima patente).
        CareerService.addMerit(player, rewards.merit());
        CareerService.changed(player);
    }

    private static void fail(ServerPlayer player, ResourceLocation id, String reason) {
        CareerService.data(player).active().remove(id);
        player.sendSystemMessage(Component.translatable("kn8.mission.failed." + reason, name(id))
                .withStyle(ChatFormatting.RED));
        player.level().playSound(null, player.blockPosition(), KN8Sounds.GUARD_BREAK.get(), SoundSource.PLAYERS,
                0.6F, 0.7F);
        CareerService.changed(player);
    }

    // --- auxiliares ----------------------------------------------------------------------------------------------

    /** Primeiro objetivo nao concluido; -1 = todos concluidos. */
    static int currentObjective(MissionDef def, CareerData.MissionProgress progress) {
        for (int i = 0; i < def.objectives().size(); i++) {
            if (progress.progress().get(i) < def.objectives().get(i).count()) {
                return i;
            }
        }
        return -1;
    }

    private static BlockPos point(CareerData.MissionProgress progress, int index) {
        return index < progress.points().size() ? progress.points().get(index) : CareerView.NO_POINT;
    }

    private static void setPoint(CareerData.MissionProgress progress, int index, BlockPos point) {
        while (progress.points().size() <= index) {
            progress.points().add(CareerView.NO_POINT);
        }
        progress.points().set(index, point);
    }

    private static double horizontalDistance(ServerPlayer player, BlockPos point) {
        double dx = player.getX() - (point.getX() + 0.5);
        double dz = player.getZ() - (point.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("kn8.mission." + id.getPath() + ".name");
    }

    private static Component targetName(ResourceLocation target) {
        if (KN8Data.BOSS.get(target, false).isPresent()) {
            return Component.translatable("kn8.boss." + target.getPath());
        }
        return Component.translatable("entity." + target.getNamespace() + "." + target.getPath());
    }

    /** Missoes ativas para a visao do dono. */
    static List<CareerView.Active> activeView(ServerPlayer player) {
        CareerData data = CareerService.data(player);
        List<CareerView.Active> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, CareerData.MissionProgress> entry : data.activeSorted()) {
            Optional<MissionDef> def = KN8Data.MISSION.get(entry.getKey(), false);
            if (def.isEmpty()) {
                continue;
            }
            long deadline = def.get().fail().timeTicks().map(limit -> entry.getValue().acceptedTick() + limit)
                    .orElse(-1L);
            int index = Math.max(0, currentObjective(def.get(), entry.getValue()));
            BlockPos point = point(entry.getValue(), index);
            list.add(new CareerView.Active(entry.getKey(), List.copyOf(entry.getValue().progress()), deadline, index,
                    point));
        }
        return list;
    }
}
