package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.kn8.common.career.CareerView;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.ResearchDef;
import com.kn8.common.data.def.WorkbenchRecipeDef;
import com.kn8.common.registry.KN8Blocks;
import com.kn8.common.research.ResearchC2S;
import com.kn8.common.research.ResearchService;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Secao PESQUISA do Arsenal (PM-8C): projetos do laboratorio (dados sincronizados), material de kaiju que o jogador
 * tem/precisa, tempo, patente e pre-requisitos, o que cada um libera e o bonus contra kaiju; a pesquisa em andamento
 * com o tempo que falta. O cliente so mostra e pede; o servidor ({@code ResearchService}) confere tudo de novo.
 */
final class ResearchSection {

    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_W = 80;
    private static final int BUTTON_H = 14;
    /** So para o aviso; o alcance de verdade e o {@code [research] terminalRange} do servidor. */
    private static final int REACH = 6;
    private static final int NEAR_CHECK_TICKS = 10;

    private int selected;
    private int scroll;
    private int listX;
    private int listY;
    private int listW;
    private int rows;
    private int buttonX;
    private int buttonY;
    private boolean buttonEnabled;
    private boolean near;
    private int nearCheck;

    static List<Map.Entry<ResourceLocation, ResearchDef>> projects() {
        List<Map.Entry<ResourceLocation, ResearchDef>> list = new ArrayList<>(KN8Data.RESEARCH.client().entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, ResearchDef> e) -> e.getValue().order())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    void render(GuiGraphics g, Font font, int x, int w, int detailX, int detailW, int bottom, int mouseX, int mouseY,
            int listX, int listY, int listW) {
        this.listX = listX;
        this.listY = listY;
        this.listW = listW;
        LocalPlayer player = MenuData.player();
        List<Map.Entry<ResourceLocation, ResearchDef>> list = projects();
        if (player == null || list.isEmpty()) {
            MenuStyle.comingSoon(g, font, Component.translatable("kn8.research.empty"), x, (listY + bottom) / 2, w);
            return;
        }
        CareerView.Research state = MenuData.career().research();
        rows = Math.max(1, (bottom - listY) / (ROW_HEIGHT + 2));
        scroll = Math.max(0, Math.min(scroll, list.size() - rows));
        selected = Math.min(selected, list.size() - 1);
        for (int i = 0; i < rows && scroll + i < list.size(); i++) {
            int index = scroll + i;
            ResourceLocation id = list.get(index).getKey();
            int ry = listY + i * (ROW_HEIGHT + 2);
            MenuStyle.card(g, listX, ry, listW, ROW_HEIGHT, index == selected);
            boolean done = state.done().contains(id);
            MenuStyle.small(g, font, ResearchService.name(id), listX + 6, ry + 4, done ? MenuStyle.TEXT
                    : MenuStyle.TEXT_DIM);
            Component status;
            int color;
            if (done) {
                status = Component.translatable("kn8.research.status.done");
                color = MenuStyle.GREEN;
            } else if (state.current().equals(id)) {
                status = Component.translatable("kn8.research.status.running", remaining(player, state));
                color = MenuStyle.YELLOW;
            } else {
                status = Component.translatable("kn8.research.category." + list.get(index).getValue().category());
                color = MenuStyle.TEXT_DIM;
            }
            MenuStyle.scaled(g, font, status, listX + 6, ry + 13, color, 0.6F);
        }
        if (list.size() > rows) {
            int trackH = rows * (ROW_HEIGHT + 2) - 2;
            int thumbH = Math.max(8, trackH * rows / list.size());
            int thumbY = listY + (trackH - thumbH) * scroll / Math.max(1, list.size() - rows);
            g.fill(listX + listW + 1, listY, listX + listW + 3, listY + trackH, MenuStyle.BAR_EMPTY);
            g.fill(listX + listW + 1, thumbY, listX + listW + 3, thumbY + thumbH, MenuStyle.ACCENT);
        }
        Map.Entry<ResourceLocation, ResearchDef> chosen = list.get(selected);
        renderDetail(g, font, player, state, chosen.getKey(), chosen.getValue(), detailX, detailW, bottom, mouseX,
                mouseY);
    }

    private void renderDetail(GuiGraphics g, Font font, LocalPlayer player, CareerView.Research state,
            ResourceLocation id, ResearchDef def, int detailX, int detailW, int bottom, int mouseX, int mouseY) {
        MenuStyle.card(g, detailX, listY, detailW, bottom - listY, false);
        g.drawString(font, ResearchService.name(id), detailX + 6, listY + 5, MenuStyle.TEXT, false);
        MenuStyle.badge(g, font, Component.translatable("kn8.research.category." + def.category()), detailX + 6,
                listY + 16, MenuStyle.ACCENT);
        int sy = listY + 30;
        sy += MenuStyle.wrapped(g, font, Component.translatable("kn8.research.desc." + id.getPath()), detailX + 6, sy,
                detailW - 12, MenuStyle.TEXT_DIM, 3) + 3;

        // Custo: material de kaiju no inventario.
        MenuStyle.small(g, font, Component.translatable("kn8.research.cost", ResearchService.formatTime(
                def.durationTicks())), detailX + 6, sy, MenuStyle.TEXT_ACCENT);
        sy += 9;
        boolean hasAll = true;
        for (WorkbenchRecipeDef.Ingredient ingredient : def.cost()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ingredient.item()));
            int have = WorkbenchService.count(player.getInventory(), stack.getItem());
            hasAll &= have >= ingredient.count() || player.getAbilities().instabuild;
            MenuStyle.item(g, stack, detailX + 8, sy - 1, 9);
            MenuStyle.small(g, font, stack.getHoverName(), detailX + 21, sy, MenuStyle.TEXT);
            Component amount = Component.literal(have + " / " + ingredient.count());
            int amountW = Math.round(font.width(amount) * MenuStyle.SMALL);
            MenuStyle.small(g, font, amount, detailX + detailW - 10 - amountW, sy,
                    have >= ingredient.count() ? MenuStyle.GREEN : MenuStyle.RED);
            sy += 10;
        }

        // O que libera e o bonus.
        for (ResourceLocation unlock : def.unlocks()) {
            Component item = BuiltInRegistries.ITEM.containsKey(unlock)
                    ? new ItemStack(BuiltInRegistries.ITEM.get(unlock)).getHoverName()
                    : Component.literal(unlock.toString());
            MenuStyle.small(g, font, Component.translatable("kn8.research.unlocks", item), detailX + 6, sy,
                    MenuStyle.GREEN);
            sy += 8;
        }
        if (def.damageBonus() > 0 && !def.targets().isEmpty()) {
            MenuStyle.small(g, font, Component.translatable("kn8.research.bonus_against", Math.round(
                    def.damageBonus() * 100), targets(def)), detailX + 6, sy, MenuStyle.GREEN);
            sy += 8;
        }

        // Requisitos.
        boolean done = state.done().contains(id);
        boolean rankOk = def.rank().map(rank -> KN8Data.RANK.client().get(rank) == null
                || MenuData.rankOrder() >= KN8Data.RANK.client().get(rank).order()).orElse(true);
        if (!rankOk) {
            MenuStyle.small(g, font, Component.translatable("kn8.workbench.needs_rank", MenuData.rankName(
                    def.rank().get())), detailX + 6, sy + 1, MenuStyle.RED);
            sy += 8;
        }
        boolean prereqOk = true;
        for (ResourceLocation required : def.requires()) {
            if (!state.done().contains(required)) {
                prereqOk = false;
                MenuStyle.small(g, font, Component.translatable("kn8.research.needs", ResearchService.name(required)),
                        detailX + 6, sy + 1, MenuStyle.RED);
                sy += 8;
            }
        }

        updateNear(player);
        buttonX = detailX + detailW - BUTTON_W - 8;
        buttonY = bottom - BUTTON_H - 8;
        boolean running = state.current().equals(id);
        if (running) {
            long total = Math.max(1, def.durationTicks());
            long left = Math.max(0, state.finishTick() - player.level().getGameTime());
            MenuStyle.bar(g, detailX + 6, buttonY - 8, detailW - 12, 4, 1.0F - (float) left / total,
                    MenuStyle.ACCENT_DARK, MenuStyle.ACCENT);
        }
        buttonEnabled = !done && !state.busy() && rankOk && prereqOk && near && hasAll;
        boolean hovered = MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H);
        Component label = done ? Component.translatable("kn8.research.status.done")
                : running ? Component.literal(remaining(player, state))
                : Component.translatable("kn8.research.start");
        MenuStyle.button(g, font, label, buttonX, buttonY, BUTTON_W, BUTTON_H, buttonEnabled, hovered,
                MenuStyle.GREEN);
        if (!done && !running) {
            Component hint = state.busy() ? Component.translatable("kn8.research.busy_hint",
                    ResearchService.name(state.current())) : !near ? Component.translatable("kn8.research.need_terminal")
                    : null;
            if (hint != null) {
                MenuStyle.small(g, font, hint, detailX + 6, buttonY + 4, MenuStyle.YELLOW);
            }
        }
    }

    private static Component targets(ResearchDef def) {
        Component result = Component.empty();
        boolean first = true;
        for (String target : def.targets()) {
            Component name = target.startsWith("#")
                    ? Component.translatable("kn8.kaiju_class." + target.substring(1))
                    : Component.translatable("entity." + target.replace(':', '.'));
            result = first ? name.copy() : result.copy().append(", ").append(name);
            first = false;
        }
        return result;
    }

    private static String remaining(LocalPlayer player, CareerView.Research state) {
        return ResearchService.formatTime(Math.max(0, state.finishTick() - player.level().getGameTime()));
    }

    /** O criativo ignora a distancia do terminal (igual ao servidor); conferir a cada meio segundo basta. */
    private void updateNear(LocalPlayer player) {
        if (player.getAbilities().instabuild) {
            near = true;
            return;
        }
        if (nearCheck-- > 0) {
            return;
        }
        nearCheck = NEAR_CHECK_TICKS;
        near = false;
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-REACH, -REACH, -REACH),
                center.offset(REACH, REACH, REACH))) {
            if (player.level().getBlockState(pos).is(KN8Blocks.RESEARCH_TERMINAL.get())) {
                near = true;
                return;
            }
        }
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        List<Map.Entry<ResourceLocation, ResearchDef>> list = projects();
        if (buttonEnabled && MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H)
                && selected < list.size()) {
            MenuStyle.click();
            PacketDistributor.sendToServer(new ResearchC2S(list.get(selected).getKey()));
            return true;
        }
        for (int i = 0; i < rows && scroll + i < list.size(); i++) {
            if (MenuStyle.inside(mouseX, mouseY, listX, listY + i * (ROW_HEIGHT + 2), listW, ROW_HEIGHT)) {
                selected = scroll + i;
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (MenuStyle.inside(mouseX, mouseY, listX, listY, listW + 4, rows * (ROW_HEIGHT + 2))) {
            scroll = Math.max(0, scroll - (int) Math.signum(amount));
            return true;
        }
        return false;
    }
}
