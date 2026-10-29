package com.kn8.common.host;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.core.power.BodyStat;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * punhos), o limite de tempo e a perda de controle sao da T-3.</p>
 */
public final class KaijuFormService {

    /** Perfil visual e aura da forma (os mesmos do No. 8 aliado). */
    public static final ResourceLocation PROFILE = KN8Constants.id("kaiju_no8");
    /** Perfil visual da forma humana (o Kafka): efeito da volta. */
    private static final ResourceLocation HUMAN_PROFILE = KN8Constants.id("kafka");
    private static final String TAG_READY = "kn8_form_ready";
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

    /** Esta na forma do No. 8? Vale no servidor e no cliente (dado publico). */
    public static boolean isTransformed(Entity entity) {
        return entity instanceof Player player && player.getData(KN8Attachments.KAIJU_FORM.get());
    }

    /** Tecla da forma: transforma ou volta. */
    public static Result toggle(ServerPlayer player) {
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
        player.setData(KN8Attachments.KAIJU_FORM.get(), true);
        applyModifiers(player, true);
        player.setHealth(player.getMaxHealth());
        effect(player, PROFILE, 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 1.5F, 0.8F);
        KN8Constants.LOGGER.info("[kn8] {} virou o Kaiju No. 8", player.getGameProfile().getName());
        return Result.TRANSFORMED;
    }

    public static Result revert(ServerPlayer player) {
        if (!isTransformed(player)) {
            return Result.UNAVAILABLE;
        }
        player.setData(KN8Attachments.KAIJU_FORM.get(), false);
        applyModifiers(player, false);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        player.getPersistentData().putLong(TAG_READY,
                player.level().getGameTime() + ServerConfig.FORM_COOLDOWN_TICKS.get());
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
