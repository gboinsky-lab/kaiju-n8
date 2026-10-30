package com.kn8.common.host;

import java.util.Optional;

import javax.annotation.Nullable;

import com.kn8.KN8Constants;
import com.kn8.core.power.BodyStat;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.career.CareerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.site.RegionService;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * T-2: o hospedeiro (T-1) vira o Kaiju No. 8 e volta, pela tecla (payload {@code kn8:kaiju_form}). O servidor decide
 * tudo: so o hospedeiro, vivo, fora da espera depois de voltar. O estado e publico ({@code kn8:kaiju_form}, sync
 * nativo) e cada cliente desenha o jogador como o No. 8 (fantoche, metodo D).
 *
 * <p>Forca da forma (Miguel 2026-10-24): o Release nao muda a forma kaiju (a % fica em 0 enquanto transformado); so os
 * atributos do corpo treinados a deixam mais forte. A forma ja e muito forte por natureza ({@code [kaijuForm]}): vida,
 * armadura, velocidade, salto, resistencia a empurrao e a queda, degrau, altura de 2 m. Os golpes da forma (so os
 * punhos com o Golpe no Chao na tecla R), o limite de tempo e a perda de controle ({@link Rampage}) sao da T-3.</p>
 */
public final class KaijuFormService {

    /** Perfil visual e aura da forma (os mesmos do No. 8 aliado). */
    public static final ResourceLocation PROFILE = KN8Constants.id("kaiju_no8");
    /** Aura da forma sem controle (vermelha). */
    public static final ResourceLocation RAMPAGE_AURA = KN8Constants.id("kaiju_no8_rampage");
    /** T-3: arma da forma (so os punhos): a mesma do No. 8 aliado, com o Golpe no Chao na tecla R. */
    public static final ResourceLocation FIST = KN8Constants.id("kaiju_no8_fist");
    /** Perfil visual da forma humana (o Kafka): efeito da volta. */
    private static final ResourceLocation HUMAN_PROFILE = KN8Constants.id("kafka");
    private static final String TAG_READY = "kn8_form_ready";
    private static final String TAG_SINCE = "kn8_form_since";
    /** Altura do jogador no vanilla (o atributo de escala multiplica por ela). */
    private static final double PLAYER_HEIGHT = 1.8;
    private static final int REFRESH_TICKS = 20;

    private static final ResourceLocation HEALTH = KN8Constants.id("kaiju_form_health");
    private static final ResourceLocation ARMOR = KN8Constants.id("kaiju_form_armor");
    private static final ResourceLocation TOUGHNESS = KN8Constants.id("kaiju_form_toughness");
    private static final ResourceLocation SPEED = KN8Constants.id("kaiju_form_speed");
    private static final ResourceLocation JUMP = KN8Constants.id("kaiju_form_jump");
    private static final ResourceLocation KNOCKBACK = KN8Constants.id("kaiju_form_knockback");
    private static final ResourceLocation FALL = KN8Constants.id("kaiju_form_fall");
    private static final ResourceLocation STEP = KN8Constants.id("kaiju_form_step");
    private static final ResourceLocation SCALE = KN8Constants.id("kaiju_form_scale");

    /** Resultado do pedido de transformar/voltar (o cliente so recebe a mensagem). */
    public enum Result {
        TRANSFORMED, REVERTED, NOT_HOST, DISABLED, COOLDOWN, UNAVAILABLE
    }

    private KaijuFormService() {
    }

    /** Estados publicos da forma ({@code kn8:kaiju_form}). */
    public static final int HUMAN = 0;
    public static final int FORM = 1;
    public static final int RAMPAGE = 2;

    /** Esta na forma do No. 8 (no controle ou nao)? Vale no servidor e no cliente (dado publico). */
    public static boolean isTransformed(Entity entity) {
        return entity instanceof Player player && player.getData(KN8Attachments.KAIJU_FORM.get()) != HUMAN;
    }

    /** T-3: passou do tempo limite e perdeu o controle do corpo (servidor e cliente). */
    public static boolean isRampaging(Entity entity) {
        return entity instanceof Player player && player.getData(KN8Attachments.KAIJU_FORM.get()) == RAMPAGE;
    }

    /**
     * T-3: forca dos golpes na forma: o punho do JSON vezes {@code [kaijuForm] damageMultiplier} (a forca treinada do
     * corpo multiplica por cima, no CombatService); o Release nao entra (Miguel 2026-10-24). Humano: 1.
     */
    public static float damageFactor(Entity entity) {
        return isTransformed(entity) ? (float) ServerConfig.FORM_DAMAGE_MULTIPLIER.get().doubleValue() : 1.0F;
    }

    // --- T-4: reacao da Forca de Defesa --------------------------------------------------------------------------

    /**
     * T-4 (Miguel 2026-10-24: "os soldados atacam o jogador transformado; a partir de certa patente nao atacam mais"):
     * a Forca de Defesa trata este jogador como kaiju. Sem controle, sempre (ataca todos em volta); no controle, so
     * abaixo da patente {@code [kaijuForm] trustedRankOrder}. Criativo e espectador nunca. So no servidor (a patente
     * nao vai ao cliente de outros jogadores).
     */
    public static boolean hostileToForce(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !isTransformed(player) || player.isCreative()
                || player.isSpectator()) {
            return false;
        }
        return isRampaging(player) || !trusted(player);
    }

    /**
     * T-4 por soldado: o esquadrao do proprio hospedeiro nao o ataca enquanto ele esta no controle (Miguel
     * 2026-10-30); sem controle, ataca como os outros.
     */
    public static boolean hostileTo(SoldierEntity soldier, @Nullable Entity entity) {
        return hostileToForce(entity) && (isRampaging(entity)
                || soldier.squadLeader().filter(entity.getUUID()::equals).isEmpty());
    }

    /** T-4: patente a partir da qual os soldados confiam no hospedeiro transformado (no controle). */
    public static boolean trusted(ServerPlayer player) {
        int order = CareerService.rank(player).map(entry -> entry.getValue().order()).orElse(0);
        return order >= ServerConfig.FORM_TRUSTED_RANK_ORDER.get();
    }

    /** T-4: transformado, a base nao atende (sem aviso: quem chama decide a mensagem). */
    public static boolean servicesClosed(ServerPlayer player) {
        return ServerConfig.FORM_CLOSES_SERVICES.get() && isTransformed(player);
    }

    /** T-4: transformado, oficiais, estacao, terminal e quadro de missoes recusam (com o aviso na barra). */
    public static boolean refusesServices(ServerPlayer player) {
        if (!servicesClosed(player)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("kn8.host.form.services_refused")
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    /**
     * T-4: soldados a ate {@code witnessRadius} com linha de visao viram a transformacao. Abaixo da patente de
     * confianca o hospedeiro perde merito uma vez por transformacao (e a partir dela ninguem estranha).
     */
    private static void witnessed(ServerPlayer player) {
        double radius = ServerConfig.FORM_WITNESS_RADIUS.get();
        int penalty = ServerConfig.FORM_WITNESS_MERIT_PENALTY.get();
        if (radius <= 0 || penalty <= 0 || player.isCreative() || trusted(player)) {
            return;
        }
        long witnesses = player.level().getEntitiesOfClass(SoldierEntity.class,
                player.getBoundingBox().inflate(radius), soldier -> soldier.isAlive()
                        && !(soldier instanceof DefenseOfficerEntity) && soldier.hasLineOfSight(player)
                        && soldier.distanceToSqr(player) <= radius * radius).size();
        if (witnesses == 0) {
            return;
        }
        int lost = Math.min(penalty, CareerService.data(player).merit());
        CareerService.addMerit(player, -lost);
        player.sendSystemMessage(Component.translatable("kn8.host.form.witnessed", witnesses, lost)
                .withStyle(ChatFormatting.RED));
        KN8Constants.LOGGER.info("[kn8] {} se transformou na frente de {} soldado(s): -{} de merito",
                player.getGameProfile().getName(), witnesses, lost);
    }

    /** Comando de operador ({@code /kn8 host clear}): tira a espera para transformar de novo. */
    public static void resetWait(ServerPlayer player) {
        player.getPersistentData().remove(TAG_READY);
    }

    /** Comando de teste: leva o tempo na forma ao limite (perde o controle no proximo tick). */
    public static boolean expire(ServerPlayer player) {
        if (!isTransformed(player)) {
            return false;
        }
        player.getPersistentData().putLong(TAG_SINCE, player.level().getGameTime()
                - Math.max(formTicks(player), ServerConfig.FORM_LIMIT_TICKS.get()));
        return true;
    }

    /** T-3: ticks na forma desde a transformacao. */
    public static long formTicks(ServerPlayer player) {
        return player.level().getGameTime() - player.getPersistentData().getLong(TAG_SINCE);
    }

    /** Tecla da forma: transforma ou volta. */
    public static Result toggle(ServerPlayer player) {
        if (isRampaging(player)) {
            // Sem controle do corpo: a tecla nao responde ate o surto passar.
            player.displayClientMessage(Component.translatable("kn8.host.form.no_control")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return Result.UNAVAILABLE;
        }
        Result result = isTransformed(player) ? revert(player) : transform(player);
        String key = switch (result) {
            case TRANSFORMED, REVERTED -> null;
            case NOT_HOST -> "kn8.host.form.not_host";
            case DISABLED -> "kn8.host.form.disabled";
            case COOLDOWN -> "kn8.host.form.cooldown";
            case UNAVAILABLE -> "kn8.host.form.unavailable";
        };
        if (key != null) {
            Object seconds = (readyTick(player) - player.level().getGameTime() + 19) / 20;
            player.displayClientMessage(Component.translatable(key, seconds).withStyle(ChatFormatting.GRAY), true);
        }
        return result;
    }

    public static Result transform(ServerPlayer player) {
        if (!ServerConfig.FORM_ENABLED.get()) {
            return Result.DISABLED;
        }
        if (!LarvaService.isHost(player)) {
            return Result.NOT_HOST;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return Result.UNAVAILABLE;
        }
        if (player.level().getGameTime() < readyTick(player)) {
            return Result.COOLDOWN;
        }
        player.setData(KN8Attachments.KAIJU_FORM.get(), FORM);
        player.getPersistentData().putLong(TAG_SINCE, player.level().getGameTime());
        applyModifiers(player, true);
        player.setHealth(player.getMaxHealth());
        effect(player, PROFILE, 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 1.5F, 0.8F);
        witnessed(player);
        KN8Constants.LOGGER.info("[kn8] {} virou o Kaiju No. 8", player.getGameProfile().getName());
        return Result.TRANSFORMED;
    }

    public static Result revert(ServerPlayer player) {
        if (!isTransformed(player)) {
            return Result.UNAVAILABLE;
        }
        boolean rampage = isRampaging(player);
        player.setData(KN8Attachments.KAIJU_FORM.get(), HUMAN);
        player.getPersistentData().remove(TAG_SINCE);
        player.setData(KN8Attachments.RAMPAGE_TARGET.get(), -1);
        applyModifiers(player, false);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        int wait = rampage ? ServerConfig.FORM_RAMPAGE_COOLDOWN_TICKS.get() : ServerConfig.FORM_COOLDOWN_TICKS.get();
        player.getPersistentData().putLong(TAG_READY, player.level().getGameTime() + wait);
        if (rampage) {
            // O corpo cobra o surto: lento e fraco por um tempo.
            int exhaust = ServerConfig.FORM_EXHAUST_TICKS.get();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, exhaust, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, exhaust, 1, false, false, true));
            player.displayClientMessage(Component.translatable("kn8.host.form.control_back")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        effect(player, HUMAN_PROFILE, 0.7F);
        KN8Constants.LOGGER.info("[kn8] {} voltou a forma humana", player.getGameProfile().getName());
        return Result.REVERTED;
    }

    /**
     * Tick do jogador: os modificadores sao transitorios (somem ao relogar) e os atributos do corpo mudam com o treino,
     * entao a forma reaplica os numeros de tempos em tempos. Quem deixou de ser hospedeiro (comando) volta.
     */
    public static void tick(ServerPlayer player) {
        if (!isTransformed(player)) {
            return;
        }
        if (!LarvaService.isHost(player)) {
            revert(player);
            return;
        }
        if (player.tickCount % REFRESH_TICKS == 0 || !hasModifier(player, Attributes.MAX_HEALTH, HEALTH)) {
            applyModifiers(player, true);
        }
        if (!player.getPersistentData().contains(TAG_SINCE)) {
            // Save de antes da T-3 (transformado sem a marca): a contagem comeca agora, nao no tick 0 do mundo.
            player.getPersistentData().putLong(TAG_SINCE, player.level().getGameTime());
        }
        long elapsed = formTicks(player);
        int limit = ServerConfig.FORM_LIMIT_TICKS.get();
        if (!isRampaging(player) && elapsed >= limit) {
            startRampage(player);
        } else if (isRampaging(player) && elapsed >= limit + ServerConfig.FORM_RAMPAGE_TICKS.get()) {
            revert(player);
            return;
        }
        if (isRampaging(player)) {
            Rampage.tick(player);
        }
        RegionService.hostTick(player);
        if (player.tickCount % REFRESH_TICKS == 0) {
            regenerate(player);
            status(player, elapsed, limit);
        }
    }

    /** T-3 (Miguel: "caso fique mais que isso ... perca o controle do corpo e saia atacando todos em volta"). */
    private static void startRampage(ServerPlayer player) {
        player.setData(KN8Attachments.KAIJU_FORM.get(), RAMPAGE);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 2.0F, 0.6F);
        if (player.level() instanceof ServerLevel level) {
            VfxService.play(level, VfxService.ROAR, player.getEyePosition(), player.getLookAngle(), 1.4F, 0.4F,
                    Optional.of(PROFILE), "roar", (float) ServerConfig.FORM_HEIGHT.get().doubleValue());
        }
        player.displayClientMessage(Component.translatable("kn8.host.form.lost_control")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        KN8Constants.LOGGER.info("[kn8] {} perdeu o controle da forma do Kaiju No. 8", player.getGameProfile()
                .getName());
    }

    /** Regeneracao da forma (como o No. 8 aliado): so abaixo de {@code regenBelow} da vida. */
    private static void regenerate(ServerPlayer player) {
        float max = player.getMaxHealth();
        if (player.getHealth() < max * ServerConfig.FORM_REGEN_BELOW.get()) {
            player.heal((float) (max * ServerConfig.FORM_REGEN_PER_SECOND.get()));
        }
    }

    /**
     * Barra de acao uma vez por segundo: tempo que resta na forma (aviso nos ultimos {@code warnTicks}) e a deteccao
     * de kaiju do No. 8 (o mais perto, distancia e direcao).
     */
    private static void status(ServerPlayer player, long elapsed, int limit) {
        Component line;
        if (isRampaging(player)) {
            line = Component.translatable("kn8.host.form.hud.rampage").withStyle(ChatFormatting.DARK_RED);
        } else {
            long left = Math.max(0, limit - elapsed) / 20;
            String time = String.format("%d:%02d", left / 60, left % 60);
            boolean warn = limit - elapsed <= ServerConfig.FORM_WARN_TICKS.get();
            line = Component.translatable("kn8.host.form.hud.time", time)
                    .withStyle(warn ? ChatFormatting.RED : ChatFormatting.AQUA);
        }
        KaijuEntity nearest = nearestKaiju(player);
        if (nearest != null) {
            Vec3 to = nearest.position().subtract(player.position());
            line = line.copy().append(Component.literal("  ")).append(Component.translatable(
                    "kn8.host.form.hud.kaiju", nearest.getDisplayName(), (int) Math.round(to.horizontalDistance()),
                    Component.translatable("kn8.host.form.dir." + direction(to)))
                    .withStyle(ChatFormatting.GRAY));
        }
        player.displayClientMessage(line, true);
    }

    @Nullable
    private static KaijuEntity nearestKaiju(ServerPlayer player) {
        double radius = ServerConfig.FORM_DETECT_RADIUS.get();
        if (radius <= 0) {
            return null;
        }
        KaijuEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (KaijuEntity kaiju : player.level().getEntitiesOfClass(KaijuEntity.class,
                player.getBoundingBox().inflate(radius), KaijuEntity::isAlive)) {
            double distance = kaiju.distanceToSqr(player);
            if (!LarvaService.isHostLarva(kaiju) && distance < bestDistance) {
                best = kaiju;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Ponto cardeal (n, ne, e, se, s, sw, w, nw) de um vetor no mundo (norte = -Z). */
    private static String direction(Vec3 to) {
        String[] names = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        double angle = Math.toDegrees(Math.atan2(-to.x, to.z));
        int index = (int) Math.floorMod(Math.round(angle / 45.0), 8);
        return names[index];
    }

    /** Tick do jogo a partir do qual pode transformar de novo. */
    public static long readyTick(ServerPlayer player) {
        return player.getPersistentData().getLong(TAG_READY);
    }

    /** Vida maxima extra da forma com o nivel de resistencia de agora (publico para os GameTests). */
    public static double healthBonus(ServerPlayer player) {
        return ServerConfig.FORM_HEALTH_BONUS.get()
                + ServerConfig.FORM_HEALTH_PER_RESISTANCE.get() * PowerService.bodyLevel(player, BodyStat.RESISTANCE);
    }

    private static void applyModifiers(ServerPlayer player, boolean on) {
        int resistance = PowerService.bodyLevel(player, BodyStat.RESISTANCE);
        int speed = PowerService.bodyLevel(player, BodyStat.SPEED);
        int agility = PowerService.bodyLevel(player, BodyStat.AGILITY);
        AttributeModifier.Operation add = AttributeModifier.Operation.ADD_VALUE;
        AttributeModifier.Operation base = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        set(player, Attributes.MAX_HEALTH, HEALTH, on ? healthBonus(player) : 0, add);
        set(player, Attributes.ARMOR, ARMOR, on ? ServerConfig.FORM_ARMOR.get()
                + ServerConfig.FORM_ARMOR_PER_RESISTANCE.get() * resistance : 0, add);
        set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, on ? ServerConfig.FORM_TOUGHNESS.get() : 0, add);
        set(player, Attributes.MOVEMENT_SPEED, SPEED, on ? ServerConfig.FORM_SPEED_BONUS.get()
                + ServerConfig.FORM_SPEED_PER_LEVEL.get() * speed : 0, base);
        set(player, Attributes.JUMP_STRENGTH, JUMP, on ? ServerConfig.FORM_JUMP_BONUS.get()
                + ServerConfig.FORM_JUMP_PER_AGILITY.get() * agility : 0, add);
        set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, on ? ServerConfig.FORM_KNOCKBACK_RESISTANCE.get() : 0,
                add);
        set(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL, on ? ServerConfig.FORM_FALL_DAMAGE_MULTIPLIER.get() - 1.0
                : 0, base);
        set(player, Attributes.STEP_HEIGHT, STEP, on ? ServerConfig.FORM_STEP_HEIGHT_BONUS.get() : 0, add);
        set(player, Attributes.SCALE, SCALE, on ? ServerConfig.FORM_HEIGHT.get() / PLAYER_HEIGHT - 1.0 : 0, base);
        // O vanilla so refaz a caixa no proximo tick (atributos sujos); a forma ja nasce com o tamanho certo.
        player.refreshDimensions();
    }

    private static boolean hasModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null && instance.getModifier(id) != null;
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.amount() == amount && current.operation() == operation) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            // Transitorio: nao vai para o save; o tick reaplica depois de relogar.
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Fumaca, linhas e pulso da receita "transform" do perfil da forma que surge (VFX-2), no tamanho do corpo. */
    private static void effect(ServerPlayer player, ResourceLocation profile, float intensity) {
        if (player.level() instanceof ServerLevel level) {
            Vec3 at = player.position().add(0, player.getBbHeight() * 0.5, 0);
            VfxService.play(level, VfxService.TRANSFORM, at, player.getLookAngle(), intensity, 0.3F,
                    Optional.of(profile), "transform", (float) ServerConfig.FORM_HEIGHT.get().doubleValue());
        }
    }
}
