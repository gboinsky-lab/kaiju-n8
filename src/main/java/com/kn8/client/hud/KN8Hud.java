// src/main/java/com/kn8/client/hud/KN8Hud.java
package com.kn8.client.hud;

import com.kn8.KN8Constants;
import com.kn8.client.combat.CombatFeedback;
import com.kn8.client.combat.CombatInput;
import com.kn8.common.attribute.PowerView;
import com.kn8.common.config.ClientConfig;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.core.power.HeatStage;
import com.kn8.core.ui.HudMath;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD de combate do traje (M6; visual da Etapa B a partir da referencia "HUD de combate avancado - estilo anime").
 *
 * <p>Bloco de RELEASE (emblema em moldura chanfrada, valor grande "efetiva% / teto%", barra de 10 segmentos inclinados
 * em degrade azul, marca do teto, Surto em laranja e "MAX" no teto) e duas faixas menores, STAMINA (corredor) e HEAT
 * (termometro, cor por estagio; no critico/pane, OVERHEAT piscando e vinheta vermelha nas bordas da tela).</p>
 *
 * <p>So DESENHA o que o servidor mandou ({@code kn8:power_view}, canal privado do M5): nenhuma regra roda aqui. Os
 * valores sao suavizados entre envios. Icones em {@code textures/gui/hud_icons.png} (substituiveis por arte propria,
 * mantendo as posicoes). O estado abaixo e so de desenho deste cliente.</p>
 */
public final class KN8Hud {

    private static final ResourceLocation ICONS = KN8Constants.id("textures/gui/hud_icons.png");
    private static final int ICONS_WIDTH = 64;
    private static final int ICONS_HEIGHT = 32;
    private static final int EMBLEM_SIZE = 32;
    private static final int SMALL_ICON = 12;
    private static final int RUNNER_U = 32;
    private static final int THERMOMETER_U = 44;

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 78;
    private static final int MARGIN = 6;
    /** Largura da hotbar vanilla e altura da faixa de hotbar + coracoes + armadura (unidades da GUI). */
    private static final int HOTBAR_WIDTH = 182;
    private static final int STATUS_BARS_HEIGHT = 65;

    // Bloco de RELEASE.
    private static final int RELEASE_HEIGHT = 46;
    private static final int EMBLEM_BOX = 42;
    private static final int RELEASE_TEXT_X = 48;
    private static final int RELEASE_BAR_Y = 31;
    private static final int RELEASE_BAR_WIDTH = 140;
    private static final int RELEASE_BAR_HEIGHT = 8;
    // Faixas de STAMINA e HEAT.
    private static final int ROW_HEIGHT = 14;
    private static final int ROW_GAP = 2;
    private static final int ROW_LABEL_X = 16;
    private static final int ROW_BAR_X = 62;
    private static final int ROW_BAR_WIDTH = 128;
    private static final int ROW_BAR_HEIGHT = 6;

    private static final int SEGMENTS = 10;
    private static final int SEGMENT_GAP = 2;
    private static final int CUT = 6;
    private static final float LABEL_SCALE = 0.75F;
    private static final float VALUE_SCALE = 1.5F;
    private static final float SMOOTHING = 0.2F;
    private static final long BLINK_MS = 250;
    private static final long PULSE_MS = 900;
    private static final int VIGNETTE_WIDTH = 24;
    private static final int LINE = 10;
    private static final int CHARGE_BAR_WIDTH = 60;

    private static final int COLOR_PANEL = 0xB00C1320;
    private static final int COLOR_OUTLINE = 0xC0B8C4D0;
    private static final int COLOR_OUTLINE_DIM = 0x60B8C4D0;
    private static final int COLOR_SEGMENT_EMPTY = 0xFF26303C;
    private static final int COLOR_LABEL = 0xFFE3E8EE;
    private static final int COLOR_VALUE = 0xFF7FC8FF;
    private static final int COLOR_VALUE_DIM = 0xFFB7D9F5;
    private static final int COLOR_RELEASE_LIGHT = 0xFF6FD3FF;
    private static final int COLOR_RELEASE_DARK = 0xFF1E6FD9;
    private static final int COLOR_SURGE = 0xFFFF9800;
    private static final int COLOR_CAP = 0xFFFFFFFF;
    private static final int COLOR_XP = 0xFF42A5F5;
    private static final int COLOR_STAMINA_LIGHT = 0xFF8EE58F;
    private static final int COLOR_STAMINA_DARK = 0xFF2E9E46;
    private static final int[] HEAT_LIGHT = {0xFFB0BEC5, 0xFFFFE082, 0xFFFFB74D, 0xFFFF7043, 0xFFFF5252};
    private static final int[] HEAT_DARK = {0xFF607D8B, 0xFFF9A825, 0xFFEF6C00, 0xFFD32F2F, 0xFFB71C1C};
    private static final int COLOR_ALERT = 0xFFFF1744;
    private static final int COLOR_ALERT_ALT = 0xFFFFFFFF;
    private static final int VIGNETTE_RED = 0xFF1744;

    private static float shownRelease;
    private static float shownStamina;
    private static float shownHeat;
    private static float shownXp;

    private KN8Hud() {
    }

    /** Mod bus: fica logo abaixo do chat (o chat continua por cima quando aberto). */
    public static void register(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, KN8Constants.id("power_hud"), KN8Hud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        PowerView view = player.getData(KN8Attachments.POWER_VIEW);
        if (view.maxStamina() <= 0) {
            // Ainda nao chegou nada do servidor.
            return;
        }
        shownRelease = HudMath.approach(shownRelease, view.effective(), SMOOTHING);
        shownStamina = HudMath.approach(shownStamina, view.stamina(), SMOOTHING);
        shownHeat = HudMath.approach(shownHeat, view.heat(), SMOOTHING);
        shownXp = HudMath.approach(shownXp, view.releaseXp(), SMOOTHING);

        boolean overheat = view.heatStage() >= HeatStage.CRITICAL.ordinal();
        if (overheat) {
            drawVignette(graphics, view.panic());
        }
        double scale = ClientConfig.HUD_SCALE.get();
        HudMath.Origin origin = HudMath.origin(corner(ClientConfig.HUD_ANCHOR.get()),
                graphics.guiWidth(), graphics.guiHeight(), PANEL_WIDTH, PANEL_HEIGHT, scale, MARGIN, HOTBAR_WIDTH,
                STATUS_BARS_HEIGHT);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        Font font = minecraft.font;
        int x = origin.x();
        int y = origin.y();
        drawRelease(graphics, font, view, x, y);
        int rowY = y + RELEASE_HEIGHT + ROW_GAP;
        drawStamina(graphics, font, view, x, rowY);
        drawHeat(graphics, font, view, x, rowY + ROW_HEIGHT + ROW_GAP);
        drawAlerts(graphics, font, overheat, x, y);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    // --- RELEASE ----------------------------------------------------------------------------------------------

    private static void drawRelease(GuiGraphics graphics, Font font, PowerView view, int x, int y) {
        bevelPanel(graphics, x, y, PANEL_WIDTH, RELEASE_HEIGHT);
        // Emblema em moldura chanfrada.
        int boxX = x + 2;
        int boxY = y + 2;
        bevelOutline(graphics, boxX, boxY, EMBLEM_BOX, EMBLEM_BOX, COLOR_OUTLINE);
        int iconOffset = (EMBLEM_BOX - EMBLEM_SIZE) / 2;
        graphics.blit(ICONS, boxX + iconOffset, boxY + iconOffset, 0, 0, EMBLEM_SIZE, EMBLEM_SIZE, ICONS_WIDTH,
                ICONS_HEIGHT);
        // Linha decorativa no alto, a direita.
        graphics.fill(x + PANEL_WIDTH - 46, y + 3, x + PANEL_WIDTH - 8, y + 4, COLOR_OUTLINE_DIM);

        int textX = x + RELEASE_TEXT_X;
        scaled(graphics, font, Component.translatable("kn8.hud.label.release"), textX, y + 4, COLOR_LABEL,
                LABEL_SCALE);
        boolean atCap = view.trained() >= view.cap() && view.surge() == 0;
        int valueColor = view.surge() > 0 ? COLOR_SURGE : COLOR_VALUE;
        Component value = Component.translatable("kn8.hud.release.value", Math.round(shownRelease), view.cap());
        scaled(graphics, font, value, textX, y + 13, valueColor, VALUE_SCALE);
        if (atCap) {
            int afterValue = textX + Math.round(font.width(value) * VALUE_SCALE) + 4;
            scaled(graphics, font, Component.translatable("kn8.hud.max"), afterValue, y + 17,
                    blink(COLOR_VALUE_DIM), LABEL_SCALE);
        }

        int barX = textX;
        int barY = y + RELEASE_BAR_Y;
        int base = Math.min(view.trained(), view.cap());
        float total = HudMath.fraction(shownRelease, 100);
        float baseFraction = HudMath.fraction(Math.min(shownRelease, base), 100);
        segmentBar(graphics, barX, barY, RELEASE_BAR_WIDTH, RELEASE_BAR_HEIGHT, total, baseFraction,
                COLOR_RELEASE_LIGHT, COLOR_RELEASE_DARK, COLOR_SURGE);
        // Marca do teto da patente.
        int capX = barX + Math.round(RELEASE_BAR_WIDTH * HudMath.fraction(view.cap(), 100));
        graphics.fill(capX, barY - 2, capX + 1, barY + RELEASE_BAR_HEIGHT + 1, COLOR_CAP);
        // Linha fina de treino ate o proximo ponto (vazia no teto).
        float xp = view.xpToNext() > 0 ? HudMath.fraction(shownXp, view.xpToNext()) : 0;
        graphics.fill(barX, barY + RELEASE_BAR_HEIGHT + 2, barX + Math.round(RELEASE_BAR_WIDTH * xp),
                barY + RELEASE_BAR_HEIGHT + 3, COLOR_XP);
    }

    // --- STAMINA e HEAT ----------------------------------------------------------------------------------------

    private static void drawStamina(GuiGraphics graphics, Font font, PowerView view, int x, int y) {
        rowPanel(graphics, x, y);
        graphics.blit(ICONS, x + 2, y + 1, RUNNER_U, 0, SMALL_ICON, SMALL_ICON, ICONS_WIDTH, ICONS_HEIGHT);
        scaled(graphics, font, Component.translatable("kn8.hud.label.stamina"), x + ROW_LABEL_X, y + 4,
                COLOR_LABEL, LABEL_SCALE);
        float fraction = HudMath.fraction(shownStamina, view.maxStamina());
        segmentBar(graphics, x + ROW_BAR_X, y + 4, ROW_BAR_WIDTH, ROW_BAR_HEIGHT, fraction, fraction,
                COLOR_STAMINA_LIGHT, COLOR_STAMINA_DARK, COLOR_STAMINA_LIGHT);
    }

    private static void drawHeat(GuiGraphics graphics, Font font, PowerView view, int x, int y) {
        rowPanel(graphics, x, y);
        graphics.blit(ICONS, x + 2, y + 1, THERMOMETER_U, 0, SMALL_ICON, SMALL_ICON, ICONS_WIDTH, ICONS_HEIGHT);
        int stage = Math.max(0, Math.min(HEAT_LIGHT.length - 1, view.heatStage()));
        boolean critical = stage >= HeatStage.CRITICAL.ordinal();
        scaled(graphics, font, Component.translatable("kn8.hud.label.heat"), x + ROW_LABEL_X, y + 4,
                critical ? blink(HEAT_LIGHT[stage]) : COLOR_LABEL, LABEL_SCALE);
        float fraction = HudMath.fraction(shownHeat, view.heatMax());
        int light = critical ? blink(HEAT_LIGHT[stage]) : HEAT_LIGHT[stage];
        segmentBar(graphics, x + ROW_BAR_X, y + 4, ROW_BAR_WIDTH, ROW_BAR_HEIGHT, fraction, fraction, light,
                HEAT_DARK[stage], light);
    }

    /** OVERHEAT (critico ou pane), combo e avisos de combate, acima do painel (ou abaixo, se estiver no topo). */
    private static void drawAlerts(GuiGraphics graphics, Font font, boolean overheat, int x, int y) {
        boolean above = y > LINE * 3;
        int lineY = above ? y - LINE - 1 : y + PANEL_HEIGHT + 2;
        int step = above ? -LINE : LINE;
        if (overheat) {
            graphics.drawString(font, Component.translatable("kn8.hud.overheat"), x + 2, lineY, blink(COLOR_ALERT),
                    true);
            lineY += step;
        }
        float charge = CombatInput.chargeFraction();
        if (charge >= 0) {
            // 0.1-B: barra de carga do ataque carregado (cheia = critico).
            boolean full = charge >= 1.0F;
            Component label = Component.translatable(full ? "kn8.hud.charge.full" : "kn8.hud.charge");
            graphics.drawString(font, label, x + 2, lineY, full ? blink(COLOR_SURGE) : COLOR_LABEL, true);
            int barX = x + 4 + font.width(label) + 4;
            graphics.fill(barX, lineY + 2, barX + CHARGE_BAR_WIDTH, lineY + 6, COLOR_SEGMENT_EMPTY);
            graphics.fill(barX, lineY + 2, barX + Math.round(CHARGE_BAR_WIDTH * charge), lineY + 6,
                    full ? COLOR_SURGE : COLOR_RELEASE_LIGHT);
            lineY += step;
        }
        Component combo = CombatFeedback.comboLine();
        if (combo != null) {
            graphics.drawString(font, combo, x + 2, lineY, COLOR_LABEL, true);
            lineY += step;
        }
        Component message = CombatFeedback.messageLine();
        if (message != null) {
            graphics.drawString(font, message, x + 2, lineY, CombatFeedback.messageColor(), true);
        }
    }

    // --- pecas de desenho ------------------------------------------------------------------------------------

    /** Painel com cantos cortados (superior esquerdo e inferior direito) e contorno claro. */
    private static void bevelPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int row = 0; row < height; row++) {
            int left = Math.max(0, CUT - row);
            int right = Math.max(0, row - (height - 1 - CUT));
            graphics.fill(x + left, y + row, x + width - right, y + row + 1, COLOR_PANEL);
            // Contorno: so as pontas de cada linha (e as linhas de cima e de baixo inteiras).
            if (row == 0 || row == height - 1) {
                graphics.fill(x + left, y + row, x + width - right, y + row + 1, COLOR_OUTLINE);
            } else {
                graphics.fill(x + left, y + row, x + left + 1, y + row + 1, COLOR_OUTLINE);
                graphics.fill(x + width - right - 1, y + row, x + width - right, y + row + 1, COLOR_OUTLINE);
            }
        }
    }

    /** So o contorno chanfrado (moldura do emblema). */
    private static void bevelOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        for (int row = 0; row < height; row++) {
            int left = Math.max(0, CUT - row);
            int right = Math.max(0, row - (height - 1 - CUT));
            if (row == 0 || row == height - 1) {
                graphics.fill(x + left, y + row, x + width - right, y + row + 1, color);
            } else {
                graphics.fill(x + left, y + row, x + left + 1, y + row + 1, color);
                graphics.fill(x + width - right - 1, y + row, x + width - right, y + row + 1, color);
            }
        }
    }

    /** Faixa inclinada (paralelogramo) das linhas de STAMINA e HEAT. */
    private static void rowPanel(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < ROW_HEIGHT; row++) {
            int slant = (ROW_HEIGHT - 1 - row) / 2;
            int right = x + PANEL_WIDTH - 4 + slant;
            graphics.fill(x, y + row, right, y + row + 1, COLOR_PANEL);
            graphics.fill(right - 1, y + row, right, y + row + 1, COLOR_OUTLINE);
        }
        graphics.fill(x, y + ROW_HEIGHT - 1, x + PANEL_WIDTH - 4, y + ROW_HEIGHT, COLOR_OUTLINE_DIM);
    }

    /**
     * Barra de 10 segmentos inclinados. Ate {@code baseFraction} usa o degrade claro->escuro; de {@code baseFraction}
     * ate {@code fraction} usa {@code extraColor} (Surto). O segmento parcial e preenchido pela metade, quando cabe.
     */
    private static void segmentBar(GuiGraphics graphics, int x, int y, int width, int height, float fraction,
            float baseFraction, int light, int dark, int extraColor) {
        int segmentWidth = (width - SEGMENT_GAP * (SEGMENTS - 1)) / SEGMENTS;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + i * (segmentWidth + SEGMENT_GAP);
            float start = i / (float) SEGMENTS;
            float end = (i + 1) / (float) SEGMENTS;
            int color;
            int fillWidth = segmentWidth;
            if (fraction >= end) {
                color = start >= baseFraction ? extraColor : lerpColor(light, dark, i / (float) (SEGMENTS - 1));
            } else if (fraction > start) {
                color = start >= baseFraction ? extraColor : lerpColor(light, dark, i / (float) (SEGMENTS - 1));
                fillWidth = Math.max(1, Math.round(segmentWidth * (fraction - start) * SEGMENTS));
            } else {
                color = COLOR_SEGMENT_EMPTY;
            }
            slantedBox(graphics, segmentX, y, segmentWidth, height, COLOR_SEGMENT_EMPTY);
            if (color != COLOR_SEGMENT_EMPTY) {
                slantedBox(graphics, segmentX, y, fillWidth, height, color);
            }
        }
    }

    /** Retangulo inclinado: cada linha deslocada meio pixel para a direita quanto mais alta. */
    private static void slantedBox(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        for (int row = 0; row < height; row++) {
            int shift = (height - 1 - row) / 2;
            graphics.fill(x + shift, y + row, x + shift + width, y + row + 1, color);
        }
    }

    private static void scaled(GuiGraphics graphics, Font font, Component text, int x, int y, int color, float size) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(size, size, 1.0F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    /** Vinheta vermelha pulsando nas bordas da tela (critico mais fraca, pane mais forte). */
    private static void drawVignette(GuiGraphics graphics, boolean panic) {
        double phase = (System.currentTimeMillis() % PULSE_MS) / (double) PULSE_MS;
        double pulse = 0.5 + 0.5 * Math.sin(phase * Math.PI * 2);
        int maxAlpha = panic ? 0x70 : 0x38;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int i = 0; i < VIGNETTE_WIDTH; i++) {
            int alpha = (int) (maxAlpha * pulse * (1.0 - i / (double) VIGNETTE_WIDTH));
            int color = (alpha << 24) | VIGNETTE_RED;
            graphics.fill(i, 0, i + 1, height, color);
            graphics.fill(width - i - 1, 0, width - i, height, color);
            graphics.fill(0, i, width, i + 1, color);
            graphics.fill(0, height - i - 1, width, height - i, color);
        }
    }

    private static int lerpColor(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blink(int color) {
        return (System.currentTimeMillis() / BLINK_MS) % 2 == 0 ? color : COLOR_ALERT_ALT;
    }

    private static HudMath.Corner corner(ClientConfig.HudAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> HudMath.Corner.TOP_LEFT;
            case TOP_RIGHT -> HudMath.Corner.TOP_RIGHT;
            case BOTTOM_LEFT -> HudMath.Corner.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> HudMath.Corner.BOTTOM_RIGHT;
        };
    }
}
