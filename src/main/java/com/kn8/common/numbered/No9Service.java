package com.kn8.common.numbered;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.boss.BossService;
import com.kn8.common.career.CareerService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedDef;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * O que o Kaiju No. 9 faz alem de lutar (0.2, Etapa 8; numeros no {@code numbered/kaiju_no9.json}):
 * <ul>
 *   <li><b>reviver</b>: para perto de uma carcaca da tabela {@code revive}, faz o gesto por {@code revive_cast_ticks}
 *   e a carcaca levanta como a especie revivida (primigenius -> ressurgido, Honju -> revivido);</li>
 *   <li><b>comandar</b>: os kaiju por perto sem alvo atacam o alvo dele;</li>
 *   <li><b>fugir</b>: com a vida em {@code flee_health} ele some (vilao recorrente) e quem lutava ganha merito.</li>
 * </ul>
 */
public final class No9Service {

    /** Marca nos kaiju levantados pelo No. 9. */
    public static final String KN8_REVIVED = KN8Constants.MOD_ID + "_revived";
    /** 0.3: este No. 9 veio numa onda {@code mass_revive}; e ja revivia o exercito (uma vez so). */
    public static final String MASS_TAG = KN8Constants.MOD_ID + "_mass_revive";
    private static final String MASS_DONE_TAG = KN8Constants.MOD_ID + "_mass_revive_done";
    private static final int THINK_INTERVAL = 10;
    private static final float TICKS_PER_SECOND = 20.0F;
    private static final int REGEN_PARTICLE_INTERVAL = 10;
    private static final int PARTICLE_INTERVAL = 4;
    private static final double ANNOUNCE_RADIUS = 64.0;
    private static final double FLEE_REWARD_RADIUS = 48.0;
    /** Verde-acido das costuras dos revividos (a "assinatura" do No. 9). */
    private static final int ABSORB_RETRY_TICKS = 100;
    private static final double ABSORB_PULL_SPEED = 0.35;
    private static final DustParticleOptions TENDRIL = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.05F),
            1.3F);
    private static final DustParticleOptions HARDEN = new DustParticleOptions(new Vector3f(0.12F, 0.12F, 0.14F),
            1.6F);
    private static final DustParticleOptions ACID = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.2F), 1.4F);

    private No9Service() {
    }

    public static Optional<NumberedDef> def(KaijuEntity kaiju) {
        return KN8Data.NUMBERED.get(kaiju.kaijuId(), false);
    }

    static void tick(KaijuNo9Entity no9) {
        Optional<NumberedDef> found = def(no9);
        if (found.isEmpty() || !(no9.level() instanceof ServerLevel level)) {
            return;
        }
        NumberedDef def = found.get();
        if (!no9.fled && no9.getHealth() <= no9.getMaxHealth() * def.fleeHealth()) {
            flee(level, no9, def);
            return;
        }
        long now = level.getGameTime();
        regenerate(level, no9, def.regeneration());
        if (tickForms(level, no9, def, now)) {
            return;
        }
        if (no9.getPersistentData().getBoolean(MASS_TAG)) {
            // 0.3: onda de ressurreicao em massa; o gesto normal (uma carcaca por vez) fica desligado.
            tickMass(level, no9, def, now);
            if (no9.tickCount % THINK_INTERVAL == 0) {
                command(level, no9, def);
            }
            return;
        }
        if (no9.reviving != null) {
            tickRevive(level, no9, def, now);
            return;
        }
        if (no9.tickCount % THINK_INTERVAL != 0) {
            return;
        }
        command(level, no9, def);
        no9.revived.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });
        if (now >= no9.nextReviveAt && no9.revived.size() < def.maxRevivedAlive() && !no9.isUsingAbility()) {
            nearestCarcass(level, no9, def).ifPresent(carcass -> startRevive(level, no9, def, carcass, now));
        }
    }

    // --- formas (0.7-E) --------------------------------------------------------------------------------------------

    /**
     * Mudanca de forma e absorcao (Biblioteca v22 secao 33). Fica de fora durante a ressurreicao em massa e o gesto
     * de reviver. Devolve true quando o No. 9 esta ocupado (absorvendo) ou deixou de existir (mudou de forma).
     */
    static boolean tickForms(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, long now) {
        if (no9.absorbing != null) {
            tickAbsorb(level, no9, def, now);
            return true;
        }
        if (no9.transformed || no9.reviving != null || no9.getPersistentData().getBoolean(MASS_TAG)
                || no9.isUsingAbility()) {
            return false;
        }
        Optional<NumberedDef.Transform> transform = def.transform();
        if (transform.isPresent() && no9.getHealth() < no9.getMaxHealth() * transform.get().healthBelow()) {
            No10Service.transform(level, no9, transform.get());
            return true;
        }
        if (no9.tickCount % THINK_INTERVAL != 0 || now < no9.nextAbsorbAt) {
            return false;
        }
        for (int i = 0; i < def.absorb().size(); i++) {
            NumberedDef.Absorb rule = def.absorb().get(i);
            if (rule.selfHealthBelow() < 1.0F && no9.getHealth() >= no9.getMaxHealth() * rule.selfHealthBelow()) {
                continue;
            }
            Optional<Entity> prey = absorbCandidate(level, no9, rule);
            if (prey.isPresent()) {
                startAbsorb(level, no9, rule, i, prey.get(), now);
                return true;
            }
        }
        return false;
    }

    static Optional<Entity> absorbCandidate(ServerLevel level, KaijuNo9Entity no9, NumberedDef.Absorb rule) {
        AABB area = no9.getBoundingBox().inflate(rule.seekRadius());
        List<Entity> found = new ArrayList<>(level.getEntitiesOfClass(KaijuEntity.class, area,
                kaiju -> kaiju != no9 && kaiju.isAlive() && rule.species().contains(kaiju.kaijuId())
                        && (rule.targetHealthBelow() >= 1.0F
                        || kaiju.getHealth() < kaiju.getMaxHealth() * rule.targetHealthBelow())));
        if (rule.carcass()) {
            found.addAll(level.getEntitiesOfClass(CarcassEntity.class, area,
                    carcass -> carcass.isAlive() && rule.species().contains(carcass.species())));
        }
        return found.stream().min(Comparator.comparingDouble(no9::distanceToSqr));
    }

    private static void startAbsorb(ServerLevel level, KaijuNo9Entity no9, NumberedDef.Absorb rule, int index,
            Entity prey, long now) {
        no9.absorbing = prey.getUUID();
        no9.absorbRule = index;
        no9.absorbEndsAt = now + rule.castTicks();
        no9.getNavigation().stop();
        no9.triggerAnim("special", "absorb");
        level.playSound(null, no9.blockPosition(), KN8Sounds.KAIJU_AMBIENT.get(), SoundSource.HOSTILE, 3.0F, 0.6F);
        announce(level, no9, Component.translatable("kn8.no9.absorbing").withStyle(ChatFormatting.DARK_RED,
                ChatFormatting.ITALIC));
    }

    /**
     * Tentaculos ligam o No. 9 a presa e a puxam para perto (kaiju vivo perde o alvo e para); no fim a presa some e
     * o No. 9 vira a forma nova. Presa morta (sem ser carcaca), sumida ou longe demais cancela.
     */
    private static void tickAbsorb(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, long now) {
        NumberedDef.Absorb rule = no9.absorbRule < def.absorb().size() ? def.absorb().get(no9.absorbRule) : null;
        Entity prey = level.getEntity(no9.absorbing);
        if (rule == null || prey == null || !prey.isAlive()
                || prey.distanceTo(no9) > rule.seekRadius() + no9.getBbWidth() + prey.getBbWidth()) {
            no9.absorbing = null;
            no9.nextAbsorbAt = now + ABSORB_RETRY_TICKS;
            return;
        }
        no9.getNavigation().stop();
        no9.getLookControl().setLookAt(prey);
        Vec3 from = no9.position().add(0, no9.getBbHeight() * 0.6, 0);
        Vec3 to = prey.position().add(0, prey.getBbHeight() * 0.5, 0);
        Vec3 pull = from.subtract(to);
        if (prey instanceof KaijuNo10Entity no10) {
            // Sendo absorvido, o No. 10 nao vira a forma gigante.
            no10.transformed = true;
        }
        if (prey instanceof KaijuEntity kaiju) {
            kaiju.setTarget(null);
            kaiju.getNavigation().stop();
            double gap = pull.horizontalDistance() - (no9.getBbWidth() + prey.getBbWidth()) * 0.5;
            if (gap > 0.5) {
                Vec3 step = new Vec3(pull.x, 0, pull.z).normalize().scale(Math.min(ABSORB_PULL_SPEED, gap));
                kaiju.setDeltaMovement(step.x, kaiju.getDeltaMovement().y, step.z);
                kaiju.hurtMarked = true;
            }
        }
        if (no9.tickCount % 2 == 0) {
            int steps = Math.max(4, (int) (pull.length() * 3));
            for (int i = 0; i <= steps; i++) {
                Vec3 point = to.add(pull.scale(i / (double) steps));
                level.sendParticles(TENDRIL, point.x, point.y, point.z, 1, 0.08, 0.08, 0.08, 0.0);
            }
        }
        if (now < no9.absorbEndsAt) {
            return;
        }
        no9.absorbing = null;
        InvasionService.release(level, prey.getUUID());
        level.sendParticles(ParticleTypes.SQUID_INK, to.x, to.y, to.z, 60, prey.getBbWidth() * 0.4,
                prey.getBbHeight() * 0.4, prey.getBbWidth() * 0.4, 0.05);
        prey.discard();
        // Sem destruicao de blocos: a forma nova nasce de dentro do No. 9 (a explosao da troca e so visual).
        No10Service.transform(level, no9, new NumberedDef.Transform(rule.into(), 0, 0.0F, 1.0F, 0.0F,
                rule.message()));
    }

    /** Dano depois da armadura: reduzido com a pele endurecida; cada golpe pode endurecer (chance do JSON). */
    static float harden(KaijuNo9Entity no9, float amount) {
        Optional<NumberedDef.Hardening> found = def(no9).flatMap(NumberedDef::hardening);
        if (found.isEmpty() || !(no9.level() instanceof ServerLevel level)) {
            return amount;
        }
        NumberedDef.Hardening hardening = found.get();
        long now = level.getGameTime();
        if (now < no9.hardenedUntil) {
            level.sendParticles(ParticleTypes.CRIT, no9.getX(), no9.getY() + no9.getBbHeight() * 0.6, no9.getZ(), 6,
                    no9.getBbWidth() * 0.4, no9.getBbHeight() * 0.3, no9.getBbWidth() * 0.4, 0.1);
            return amount * (1.0F - hardening.reduction());
        }
        if (now >= no9.hardenReadyAt && no9.getRandom().nextFloat() < hardening.chance()) {
            no9.hardenedUntil = now + hardening.durationTicks();
            no9.hardenReadyAt = no9.hardenedUntil + hardening.cooldownTicks();
            level.sendParticles(HARDEN, no9.getX(), no9.getY() + no9.getBbHeight() * 0.5, no9.getZ(), 40,
                    no9.getBbWidth() * 0.5, no9.getBbHeight() * 0.4, no9.getBbWidth() * 0.5, 0.0);
            level.playSound(null, no9.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        return amount;
    }

    /**
     * Analise (v1.2): o tipo do golpe e o tipo de dano mais a arma de quem bate (ou a especie, sem arma; ou o
     * projetil). Cada golpe do mesmo tipo soma uma marca e reduz o dano pela tabela do JSON; uma marca some a cada
     * {@code decay_ticks} sem levar esse tipo. Faiscas cinzentas avisam que ele esta se adaptando.
     */
    static float adapt(KaijuNo9Entity no9, DamageSource source, float amount) {
        Optional<NumberedDef.Adaptation> found = def(no9).flatMap(NumberedDef::adaptation);
        if (found.isEmpty() || amount <= 0 || !(no9.level() instanceof ServerLevel level)) {
            return amount;
        }
        NumberedDef.Adaptation adaptation = found.get();
        long now = level.getGameTime();
        long[] marks = no9.adaptation.computeIfAbsent(attackKind(source), key -> new long[] {0, now});
        marks[0] = Math.max(0, marks[0] - (now - marks[1]) / Math.max(1, adaptation.decayTicks()));
        marks[0]++;
        marks[1] = now;
        float reduction = adaptation.reductionFor((int) marks[0]);
        if (reduction > 0) {
            level.sendParticles(ParticleTypes.SMOKE, no9.getX(), no9.getY() + no9.getBbHeight() * 0.6, no9.getZ(),
                    (int) (reduction * 20), no9.getBbWidth() * 0.4, no9.getBbHeight() * 0.3, no9.getBbWidth() * 0.4,
                    0.02);
        }
        return amount * (1.0F - reduction);
    }

    /** Tipo do golpe para a adaptacao: tipo de dano + projetil, arma na mao ou especie de quem bateu. */
    static String attackKind(DamageSource source) {
        String type = source.typeHolder().getRegisteredName();
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (direct != null && direct != attacker) {
            return type + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType());
        }
        if (attacker instanceof LivingEntity living && !living.getMainHandItem().isEmpty()) {
            return type + "/" + BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem());
        }
        return attacker == null ? type : type + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
    }

    // --- regeneracao (0.6-D) -------------------------------------------------------------------------------------

    /**
     * Cura por tick pela fracao do JSON (mais rapida com a vida muito baixa), menos logo depois de levar golpe.
     * Particulas verdes no corpo para quem luta ver que ele esta se recuperando.
     */
    static void regenerate(ServerLevel level, KaijuEntity kaiju, NumberedDef.Regeneration regeneration) {
        if (!kaiju.isAlive() || kaiju.getHealth() >= kaiju.getMaxHealth()) {
            return;
        }
        float rate = regeneration.rateAt(kaiju.getHealth() / kaiju.getMaxHealth());
        int sinceHit = kaiju.getLastHurtByMobTimestamp() == 0 ? Integer.MAX_VALUE
                : kaiju.tickCount - kaiju.getLastHurtByMobTimestamp();
        if (rate <= 0 || sinceHit < regeneration.delayAfterHitTicks()) {
            return;
        }
        kaiju.heal(kaiju.getMaxHealth() * rate / TICKS_PER_SECOND);
        if (kaiju.tickCount % REGEN_PARTICLE_INTERVAL == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, kaiju.getX(), kaiju.getY() + kaiju.getBbHeight() * 0.6,
                    kaiju.getZ(), 6, kaiju.getBbWidth() * 0.5, kaiju.getBbHeight() * 0.3, kaiju.getBbWidth() * 0.5, 0.0);
        }
    }

    // --- reviver -------------------------------------------------------------------------------------------------

    private static Optional<CarcassEntity> nearestCarcass(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        AABB area = no9.getBoundingBox().inflate(def.reviveRadius());
        return level.getEntitiesOfClass(CarcassEntity.class, area, carcass -> carcass.isAlive()
                        && revivable(def, carcass)).stream()
                .min(Comparator.comparingDouble(no9::distanceToSqr));
    }

    private static void startRevive(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, CarcassEntity carcass,
            long now) {
        no9.reviving = carcass.getUUID();
        no9.reviveEndsAt = now + def.reviveCastTicks();
        no9.getNavigation().stop();
        no9.triggerAnim("special", "revive");
        level.playSound(null, no9.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F,
                0.6F);
        announce(level, no9, Component.translatable("kn8.no9.reviving",
                Component.translatable("entity." + carcass.species().getNamespace() + "." + carcass.species()
                        .getPath())).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void tickRevive(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, long now) {
        Entity entity = level.getEntity(no9.reviving);
        if (!(entity instanceof CarcassEntity carcass) || !carcass.isAlive()) {
            // Desmontaram a carcaca a tempo: o gesto falha.
            no9.reviving = null;
            no9.nextReviveAt = now + def.reviveCooldownTicks() / 2;
            return;
        }
        no9.getNavigation().stop();
        no9.getLookControl().setLookAt(carcass);
        if (no9.tickCount % PARTICLE_INTERVAL == 0) {
            // Fio verde das maos ate a carcaca.
            Vec3 from = no9.position().add(0, no9.getBbHeight() * 0.6, 0);
            Vec3 to = carcass.position().add(0, carcass.getBbHeight() * 0.5, 0);
            for (int i = 0; i <= 8; i++) {
                Vec3 point = from.lerp(to, i / 8.0);
                level.sendParticles(ACID, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.SOUL, to.x, to.y, to.z, 4, carcass.getBbWidth() * 0.3, 0.4,
                    carcass.getBbWidth() * 0.3, 0.02);
        }
        if (now < no9.reviveEndsAt) {
            return;
        }
        no9.reviving = null;
        no9.nextReviveAt = now + def.reviveCooldownTicks();
        revive(level, no9, def, carcass, true);
    }

    private static boolean revivable(NumberedDef def, CarcassEntity carcass) {
        return def.revive().containsKey(carcass.species()) || def.reviveBoss().containsKey(carcass.species());
    }

    /**
     * Levanta a carcaca: especie revivida (tabela {@code revive}) ou chefe ({@code revive_boss}, com barra e fases).
     * O revivido entra na invasao do No. 9 e ataca o alvo dele.
     */
    private static Optional<KaijuEntity> revive(ServerLevel level, KaijuNo9Entity no9, NumberedDef def,
            CarcassEntity carcass, boolean announceEach) {
        BlockPos pos = carcass.blockPosition();
        float yaw = carcass.getYRot();
        ResourceLocation boss = def.reviveBoss().get(carcass.species());
        ResourceLocation species = def.revive().get(carcass.species());
        carcass.discard();
        Optional<KaijuEntity> result = boss != null ? BossService.spawn(level, boss, pos)
                : species != null ? KaijuSpawner.spawn(level, species, pos) : Optional.empty();
        result.ifPresent(revived -> {
            revived.setYRot(yaw);
            revived.getPersistentData().putBoolean(KN8_REVIVED, true);
            no9.revived.add(revived.getUUID());
            if (no9.getPersistentData().getBoolean(InvasionService.TAG)) {
                InvasionService.join(level, revived);
            }
            if (no9.getTarget() != null) {
                revived.setTarget(no9.getTarget());
            }
            level.sendParticles(ACID, revived.getX(), revived.getY() + revived.getBbHeight() * 0.5, revived.getZ(),
                    60, revived.getBbWidth() * 0.4, revived.getBbHeight() * 0.4, revived.getBbWidth() * 0.4, 0.0);
            level.playSound(null, pos, KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 4.0F, 0.8F);
            if (announceEach) {
                announce(level, no9, Component.translatable("kn8.no9.revived", revived.getDisplayName())
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            }
        });
        return result;
    }

    // --- ressurreicao em massa (0.3) -----------------------------------------------------------------------------

    /**
     * Onda {@code mass_revive}: quando ha carcacas no raio, o No. 9 para, faz o gesto longo e depois levanta todas, uma
     * a cada {@code mass_revive_interval_ticks} (o exercito volta em sequencia, nao num tick so). Uma vez por No. 9.
     */
    private static void tickMass(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, long now) {
        if (!no9.massQueue.isEmpty()) {
            no9.getNavigation().stop();
            if (now >= no9.nextMassAt) {
                no9.nextMassAt = now + def.massReviveIntervalTicks();
                Entity entity = level.getEntity(no9.massQueue.remove(0));
                if (entity instanceof CarcassEntity carcass && carcass.isAlive()) {
                    revive(level, no9, def, carcass, false);
                }
                if (no9.massQueue.isEmpty()) {
                    announce(level, no9, Component.translatable("kn8.no9.mass_done", no9.revived.size())
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
                }
            }
            return;
        }
        if (no9.getPersistentData().getBoolean(MASS_DONE_TAG)) {
            return;
        }
        List<CarcassEntity> carcasses = new ArrayList<>(level.getEntitiesOfClass(CarcassEntity.class,
                massArea(level, no9, def), carcass -> carcass.isAlive() && revivable(def, carcass)));
        if (no9.reviving == null) {
            if (carcasses.isEmpty() || no9.tickCount % THINK_INTERVAL != 0) {
                return;
            }
            no9.reviving = no9.getUUID(); // marca "gesto em andamento" (o alvo e a area inteira)
            no9.reviveEndsAt = now + def.massReviveCastTicks();
            no9.getNavigation().stop();
            no9.triggerAnim("special", "revive");
            level.playSound(null, no9.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.6F);
            announceWide(level, no9, def, Component.translatable("kn8.no9.mass_reviving", carcasses.size())
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
            return;
        }
        no9.getNavigation().stop();
        if (no9.tickCount % (PARTICLE_INTERVAL * 2) == 0) {
            for (CarcassEntity carcass : carcasses) {
                level.sendParticles(ParticleTypes.SOUL, carcass.getX(), carcass.getY() + carcass.getBbHeight() * 0.5,
                        carcass.getZ(), 3, carcass.getBbWidth() * 0.3, 0.4, carcass.getBbWidth() * 0.3, 0.02);
                level.sendParticles(ACID, carcass.getX(), carcass.getY() + 0.3, carcass.getZ(), 4,
                        carcass.getBbWidth() * 0.4, 0.1, carcass.getBbWidth() * 0.4, 0.0);
            }
            level.sendParticles(ACID, no9.getX(), no9.getY() + 1.4, no9.getZ(), 12, 0.6, 0.6, 0.6, 0.0);
        }
        if (now < no9.reviveEndsAt) {
            return;
        }
        no9.reviving = null;
        no9.getPersistentData().putBoolean(MASS_DONE_TAG, true);
        // Ordem: mais perto primeiro (a onda de ressurreicao se espalha a partir dele).
        carcasses.sort(Comparator.comparingDouble(no9::distanceToSqr));
        carcasses.forEach(carcass -> no9.massQueue.add(carcass.getUUID()));
        no9.nextMassAt = now;
    }

    /**
     * Area da ressurreicao em massa: numa invasao, a area inteira dela (as carcacas ficam espalhadas pelo anel de
     * chegada, ate 64 blocos do centro, e o No. 9 chega por um lado so); fora dela, o raio em volta dele.
     */
    private static AABB massArea(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        if (no9.getPersistentData().getBoolean(InvasionService.TAG)) {
            Optional<Invasion> invasion = InvasionService.active(level);
            if (invasion.isPresent()) {
                double radius = Math.max(def.massReviveRadius(), invasion.get().def().radius());
                return new AABB(invasion.get().center()).inflate(radius, def.massReviveRadius(), radius);
            }
        }
        return no9.getBoundingBox().inflate(def.massReviveRadius());
    }

    private static void announceWide(ServerLevel level, Entity source, NumberedDef def, Component message) {
        double radius = Math.max(ANNOUNCE_RADIUS, def.massReviveRadius() * 2);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(source) <= radius * radius) {
                player.displayClientMessage(message, true);
                player.sendSystemMessage(message);
            }
        }
    }

    // --- comandar e fugir ----------------------------------------------------------------------------------------

    private static void command(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        command(level, no9, def.commandRadius());
    }

    /** Kaiju sem alvo a ate {@code radius} blocos passam a atacar o alvo do numerado (No. 9, No. 10). */
    static void command(ServerLevel level, KaijuEntity leader, float radius) {
        LivingEntity target = leader.getTarget();
        if (target == null || radius <= 0) {
            return;
        }
        for (KaijuEntity kaiju : level.getEntitiesOfClass(KaijuEntity.class,
                leader.getBoundingBox().inflate(radius), kaiju -> kaiju != leader && kaiju.isAlive()
                        && kaiju.getTarget() == null)) {
            kaiju.setTarget(target);
        }
    }

    private static void flee(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        no9.fled = true;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, no9.getX(), no9.getY() + 1.0, no9.getZ(), 40, 0.5, 1.0, 0.5,
                0.05);
        level.sendParticles(ACID, no9.getX(), no9.getY() + 1.0, no9.getZ(), 40, 0.4, 1.0, 0.4, 0.0);
        level.playSound(null, no9.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
        announce(level, no9, Component.translatable("kn8.no9.fled").withStyle(ChatFormatting.DARK_GREEN,
                ChatFormatting.ITALIC));
        List<ServerPlayer> fighters = level.getEntitiesOfClass(ServerPlayer.class,
                no9.getBoundingBox().inflate(FLEE_REWARD_RADIUS), player -> !player.isSpectator()
                        && no9.attackers.contains(player.getUUID()));
        for (ServerPlayer player : fighters) {
            if (def.fleeMerit() > 0) {
                player.sendSystemMessage(Component.translatable("kn8.no9.flee_merit", def.fleeMerit())
                        .withStyle(ChatFormatting.GOLD));
                CareerService.addMerit(player, def.fleeMerit());
            }
        }
        InvasionService.release(level, no9.getUUID());
        KN8Constants.LOGGER.info("[kn8] Kaiju No. 9 fugiu em {}", no9.blockPosition().toShortString());
        no9.discard();
    }

    private static void announce(ServerLevel level, Entity source, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(source) <= ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                player.displayClientMessage(message, true);
            }
        }
    }
}
