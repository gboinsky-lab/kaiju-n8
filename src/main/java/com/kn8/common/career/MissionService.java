package com.kn8.common.career;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.kn8.common.attribute.PowerService;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.boss.BossService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.destruction.ProtectedAreas;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.soldier.SquadService;
import com.kn8.common.world.KaijuSpawner;
import com.kn8.core.career.MissionGrader;
import com.kn8.core.power.BodyStat;

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
        OK, UNKNOWN, ALREADY_ACTIVE, ALREADY_DONE, COOLDOWN, RANK_TOO_LOW, MISSING_PREREQUISITE, TOO_MANY,
        /** PM-7: a prova pede um esquadrao minimo ({@code grading.min_squad}). */
        SQUAD_TOO_SMALL,
        /** PM-8B: prova so com o Oficial de Provas perto (dentro de uma base). */
        NEEDS_EXAM_OFFICER
    }

    /** PM-8B: prova = categoria {@code exam} (curso dentro da base); o resto e missao de campo. */
    public static boolean isExam(MissionDef def) {
        return def.category() == MissionDef.Category.EXAM;
    }

    private static boolean nearExamOfficer(ServerPlayer player) {
        double range = ServerConfig.EXAM_OFFICER_RANGE.get();
        return !player.serverLevel().getEntitiesOfClass(DefenseOfficerEntity.class,
                player.getBoundingBox().inflate(range),
                officer -> DefenseOfficerEntity.EXAM_OFFICER.equals(officer.giver())).isEmpty();
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
        if (isExam(def.get()) && !nearExamOfficer(player)) {
            return Result.NEEDS_EXAM_OFFICER;
        }
        int minSquad = def.get().grading().map(MissionDef.Grading::minSquad).orElse(0);
        if (minSquad > 0 && SquadService.members(player).size() < minSquad) {
            return Result.SQUAD_TOO_SMALL;
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
        // PM-8B: a prova acontece onde foi aceita (na base, junto do oficial); a area nao pode ser quebrada.
        Optional<BlockPos> origin = isExam(def) ? Optional.of(player.blockPosition()) : Optional.empty();
        CareerData.MissionProgress progress = new CareerData.MissionProgress(
                new ArrayList<>(Collections.nCopies(count, 0)), player.level().getGameTime(),
                new ArrayList<>(Collections.nCopies(count, CareerView.NO_POINT)), new CareerData.MissionStats(),
                origin);
        origin.ifPresent(spot -> {
            int r = ServerConfig.EXAM_PROTECT_RADIUS.get();
            if (r > 0) {
                ProtectedAreas.get(player.serverLevel()).add(ExamTargets.areaName(player.getUUID(), id),
                        spot.offset(-r, -r, -r), spot.offset(r, r, r));
            }
            player.sendSystemMessage(Component.translatable("kn8.mission.exam_area",
                    (int) Math.round(ServerConfig.EXAM_LEAVE_DISTANCE.get())).withStyle(ChatFormatting.GREEN));
        });
        CareerService.data(player).active().put(id, progress);
        player.sendSystemMessage(Component.translatable("kn8.mission.accepted", name(id))
                .withStyle(ChatFormatting.AQUA));
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.PLAYERS, 0.8F, 1.2F);
        def.grading().ifPresent(grading -> showRequirements(player, grading));
        activateObjective(player, id, def, progress, 0);
        CareerService.changed(player);
        return Result.OK;
    }

    public static boolean abandon(ServerPlayer player, ResourceLocation id) {
        CareerData.MissionProgress removed = CareerService.data(player).active().remove(id);
        if (removed == null) {
            return false;
        }
        endExam(player, id, removed);
        player.sendSystemMessage(Component.translatable("kn8.mission.abandoned", name(id))
                .withStyle(ChatFormatting.GRAY));
        CareerService.changed(player);
        return true;
    }

    // --- medidas para a avaliacao (PM-7) --------------------------------------------------------------------------

    /** Tiro de arma de fogo do jogador; {@code hit} = o raio pegou um kaiju. Conta na carreira e nas missoes. */
    public static void onShot(ServerPlayer player, boolean hit) {
        CareerService.data(player).addShot(hit);
        forEachActive(player, stats -> {
            stats.shots++;
            if (hit) {
                stats.hits++;
            }
        });
        CareerService.changed(player);
    }

    public static void onDamageDealt(ServerPlayer player, float amount) {
        forEachActive(player, stats -> stats.damageDealt += amount);
    }

    public static void onDamageTaken(ServerPlayer player, float amount) {
        forEachActive(player, stats -> stats.damageTaken += amount);
    }

    /** Soldado do esquadrao do jogador morreu. */
    public static void onAllyLost(ServerPlayer player) {
        forEachActive(player, stats -> stats.alliesLost++);
    }

    private static void forEachActive(ServerPlayer player, Consumer<CareerData.MissionStats> fn) {
        for (CareerData.MissionProgress progress : CareerService.data(player).active().values()) {
            fn.accept(progress.stats());
        }
    }

    /** Nota da missao com o que foi medido ate agora (requisitos da prova, se houver). */
    public static MissionGrader.Result evaluate(ServerPlayer player, MissionDef def,
            CareerData.MissionProgress progress) {
        CareerData.MissionStats s = progress.stats();
        long ticks = Math.max(0, player.level().getGameTime() - progress.acceptedTick());
        MissionGrader.Stats stats = new MissionGrader.Stats(s.shots, s.hits, s.damageDealt, s.damageTaken,
                player.getMaxHealth(), s.alliesLost, ticks);
        return MissionGrader.grade(stats, requirements(def), gradeParams());
    }

    static MissionGrader.Requirements requirements(MissionDef def) {
        return def.grading().map(g -> new MissionGrader.Requirements(g.minAccuracy().orElse(-1.0), g.minShots(),
                g.maxDamageTaken().orElse(-1.0), g.maxAlliesLost().orElse(-1), g.parTicks().orElse(-1),
                MissionGrader.Grade.valueOf(g.minGrade().toUpperCase()))).orElse(MissionGrader.Requirements.NONE);
    }

    public static MissionGrader.Params gradeParams() {
        return new MissionGrader.Params(ServerConfig.GRADE_S.get(), ServerConfig.GRADE_A.get(),
                ServerConfig.GRADE_B.get(), ServerConfig.GRADE_C.get(), ServerConfig.GRADE_ACCURACY_FULL.get(),
                ServerConfig.GRADE_DAMAGE_ZERO.get(), ServerConfig.GRADE_ALLY_LOST_PENALTY.get(),
                ServerConfig.GRADE_MIN_SHOTS.get());
    }

    // --- progresso -----------------------------------------------------------------------------------------------

    /** Um evento de jogo (abate, desmonte, chefe derrotado) conta para o objetivo ATUAL das missoes ativas. */
    public static void progress(ServerPlayer player, MissionDef.ObjectiveType type, ResourceLocation target) {
        progress(player, type, target, false);
    }

    /**
     * {@code fromExam}: evento de um kaiju de prova. Prova aceita com o oficial (com lugar) so conta os kaiju dela, e
     * os kaiju dela nao contam para as missoes de campo (a prova nao vira atalho nem farm).
     */
    private static void progress(ServerPlayer player, MissionDef.ObjectiveType type, ResourceLocation target,
            boolean fromExam) {
        CareerData data = CareerService.data(player);
        for (Map.Entry<ResourceLocation, CareerData.MissionProgress> entry : List.copyOf(data.active().entrySet())) {
            Optional<MissionDef> def = KN8Data.MISSION.get(entry.getKey(), false);
            // Defesa de invasao conta igual nos dois (a invasao da prova de Capitao nao marca os kaiju).
            if (def.isEmpty() || (type != MissionDef.ObjectiveType.DEFEND_INVASION
                    && entry.getValue().origin().isPresent() != fromExam)) {
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
            Optional<BlockPos> origin = entry.getValue().origin();
            if (origin.isPresent()
                    && horizontalDistance(player, origin.get()) > ServerConfig.EXAM_LEAVE_DISTANCE.get()) {
                fail(player, entry.getKey(), "left");
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
            setPoint(progress, index, patrolPoint(player, progress));
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
        // PM-8B: na prova tudo acontece perto de onde ela foi aceita (dentro da base); na missao, em campo.
        Optional<BlockPos> origin = progress.origin();
        BlockPos center = origin.orElse(player.blockPosition());
        double areaMin = origin.isPresent() ? ServerConfig.EXAM_AREA_MIN.get() : ServerConfig.MISSION_AREA_MIN.get();
        double areaMax = origin.isPresent() ? ServerConfig.EXAM_AREA_MAX.get() : ServerConfig.MISSION_AREA_MAX.get();
        BlockPos area = KaijuSpawner.surfaceAround(level, center, areaMin, areaMax, player.getRandom());
        double spread = origin.isPresent() ? 3 : 8;
        switch (objective.type()) {
            case KILL_KAIJU, DISMANTLE -> {
                objective.target().ifPresent(species -> {
                    for (int i = 0; i < objective.count() - progress.progress().get(index); i++) {
                        BlockPos spot = KaijuSpawner.surfaceAround(level, area, 0, spread, player.getRandom());
                        KaijuSpawner.spawn(level, species, spot).ifPresent(kaiju -> {
                            if (origin.isPresent()) {
                                ExamTargets.mark(kaiju, player.getUUID(), id);
                            }
                        });
                    }
                });
                setPoint(progress, index, area);
            }
            case DEFEAT_BOSS -> {
                objective.target().ifPresent(boss -> BossService.spawn(level, boss, area).ifPresent(kaiju -> {
                    if (origin.isPresent()) {
                        ExamTargets.mark(kaiju, player.getUUID(), id);
                    }
                }));
                setPoint(progress, index, area);
            }
            case REACH_AREA -> setPoint(progress, index, area);
            case DEFEND_INVASION -> {
                // Invasao centrada na area; se ja houver uma na dimensao, defender aquela tambem vale o objetivo.
                objective.target().ifPresent(invasion -> InvasionService.start(level, invasion, area));
                setPoint(progress, index, InvasionService.active(level).map(value -> value.center()).orElse(area));
            }
            case PATROL -> setPoint(progress, index, patrolPoint(player, progress));
        }
        player.sendSystemMessage(Component.translatable("kn8.mission.objective." + objective.type()
                .getSerializedName(), objective.target().map(MissionService::targetName)
                .orElse(Component.empty()), objective.count()).withStyle(ChatFormatting.YELLOW));
        CareerService.changed(player);
    }

    private static void complete(ServerPlayer player, ResourceLocation id, MissionDef def) {
        CareerData data = CareerService.data(player);
        CareerData.MissionProgress progress = data.active().remove(id);
        endExam(player, id, progress);
        MissionGrader.Result result = evaluate(player, def, progress);
        if (def.grading().isPresent() && !result.passed()) {
            failExam(player, id, def, progress, result);
            return;
        }
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
        // PM-8B: a prova (curso) fortalece o corpo.
        for (BodyStat stat : BodyStat.values()) {
            double xp = rewards.bodyXp().getOrDefault(stat.key(), 0.0);
            if (xp > 0) {
                PowerService.addBodyXp(player, stat, xp);
                player.sendSystemMessage(Component.translatable("kn8.mission.body_xp", Math.round(xp),
                        Component.translatable("kn8.body." + stat.key())).withStyle(ChatFormatting.AQUA));
            }
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
                CareerService.promote(player, rank);
            }
        });
        // PM-7: nota e resumo uma vez por conclusao (a missao ja saiu das ativas); merito extra so em S e A.
        int bonus = MissionGrader.meritBonus(rewards.merit(), result.grade(), ServerConfig.GRADE_MERIT_BONUS_S.get(),
                ServerConfig.GRADE_MERIT_BONUS_A.get());
        String best = data.bestGrades().get(id);
        if (best == null || result.grade().atLeast(MissionGrader.Grade.valueOf(best))) {
            data.bestGrades().put(id, result.grade().name());
        }
        sendEvaluation(player, id, def, progress, result, bonus);
        // O merito tambem tenta promover (a missao concluida pode ser a de avaliacao da proxima patente).
        CareerService.addMerit(player, rewards.merit() + bonus);
        CareerService.changed(player);
    }

    /** Prova reprovada: nao conclui, nao recompensa e so volta depois da espera. */
    private static void failExam(ServerPlayer player, ResourceLocation id, MissionDef def,
            CareerData.MissionProgress progress, MissionGrader.Result result) {
        int retry = def.grading().map(MissionDef.Grading::retryCooldownTicks).orElse(0);
        if (retry > 0) {
            CareerService.data(player).cooldowns().put(id, player.level().getGameTime() + retry);
        }
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("kn8.mission.eval.failed_title")
                .withStyle(ChatFormatting.RED)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(name(id)));
        sendEvaluation(player, id, def, progress, result, 0);
        player.sendSystemMessage(Component.translatable("kn8.mission.eval.retry", Math.max(1, retry / 1200))
                .withStyle(ChatFormatting.RED));
        player.level().playSound(null, player.blockPosition(), KN8Sounds.GUARD_BREAK.get(), SoundSource.PLAYERS,
                0.6F, 0.7F);
        CareerService.changed(player);
    }

    /** Resumo no chat: nota, medidas, linhas da prova e uma sugestao pela parte mais fraca. */
    private static void sendEvaluation(ServerPlayer player, ResourceLocation id, MissionDef def,
            CareerData.MissionProgress progress, MissionGrader.Result result, int bonus) {
        CareerData.MissionStats s = progress.stats();
        long ticks = Math.max(0, player.level().getGameTime() - progress.acceptedTick());
        player.sendSystemMessage(Component.translatable("kn8.mission.eval.header", name(id),
                Component.literal(result.grade().name()).withStyle(gradeColor(result.grade()), ChatFormatting.BOLD),
                result.score()).withStyle(ChatFormatting.GOLD));
        Component line = Component.translatable("kn8.mission.eval.stats", clock(ticks),
                Math.round(s.damageDealt), oneDecimal(s.damageTaken),
                Math.round(100 * s.damageTaken / Math.max(1.0F, player.getMaxHealth())), s.alliesLost);
        if (s.shots > 0) {
            line = line.copy().append(Component.translatable("kn8.mission.eval.accuracy",
                    Math.round(100.0 * s.hits / s.shots), s.hits, s.shots));
        }
        player.sendSystemMessage(line.copy().withStyle(ChatFormatting.GRAY));
        if (def.grading().isPresent()) {
            for (MissionGrader.Check check : result.checks()) {
                player.sendSystemMessage(Component.literal(check.ok() ? " \u2714 " : " \u2718 ")
                        .append(checkText(check.key(), check.limit()))
                        .withStyle(check.ok() ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            player.sendSystemMessage(Component.translatable(result.passed() ? "kn8.mission.eval.passed"
                    : "kn8.mission.eval.failed").withStyle(result.passed() ? ChatFormatting.GREEN : ChatFormatting.RED,
                    ChatFormatting.BOLD));
        }
        if (bonus > 0) {
            player.sendSystemMessage(Component.translatable("kn8.mission.eval.bonus", bonus)
                    .withStyle(ChatFormatting.YELLOW));
        }
        String weakest = result.weakest();
        if (!weakest.isEmpty()) {
            player.sendSystemMessage(Component.translatable("kn8.mission.eval.tip." + weakest)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** Requisitos da prova no chat ao aceitar (o menu mostra os mesmos). */
    private static void showRequirements(ServerPlayer player, MissionDef.Grading grading) {
        player.sendSystemMessage(Component.translatable("kn8.mission.exam_requirements")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        grading.minAccuracy().ifPresent(value -> player.sendSystemMessage(Component.literal(" - ")
                .append(Component.translatable("kn8.mission.eval.check.accuracy_shots", Math.round(value * 100),
                        grading.minShots())).withStyle(ChatFormatting.AQUA)));
        grading.maxDamageTaken().ifPresent(value -> player.sendSystemMessage(Component.literal(" - ")
                .append(checkText("damage", value)).withStyle(ChatFormatting.AQUA)));
        grading.maxAlliesLost().ifPresent(value -> player.sendSystemMessage(Component.literal(" - ")
                .append(checkText("allies", value)).withStyle(ChatFormatting.AQUA)));
        grading.parTicks().ifPresent(value -> player.sendSystemMessage(Component.literal(" - ")
                .append(Component.translatable("kn8.mission.eval.check.par", clock(value)))
                .withStyle(ChatFormatting.AQUA)));
        player.sendSystemMessage(Component.literal(" - ").append(checkText("grade",
                MissionGrader.Grade.valueOf(grading.minGrade().toUpperCase()).ordinal()))
                .withStyle(ChatFormatting.AQUA));
    }

    private static Component checkText(String key, double limit) {
        Object value = switch (key) {
            case "accuracy", "damage" -> Math.round(limit * 100);
            case "grade" -> MissionGrader.Grade.values()[(int) limit].name();
            default -> (int) limit;
        };
        return Component.translatable("kn8.mission.eval.check." + key, value);
    }

    private static ChatFormatting gradeColor(MissionGrader.Grade grade) {
        return switch (grade) {
            case S -> ChatFormatting.LIGHT_PURPLE;
            case A -> ChatFormatting.GREEN;
            case B -> ChatFormatting.AQUA;
            case C -> ChatFormatting.YELLOW;
            case D -> ChatFormatting.RED;
        };
    }

    static String clock(long ticks) {
        long seconds = ticks / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void fail(ServerPlayer player, ResourceLocation id, String reason) {
        CareerData.MissionProgress removed = CareerService.data(player).active().remove(id);
        if (removed != null) {
            endExam(player, id, removed);
        }
        player.sendSystemMessage(Component.translatable("kn8.mission.failed." + reason, name(id))
                .withStyle(ChatFormatting.RED));
        player.level().playSound(null, player.blockPosition(), KN8Sounds.GUARD_BREAK.get(), SoundSource.PLAYERS,
                0.6F, 0.7F);
        CareerService.changed(player);
    }

    // --- auxiliares ----------------------------------------------------------------------------------------------

    /** Fim da prova (qualquer motivo): somem os kaiju dela e a area volta a poder ser quebrada. */
    private static void endExam(ServerPlayer player, ResourceLocation id, CareerData.MissionProgress progress) {
        progress.origin().ifPresent(origin -> {
            ExamTargets.clear(player.serverLevel(), player.getUUID(), id, origin,
                    ServerConfig.EXAM_LEAVE_DISTANCE.get() + 32);
            ProtectedAreas.get(player.serverLevel()).remove(ExamTargets.areaName(player.getUUID(), id));
        });
    }

    /** Ponto de patrulha: em campo longe do jogador; na prova, perto de onde ela foi aceita. */
    private static BlockPos patrolPoint(ServerPlayer player, CareerData.MissionProgress progress) {
        Optional<BlockPos> origin = progress.origin();
        return KaijuSpawner.surfaceAround(player.serverLevel(), origin.orElse(player.blockPosition()),
                origin.isPresent() ? ServerConfig.EXAM_AREA_MIN.get() : ServerConfig.MISSION_PATROL_MIN.get(),
                origin.isPresent() ? ServerConfig.EXAM_AREA_MAX.get() : ServerConfig.MISSION_PATROL_MAX.get(),
                player.getRandom());
    }

    /** PM-8B: kaiju de prova abatido: conta so para a prova do dono (sem merito nem XP de abate). */
    public static void onExamKill(ServerPlayer owner, ResourceLocation species, Optional<ResourceLocation> boss) {
        progress(owner, MissionDef.ObjectiveType.KILL_KAIJU, species, true);
        boss.ifPresent(id -> progress(owner, MissionDef.ObjectiveType.DEFEAT_BOSS, id, true));
    }

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
