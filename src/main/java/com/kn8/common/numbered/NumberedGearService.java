package com.kn8.common.numbered;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.career.CareerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedGearDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Compatibilidade e dominio dos equipamentos numerados do jogador (PM-11, Plano Mestre secao 11). O servidor decide:
 * a arma numerada so ativa (golpe, tiro, especial) com a patente, a pesquisa e o limite pessoal de Release do
 * {@code numbered_gear/*.json}; sem eles nada e gasto e o motivo vai para a barra. Com eles, o dominio (XP pelo dano
 * em kaiju com a propria arma) leva o dano de {@code effect_start} a 1,0, tira o calor extra do especial e, no
 * Numbers 1, alarga a esquiva perfeita. Nada some do inventario e as armas antigas continuam iguais. PM-11B: o
 * traje numerado vestido (Numbers 10) tem cauda que corta e guarda sozinha; o machado com asas (Numbers 4) plana.
 */
public final class NumberedGearService {

    private static final String TAG = "kn8_numbered";
    private static final String TAIL_READY = "tail_ready";
    private static final String GUARD_READY = "guard_ready";
    private static final String GLIDING = "gliding";
    private static final String GLIDE_TIRED = "glide_tired";
    /** Cosseno do limite "atras ou do lado" da cauda (o mesmo do Hoshina No. 10: ~60 graus da frente). */
    private static final double SIDE_COSINE = 0.5;
    private static final int SECOND = 20;
    private static final int GLIDE_EFFECT_TICKS = 4;
    private static final int WING_BEAT_TICKS = 9;
    private static final double TAIL_VFX_AHEAD = 1.8;
    private static final ResourceLocation NO10_VFX = KN8Constants.id("hoshina_no10");
    private static final ResourceLocation NO4_VFX = KN8Constants.id("kikoru_no4");

    private NumberedGearService() {
    }

    /** Equipamento numerado do item (o id do registro e o lado decidem; {@code clientSide} para o menu). */
    public static Optional<Map.Entry<ResourceLocation, NumberedGearDef>> of(ItemStack stack, boolean clientSide) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return KN8Data.NUMBERED_GEAR.forSide(clientSide).entrySet().stream()
                .filter(entry -> entry.getValue().item().equals(item)).findFirst();
    }

    private static Optional<Map.Entry<ResourceLocation, NumberedGearDef>> held(ServerPlayer player) {
        return of(player.getMainHandItem(), false);
    }

    /** Motivo da recusa ({@code rank}, {@code research}, {@code limit}) ou vazio se o jogador pode usar. */
    public static Optional<String> blocked(ServerPlayer player, NumberedGearDef def) {
        if (player.isCreative()) {
            return Optional.empty();
        }
        if (def.rank().filter(rank -> !CareerService.hasRank(player, rank)).isPresent()) {
            return Optional.of("rank");
        }
        if (def.research().filter(id -> !CareerService.data(player).research().contains(id)).isPresent()) {
            return Optional.of("research");
        }
        if (PowerService.limit(player) < def.minLimit()) {
            return Optional.of("limit");
        }
        return Optional.empty();
    }

    /**
     * Confere a arma na mao antes de um golpe/especial: falso (com o motivo na barra) quando e numerada e o jogador
     * ainda nao cumpre os requisitos.
     */
    public static boolean allowed(ServerPlayer player) {
        Optional<Map.Entry<ResourceLocation, NumberedGearDef>> gear = held(player);
        if (gear.isEmpty()) {
            return true;
        }
        Optional<String> reason = blocked(player, gear.get().getValue());
        reason.ifPresent(key -> player.displayClientMessage(Component.translatable("kn8.numbered.blocked." + key,
                requirement(gear.get().getValue(), key)).withStyle(ChatFormatting.RED), true));
        return reason.isEmpty();
    }

    private static Object requirement(NumberedGearDef def, String key) {
        return switch (key) {
            case "rank" -> Component.translatable("kn8.rank." + def.rank().map(ResourceLocation::getPath).orElse(""));
            case "research" -> Component.translatable("kn8.research.name." + def.research()
                    .map(ResourceLocation::getPath).orElse(""));
            default -> def.minLimit();
        };
    }

    public static int xp(ServerPlayer player, ResourceLocation gear) {
        return CareerService.data(player).mastery().getOrDefault(gear, 0);
    }

    /** Fracao do dano da arma numerada na mao pelo dominio (1,0 sem arma numerada ou com dominio completo). */
    public static float damageFactor(ServerPlayer player) {
        return held(player).map(entry -> {
            NumberedGearDef def = entry.getValue();
            double progress = def.progress(xp(player, entry.getKey()));
            return (float) (def.effectStart() + (1.0 - def.effectStart()) * progress);
        }).orElse(1.0F);
    }

    /** Calor extra do especial (instabilidade de quem ainda nao domina a arma). */
    public static double extraSpecialHeat(ServerPlayer player) {
        return held(player).map(entry -> entry.getValue().instabilityHeat()
                * (1.0 - entry.getValue().progress(xp(player, entry.getKey())))).orElse(0.0);
    }

    /** Ticks a mais da esquiva perfeita pelo dominio (Numbers 1). */
    public static int perfectDodgeBonus(ServerPlayer player) {
        return held(player).map(entry -> (int) Math.round(entry.getValue().perfectDodgeBonusTicks()
                * entry.getValue().progress(xp(player, entry.getKey())))).orElse(0);
    }

    /**
     * Dano causado em kaiju vira XP de dominio da arma numerada na mao e do traje numerado vestido (o traje
     * sincroniza com toda luta); subir de nivel avisa.
     */
    public static void onKaijuDamaged(ServerPlayer player, float damage) {
        Optional<Map.Entry<ResourceLocation, NumberedGearDef>> hand = held(player);
        hand.ifPresent(entry -> gainXp(player, entry, damage));
        worn(player).filter(entry -> hand.map(h -> !h.getKey().equals(entry.getKey())).orElse(true))
                .ifPresent(entry -> gainXp(player, entry, damage));
    }

    private static void gainXp(ServerPlayer player, Map.Entry<ResourceLocation, NumberedGearDef> entry, float damage) {
        NumberedGearDef def = entry.getValue();
        int gain = (int) Math.round(damage * def.xpPerDamage());
        if (gain <= 0 || blocked(player, def).isPresent()) {
            return;
        }
        int before = xp(player, entry.getKey());
        int after = before + gain;
        CareerService.data(player).mastery().put(entry.getKey(), after);
        if (def.level(after) > def.level(before)) {
            player.sendSystemMessage(Component.translatable("kn8.numbered.level_up", def.number(),
                    def.level(after), def.maxLevel()).withStyle(ChatFormatting.AQUA));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        CareerService.changed(player);
    }

    // --- PM-11B: traje com cauda (Numbers 10) e asas (Numbers 4) ---------------------------------------------------

    /** Equipamento numerado vestido no peito (traje). */
    private static Optional<Map.Entry<ResourceLocation, NumberedGearDef>> worn(ServerPlayer player) {
        return of(player.getItemBySlot(EquipmentSlot.CHEST), false);
    }

    /** So o equipamento cujos requisitos o jogador cumpre (sem eles o traje e so armadura, o machado so machado). */
    private static Optional<Map.Entry<ResourceLocation, NumberedGearDef>> usable(ServerPlayer player,
            Optional<Map.Entry<ResourceLocation, NumberedGearDef>> gear) {
        return gear.filter(entry -> blocked(player, entry.getValue()).isEmpty());
    }

    private static double scale(ServerPlayer player, Map.Entry<ResourceLocation, NumberedGearDef> entry) {
        return entry.getValue().scale(xp(player, entry.getKey()));
    }

    /** Tick do jogador no servidor: cauda do traje e asas do machado. */
    public static void tick(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        usable(player, worn(player)).ifPresent(entry -> entry.getValue().tail()
                .ifPresent(tail -> tailTick(player, entry, tail, now)));
        Optional<Map.Entry<ResourceLocation, NumberedGearDef>> wings = usable(player, held(player))
                .filter(entry -> entry.getValue().wings().isPresent());
        if (wings.isPresent()) {
            wingsTick(player, wings.get(), wings.get().getValue().wings().get(), now);
        } else {
            state(player).remove(GLIDING);
        }
    }

    /** Estado curto do jogador (esperas da cauda e da guarda, planando); fora do bloco que passa pela morte. */
    private static CompoundTag state(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        player.getPersistentData().put(TAG, tag);
        return tag;
    }

    private static void tailTick(ServerPlayer player, Map.Entry<ResourceLocation, NumberedGearDef> entry,
            NumberedGearDef.Tail tail, long now) {
        CompoundTag state = state(player);
        if (now < state.getLong(TAIL_READY)) {
            return;
        }
        Optional<KaijuEntity> target = tailTarget(player, tail.reach());
        if (target.isEmpty()) {
            return;
        }
        float damage = (float) (tail.damage() * scale(player, entry)
                * (1.0 + PowerService.activeRelease(player) / 100.0));
        KaijuEntity kaiju = target.get();
        kaiju.hurt(player.damageSources().playerAttack(player), damage);
        state.putLong(TAIL_READY, now + tail.cooldownTicks());
        ServerLevel level = player.serverLevel();
        Vec3 aim = kaiju.position().subtract(player.position());
        // Mesma receita da cauda do Hoshina No. 10 (arco vermelho do traje e faiscas): o traje e o mesmo. O arco
        // nasce entre o jogador e o kaiju, na altura da cintura (a receita o puxa um pouco para tras).
        Vec3 from = player.position().add(aim.multiply(1, 0, 1).normalize().scale(TAIL_VFX_AHEAD))
                .add(0, player.getBbHeight() * 0.5, 0);
        VfxService.play(level, VfxService.TECHNIQUE, from, aim, 1.0F, 0.0F, Optional.of(NO10_VFX), "tail",
                player.getBbHeight());
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.7F, 0.7F);
    }

    /**
     * Alvo da cauda: kaiju a ate {@code reach} da caixa do jogador. Primeiro quem esta mirando o jogador (proteger),
     * depois quem esta atras ou do lado (a cauda cobre as costas), depois o mais perto; como no Hoshina No. 10.
     */
    static Optional<KaijuEntity> tailTarget(ServerPlayer player, double reach) {
        List<KaijuEntity> near = player.level().getEntitiesOfClass(KaijuEntity.class,
                player.getBoundingBox().inflate(reach + 1.0), kaiju -> kaiju.isAlive()
                        && edge(player.getBoundingBox(), kaiju.getBoundingBox()) <= reach);
        return near.stream().min(Comparator
                .comparing((KaijuEntity kaiju) -> !(kaiju.getTarget() == player
                        || kaiju.abilityTarget().map(target -> target == player).orElse(false)))
                .thenComparing(kaiju -> facingCosine(player, kaiju.position()) >= SIDE_COSINE)
                .thenComparingDouble(kaiju -> edge(player.getBoundingBox(), kaiju.getBoundingBox())));
    }

    /** Distancia entre as bordas de duas caixas (0 se encostam). */
    private static double edge(AABB a, AABB b) {
        double dx = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Cosseno entre a frente do jogador e a direcao ate o ponto (1 = bem na frente, -1 = bem atras). */
    private static double facingCosine(ServerPlayer player, Vec3 point) {
        Vec3 to = new Vec3(point.x - player.getX(), 0, point.z - player.getZ());
        if (to.lengthSqr() < 1.0E-4) {
            return 1.0;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).dot(to.normalize());
    }

    /**
     * Guarda da cauda: golpe pesado de kaiju ({@code [combat] tailGuardBlocksHeavy}) ou projetil vindo de fora do arco
     * da frente leva o dano de 1 ate {@code guard_factor} (pelo dominio), uma vez por {@code guard_cooldown_ticks}.
     * Devolve o dano que fica.
     */
    public static float guard(ServerPlayer player, DamageSource source, float amount) {
        Optional<Map.Entry<ResourceLocation, NumberedGearDef>> gear = usable(player, worn(player));
        Vec3 from = source.getSourcePosition();
        if (gear.isEmpty() || gear.get().getValue().tail().isEmpty() || from == null) {
            return amount;
        }
        NumberedGearDef.Tail tail = gear.get().getValue().tail().get();
        boolean heavyHit = source.getEntity() instanceof KaijuEntity && CombatService.isHeavyHit(source);
        boolean heavy = heavyHit && ServerConfig.TAIL_GUARD_BLOCKS_HEAVY.get();
        boolean threat = heavy || (source.is(DamageTypeTags.IS_PROJECTILE) && !heavyHit);
        long now = player.level().getGameTime();
        CompoundTag state = state(player);
        double half = Math.toRadians(tail.frontArcDegrees() / 2.0);
        if (!threat || now < state.getLong(GUARD_READY) || facingCosine(player, from) >= Math.cos(half)) {
            return amount;
        }
        double factor = 1.0 - (1.0 - tail.guardFactor()) * scale(player, gear.get());
        state.putLong(GUARD_READY, now + tail.guardCooldownTicks());
        Vec3 at = player.position().add(from.subtract(player.position()).normalize()).add(0, player.getBbHeight()
                * 0.6, 0);
        VfxService.play(player.serverLevel(), VfxService.IMPACT, at, from.subtract(player.position()), 0.8F, 0.0F,
                Optional.of(NO10_VFX), "tail_guard", player.getBbHeight());
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.6F, 1.6F);
        return (float) (amount * factor);
    }

    /**
     * Asas do Numbers 4: Shift no ar caindo abre as asas. Ao abrir, impulso para onde o jogador olha (pelo dominio);
     * enquanto plana, queda lenta (efeito escondido renovado a cada tick) e stamina por segundo; sem stamina as asas
     * fecham ate pousar. Solta o Shift ou toca o chao: fecha.
     */
    private static void wingsTick(ServerPlayer player, Map.Entry<ResourceLocation, NumberedGearDef> entry,
            NumberedGearDef.Wings wings, long now) {
        CompoundTag state = state(player);
        boolean wants = !player.onGround() && player.isShiftKeyDown() && !player.isInWater()
                && !player.getAbilities().flying && !player.isFallFlying()
                && (state.getBoolean(GLIDING) || player.getDeltaMovement().y < -0.05);
        if (!wants) {
            state.remove(GLIDING);
            state.remove(GLIDE_TIRED);
            return;
        }
        if (state.getBoolean(GLIDE_TIRED)) {
            return;
        }
        if (!state.getBoolean(GLIDING)) {
            state.putBoolean(GLIDING, true);
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() > 1.0E-4) {
                Vec3 push = flat.normalize().scale(wings.boost() * scale(player, entry));
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.2, 1).add(push));
                player.hurtMarked = true;
            }
        }
        if (now % SECOND == 0 && wings.staminaPerSecond() > 0 && !player.isCreative()
                && !PowerService.tryConsumeStamina(player, wings.staminaPerSecond())) {
            state.putBoolean(GLIDE_TIRED, true);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, GLIDE_EFFECT_TICKS, 0, true, false, false));
        player.fallDistance = 0.0F;
        if (now % WING_BEAT_TICKS == 0) {
            // Mesma batida das asas da Kikoru No. 4 (tracos ciano saindo das costas).
            VfxService.play(player.serverLevel(), VfxService.TECHNIQUE, player.position().add(0, 1.2, 0),
                    player.getLookAngle(), 1.0F, 0.0F, Optional.of(NO4_VFX), "wing_beat", player.getBbHeight());
        }
    }

    /** Planando agora (GameTests e HUD). */
    public static boolean gliding(ServerPlayer player) {
        return state(player).getBoolean(GLIDING) && !state(player).getBoolean(GLIDE_TIRED);
    }
}
