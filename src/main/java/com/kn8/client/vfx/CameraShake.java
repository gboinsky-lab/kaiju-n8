// src/main/java/com/kn8/client/vfx/CameraShake.java
package com.kn8.client.vfx;

import com.kn8.KN8Constants;
import com.kn8.common.config.ClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Tremor de camera (Etapa D): impactos fortes somam "trauma" (0-1), que decai sozinho; a camera treme com o quadrado
 * do trauma, menos com a distancia e no maximo {@code fx.cameraShake}. Estado so de desenho deste cliente.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class CameraShake {

    private static final float DECAY_PER_TICK = 0.05F;
    private static final float MAX_DEGREES = 3.0F;
    private static final double FULL_STRENGTH_DISTANCE = 8.0;
    private static final double FADE_DISTANCE = 48.0;

    private static float trauma;
    private static long seed;

    private CameraShake() {
    }

    static void add(float amount, Vec3 source) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double distance = player.position().distanceTo(source);
        double falloff = distance <= FULL_STRENGTH_DISTANCE ? 1.0
                : Math.max(0.0, 1.0 - (distance - FULL_STRENGTH_DISTANCE) / FADE_DISTANCE);
        trauma = (float) Math.min(1.0, trauma + amount * falloff);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        trauma = Math.max(0.0F, trauma - DECAY_PER_TICK);
        seed++;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        double limit = ClientConfig.CAMERA_SHAKE.get();
        if (trauma <= 0 || limit <= 0) {
            return;
        }
        float shake = (float) (trauma * trauma * limit * MAX_DEGREES);
        double time = seed + event.getPartialTick();
        event.setYaw(event.getYaw() + shake * (float) Math.sin(time * 1.7));
        event.setPitch(event.getPitch() + shake * (float) Math.sin(time * 2.3 + 1.0));
        event.setRoll(event.getRoll() + shake * 0.5F * (float) Math.sin(time * 1.3 + 2.0));
    }
}
