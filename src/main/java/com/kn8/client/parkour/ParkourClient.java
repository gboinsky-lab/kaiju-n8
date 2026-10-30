// src/main/java/com/kn8/client/parkour/ParkourClient.java
package com.kn8.client.parkour;

import org.lwjgl.glfw.GLFW;

import com.kn8.KN8Constants;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.common.parkour.ParkourInputC2S;
import com.kn8.common.parkour.ParkourMove;
import com.kn8.common.parkour.ParkourProbe;
import com.kn8.common.parkour.ParkourState;
import com.kn8.common.parkour.ParkourStateS2C;
import com.kn8.core.parkour.ParkourMath;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Parkour do jogador no cliente (0.5.0-E). So detecta e pede ({@link ParkourInputC2S}); o servidor decide. Quando o
 * servidor aprova um estado continuo ({@link ParkourStateS2C}), este cliente aplica a fisica dele tick a tick com os
 * numeros recebidos (o movimento do jogador e do cliente no Minecraft).
 *
 * <p>Comandos (Miguel pode trocar a tecla nas opcoes):</p>
 * <ul>
 *   <li>pular correndo contra um obstaculo: vault (baixo) ou escalada curta (ate 2,5 blocos);</li>
 *   <li>C correndo no chao: salto longo;</li>
 *   <li>C no ar: segura a borda a frente; sem borda, corre pela parede ao lado; de frente para a parede,
 *   escorrega;</li>
 *   <li>pular na parede: wall-jump; pendurado: sobe; Shift: solta;</li>
 *   <li>Shift correndo: slide; Shift ao cair de 4+ blocos: rolamento (so no servidor).</li>
 *   <li>FIX-3: C de frente para uma parede (no chao ou no ar): escala o predio (W sobe, S desce, Espaco pula para
 *   tras, Shift solta); no topo agarra a borda sozinho e, com C ainda segurado, sobe;</li>
 *   <li>FIX-3: caindo ou no alto do pulo contra uma borda na altura das maos, agarra sem a tecla; pendurado, A/D anda
 *   de lado.</li>
 * </ul>
 *
 * <p>Os alcances abaixo sao da deteccao do cliente (um pouco menores que o {@code probeReach} do servidor, que
 * confere com folga); os numeros de jogo vem do config do servidor.</p>
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ParkourClient {

    public static final KeyMapping PARKOUR_KEY = new KeyMapping("key.kn8.parkour", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C, "key.categories.kn8");

    /** Deteccao do cliente: distancia do obstaculo/parede e maior altura procurada. */
    private static final double OBSTACLE_REACH = 0.6;
    private static final double WALL_REACH = 0.45;
    private static final double MAX_PROBE_HEIGHT = 4.5;
    /** Borda procurada pelo cliente; o servidor confere com ledgeMin/MaxHeight x nivel (forte alcanca mais). */
    private static final double LEDGE_MIN = 0.9;
    private static final double LEDGE_MAX = 3.6;
    /** Segurando C no ar, tenta de novo a cada tantos ticks (sem inundar o servidor). */
    private static final int RETRY_TICKS = 4;
    /** Wall-jump ainda pedido tantos ticks depois de sair da parede (o servidor tem a propria folga). */
    private static final int COYOTE_TICKS = 5;
    /** Ticks sem parede/chao antes de soltar o estado localmente. */
    private static final int LOST_TICKS = 3;
    /** Empurrao contra a parede por tick (mantem o contato no wall-run e pendurado). */
    private static final double STICK = 0.04;
    /** Velocidade vertical maxima ao ajustar a altura pendurado. */
    private static final double HANG_SNAP = 0.5;
    /** Slide termina abaixo desta velocidade. */
    private static final double SLIDE_MIN_SPEED = 0.08;
    /** FIX-3: borda agarrada sem a tecla (altura acima dos pes) e subida mais rapida que isso nao agarra. */
    private static final double AUTO_LEDGE_MIN = 1.2;
    private static final double AUTO_LEDGE_MAX = 2.0;
    private static final double AUTO_LEDGE_RISE = 0.1;
    /** FIX-3: escalando, o topo dentro desta altura vira borda; com C segurado, sobe depois de tantos ticks. */
    private static final double CLIMB_TOP_MAX = 2.0;
    private static final int AUTO_MANTLE_TICKS = 3;

    private static LocalPlayer boundPlayer;
    private static ParkourStateS2C active = ParkourStateS2C.NONE;
    private static int activeTicks;
    private static int lostTicks;
    private static int leftWallTicks = Integer.MAX_VALUE;
    private static int retryIn;
    private static int groundRetryIn;
    private static boolean lastJump;
    private static boolean lastShift;
    private static boolean lastKey;
    private static boolean wasSprinting;
    /** FIX-3: entrada guardada para a fisica do tick (escalada: para a frente; borda: para o lado). */
    private static float climbInput;
    private static float shimmyInput;

    private ParkourClient() {
    }

    /** Mod bus. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PARKOUR_KEY);
    }

    /** Chamado no setup do cliente. */
    public static void install() {
        KN8ClientHooks.register(ParkourStateS2C.TYPE, ParkourClient::onState);
    }

    /** Estado atual do jogador local (para HUD e testes). */
    public static ParkourState state() {
        return active.parkourState();
    }

    private static void onState(ParkourStateS2C payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        ParkourState old = active.parkourState();
        active = payload;
        activeTicks = 0;
        lostTicks = 0;
        if (player == null) {
            return;
        }
        if (payload.parkourState() == ParkourState.SLIDE) {
            player.setForcedPose(Pose.SWIMMING);
        } else if (old == ParkourState.SLIDE) {
            player.setForcedPose(null);
        }
        if (old.onWall() && !payload.parkourState().onWall()) {
            leftWallTicks = 0;
        }
    }

    private static void send(ParkourMove move, int side) {
        PacketDistributor.sendToServer(new ParkourInputC2S(move, side));
    }

    /** Solta o estado aqui sem esperar o servidor (pulo, Shift, perda da parede) e avisa o servidor. */
    private static void endLocally(LocalPlayer player, boolean tellServer) {
        ParkourState old = active.parkourState();
        if (old == ParkourState.NONE) {
            return;
        }
        if (old == ParkourState.SLIDE) {
            player.setForcedPose(null);
        }
        if (old.onWall()) {
            leftWallTicks = 0;
        }
        active = ParkourStateS2C.NONE;
        if (tellServer) {
            send(ParkourMove.STOP, 0);
        }
    }

    // --- entrada (dentro do aiStep, antes do movimento) ---------------------------------------------------------

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        if (player != boundPlayer) {
            // Jogador novo (login, respawn, dimensao): nada do anterior vale.
            boundPlayer = player;
            active = ParkourStateS2C.NONE;
            leftWallTicks = Integer.MAX_VALUE;
        }
        Input input = event.getInput();
        boolean key = minecraft.screen == null && PARKOUR_KEY.isDown();
        boolean jumpPressed = input.jumping && !lastJump;
        boolean shiftPressed = input.shiftKeyDown && !lastShift;
        // consumeClick pega o toque rapido que comeca e termina entre dois ticks (isDown nao veria); com a tecla ja
        // segurada os cliques sao a repeticao do teclado e nao contam.
        boolean clicked = false;
        while (PARKOUR_KEY.consumeClick()) {
            clicked = true;
        }
        boolean keyPressed = minecraft.screen == null && !lastKey && (clicked || key);
        lastJump = input.jumping;
        lastShift = input.shiftKeyDown;
        lastKey = key;
        boolean sprinting = player.isSprinting() || wasSprinting;
        wasSprinting = player.isSprinting();
        if (player.isSpectator() || player.getAbilities().flying || player.isPassenger() || player.isInWater()
                || player.isFallFlying()) {
            endLocally(player, true);
            return;
        }
        ParkourState state = active.parkourState();
        switch (state) {
            case WALL_RUN, WALL_SLIDE -> {
                if (jumpPressed) {
                    endLocally(player, false);
                    send(ParkourMove.WALL_JUMP, 0);
                } else if (shiftPressed) {
                    endLocally(player, true);
                }
                input.jumping = false;
            }
            case LEDGE_HANG -> {
                shimmyInput = input.leftImpulse;
                if (jumpPressed || key && input.forwardImpulse > 0.0F && activeTicks >= AUTO_MANTLE_TICKS) {
                    endLocally(player, false);
                    send(ParkourMove.LEDGE_CLIMB, 0);
                } else if (shiftPressed) {
                    endLocally(player, true);
                }
                input.forwardImpulse = 0.0F;
                input.leftImpulse = 0.0F;
                input.jumping = false;
            }
            case SLIDE -> {
                if (!input.shiftKeyDown || jumpPressed) {
                    endLocally(player, true);
                }
            }
            case CLIMB -> {
                climbInput = input.forwardImpulse;
                if (jumpPressed) {
                    endLocally(player, false);
                    send(ParkourMove.WALL_JUMP, 0);
                } else if (shiftPressed) {
                    endLocally(player, true);
                } else if (--retryIn <= 0) {
                    // No topo do predio: a borda aparece na altura das maos e vira o pendurado (depois sobe).
                    retryIn = RETRY_TICKS;
                    Vec3 toWall = new Vec3(-active.normalX(), 0.0, -active.normalZ());
                    double top = ParkourProbe.obstacleHeight(player, toWall, OBSTACLE_REACH, CLIMB_TOP_MAX);
                    if (top >= LEDGE_MIN) {
                        send(ParkourMove.LEDGE_GRAB, -1);
                    }
                }
                input.forwardImpulse = 0.0F;
                input.leftImpulse = 0.0F;
                input.jumping = false;
            }
            case NONE -> idleInput(player, input, key, keyPressed, jumpPressed, shiftPressed, sprinting);
        }
    }

    private static void idleInput(LocalPlayer player, Input input, boolean key, boolean keyPressed,
            boolean jumpPressed, boolean shiftPressed, boolean sprinting) {
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        if (player.onGround()) {
            groundRetryIn--;
            // Segurar Espaco tambem vale (de novo a cada RETRY_TICKS x 2, sem inundar o servidor).
            if (input.jumping && input.forwardImpulse > 0.0F && (jumpPressed || groundRetryIn <= 0)) {
                double height = ParkourProbe.obstacleHeight(player, forward, OBSTACLE_REACH, MAX_PROBE_HEIGHT);
                if (height > player.maxUpStep()) {
                    // O pulo vanilla sai junto; o impulso do servidor chega logo depois e o substitui.
                    send(ParkourMove.VAULT, 0);
                    groundRetryIn = RETRY_TICKS * 2;
                }
            } else if ((keyPressed || key && groundRetryIn <= 0) && ParkourProbe.wallAt(player, forward, WALL_REACH)
                    && ParkourProbe.obstacleHeight(player, forward, OBSTACLE_REACH, CLIMB_TOP_MAX) < 0.0) {
                // FIX-3: parede mais alta que a escalada curta: escala o predio (tambem chegando nela com C segurado).
                send(ParkourMove.WALL_CLIMB, 0);
                groundRetryIn = RETRY_TICKS * 2;
            } else if (keyPressed && sprinting) {
                send(ParkourMove.LONG_JUMP, 0);
            } else if (shiftPressed && sprinting) {
                send(ParkourMove.SLIDE, 0);
            }
            return;
        }
        if (jumpPressed && leftWallTicks <= COYOTE_TICKS) {
            send(ParkourMove.WALL_JUMP, 0);
            leftWallTicks = Integer.MAX_VALUE;
            return;
        }
        if (!key) {
            // FIX-3 (Miguel: "agarrar bordas"): sem a tecla, no alto do pulo ou caindo contra a borda.
            if (--retryIn <= 0) {
                retryIn = RETRY_TICKS;
                double ledge = ParkourProbe.obstacleHeight(player, forward, OBSTACLE_REACH, AUTO_LEDGE_MAX);
                if (ParkourMath.autoLedge(ledge, player.getDeltaMovement().y, input.forwardImpulse > 0.0F,
                        AUTO_LEDGE_MIN, AUTO_LEDGE_MAX, AUTO_LEDGE_RISE)) {
                    send(ParkourMove.LEDGE_GRAB, -1);
                }
            }
            return;
        }
        if (!keyPressed && --retryIn > 0) {
            return;
        }
        retryIn = RETRY_TICKS;
        double ledge = ParkourProbe.obstacleHeight(player, forward, OBSTACLE_REACH, LEDGE_MAX);
        if (ledge >= LEDGE_MIN) {
            send(ParkourMove.LEDGE_GRAB, 0);
            return;
        }
        if (input.forwardImpulse > 0.0F && ParkourProbe.wallAt(player, forward, WALL_REACH)) {
            // FIX-3: no ar de frente para a parede, andando para ela: escala (sem andar, escorrega como antes).
            send(ParkourMove.WALL_CLIMB, 0);
            return;
        }
        int side = ParkourProbe.wallSide(player, WALL_REACH, 1);
        Vec3 motion = player.getDeltaMovement();
        if (side != 0 && sprinting && motion.horizontalDistanceSqr() > 0.01) {
            send(ParkourMove.WALL_RUN, side);
        } else if (ParkourProbe.wallAt(player, forward, WALL_REACH) && motion.y < 0.0) {
            send(ParkourMove.WALL_SLIDE, 0);
        }
    }

    // --- fisica dos estados (depois do tick do jogador: vale para o movimento do proximo tick) --------------------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player != boundPlayer) {
            return;
        }
        if (leftWallTicks < Integer.MAX_VALUE) {
            leftWallTicks++;
        }
        ParkourState state = active.parkourState();
        if (state == ParkourState.NONE) {
            return;
        }
        if (activeTicks > active.maxTicks() + LOST_TICKS) {
            endLocally(player, true);
            return;
        }
        Vec3 toWall = new Vec3(-active.normalX(), 0.0, -active.normalZ());
        Vec3 dir = new Vec3(active.dirX(), 0.0, active.dirZ());
        Vec3 motion = player.getDeltaMovement();
        boolean valid = switch (state) {
            case WALL_RUN, WALL_SLIDE -> !player.onGround() && ParkourProbe.wallAt(player, toWall, WALL_REACH);
            case LEDGE_HANG -> true;
            case CLIMB -> ParkourProbe.wallAt(player, toWall, WALL_REACH);
            case SLIDE -> true;
            case NONE -> false;
        };
        lostTicks = valid ? 0 : lostTicks + 1;
        if (lostTicks > LOST_TICKS) {
            endLocally(player, true);
            return;
        }
        switch (state) {
            case WALL_RUN -> {
                double vy = ParkourMath.wallRunVertical(activeTicks, active.maxTicks(), active.vertical());
                Vec3 along = dir.scale(active.speed()).add(toWall.scale(STICK));
                player.setDeltaMovement(along.x, vy, along.z);
            }
            case WALL_SLIDE -> {
                double vy = ParkourMath.wallSlideVertical(motion.y, active.vertical());
                player.setDeltaMovement(motion.x * 0.5 + toWall.x * STICK, vy, motion.z * 0.5 + toWall.z * STICK);
            }
            case LEDGE_HANG -> {
                double vy = Math.max(-HANG_SNAP, Math.min(HANG_SNAP, active.vertical() - player.getY()));
                // FIX-3: A/D anda de lado ao longo da parede (a altura do pendurado fica a mesma).
                Vec3 side = ParkourProbe.right(player.getYRot()).scale(-shimmyInput * active.speed());
                Vec3 along = side.subtract(toWall.scale(side.dot(toWall)));
                player.setDeltaMovement(toWall.x * STICK + along.x, vy, toWall.z * STICK + along.z);
            }
            case CLIMB -> {
                double vy = ParkourMath.climbVertical(climbInput, active.vertical());
                player.setDeltaMovement(toWall.x * STICK, vy, toWall.z * STICK);
            }
            case SLIDE -> {
                double speed = ParkourMath.slideSpeed(active.speed(), activeTicks, active.friction());
                if (speed < SLIDE_MIN_SPEED) {
                    endLocally(player, true);
                    return;
                }
                player.setDeltaMovement(dir.x * speed, motion.y, dir.z * speed);
            }
            default -> {
            }
        }
        player.resetFallDistance();
        activeTicks++;
    }
}
