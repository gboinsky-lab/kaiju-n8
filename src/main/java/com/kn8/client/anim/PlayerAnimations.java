// src/main/java/com/kn8/client/anim/PlayerAnimations.java
package com.kn8.client.anim;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.anim.AnimTriggerS2C;
import com.kn8.common.anim.AnimationBridge;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.WeaponHandling;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.data.def.WeaponProfileDef;
import com.kn8.common.config.ClientConfig;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.core.anim.AnimTiming;
import com.mojang.brigadier.context.CommandContext;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Lado cliente das animacoes do jogador (M9, padrao provado no PT6): camada {@code kn8:combat} na Player Animation
 * Library, prioridade 2000 (acima da camada padrao da PAL, 1000), que so toca o que o servidor manda.
 *
 * <p>Sincronia: a animacao comeca adiantada pelo atraso medido ({@link AnimTiming}), ate
 * {@code fx.animationMaxCatchUpTicks}. {@code /kn8client anim} mostra os ultimos disparos recebidos (atraso bruto e
 * compensacao), para conferir o criterio de +-2 ticks.</p>
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class PlayerAnimations {

    public static final ResourceLocation LAYER_ID = KN8Constants.id("combat");
    private static final int LAYER_PRIORITY = 2000;
    /**
     * 0.5.0-D: postura com a arma na mao ({@code player.<perfil>.stance}, em laco), abaixo da camada de combate (os
     * golpes passam por cima) e acima da padrao da PAL. Cada cliente decide pelo item que ve na mao de cada jogador:
     * nao precisa de pacote.
     */
    public static final ResourceLocation STANCE_LAYER_ID = KN8Constants.id("stance");
    private static final int STANCE_PRIORITY = 1500;
    /** Primeira pessoa: bracos e itens do modelo de terceira pessoa durante os golpes (perfil com first_person). */
    private static final FirstPersonConfiguration FIRST_PERSON = new FirstPersonConfiguration(true, true, true, true);
    private static final int HISTORY_SIZE = 10;

    /** Diagnostico deste cliente (ultimos disparos); nao e estado de jogo. */
    private record Received(String player, ResourceLocation animation, long delay, int catchUp, boolean found) {
    }

    private static final Deque<Received> HISTORY = new ArrayDeque<>();
    /** Postura que cada jogador visivel esta tocando (id da entidade -> animacao); so exibicao deste cliente. */
    private static final Map<Integer, ResourceLocation> STANCES = new HashMap<>();

    private PlayerAnimations() {
    }

    /** Mod bus: registra a camada na PAL e o tratador do pacote na ponte comum. */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER_ID, LAYER_PRIORITY, player -> {
                PlayerAnimationController controller = new PlayerAnimationController(player,
                        (anim, state, setter) -> PlayState.STOP);
                controller.setFirstPersonModeHandler(anim -> firstPerson(player)
                        ? FirstPersonMode.THIRD_PERSON_MODEL : FirstPersonMode.NONE);
                controller.setFirstPersonConfiguration(FIRST_PERSON);
                return controller;
            });
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(STANCE_LAYER_ID, STANCE_PRIORITY,
                    player -> new PlayerAnimationController(player, (anim, state, setter) -> PlayState.STOP));
            KN8ClientHooks.register(AnimTriggerS2C.TYPE, PlayerAnimations::onTrigger);
        });
    }

    private static void onTrigger(AnimTriggerS2C payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
        if (!(layer instanceof PlayerAnimationController controller)) {
            KN8Constants.LOGGER.warn("[kn8] Camada de animacao {} ausente em {}", LAYER_ID,
                    player.getGameProfile().getName());
            return;
        }
        if (payload.stop()) {
            controller.stop();
            return;
        }
        long clientTick = minecraft.level.getGameTime();
        int catchUp = AnimTiming.catchUpTicks(payload.serverTick(), clientTick,
                ClientConfig.ANIMATION_MAX_CATCH_UP_TICKS.get());
        boolean found = controller.triggerAnimation(payload.animation(), catchUp);
        remember(new Received(player.getGameProfile().getName(), payload.animation(),
                AnimTiming.delayTicks(payload.serverTick(), clientTick), catchUp, found));
    }

    /** Os golpes desta arma aparecem em primeira pessoa? ({@code first_person} do perfil; sem perfil, nao). */
    private static boolean firstPerson(AbstractClientPlayer player) {
        return heldProfile(player).map(WeaponProfileDef::firstPerson).orElse(false);
    }

    private static Optional<WeaponProfileDef> heldProfile(AbstractClientPlayer player) {
        return CombatService.heldWeapon(player).flatMap(weapon -> WeaponHandling.profile(weapon, true));
    }

    /** Postura de cada jogador pela arma na mao: troca so quando muda (a animacao fica em laco). */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STANCES.clear();
            return;
        }
        STANCES.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
        for (AbstractClientPlayer player : minecraft.level.players()) {
            Optional<WeaponDef> weapon = CombatService.heldWeapon(player);
            ResourceLocation wanted = weapon.flatMap(def -> WeaponHandling.profile(def, true)
                            .filter(WeaponProfileDef::stance).flatMap(profile -> def.profile()))
                    .map(id -> AnimationBridge.profileAction(id, "stance")).orElse(null);
            ResourceLocation current = STANCES.get(player.getId());
            if (wanted == null ? current == null : wanted.equals(current)) {
                continue;
            }
            IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, STANCE_LAYER_ID);
            if (!(layer instanceof PlayerAnimationController controller)) {
                continue;
            }
            // Animacao que nao existe tambem fica guardada: nao tenta de novo a cada tick.
            if (wanted == null || !controller.triggerAnimation(wanted)) {
                controller.stop();
            }
            if (wanted == null) {
                STANCES.remove(player.getId());
            } else {
                STANCES.put(player.getId(), wanted);
            }
        }
    }

    private static void remember(Received received) {
        HISTORY.addFirst(received);
        while (HISTORY.size() > HISTORY_SIZE) {
            HISTORY.removeLast();
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kn8client").then(Commands.literal("anim")
                .executes(PlayerAnimations::showHistory)));
    }

    private static int showHistory(CommandContext<CommandSourceStack> ctx) {
        if (HISTORY.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.anim.none"), false);
            return 0;
        }
        for (Received received : HISTORY) {
            ctx.getSource().sendSuccess(() -> Component.translatable(received.found()
                            ? "kn8.client.anim.entry" : "kn8.client.anim.missing", received.player(),
                    received.animation().toString(), received.delay(), received.catchUp()), false);
        }
        return HISTORY.size();
    }
}
