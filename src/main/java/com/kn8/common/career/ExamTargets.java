package com.kn8.common.career;

import java.util.List;
import java.util.UUID;

import com.kn8.common.arena.TrainingArenaService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Kaiju de prova (PM-8B): os que a prova faz surgir dentro da base. Marcados nos dados persistentes com o dono e a
 * missao: o abate conta so para a prova do dono (sem merito nem XP de abate, para a prova nao virar farm), nao deixam
 * carcaca e somem quando a prova acaba (concluida, reprovada ou abandonada).
 */
public final class ExamTargets {

    public static final String TAG = "kn8_exam";
    private static final String OWNER = "owner";
    private static final String MISSION = "mission";

    private ExamTargets() {
    }

    public static void mark(Entity entity, UUID owner, ResourceLocation mission) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(OWNER, owner);
        tag.putString(MISSION, mission.toString());
        entity.getPersistentData().put(TAG, tag);
    }

    /** EXAME: soldado que faz a prova junto com o jogador (marca com o dono e a missao, como os kaiju). */
    public static final String CANDIDATE_TAG = "kn8_exam_candidate";

    public static void markCandidate(Entity entity, UUID owner, ResourceLocation mission) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(OWNER, owner);
        tag.putString(MISSION, mission.toString());
        entity.getPersistentData().put(CANDIDATE_TAG, tag);
    }

    /** Dono da prova que o soldado faz junto (null = nao e candidato). */
    public static UUID candidateOwner(Entity entity) {
        CompoundTag tag = entity.getPersistentData().getCompound(CANDIDATE_TAG);
        return tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    /** Fim da prova: os candidatos que a fizeram junto vao embora. */
    public static void clearCandidates(ServerLevel level, UUID owner, ResourceLocation mission, BlockPos origin,
            double radius) {
        level.getEntitiesOfClass(SoldierEntity.class, new AABB(origin).inflate(radius),
                soldier -> owner.equals(candidateOwner(soldier)) && mission.toString().equals(
                        soldier.getPersistentData().getCompound(CANDIDATE_TAG).getString(MISSION)))
                .forEach(Entity::discard);
    }

    /** Passa a marca da prova de um kaiju para outro (invocados do chefe da prova). */
    public static void copy(Entity from, Entity to) {
        if (isExam(from)) {
            to.getPersistentData().put(TAG, from.getPersistentData().getCompound(TAG).copy());
        }
    }

    /** Tira a marca (testes). */
    public static void unmark(Entity entity) {
        entity.getPersistentData().remove(TAG);
    }

    public static boolean isExam(Entity entity) {
        return entity.getPersistentData().contains(TAG);
    }

    public static UUID owner(Entity entity) {
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        return tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    /** Some com os kaiju desta prova perto do lugar dela. */
    public static void clear(ServerLevel level, UUID owner, ResourceLocation mission, BlockPos origin,
            double radius) {
        List<KaijuEntity> list = level.getEntitiesOfClass(KaijuEntity.class, new AABB(origin).inflate(radius),
                kaiju -> isExam(kaiju) && owner.equals(owner(kaiju))
                        && mission.toString().equals(kaiju.getPersistentData().getCompound(TAG).getString(MISSION)));
        list.forEach(kaiju -> TrainingArenaService.vanish(level, kaiju));
    }

    public static String areaName(UUID owner, ResourceLocation mission) {
        return "exam_" + owner + "_" + mission.getPath();
    }
}
