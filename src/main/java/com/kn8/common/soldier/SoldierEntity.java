// src/main/java/com/kn8/common/soldier/SoldierEntity.java
package com.kn8.common.soldier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.kn8.common.anim.Locomotion;
import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.MeleeRaycast;
import com.kn8.common.combat.WeaponIndex;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SoldierDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.combat.ActionTimeline;
import com.kn8.core.power.PowerMath;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Soldado da Forca de Defesa (0.1-B / Etapa F): uma entidade so, com variante (arma) e nivel de potencia vindos de
 * {@code data/kn8/kn8/soldier/soldier_1.json}. Aliado dos jogadores: caca kaiju, nunca ataca jogadores nem outros
 * soldados (o raycast tambem os ignora).
 *
 * <p>O nivel de potencia e uma % de Release do traje, com as MESMAS formulas do jogador (PowerMath): dano,
 * velocidade e reducao de dano. Combate no fluxo do jogador: a acao entra numa {@link ActionTimeline}, a animacao
 * GeckoLib toca, e o dano sai no tick de impacto do JSON da arma, por raycast (acerta partes de kaiju).</p>
 */
public class SoldierEntity extends PathfinderMob implements GeoEntity {

    public static final ResourceLocation DEFINITION = KN8Constants.id("soldier_1");
    public static final String DEFAULT_VARIANT = "rifle";
    public static final String DEFAULT_LEVEL = "normal";

    private static final EntityDataAccessor<String> VARIANT =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> LEVEL =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final String TAG_VARIANT = "kn8_variant";
    private static final String TAG_LEVEL = "kn8_power_level";
    /** Chaves que o vanilla grava em todo mob salvo (Mob / LivingEntity). */
    private static final String TAG_HAND_ITEMS = "HandItems";
    private static final String TAG_ATTRIBUTES = "attributes";
    private static final int DEFAULT_RELEASE = 10;
    private static final float SHOT_VOLUME = 1.0F;
    private static final float MELEE_VOLUME = 0.8F;
    private static final double MUZZLE_DISTANCE = 0.9;
    private static final int SWAP_COOLDOWN_TICKS = 20;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("soldier.movement.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("soldier.movement.walk");
    /** Poses dos bracos por classe de arma: rifle, pistola, lamina, sem arma. */
    public static final String POSE_RIFLE = "rifle";
    public static final String POSE_PISTOL = "pistol";
    public static final String POSE_BLADE = "blade";
    public static final String POSE_UNARMED = "unarmed";
    /** "soldier.arms.<pose>_<postura>": ready (parado), walk (andando), aim (com alvo). */
    private static final Map<String, RawAnimation> ARM_ANIMATIONS = new HashMap<>();
    private static final int MOVEMENT_TRANSITION_TICKS = 4;
    private static final int ARMS_TRANSITION_TICKS = 5;
    private static final int ACTION_TRANSITION_TICKS = 2;

    static {
        for (String pose : new String[] {POSE_RIFLE, POSE_PISTOL, POSE_BLADE, POSE_UNARMED}) {
            for (String stance : new String[] {"ready", "walk", "aim"}) {
                ARM_ANIMATIONS.put(pose + "_" + stance,
                        RawAnimation.begin().thenLoop("soldier.arms." + pose + "_" + stance));
            }
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionTimeline timeline = new ActionTimeline();
    private WeaponDef currentWeapon;
    private float currentMultiplier = 1.0F;
    private boolean currentUnarmed;
    private LivingEntity actionTarget;
    /** 0.4: variante ja escolhida (comando, invasao, NBT); senao o finalizeSpawn sorteia. */
    private boolean variantChosen;
    private int swapCooldown;

    public SoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    // --- dados -------------------------------------------------------------------------------------------------

    public Optional<SoldierDef> def() {
        return KN8Data.SOLDIER.get(DEFINITION, false);
    }

    public String variant() {
        return entityData.get(VARIANT);
    }

    public String powerLevel() {
        return entityData.get(LEVEL);
    }

    /** % de Release do traje pelo nivel de potencia (JSON). */
    public int release() {
        return def().map(def -> def.powerLevels().getOrDefault(powerLevel(), DEFAULT_RELEASE)).orElse(DEFAULT_RELEASE);
    }

    public double damageMultiplier() {
        return PowerMath.damageMultiplier(release(), PowerService.params());
    }

    /** Multiplicador do dano contra kaiju pelo nivel de potencia ({@code kaiju_damage} do JSON; padrao 1,0). */
    public float kaijuDamageMultiplier() {
        return def().map(def -> def.kaijuDamage().getOrDefault(powerLevel(), 1.0F)).orElse(1.0F);
    }

    /**
     * Variante = arma principal na mao e arma de apoio na outra mao (0.4; itens do JSON, {@code minecraft:air} = sem
     * arma). O atirador troca de mao quando o kaiju chega perto ({@link #updateLoadout}).
     */
    public void setVariant(String variant) {
        entityData.set(VARIANT, variant);
        variantChosen = true;
        Optional<SoldierDef.Variant> loadout = def().map(def -> def.variants().get(variant));
        setItemSlot(EquipmentSlot.MAINHAND, stack(loadout.map(SoldierDef.Variant::weapon)));
        setItemSlot(EquipmentSlot.OFFHAND, stack(loadout.flatMap(SoldierDef.Variant::sidearm)));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    private static ItemStack stack(Optional<ResourceLocation> id) {
        ItemStack stack = id.map(value -> new ItemStack(BuiltInRegistries.ITEM.get(value))).orElse(ItemStack.EMPTY);
        return stack.is(Items.AIR) ? ItemStack.EMPTY : stack;
    }

    /**
     * Arma de apoio (0.4): com uma arma de fogo na mao e o kaiju a menos de {@code sidearm_distance} blocos (borda da
     * hitbox), troca para a arma de apoio (faca); com o kaiju a mais do dobro disso, volta para a arma de fogo.
     * Espera {@link #SWAP_COOLDOWN_TICKS} entre trocas para nao ficar trocando a cada tick na beira da distancia.
     */
    void updateLoadout(double edge) {
        if (swapCooldown > 0 || getOffhandItem().isEmpty() || isActing()) {
            return;
        }
        float distance = def().map(SoldierDef::sidearmDistance).orElse(3.5F);
        boolean shooting = isShooter();
        boolean primaryIsGun = WeaponIndex.find(getOffhandItem(), false)
                .map(weapon -> weapon.style() == WeaponDef.Style.FIREARM).orElse(false);
        if ((shooting && edge < distance) || (!shooting && primaryIsGun && edge > distance * 2)) {
            swapHands();
        }
    }

    /** Sem alvo: volta para a arma principal (a de fogo, se ela estiver na outra mao). */
    void restorePrimary() {
        boolean primaryIsGun = WeaponIndex.find(getOffhandItem(), false)
                .map(weapon -> weapon.style() == WeaponDef.Style.FIREARM).orElse(false);
        if (!isShooter() && primaryIsGun) {
            swapHands();
        }
    }

    private void swapHands() {
        ItemStack main = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, getOffhandItem());
        setItemSlot(EquipmentSlot.OFFHAND, main);
        swapCooldown = SWAP_COOLDOWN_TICKS;
        level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.NEUTRAL, 0.6F,
                1.2F);
    }

    public void setPowerLevel(String level) {
        entityData.set(LEVEL, level);
        applyDefinition();
    }

    /** Vida, armadura, velocidade (com o bonus do Release) e alcance de visao do JSON. */
    protected void applyDefinition() {
        def().ifPresent(def -> {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(def.health());
            getAttribute(Attributes.ARMOR).setBaseValue(def.armor());
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.speed()
                    * (1.0 + PowerMath.speedBonus(release(), PowerService.params())));
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(def.followRange());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(def.unarmedDamage());
            setHealth(getMaxHealth());
        });
    }

    public Optional<WeaponDef> weapon() {
        return WeaponIndex.find(getMainHandItem(), level().isClientSide());
    }

    public boolean isShooter() {
        return weapon().map(weapon -> weapon.style() == WeaponDef.Style.FIREARM).orElse(false);
    }

    /** Alcance do ataque atual (arma ou soco). */
    public double attackReach() {
        return weapon().map(WeaponDef::reach).orElse(2.5F);
    }

    /**
     * Classe de pose dos bracos (cliente e servidor): vem da variante sincronizada, sem depender dos dados das
     * armas no cliente. Variante desconhecida: lamina se tiver item na mao, senao sem arma.
     */
    public String armPose() {
        // 0.4: pela arma que esta na mao (o atirador pode estar com a faca de apoio).
        ItemStack held = getMainHandItem();
        if (held.is(KN8Items.RIFLE.get())) {
            return POSE_RIFLE;
        }
        if (held.is(KN8Items.PISTOL.get())) {
            return POSE_PISTOL;
        }
        return held.isEmpty() ? POSE_UNARMED : POSE_BLADE;
    }

    public boolean isFirearmPose() {
        String pose = armPose();
        return POSE_RIFLE.equals(pose) || POSE_PISTOL.equals(pose);
    }

    public float keepDistance() {
        return def().map(SoldierDef::keepDistance).orElse(12.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, DEFAULT_VARIANT);
        builder.define(LEVEL, DEFAULT_LEVEL);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        // 0.4: sem variante pedida (ovo, /summon sem NBT, defensor "random"), sorteia pelo peso do JSON.
        setVariant(variantChosen ? variant() : def().map(def -> def.randomVariant(getRandom(), DEFAULT_VARIANT))
                .orElse(DEFAULT_VARIANT));
        setPowerLevel(powerLevel());
        return result;
    }

    // --- IA ----------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SoldierCombatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SoldierEntity.class, Player.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, KaijuEntity.class, true));
    }

    /** Aliado: nunca mira em jogadores nem em outros soldados. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Player) && !(target instanceof SoldierEntity) && super.canAttack(target);
    }

    /** Release do traje reduz o dano recebido (mesma formula do jogador). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player || source.getEntity() instanceof SoldierEntity) {
            // Fogo amigo nao fere (o raycast ja evita, isto cobre o resto).
            return false;
        }
        double reduction = PowerMath.damageReduction(release(), PowerService.params());
        boolean hurt = super.hurt(source, (float) (amount * (1.0 - reduction)));
        if (hurt && !level().isClientSide()) {
            triggerAnim("reaction", "hurt");
        }
        return hurt;
    }

    // --- combate -----------------------------------------------------------------------------------------------

    public boolean isActing() {
        return timeline.isActive(level().getGameTime());
    }

    /** Comeca um golpe/tiro contra o alvo (servidor); o dano sai no tick de impacto. */
    public boolean startAttack(LivingEntity target) {
        long now = level().getGameTime();
        if (isActing()) {
            return false;
        }
        Optional<WeaponDef> weapon = weapon();
        int duration;
        int impact;
        if (weapon.isPresent() && weapon.get().actions().containsKey("light")) {
            WeaponDef.Action action = weapon.get().actions().get("light");
            duration = action.durationTicks();
            impact = action.impactTick();
            currentMultiplier = action.multiplier();
            currentWeapon = weapon.get();
            currentUnarmed = false;
        } else {
            duration = def().map(SoldierDef::unarmedIntervalTicks).orElse(12);
            impact = duration / 2;
            currentMultiplier = 1.0F;
            currentWeapon = null;
            currentUnarmed = true;
        }
        if (!timeline.tryStart(now, "attack", duration, Math.max(0, impact))) {
            return false;
        }
        actionTarget = target;
        if (currentWeapon != null && currentWeapon.style() != WeaponDef.Style.FIREARM) {
            // 0.2: o soldado usa os mesmos sons de arma do jogador (JSON da arma).
            level().playSound(null, getX(), getEyeY(), getZ(),
                    CombatService.weaponSound(currentWeapon, "swing", KN8Sounds.BLADE_SWING.get()),
                    SoundSource.HOSTILE, MELEE_VOLUME, 1.0F);
        }
        triggerAnim("action", isShooter() ? (POSE_PISTOL.equals(armPose()) ? "shoot_pistol" : "shoot_rifle")
                : "attack");
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (swapCooldown > 0) {
            swapCooldown--;
        }
        if (!level().isClientSide() && timeline.consumeImpact(level().getGameTime())) {
            resolveImpact();
        }
    }

    private void resolveImpact() {
        LivingEntity target = actionTarget;
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 eye = getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(eye);
        double reach = currentUnarmed ? 2.5 : currentWeapon.reach();
        boolean firearm = currentWeapon != null && currentWeapon.style() == WeaponDef.Style.FIREARM;
        ServerLevel level = (ServerLevel) level();
        if (firearm) {
            level.playSound(null, getX(), getEyeY(), getZ(), CombatService.shotSound(currentWeapon),
                    SoundSource.HOSTILE, SHOT_VOLUME, 1.0F);
            VfxService.play(level, VfxService.WEAPON_FIRE, eye.add(aim.normalize().scale(MUZZLE_DISTANCE)),
                    aim.normalize(), 1.0F, 0.0F);
        }
        Optional<Entity> hit = MeleeRaycast.findTarget(level, this, eye, aim, reach,
                entity -> !(rootOf(entity) instanceof Player) && !(rootOf(entity) instanceof SoldierEntity));
        if (hit.isEmpty()) {
            return;
        }
        float base = currentUnarmed ? (float) getAttributeValue(Attributes.ATTACK_DAMAGE) : currentWeapon.baseDamage();
        float damage = (float) (base * currentMultiplier * damageMultiplier());
        Entity struck = hit.get();
        if (rootOf(struck) instanceof KaijuEntity kaiju) {
            damage *= kaijuDamageMultiplier();
            // [SUPOSICAO] 0.3: o soldado acerta o corpo (multiplicador 1, sem nucleo). Mirando no centro do kaiju ele
            // pegava a parte do nucleo (x3, nucleo zerado mata) e derrubava uma aranha em 2 tiros; mirar no nucleo e
            // nos pontos fracos fica sendo habilidade do jogador.
            struck = kaiju;
        }
        if (struck.hurt(damageSources().mobAttack(this), damage)) {
            VfxService.play(level, firearm ? VfxService.IMPACT : VfxService.SLASH, hit.get().getBoundingBox()
                    .getCenter(), aim.normalize(), 0.8F, 0.0F);
            if (!firearm && currentWeapon != null) {
                level.playSound(null, hit.get().getX(), hit.get().getY(), hit.get().getZ(),
                        CombatService.weaponSound(currentWeapon, "hit", SoundEvents.PLAYER_ATTACK_SWEEP),
                        SoundSource.HOSTILE, MELEE_VOLUME, 1.0F);
            }
        }
    }

    private static Entity rootOf(Entity entity) {
        return entity instanceof PartEntity<?> part ? part.getParent() : entity;
    }

    // --- salvar ------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_VARIANT, variant());
        tag.putString(TAG_LEVEL, powerLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_VARIANT)) {
            entityData.set(VARIANT, tag.getString(TAG_VARIANT));
            variantChosen = true;
        }
        if (tag.contains(TAG_LEVEL)) {
            entityData.set(LEVEL, tag.getString(TAG_LEVEL));
        }
        // "/summon kn8:soldier ~ ~ ~ {kn8_variant:...}" nao chama finalizeSpawn: sem itens/atributos salvos (soldado
        // novo, nao carregado do mundo), aplica aqui a arma da variante e os atributos do nivel.
        if (!level().isClientSide()) {
            if (!tag.contains(TAG_HAND_ITEMS)) {
                setVariant(variant());
            }
            if (!tag.contains(TAG_ATTRIBUTES)) {
                applyDefinition();
            }
        }
    }

    // --- GeckoLib ----------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Cada controller mexe em ossos/canais diferentes (pernas e tronco x bracos), entao andar + mirar se somam;
        // "action" e "reaction" vem depois e passam por cima enquanto tocam.
        controllers.add(Locomotion.drive(new AnimationController<>(this, "movement", MOVEMENT_TRANSITION_TICKS, state ->
                state.setAndContinue(state.isMoving() ? WALK : IDLE)), Locomotion::typeId));
        controllers.add(new AnimationController<>(this, "arms", ARMS_TRANSITION_TICKS, state -> {
            // Mob#isAggressive e sincronizado (flags do Mob); o SoldierCombatGoal liga enquanto tem alvo.
            String stance = isAggressive() ? "aim" : state.isMoving() ? "walk" : "ready";
            return state.setAndContinue(ARM_ANIMATIONS.get(armPose() + "_" + stance));
        }));
        controllers.add(new AnimationController<>(this, "action", ACTION_TRANSITION_TICKS, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("soldier.action.attack"))
                .triggerableAnim("shoot_rifle", RawAnimation.begin().thenPlay("soldier.action.shoot_rifle"))
                .triggerableAnim("shoot_pistol", RawAnimation.begin().thenPlay("soldier.action.shoot_pistol")));
        controllers.add(new AnimationController<>(this, "reaction", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("soldier.reaction.hurt")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
