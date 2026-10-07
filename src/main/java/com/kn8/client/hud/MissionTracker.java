package com.kn8.client.hud;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerView;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Rastreador de missao (0.2, Etapa 5), canto superior direito: nome da primeira missao ativa, objetivo atual com
 * progresso e, se o objetivo tem ponto no mundo, distancia e uma seta que aponta para ele (relativa a camera).
 * So le a {@code CareerView} enviada pelo servidor.
 */
public final class MissionTracker {

    private static final int MARGIN = 6;
    private static final int WIDTH = 150;
    private static final int BACKGROUND = 0x90101820;
    private static final int ACCENT = 0xFF4DD0E1;
    private static final int TEXT = 0xFFE6F0F5;
    private static final int DIM = 0xFF9FB3BF;
    private static final int YELLOW = 0xFFFFD54F;

    private MissionTracker() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, KN8Constants.id("mission_tracker"), MissionTracker::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        List<CareerView.Active> active = player.getData(KN8Attachments.CAREER_VIEW).active();
        if (active.isEmpty()) {
            return;
        }
        CareerView.Active mission = active.get(0);
        Optional<MissionDef> def = KN8Data.MISSION.get(mission.id(), true);
        if (def.isEmpty() || mission.objective() >= def.get().objectives().size()) {
            return;
        }
        Font font = minecraft.font;
        MissionDef.Objective objective = def.get().objectives().get(mission.objective());
        int done = mission.objective() < mission.progress().size() ? mission.progress().get(mission.objective()) : 0;
        Component target = objective.target().map(id -> objective.type() == MissionDef.ObjectiveType.DEFEAT_BOSS
                ? Component.translatable("kn8.boss." + id.getPath())
                : Component.translatable("entity." + id.getNamespace() + "." + id.getPath()))
                .orElse(Component.empty());
        Component line = Component.translatable("kn8.menu.objective." + objective.type().getSerializedName(),
                objective.count(), target).append(" (" + done + "/" + objective.count() + ")");

        int x = g.guiWidth() - WIDTH - MARGIN;
        // Abaixo da barra de vida do kaiju quando ela aparece (antes uma cobria a outra).
        int y = Math.max(MARGIN + 30, KaijuHealthBar.lastBottom() + MARGIN);
        int height = mission.hasPoint() ? 40 : 28;
        g.fill(x, y, x + WIDTH, y + height, BACKGROUND);
        g.fill(x, y, x + 2, y + height, ACCENT);
        g.drawString(font, Component.translatable("kn8.mission." + mission.id().getPath() + ".name"), x + 6, y + 4,
                ACCENT, false);
        g.pose().pushPose();
        g.pose().translate(x + 6, y + 15, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, font.plainSubstrByWidth(line.getString(), (int) ((WIDTH - 10) / 0.8F)), 0, 0, YELLOW,
                false);
        g.pose().popPose();
        int remaining = mission.remainingTicks(player.level().getGameTime());
        if (remaining >= 0) {
            int seconds = remaining / 20;
            String time = (seconds / 60) + ":" + String.format("%02d", seconds % 60);
            g.drawString(font, time, x + WIDTH - 4 - font.width(time), y + 4, DIM, false);
        }
        if (mission.hasPoint()) {
            double dx = mission.point().getX() + 0.5 - player.getX();
            double dz = mission.point().getZ() + 0.5 - player.getZ();
            int distance = (int) Math.sqrt(dx * dx + dz * dz);
            // Angulo do ponto relativo ao olhar: 0 = em frente; positivo = a direita.
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float relative = Mth.wrapDegrees(targetYaw - player.getYRot());
            drawArrow(g, x + 12, y + 32, relative);
            g.drawString(font, Component.translatable("kn8.mission.distance", distance), x + 24, y + 28, TEXT, false);
        }
    }

    /** Seta de 9 px girada (desenhada com retangulos ao longo de um eixo). */
    private static void drawArrow(GuiGraphics g, int cx, int cy, float degrees) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(degrees));
        g.fill(-1, -4, 1, 5, ACCENT);
        g.fill(-2, -3, 2, -2, ACCENT);
        g.fill(-3, -2, 3, -1, ACCENT);
        g.fill(-4, -1, 4, 0, ACCENT);
        g.pose().popPose();
    }
}
