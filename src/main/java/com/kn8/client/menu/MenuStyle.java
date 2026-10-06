package com.kn8.client.menu;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Pecas de desenho do menu da Forca de Defesa (0.2), no estilo da HUD e do concept do Miguel: paineis escuros com
 * cantos cortados, contorno prateado, destaque ciano, titulos com barrinha ciano e barras com degrade. Tudo com
 * {@code fill}, entao a tela fica nitida em qualquer escala de GUI.
 */
public final class MenuStyle {

    public static final int BACKGROUND = 0xF0070C16;
    public static final int PANEL = 0xE00E1626;
    public static final int PANEL_LIGHT = 0xE0152035;
    public static final int CARD = 0xE0111B2D;
    public static final int CARD_SELECTED = 0xE0142844;
    public static final int OUTLINE = 0xFF3A4A62;
    public static final int OUTLINE_LIGHT = 0xFF8FA2BC;
    public static final int ACCENT = 0xFF35C8FF;
    public static final int ACCENT_DARK = 0xFF1A6FB0;
    public static final int TEXT = 0xFFE6EDF5;
    public static final int TEXT_DIM = 0xFF8D9BB0;
    public static final int TEXT_ACCENT = 0xFF7FD4FF;
    public static final int GREEN = 0xFF4CD964;
    public static final int YELLOW = 0xFFFFC233;
    public static final int ORANGE = 0xFFFF8A2A;
    public static final int RED = 0xFFFF3B3B;
    public static final int RED_DARK = 0xC0400A12;
    public static final int BAR_EMPTY = 0xFF202C3E;

    /** Tooltip pedido por uma aba; desenhado pela tela no fim do quadro (fora de recortes). */
    private static Component pendingTooltip;

    /** Tamanho do corte dos cantos. */
    private static final int CUT = 5;

    private MenuStyle() {
    }

    /** Painel com cantos superior esquerdo e inferior direito cortados, contorno de 1 px. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int outline) {
        int cut = Math.min(CUT, Math.min(w, h) / 3);
        for (int row = 0; row < h; row++) {
            int left = Math.max(0, cut - row);
            int right = Math.max(0, row - (h - 1 - cut));
            g.fill(x + left, y + row, x + w - right, y + row + 1, fill);
            if (row == 0 || row == h - 1) {
                g.fill(x + left, y + row, x + w - right, y + row + 1, outline);
            } else {
                g.fill(x + left, y + row, x + left + 1, y + row + 1, outline);
                g.fill(x + w - right - 1, y + row, x + w - right, y + row + 1, outline);
            }
        }
    }

    /** Cartao simples (retangulo com contorno). */
    public static void card(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        g.fill(x, y, x + w, y + h, selected ? CARD_SELECTED : CARD);
        int color = selected ? ACCENT : OUTLINE;
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Titulo de secao: barrinha ciano a esquerda e texto em caixa alta. */
    public static void title(GuiGraphics g, Font font, Component text, int x, int y) {
        g.fill(x, y, x + 2, y + 9, ACCENT);
        g.fill(x + 3, y + 2, x + 4, y + 7, ACCENT_DARK);
        g.drawString(font, text, x + 7, y + 1, TEXT, false);
    }

    /** Barra horizontal com degrade de {@code from} a {@code to} e trilho escuro. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int from, int to) {
        g.fill(x, y, x + w, y + h, BAR_EMPTY);
        int filled = Math.round(w * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + h, from, to);
            g.fill(x, y, x + filled, y + 1, lighten(from));
        }
    }

    /** Selo colorido com texto (ex.: categoria da missao, nivel de ameaca). */
    public static int badge(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        int w = Math.round(font.width(text) * SMALL) + 6;
        g.fill(x, y, x + w, y + 8, (color & 0x00FFFFFF) | 0x50000000);
        g.fill(x, y, x + 1, y + 8, color);
        small(g, font, text, x + 3, y + 1, color);
        return w;
    }

    /** Botao desenhado (o clique e tratado pela aba). Desativado = cinza. */
    public static void button(GuiGraphics g, Font font, Component text, int x, int y, int w, int h, boolean enabled,
            boolean hovered, int color) {
        int base = enabled ? color : OUTLINE;
        g.fill(x, y, x + w, y + h, (base & 0x00FFFFFF) | (hovered && enabled ? 0x70000000 : 0x38000000));
        g.fill(x, y, x + w, y + 1, base);
        g.fill(x, y + h - 1, x + w, y + h, base);
        g.fill(x, y, x + 1, y + h, base);
        g.fill(x + w - 1, y, x + w, y + h, base);
        int textWidth = font.width(text);
        g.drawString(font, text, x + (w - textWidth) / 2, y + (h - 8) / 2, enabled ? TEXT : TEXT_DIM, false);
    }

    public static final float SMALL = 0.75F;

    /** Texto em 75% (detalhes, rotulos). */
    public static void small(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        scaled(g, font, text, x, y, color, SMALL);
    }

    public static void scaled(GuiGraphics g, Font font, Component text, float x, float y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Texto pequeno quebrado em linhas ate a largura dada; devolve a altura usada. */
    public static int wrapped(GuiGraphics g, Font font, Component text, int x, int y, int width, int color,
            int maxLines) {
        int line = 0;
        for (FormattedCharSequence part : font.split(text, Math.round(width / SMALL))) {
            if (line >= maxLines) {
                break;
            }
            g.pose().pushPose();
            g.pose().translate(x, y + line * 7, 0);
            g.pose().scale(SMALL, SMALL, 1.0F);
            g.drawString(font, part, 0, 0, color, false);
            g.pose().popPose();
            line++;
        }
        return line * 7;
    }

    /** Item desenhado em qualquer tamanho (icone das armas e materiais). */
    public static void item(GuiGraphics g, ItemStack stack, float x, float y, float size) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    /** Aviso de recurso que chega numa proxima etapa (texto central, apagado). */
    public static void comingSoon(GuiGraphics g, Font font, Component text, int x, int y, int w) {
        int textWidth = Math.round(font.width(text) * SMALL);
        small(g, font, text, x + (w - textWidth) / 2, y, TEXT_DIM);
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    public static void tooltip(Component text) {
        pendingTooltip = text;
    }

    /** Desenha e limpa o tooltip pendente (chamado pela tela depois da aba). */
    public static void flushTooltip(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (pendingTooltip != null) {
            g.renderTooltip(font, pendingTooltip, mouseX, mouseY);
            pendingTooltip = null;
        }
    }

    public static void resetColor() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int lighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 60);
        int gr = Math.min(255, ((color >> 8) & 0xFF) + 60);
        int b = Math.min(255, (color & 0xFF) + 60);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
}
