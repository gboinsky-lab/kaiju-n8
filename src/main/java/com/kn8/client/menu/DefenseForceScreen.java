package com.kn8.client.menu;

import java.util.EnumMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/**
 * Menu da Forca de Defesa (0.2), a partir do concept do Miguel: barra de abas no topo (Perfil, Missoes, Alertas,
 * Esquadrao, Bestiario, Arsenal) e uma aba por vez em tela cheia. Abre e fecha com a tecla do menu (M); Q/E trocam de
 * aba (o LB/RB do concept).
 *
 * <p>So DESENHA: o que o cliente ja tem (PowerView, dados sincronizados, estatisticas vanilla, entidades por perto).
 * O que depende de sistemas das proximas etapas (patente/merito, missoes aceitas, invasao, ordens, fabricacao)
 * aparece com o layout pronto e o aviso "em breve".</p>
 */
public class DefenseForceScreen extends Screen {

    enum Tab {
        PROFILE, MISSIONS, ALERTS, SQUAD, BESTIARY, ARSENAL
    }

    private static final int BAR_HEIGHT = 22;
    private static final int MARGIN = 6;
    private static final int TAB_SIDE_WIDTH = 92;
    private static Tab current = Tab.PROFILE;

    private final Map<Tab, MenuTab> tabs = new EnumMap<>(Tab.class);
    private int tabsX;
    private int tabWidth;

    public DefenseForceScreen() {
        super(Component.translatable("kn8.menu.title"));
        tabs.put(Tab.PROFILE, new ProfileTab());
        tabs.put(Tab.MISSIONS, new MissionsTab());
        tabs.put(Tab.ALERTS, new AlertsTab());
        tabs.put(Tab.SQUAD, new SquadTab());
        tabs.put(Tab.BESTIARY, new BestiaryTab());
        tabs.put(Tab.ARSENAL, new ArsenalTab());
    }

    @Override
    protected void init() {
        super.init();
        // Estatisticas vanilla (kaiju abatidos por especie): o servidor responde atualizando player.getStats().
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Fundo proprio (sem o desfoque vanilla): azul escuro com uma grade bem fraca.
        g.fill(0, 0, width, height, MenuStyle.BACKGROUND);
        for (int x = 0; x < width; x += 24) {
            g.fill(x, 0, x + 1, height, 0x0A35C8FF);
        }
        for (int y = 0; y < height; y += 24) {
            g.fill(0, y, width, y + 1, 0x0A35C8FF);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Font font = this.font;
        drawTopBar(g, font, mouseX, mouseY);
        int x = MARGIN;
        int y = BAR_HEIGHT + 4;
        tabs.get(current).render(g, font, x, y, width - 2 * MARGIN, height - y - MARGIN, mouseX, mouseY,
                partialTick);
        MenuStyle.resetColor();
        MenuStyle.flushTooltip(g, font, mouseX, mouseY);
    }

    private void drawTopBar(GuiGraphics g, Font font, int mouseX, int mouseY) {
        g.fill(0, 0, width, BAR_HEIGHT, 0xF00A1220);
        g.fill(0, BAR_HEIGHT - 1, width, BAR_HEIGHT, MenuStyle.OUTLINE);
        // Logo a esquerda.
        MenuStyle.scaled(g, font, Component.literal("KAIJU"), 6, 5, MenuStyle.TEXT, 1.25F);
        MenuStyle.scaled(g, font, Component.literal("No. 8"), 46, 5, MenuStyle.TEXT_ACCENT, 1.25F);
        boolean brand = width >= 470;
        int right = brand ? width - TAB_SIDE_WIDTH : width - 6;
        if (brand) {
            MenuStyle.small(g, font, Component.translatable("kn8.menu.brand"), right + 6, 4, MenuStyle.TEXT);
            MenuStyle.scaled(g, font, Component.translatable("kn8.menu.brand_sub"), right + 6, 12,
                    MenuStyle.TEXT_DIM, 0.5F);
        }
        // Abas entre o logo e a marca, com [Q] e [E] nas pontas.
        int left = 88;
        MenuStyle.small(g, font, Component.literal("[Q]"), left, 8, MenuStyle.TEXT_DIM);
        MenuStyle.small(g, font, Component.literal("[E]"), right - 14, 8, MenuStyle.TEXT_DIM);
        tabsX = left + 16;
        tabWidth = (right - 18 - tabsX) / Tab.values().length;
        for (Tab tab : Tab.values()) {
            int tx = tabsX + tab.ordinal() * tabWidth;
            boolean selected = tab == current;
            boolean hovered = MenuStyle.inside(mouseX, mouseY, tx, 2, tabWidth, BAR_HEIGHT - 4);
            if (selected) {
                g.fillGradient(tx, 2, tx + tabWidth - 1, BAR_HEIGHT - 1, 0x0035C8FF, 0x6035C8FF);
                g.fill(tx, BAR_HEIGHT - 3, tx + tabWidth - 1, BAR_HEIGHT - 1, MenuStyle.ACCENT);
            } else if (hovered) {
                g.fill(tx, 2, tx + tabWidth - 1, BAR_HEIGHT - 1, 0x20FFFFFF);
            }
            g.fill(tx + tabWidth - 1, 6, tx + tabWidth, BAR_HEIGHT - 6, MenuStyle.OUTLINE);
            Component label = Component.translatable("kn8.menu.tab." + tab.name().toLowerCase(java.util.Locale.ROOT));
            int labelWidth = Math.round(font.width(label) * MenuStyle.SMALL);
            MenuStyle.small(g, font, label, tx + (tabWidth - labelWidth) / 2, 8,
                    selected ? MenuStyle.TEXT : MenuStyle.TEXT_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY < BAR_HEIGHT && mouseX >= tabsX && tabWidth > 0) {
            int index = (int) ((mouseX - tabsX) / tabWidth);
            if (index >= 0 && index < Tab.values().length) {
                current = Tab.values()[index];
                return true;
            }
        }
        return tabs.get(current).mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return tabs.get(current).mouseScrolled(mouseX, mouseY, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_Q || keyCode == GLFW.GLFW_KEY_E) {
            int step = keyCode == GLFW.GLFW_KEY_Q ? -1 : 1;
            int count = Tab.values().length;
            current = Tab.values()[(current.ordinal() + step + count) % count];
            return true;
        }
        if (MenuInput.MENU_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        tabs.get(current).tick();
    }
}
