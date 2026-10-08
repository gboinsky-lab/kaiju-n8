// src/main/java/com/kn8/client/attribute/ReleaseInput.java
package com.kn8.client.attribute;

import org.lwjgl.glfw.GLFW;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.ReleaseInputC2S;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tecla de Release (0.5.0, decisao do Miguel): segurar G sobe a % (a aura aparece junto); Shift + G desce. O
 * cliente so manda o estado da tecla quando muda; o servidor confere o traje e decide a %.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ReleaseInput {

    public static final KeyMapping RELEASE_KEY = new KeyMapping("key.kn8.release", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.kn8");

    /** Ultimo estado enviado (+1, -1 ou 0). */
    private static int sent;

    private ReleaseInput() {
    }

    /** Mod bus. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RELEASE_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null) {
            sent = 0;
            return;
        }
        int wanted = 0;
        if (minecraft.screen == null && RELEASE_KEY.isDown()) {
            wanted = player.isShiftKeyDown() ? -1 : 1;
        }
        if (wanted != sent) {
            sent = wanted;
            PacketDistributor.sendToServer(new ReleaseInputC2S(wanted));
        }
    }
}
