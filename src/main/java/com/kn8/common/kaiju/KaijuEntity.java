// src/main/java/com/kn8/common/kaiju/KaijuEntity.java
package com.kn8.common.kaiju;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.boss.BossService;
import com.kn8.common.boss.BossState;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.vfx.AbilityEffects;
import com.kn8.common.vfx.VfxService;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.core.KN8Ids;
import com.kn8.core.combat.ActionTimeline;
import com.kn8.core.kaiju.AbilityGeometry;
import com.kn8.core.kaiju.KaijuBrain;
import com.kn8.core.kaiju.KaijuState;
import com.kn8.core.kaiju.KaijuStats;

import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 * Entidade base de TODO kaiju (M7a). Um EntityType por especie (decisao da Fase 4), mas nenhum numero no Java: a
 * especie e o id do proprio EntityType ({@code kn8:primigenius}) e tudo vem do {@code KaijuDef} do JSON.
 *
 * <ul>
 *   <li>Atributos: curva de fortitude + multiplicadores do config ({@link KaijuStats}); recalculados quando os
 *   dados mudam ({@code /reload}), mantendo a fracao de vida.</li>
 *   <li>Hitbox: {@code dimensions} do JSON, nos dois lados (o cliente recebe os dados de kaiju desde o M4).</li>
 *   <li>Estado ({@link KaijuBrain}): calculado no servidor, sincronizado para as animacoes.</li>
 *   <li>Animacoes GeckoLib com os 4 controllers do PT3; nomes {@code <especie>.<camada>.<nome>}.</li>
 *   <li>Multipartes (M7b, regras provadas no PT4): uma {@link KaijuPart} por entrada de {@code parts}; corpo nao
 *   miravel quando ha partes; multiplicador por parte; explosao conta uma vez por tick no corpo inteiro e nao
 *   atinge o nucleo; nucleo com vida propria ({@code core.health_fraction}) que, zerada, mata na hora.</li>
 *   <li>Habilidades (M8): as {@code abilities} do JSON, na ordem, seguindo a {@link ActionTimeline} do PT7
 *   (preparo, impacto no tick {@code windup_ticks}, ativo, recarga). Dano no servidor, no tick de impacto; a
 *   animacao e so visual. Atordoamento cancela o preparo. Especie sem habilidades usa um ataque basico.</li>
 *   <li>Bando: especies com a tag {@code pack} chamam as vizinhas quando sao atacadas.</li>
 * </ul>
 * Dados ausentes (JSON quebrado) nao derrubam nada: o kaiju fica com os atributos base e um aviso no log.
 *
 * <p>Quantidade e nomes das partes ficam fixos na criacao da entidade: o NeoForge registra as partes no nivel ao
 * comecar a rastrear a entidade e os IDs (pai + partes, padrao do Ender Dragon) sao reservados no construtor.
 * Medidas, posicoes e multiplicadores mudam ao vivo com {@code /reload}.</p>
 */
public class KaijuEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    /** 0.1-B (barra de vida): fracao do nucleo (0-1; negativa = especie sem nucleo) e nucleo exposto. */
    private static final EntityDataAccessor<Float> CORE_FRACTION =
            SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CORE_EXPOSED =
            SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.BOOLEAN);

    // Atributos base so valem ate o JSON ser aplicado (primeiro tick no servidor).
    private static final double BASE_HEALTH = 20.0;
    private static final double BASE_DAMAGE = 2.0;
    private static final double BASE_SPEED = 0.25;
    private static final double BASE_FOLLOW_RANGE = 16.0;
    private static final double BASE_KNOCKBACK_RESISTANCE = 0.5;
    private static final double MELEE_SPEED_MODIFIER = 1.0;
    private static final double STROLL_SPEED_MODIFIER = 0.8;
    private static final float LOOK_DISTANCE = 12.0F;
    private static final int TRANSITION_TICKS = 5;
    private static final String TAG_CORE_HEALTH = "kn8_core_health";
    private static final String TAG_BOSS = "kn8_boss";
    private static final String TAG_BOSS_PHASE = "kn8_boss_phase";
    private static final double MAX_STEP_HEIGHT = 4.0;
    private static final int CORE_SYNC_STEPS = 200;
    private static final String PACK_TAG = "pack";
    private static final String TYPE_MELEE = "melee";
    private static final String TYPE_AREA_MELEE = "area_melee";
    private static final String TYPE_CHARGE = "charge";
    /** 0.6: o salto so "aterrissa" depois de alguns ticks no ar; desiste se nao voltar ao chao. */
    private static final int LEAP_MIN_AIR_TICKS = 3;
    private static final int LEAP_MAX_TICKS = 80;
    private static final int RAGE_CHECK_TICKS = 10;
    private static final ResourceLocation RAGE_MODIFIER = KN8Constants.id("kaiju_rage");
    /** Folga no alcance no tick de impacto: o alvo pode ter dado um passo durante o preparo. */
    private static final double IMPACT_REACH_TOLERANCE = 1.25;

    // Avisos de "dados ausentes" uma unica vez por especie (definicao de log, nao estado de jogo).
    private static final Set<ResourceLocation> WARNED_MISSING = new HashSet<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final KaijuPart[] parts;
    private int appliedVersion = -1;
    private int staggerTicks;
    /** Escala do empurrao do golpe em andamento (so dentro do hurt; 1 fora dele). */
    private double knockbackScale = 1.0;
    /** Vida do nucleo; negativa = ainda nao inicializada (enche no primeiro tick com a definicao). */
    private float coreHealth = -1;
    private long lastExplosionTick = Long.MIN_VALUE;
    private long coreExposedUntil = Long.MIN_VALUE;
    private long nextPathClearTick;
    private final ActionTimeline abilityTimeline = new ActionTimeline();
    private final Map<ResourceLocation, Long> abilityReadyAt = new HashMap<>();
    private ResourceLocation currentAbility;
    private LivingEntity abilityTarget;
    private long nextBasicAttackTick;
    /** Investida em andamento: direcao fixa e alvos ja atingidos (cada um so uma vez por investida). */
    private Vec3 chargeDirection;
    /** 0.6: golpes que faltam do multi_hit em andamento, quando sai o proximo, a habilidade e o alvo. */
    private int multiHitsLeft;
    private long nextMultiHitTick;
    private ResourceLocation multiHitAbility;
    private LivingEntity multiHitTarget;
    /** 0.6: salto em andamento (dano na aterrissagem) e quando comecou. */
    private ResourceLocation leapAbility;
    private long leapStartTick;
    /** 0.6: furia ja ativada (rage do JSON; nao volta). */
    private boolean enraged;
    private final Set<UUID> chargeHits = new HashSet<>();
    /** Verdadeiro so durante a aplicacao de dano de uma habilidade "heavy" (lido pelo bloqueio do jogador). */
    private boolean dealingHeavyHit;

    public KaijuEntity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level);
        List<KaijuDef.Part> definitions = def().map(KaijuDef::parts).orElse(List.of());
        this.parts = new KaijuPart[definitions.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new KaijuPart(this, definitions.get(i));
        }
        if (parts.length > 0) {
            // Reserva o bloco de IDs pai + partes (ENTITY_COUNTER e protected via access transformer do NeoForge).
            setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        }
    }

    // --- sons (0.2) -------------------------------------------------------------------------------------------

    /** Altura de referencia do tom: kaiju maiores soam mais graves, menores mais agudos. */
    private static final float VOICE_REFERENCE_HEIGHT = 4.0F;
    private static final float VOICE_EXPONENT = 0.35F;
    private static final int AMBIENT_INTERVAL_TICKS = 200;
    private static final float STEP_VOLUME_PER_BLOCK = 0.12F;

    @Override
    protected SoundEvent getAmbientSound() {
        return KN8Sounds.KAIJU_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KN8Sounds.KAIJU_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KN8Sounds.KAIJU_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AMBIENT_INTERVAL_TICKS;
    }

    @Override
    public float getVoicePitch() {
        float size = Math.max(getBbHeight(), getBbWidth() * 0.6F);
        float pitch = (float) Math.pow(VOICE_REFERENCE_HEIGHT / size, VOICE_EXPONENT);
        return Mth.clamp(pitch, 0.5F, 1.6F) * (0.95F + random.nextFloat() * 0.1F);
    }

    @Override
    protected float getSoundVolume() {
        return Math.max(1.0F, getBbHeight() / VOICE_REFERENCE_HEIGHT);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(KN8Sounds.KAIJU_STEP.get(), Math.min(1.5F, getBbHeight() * STEP_VOLUME_PER_BLOCK), getVoicePitch());
    }

    public static AttributeSupplier.Builder baseAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_DAMAGE)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.FOLLOW_RANGE, BASE_FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, BASE_KNOCKBACK_RESISTANCE)
                // 0.6-E: so os voadores usam (FlyingMoveControl); o valor real sai do flyer/<id>.json.
                .add(Attributes.FLYING_SPEED, BASE_SPEED);
    }

    /** Id da especie = id do EntityType = id do arquivo {@code kaiju/<id>.json}. */
    public ResourceLocation kaijuId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(getType());
    }

    public Optional<KaijuDef> def() {
        return KN8Data.KAIJU.get(kaijuId(), level().isClientSide());
    }

    public KaijuState state() {
        return KaijuState.byIndex(this.entityData.get(STATE));
    }

    public KaijuPart[] kaijuParts() {
        return parts.clone();
    }

    public float coreHealth() {
        return Math.max(0, coreHealth);
    }

    public float maxCoreHealth() {
        return getMaxHealth() * def().map(definition -> definition.core().healthFraction()).orElse(0.25F);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return parts.length > 0;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    /** Com partes, o corpo nao e miravel: golpes e flechas acertam as partes. Sem partes, e um mob normal. */
    @Override
    public boolean isPickable() {
        return parts.length == 0 && super.isPickable();
    }

    /** Servidor: atualiza o que a barra de vida dos clientes mostra (o SynchedEntityData so envia quando muda). */
    private void syncCoreForClients() {
        float max = maxCoreHealth();
        float fraction = max > 0 && coreHealth >= 0 ? coreHealth / max : -1.0F;
        // Arredonda para nao reenviar a cada fracao minima.
        fraction = Math.round(fraction * CORE_SYNC_STEPS) / (float) CORE_SYNC_STEPS;
        if (entityData.get(CORE_FRACTION) != fraction) {
            entityData.set(CORE_FRACTION, fraction);
        }
        boolean exposed = isCoreExposed();
        if (entityData.get(CORE_EXPOSED) != exposed) {
            entityData.set(CORE_EXPOSED, exposed);
        }
    }

    /** Cliente e servidor: fracao do nucleo (negativa = sem nucleo). */
    public float syncedCoreFraction() {
        return entityData.get(CORE_FRACTION);
    }

    public boolean syncedCoreExposed() {
        return entityData.get(CORE_EXPOSED);
    }

    /** GDD secao 12: golpe pesado expoe o nucleo por alguns ticks (multiplicador extra do config). */
    public void exposeCore(int ticks) {
        coreExposedUntil = Math.max(coreExposedUntil, level().getGameTime() + ticks);
    }

    public boolean isCoreExposed() {
        return level().getGameTime() < coreExposedUntil;
    }

    /** Atordoamento (fraquezas e golpes pesados nos proximos modulos; hoje so por comando). */
    public void stagger(int ticks) {
        staggerTicks = Math.max(staggerTicks, ticks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CORE_FRACTION, -1.0F);
        builder.define(CORE_EXPOSED, false);
        builder.define(STATE, KaijuState.IDLE.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new KaijuCombatGoal(this, MELEE_SPEED_MODIFIER));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED_MODIFIER));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        HurtByTargetGoal revenge = new HurtByTargetGoal(this);
        if (def().map(definition -> definition.tags().contains(PACK_TAG)).orElse(false)) {
            // Bando (Trichonephila): quem e atacado chama as vizinhas da mesma especie para o mesmo alvo.
            revenge.setAlertOthers();
        }
        this.targetSelector.addGoal(1, revenge);
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // 0.1-B: kaiju tambem cacam soldados da Forca de Defesa (prioridade menor que jogadores).
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, true));
    }

    @Override
    public void tick() {
        int version = KN8Data.KAIJU.version(level().isClientSide());
        if (version != appliedVersion) {
            appliedVersion = version;
            applyDefinition();
        }
        super.tick();
        positionParts();
        if (!level().isClientSide()) {
            if (staggerTicks > 0) {
                staggerTicks--;
            }
            tickAbility();
            breakWhileWalking();
            checkRage();
            updateState();
            syncCoreForClients();
            if (bossState != null) {
                BossService.tick(this, bossState);
            }
        }
    }

    // --- chefe (0.2, Etapa 6) ----------------------------------------------------------------------------------

    /** Estado de chefe; null = kaiju comum. */
    private BossState bossState;

    public BossState bossState() {
        return bossState;
    }

    public void setBossState(BossState state) {
        bossState = state;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (bossState != null) {
            bossState.clearBar();
        }
        super.remove(reason);
    }

    // --- habilidades (M8) ------------------------------------------------------------------------------------

    public boolean isUsingAbility() {
        return abilityTimeline.isActive(level().getGameTime());
    }

    public Optional<ResourceLocation> currentAbility() {
        return isUsingAbility() ? Optional.ofNullable(currentAbility) : Optional.empty();
    }

    /** 0.6-D: alvo da habilidade em andamento (o soldado especial reage quando e ele). */
    public Optional<LivingEntity> abilityTarget() {
        return isUsingAbility() ? Optional.ofNullable(abilityTarget) : Optional.empty();
    }

    /**
     * 0.6-D: ticks ate o impacto da habilidade em andamento (o fim do telegraph); vazio sem habilidade ou depois do
     * impacto. E o que o Hoshina usa para esquivar e contra-atacar no tempo certo.
     */
    public Optional<Integer> ticksToImpact() {
        if (!isUsingAbility() || currentAbility == null) {
            return Optional.empty();
        }
        return KN8Data.ABILITY.get(currentAbility, false).map(ability -> (int) (abilityTimeline.startTick()
                + ability.windupTicks() - level().getGameTime())).filter(ticks -> ticks >= 0);
    }

    /** 0.6-D: a habilidade em andamento e "heavy" (atravessa o bloqueio; o Hoshina responde com Kaeshi-uchi)? */
    public boolean isPreparingHeavy() {
        return currentAbility().flatMap(id -> KN8Data.ABILITY.get(id, false)).map(AbilityDef::heavy).orElse(false);
    }

    /**
     * Distancia entre as BORDAS das hitboxes, no plano horizontal (0 = encostadas). Correcao do M8: medir de centro
     * a centro falhava porque a navegacao para antes de encostar e kaiju largos nunca "alcancavam" o alvo.
     */
    public double edgeDistance(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return Math.max(0.0, horizontal - getBbWidth() / 2.0 - target.getBbWidth() / 2.0);
    }

    /** O alvo esta na faixa de altura do kaiju (com a folga de alcance para cima e para baixo)? */
    private boolean verticallyInReach(LivingEntity target, double reach) {
        double bottom = getY() - reach;
        double top = getY() + getBbHeight() + reach;
        return target.getY() < top && target.getY() + target.getBbHeight() > bottom;
    }

    /** Alcance de corpo a corpo entre as bordas: folga do config ({@code kaiju.meleeReachBonus}). */
    public double meleeReach() {
        return ServerConfig.MELEE_REACH_BONUS.get();
    }

    /** O alvo esta ao alcance de corpo a corpo (sem habilidade especifica)? */
    public boolean inMeleeReach(LivingEntity target) {
        return isInReach(target, meleeReach());
    }

    boolean isInReach(LivingEntity target, double reach) {
        return edgeDistance(target) <= reach && verticallyInReach(target, reach);
    }

    /**
     * Chamado pelo goal de combate: inicia a primeira habilidade do JSON pronta e ao alcance; sem habilidades,
     * usa o ataque basico. Retorna true se algo comecou.
     */
    boolean tryAttack(LivingEntity target) {
        long now = level().getGameTime();
        if (staggerTicks > 0 || isUsingAbility() || !ServerConfig.SPEC.isLoaded()) {
            return false;
        }
        Optional<KaijuDef> def = def();
        List<ResourceLocation> abilities = def.map(KaijuDef::abilities).orElse(List.of());
        if (abilities.isEmpty()) {
            if (now >= nextBasicAttackTick && inMeleeReach(target)) {
                nextBasicAttackTick = now + ServerConfig.BASIC_ATTACK_INTERVAL_TICKS.get();
                return doHurtTarget(target);
            }
            return false;
        }
        // 0.6: prontas, ao alcance (corpo a corpo ou a distancia com linha de visao), pela prioridade do JSON.
        Optional<ResourceLocation> chosen = KaijuAbilities.choose(this, target, abilities, abilityReadyAt, now);
        return chosen.isPresent() && startAbility(chosen.get(), target);
    }

    /** Inicia uma habilidade especifica (goal de combate, comandos e testes). */
    public boolean startAbility(ResourceLocation id, LivingEntity target) {
        Optional<AbilityDef> found = KN8Data.ABILITY.get(id, false);
        long now = level().getGameTime();
        if (found.isEmpty() || isUsingAbility()) {
            return false;
        }
        AbilityDef ability = found.get();
        int duration = ability.windupTicks() + Math.max(1, ability.activeTicks());
        if (!abilityTimeline.tryStart(now, id.toString(), duration, ability.windupTicks())) {
            return false;
        }
        currentAbility = id;
        abilityTarget = target;
        // 0.6: enfurecido (rage do JSON da especie), as recargas encurtam.
        double cooldown = ability.cooldownTicks() * (enraged ? def().flatMap(KaijuDef::rage)
                .map(KaijuDef.Rage::cooldownMultiplier).orElse(1.0F) : 1.0F);
        abilityReadyAt.put(id, now + duration + Math.round(cooldown));
        triggerAnim("action", ability.animation());
        // 0.6 (telegraph): o efeito de aviso ("particles" do JSON) sai no inicio do preparo, para dar tempo de reagir.
        ability.particles().ifPresent(effect -> {
            if (level() instanceof ServerLevel server) {
                VfxService.play(server, effect, KaijuAbilities.mouth(this), bodyForward(), 1.0F, 0.0F);
            }
        });
        return true;
    }

    /** Um tick do servidor: cancela se atordoado e resolve o impacto no tick exato do JSON. */
    private void tickAbility() {
        long now = level().getGameTime();
        tickFollowUps(now);
        if (currentAbility == null) {
            return;
        }
        if (staggerTicks > 0 && isUsingAbility()) {
            abilityTimeline.cancel();
            endAbility();
            return;
        }
        if (!abilityTimeline.consumeImpact(now)) {
            if (!isUsingAbility()) {
                endAbility();
            } else if (chargeDirection != null) {
                tickCharge();
            }
            return;
        }
        Optional<AbilityDef> ability = KN8Data.ABILITY.get(currentAbility, false);
        if (ability.isEmpty()) {
            return;
        }
        resolveImpact(currentAbility, ability.get());
    }

    /**
     * 0.6: o que continua depois do tick de impacto: golpes seguintes do {@code kn8:multi_hit} (no ritmo do JSON) e a
     * aterrissagem do {@code kn8:leap} (dano quando volta ao chao). Atordoado, o combo para.
     */
    private void tickFollowUps(long now) {
        if (multiHitsLeft > 0 && now >= nextMultiHitTick) {
            Optional<AbilityDef> ability = KN8Data.ABILITY.get(multiHitAbility, false);
            if (ability.isEmpty() || staggerTicks > 0) {
                multiHitsLeft = 0;
            } else {
                KaijuAbilities.multiHit(this, ability.get(), multiHitTarget);
                multiHitsLeft--;
                nextMultiHitTick = now + ability.get().behavior().hitInterval();
            }
        }
        if (leapAbility != null && now - leapStartTick > LEAP_MIN_AIR_TICKS && (onGround() || isInWater())) {
            KN8Data.ABILITY.get(leapAbility, false).ifPresent(ability -> KaijuAbilities.land(this, ability));
            leapAbility = null;
        } else if (leapAbility != null && now - leapStartTick > LEAP_MAX_TICKS) {
            leapAbility = null;
        }
    }

    /**
     * 0.6: furia ({@code rage} do JSON da especie): com a vida abaixo do limite, de uma vez, dano e velocidade sobem
     * (modificadores de atributo) e as recargas encurtam; avisa com o efeito de rugido e o som.
     */
    private void checkRage() {
        if (enraged || tickCount % RAGE_CHECK_TICKS != 0) {
            return;
        }
        Optional<KaijuDef.Rage> rage = def().flatMap(KaijuDef::rage);
        if (rage.isEmpty() || getHealth() / Math.max(1.0F, getMaxHealth()) > rage.get().healthBelow()) {
            return;
        }
        enraged = true;
        addRageModifier(Attributes.ATTACK_DAMAGE, rage.get().damageMultiplier() - 1.0);
        addRageModifier(Attributes.MOVEMENT_SPEED, rage.get().speedMultiplier() - 1.0);
        if (level() instanceof ServerLevel server) {
            VfxService.play(server, VfxService.ROAR, KaijuAbilities.mouth(this), bodyForward(), 1.5F, 0.4F);
            server.playSound(null, getX(), getY(), getZ(), KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 3.0F,
                    0.8F);
        }
    }

    private void addRageModifier(Holder<Attribute> attribute, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null && amount != 0.0) {
            instance.removeModifier(RAGE_MODIFIER);
            instance.addTransientModifier(new AttributeModifier(RAGE_MODIFIER, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Enfurecido (rage do JSON)? */
    public boolean isEnraged() {
        return enraged;
    }

    private void endAbility() {
        currentAbility = null;
        abilityTarget = null;
        chargeDirection = null;
        chargeHits.clear();
    }

    /** Tick de impacto: cada tipo de habilidade do JSON resolve o dano do seu jeito, sempre no servidor. */
    private void resolveImpact(ResourceLocation id, AbilityDef ability) {
        switch (ability.type().getPath()) {
            case TYPE_MELEE -> {
                LivingEntity target = abilityTarget;
                if (target != null && target.isAlive()
                        && isInReach(target, reachOf(ability) * IMPACT_REACH_TOLERANCE)) {
                    dealAbilityDamage(target, ability, null);
                    AbilityEffects.play(this, ability, target.position().add(0, target.getBbHeight() / 2, 0));
                }
            }
            case TYPE_AREA_MELEE -> resolveSlam(ability);
            case TYPE_CHARGE -> startCharge();
            case KaijuAbilities.TYPE_SWEEP -> KaijuAbilities.sweep(this, ability);
            case KaijuAbilities.TYPE_PROJECTILE -> KaijuAbilities.fire(this, id, ability, abilityTarget);
            case KaijuAbilities.TYPE_LEAP -> {
                KaijuAbilities.leap(this, abilityTarget);
                leapAbility = id;
                leapStartTick = level().getGameTime();
            }
            case KaijuAbilities.TYPE_MULTI_HIT -> {
                KaijuAbilities.multiHit(this, ability, abilityTarget);
                multiHitAbility = id;
                multiHitTarget = abilityTarget;
                multiHitsLeft = ability.behavior().hits() - 1;
                nextMultiHitTick = level().getGameTime() + ability.behavior().hitInterval();
            }
            default -> {
                if (WARNED_MISSING.add(ability.type().withPrefix("ability_type/"))) {
                    KN8Constants.LOGGER.warn("[kn8] Tipo de habilidade {} ({}) desconhecido; so animacao.",
                            ability.type(), id);
                }
            }
        }
    }

    /** Frente do corpo no plano horizontal (rotacao do corpo, nao da cabeca). */
    Vec3 bodyForward() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** {@code kn8:area_melee}: todos na area a frente do kaiju levam o golpe e sao empurrados para fora. */
    private void resolveSlam(AbilityDef ability) {
        Vec3 center = position().add(bodyForward().scale(AbilityGeometry.slamCenterForward(getBbWidth())));
        double reach = ability.radius() + getBbWidth();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(reach, 0, reach))) {
            if (target == this || target instanceof KaijuEntity || !target.isAlive()) {
                continue;
            }
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            if (AbilityGeometry.inSlamArea(dx, dz, target.getY() - getY(), target.getBbWidth(),
                    target.getBbHeight(), ability.radius(), getBbHeight())) {
                dealAbilityDamage(target, ability, new Vec3(dx, 0, dz));
            }
        }
        // 0.1-B: o mesmo impacto quebra o ambiente, solta os efeitos, o tremor e o som.
        ability.destruction().ifPresent(destruction -> DestructionService.request((ServerLevel) level(), center,
                destruction, this));
        AbilityEffects.play(this, ability, center);
    }

    /** {@code kn8:charge}: no impacto comeca a investida na direcao do alvo (ou da frente do corpo). */
    private void startCharge() {
        LivingEntity target = abilityTarget;
        Vec3 direction = target != null && target.isAlive()
                ? new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()) : bodyForward();
        chargeDirection = direction.lengthSqr() < 1.0E-4 ? bodyForward() : direction.normalize();
        chargeHits.clear();
        tickCharge();
    }

    /** Um tick da investida: anda reto e acerta cada alvo no caminho uma unica vez. Para ao bater numa parede. */
    private void tickCharge() {
        Optional<AbilityDef> ability = KN8Data.ABILITY.get(currentAbility, false);
        if (ability.isEmpty() || chargeDirection == null) {
            return;
        }
        if (horizontalCollision) {
            chargeDirection = null;
            return;
        }
        double speed = ServerConfig.CHARGE_SPEED.get();
        setDeltaMovement(chargeDirection.x * speed, getDeltaMovement().y, chargeDirection.z * speed);
        // 0.1-B: a investida atravessa o que a forca dela quebra (faixa logo a frente do corpo).
        ability.get().destruction().ifPresent(destruction -> DestructionService.requestFront(this,
                destruction.power(), destruction.radius()));
        double radius = ability.get().radius();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius, 0, radius))) {
            if (target != this && !(target instanceof KaijuEntity) && target.isAlive()
                    && chargeHits.add(target.getUUID())) {
                dealAbilityDamage(target, ability.get(), chargeDirection);
                AbilityEffects.play(this, ability.get(), target.position());
            }
        }
    }

    /**
     * Dano de habilidade: {@code ATTACK_DAMAGE * damage_multiplier}. Habilidades "heavy" ficam marcadas durante a
     * aplicacao (o bloqueio comum do jogador nao segura; so parry ou esquiva). Empurrao opcional na direcao dada.
     */
    void dealAbilityDamage(LivingEntity target, AbilityDef ability, Vec3 push) {
        float damage = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * ability.damageMultiplier());
        dealingHeavyHit = ability.heavy();
        boolean hurt;
        try {
            hurt = target.hurt(damageSources().mobAttack(this), damage);
        } finally {
            dealingHeavyHit = false;
        }
        if (hurt && push != null && push.lengthSqr() > 1.0E-4) {
            Vec3 away = push.normalize();
            target.knockback(ServerConfig.ABILITY_KNOCKBACK.get(), -away.x, -away.z);
        }
    }

    /** Durante a aplicacao de dano de uma habilidade "heavy"? (o bloqueio comum nao segura esse golpe). */
    public boolean isDealingHeavyHit() {
        return dealingHeavyHit;
    }

    /** Alcance de uma habilidade entre as bordas: folga de corpo a corpo + {@code radius} do JSON. */
    public double reachOf(AbilityDef ability) {
        return meleeReach() + ability.radius();
    }

    public long nextPathClearTick() {
        return nextPathClearTick;
    }

    public void setNextPathClearTick(long tick) {
        nextPathClearTick = tick;
    }

    /** Kaiju largo demais para a navegacao vanilla (Etapa C): anda direto ate o alvo. */
    public boolean isLarge() {
        return getBbWidth() >= ServerConfig.LARGE_KAIJU_WIDTH.get();
    }

    /**
     * Kaiju travado num obstaculo (colisao horizontal) a caminho do alvo: pede ao DestructionService para abrir
     * passagem (so blocos que a forca do kaiju quebra; respeita areas protegidas, mobGriefing e o config). Desde a
     * 0.6 vale para qualquer tamanho e tambem fora do combate ({@link #breakWhileWalking}).
     */
    public void clearPathIfBlocked() {
        if (!level().isClientSide() && horizontalCollision) {
            DestructionService.requestPathClear(this, false);
        }
    }

    /**
     * 0.6 (Miguel: kaiju ficavam presos em construcoes): todo tick no servidor. Batendo de frente em blocos enquanto
     * tenta andar (caminho, movimento direto ou alvo), quebra a faixa da frente; preso dentro de blocos, quebra o que
     * ocupa o corpo. Forca e intervalo em {@code [destruction] walk*}; protecoes e mobGriefing continuam valendo.
     */
    private void breakWhileWalking() {
        if (isNoAi() || isUsingAbility()) {
            return;
        }
        if (isInWall()) {
            DestructionService.requestPathClear(this, true);
            return;
        }
        boolean tryingToMove = getNavigation().isInProgress() || getMoveControl().hasWanted() || getTarget() != null;
        if (horizontalCollision && tryingToMove) {
            DestructionService.requestPathClear(this, false);
        }
    }

    /** Alcance da primeira habilidade da especie (ou corpo a corpo): o goal de combate usa para se aproximar. */
    double approachReach() {
        return def().map(KaijuDef::abilities).orElse(List.of()).stream()
                .map(id -> KN8Data.ABILITY.get(id, false))
                .flatMap(Optional::stream)
                .mapToDouble(this::reachOf)
                .min()
                .orElse(meleeReach());
    }

    /**
     * Hitbox do JSON; antes dos dados chegarem, o tamanho registrado no EntityType. No NeoForge 21.1
     * {@code LivingEntity#getDimensions} e final: o ponto de extensao e {@code getDefaultDimensions} (bug do M7a).
     */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        Optional<KaijuDef> def = def();
        if (def.isEmpty()) {
            return super.getDefaultDimensions(pose);
        }
        KaijuDef.Dimensions size = def.get().dimensions();
        return EntityDimensions.scalable(size.width(), size.height());
    }

    private void applyDefinition() {
        refreshDimensions();
        Optional<KaijuDef> found = def();
        found.ifPresent(this::updateParts);
        if (level().isClientSide()) {
            return;
        }
        if (found.isEmpty()) {
            if (WARNED_MISSING.add(kaijuId())) {
                KN8Constants.LOGGER.warn("[kn8] Kaiju {} sem definicao valida nos dados; usando atributos base.",
                        kaijuId());
            }
            return;
        }
        if (!ServerConfig.SPEC.isLoaded()) {
            // Config ainda nao carregou: tenta de novo no proximo tick.
            appliedVersion = -1;
            return;
        }
        KaijuDef def = found.get();
        KaijuDef.Overrides overrides = def.overrides();
        KaijuStats stats = KaijuStats.of(def.fortitude(), ServerConfig.fortitudeCurve(),
                ServerConfig.kaijuMultipliers(), optional(overrides.health()), optional(overrides.damage()),
                optional(overrides.armor()));
        float healthFraction = getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1.0F;
        float coreFraction = coreHealth < 0 || maxCoreHealth() <= 0 ? 1.0F : coreHealth / maxCoreHealth();
        setBase(Attributes.MAX_HEALTH, stats.health());
        setBase(Attributes.ATTACK_DAMAGE, stats.damage());
        setBase(Attributes.ARMOR, stats.armor());
        setBase(Attributes.MOVEMENT_SPEED, def.speed());
        setBase(Attributes.FOLLOW_RANGE, ServerConfig.FOLLOW_RANGE_BASE.get()
                + ServerConfig.FOLLOW_RANGE_PER_INTELLIGENCE.get() * def.intelligence());
        // Etapa C: kaiju grandes sobem degraus proporcionais ao tamanho (entre 1 e 4 blocos).
        setBase(Attributes.STEP_HEIGHT, Math.max(1.0, Math.min(MAX_STEP_HEIGHT,
                def.dimensions().height() * ServerConfig.STEP_HEIGHT_FRACTION.get())));
        setHealth(Math.max(1.0F, getMaxHealth() * healthFraction));
        coreHealth = maxCoreHealth() * coreFraction;
    }

    /** Aplica medidas/posicoes/multiplicadores novos; quantidade ou nomes diferentes so valem para kaiju novos. */
    private void updateParts(KaijuDef def) {
        List<KaijuDef.Part> definitions = def.parts();
        boolean sameLayout = definitions.size() == parts.length;
        for (int i = 0; sameLayout && i < parts.length; i++) {
            sameLayout = definitions.get(i).name().equals(parts[i].partName());
        }
        if (!sameLayout) {
            if (!level().isClientSide() && WARNED_MISSING.add(kaijuId().withSuffix("/parts"))) {
                KN8Constants.LOGGER.warn("[kn8] Partes de {} mudaram de quantidade ou nome; vale para kaiju novos.",
                        kaijuId());
            }
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i].setDefinition(definitions.get(i));
        }
    }

    /**
     * Posiciona cada parte pelo offset do JSON ([direita, cima, frente]) girado pela rotacao do corpo. Roda nos
     * dois lados; valores antigos = posicao anterior, para o F3+B interpolar as caixas sem tremer.
     */
    private void positionParts() {
        if (parts.length == 0) {
            return;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double lookX = -Mth.sin(yaw);
        double lookZ = Mth.cos(yaw);
        double rightX = -Mth.cos(yaw);
        double rightZ = -Mth.sin(yaw);
        for (KaijuPart part : parts) {
            Vec3 offset = part.definition().offset();
            double previousX = part.getX();
            double previousY = part.getY();
            double previousZ = part.getZ();
            part.setPos(getX() + lookX * offset.z + rightX * offset.x, getY() + offset.y,
                    getZ() + lookZ * offset.z + rightZ * offset.x);
            part.xo = previousX;
            part.yo = previousY;
            part.zo = previousZ;
            part.xOld = previousX;
            part.yOld = previousY;
            part.zOld = previousZ;
        }
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static OptionalDouble optional(Optional<Double> value) {
        return value.map(OptionalDouble::of).orElse(OptionalDouble.empty());
    }

    private void updateState() {
        LivingEntity target = getTarget();
        boolean hasTarget = target != null && target.isAlive();
        // Mesma medida do combate (bordas das hitboxes), para ATTACK significar "ao alcance de verdade".
        double edge = hasTarget ? edgeDistance(target) : Double.MAX_VALUE;
        double reach = hasTarget ? approachReach() : 0.0;
        boolean moving = getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
        KaijuState next = KaijuBrain.next(new KaijuBrain.Inputs(isDeadOrDying(), staggerTicks, hasTarget,
                edge * edge, reach * reach, moving));
        if (next.ordinal() != this.entityData.get(STATE)) {
            this.entityData.set(STATE, next.ordinal());
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("action", "attack");
        }
        return hit;
    }

    /** Dano direto no corpo (explosao, fogo, queda, comandos, ou golpe em kaiju sem partes): multiplicador 1,0. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.is(DamageTypeTags.IS_EXPLOSION)) {
            long now = level().getGameTime();
            if (now == lastExplosionTick) {
                return false;
            }
            lastExplosionTick = now;
        }
        return applyDamage(source, capPlayerHit(source, amount));
    }

    /**
     * 0.5.0 (Miguel): golpe comum de jogador nunca mata kaiju de uma vez, por mais forte que ele esteja; so o golpe
     * especial da arma ({@code kn8:weapon_special}) passa do teto. O teto vale sobre o dano ja com o multiplicador da
     * parte e do nucleo exposto ({@code combat.maxHitFractionOfKaijuHealth} da vida maxima).
     */
    private float capPlayerHit(DamageSource source, float amount) {
        if (!ServerConfig.SPEC.isLoaded() || !(source.getEntity() instanceof Player)
                || source.is(CombatService.WEAPON_SPECIAL)) {
            return amount;
        }
        return Math.min(amount, getMaxHealth() * ServerConfig.MAX_HIT_FRACTION_OF_KAIJU_HEALTH.get().floatValue());
    }

    /**
     * Chamado pelas partes: multiplicador da parte e regras do nucleo (GDD secao 12).
     *
     * <p>Destruir o nucleo mata sempre. O golpe passa pelo {@code super.hurt} normal (armadura, invulnerabilidade,
     * eventos); se foi aceito e o nucleo chegou a zero, a morte e garantida depois, sem armadura (bug do M7b: a
     * armadura reduzia o golpe e o kaiju sobrevivia com o nucleo destruido).</p>
     */
    boolean hurtFromPart(KaijuPart part, DamageSource source, float amount) {
        if (level().isClientSide()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            // Explosao nao "mira": corpo inteiro, uma vez por tick, sem atingir o nucleo (regra do PT4).
            return hurt(source, amount);
        }
        float scaled = amount * part.multiplier();
        if (part.isCore() && isCoreExposed()) {
            scaled *= ServerConfig.CORE_EXPOSED_MULTIPLIER.get().floatValue();
        }
        scaled = capPlayerHit(source, scaled);
        float coreBefore = coreHealth;
        boolean coreDestroyed = false;
        if (part.isCore() && coreHealth >= 0) {
            coreHealth = Math.max(0.0F, coreHealth - scaled);
            coreDestroyed = coreHealth <= 0.0F;
        }
        boolean applied = applyDamage(source, scaled);
        if (!applied) {
            // Invulnerabilidade de dano: nada foi aplicado, entao o nucleo tambem nao perde vida.
            coreHealth = coreBefore;
            return false;
        }
        if (coreDestroyed && isAlive()) {
            KN8Constants.LOGGER.debug("[kn8] Nucleo de {} destruido: morte garantida.", kaijuId());
            setHealth(0.0F);
            die(source);
        }
        return true;
    }

    private boolean applyDamage(DamageSource source, float amount) {
        if (bossState != null && !level().isClientSide()) {
            if (bossState.isInvulnerable(level().getGameTime())) {
                // Troca de fase do chefe: alguns ticks sem levar dano (boss/*.json, invuln_ticks).
                return false;
            }
            if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                bossState.participants().add(player.getUUID());
            }
        }
        if (!level().isClientSide() && source.getEntity() != null && !source.is(DamageTypeTags.IS_EXPLOSION)) {
            // 0.3: golpe de quem ataca (jogador, soldado, kaiju) ignora a invulnerabilidade vanilla de 10 ticks. O
            // combate do mod ja acerta uma vez por acao (ActionTimeline); a invulnerabilidade so engolia os golpes
            // de quem luta em grupo (4 soldados matavam quase no mesmo tempo que 1; 2 jogadores perdiam golpes).
            // Fogo, lava, queda e explosao (uma vez por tick, ver hurt) continuam com ela.
            invulnerableTime = 0;
        }
        // 0.5 (Miguel): o empurrao vanilla de cada golpe de soldado impedia o kaiju de chegar perto de um grupo.
        knockbackScale = source.getEntity() instanceof SoldierEntity
                ? ServerConfig.SOLDIER_KNOCKBACK_ON_KAIJU.get() : 1.0;
        boolean hurt;
        try {
            hurt = super.hurt(source, amount);
        } finally {
            knockbackScale = 1.0;
        }
        if (hurt && !level().isClientSide()) {
            triggerAnim("reaction", "hurt");
        }
        return hurt;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (knockbackScale <= 0.0) {
            return;
        }
        super.knockback(strength * knockbackScale, x, z);
    }

    /**
     * M11b / 0.1-B: em vez da animacao de morte vanilla, o kaiju vira uma carcaca desmontavel (mesmo modelo, mesma
     * hitbox) no primeiro tick morto.
     */
    @Override
    protected void tickDeath() {
        if (level().isClientSide()) {
            return;
        }
        CarcassEntity.from(this).ifPresent(level()::addFreshEntity);
        remove(RemovalReason.KILLED);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(TAG_CORE_HEALTH, coreHealth);
        if (bossState != null) {
            tag.putString(TAG_BOSS, bossState.id().toString());
            tag.putInt(TAG_BOSS_PHASE, bossState.phase());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_CORE_HEALTH)) {
            coreHealth = tag.getFloat(TAG_CORE_HEALTH);
        }
        if (tag.contains(TAG_BOSS)) {
            ResourceLocation boss = ResourceLocation.tryParse(tag.getString(TAG_BOSS));
            if (boss != null) {
                bossState = new BossState(boss, tag.getInt(TAG_BOSS_PHASE));
            }
        }
    }

    // --- animacao --------------------------------------------------------------------------------------------

    protected RawAnimation loop(String layer, String name) {
        return RawAnimation.begin().thenLoop(KN8Ids.animationName(kaijuId().getPath(), layer, name));
    }

    protected RawAnimation once(String layer, String name) {
        return RawAnimation.begin().thenPlay(KN8Ids.animationName(kaijuId().getPath(), layer, name));
    }

    /** Animacao do controller "movement" (0.6-E: o Preondactyl troca por voo enquanto esta no ar). */
    protected RawAnimation movementAnimation(boolean moving, RawAnimation idle, RawAnimation walk) {
        return moving ? walk : idle;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = loop("movement", "idle");
        RawAnimation walk = loop("movement", "walk");
        RawAnimation breathe = loop("overlay", "breathe");
        controllers.add(new AnimationController<>(this, "movement", TRANSITION_TICKS,
                state -> state.setAndContinue(movementAnimation(state.isMoving(), idle, walk))));
        AnimationController<KaijuEntity> action = new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", once("action", "attack"));
        // Uma animacao disparavel por habilidade da especie; o nome e o campo "animation" do JSON da habilidade.
        for (ResourceLocation id : def().map(KaijuDef::abilities).orElse(List.of())) {
            KN8Data.ABILITY.get(id, level().isClientSide()).ifPresent(ability -> action.triggerableAnim(
                    ability.animation(), RawAnimation.begin().thenPlay(
                            kaijuId().getPath() + "." + ability.animation())));
        }
        controllers.add(action);
        controllers.add(new AnimationController<>(this, "reaction", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", once("reaction", "hurt")));
        controllers.add(new AnimationController<>(this, "overlay", 0, state -> state.setAndContinue(breathe)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
