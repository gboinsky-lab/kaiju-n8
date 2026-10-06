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
 * (termometro; no critico/pane, a barra pisca, OVERHEAT e vinheta vermelha nas bordas da tela).</p>
 *
 * <p>0.2: a arte e de texturas em alta resolucao ({@code textures/gui/hud/}, geradas por {@code tools/art/gen_hud.py}
 * fiel a referencia), desenhadas em pixels da textura; so os segmentos acesos e os numeros mudam por quadro.</p>
 *
 * <p>So DESENHA o que o servidor mandou ({@code kn8:power_view}, canal privado do M5): nenhuma regra roda aqui. Os
 * valores sao suavizados entre envios. O estado abaixo e so de desenho deste cliente.</p>
 */
public final class KN8Hud {

    // Arte da HUD (0.2): texturas em SCALE pixels por unidade da GUI, geradas por tools/art/gen_hud.py. Todas as
    // posicoes abaixo vem do LAYOUT daquele script (unidades da GUI): mudou la, mude aqui.
    private static final ResourceLocation FRAME = KN8Constants.id("textures/gui/hud/frame.png");
    private static final ResourceLocation FILL_RELEASE = KN8Constants.id("textures/gui/hud/fill_release.png");
    private static final ResourceLocation FILL_SURGE = KN8Constants.id("textures/gui/hud/fill_surge.png");
    private static final ResourceLocation FILL_STAMINA = KN8Constants.id("textures/gui/hud/fill_stamina.png");
    private static final ResourceLocation FILL_HEAT = KN8Constants.id("textures/gui/hud/fill_heat.png");
    private static final ResourceLocation DIGITS = KN8Constants.id("textures/gui/hud/digits.png");
    private static final int SCALE = 4;
    private static final int PANEL_WIDTH = 214;
    private static final int PANEL_HEIGHT = 97;
    private static final int MARGIN = 6;
    /** Largura da hotbar vanilla e altura da faixa de hotbar + coracoes + armadura (unidades da GUI). */
    private static final int HOTBAR_WIDTH = 182;
    private static final int STATUS_BARS_HEIGHT = 65;

    /** Barra: x, y, largura, altura (GUI). As texturas de preenchimento tem FILL_MARGIN de brilho em volta. */
    private static final int[] RELEASE_BAR = {50, 39, 146, 8};
    private static final int[] STAMINA_BAR = {71, 64, 122, 6};
    private static final int[] HEAT_BAR = {71, 85, 122, 6};
    private static final int FILL_MARGIN = 2;
    private static final int[] RELEASE_FILL_SIZE = {600, 48};
    private static final int[] ROW_FILL_SIZE = {504, 40};

    // Numeros: celula 10x13 pixels da fonte (40x52 na textura, glifo 6x9 com margem do brilho), azul e laranja.
    private static final String DIGIT_CHARS = "0123456789%/ ";
    private static final int DIGIT_CELL_WIDTH = 40;
    private static final int DIGIT_CELL_HEIGHT = 52;
    private static final int DIGITS_WIDTH = DIGIT_CELL_WIDTH * 13;
    private static final int DIGITS_HEIGHT = DIGIT_CELL_HEIGHT * 2;
    /** Avanco por caractere (7 pixels da fonte; espaco = 3). */
    private static final int DIGIT_ADVANCE = 28;
    private static final int SPACE_ADVANCE = 12;
    private static final int VALUE_X = 69;
    private static final int VALUE_Y = 13;
    private static final float VALUE_CELL_HEIGHT = 16.0F;

    private static final float LABEL_SCALE = 0.75F;
    private static final float SMOOTHING = 0.2F;
    private static final long BLINK_MS = 250;
    private static final long PULSE_MS = 900;
    private static final int VIGNETTE_WIDTH = 24;
    private static final int LINE = 10;
    private static final int CHARGE_BAR_WIDTH = 60;

    private static final int COLOR_SEGMENT_EMPTY = 0xFF26303C;
    private static final int COLOR_LABEL = 0xFFE3E8EE;
    private static final int COLOR_VALUE_DIM = 0xFFB7D9F5;
    private static final int COLOR_RELEASE_LIGHT = 0xFF6FD3FF;
    private static final int COLOR_SURGE = 0xFFFF9800;
    private static final int COLOR_CAP = 0xFFFFFFFF;
    private static final int COLOR_XP = 0xFF42A5F5;
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
        int x = origin.x();
        int y = origin.y();
        // Arte: em pixels da textura (1/SCALE da GUI), para ficar nitida em qualquer escala.
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(1.0F / SCALE, 1.0F / SCALE, 1.0F);
        graphics.blit(FRAME, 0, 0, 0, 0, PANEL_WIDTH * SCALE, PANEL_HEIGHT * SCALE, PANEL_WIDTH * SCALE,
                PANEL_HEIGHT * SCALE);
        drawRelease(graphics, view);
        drawRows(graphics, view);
        graphics.pose().popPose();
        drawReleaseText(graphics, minecraft.font, view, x, y);
        drawAlerts(graphics, minecraft.font, overheat, x, y);
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // --- RELEASE ----------------------------------------------------------------------------------------------

    /** Segmentos acesos (azul ate o treinado, laranja no Surto), marca do teto e linha de treino. */
    private static void drawRelease(GuiGraphics graphics, PowerView view) {
        int base = Math.min(view.trained(), view.cap());
        float total = HudMath.fraction(shownRelease, 100);
        float baseFraction = HudMath.fraction(Math.min(shownRelease, base), 100);
        fillBar(graphics, FILL_RELEASE, RELEASE_BAR, RELEASE_FILL_SIZE, 0, baseFraction);
        if (total > baseFraction) {
            fillBar(graphics, FILL_SURGE, RELEASE_BAR, RELEASE_FILL_SIZE, baseFraction, total);
        }
        int barX = RELEASE_BAR[0] * SCALE;
        int barY = RELEASE_BAR[1] * SCALE;
        int barWidth = RELEASE_BAR[2] * SCALE;
        int barHeight = RELEASE_BAR[3] * SCALE;
        // Marca do teto atual (ate onde o treino ja chegou a liberar).
        int capX = barX + Math.round(barWidth * HudMath.fraction(view.cap(), 100));
        graphics.fill(capX, barY - SCALE, capX + 2, barY + barHeight + SCALE, COLOR_CAP);
        // Linha fina de treino ate o proximo ponto (vazia no teto).
        float xp = view.xpToNext() > 0 ? HudMath.fraction(shownXp, view.xpToNext()) : 0;
        graphics.fill(barX, barY + barHeight + SCALE, barX + Math.round(barWidth * xp), barY + barHeight + SCALE + 2,
                COLOR_XP);
    }

    /** "efetiva% / teto%" com os numeros em neon (laranja no Surto) e "MAX" piscando no teto. */
    private static void drawReleaseText(GuiGraphics graphics, Font font, PowerView view, int x, int y) {
        String value = Math.round(shownRelease) + "% / " + view.cap() + "%";
        int row = view.surge() > 0 ? 1 : 0;
        float cellScale = VALUE_CELL_HEIGHT / DIGIT_CELL_HEIGHT;
        graphics.pose().pushPose();
        graphics.pose().translate(x + VALUE_X, y + VALUE_Y, 0);
        graphics.pose().scale(cellScale, cellScale, 1.0F);
        int cursor = 0;
        for (char c : value.toCharArray()) {
            int index = DIGIT_CHARS.indexOf(c);
            if (index < 0) {
                continue;
            }
            graphics.blit(DIGITS, cursor, 0, index * DIGIT_CELL_WIDTH, row * DIGIT_CELL_HEIGHT, DIGIT_CELL_WIDTH,
                    DIGIT_CELL_HEIGHT, DIGITS_WIDTH, DIGITS_HEIGHT);
            cursor += c == ' ' ? SPACE_ADVANCE : DIGIT_ADVANCE;
        }
        graphics.pose().popPose();
        boolean atCap = view.trained() >= view.cap() && view.surge() == 0;
        if (atCap) {
            int afterValue = x + VALUE_X + Math.round(cursor * cellScale) + 4;
            scaled(graphics, font, Component.translatable("kn8.hud.max"), afterValue, y + VALUE_Y + 6,
                    blink(COLOR_VALUE_DIM), LABEL_SCALE);
        }
    }

    // --- STAMINA e HEAT ----------------------------------------------------------------------------------------

    private static void drawRows(GuiGraphics graphics, PowerView view) {
        fillBar(graphics, FILL_STAMINA, STAMINA_BAR, ROW_FILL_SIZE, 0,
                HudMath.fraction(shownStamina, view.maxStamina()));
        boolean critical = view.heatStage() >= HeatStage.CRITICAL.ordinal();
        if (critical && blinkOn()) {
            // Critico/pane: o calor pisca (a vinheta vermelha e o OVERHEAT completam o aviso).
            RenderSystem.setShaderColor(1.0F, 0.55F, 0.55F, 1.0F);
        }
        fillBar(graphics, FILL_HEAT, HEAT_BAR, ROW_FILL_SIZE, 0, HudMath.fraction(shownHeat, view.heatMax()));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Desenha a faixa de segmentos acesos de {@code from} ate {@code to} (fracoes da barra), recortando a textura
     * de preenchimento na vertical. A textura tem FILL_MARGIN de brilho em volta, que entra junto nas pontas.
     */
    private static void fillBar(GuiGraphics graphics, ResourceLocation texture, int[] bar, int[] size, float from,
            float to) {
        if (to <= from) {
            return;
        }
        int margin = FILL_MARGIN * SCALE;
        int width = bar[2] * SCALE;
        int start = from <= 0 ? 0 : margin + Math.round(width * from);
        int end = margin + Math.round(width * Math.min(1.0F, to)) + (to >= 1.0F ? margin : 0);
        int originX = (bar[0] - FILL_MARGIN) * SCALE;
        int originY = (bar[1] - FILL_MARGIN) * SCALE;
        graphics.blit(texture, originX + start, originY, start, 0, end - start, size[1], size[0], size[1]);
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

    private static boolean blinkOn() {
        return (System.currentTimeMillis() / BLINK_MS) % 2 == 0;
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
