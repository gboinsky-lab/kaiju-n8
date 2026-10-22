// src/main/java/com/kn8/client/vfx/draw/DrawnRenderer.java
package com.kn8.client.vfx.draw;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joml.Matrix4f;

import com.kn8.KN8Constants;
import com.kn8.common.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * VFX-R1: lista dos efeitos desenhados vivos no cliente. Envelhece no tick, desenha depois das particulas do vanilla
 * (para ficar por cima da fumaca e dos blocos transparentes) e esvazia ao sair do mundo. O teto de efeitos vivos e o
 * {@code [fx] maxDrawnEffects} (o mais velho sai primeiro).
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class DrawnRenderer {

    private static final List<Drawn> ACTIVE = new ArrayList<>();

    private DrawnRenderer() {
    }

    public static void add(Drawn drawn) {
        int max = ClientConfig.MAX_DRAWN_EFFECTS.get();
        if (max <= 0) {
            return;
        }
        while (ACTIVE.size() >= max) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(drawn);
    }

    /** Efeitos vivos (diagnostico e testes de carga). */
    public static int active() {
        return ACTIVE.size();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        for (Iterator<Drawn> it = ACTIVE.iterator(); it.hasNext();) {
            if (!it.next().tick()) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        // Duas passadas, uma por tipo: o buffer compartilhado encerra o anterior ao pedir outro tipo.
        VertexConsumer shade = buffers.getBuffer(KN8RenderTypes.SHADE);
        for (Drawn drawn : ACTIVE) {
            drawn.render(true, shade, m, camera, partial);
        }
        buffers.endBatch(KN8RenderTypes.SHADE);
        VertexConsumer glow = buffers.getBuffer(KN8RenderTypes.GLOW);
        for (Drawn drawn : ACTIVE) {
            drawn.render(false, glow, m, camera, partial);
        }
        buffers.endBatch(KN8RenderTypes.GLOW);
        pose.popPose();
    }
}
