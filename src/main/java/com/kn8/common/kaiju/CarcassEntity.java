// src/main/java/com/kn8/common/kaiju/CarcassEntity.java
package com.kn8.common.kaiju;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.DismantleDef;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.core.dismantle.DismantleMath;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Carcaca de kaiju (M11b / 0.1-B). Nasce no lugar do kaiju quando ele morre, com o mesmo modelo (parado, sem IA) e a
 * hitbox da especie. O total de cada material da tabela de desmonte ({@code dismantle} do JSON da especie) e sorteado
 * na criacao, ja filtrado pelo estado do nucleo (destruido = fragmento; intacto = nucleo intacto), e entregue aos
 * poucos: segurar o clique direito com uma lamina (tag {@code kn8:dismantle_tools}) avanca {@code ticks_per_step};
 * cada etapa solta a sua parte ({@link DismantleMath#share}); na ultima a carcaca some. Some sozinha depois de
 * {@code carcass.despawnTicks}.
 */
public class CarcassEntity extends Entity implements GeoEntity {

    public static final TagKey<Item> DISMANTLE_TOOLS =
            TagKey.create(Registries.ITEM, KN8Constants.id("dismantle_tools"));

    private static final EntityDataAccessor<String> SPECIES =
            SynchedEntityData.defineId(CarcassEntity.class, EntityDataSerializers.STRING);
    /** Intervalo de repeticao do "usar" segurado no vanilla: cada interacao vale esses ticks de progresso. */
    private static final int USE_REPEAT_TICKS = 4;
    private static final double GRAVITY = 0.04;
    private static final double FRICTION = 0.6;
    private static final int PARTICLES_PER_STEP = 12;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean coreDestroyed;
    private final Map<ResourceLocation, Integer> totals = new LinkedHashMap<>();
    private int steps = 1;
    private int ticksPerStep = 1;
    private int step;
    private int progress;
    private int age;

    public CarcassEntity(EntityType<? extends CarcassEntity> type, Level level) {
        super(type, level);
    }

    /** Cria a carcaca de um kaiju que acabou de morrer (servidor). */
    public static Optional<CarcassEntity> from(KaijuEntity kaiju) {
        CarcassEntity carcass = KN8Entities.CARCASS.get().create(kaiju.level());
        if (carcass == null) {
            return Optional.empty();
        }
        carcass.setSpecies(kaiju.kaijuId());
        carcass.moveTo(kaiju.getX(), kaiju.getY(), kaiju.getZ(), kaiju.yBodyRot, 0.0F);
        carcass.coreDestroyed = kaiju.coreHealth() <= 0.0F;
        kaiju.def().flatMap(KaijuDef::dismantle).flatMap(id -> KN8Data.DISMANTLE.get(id, false))
                .ifPresent(table -> carcass.roll(table, kaiju.getRandom()));
        return Optional.of(carcass);
    }

    private void roll(DismantleDef table, RandomSource random) {
        steps = table.steps();
        ticksPerStep = table.ticksPerStep();
        for (DismantleDef.Drop drop : table.drops()) {
            boolean applies = switch (drop.condition()) {
                case ALWAYS -> true;
                case CORE_INTACT -> !coreDestroyed;
                case CORE_DESTROYED -> coreDestroyed;
            };
            if (applies) {
                int count = drop.minCount() + random.nextInt(drop.maxCount() - drop.minCount() + 1);
                totals.merge(drop.item(), count, Integer::sum);
            }
        }
    }

    public ResourceLocation species() {
        return ResourceLocation.parse(entityData.get(SPECIES));
    }

    private void setSpecies(ResourceLocation species) {
        entityData.set(SPECIES, species.toString());
        refreshDimensions();
    }

    public Optional<KaijuDef> def() {
        return KN8Data.KAIJU.get(species(), level().isClientSide());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPECIES, KN8Constants.id("primigenius").toString());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SPECIES.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return def().map(def -> EntityDimensions.scalable(def.dimensions().width(), def.dimensions().height()))
                .orElse(super.getDimensions(pose));
    }

    @Override
    public void tick() {
        super.tick();
        // Cai e assenta no chao (sem IA).
        setDeltaMovement(getDeltaMovement().add(0, -GRAVITY, 0).multiply(FRICTION, 1.0, FRICTION));
        move(MoverType.SELF, getDeltaMovement());
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
        if (!level().isClientSide() && ++age > ServerConfig.CARCASS_DESPAWN_TICKS.get()) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** Segurar o clique direito com uma lamina desmonta (o vanilla repete a interacao a cada 4 ticks). */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(DISMANTLE_TOOLS)) {
            if (!level().isClientSide()) {
                player.displayClientMessage(Component.translatable("kn8.carcass.need_tool"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        age = 0;
        progress += USE_REPEAT_TICKS;
        if (DismantleMath.stepComplete(progress, ticksPerStep)) {
            progress = 0;
            completeStep(player);
        }
        return InteractionResult.CONSUME;
    }

    private void completeStep(Player player) {
        step++;
        ServerLevel level = (ServerLevel) level();
        ItemStack particleItem = ItemStack.EMPTY;
        for (Map.Entry<ResourceLocation, Integer> entry : totals.entrySet()) {
            int amount = DismantleMath.share(entry.getValue(), step, steps);
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (amount > 0 && item != null) {
                ItemStack stack = new ItemStack(item, amount);
                particleItem = stack.copy();
                spawnAtLocation(stack, getBbHeight() * 0.5F);
            }
        }
        if (!particleItem.isEmpty()) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleItem), getX(),
                    getY() + getBbHeight() * 0.5, getZ(), PARTICLES_PER_STEP, getBbWidth() * 0.3, 0.3,
                    getBbWidth() * 0.3, 0.05);
        }
        level.playSound(null, blockPosition(), KN8Sounds.DISMANTLE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (step >= steps) {
            player.displayClientMessage(Component.translatable("kn8.carcass.done"), true);
            discard();
        } else {
            player.displayClientMessage(Component.translatable("kn8.carcass.progress", step, steps), true);
        }
    }

    // --- salvar ------------------------------------------------------------------------------------------------

    private static final String TAG_SPECIES = "species";
    private static final String TAG_CORE_DESTROYED = "core_destroyed";
    private static final String TAG_TOTALS = "totals";
    private static final String TAG_STEPS = "steps";
    private static final String TAG_TICKS = "ticks_per_step";
    private static final String TAG_STEP = "step";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_AGE = "age";

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_SPECIES)) {
            setSpecies(ResourceLocation.parse(tag.getString(TAG_SPECIES)));
        }
        coreDestroyed = tag.getBoolean(TAG_CORE_DESTROYED);
        CompoundTag totalsTag = tag.getCompound(TAG_TOTALS);
        totals.clear();
        for (String key : totalsTag.getAllKeys()) {
            totals.put(ResourceLocation.parse(key), totalsTag.getInt(key));
        }
        steps = Math.max(1, tag.getInt(TAG_STEPS));
        ticksPerStep = Math.max(1, tag.getInt(TAG_TICKS));
        step = tag.getInt(TAG_STEP);
        progress = tag.getInt(TAG_PROGRESS);
        age = tag.getInt(TAG_AGE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString(TAG_SPECIES, species().toString());
        tag.putBoolean(TAG_CORE_DESTROYED, coreDestroyed);
        CompoundTag totalsTag = new CompoundTag();
        totals.forEach((item, count) -> totalsTag.putInt(item.toString(), count));
        tag.put(TAG_TOTALS, totalsTag);
        tag.putInt(TAG_STEPS, steps);
        tag.putInt(TAG_TICKS, ticksPerStep);
        tag.putInt(TAG_STEP, step);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putInt(TAG_AGE, age);
    }

    // --- GeckoLib (sem animacao: carcaca parada) ---------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
