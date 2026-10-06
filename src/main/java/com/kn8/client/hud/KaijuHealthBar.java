// src/main/java/com/kn8/client/hud/KaijuHealthBar.java
package com.kn8.client.hud;

import java.util.Locale;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.core.kaiju.KaijuScale;
import com.kn8.core.ui.HudMath;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Barra de vida do kaiju (0.1-B), no topo da tela, no estilo da HUD do traje: nome, categoria e porte
 * (Yoju/Honju/Daikaiju; pequeno/medio/grande), vida, vida do nucleo (pisca "EXPOSTO" quando exposto) e estado.
 * Aparece ao mirar num kaiju (ou numa parte dele) a ate {@link #MAX_DISTANCE} blocos e fica alguns segundos depois de
 * desviar a mira. So mostra o que o servidor sincronizou (vida vanilla + nucleo pelo SynchedEntityData).
 */
public final class KaijuHealthBar {

    private static final double MAX_DISTANCE = 48.0;
    private static final long LINGER_MS = 5000;
    private static final int INFO_GAP = 6;
    private static final float MIN_INFO_SCALE = 0.6F;
    private static final int WIDTH = 220;
    private static final int TOP = 6;
    private static final int BAR_HEIGHT = 5;
    private static final int CORE_HEIGHT = 3;
    private static final float SMOOTHING = 0.25F;
    private static final long BLINK_MS = 250;

    private static final int COLOR_PANEL = 0xB00C1320;
    private static final int COLOR_OUTLINE = 0xC0B8C4D0;
    private static final int COLOR_EMPTY = 0xFF26303C;
    private static final int COLOR_HEALTH = 0xFFE53935;
    private static final int COLOR_HEALTH_LIGHT = 0x40FFFFFF;
    private static final int COLOR_CORE = 0xFFFF9800;
    private static final int COLOR_CORE_EXPOSED = 0xFFFFE082;
    private static final int COLOR_TEXT = 0xFFE3E8EE;
    private static final int COLOR_SUBTEXT = 0xFF9FB3C8;

    /** Estado de desenho deste cliente: ultimo kaiju mirado e quando. */
    private static KaijuEntity target;
    private static long lastSeen;
    private static float shownHealth = -1;

    private KaijuHealthBar() {
    }

    /** Mod bus: no topo, junto da barra de chefe vanilla. */
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, KN8Constants.id("kaiju_health"), KaijuHealthBar::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        updateTarget(minecraft);
        if (target == null) {
            return;
        }
        KaijuEntity kaiju = target;
        float health = kaiju.getHealth() / Math.max(1.0F, kaiju.getMaxHealth());
        shownHealth = shownHealth < 0 ? health : HudMath.approach(shownHealth, health, SMOOTHING);

        Font font = minecraft.font;
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = TOP;
        int height = 30;
        graphics.fill(x - 4, y - 3, x + WIDTH + 4, y + height, COLOR_PANEL);
        graphics.fill(x - 4, y - 3, x + WIDTH + 4, y - 2, COLOR_OUTLINE);
        graphics.fill(x - 4, y + height - 1, x + WIDTH + 4, y + height, COLOR_OUTLINE);

        graphics.drawString(font, kaiju.getDisplayName(), x, y, COLOR_TEXT, true);
        Component info = info(kaiju);
        // Nome comprido (ex.: "Resurrected Primigenius") encostava no texto da direita: diminui o texto da direita
        // ate caber no espaco que sobra.
        int free = WIDTH - font.width(kaiju.getDisplayName()) - INFO_GAP;
        float infoScale = Math.min(1.0F, Math.max(MIN_INFO_SCALE, free / (float) Math.max(1, font.width(info))));
        graphics.pose().pushPose();
        graphics.pose().translate(x + WIDTH - font.width(info) * infoScale, y + (1.0F - infoScale) * font.lineHeight, 0);
        graphics.pose().scale(infoScale, infoScale, 1.0F);
        graphics.drawString(font, info, 0, 0, COLOR_SUBTEXT, true);
        graphics.pose().popPose();

        int barY = y + 11;
        graphics.fill(x, barY, x + WIDTH, barY + BAR_HEIGHT, COLOR_EMPTY);
        graphics.fill(x, barY, x + Math.round(WIDTH * shownHealth), barY + BAR_HEIGHT, COLOR_HEALTH);
        graphics.fill(x, barY, x + Math.round(WIDTH * shownHealth), barY + 1, COLOR_HEALTH_LIGHT);

        float core = kaiju.syncedCoreFraction();
        if (core >= 0) {
            int coreY = barY + BAR_HEIGHT + 2;
            boolean exposed = kaiju.syncedCoreExposed();
            int color = exposed && (System.currentTimeMillis() / BLINK_MS) % 2 == 0 ? COLOR_CORE_EXPOSED
                    : COLOR_CORE;
            graphics.fill(x, coreY, x + WIDTH, coreY + CORE_HEIGHT, COLOR_EMPTY);
            graphics.fill(x, coreY, x + Math.round(WIDTH * core), coreY + CORE_HEIGHT, color);
            if (exposed) {
                Component tag = Component.translatable("kn8.hud.kaiju.exposed");
                graphics.drawString(font, tag, x + WIDTH - font.width(tag), coreY + CORE_HEIGHT + 1, color, true);
            }
        }
    }

    /** Mirado agora (kaiju ou parte dele) -> vira o alvo; sem mira, o alvo fica alguns segundos. */
    private static void updateTarget(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        HitResult hit = minecraft.hitResult;
        KaijuEntity aimed = null;
        if (hit instanceof EntityHitResult entityHit) {
            aimed = asKaiju(entityHit.getEntity());
        }
        if (aimed == null) {
            // A mira vanilla vai so ate ~3-5 blocos; para kaiju grandes, procura ao longo do olhar ate 48.
            aimed = lookingAt(minecraft);
        }
        if (aimed != null) {
            if (aimed != target) {
                shownHealth = -1;
            }
            target = aimed;
            lastSeen = now;
        } else if (target != null && (now - lastSeen > LINGER_MS || !target.isAlive()
                || target.distanceTo(minecraft.player) > MAX_DISTANCE)) {
            target = null;
        }
    }

    private static KaijuEntity lookingAt(Minecraft minecraft) {
        Entity camera = minecraft.getCameraEntity();
        if (camera == null || minecraft.level == null) {
            return null;
        }
        Vec3 eye = camera.getEyePosition();
        Vec3 end = eye.add(camera.getViewVector(1.0F).scale(MAX_DISTANCE));
        EntityHitResult result = ProjectileUtil.getEntityHitResult(camera, eye,
                end, camera.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0),
                entity -> !entity.isSpectator() && entity.isPickable(), MAX_DISTANCE * MAX_DISTANCE);
        return result == null ? null : asKaiju(result.getEntity());
    }

    private static KaijuEntity asKaiju(Entity entity) {
        if (entity instanceof KaijuEntity kaiju) {
            return kaiju;
        }
        return entity instanceof KaijuPart part && part.getParent() instanceof KaijuEntity kaiju ? kaiju : null;
    }

    /** "Yoju · medio · COMBATE". */
    private static Component info(KaijuEntity kaiju) {
        String state = kaiju.state().name().toLowerCase(Locale.ROOT);
        return kaiju.def().map(def -> Component.translatable("kn8.hud.kaiju.info",
                Component.translatable("kn8.kaiju_class." + def.kaijuClass().getSerializedName()),
                sizeClass(def), Component.translatable("kn8.kaiju_state." + state)))
                .orElse(Component.translatable("kn8.kaiju_state." + state));
    }

    private static Component sizeClass(KaijuDef def) {
        return KaijuScale.sizeClass(def.kaijuClass().getSerializedName(), def.dimensions().width(),
                def.dimensions().height())
                .map(size -> (Component) Component.translatable(
                        "kn8.kaiju_size." + size.name().toLowerCase(Locale.ROOT)))
                .orElse(Component.empty());
    }
}
