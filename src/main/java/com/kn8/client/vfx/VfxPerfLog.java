// src/main/java/com/kn8/client/vfx/VfxPerfLog.java
package com.kn8.client.vfx;

import org.slf4j.Logger;

import com.kn8.KN8Constants;
import com.kn8.client.vfx.draw.DrawnRenderer;
import com.kn8.common.config.ClientConfig;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * VFX-10 (regressao dos efeitos): com {@code [fx] perfLog} ligado, escreve no log a cada 5 s o FPS, os efeitos
 * desenhados vivos (agora e o pico da janela), o tempo medio de desenho deles por quadro com efeitos e a memoria
 * usada. Serve para medir a carga com muitos personagens lutando, sem abrir a tela F3 nos dois clientes.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class VfxPerfLog {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INTERVAL_TICKS = 100;

    private static int ticks;
    private static int peak;
    private static int minFps = Integer.MAX_VALUE;

    private VfxPerfLog() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientConfig.PERF_LOG.get()) {
            ticks = 0;
            return;
        }
        peak = Math.max(peak, DrawnRenderer.active());
        minFps = Math.min(minFps, minecraft.getFps());
        if (++ticks < INTERVAL_TICKS) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        LOGGER.info("[kn8 perf] fps={} minFps={} drawn={} drawnPeak={} drawMs={} memMb={} maxMb={}",
                minecraft.getFps(), minFps, DrawnRenderer.active(), peak,
                String.format(java.util.Locale.ROOT, "%.3f", DrawnRenderer.takeDrawMillisPerFrame()), usedMb,
                runtime.maxMemory() / (1024 * 1024));
        ticks = 0;
        peak = 0;
        minFps = Integer.MAX_VALUE;
    }
}
