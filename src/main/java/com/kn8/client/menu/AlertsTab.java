package com.kn8.client.menu;

import java.util.List;
import java.util.Locale;

import com.kn8.client.invasion.ClientInvasion;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionStateS2C;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Aba ALERTAS: quadro de invasao (o evento chega na Etapa 7) e a lista de kaiju avistados perto do jogador, do mais
 * perto ao mais longe, com ameaca, distancia, direcao e o que estao fazendo. A lista vem das entidades que o
 * cliente ja enxerga (nada novo e mandado pelo servidor).
 */
final class AlertsTab implements MenuTab {

    private static final double RADIUS = 128.0;
    private static final int[] LEVEL_COLORS = {MenuStyle.GREEN, MenuStyle.YELLOW, MenuStyle.ORANGE, MenuStyle.RED,
            0xFFD040FF};
    private static final int ROW_HEIGHT = 30;

    private List<KaijuEntity> sighted = List.of();
    private int scroll;

    @Override
    public void tick() {
        sighted = MenuData.nearbyKaiju(RADIUS);
        MenuData.tickPreviews();
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        if (sighted.isEmpty()) {
            sighted = MenuData.nearbyKaiju(RADIUS);
        }
        MenuStyle.panel(g, x, y, w, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.tab.alerts"), x + 6, y + 6);
        Component count = Component.translatable("kn8.menu.alerts.count", sighted.size());
        int countColor = sighted.isEmpty() ? MenuStyle.TEXT_DIM : MenuStyle.RED;
        MenuStyle.small(g, font, count, x + w - 8 - Math.round(font.width(count) * MenuStyle.SMALL), y + 7,
                countColor);

        // Quadro da invasao.
        int iy = y + 20;
        int ih = 30;
        g.fill(x + 6, iy, x + w - 6, iy + ih, MenuStyle.RED_DARK);
        g.fill(x + 6, iy, x + w - 6, iy + 1, MenuStyle.RED);
        g.fill(x + 6, iy + ih - 1, x + w - 6, iy + ih, MenuStyle.RED);
        g.fill(x + 6, iy, x + 7, iy + ih, MenuStyle.RED);
        g.fill(x + w - 7, iy, x + w - 6, iy + ih, MenuStyle.RED);
        drawInvasion(g, font, x + 12, iy, w - 24);

        // Kaiju avistados.
        int listY = iy + ih + 6;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.alerts.sighted", (int) RADIUS), x + 8, listY,
                MenuStyle.TEXT);
        listY += 10;
        int listH = y + h - 6 - listY;
        if (sighted.isEmpty()) {
            MenuStyle.comingSoon(g, font, Component.translatable("kn8.menu.alerts.none"), x, listY + listH / 2 - 4,
                    w);
            return;
        }
        int visible = Math.max(1, listH / (ROW_HEIGHT + 2));
        scroll = Math.max(0, Math.min(scroll, sighted.size() - visible));
        for (int i = 0; i < visible && i + scroll < sighted.size(); i++) {
            drawRow(g, font, sighted.get(i + scroll), x + 6, listY + i * (ROW_HEIGHT + 2), w - 12);
        }
    }

    /** Quadro da invasao: nome, fase/onda, kaiju restantes, relogio e direcao do centro. */
    private static void drawInvasion(GuiGraphics g, Font font, int x, int y, int w) {
        InvasionStateS2C state = ClientInvasion.state();
        if (!state.active()) {
            g.drawString(font, Component.translatable("kn8.menu.alerts.invasion_none_title"), x, y + 6,
                    MenuStyle.TEXT_DIM, false);
            MenuStyle.small(g, font, Component.translatable("kn8.menu.alerts.no_invasion"), x, y + 18,
                    MenuStyle.TEXT_DIM);
            return;
        }
        Invasion.Phase phase = Invasion.Phase.values()[Math.min(state.phase(), Invasion.Phase.values().length - 1)];
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        Component title = Component.translatable("kn8.menu.alerts.invasion").append(" · ")
                .append(Component.translatable("kn8.invasion." + state.invasion().getPath()));
        g.drawString(font, title, x, y + 6, blink ? MenuStyle.RED : MenuStyle.ORANGE, false);
        // 0.3: selo do nivel (1 a 5) ao lado do nome, mais quente quanto maior.
        int level = Math.max(1, Math.min(5, state.level()));
        MenuStyle.badge(g, font, Component.translatable("kn8.menu.alerts.level", level,
                Component.translatable("kn8.invasion.level." + level)), x + font.width(title) + 6, y + 6,
                LEVEL_COLORS[level - 1]);
        LocalPlayer player = MenuData.player();
        long now = player == null ? 0 : player.level().getGameTime();
        long seconds = Math.max(0, (state.timerEnd() - now) / 20);
        Component phaseText = Component.translatable("kn8.menu.alerts.phase." + phase.name().toLowerCase(Locale.ROOT),
                state.wave() + 1, state.waves(), String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60));
        MenuStyle.small(g, font, phaseText, x, y + 18, MenuStyle.TEXT);
        Component right = Component.translatable("kn8.menu.alerts.remaining", state.remaining(), state.total());
        if (player != null) {
            double dx = state.center().getX() + 0.5 - player.getX();
            double dz = state.center().getZ() + 0.5 - player.getZ();
            right = right.copy().append(" · ").append(Component.translatable("kn8.menu.alerts.distance_to",
                    Math.round(Math.sqrt(dx * dx + dz * dz))));
        }
        int rightW = Math.round(font.width(right) * MenuStyle.SMALL);
        MenuStyle.small(g, font, right, x + w - rightW, y + 18, MenuStyle.YELLOW);
    }

    private static void drawRow(GuiGraphics g, Font font, KaijuEntity kaiju, int x, int y, int w) {
        KaijuDef def = kaiju.def().orElse(null);
        MenuData.Threat threat = def == null ? MenuData.Threat.LOW : MenuData.threat(def);
        MenuStyle.card(g, x, y, w, ROW_HEIGHT, false);
        g.fill(x + 1, y + 1, x + 3, y + ROW_HEIGHT - 1, threat.color);
        // Miniatura da especie.
        g.fill(x + 5, y + 2, x + 5 + ROW_HEIGHT - 4, y + ROW_HEIGHT - 2, 0xFF0A1220);
        MenuData.renderEntity(g, MenuData.preview(kaiju.getType()), x + 5, y + 2, x + 1 + ROW_HEIGHT, y + ROW_HEIGHT
                - 2, 0.6F, 0.0F, false);
        // Sinal de alerta (!) com a cor da ameaca.
        int ax = x + ROW_HEIGHT + 6;
        g.fill(ax, y + 7, ax + 12, y + ROW_HEIGHT - 7, (threat.color & 0x00FFFFFF) | 0x40000000);
        g.drawString(font, "!", ax + 5, y + 11, threat.color, false);

        int tx = ax + 18;
        g.drawString(font, kaiju.getDisplayName(), tx, y + 4, MenuStyle.TEXT, false);
        Component kind = def == null ? Component.empty() : MenuData.kaijuClass(def);
        MenuStyle.small(g, font, Component.translatable("kn8.menu.alerts.threat", kind, threat.label()), tx,
                y + 14, threat.color);
        MenuStyle.small(g, font, Component.translatable("kn8.kaiju_state." + kaiju.state().name()
                .toLowerCase(Locale.ROOT)), tx, y + 21, MenuStyle.TEXT_DIM);

        int distance = Math.round(kaiju.distanceTo(MenuData.player()));
        Component where = Component.translatable("kn8.menu.alerts.distance", distance, MenuData.direction(kaiju));
        g.drawString(font, where, x + w - 8 - font.width(where), y + 11, MenuStyle.TEXT, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll = Math.max(0, scroll - (int) Math.signum(amount));
        return true;
    }
}
