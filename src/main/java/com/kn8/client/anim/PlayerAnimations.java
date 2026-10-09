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
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.FadeType;
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
    /** Blocos por tick acima dos quais o jogador esta andando (troca para a postura em movimento). */
    private static final double MOVING_PER_TICK = 0.02;
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
                // 0.5.0-D7: primeiro modificador da camada = velocidade do golpe (os de transicao vem depois dele e
                // contam o tempo da animacao, ja acelerado).
                controller.addModifierLast(new SpeedModifier(1.0F));
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
        // 0.5.0-D7 (Miguel: golpes continuos e mais rapidos com o Release): a animacao toca na velocidade da acao no
        // servidor; um golpe que comeca com o anterior ainda na volta parte da pose atual (sem corte seco), e o fim
        // de cada golpe se mistura com a postura de baixo.
        float speed = Float.isFinite(payload.speed()) && payload.speed() > 0.0F ? payload.speed() : 1.0F;
        controller.getModifiers().stream().filter(SpeedModifier.class::isInstance).map(SpeedModifier.class::cast)
                .findFirst().ifPresent(modifier -> modifier.speed = speed);
        controller.removeModifierIf(EndFade.class::isInstance);
        int fade = ClientConfig.ATTACK_FADE_TICKS.get();
        float startTick = catchUp * speed;
        boolean found;
        if (fade > 0 && controller.isActive()) {
            found = controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(fade,
                    EasingType.EASE_IN_OUT_SINE), payload.animation(), true);
            startTick = 0.0F;
        } else {
            found = controller.triggerAnimation(payload.animation(), startTick);
            if (found && fade > 0) {
                // Sem golpe tocando, a camada de combate entra misturando com a postura de baixo (saque e guarda nao
                // comecam na pose da postura e cortavam seco).
                controller.addModifierLast(AbstractFadeModifier.standardFadeIn(fade, EasingType.EASE_IN_OUT_SINE));
            }
        }
        Animation playing = found ? controller.getCurrentAnimationInstance() : null;
        if (fade > 0 && playing != null && playing.loopType() != Animation.LoopType.HOLD_ON_LAST_FRAME
                && playing.loopType() != Animation.LoopType.LOOP && playing.length() - startTick > fade) {
            controller.addModifierLast(new EndFade(fade, playing.length() - startTick));
        }
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
            // 0.5.0-D2: parado, andando ou correndo (cada familia tem as tres posturas de corpo inteiro).
            String variant = player.isSprinting() ? "stance_run"
                    : Math.hypot(player.getX() - player.xo, player.getZ() - player.zo) > MOVING_PER_TICK
                    ? "stance_move" : "stance";
            // 0.5.0-D6: morto, morrendo ou espectador fica sem postura (a corrida nao passa por cima da morte).
            boolean posing = !player.isDeadOrDying() && !player.isSpectator();
            ResourceLocation wanted = !posing ? null : weapon.flatMap(def -> WeaponHandling.profile(def, true)
                            .filter(WeaponProfileDef::stance).flatMap(profile -> def.profile()))
                    .map(id -> AnimationBridge.profileAction(id, variant)).orElse(null);
            ResourceLocation current = STANCES.get(player.getId());
            if (wanted == null ? current == null : wanted.equals(current)) {
                continue;
            }
            IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, STANCE_LAYER_ID);
            if (!(layer instanceof PlayerAnimationController controller)) {
                continue;
            }
            // Animacao que nao existe tambem fica guardada: nao tenta de novo a cada tick. 0.5.0-D6: parado, andando,
            // correndo e a troca de arma passam da pose atual para a nova em alguns ticks (sem corte seco).
            if (wanted == null || !controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(
                    ClientConfig.STANCE_FADE_TICKS.get(), EasingType.EASE_IN_OUT_SINE), wanted, true)) {
                controller.stop();
            }
            if (wanted == null) {
                STANCES.remove(player.getId());
            } else {
                STANCES.put(player.getId(), wanted);
            }
        }
    }

    /**
     * 0.5.0-D7: os ultimos {@code length} ticks do golpe passam para a postura de baixo (a PAL so tem a saida pelo
     * comprimento da animacao a partir do inicio; aqui o fim desconta o adiantamento da sincronia).
     */
    private static final class EndFade extends AbstractFadeModifier {
        private final float end;

        EndFade(int length, float end) {
            super(length);
            this.end = end;
        }

        @Override
        protected float getAlpha(String bone, float progress) {
            // Mesma curva do EASE_IN_OUT_SINE (o EasingType precisa da Mocha, fora do classpath: PAL sem transitivas).
            return (float) (0.5 - 0.5 * Math.cos(Math.PI * progress));
        }

        @Override
        protected FadeType getFadeType() {
            return FadeType.FADE_OUT;
        }

        @Override
        protected float getEndTime(String bone) {
            return end;
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
