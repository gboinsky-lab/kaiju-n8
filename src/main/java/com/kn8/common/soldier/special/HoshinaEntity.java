// src/main/java/com/kn8/common/soldier/special/HoshinaEntity.java
package com.kn8.common.soldier.special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.kn8.KN8Constants;
import com.kn8.common.ai.AiTuning;
import com.kn8.common.anim.Locomotion;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.SoldierDef;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.numbered.No9Service;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.soldier.DangerAvoid;
import com.kn8.common.soldier.MoveSafety;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SoldierTargetGoal;
import com.kn8.common.soldier.SquadMoveGoal;
import com.kn8.common.vfx.VfxKeyframes;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.combat.CombatMath;
import com.kn8.core.kaiju.DangerZone;
import com.kn8.core.power.PowerMath;
import com.kn8.core.soldier.TechniqueMath;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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

    /**
     * 0.5.0-D7 (Miguel): velocidade das tecnicas pelo Release (o servidor decide; o controller "action" de cada
     * cliente toca nessa velocidade).
     */
    private static final String TAG_TECHNIQUE_USES = "kn8_technique_uses";
    private static final String TAG_FORM_MEMORY = "kn8_form_memory";
    private static final EntityDataAccessor<Float> ACTION_SPEED =
            SynchedEntityData.defineId(HoshinaEntity.class, EntityDataSerializers.FLOAT);

    /** 0.5.0-D2: acima disso o Hoshina corre (postura de corrida baixa); abaixo, anda agachado. */
    private static final double RUN_BLOCKS_PER_TICK = 0.12;

    public static final ResourceLocation PROFILE = KN8Constants.id("hoshina");
    public static final String VARIANT = "hoshina";
    /** Tecnicas com animacao propria (hoshina.action.<id>); outras do JSON tocam o golpe basico. */
    public static final List<String> ANIMATED = List.of("kuuchi", "kosa_uchi", "ran_uchi", "kasumi_uchi", "yae_uchi",
            "kaeshi_uchi", "dash", "parry", "juni_hitoe");
    private static final float REACH_SLACK = 1.0F;
    /** Folga alem da distancia de manter em que a cobertura ainda vale (como a do goal de combate). */
    private static final double COVER_RANGE_SLACK = 3.0;
    /**
     * MOV-5B: blocos andados por unidade de velocidade do {@code dash_in} (atrito do chao do vanilla, 0,6 x 0,91:
     * v / (1 - 0,546) ~ 2,2 v).
     */
    private static final double DASH_IN_REACH = 2.2;
    /** Log de tecnica recusada no maximo uma vez por segundo (a escolha roda a cada tick). */
    private static final int LOG_EVERY_TICKS = 20;
    /** Kaeshi-uchi: o contra-golpe vem com um avanco (o dash lateral pode deixa-lo um pouco longe). */
    private static final float COUNTER_REACH_SLACK = 3.0F;
    private static final double SIDESTEP_SPEED = 0.6;
    private static final double DASH_LIFT = 0.15;
    /** MOV-4: o tiro acerta se passar a menos de meia largura + isto do centro (blocos). */
    private static final double PROJECTILE_HIT_SLACK = 0.8;
    private static final double REACTION_SEARCH = 16.0;
    /** IA-7: kaiju por perto para as reacoes (refeito a cada scanIntervalTicks). */
    private List<KaijuEntity> reactionKaiju = List.of();
    /** MOV-4: projeteis de kaiju por perto (lista refeita junto com a dos kaiju). */
    private List<KaijuProjectile> reactionProjectiles = List.of();
    private long reactionScanAt;
    private static final float SWING_VOLUME = 0.8F;
    private static final float TICKS_PER_SECOND = 20.0F;


    private final Map<String, Long> readyAt = new HashMap<>();
    /** IA-2: usos das tecnicas com limite ({@code technique_uses}; Anti-Giant da Mina = 4 tiros), salvos no mundo. */
    private final Map<String, Integer> techniqueUsed = new HashMap<>();
    /** IA-2: golpe pesado sendo aplicado agora (dentro do hurt; atravessa a pele endurecida do No. 9). */
    private boolean heavyStrike;
    private String techniqueId;
    private SpecialSoldierDef.Technique technique;
    private long techniqueStart;
    private int nextHit;
    /** MOV-5C: a ultima tecnica foi pesada (null = nenhuma ainda), para alternar rapida e pesada. */
    @Nullable
    private Boolean lastHeavy;
    /** MOV-5B: golpes da tecnica atual que acertaram. */
    private int landedHits;
    private LivingEntity techniqueTarget;
    private long dashReadyAt;
    private long counterReadyAt;
    private long parryReadyAt;
    private long invulnerableUntil;
    private long counterWindowUntil;
    /** IA-3: guarda contra golpe pesado (heavy_guard do perfil) e quantas vezes defendeu (GameTests). */
    private long heavyGuardReadyAt;
    private int heavyGuards;
    private long counterStrikeAt = -1;
    private LivingEntity counterTarget;
    private int lastVisualRelease = -1;
    private boolean profileChecked;
    /** Pontos de Release ganhos na luta (escalada de combate); nao salvo: recomeca a cada luta. */
    private float escalation;
    /** Velocidade da tecnica em andamento (fixada no inicio dela). */
    private float techniqueSpeed = 1.0F;
    /** Contadores para os GameTests (quantas vezes cada reacao saiu). */
    private int dodges;
    private int counters;
    private int parries;

    public HoshinaEntity(EntityType<? extends HoshinaEntity> type, Level level) {
        super(type, level);
    }

    // --- perfil ------------------------------------------------------------------------------------------------

    /** Distancia de manter do perfil de atirador (a do soldado comum vem do soldier_1.json; o especial nao tem). */
    @Override
    public float keepDistance() {
        return profile().flatMap(SpecialSoldierDef::ranged).map(SpecialSoldierDef.Ranged::keepDistance)
                .orElseGet(super::keepDistance);
    }

    /**
     * MOV-5D: atirador especial se esconde do kaiju que atira so sob pressao (vida abaixo de {@code cover_health}) e
     * sem uma janela para pressionar (alvo regenerando ou de guarda aberta), com o alvo na faixa de combate (longe
     * dela, persegue). Para recarregar a cobertura vale sempre.
     */
    public boolean wantsCoverFrom(LivingEntity target) {
        Optional<SpecialSoldierDef.Ranged> ranged = profile().flatMap(SpecialSoldierDef::ranged);
        if (ranged.isEmpty() || getHealth() >= getMaxHealth() * ranged.get().coverHealth()
                || edgeTo(target) > ranged.get().keepDistance() + COVER_RANGE_SLACK) {
            return false;
        }
        return !No9Service.regenerating(target) && !(target instanceof KaijuEntity kaiju && kaiju.isOpen());
    }

    public Optional<SpecialSoldierDef> profile() {
        return KN8Data.SPECIAL_SOLDIER.get(profileId(), false);
    }

    /** Perfil em special_soldier/ (0.6-F: o Hoshina com o traje numerado 10 usa outro). */
    protected ResourceLocation profileId() {
        return PROFILE;
    }

    /** Variante sincronizada (nome no menu e nos defensores de invasao). */
    protected String variantName() {
        return VARIANT;
    }

    /** Prefixo das animacoes ({@code <prefixo>.<camada>.<nome>}). */
    protected String animPrefix() {
        return VARIANT;
    }

    /** Tecnicas e reacoes com animacao propria ({@code <prefixo>.action.<id>}); as outras tocam o golpe basico. */
    protected List<String> animatedTechniques() {
        return ANIMATED;
    }

    /** Familia das posturas dos bracos ({@code <prefixo>.arms.<familia>_ready/walk/run/aim}). */
    protected String armsFamily() {
        return "blade";
    }

    /** Animacao do contra-ataque (Kaeshi-uchi no Hoshina). */
    protected String counterAnimation() {
        return "kaeshi_uchi";
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
        super.setVariant(variantName());
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
            applyHeight(def.height());
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
                setVariant(variantName());
            }
            if (getAttribute(Attributes.MAX_HEALTH).getBaseValue() != def.health()) {
                applyDefinition();
            } else if (!heightApplied(def.height())) {
                // Mundo salvo antes da tabela de alturas: so a escala muda, a vida salva fica.
                applyHeight(def.height());
            }
        });
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setVariant(variantName());
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
        // IA-1B: o mesmo alvo dos soldados comuns (detecta sem ver, cobre quem recarrega, revida).
        targetSelector.addGoal(1, new SoldierTargetGoal(this));
        goalSelector.addGoal(2, new SquadMoveGoal(this));
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
        // MOV-4: tecnica corpo a corpo so com caminho livre ate o alvo (antes saia atraves de parede).
        boolean reaches = sees || DangerAvoid.clearReach(this, target);
        // MOV-5B: quanto o alvo se afasta por tick (para nao comecar um combo que o alvo ja vai ter deixado para tras).
        Vec3 away = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
        Vec3 moved = new Vec3(target.getX() - target.xo, 0.0, target.getZ() - target.zo);
        double recede = away.lengthSqr() < 1.0E-6 ? 0.0 : moved.dot(away.normalize());
        // MOV-5F (Numbers 1 do Narumi: preve o movimento): ticks ate o impacto do golpe que o kaiju prepara (-1 sem
        // golpe ou sem a leitura no perfil) e se o golpe e contra este soldado.
        SpecialSoldierDef.Rhythm rhythm = def.get().rhythm();
        int impact = rhythm.readWindup() > 0 && target instanceof KaijuEntity reading
                ? reading.ticksToImpact().orElse(-1) : -1;
        boolean aimedAtMe = impact >= 0 && ((KaijuEntity) target).abilityTarget().orElse(null) == this;
        int margin = ServerConfig.SOLDIER_READ_WINDUP_MARGIN.get();
        List<String> valid = new ArrayList<>();
        for (Map.Entry<String, SpecialSoldierDef.Technique> entry : def.get().techniques().entrySet()) {
            SpecialSoldierDef.Technique candidate = entry.getValue();
            if ((candidate.requiresFullRelease() && !isFullRelease())
                    || now < readyAt.getOrDefault(entry.getKey(), 0L) || edge < candidate.minRange()
                    || edge > candidate.maxRange() || target.getBbHeight() < candidate.slash().minTargetHeight()
                    || (candidate.type() == SpecialSoldierDef.TechniqueType.SLASH && !sees) || !reaches
                    || usesLeft(def.get(), entry.getKey()) == 0) {
                continue;
            }
            if (!big && def.get().techniqueUses().containsKey(entry.getKey())
                    && def.get().ranged().map(SpecialSoldierDef.Ranged::reserveLimited).orElse(false)) {
                // MOV-5D: tiro de usos limitados (Anti-Giant) guardado para alvo grande.
                continue;
            }
            if (candidate.type() == SpecialSoldierDef.TechniqueType.COMBO && !TechniqueMath.stillInReach(
                    TechniqueMath.predictedEdge(edge, recede, candidate.hitTick(0) / actionSpeed()),
                    candidate.maxRange(), REACH_SLACK + candidate.dashIn() * DASH_IN_REACH)) {
                if (now % LOG_EVERY_TICKS == 0) {
                    AiTuning.logMove(this, "tecnica " + entry.getKey() + " recusada: o alvo sai do alcance");
                }
                continue;
            }
            if (aimedAtMe && !TechniqueMath.landsBefore(candidate.hitTick(0), actionSpeed(), impact, margin)) {
                // MOV-5F: o golpe do kaiju chega antes: nao entra nele (fica a esquiva/contra-ataque).
                if (now % LOG_EVERY_TICKS == 0) {
                    AiTuning.logMove(this, "tecnica " + entry.getKey() + " espera: o golpe do kaiju chega antes");
                }
                continue;
            }
            valid.add(entry.getKey());
        }
        // MOV-5B: guarda aberta do kaiju (janela depois do golpe pesado, atordoado): a tecnica que mais tira ganha
        // o bonus exploit_openings do perfil (Hoshina: explorar brechas).
        int opening = target instanceof KaijuEntity open && open.isOpen() ? def.get().tactics().exploitOpenings() : 0;
        double[] totals = valid.stream().mapToDouble(id -> totalHits(def.get().techniques().get(id))).toArray();
        double bestTotal = TechniqueMath.bestTotal(totals);
        // MOV-5C: alvo regenerando: a tecnica que acerta mais cedo ganha o bonus press_regeneration.
        int press = rhythm.pressRegeneration() > 0 && No9Service.regenerating(target) ? rhythm.pressRegeneration() : 0;
        int earliest = valid.stream().mapToInt(id -> def.get().techniques().get(id).hitTick(0)).min()
                .orElse(Integer.MAX_VALUE);
        int best = Integer.MIN_VALUE;
        List<String> tied = new ArrayList<>();
        for (int i = 0; i < valid.size(); i++) {
            String id = valid.get(i);
            SpecialSoldierDef.Technique candidate = def.get().techniques().get(id);
            int priority = candidate.priority() + (big ? candidate.honjuPriority() : 0)
                    + tacticsBonus(def.get(), id, big) + (totals[i] >= bestTotal ? opening : 0)
                    + (candidate.hitTick(0) <= earliest ? press : 0)
                    + (impact >= 0 && TechniqueMath.landsBefore(candidate.hitTick(0), actionSpeed(), impact, margin)
                            ? rhythm.readWindup() : 0)
                    + (lastHeavy != null && isHeavy(def.get(), id) != lastHeavy ? rhythm.alternateBonus() : 0);
            if (priority > best) {
                best = priority;
                tied.clear();
            }
            if (priority == best) {
                tied.add(id);
            }
        }
        tied.sort(String::compareTo);
        Optional<String> chosen = tied.isEmpty() ? Optional.empty()
                : Optional.of(tied.get(getRandom().nextInt(tied.size())));
        if (impact >= 0 && chosen.isPresent()) {
            AiTuning.logMove(this, "previsao: " + chosen.get() + " antes do impacto do kaiju (" + impact + " ticks)");
        }
        return chosen;
    }

    /** MOV-5C: a tecnica esta na lista {@code heavy} do perfil (golpe comprometido). */
    private static boolean isHeavy(SpecialSoldierDef def, String id) {
        return def.heavy().map(value -> value.techniques().contains(id)).orElse(false);
    }

    /** Dano total da tecnica (soma dos multiplicadores dos golpes; corte a distancia = o golpe x os cortes). */
    private static double totalHits(SpecialSoldierDef.Technique technique) {
        double sum = technique.hits().stream().mapToDouble(Float::doubleValue).sum();
        return technique.type() == SpecialSoldierDef.TechniqueType.SLASH
                ? technique.hits().get(0) * Math.max(1, technique.slash().count()) : sum;
    }

    /**
     * IA-3: preferencia de golpe do perfil tatico: tecnicas pesadas contra alvo grande ({@code heavy_vs_big}), as
     * outras contra Yoju ({@code fast_vs_small}).
     */
    private static int tacticsBonus(SpecialSoldierDef def, String id, boolean big) {
        boolean heavy = def.heavy().map(value -> value.techniques().contains(id)).orElse(false);
        SpecialSoldierDef.Tactics tactics = def.tactics();
        return heavy && big ? tactics.heavyVsBig() : !heavy && !big ? tactics.fastVsSmall() : 0;
    }

    /** Usos que restam da tecnica (-1 = sem limite). */
    public int usesLeft(SpecialSoldierDef def, String id) {
        Integer limit = def.techniqueUses().get(id);
        return limit == null ? -1 : Math.max(0, limit - techniqueUsed.getOrDefault(id, 0));
    }

    /** IA-2: a tecnica em andamento e pesada (lista {@code heavy.techniques} do perfil)? */
    private Optional<SpecialSoldierDef.Heavy> heavyTechnique() {
        return techniqueId == null ? Optional.empty()
                : profile().flatMap(SpecialSoldierDef::heavy).filter(heavy -> heavy.techniques().contains(techniqueId));
    }

    /** Golpe pesado sendo aplicado agora (lido pela pele endurecida do No. 9). */
    public boolean isHeavyStrike() {
        return heavyStrike;
    }

    /** Comeca uma tecnica do perfil contra o alvo (goal, comando, testes). */
    public boolean startTechnique(String id, LivingEntity target) {
        Optional<SpecialSoldierDef.Technique> found = profile().map(def -> def.techniques().get(id));
        if (found.isEmpty() || isUsingTechnique() || isActing() || weapon().isEmpty()
                || (found.get().requiresFullRelease() && !isFullRelease())
                || profile().map(def -> usesLeft(def, id)).orElse(-1) == 0) {
            return false;
        }
        if (profile().map(def -> def.techniqueUses().containsKey(id)).orElse(false)) {
            techniqueUsed.merge(id, 1, Integer::sum);
        }
        long now = level().getGameTime();
        technique = found.get();
        techniqueId = id;
        lastHeavy = profile().map(def -> isHeavy(def, id)).orElse(false);
        techniqueStart = now;
        techniqueTarget = target;
        nextHit = 0;
        landedHits = 0;
        techniqueSpeed = actionSpeed();
        readyAt.put(id, now + CombatMath.faster(technique.durationTicks(), techniqueSpeed)
                + scaledCooldown(technique.cooldownTicks()));
        getNavigation().stop();
        lookAt(target, 360.0F, 90.0F);
        if (technique.dashIn() > 0) {
            dashSafe(target.position().subtract(position()), technique.dashIn(), "avanco da tecnica " + id);
        }
        triggerAnim("action", animatedTechniques().contains(id) ? id : "attack");
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
        // Tecnica de Full Release (Juni-hitoe) nao e interrompida por esquiva nem contra-ataque.
        if (isAlive() && !(technique != null && technique.requiresFullRelease())) {
            react(now);
        }
        if (isAlive()) {
            tickExtra(now);
        }
        if (isAlive()) {
            tickForm(now);
        }
    }

    /** Ticks seguidos sem alvo (volta da forma Kaiju No. 8 para o Kafka). */
    private int idleTicks;
    /** MOV-5A: arma e municao das outras formas, pelo id do tipo de entidade (salvo no mundo). */
    private CompoundTag formMemory = new CompoundTag();

    /**
     * 0.7-F: regeneracao e troca de forma do perfil ({@code regeneration}/{@code transform}), no servidor. A troca
     * cria a outra entidade no mesmo lugar (vida cheia, mesmo alvo), com fumaca, explosao sem dano e rugido, e some.
     */
    private void tickForm(long now) {
        Optional<SpecialSoldierDef> def = profile();
        if (def.isEmpty()) {
            return;
        }
        def.get().regeneration().filter(regen -> now % 20 == 0 && getHealth() < getMaxHealth() * regen.healthBelow())
                .ifPresent(regen -> {
                    heal(getMaxHealth() * regen.perSecond());
                    // VFX-2: um pulso por segundo enquanto cura (No. 8: pontos ciano entrando no corpo).
                    VfxService.play((ServerLevel) level(), VfxService.REGENERATE,
                            position().add(0, getBbHeight() * 0.5, 0), Vec3.ZERO, 1.0F, 0.0F, this, "regenerate");
                });
        idleTicks = getTarget() == null ? idleTicks + 1 : 0;
        def.get().transform().ifPresent(transform -> {
            boolean hurt = transform.healthBelow() > 0.0F && getHealth() < getMaxHealth() * transform.healthBelow();
            boolean calm = transform.revertAfterIdleTicks() > 0 && idleTicks >= transform.revertAfterIdleTicks();
            if (hurt || calm) {
                transformInto(transform.into());
            }
        });
    }

    private void transformInto(ResourceLocation id) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Entity other = BuiltInRegistries.ENTITY_TYPE.get(id).create(server);
        if (other == null) {
            return;
        }
        other.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        if (other instanceof Mob mob && getTarget() != null) {
            mob.setTarget(getTarget());
        }
        if (other instanceof HoshinaEntity next) {
            next.inheritForm(this);
            InvasionService.replaceDefender(server, getUUID(), next.getUUID());
            AiTuning.logMove(next, "perfil ativo " + next.activeProfileId() + " (antes " + activeProfileId() + ")");
        }
        // Duelo de teste (/kn8 duel): a forma nova herda o oponente e o oponente passa a mirar nela.
        duelOpponent().ifPresent(foe -> {
            other.getPersistentData().putUUID(DUEL_TAG, foe);
            if (server.getEntity(foe) instanceof SoldierEntity rival) {
                rival.getPersistentData().putUUID(DUEL_TAG, other.getUUID());
            }
        });
        server.addFreshEntity(other);
        // VFX-2: o efeito e da forma que surge (receita "transform" do perfil dela: Kafka -> No. 8 racha em ciano e
        // fecha com o anel de choque; No. 8 -> Kafka apaga as linhas); sem receita, estouro e fumaca.
        VfxService.play(server, VfxService.TRANSFORM, position(), getLookAngle(), 1.0F, 0.3F, other, "transform");
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 1.5F, 0.8F);
        discard();
    }

    /**
     * MOV-5A (bug 10 da MOV-0): o que passa de uma forma para a outra. Passam o esquadrao (lider, ordem, posto, kaiju
     * designado), os dados persistentes (invasao, duelo) e os usos de tecnica limitados. Nao passam tecnicas,
     * atributos, arma nem escalada: cada forma luta so com o proprio perfil (nada herdado). A arma e a municao da forma
     * que saiu ficam guardadas na memoria de formas e voltam quando ela voltar (o Kafka volta com os pentes que tinha,
     * ou com a faca se ja tinha ficado sem municao). [SUPOSICAO] a escalada nao passa: o Kafka nao escala e o No. 8 so
     * volta ao Kafka depois de um tempo sem alvo, quando a escalada ja teria caido.
     */
    protected void inheritForm(HoshinaEntity from) {
        inheritSquadFrom(from);
        techniqueUsed.putAll(from.techniqueUsed);
        formMemory = from.formMemory.copy();
        formMemory.put(formKey(from), from.weaponState());
        String mine = formKey(this);
        if (formMemory.contains(mine)) {
            restoreWeaponState(formMemory.getCompound(mine));
            formMemory.remove(mine);
        }
    }

    /** Perfil em uso por esta forma (log da IA, GameTests). */
    public ResourceLocation activeProfileId() {
        return profileId();
    }

    /** Formas guardadas (GameTests): ids de tipo de entidade com arma e municao esperando a volta. */
    public Set<String> storedForms() {
        return formMemory.getAllKeys();
    }

    private static String formKey(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    /** 0.6-F: comportamento extra por tick no servidor (a cauda do traje numerado 10). */
    protected void tickExtra(long now) {
    }

    /** 0.6-F: Full Release (traje numerado 10 a 100%); o Hoshina comum nunca chega. */
    public boolean isFullRelease() {
        return false;
    }

    /** Fator das recargas (escalada a parte): o Full Release encurta. */
    protected double cooldownFactor() {
        return 1.0;
    }

    /** Fator da velocidade sobre o bonus do Release: o Full Release acelera. */
    protected double speedFactor() {
        return 1.0;
    }

    public Optional<SpecialSoldierDef.Technique> currentTechniqueDef() {
        return Optional.ofNullable(technique);
    }

    private void tickTechnique(long now) {
        LivingEntity target = techniqueTarget;
        // 0.5.0-D7: com mais Release os golpes da tecnica saem antes (tempo da tecnica = tempo real x velocidade).
        int elapsed = (int) ((now - techniqueStart) * techniqueSpeed);
        int count = technique.type() == SpecialSoldierDef.TechniqueType.SLASH ? 1 : technique.hits().size();
        if (target != null && target.isAlive()) {
            getLookControl().setLookAt(target, 60.0F, 60.0F);
            String invalid = nextHit == 0 && technique.type() == SpecialSoldierDef.TechniqueType.SLASH
                    ? shotInvalid(target, elapsed >= technique.hitTick(0)) : null;
            if (invalid != null) {
                cancelTechnique(now, invalid);
                return;
            }
            // MOV-5B (bug 11 da MOV-0): no maximo um golpe por tick, mesmo com o Release acelerando a tecnica; cada
            // corte e um impacto separado.
            if (nextHit < count && elapsed >= technique.hitTick(nextHit)) {
                fireHit(target, nextHit, count);
                nextHit++;
            }
        } else if (nextHit < count) {
            // MOV-4: o alvo morreu (ou sumiu) no meio da tecnica: corta a tecnica e a animacao em vez de golpear o ar
            // ate o fim; o goal escolhe outro alvo.
            cancelTechnique(now, "alvo morto");
            return;
        }
        if (elapsed >= technique.durationTicks() && nextHit >= count) {
            missPenalty(now);
            technique = null;
            techniqueId = null;
            techniqueTarget = null;
        }
    }

    /**
     * MOV-5D (plano v3, ciclo de vida: "validacao final: alvo e espaco continuam validos"): o tiro da tecnica so sai
     * com linha de tiro e o alvo ao alcance ({@code max_range} + folga) no tick do disparo; atirador ({@code ranged})
     * tambem cancela no preparo se o alvo chegar mais perto que o {@code min_range}. Null = tiro valido.
     */
    @Nullable
    private String shotInvalid(LivingEntity target, boolean firing) {
        double edge = edgeTo(target);
        if (profile().flatMap(SpecialSoldierDef::ranged).isPresent() && edge < technique.minRange()) {
            return "alvo perto demais";
        }
        if (!firing) {
            return null;
        }
        if (!getSensing().hasLineOfSight(target)) {
            return "sem linha de tiro";
        }
        return edge > technique.maxRange() + REACH_SLACK ? "alvo fora do alcance" : null;
    }

    /**
     * Corta a tecnica antes do primeiro golpe e da animacao. Antes do primeiro golpe nada foi gasto: o uso limitado
     * volta e a tecnica fica pronta depois do preparo dela (pode tentar de novo de outra posicao).
     */
    private void cancelTechnique(long now, String reason) {
        AiTuning.logMove(this, "tecnica " + techniqueId + " cortada: " + reason);
        stopTriggeredAnim("action", animatedTechniques().contains(techniqueId) ? techniqueId : "attack");
        if (nextHit == 0) {
            if (profile().map(def -> def.techniqueUses().containsKey(techniqueId)).orElse(false)) {
                techniqueUsed.computeIfPresent(techniqueId, (id, used) -> used > 1 ? used - 1 : null);
            }
            readyAt.put(techniqueId, now + technique.windupTicks());
        }
        technique = null;
        techniqueId = null;
        techniqueTarget = null;
    }

    /**
     * MOV-5B: combo que terminou sem acertar nenhum golpe (o alvo saiu do alcance ou ficou atras de parede): espera
     * extra ({@code tactics.miss_cooldown_ticks}) antes de repetir a mesma tecnica; o goal reposiciona nesse tempo.
     */
    private void missPenalty(long now) {
        int extra = profile().map(def -> def.tactics().missCooldownTicks()).orElse(0);
        if (technique.type() != SpecialSoldierDef.TechniqueType.COMBO || landedHits > 0 || extra <= 0) {
            return;
        }
        readyAt.put(techniqueId, Math.max(readyAt.getOrDefault(techniqueId, now), now) + extra);
        AiTuning.logMove(this, "tecnica " + techniqueId + " errou todos os golpes: espera " + extra + " ticks");
    }

    /** Tick em que a tecnica fica pronta de novo (GameTests). */
    public long techniqueReadyAt(String id) {
        return readyAt.getOrDefault(id, 0L);
    }

    /** Golpes da tecnica atual que acertaram (GameTests). */
    public int landedHits() {
        return landedHits;
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
            Optional<SpecialSoldierDef.Heavy> heavy = heavyTechnique();
            SlashProjectile.fire(this, aim, technique.slash(), base * technique.hits().get(0), kaijuDamageMultiplier(),
                    heavy.map(SpecialSoldierDef.Heavy::power).orElse(0),
                    heavy.map(SpecialSoldierDef.Heavy::radius).orElse(0.0F));
            // VFX-2: a tecnica a distancia (golpe no chao, rugido) sai do corpo de quem a faz; so desenha se o perfil
            // visual tiver receita para ela (o rastro do corte continua o do SlashProjectile).
            VfxService.play((ServerLevel) level(), VfxService.TECHNIQUE, position(), aim, 1.0F, 0.0F, this,
                    techniqueId == null ? "" : techniqueId);
            level().playSound(null, getX(), getEyeY(), getZ(), KN8Sounds.SWORD_HEAVY.get(), SoundSource.NEUTRAL,
                    SWING_VOLUME, 1.1F);
            return;
        }
        boolean last = index == count - 1;
        // VFX-3: cada golpe com a propria receita ("yae_uchi/3"; sem ela, a da tecnica).
        if (strike(target, technique.hits().get(index), last ? technique.finalKnockback() : 0.0F, weapon.get(),
                REACH_SLACK, techniqueId + "/" + (index + 1))) {
            landedHits++;
        }
        if (last && technique.exposeCoreTicks() > 0 && target instanceof KaijuEntity kaiju) {
            // Yae-uchi: abre a guarda do kaiju (nucleo exposto) para os aliados aproveitarem.
            kaiju.exposeCore(technique.exposeCoreTicks());
        }
        if (technique.sidestepBeforeLast() && index == count - 2) {
            // Kasumi-uchi: o segundo corte distrai; um passo para o lado e o terceiro vem forte.
            Vec3 toTarget = target.position().subtract(position());
            Vec3 side = new Vec3(-toTarget.z, 0, toTarget.x);
            Vec3 first = getRandom().nextBoolean() ? side : side.scale(-1);
            if (!dashSafe(first, SIDESTEP_SPEED, "passo lateral")) {
                dashSafe(first.scale(-1), SIDESTEP_SPEED, "passo lateral");
            }
        }
    }

    /**
     * Golpe corpo a corpo de tecnica: acerta se a borda do alvo estiver ao alcance da arma (com folga: os combos
     * avancam junto). Kaiju levam no corpo, com o {@code kaiju_damage} do perfil.
     */
    protected boolean strike(LivingEntity target, float multiplier, float knockback, WeaponDef weapon, float slack) {
        return strike(target, multiplier, knockback, weapon, slack, technique != null && techniqueId != null
                ? techniqueId : "");
    }

    /**
     * VFX-2: {@code vfxKey} = receita do perfil visual para o golpe (tecnica, {@code counter}, {@code tail}); sem
     * receita, o corte comum com a paleta do personagem.
     */
    protected boolean strike(LivingEntity target, float multiplier, float knockback, WeaponDef weapon, float slack,
            String vfxKey) {
        // MOV-4: nada de acertar atraves de parede (antes media so a distancia).
        if (edgeTo(target) > weapon.reach() + slack || !DangerAvoid.clearReach(this, target)) {
            return false;
        }
        float damage = (float) (weapon.baseDamage() * multiplier * damageMultiplier());
        if (target instanceof KaijuEntity) {
            damage *= kaijuDamageMultiplier();
        }
        // Combos de 1-2 ticks entre golpes: a invulnerabilidade vanilla (10 ticks) engolia quase todos (como no
        // multi_hit dos kaiju, 0.6-A); a tecnica ja acerta uma vez por golpe do JSON.
        target.invulnerableTime = 0;
        // [SUPOSICAO] tecnica de Full Release (Juni-hitoe) "ignora parte da defesa": dano que passa pela armadura.
        DamageSource source = technique != null && technique.requiresFullRelease()
                ? damageSources().indirectMagic(this, this) : damageSources().mobAttack(this);
        Optional<SpecialSoldierDef.Heavy> heavy = heavyTechnique();
        heavyStrike = heavy.isPresent();
        boolean hurt;
        try {
            hurt = target.hurt(source, damage);
        } finally {
            heavyStrike = false;
        }
        if (!hurt) {
            return false;
        }
        ServerLevel level = (ServerLevel) level();
        // IA-2 (Miguel): golpe pesado dos especiais pesados quebra o que estiver em volta do ponto do golpe.
        heavy.ifPresent(value -> DestructionService.request(level, target.position(),
                new AbilityDef.Destruction(value.radius(), value.power(), false, 1.0F), this));
        Vec3 aim = target.getBoundingBox().getCenter().subtract(getEyePosition()).normalize();
        // VFX-3: o corte sai na superficie do alvo voltada para quem golpeia (no centro de um kaiju de 8 m ficava
        // escondido dentro do corpo).
        Vec3 eye = getEyePosition();
        AABB box = target.getBoundingBox();
        Vec3 contact = new Vec3(Mth.clamp(eye.x, box.minX, box.maxX), Mth.clamp(eye.y, box.minY, box.maxY),
                Mth.clamp(eye.z, box.minZ, box.maxZ));
        VfxService.play(level, VfxService.SLASH, contact, aim, 0.8F, 0.0F, this, vfxKey);
        // VFX-4B: marca do golpe no chao, aos pes de quem golpeia na direcao do alvo (machado da Kikoru), so com
        // receita "<tecnica>_<n>_ground" ("/" viraria a receita do golpe de novo).
        String groundKey = vfxKey.replace('/', '_') + "_ground";
        if (VfxService.hasRecipe(this, groundKey)) {
            VfxService.play(level, VfxService.TECHNIQUE, new Vec3(contact.x, getY(), contact.z), aim, 1.0F, 0.0F, this,
                    groundKey);
        }
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

    /**
     * MOV-1: impulso so se o pouso for valido (sem parede logo a frente, com chao ate a queda maxima); senao nao sai
     * do lugar. Verdadeiro se saiu.
     */
    private boolean dashSafe(Vec3 direction, double speed, String what) {
        if (MoveSafety.landing(this, direction, MoveSafety.dashDistance(speed)).isEmpty()) {
            AiTuning.logMove(this, what + " sem pouso valido");
            return false;
        }
        dash(direction, speed);
        return true;
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
        SpecialSoldierDef.Tactics tactics = def.get().tactics();
        // IA-3 (bug #16 da auditoria): atirador nao usa o contra-ataque de espada (Mina e Reno davam dash para dentro
        // do kaiju). O contra-ataque continua cortando a tecnica em andamento: contra golpe pesado e a defesa.
        boolean canCounter = tactics.meleeCounter();
        if (canCounter && now < counterWindowUntil && now >= counterReadyAt && getTarget() != null
                && getTarget().isAlive()) {
            counterWindowUntil = 0;
            startCounter(getTarget(), counter, now);
            return;
        }
        // IA-7: a lista de kaiju por perto e refeita a cada [kaijuAi] scanIntervalTicks (antes, todo tick); o tempo
        // ate o impacto de cada um continua lido todo tick (a reacao nao atrasa).
        if (now >= reactionScanAt) {
            reactionScanAt = now + ServerConfig.AI_SCAN_INTERVAL.get();
            reactionKaiju = level().getEntitiesOfClass(KaijuEntity.class, getBoundingBox().inflate(REACTION_SEARCH),
                    KaijuEntity::isAlive);
            reactionProjectiles = level().getEntitiesOfClass(KaijuProjectile.class, getBoundingBox().inflate(
                    ServerConfig.SOLDIER_PROJECTILE_DODGE_RANGE.get()), Entity::isAlive);
        }
        // MOV-4: tiro de kaiju que vai acertar (antes a esquiva so lia golpes de kaiju): de lado para fora da linha.
        if (now >= dashReadyAt && dodgeProjectile(dashDef, tactics, now)) {
            return;
        }
        for (KaijuEntity kaiju : reactionKaiju) {
            if (!kaiju.isAlive() || !threatens(kaiju)) {
                continue;
            }
            int ticks = kaiju.ticksToImpact().orElse(Integer.MAX_VALUE);
            if (canCounter && kaiju.isPreparingHeavy() && ticks <= counter.reactTicks() && now >= counterReadyAt) {
                startCounter(kaiju, counter, now);
                return;
            }
            if (ticks <= dashDef.reactTicks() && now >= dashReadyAt) {
                if (getRandom().nextFloat() < tactics.dodgeChance()) {
                    dodge(kaiju, dashDef, now);
                } else {
                    // IA-3: nao esquivou deste golpe; nao sorteia de novo a cada tick ate ele sair.
                    dashReadyAt = now + dashDef.reactTicks() + 1;
                }
                return;
            }
        }
    }

    /**
     * MOV-4: algum projetil de kaiju por perto passa por ele em ate {@code react_ticks}: esquiva (pela chance do
     * perfil) para o lado de fora da linha do tiro. Verdadeiro se reagiu (esquivou ou decidiu nao esquivar deste).
     */
    private boolean dodgeProjectile(SpecialSoldierDef.Dash def, SpecialSoldierDef.Tactics tactics, long now) {
        Vec3 center = getBoundingBox().getCenter();
        for (KaijuProjectile projectile : reactionProjectiles) {
            Vec3 velocity = projectile.getDeltaMovement();
            double speed2 = velocity.lengthSqr();
            if (!projectile.isAlive() || speed2 < 1.0E-4) {
                continue;
            }
            Vec3 toMe = center.subtract(projectile.position());
            double ticks = toMe.dot(velocity) / speed2;
            if (ticks <= 0.0 || ticks > def.reactTicks()) {
                continue;
            }
            Vec3 miss = toMe.subtract(velocity.scale(ticks));
            if (miss.length() > getBbWidth() * 0.5 + PROJECTILE_HIT_SLACK) {
                continue;
            }
            if (getRandom().nextFloat() < tactics.dodgeChance()) {
                Vec3 side = new Vec3(miss.x, 0.0, miss.z);
                if (side.lengthSqr() < 1.0E-4) {
                    side = new Vec3(-velocity.z, 0.0, velocity.x);
                }
                AiTuning.logMove(this, "esquiva de projetil");
                dodgeTowards(side, def, now);
            } else {
                dashReadyAt = now + def.reactTicks() + 1;
            }
            return true;
        }
        return false;
    }

    /** O golpe em preparo e contra ele: alvo da habilidade, ou ele perto o bastante do kaiju para ser pego. */
    private boolean threatens(KaijuEntity kaiju) {
        if (kaiju.abilityTarget().map(target -> target == this).orElse(false)) {
            return true;
        }
        // MOV-4: ou dentro da area real do golpe pesado (a do aviso no chao).
        return kaiju.isUsingAbility() && (kaiju.edgeDistance(this) <= kaiju.meleeReach() + 2.0
                || ServerConfig.SOLDIER_AVOID_DANGER.get() && kaiju.dangerZone().map(zone -> zone.contains(getX(),
                        getZ(), ServerConfig.SOLDIER_DANGER_MARGIN.get())).orElse(false));
    }

    private void dodge(KaijuEntity kaiju, SpecialSoldierDef.Dash def, long now) {
        // MOV-1: o lado sorteado, o outro e, por ultimo, para tras; so para onde ha pouso (antes: lado sorteado as
        // cegas, contra parede ou para dentro de um buraco).
        // MOV-4: dentro da area do golpe pesado, a esquiva sai pelo caminho mais curto para fora dela.
        Vec3 away = position().subtract(kaiju.position());
        Optional<DangerZone> zone = kaiju.dangerZone().filter(z -> ServerConfig.SOLDIER_AVOID_DANGER.get()
                && z.contains(getX(), getZ(), ServerConfig.SOLDIER_DANGER_MARGIN.get()));
        if (zone.isPresent()) {
            double[] exit = zone.get().exit(getX(), getZ(), away.x, away.z);
            away = new Vec3(exit[0], 0.0, exit[1]);
        }
        dodgeTowards(away, def, now);
    }

    /** Esquiva para longe de {@code away} (lados, depois tras), so com pouso valido. */
    private void dodgeTowards(Vec3 away, SpecialSoldierDef.Dash def, long now) {
        Optional<Vec3> dir = MoveSafety.dodgeDirection(this, away, getRandom().nextBoolean(),
                MoveSafety.dashDistance(def.speed()));
        if (dir.isEmpty()) {
            AiTuning.logMove(this, "esquiva sem espaco");
            dashReadyAt = now + def.reactTicks() + 1;
            return;
        }
        dash(dir.get(), def.speed());
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
        if (!dashSafe(target.position().subtract(position()), def.get().speed(), "investida")) {
            return false;
        }
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
        Vec3 forward = new Vec3(toEnemy.x, 0, toEnemy.z).normalize();
        Vec3 side = new Vec3(-toEnemy.z, 0, toEnemy.x).normalize().scale(getRandom().nextBoolean() ? 1 : -1);
        // MOV-1: passa pelo lado sorteado, pelo outro ou fica (o contra-ataque sai mesmo assim).
        if (!dashSafe(side.add(forward.scale(0.8)), def.dashSpeed(), "contra-ataque")) {
            dashSafe(side.scale(-1).add(forward.scale(0.8)), def.dashSpeed(), "contra-ataque (outro lado)");
        }
        counterReadyAt = now + def.cooldownTicks();
        invulnerableUntil = now + def.invulnerableTicks();
        counterStrikeAt = now + def.strikeDelayTicks();
        counterTarget = enemy;
        counters++;
        triggerAnim("action", counterAnimation());
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
        dashSafe(target.position().subtract(position()), lunge, "estocada do contra-ataque");
        strike(target, multiplier, 0.0F, weapon.get(), COUNTER_REACH_SLACK, "counter");
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
        float dealt = heavyGuard(source, amount, now);
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
            VfxService.play(level, VfxService.PARRY, at, getLookAngle(), 1.0F, 0.0F, this);
            level.playSound(null, getX(), getEyeY(), getZ(), KN8Sounds.PARRY.get(), SoundSource.NEUTRAL, 1.0F,
                    1.1F);
        }
        return super.hurt(source, dealt);
    }

    /**
     * IA-3 (Miguel: os mais fortes "conseguem defender mas ainda tomam dano"): golpe pesado de kaiju vindo de frente
     * cai para o {@code factor} do {@code heavy_guard}; uma vez a cada {@code cooldown_ticks}.
     */
    private float heavyGuard(DamageSource source, float amount, long now) {
        Optional<SpecialSoldierDef.HeavyGuard> guard = profile().flatMap(SpecialSoldierDef::heavyGuard);
        Vec3 from = source.getSourcePosition();
        if (guard.isEmpty() || from == null || now < heavyGuardReadyAt || !(source.getEntity() instanceof KaijuEntity)
                || !CombatService.isHeavyHit(source)) {
            return amount;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 to = from.subtract(position());
        double cos = (look.x * to.x + look.z * to.z) / Math.max(1.0E-6, Math.hypot(look.x, look.z) * Math.hypot(to.x,
                to.z));
        if (cos < Math.cos(Math.toRadians(guard.get().frontArcDegrees() / 2.0))) {
            return amount;
        }
        heavyGuardReadyAt = now + guard.get().cooldownTicks();
        heavyGuards++;
        triggerAnim("action", "parry");
        ServerLevel level = (ServerLevel) level();
        Vec3 at = getEyePosition().add(getLookAngle().scale(0.6));
        VfxService.play(level, VfxService.PARRY, at, getLookAngle(), 1.3F, 0.0F, this);
        level.playSound(null, getX(), getEyeY(), getZ(), KN8Sounds.PARRY.get(), SoundSource.NEUTRAL, 1.0F, 0.8F);
        return amount * guard.get().factor();
    }

    /** Golpes pesados defendidos com a guarda (GameTests). */
    public int heavyGuards() {
        return heavyGuards;
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

    /** Fixa a escalada (comando e testes), limitada ao maximo do perfil. */
    public void setEscalation(float points) {
        float max = profile().map(def -> (float) def.escalation().maxPoints()).orElse(0.0F);
        escalation = Mth.clamp(points, 0.0F, max);
    }

    /** Recarga encurtada pela escalada (ate cooldown_reduction_at_max na escalada maxima). */
    protected long scaledCooldown(int ticks) {
        long scaled = profile().map(SpecialSoldierDef::escalation).filter(def -> def.maxPoints() > 0)
                .map(def -> Math.round(ticks * (1.0 - def.cooldownReductionAtMax() * escalation / def.maxPoints())))
                .orElse((long) ticks);
        return Math.round(scaled * cooldownFactor());
    }

    /** Velocidade do perfil com o bonus do Release atual (o soldado comum so aplica no nascimento). */
    private void applySpeed(int release) {
        profile().ifPresent(def -> getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(def.speed()
                * (1.0 + PowerMath.speedBonus(release, PowerService.params())) * speedFactor()));
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
            entityData.set(ACTION_SPEED, actionSpeed());
        }
    }

    /** 0.5.0-D7: fator de velocidade das tecnicas pelo Release atual ({@code [combat] attackSpeedAtFullRelease}). */
    public float actionSpeed() {
        if (!ServerConfig.SPEC.isLoaded()) {
            return 1.0F;
        }
        return (float) CombatMath.releaseSpeed(release(), ServerConfig.ATTACK_SPEED_AT_FULL_RELEASE.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION_SPEED, 1.0F);
    }

    // --- GeckoLib ----------------------------------------------------------------------------------------------

    /** Andando rapido (blocos por tick no ultimo tick): troca para a corrida baixa (cliente e servidor). */
    private boolean running() {
        return Math.hypot(getX() - xo, getZ() - zo) > RUN_BLOCKS_PER_TICK;
    }

    /**
     * 0.7-D: nome da animacao de movimento ({@code <prefixo>.movement.<nome>}) para o estado atual; a Kikoru com a arma
     * numerada 4 troca por "fly" quando esta no ar.
     */
    protected String movementName(boolean moving) {
        return !moving ? "idle" : running() ? "run" : "walk";
    }

    /** 0.7-D: controllers a mais da especie (as asas da Numbers 4). */
    protected void registerExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    /** Ticks de transicao do controller "action" (tecnicas). */
    private static final int ACTION_TRANSITION_TICKS = 3;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = animPrefix() + ".";
        // 0.5.0-D2 (referencias do Miguel): postura baixa parado, andando agachado e correndo com os bracos para tras.
        Map<String, RawAnimation> movement = new HashMap<>();
        Map<String, RawAnimation> arms = new HashMap<>();
        for (String stance : new String[] {"ready", "walk", "run", "aim"}) {
            arms.put(stance, RawAnimation.begin().thenLoop(prefix + "arms." + armsFamily() + "_" + stance));
        }
        controllers.add(Locomotion.drive(new AnimationController<>(this, "movement", 3, state ->
                state.setAndContinue(movement.computeIfAbsent(movementName(state.isMoving()),
                        name -> RawAnimation.begin().thenLoop(prefix + "movement." + name)))), Locomotion::typeId));
        controllers.add(new AnimationController<>(this, "arms", 4, state -> {
            String stance = isAggressive() && !running() ? "aim"
                    : state.isMoving() ? (running() ? "run" : "walk") : "ready";
            return state.setAndContinue(arms.get(stance));
        }));
        // 0.5.0-D7 (Miguel: "paradas muito bruscas"): o golpe entra misturando com a postura em vez de cortar seco.
        AnimationController<HoshinaEntity> action = new AnimationController<>(this, "action", ACTION_TRANSITION_TICKS,
                state -> PlayState.STOP);
        // 0.5.0-D7: as tecnicas aceleram com o Release, junto com os golpes no servidor.
        action.setAnimationSpeedHandler(hoshina -> (double) hoshina.getEntityData().get(ACTION_SPEED));
        action.triggerableAnim("attack", RawAnimation.begin().thenPlay(prefix + "action.attack"));
        // MOV-2: salto tatico (gen_vault_animations.py).
        action.triggerableAnim("vault", RawAnimation.begin().thenPlay(prefix + "action.vault"));
        for (String id : animatedTechniques()) {
            action.triggerableAnim(id, RawAnimation.begin().thenPlay(prefix + "action." + id));
        }
        // VFX-1: marcas de efeito das animacoes (particle_effects) no quadro certo, no cliente.
        controllers.add(VfxKeyframes.attach(action));
        controllers.add(new AnimationController<>(this, "reaction", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(prefix + "reaction.hurt")));
        registerExtraControllers(controllers);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag uses = new CompoundTag();
        techniqueUsed.forEach(uses::putInt);
        tag.put(TAG_TECHNIQUE_USES, uses);
        tag.put(TAG_FORM_MEMORY, formMemory);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        techniqueUsed.clear();
        CompoundTag uses = tag.getCompound(TAG_TECHNIQUE_USES);
        for (String key : uses.getAllKeys()) {
            techniqueUsed.put(key, uses.getInt(key));
        }
        formMemory = tag.getCompound(TAG_FORM_MEMORY);
    }
}
