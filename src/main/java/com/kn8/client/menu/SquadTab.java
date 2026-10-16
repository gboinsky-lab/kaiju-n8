package com.kn8.client.menu;

import java.util.List;

import com.kn8.common.data.def.RankDef;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SquadCommandC2S;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Aba ESQUADRAO: soldados da Forca de Defesa perto do jogador (arma, nivel de forca, vida) e o soldado escolhido em
 * destaque. IA-5: recrutar/dispensar o escolhido e as ordens (seguir, ficar, atacar, recuar) para o esquadrao
 * inteiro, pelo {@link SquadCommandC2S}; o servidor confere patente, distancia e dono (o menu so pede).
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
    /** Botoes da ultima renderizacao (o clique usa as mesmas caixas). */
    private int ordersX;
    private int ordersY;
    private int orderW;
    private int recruitX;
    private int recruitY;
    private int recruitW;
    private boolean ordersEnabled;

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

        // Ordens (embaixo), para o esquadrao inteiro; tamanho do esquadrao pela patente.
        ordersY = y + h - 22;
        int members = members();
        int size = squadSize();
        MenuStyle.small(g, font, Component.translatable("kn8.menu.squad.orders"), x + 8, ordersY - 9,
                MenuStyle.TEXT_ACCENT);
        MenuStyle.small(g, font, Component.translatable("kn8.menu.squad.members", members, size), x + 60,
                ordersY - 9, MenuStyle.TEXT_DIM);
        ordersX = x + 8;
        orderW = (listPanelW - 16) / ORDERS.length;
        ordersEnabled = members > 0;
        for (int i = 0; i < ORDERS.length; i++) {
            int bx = ordersX + i * orderW;
            boolean hovered = MenuStyle.inside(mouseX, mouseY, bx, ordersY, orderW - 3, 15);
            MenuStyle.button(g, font, Component.translatable("kn8.menu.squad.order." + ORDERS[i]), bx, ordersY,
                    orderW - 3, 15, ordersEnabled, hovered, MenuStyle.ACCENT);
            if (hovered) {
                MenuStyle.tooltip(Component.translatable(ordersEnabled ? "kn8.menu.squad.order." + ORDERS[i]
                        + ".desc" : size > 0 ? "kn8.menu.squad.no_members" : "kn8.menu.squad.no_rank"));
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
            MenuData.renderEntity(g, soldier, px + 4, y + 4, px + pw - 4, y + h - 42, 0.5F, 0.0F, false);
            Component name = soldier.hasCustomName() ? soldier.getDisplayName()
                    : Component.translatable("kn8.soldier.variant." + soldier.variant());
            g.drawString(font, name, px + (pw - font.width(name)) / 2, y + h - 38, MenuStyle.TEXT, false);
            // Recrutar ou dispensar o escolhido.
            boolean mine = isMine(soldier);
            recruitW = pw - 12;
            recruitX = px + 6;
            recruitY = y + h - 22;
            boolean hovered = MenuStyle.inside(mouseX, mouseY, recruitX, recruitY, recruitW, 15);
            MenuStyle.button(g, font, Component.translatable(mine ? "kn8.menu.squad.dismiss"
                    : "kn8.menu.squad.recruit"), recruitX, recruitY, recruitW, 15, mine || size > 0, hovered,
                    mine ? MenuStyle.TEXT_DIM : MenuStyle.GREEN);
            if (hovered && !mine && size <= 0) {
                MenuStyle.tooltip(Component.translatable("kn8.menu.squad.no_rank"));
            }
        } else {
            recruitW = 0;
        }
    }

    /** Soldados por perto que seguem o jogador local. */
    private int members() {
        int count = 0;
        for (SoldierEntity soldier : soldiers) {
            if (isMine(soldier)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isMine(SoldierEntity soldier) {
        Player player = Minecraft.getInstance().player;
        return player != null && soldier.isLedBy(player);
    }

    /** Tamanho do esquadrao da patente ({@code npc_squad_size}); no criativo, o maior (o servidor faz igual). */
    private static int squadSize() {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.isCreative()) {
            return MenuData.ranks().stream().mapToInt(entry -> entry.getValue().npcSquadSize()).max().orElse(0);
        }
        return MenuData.currentRank().map(entry -> entry.getValue()).map(RankDef::npcSquadSize).orElse(0);
    }

    private static void drawRow(GuiGraphics g, Font font, SoldierEntity soldier, int x, int y, int w,
            boolean selected) {
        MenuStyle.card(g, x, y, w, ROW_HEIGHT, selected);
        g.fill(x + 3, y + 2, x + 3 + ROW_HEIGHT - 4, y + ROW_HEIGHT - 2, 0xFF0A1220);
        MenuData.renderEntity(g, soldier, x + 3, y + 2, x - 1 + ROW_HEIGHT, y + ROW_HEIGHT - 2, 0.3F, 0.0F, false);
        int tx = x + ROW_HEIGHT + 4;
        if (isMine(soldier)) {
            // IA-5: membro do esquadrao do jogador (faixa verde e a ordem atual).
            g.fill(x, y, x + 2, y + ROW_HEIGHT, MenuStyle.GREEN);
            Component order = Component.translatable("kn8.menu.squad.order."
                    + ORDERS[soldier.squadOrder().ordinal()]);
            MenuStyle.small(g, font, order, x + w - Math.round(font.width(order) * MenuStyle.SMALL) - 6, y + 21,
                    MenuStyle.GREEN);
        }
        // 0.4: papel da variante (Atirador, Patrulheiro...) no lugar de "Soldado".
        Component name = Component.translatable("kn8.menu.squad.soldier_role",
                Component.translatable("kn8.soldier.variant." + soldier.variant()),
                Integer.toHexString(soldier.getId()).toUpperCase());
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
        if (ordersEnabled) {
            for (int i = 0; i < ORDERS.length; i++) {
                if (MenuStyle.inside(mouseX, mouseY, ordersX + i * orderW, ordersY, orderW - 3, 15)) {
                    PacketDistributor.sendToServer(new SquadCommandC2S(SquadCommandC2S.ORDER, 0, i));
                    return true;
                }
            }
        }
        if (recruitW > 0 && !soldiers.isEmpty() && MenuStyle.inside(mouseX, mouseY, recruitX, recruitY, recruitW,
                15)) {
            SoldierEntity soldier = soldiers.get(Math.min(selected, soldiers.size() - 1));
            PacketDistributor.sendToServer(new SquadCommandC2S(isMine(soldier) ? SquadCommandC2S.DISMISS
                    : SquadCommandC2S.RECRUIT, soldier.getId(), 0));
            return true;
        }
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
