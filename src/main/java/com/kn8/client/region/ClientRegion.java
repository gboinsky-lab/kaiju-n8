package com.kn8.client.region;

import com.kn8.KN8Constants;
import com.kn8.client.hud.KaijuHealthBar;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.common.site.RegionAlertS2C;
import com.kn8.common.site.RegionService;
import com.kn8.common.site.SiteService;
import com.kn8.core.world.RegionAlertMath.State;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Estado de alerta da regiao onde o jogador esta (PM-9): so a copia do ultimo {@link RegionAlertS2C}. Desenha uma
 * faixa no canto superior esquerdo quando a regiao nao esta normal; a aba Alertas le o mesmo estado.
 */
public final class ClientRegion {

    private static final int MARGIN = 6;
    private static final int BACKGROUND = 0x90101820;
    private static volatile RegionAlertS2C state = RegionAlertS2C.NONE;

    private ClientRegion() {
    }

    public static void install() {
        KN8ClientHooks.register(RegionAlertS2C.TYPE, payload -> state = payload);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> state = RegionAlertS2C.NONE);
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, KN8Constants.id("region_alert"), ClientRegion::render);
    }

    public static RegionAlertS2C state() {
        return state;
    }

    public static State level() {
        return State.byIndex(state.state());
    }

    public static int color(State value) {
        return switch (value) {
            case COMBAT -> 0xFFFF3B3B;
            case ALERT -> 0xFFFFC233;
            case RECOVERY -> 0xFF35C8FF;
            case NORMAL -> 0xFF4CD964;
        };
    }

    /** "Shinagawa — ALERTA: Primigenius (0:45)" ou vazio fora de uma regiao. */
    public static Component line(long gameTime) {
        RegionAlertS2C current = state;
        ResourceLocation site = ResourceLocation.tryParse(current.site());
        if (current.site().isEmpty() || site == null) {
            return Component.empty();
        }
        State value = level();
        Component text = SiteService.name(site).append(" — ").append(Component.translatable("kn8.region.state."
                + value.name().toLowerCase(java.util.Locale.ROOT)));
        if (value == State.NORMAL) {
            return text;
        }
        long left = Math.max(0, current.until() - gameTime);
        long seconds = (left + 19) / 20;
        return Component.empty().append(text).append(": ").append(RegionService.causeName(current.cause()))
                .append(String.format(java.util.Locale.ROOT, " (%d:%02d)", seconds / 60, seconds % 60));
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || level() == State.NORMAL
                || state.site().isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        Component text = line(minecraft.player.level().getGameTime());
        int width = (int) (font.width(text) * 0.8F) + 12;
        // Abaixo da barra de vida do kaiju quando ela aparece (as duas ficam no topo).
        int y = Math.max(MARGIN, KaijuHealthBar.lastBottom() + MARGIN);
        g.fill(MARGIN, y, MARGIN + width, y + 14, BACKGROUND);
        g.fill(MARGIN, y, MARGIN + 2, y + 14, color(level()));
        g.pose().pushPose();
        g.pose().translate(MARGIN + 6, y + 4, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, text, 0, 0, color(level()), false);
        g.pose().popPose();
    }
}
