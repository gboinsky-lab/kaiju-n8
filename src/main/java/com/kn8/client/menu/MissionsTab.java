package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.career.CareerView;
import com.kn8.common.career.MissionActionC2S;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.MissionDef;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Aba MISSOES: missoes do datapack (sincronizadas) em cartoes com categoria, objetivos, requisito e recompensa.
 * Desde a Etapa 5: Disponiveis (ACEITAR), Ativas (progresso e ABANDONAR) e Concluidas. O botao so manda o pedido;
 * o servidor decide (requisitos, espera, limite de missoes ativas).
 */
final class MissionsTab implements MenuTab {

    private static final String[] FILTERS = {"available", "active", "done"};
    private static final int CARD_HEIGHT = 54;

    private int filter;
    private int scroll;
    /** Botoes desenhados no ultimo quadro: x, y, largura, altura e a acao (missao + aceitar/abandonar). */
    private final List<Button> buttons = new ArrayList<>();

    private record Button(int x, int y, int w, int h, ResourceLocation mission, int action) {
    }
    private int filterX;
    private int filterY;
    private int filterW;

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        MenuStyle.panel(g, x, y, w, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.tab.missions"), x + 6, y + 6);

        List<Map.Entry<ResourceLocation, MissionDef>> all = new ArrayList<>(KN8Data.MISSION.client().entrySet());
        all.sort(Comparator.comparing(entry -> entry.getKey().getPath()));
        CareerView career = MenuData.career();
        Map<ResourceLocation, CareerView.Active> active = new HashMap<>();
        career.active().forEach(mission -> active.put(mission.id(), mission));
        List<Map.Entry<ResourceLocation, MissionDef>> available = all.stream()
                .filter(entry -> !active.containsKey(entry.getKey())
                        && (entry.getValue().repeatable() || !career.completed().contains(entry.getKey())))
                .toList();
        List<Map.Entry<ResourceLocation, MissionDef>> running = all.stream()
                .filter(entry -> active.containsKey(entry.getKey())).toList();
        List<Map.Entry<ResourceLocation, MissionDef>> done = all.stream()
                .filter(entry -> career.completed().contains(entry.getKey())).toList();
        List<Map.Entry<ResourceLocation, MissionDef>> missions = filter == 0 ? available : filter == 1 ? running
                : done;
        buttons.clear();
        filterX = x + 6;
        filterY = y + 20;
        filterW = (w - 12) / FILTERS.length;
        for (int i = 0; i < FILTERS.length; i++) {
            int count = i == 0 ? available.size() : i == 1 ? running.size() : done.size();
            Component label = Component.translatable("kn8.menu.missions." + FILTERS[i], count);
            boolean hovered = MenuStyle.inside(mouseX, mouseY, filterX + i * filterW, filterY, filterW - 2, 13);
            MenuStyle.button(g, font, label, filterX + i * filterW, filterY, filterW - 2, 13, true,
                    hovered || i == filter, i == filter ? MenuStyle.ACCENT : MenuStyle.OUTLINE_LIGHT);
        }

        int listY = filterY + 18;
        int listH = y + h - 6 - listY;
        if (missions.isEmpty()) {
            MenuStyle.comingSoon(g, font, Component.translatable("kn8.menu.missions.empty." + FILTERS[filter]), x,
                    listY + listH / 2 - 4, w);
            return;
        }
        int visible = Math.max(1, listH / (CARD_HEIGHT + 3));
        scroll = Math.max(0, Math.min(scroll, missions.size() - visible));
        g.enableScissor(x + 1, listY, x + w - 1, y + h - 4);
        for (int i = 0; i < missions.size() - scroll && i <= visible; i++) {
            Map.Entry<ResourceLocation, MissionDef> entry = missions.get(i + scroll);
            drawCard(g, font, entry.getKey(), entry.getValue(), active.get(entry.getKey()),
                    career.completed().contains(entry.getKey()), x + 6, listY + i * (CARD_HEIGHT + 3), w - 12,
                    mouseX, mouseY);
        }
        g.disableScissor();
    }

    private void drawCard(GuiGraphics g, Font font, ResourceLocation id, MissionDef mission,
            CareerView.Active running, boolean completed, int x, int y, int w, int mouseX, int mouseY) {
        int color = categoryColor(mission.category());
        MenuStyle.card(g, x, y, w, CARD_HEIGHT, false);
        // Icone da categoria (losango) a esquerda.
        g.fill(x + 4, y + 4, x + 20, y + 20, (color & 0x00FFFFFF) | 0x40000000);
        g.fill(x + 4, y + 4, x + 20, y + 5, color);
        g.fill(x + 4, y + 19, x + 20, y + 20, color);
        g.fill(x + 11, y + 8, x + 13, y + 16, color);
        g.fill(x + 8, y + 11, x + 16, y + 13, color);

        int textX = x + 25;
        int rewardsX = x + w * 7 / 10;
        MenuStyle.badge(g, font, Component.translatable("kn8.mission_category."
                + mission.category().getSerializedName()), textX, y + 3, color);
        g.drawString(font, Component.translatable("kn8.mission." + id.getPath() + ".name"), textX, y + 13,
                MenuStyle.TEXT, false);
        MenuStyle.wrapped(g, font, Component.translatable("kn8.mission." + id.getPath() + ".desc"), textX, y + 23,
                rewardsX - textX - 4, MenuStyle.TEXT_DIM, 1);
        // Objetivos com progresso (o atual em destaque) e requisito de patente.
        int oy = y + 31;
        for (int i = 0; i < mission.objectives().size(); i++) {
            if (oy > y + CARD_HEIGHT - 8) {
                break;
            }
            MissionDef.Objective objective = mission.objectives().get(i);
            int done = running == null ? (completed ? objective.count() : 0)
                    : i < running.progress().size() ? running.progress().get(i) : 0;
            int lineColor = done >= objective.count() ? MenuStyle.GREEN
                    : running != null && running.objective() == i ? MenuStyle.YELLOW : MenuStyle.TEXT;
            MenuStyle.small(g, font, objectiveText(objective, done), textX, oy, lineColor);
            oy += 7;
        }
        int remaining = running == null ? -1 : running.remainingTicks(MenuData.player().level().getGameTime());
        if (remaining >= 0) {
            int seconds = remaining / 20;
            MenuStyle.small(g, font, Component.translatable("kn8.menu.missions.time", seconds / 60,
                    String.format("%02d", seconds % 60)), rewardsX - 70, y + 13, MenuStyle.ORANGE);
        }
        mission.requires().rank().ifPresent(rank -> MenuStyle.small(g, font,
                Component.translatable("kn8.menu.missions.requires", MenuData.rankName(rank)),
                rewardsX - 70, y + 3, MenuStyle.TEXT_DIM));

        // Recompensas e botao.
        g.fill(rewardsX - 3, y + 3, rewardsX - 2, y + CARD_HEIGHT - 3, MenuStyle.OUTLINE);
        MenuStyle.small(g, font, Component.translatable("kn8.menu.missions.reward"), rewardsX, y + 3,
                MenuStyle.TEXT_DIM);
        int ry = y + 11;
        MissionDef.Rewards rewards = mission.rewards();
        if (rewards.merit() > 0) {
            MenuStyle.small(g, font, Component.translatable("kn8.menu.missions.merit", rewards.merit()), rewardsX,
                    ry, MenuStyle.YELLOW);
            ry += 7;
        }
        if (rewards.promoteTo().isPresent()) {
            MenuStyle.small(g, font, Component.translatable("kn8.menu.missions.promotion",
                    MenuData.rankName(rewards.promoteTo().get())), rewardsX, ry, MenuStyle.TEXT_ACCENT);
            ry += 7;
        }
        if (rewards.trainingXp() > 0) {
            MenuStyle.small(g, font, Component.translatable("kn8.menu.missions.training_xp", rewards.trainingXp()),
                    rewardsX, ry, MenuStyle.ACCENT);
            ry += 7;
        }
        int ix = rewardsX;
        for (ResourceLocation item : rewards.items()) {
            MenuStyle.item(g, new ItemStack(BuiltInRegistries.ITEM.get(item)), ix, ry, 10);
            ix += 12;
        }
        int bw = Math.min(56, x + w - rewardsX - 4);
        int bx = x + w - bw - 4;
        int by = y + CARD_HEIGHT - 15;
        boolean hovered = MenuStyle.inside(mouseX, mouseY, bx, by, bw, 12);
        if (running != null) {
            MenuStyle.button(g, font, Component.translatable("kn8.menu.missions.abandon"), bx, by, bw, 12, true,
                    hovered, MenuStyle.ORANGE);
            buttons.add(new Button(bx, by, bw, 12, id, MissionActionC2S.ABANDON));
        } else if (completed && !mission.repeatable()) {
            MenuStyle.badge(g, font, Component.translatable("kn8.menu.missions.completed"), bx, by + 2,
                    MenuStyle.GREEN);
        } else {
            Optional<Component> blocked = blockedReason(mission);
            MenuStyle.button(g, font, Component.translatable("kn8.menu.missions.accept"), bx, by, bw, 12,
                    blocked.isEmpty(), hovered, MenuStyle.RED);
            if (blocked.isEmpty()) {
                buttons.add(new Button(bx, by, bw, 12, id, MissionActionC2S.ACCEPT));
            } else if (hovered) {
                MenuStyle.tooltip(blocked.get());
            }
        }
    }

    /** Por que nao da para aceitar (patente ou missao anterior); o servidor confere de novo. */
    private static Optional<Component> blockedReason(MissionDef mission) {
        Optional<ResourceLocation> rank = mission.requires().rank();
        if (rank.isPresent()) {
            int needed = KN8Data.RANK.get(rank.get(), true).map(def -> def.order()).orElse(0);
            if (MenuData.rankOrder() < needed) {
                return Optional.of(Component.translatable("kn8.menu.missions.requires", MenuData.rankName(rank.get())));
            }
        }
        for (ResourceLocation before : mission.requires().completed()) {
            if (!MenuData.career().completed().contains(before)) {
                return Optional.of(Component.translatable("kn8.menu.missions.requires_mission",
                        Component.translatable("kn8.mission." + before.getPath() + ".name")));
            }
        }
        return Optional.empty();
    }

    private static Component objectiveText(MissionDef.Objective objective, int done) {
        // Chefe: nome do registry de chefes; os outros alvos sao especies de kaiju.
        Component target = objective.target().map(id -> objective.type() == MissionDef.ObjectiveType.DEFEAT_BOSS
                ? Component.translatable("kn8.boss." + id.getPath()) : MenuData.speciesName(id))
                .orElse(Component.empty());
        String key = "kn8.menu.objective." + objective.type().getSerializedName();
        return Component.translatable(key, objective.count(), target).append(" (" + done + "/" + objective.count()
                + ")");
    }

    private static int categoryColor(MissionDef.Category category) {
        return switch (category) {
            case STORY, BOSS -> MenuStyle.ACCENT;
            case DEFENSE, EMERGENCY, EXTERMINATION -> MenuStyle.RED;
            case PATROL, INVESTIGATION, ESCORT, RESCUE -> MenuStyle.ORANGE;
            case DISMANTLE -> MenuStyle.YELLOW;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Button pressed : buttons) {
            if (MenuStyle.inside(mouseX, mouseY, pressed.x(), pressed.y(), pressed.w(), pressed.h())) {
                PacketDistributor.sendToServer(new MissionActionC2S(pressed.action(), pressed.mission()));
                MenuStyle.click();
                return true;
            }
        }
        for (int i = 0; i < FILTERS.length; i++) {
            if (MenuStyle.inside(mouseX, mouseY, filterX + i * filterW, filterY, filterW - 2, 13)) {
                filter = i;
                scroll = 0;
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
