package com.kn8.client.menu;

import java.util.List;

import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Aba ESQUADRAO: soldados da Forca de Defesa perto do jogador (arma, nivel de forca, vida) e o soldado escolhido em
 * destaque. O esquadrao de verdade (soldados que seguem o jogador e as ordens) vem com as patentes (Etapa 2): os
 * botoes de ordem ja estao no lugar, desativados.
 */
final class SquadTab implements MenuTab {

    private static final double RADIUS = 48.0;
    private static final int ROW_HEIGHT = 30;
    private static final String[] ORDERS = {"follow", "stay", "attack", "retreat"};

    private List<SoldierEntity> soldiers = List.of();
    private int selected;
    private int scroll;
    private int listX;
    private int listY;
    private int listW;
    private int visible;

    @Override
    public void tick() {
        soldiers = MenuData.nearbySoldiers(RADIUS);
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        if (soldiers.isEmpty()) {
            soldiers = MenuData.nearbySoldiers(RADIUS);
        }
        int listPanelW = w * 11 / 20;
        MenuStyle.panel(g, x, y, listPanelW, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.tab.squad"), x + 6, y + 6);
        Component count = Component.literal(soldiers.size() + "");
        MenuStyle.small(g, font, Component.translatable("kn8.menu.squad.nearby", (int) RADIUS, count), x + 70, y + 7,
                MenuStyle.TEXT_DIM);

        // Ordens (embaixo).
        int ordersY = y + h - 22;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.squad.orders"), x + 8, ordersY - 9,
                MenuStyle.TEXT_ACCENT);
        int bw = (listPanelW - 16) / ORDERS.length;
        for (int i = 0; i < ORDERS.length; i++) {
            int bx = x + 8 + i * bw;
            boolean hovered = MenuStyle.inside(mouseX, mouseY, bx, ordersY, bw - 3, 15);
            MenuStyle.button(g, font, Component.translatable("kn8.menu.squad.order." + ORDERS[i]), bx, ordersY,
                    bw - 3, 15, false, hovered, MenuStyle.ACCENT);
            if (hovered) {
                MenuStyle.tooltip(Component.translatable("kn8.menu.soon_stage", 2));
            }
        }

        listX = x + 6;
        listY = y + 20;
        listW = listPanelW - 12;
        int listH = ordersY - 12 - listY;
        visible = Math.max(1, listH / (ROW_HEIGHT + 2));
        if (soldiers.isEmpty()) {
            MenuStyle.comingSoon(g, font, Component.translatable("kn8.menu.squad.none"), x, listY + listH / 2 - 4,
                    listPanelW);
        } else {
            scroll = Math.max(0, Math.min(scroll, soldiers.size() - visible));
            selected = Math.min(selected, soldiers.size() - 1);
            for (int i = 0; i < visible && i + scroll < soldiers.size(); i++) {
                drawRow(g, font, soldiers.get(i + scroll), listX, listY + i * (ROW_HEIGHT + 2), listW,
                        i + scroll == selected);
            }
        }

        // Destaque: o soldado escolhido em 3D.
        int px = x + listPanelW + 4;
        int pw = w - listPanelW - 4;
        MenuStyle.panel(g, px, y, pw, h, MenuStyle.PANEL_LIGHT, MenuStyle.OUTLINE);
        if (!soldiers.isEmpty()) {
            SoldierEntity soldier = soldiers.get(selected);
            MenuData.renderEntity(g, soldier, px + 4, y + 4, px + pw - 4, y + h - 24, 0.5F, 0.0F, false);
            Component name = soldier.getDisplayName();
            g.drawString(font, name, px + (pw - font.width(name)) / 2, y + h - 18, MenuStyle.TEXT, false);
        }
    }

    private static void drawRow(GuiGraphics g, Font font, SoldierEntity soldier, int x, int y, int w,
            boolean selected) {
        MenuStyle.card(g, x, y, w, ROW_HEIGHT, selected);
        g.fill(x + 3, y + 2, x + 3 + ROW_HEIGHT - 4, y + ROW_HEIGHT - 2, 0xFF0A1220);
        MenuData.renderEntity(g, soldier, x + 3, y + 2, x - 1 + ROW_HEIGHT, y + ROW_HEIGHT - 2, 0.3F, 0.0F, false);
        int tx = x + ROW_HEIGHT + 4;
        Component name = Component.translatable("kn8.menu.squad.soldier", Integer.toHexString(soldier.getId())
                .toUpperCase());
        g.drawString(font, name, tx, y + 3, MenuStyle.TEXT, false);
        // Vida a direita; o texto da esquerda e cortado antes dela.
        int barW = Math.min(60, w / 4);
        int bx = x + w - barW - 6;
        int textW = Math.round((bx - 4 - tx) / MenuStyle.SMALL);
        Component weapon = soldier.getMainHandItem().isEmpty() ? Component.translatable("kn8.menu.squad.unarmed")
                : soldier.getMainHandItem().getHoverName();
        MenuStyle.small(g, font, Component.literal(font.plainSubstrByWidth(Component.translatable(
                "kn8.menu.squad.weapon", weapon).getString(), textW)), tx, y + 13, MenuStyle.TEXT_DIM);
        MenuStyle.small(g, font, Component.literal(font.plainSubstrByWidth(Component.translatable(
                "kn8.menu.squad.level", Component.translatable("kn8.menu.squad.level." + soldier.powerLevel()))
                .getString(), textW)), tx, y + 20, MenuStyle.TEXT_ACCENT);
        float health = soldier.getHealth() / Math.max(1.0F, soldier.getMaxHealth());
        MenuStyle.small(g, font, Component.literal(Math.round(soldier.getHealth()) + " / "
                + Math.round(soldier.getMaxHealth())), bx, y + 6, MenuStyle.TEXT_DIM);
        MenuStyle.bar(g, bx, y + 14, barW, 4, health, 0xFF1E8A3A, MenuStyle.GREEN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < visible && i + scroll < soldiers.size(); i++) {
            if (MenuStyle.inside(mouseX, mouseY, listX, listY + i * (ROW_HEIGHT + 2), listW, ROW_HEIGHT)) {
                selected = i + scroll;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll = Math.max(0, scroll - (int) Math.signum(amount));
        return true;
    }
}
