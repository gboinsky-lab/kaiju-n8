// src/main/java/com/kn8/common/soldier/special/HoshinaEntity.java
package com.kn8.common.soldier.special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.SoldierDef;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.power.PowerMath;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Hoshina (0.6-D), primeiro soldado especial: espadachim muito rapido com duas espadas (texto do Miguel,
 * docs/ESPECIFICACAO_HABILIDADES_MOBS.md secao 2). Aliado como os soldados comuns (e um {@link SoldierEntity}: nao
 * fere jogadores nem soldados, kaiju o cacam, o menu o mostra), mas com perfil proprio em
 * {@code special_soldier/hoshina.json}: atributos, Release, aura e tecnicas.
 *
 * <p>Tudo no servidor (regra 1): a tecnica entra numa linha do tempo propria; cada golpe sai no tick do JSON
 * ({@code windup_ticks} + {@code hit_interval}), nunca pela animacao. Reacoes, por ordem: Kaeshi-uchi contra golpe
 * "heavy" de kaiju prestes a acertar, esquiva contra os outros, parry (chance) quando o golpe chega mesmo assim.</p>
 *
 * <p>Aura: id publico {@code kn8:aura} (sync nativo) e a % de Release em {@code kn8:release_visual}, so na mudanca;
 * com a vida baixa a % sobe como no jogador (desespero), e a aura cresce junto.</p>
 */
public class HoshinaEntity extends SoldierEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("hoshina");
    public static final String VARIANT = "hoshina";
    /** Tecnicas com animacao propria (hoshina.action.<id>); outras do JSON tocam o golpe basico. */
    public static final List<String> ANIMATED = List.of("kuuchi", "kosa_uchi", "ran_uchi", "kasumi_uchi", "yae_uchi",
            "kaeshi_uchi", "dash", "parry");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("hoshina.movement.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("hoshina.movement.walk");
    private static final Map<String, RawAnimation> ARMS = new HashMap<>();
    private static final float REACH_SLACK = 1.0F;
    /** Kaeshi-uchi: o contra-golpe vem com um avanco (o dash lateral pode deixa-lo um pouco longe). */
    private static final float COUNTER_REACH_SLACK = 3.0F;
    private static final double SIDESTEP_SPEED = 0.6;
    private static final double DASH_LIFT = 0.15;
    private static final double REACTION_SEARCH = 16.0;
    private static final float SWING_VOLUME = 0.8F;
    private static final float TICKS_PER_SECOND = 20.0F;

    static {
        for (String stance : new String[] {"ready", "walk", "aim"}) {
            ARMS.put(stance, RawAnimation.begin().thenLoop("hoshina.arms.blade_" + stance));
        }
    }

    private final Map<String, Long> readyAt = new HashMap<>();
    private String techniqueId;
    private SpecialSoldierDef.Technique technique;
    private long techniqueStart;
    private int nextHit;
    private LivingEntity techniqueTarget;
    private long dashReadyAt;
    private long counterReadyAt;
    private long parryReadyAt;
    private long invulnerableUntil;
    private long counterWindowUntil;
    private long counterStrikeAt = -1;
    private LivingEntity counterTarget;
    private int lastVisualRelease = -1;
    private boolean profileChecked;
    /** Pontos de Release ganhos na luta (escalada de combate); nao salvo: recomeca a cada luta. */
    private float escalation;
    /** Contadores para os GameTests (quantas vezes cada reacao saiu). */
    private int dodges;
    private int counters;
    private int parries;

    public HoshinaEntity(EntityType<? extends HoshinaEntity> type, Level level) {
        super(type, level);
    }

    // --- perfil ------------------------------------------------------------------------------------------------

    public Optional<SpecialSoldierDef> profile() {
        return KN8Data.SPECIAL_SOLDIER.get(PROFILE, false);
    }

    /** Nao usa o soldier_1.json (variantes e niveis de soldado comum). */
    @Override
    public Optional<SoldierDef> def() {
        return Optional.empty();
    }

    /** Release base do perfil + escalada de combate + desespero com a vida baixa (mesma regra do jogador). */
    @Override
    public int release() {
        int base = profile().map(SpecialSoldierDef::release).orElse(0) + (int) escalation;
        int max = profile().map(SpecialSoldierDef::maxRelease).orElse(100);
        if (!ServerConfig.SPEC.isLoaded()) {
            return Math.min(max, base);
        }
        int desperation = PowerMath.desperationBonus(getHealth() / Math.max(1.0F, getMaxHealth()),
                ServerConfig.DESPERATION_HEALTH.get(), ServerConfig.DESPERATION_MAX_POINTS.get());
        return Math.min(max, base + desperation);
    }

    @Override
    public float kaijuDamageMultiplier() {
        return profile().map(SpecialSoldierDef::kaijuDamage).orElse(1.0F);
    }

    /** As duas espadas do perfil; a variante sincronizada so da o nome no menu. */
    @Override
    public void setVariant(String variant) {
        super.setVariant(VARIANT);
        profile().ifPresent(def -> {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BuiltInRegistries.ITEM.get(def.weapon())));
            setItemSlot(EquipmentSlot.OFFHAND, def.offhand()
                    .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id))).orElse(ItemStack.EMPTY));
        });
    }

    @Override
    protected void applyDefinition() {
        profile().ifPresent(def -> {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(def.health());
            getAttribute(Attributes.ARMOR).setBaseValue(def.armor());
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.speed()
                    * (1.0 + PowerMath.speedBonus(def.release(), PowerService.params())));
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(def.followRange());
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(def.knockbackResistance());
            setHealth(getMaxHealth());
        });
    }

    /**
     * Sem finalizeSpawn (GameTest, comando sem NBT de itens): no primeiro tick arma as espadas e aplica os atributos
     * do perfil, se ainda nao estiverem (carregado do mundo, ja estao: a vida salva fica).
     */
    private void ensureProfile() {
        profile().ifPresent(def -> {
            if (getMainHandItem().isEmpty()) {
                setVariant(VARIANT);
            }
            if (getAttribute(Attributes.MAX_HEALTH).getBaseValue() != def.health()) {
                applyDefinition();
            }
        });
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setVariant(VARIANT);
        applyDefinition();
        return result;
    }

    // --- IA ----------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoshinaCombatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SoldierEntity.class, Player.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, KaijuEntity.class, true));
    }

    public boolean isUsingTechnique() {
        return technique != null;
    }

    public Optional<String> currentTechnique() {
        return Optional.ofNullable(techniqueId);
    }

    public boolean isInvulnerableNow() {
        return level().getGameTime() < invulnerableUntil;
    }

    public int dodges() {
        return dodges;
    }

    public int counters() {
        return counters;
    }

    public int parries() {
        return parries;
    }

    /** Distancia da borda da hitbox do alvo ate os olhos (a mesma do goal dos soldados). */
    public double edgeTo(LivingEntity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(getEyePosition()));
    }

    /**
     * Escolhe a tecnica: entre as prontas e com o alvo na faixa de alcance, a de maior prioridade (mais
     * {@code honju_priority} contra Honju/numerados/Daikaiju); empate sorteado. Cortes a distancia precisam de linha de
     * visao.
     */
    public Optional<String> chooseTechnique(LivingEntity target) {
        Optional<SpecialSoldierDef> def = profile();
        if (def.isEmpty()) {
            return Optional.empty();
        }
        long now = level().getGameTime();
        double edge = edgeTo(target);
        boolean big = target instanceof KaijuEntity kaiju
                && kaiju.def().map(kd -> kd.kaijuClass() != KaijuClass.YOJU).orElse(false);
        boolean sees = getSensing().hasLineOfSight(target);
        int best = Integer.MIN_VALUE;
        List<String> tied = new ArrayList<>();
        for (Map.Entry<String, SpecialSoldierDef.Technique> entry : def.get().techniques().entrySet()) {
            SpecialSoldierDef.Technique candidate = entry.getValue();
            if (now < readyAt.getOrDefault(entry.getKey(), 0L) || edge < candidate.minRange()
                    || edge > candidate.maxRange()
                    || (candidate.type() == SpecialSoldierDef.TechniqueType.SLASH && !sees)) {
                continue;
            }
            int priority = candidate.priority() + (big ? candidate.honjuPriority() : 0);
            if (priority > best) {
                best = priority;
                tied.clear();
            }
            if (priority == best) {
                tied.add(entry.getKey());
            }
        }
        tied.sort(String::compareTo);
        return tied.isEmpty() ? Optional.empty() : Optional.of(tied.get(getRandom().nextInt(tied.size())));
    }

    /** Comeca uma tecnica do perfil contra o alvo (goal, comando, testes). */
    public boolean startTechnique(String id, LivingEntity target) {
        Optional<SpecialSoldierDef.Technique> found = profile().map(def -> def.techniques().get(id));
        if (found.isEmpty() || isUsingTechnique() || isActing() || weapon().isEmpty()) {
            return false;
        }
        long now = level().getGameTime();
        technique = found.get();
        techniqueId = id;
        techniqueStart = now;
        techniqueTarget = target;
        nextHit = 0;
        readyAt.put(id, now + technique.durationTicks() + scaledCooldown(technique.cooldownTicks()));
        getNavigation().stop();
        lookAt(target, 360.0F, 90.0F);
        if (technique.dashIn() > 0) {
            dash(target.position().subtract(position()), technique.dashIn());
        }
        triggerAnim("action", ANIMATED.contains(id) ? id : "attack");
        level().playSound(null, getX(), getEyeY(), getZ(), KN8Sounds.SWORD_SWING.get(), SoundSource.NEUTRAL,
                SWING_VOLUME, 1.2F);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        long now = level().getGameTime();
        if (!profileChecked) {
            profileChecked = true;
            ensureProfile();
        }
        tickEscalation();
        updateAura();
        if (counterStrikeAt >= 0 && now >= counterStrikeAt) {
            counterStrikeAt = -1;
            counterStrike();
        }
        if (technique != null) {
            tickTechnique(now);
        }
        if (isAlive()) {
            react(now);
        }
    }

    private void tickTechnique(long now) {
        LivingEntity target = techniqueTarget;
        int elapsed = (int) (now - techniqueStart);
        if (target != null && target.isAlive()) {
            getLookControl().setLookAt(target, 60.0F, 60.0F);
            int count = technique.type() == SpecialSoldierDef.TechniqueType.SLASH ? 1 : technique.hits().size();
            while (nextHit < count && elapsed >= technique.hitTick(nextHit)) {
                fireHit(target, nextHit, count);
                nextHit++;
            }
        }
        if (elapsed >= technique.durationTicks()) {
            technique = null;
            techniqueId = null;
            techniqueTarget = null;
        }
    }

    private void fireHit(LivingEntity target, int index, int count) {
        Optional<WeaponDef> weapon = weapon();
        if (weapon.isEmpty()) {
            return;
        }
        float base = (float) (weapon.get().baseDamage() * damageMultiplier());
        if (technique.type() == SpecialSoldierDef.TechniqueType.SLASH) {
            lookAt(target, 360.0F, 90.0F);
            Vec3 aim = target.getBoundingBox().getCenter().subtract(getEyePosition());
            SlashProjectile.fire(this, aim, technique.slash(), base * technique.hits().get(0), kaijuDamageMultiplier());
            level().playSound(null, getX(), getEyeY(), getZ(), KN8Sounds.SWORD_HEAVY.get(), SoundSource.NEUTRAL,
                    SWING_VOLUME, 1.1F);
            return;
        }
        boolean last = index == count - 1;
        strike(target, technique.hits().get(index), last ? technique.finalKnockback() : 0.0F, weapon.get(),
                REACH_SLACK);
        if (last && technique.exposeCoreTicks() > 0 && target instanceof KaijuEntity kaiju) {
            // Yae-uchi: abre a guarda do kaiju (nucleo exposto) para os aliados aproveitarem.
            kaiju.exposeCore(technique.exposeCoreTicks());
        }
        if (technique.sidestepBeforeLast() && index == count - 2) {
            // Kasumi-uchi: o segundo corte distrai; um passo para o lado e o terceiro vem forte.
            Vec3 toTarget = target.position().subtract(position());
            Vec3 side = new Vec3(-toTarget.z, 0, toTarget.x);
            dash(getRandom().nextBoolean() ? side : side.scale(-1), SIDESTEP_SPEED);
        }
    }

    /**
     * Golpe corpo a corpo de tecnica: acerta se a borda do alvo estiver ao alcance da arma (com folga: os combos
     * avancam junto). Kaiju levam no corpo, com o {@code kaiju_damage} do perfil.
     */
    private boolean strike(LivingEntity target, float multiplier, float knockback, WeaponDef weapon, float slack) {
        if (edgeTo(target) > weapon.reach() + slack) {
            return false;
        }
        float damage = (float) (weapon.baseDamage() * multiplier * damageMultiplier());
        if (target instanceof KaijuEntity) {
            damage *= kaijuDamageMultiplier();
        }
        // Combos de 1-2 ticks entre golpes: a invulnerabilidade vanilla (10 ticks) engolia quase todos (como no
        // multi_hit dos kaiju, 0.6-A); a tecnica ja acerta uma vez por golpe do JSON.
        target.invulnerableTime = 0;
        if (!target.hurt(damageSources().mobAttack(this), damage)) {
            return false;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(getEyePosition()).normalize();
        VfxService.play(level, VfxService.SLASH, target.getBoundingBox().getCenter(), aim, 0.8F, 0.0F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                CombatService.weaponSound(weapon, "hit", SoundEvents.PLAYER_ATTACK_SWEEP), SoundSource.NEUTRAL,
                SWING_VOLUME, 1.0F + getRandom().nextFloat() * 0.2F);
        if (knockback > 0) {
            target.knockback(knockback, -aim.x, -aim.z);
        }
        return true;
    }

    /** Impulso numa direcao horizontal (dash, esquiva, passo lateral). */
    private void dash(Vec3 direction, double speed) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }
        setDeltaMovement(flat.normalize().scale(speed).add(0, DASH_LIFT, 0));
        hasImpulse = true;
    }

    // --- reacoes -----------------------------------------------------------------------------------------------

    /**
     * Le os kaiju por perto: golpe "heavy" vindo nele (ou com ele na area) em ate {@code react_ticks} = Kaeshi-uchi;
     * outro golpe = esquiva. A janela do parry tambem dispara o Kaeshi-uchi.
     */
    private void react(long now) {
        Optional<SpecialSoldierDef> def = profile();
        if (def.isEmpty()) {
            return;
        }
        SpecialSoldierDef.Counter counter = def.get().counter();
        SpecialSoldierDef.Dash dashDef = def.get().dash();
        if (now < counterWindowUntil && now >= counterReadyAt && getTarget() != null && getTarget().isAlive()) {
            counterWindowUntil = 0;
            startCounter(getTarget(), counter, now);
            return;
        }
        AABB area = getBoundingBox().inflate(REACTION_SEARCH);
        for (KaijuEntity kaiju : level().getEntitiesOfClass(KaijuEntity.class, area, KaijuEntity::isAlive)) {
            if (!threatens(kaiju)) {
                continue;
            }
            int ticks = kaiju.ticksToImpact().orElse(Integer.MAX_VALUE);
            if (kaiju.isPreparingHeavy() && ticks <= counter.reactTicks() && now >= counterReadyAt) {
                startCounter(kaiju, counter, now);
                return;
            }
            if (ticks <= dashDef.reactTicks() && now >= dashReadyAt) {
                dodge(kaiju, dashDef, now);
                return;
            }
        }
    }

    /** O golpe em preparo e contra ele: alvo da habilidade, ou ele perto o bastante do kaiju para ser pego. */
    private boolean threatens(KaijuEntity kaiju) {
        if (kaiju.abilityTarget().map(target -> target == this).orElse(false)) {
            return true;
        }
        return kaiju.isUsingAbility() && kaiju.edgeDistance(this) <= kaiju.meleeReach() + 2.0;
    }

    private void dodge(KaijuEntity kaiju, SpecialSoldierDef.Dash def, long now) {
        Vec3 away = position().subtract(kaiju.position());
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(getRandom().nextBoolean() ? 1 : -1);
        dash(side.normalize().add(new Vec3(away.x, 0, away.z).normalize().scale(0.6)), def.speed());
        dashReadyAt = now + scaledCooldown(def.cooldownTicks());
        invulnerableUntil = now + def.invulnerableTicks();
        dodges++;
        triggerAnim("action", "dash");
        level().playSound(null, getX(), getY(), getZ(), KN8Sounds.DASH.get(), SoundSource.NEUTRAL, 0.7F, 1.2F);
    }

    /** Fecha distancia com um dash para a frente (goal: alvo longe e dash pronto). */
    public boolean gapClose(LivingEntity target) {
        Optional<SpecialSoldierDef.Dash> def = profile().map(SpecialSoldierDef::dash);
        long now = level().getGameTime();
        if (def.isEmpty() || now < dashReadyAt || isUsingTechnique() || edgeTo(target) < def.get().gapCloseDistance()
                || !onGround()) {
            return false;
        }
        dash(target.position().subtract(position()), def.get().speed());
        dashReadyAt = now + scaledCooldown(def.get().cooldownTicks());
        triggerAnim("action", "dash");
        level().playSound(null, getX(), getY(), getZ(), KN8Sounds.DASH.get(), SoundSource.NEUTRAL, 0.7F, 1.2F);
        return true;
    }

    /** Kaeshi-uchi: dash lateral passando pelo inimigo, invulneravel, e o contra-ataque sai depois. */
    private void startCounter(LivingEntity enemy, SpecialSoldierDef.Counter def, long now) {
        technique = null;
        techniqueId = null;
        Vec3 toEnemy = enemy.position().subtract(position());
        Vec3 side = new Vec3(-toEnemy.z, 0, toEnemy.x).normalize().scale(getRandom().nextBoolean() ? 1 : -1);
        dash(side.add(new Vec3(toEnemy.x, 0, toEnemy.z).normalize().scale(0.8)), def.dashSpeed());
        counterReadyAt = now + def.cooldownTicks();
        invulnerableUntil = now + def.invulnerableTicks();
        counterStrikeAt = now + def.strikeDelayTicks();
        counterTarget = enemy;
        counters++;
        triggerAnim("action", "kaeshi_uchi");
        level().playSound(null, getX(), getY(), getZ(), KN8Sounds.DASH.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    private void counterStrike() {
        LivingEntity target = counterTarget;
        counterTarget = null;
        Optional<WeaponDef> weapon = weapon();
        if (target == null || !target.isAlive() || weapon.isEmpty()) {
            return;
        }
        lookAt(target, 360.0F, 90.0F);
        float multiplier = profile().map(def -> def.counter().multiplier()).orElse(1.0F);
        float lunge = profile().map(def -> def.counter().dashSpeed()).orElse(1.0F);
        dash(target.position().subtract(position()), lunge);
        strike(target, multiplier, 0.0F, weapon.get(), COUNTER_REACH_SLACK);
    }

    /**
     * Invulneravel durante esquiva/Kaeshi-uchi; golpe corpo a corpo de kaiju (nao "heavy") pode ser aparado (parry).
     * O resto segue o soldado (fogo amigo nao fere, Release reduz o dano).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) {
            return super.hurt(source, amount);
        }
        long now = level().getGameTime();
        if (now < invulnerableUntil && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        float dealt = amount;
        Optional<SpecialSoldierDef.Parry> parry = profile().map(SpecialSoldierDef::parry);
        if (parry.isPresent() && source.getEntity() instanceof KaijuEntity kaiju
                && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION)
                && !kaiju.isDealingHeavyHit() && now >= parryReadyAt
                && getRandom().nextFloat() < parry.get().chance()) {
            dealt = amount * parry.get().damageFactor();
            parryReadyAt = now + parry.get().cooldownTicks();
            counterWindowUntil = now + parry.get().counterWindowTicks();
            parries++;
            triggerAnim("action", "parry");
            ServerLevel level = (ServerLevel) level();
            Vec3 at = getEyePosition().add(getLookAngle().scale(0.6));
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.3);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.2);
            level.playSound(null, getX(), getEyeY(), getZ(), KN8Sounds.PARRY.get(), SoundSource.NEUTRAL, 1.0F,
                    1.1F);
        }
        return super.hurt(source, dealt);
    }

    // --- aura --------------------------------------------------------------------------------------------------

    /** Escalada de combate: sobe com alvo vivo, cai sem alvo (pontos por segundo do JSON). */
    private void tickEscalation() {
        profile().map(SpecialSoldierDef::escalation).ifPresent(def -> {
            boolean fighting = getTarget() != null && getTarget().isAlive();
            escalation = fighting ? Math.min(def.maxPoints(), escalation + def.pointsPerSecond() / TICKS_PER_SECOND)
                    : Math.max(0.0F, escalation - def.decayPerSecond() / TICKS_PER_SECOND);
        });
    }

    public float escalation() {
        return escalation;
    }

    /** Recarga encurtada pela escalada (ate cooldown_reduction_at_max na escalada maxima). */
    private long scaledCooldown(int ticks) {
        return profile().map(SpecialSoldierDef::escalation).filter(def -> def.maxPoints() > 0)
                .map(def -> Math.round(ticks * (1.0 - def.cooldownReductionAtMax() * escalation / def.maxPoints())))
                .orElse((long) ticks);
    }

    /** Velocidade do perfil com o bonus do Release atual (o soldado comum so aplica no nascimento). */
    private void applySpeed(int release) {
        profile().ifPresent(def -> getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.speed()
                * (1.0 + PowerMath.speedBonus(release, PowerService.params()))));
    }

    /** Publica a aura do perfil e a % de Release (so na mudanca: sync nativo dos attachments). */
    private void updateAura() {
        profile().ifPresent(def -> {
            if (!def.aura().equals(getData(KN8Attachments.AURA))) {
                setData(KN8Attachments.AURA, def.aura());
            }
        });
        int release = release();
        if (release != lastVisualRelease) {
            lastVisualRelease = release;
            setData(KN8Attachments.RELEASE_VISUAL, release);
            // Mais poder, mais rapido: a velocidade acompanha o Release (escalada e desespero).
            applySpeed(release);
        }
    }

    // --- GeckoLib ----------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3, state ->
                state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "arms", 4, state -> {
            String stance = isAggressive() ? "aim" : state.isMoving() ? "walk" : "ready";
            return state.setAndContinue(ARMS.get(stance));
        }));
        AnimationController<HoshinaEntity> action = new AnimationController<>(this, "action", 1,
                state -> PlayState.STOP);
        action.triggerableAnim("attack", RawAnimation.begin().thenPlay("hoshina.action.attack"));
        for (String id : ANIMATED) {
            action.triggerableAnim(id, RawAnimation.begin().thenPlay("hoshina.action." + id));
        }
        controllers.add(action);
        controllers.add(new AnimationController<>(this, "reaction", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("hoshina.reaction.hurt")));
    }
}
