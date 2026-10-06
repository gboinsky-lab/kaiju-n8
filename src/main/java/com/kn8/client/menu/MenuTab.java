package com.kn8.client.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Uma aba do menu da Forca de Defesa: desenha na area dada e trata os proprios cliques. */
interface MenuTab {

    void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return false;
    }

    /** Chamado a cada tick com a aba aberta (animacao dos modelos, listas de entidades proximas). */
    default void tick() {
    }
}
