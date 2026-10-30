package com.kn8.common.evacuation;

import java.util.EnumSet;
import java.util.UUID;

import javax.annotation.Nullable;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.host.KaijuFormService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Civil (PM-10A, Plano Mestre secao 14; Miguel 2026-10-23: "criar uma entidade civil; o modelo muda depois"). So
 * existe pelas missoes de evacuacao: surge uma vez ao aceitar (o mundo salva; nunca e criado de novo depois de
 * recarregar), marcado com o dono e a missao. Segue o jogador ou soldado mais perto, foge de kaiju por perto e, perto
 * do ponto de extracao, vai sozinho ate ele; no ponto e evacuado (conta para a missao e some). Kaiju o atacam como a um
 * soldado ({@code [evacuation] kaijuTargetCivilians}); jogadores e soldados nao o ferem. Modelo do jogador com uma
 * pele de roupa comum [SUPOSICAO provisorio].
 */
public class CivilianEntity extends PathfinderMob {

    public static final int VARIANTS = 4;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CivilianEntity.class,
            EntityDataSerializers.INT);
    private static final String TAG_OWNER = "kn8_evac_owner";
    private static final String TAG_MISSION = "kn8_evac_mission";
    private static final String TAG_EXTRACTION = "kn8_evac_point";
    private static final String TAG_VARIANT = "kn8_variant";
    /** Fallback do lado do cliente (o config de servidor so existe no servidor). */
    private static final float DEFAULT_PANIC = 8.0F;

    @Nullable
    private UUID owner;
    @Nullable
    private ResourceLocation mission;
    @Nullable
    private BlockPos extraction;

    public CivilianEntity(EntityType<? extends CivilianEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        float panic = ServerConfig.SPEC.isLoaded() ? ServerConfig.EVAC_PANIC_DISTANCE.get().floatValue()
                : DEFAULT_PANIC;
        goalSelector.addGoal(0, new FloatGoal(this));
        if (panic > 0) {
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, KaijuEntity.class, panic, 1.0, 1.4));
            // T-4: o civil nao sabe quem e o hospedeiro: foge do jogador transformado como de qualquer kaiju.
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, panic, 1.0, 1.4,
                    KaijuFormService::isTransformed));
        }
        goalSelector.addGoal(2, new EvacuateGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // --- dados da evacuacao --------------------------------------------------------------------------------------

    public void assign(UUID owner, ResourceLocation mission, BlockPos extraction) {
        this.owner = owner;
        this.mission = mission;
        this.extraction = extraction;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Nullable
    public ResourceLocation mission() {
        return mission;
    }

    @Nullable
    public BlockPos extraction() {
        return extraction;
    }

    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int value) {
        entityData.set(VARIANT, Math.floorMod(value, VARIANTS));
    }

    // --- vida ----------------------------------------------------------------------------------------------------

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        setVariant(getRandom().nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && isAlive()) {
            EvacuationService.civilianTick(this);
        }
    }

    /** Fogo amigo nao fere: o civil so corre perigo pelos kaiju (e pelo ambiente). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if ((source.getEntity() instanceof Player || source.getEntity() instanceof SoldierEntity)
                && !KaijuFormService.isRampaging(source.getEntity())) {
            // T-3: so o hospedeiro sem controle do corpo fere civis.
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        boolean wasAlive = !isRemoved() && !dead;
        super.die(source);
        if (wasAlive && level() instanceof ServerLevel) {
            EvacuationService.lost(this);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_VARIANT, variant());
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner);
        }
        if (mission != null) {
            tag.putString(TAG_MISSION, mission.toString());
        }
        if (extraction != null) {
            tag.put(TAG_EXTRACTION, NbtUtils.writeBlockPos(extraction));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt(TAG_VARIANT));
        owner = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        mission = tag.contains(TAG_MISSION) ? ResourceLocation.tryParse(tag.getString(TAG_MISSION)) : null;
        extraction = NbtUtils.readBlockPos(tag, TAG_EXTRACTION).orElse(null);
    }

    /**
     * Movimento: perto do ponto de extracao vai sozinho ate ele; longe, segue o jogador (fora do espectador) ou o
     * soldado mais perto dentro de {@code followRange}; sem ninguem perto, espera no lugar (nao sai andando sozinho
     * pela zona de perigo). Fugir de kaiju tem prioridade maior ({@link AvoidEntityGoal}).
     */
    static final class EvacuateGoal extends Goal {

        private static final int REPATH_TICKS = 10;
        private static final double STOP_DISTANCE = 2.5;

        private final CivilianEntity civilian;
        private int cooldown;

        EvacuateGoal(CivilianEntity civilian) {
            this.civilian = civilian;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return civilian.extraction != null && ServerConfig.SPEC.isLoaded();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (--cooldown > 0) {
                return;
            }
            cooldown = REPATH_TICKS;
            BlockPos point = civilian.extraction;
            Vec3 target = Vec3.atBottomCenterOf(point);
            if (horizontal(civilian.position(), target) <= ServerConfig.EVAC_SELF_WALK_RADIUS.get()) {
                civilian.getNavigation().moveTo(target.x, target.y, target.z, 1.1);
                return;
            }
            LivingEntity leader = leader();
            if (leader == null) {
                civilian.getNavigation().stop();
                return;
            }
            if (civilian.distanceTo(leader) > STOP_DISTANCE) {
                civilian.getNavigation().moveTo(leader, 1.15);
            } else {
                civilian.getNavigation().stop();
            }
        }

        @Nullable
        private LivingEntity leader() {
            double range = ServerConfig.EVAC_FOLLOW_RANGE.get();
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity entity : civilian.level().getEntitiesOfClass(LivingEntity.class,
                    civilian.getBoundingBox().inflate(range), e -> e.isAlive() && (e instanceof SoldierEntity
                            || e instanceof Player player && !player.isSpectator()))) {
                double distance = civilian.distanceToSqr(entity);
                if (distance <= range * range && distance < bestDistance) {
                    best = entity;
                    bestDistance = distance;
                }
            }
            return best;
        }

        private static double horizontal(Vec3 a, Vec3 b) {
            double dx = a.x - b.x;
            double dz = a.z - b.z;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }
}
