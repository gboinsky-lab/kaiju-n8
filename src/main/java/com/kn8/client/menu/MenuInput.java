package com.kn8.client.menu;

import org.lwjgl.glfw.GLFW;

import com.kn8.KN8Constants;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Tecla do menu da Forca de Defesa (M por padrao, configuravel em Controles, categoria kn8). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class MenuInput {

    public static final KeyMapping MENU_KEY = new KeyMapping("key.kn8.menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M, "key.categories.kn8");

    private MenuInput() {
    }

    /** Mod bus. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MENU_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (MENU_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new DefenseForceScreen());
            }
        }
    }
}
