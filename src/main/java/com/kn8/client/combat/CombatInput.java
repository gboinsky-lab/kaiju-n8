// src/main/java/com/kn8/client/combat/CombatInput.java
package com.kn8.client.combat;

import org.lwjgl.glfw.GLFW;

import com.kn8.KN8Constants;
import com.kn8.common.combat.CombatAction;
import com.kn8.common.combat.CombatInputC2S;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.core.combat.CombatMath;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Entrada de combate do jogador (M10a). Com uma arma do kn8 na mao: clique esquerdo = golpe leve, clique direito =
 * golpe pesado/carregado ao soltar (no lugar do ataque e do uso vanilla); V segurado = bloqueio; Z = esquiva na
 * direcao do movimento; Alt esquerdo = dash; R = ataque especial da arma (0.5).
 * Teclas configuraveis em Controles (categoria kn8). O cliente so manda a intencao; o servidor decide tudo.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class CombatInput {

    private static final String CATEGORY = "key.categories.kn8";
    public static final KeyMapping BLOCK_KEY = new KeyMapping("key.kn8.block", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping DODGE_KEY = new KeyMapping("key.kn8.dodge", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z, CATEGORY);
    /** 0.1-B: dash (GDD secao 7). */
    public static final KeyMapping DASH_KEY = new KeyMapping("key.kn8.dash", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    /** 0.5: ataque especial da arma (so armas especiais, com {@code special} no JSON). */
    public static final KeyMapping SPECIAL_KEY = new KeyMapping("key.kn8.special", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, CATEGORY);

    /** Ultimo estado enviado do bloqueio (para mandar so as mudancas); estado de entrada deste cliente. */
    private static boolean blockSent;
    /** Ataque carregado em andamento (clique direito segurado com lamina) e quando comecou (tick do cliente). */
    private static boolean useStartedOnCarcass;
    private static boolean charging;
    private static long chargeStartTick;
    private static long clientTicks;

    private CombatInput() {
    }

    /** Mod bus. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(BLOCK_KEY);
        event.register(DODGE_KEY);
        event.register(DASH_KEY);
        event.register(SPECIAL_KEY);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || CombatService.heldWeapon(player).isEmpty()) {
            return;
        }
        // M11b: mirando numa carcaca, o clique direito e o desmonte (interacao vanilla), nao o golpe pesado.
        if (event.isUseItem() && Minecraft.getInstance().hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof CarcassEntity) {
            useStartedOnCarcass = true;
            return;
        }
        // Clique direito que comecou numa carcaca: quando ela some (ultima etapa do desmonte) o botao continua
        // segurado e o vanilla repete o evento; isso nao pode virar ataque carregado ate soltar (teste em jogo, 0.2).
        if (event.isUseItem() && useStartedOnCarcass) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (event.isAttack()) {
            send(CombatAction.LIGHT, true, 0, 0);
        } else if (event.isUseItem()) {
            // 0.1-B: segurar o clique direito carrega; o golpe sai ao soltar (CHARGE_RELEASE no tick do cliente).
            // O vanilla repete este evento a cada 4 ticks enquanto o botao esta segurado: so o primeiro conta.
            if (!charging) {
                charging = true;
                chargeStartTick = clientTicks;
                send(CombatAction.CHARGE_START, true, 0, 0);
            }
        } else {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        clientTicks++;
        if (player == null || minecraft.getConnection() == null) {
            blockSent = false;
            charging = false;
            return;
        }
        if (!minecraft.options.keyUse.isDown()) {
            useStartedOnCarcass = false;
        }
        if (charging && !minecraft.options.keyUse.isDown()) {
            charging = false;
            send(CombatAction.CHARGE_RELEASE, false, 0, 0);
        }
        while (DASH_KEY.consumeClick()) {
            if (CombatService.heldWeapon(player).isPresent()) {
                float[] direction = movementDirection(player);
                send(CombatAction.DASH, true, direction[0], direction[1]);
            }
        }
        boolean armed = CombatService.heldWeapon(player).isPresent();
        while (SPECIAL_KEY.consumeClick()) {
            if (armed && minecraft.screen == null) {
                send(CombatAction.SPECIAL, true, 0, 0);
            }
        }
        boolean wantBlock = armed && minecraft.screen == null && BLOCK_KEY.isDown();
        if (wantBlock != blockSent) {
            blockSent = wantBlock;
            send(CombatAction.BLOCK, wantBlock, 0, 0);
        }
        while (DODGE_KEY.consumeClick()) {
            if (armed) {
                float[] direction = movementDirection(player);
                send(CombatAction.DODGE, true, direction[0], direction[1]);
            }
        }
    }

    /**
     * Carga mostrada na HUD (0-1), estimada no cliente com os mesmos numeros do config do servidor (sincronizado no
     * login); -1 = nao carregando, 0 ainda abaixo do minimo. Quem decide o dano e o servidor.
     */
    public static float chargeFraction() {
        if (!charging || !ServerConfig.SPEC.isLoaded()) {
            return -1.0F;
        }
        float fraction = CombatMath.chargeFraction(clientTicks - chargeStartTick, ServerConfig.CHARGE_MIN_TICKS.get(),
                ServerConfig.CHARGE_MAX_TICKS.get());
        return Math.max(0.0F, fraction);
    }

    /** Direcao do movimento pedido pelas teclas, no espaco do mundo (zero = parado; o servidor esquiva para tras). */
    private static float[] movementDirection(LocalPlayer player) {
        float strafe = player.input.leftImpulse;
        float forward = player.input.forwardImpulse;
        if (strafe == 0 && forward == 0) {
            return new float[] {0, 0};
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);
        return new float[] {strafe * cos - forward * sin, forward * cos + strafe * sin};
    }

    private static void send(CombatAction action, boolean pressed, float dirX, float dirZ) {
        PacketDistributor.sendToServer(new CombatInputC2S(action.ordinal(), pressed, dirX, dirZ));
    }
}
