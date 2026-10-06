package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.kn8.common.craft.CraftC2S;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WorkbenchRecipeDef;
import com.kn8.common.registry.KN8Blocks;

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
 * Secao FABRICACAO do Arsenal (0.2, Etapa 3): receitas da bancada (dados sincronizados), ingredientes que o jogador
 * tem/precisa, trava por patente e o botao FABRICAR. O cliente so mostra e pede; o servidor confere tudo de novo.
 */
final class CraftingSection {

    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_W = 70;
    private static final int BUTTON_H = 14;
    /** Mesmo alcance do servidor ({@code WorkbenchService}); so para o aviso, quem decide e o servidor. */
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

    static List<Map.Entry<ResourceLocation, WorkbenchRecipeDef>> recipes() {
        List<Map.Entry<ResourceLocation, WorkbenchRecipeDef>> list =
                new ArrayList<>(KN8Data.WORKBENCH.client().entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, WorkbenchRecipeDef> e) -> e.getValue().order())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    void render(GuiGraphics g, Font font, int x, int y, int w, int detailX, int detailW, int bottom, int mouseX,
            int mouseY, int listX, int listY, int listW) {
        this.listX = listX;
        this.listY = listY;
        this.listW = listW;
        LocalPlayer player = MenuData.player();
        List<Map.Entry<ResourceLocation, WorkbenchRecipeDef>> list = recipes();
        if (player == null || list.isEmpty()) {
            MenuStyle.comingSoon(g, font, Component.translatable("kn8.workbench.empty"), x, (listY + bottom) / 2, w);
            return;
        }
        rows = Math.max(1, (bottom - listY) / (ROW_HEIGHT + 2));
        scroll = Math.max(0, Math.min(scroll, list.size() - rows));
        selected = Math.min(selected, list.size() - 1);
        for (int i = 0; i < rows && scroll + i < list.size(); i++) {
            int index = scroll + i;
            WorkbenchRecipeDef recipe = list.get(index).getValue();
            int ry = listY + i * (ROW_HEIGHT + 2);
            ItemStack stack = stack(recipe.result(), recipe.count());
            boolean unlocked = MenuData.unlocked(recipe.unlockId());
            MenuStyle.card(g, listX, ry, listW, ROW_HEIGHT, index == selected);
            MenuStyle.item(g, stack, listX + 3, ry + 3, 16);
            MenuStyle.small(g, font, stack.getHoverName(), listX + 24, ry + 4,
                    unlocked ? MenuStyle.TEXT : MenuStyle.TEXT_DIM);
            Component status = !unlocked ? Component.translatable("kn8.workbench.locked")
                    : hasAll(player, recipe) ? Component.translatable("kn8.workbench.ready")
                    : Component.translatable("kn8.workbench.category." + recipe.category());
            MenuStyle.scaled(g, font, status, listX + 24, ry + 13, !unlocked ? MenuStyle.RED
                    : hasAll(player, recipe) ? MenuStyle.GREEN : MenuStyle.TEXT_DIM, 0.6F);
        }
        if (list.size() > rows) {
            int trackH = rows * (ROW_HEIGHT + 2) - 2;
            int thumbH = Math.max(8, trackH * rows / list.size());
            int thumbY = listY + (trackH - thumbH) * scroll / Math.max(1, list.size() - rows);
            g.fill(listX + listW + 1, listY, listX + listW + 3, listY + trackH, MenuStyle.BAR_EMPTY);
            g.fill(listX + listW + 1, thumbY, listX + listW + 3, thumbY + thumbH, MenuStyle.ACCENT);
        }
        renderDetail(g, font, player, list.get(selected).getValue(), detailX, detailW, bottom, mouseX, mouseY);
    }

    private void renderDetail(GuiGraphics g, Font font, LocalPlayer player, WorkbenchRecipeDef recipe, int detailX,
            int detailW, int bottom, int mouseX, int mouseY) {
        ItemStack result = stack(recipe.result(), recipe.count());
        MenuStyle.card(g, detailX, listY, detailW, bottom - listY, false);
        Component name = recipe.count() > 1 ? Component.literal(recipe.count() + "x ").append(result.getHoverName())
                : result.getHoverName();
        g.drawString(font, name, detailX + 6, listY + 5, MenuStyle.TEXT, false);
        MenuStyle.badge(g, font, Component.translatable("kn8.workbench.category." + recipe.category()), detailX + 6,
                listY + 16, MenuStyle.ACCENT);
        float iconSize = Math.min(40, (bottom - listY) / 4.0F);
        MenuStyle.item(g, result, detailX + detailW - iconSize - 8, listY + 6, iconSize);

        int sy = listY + 30 + Math.max(0, Math.round(iconSize) - 30);
        MenuStyle.title(g, font, Component.translatable("kn8.workbench.ingredients"), detailX + 6, sy);
        sy += 13;
        for (WorkbenchRecipeDef.Ingredient ingredient : recipe.ingredients()) {
            ItemStack stack = stack(ingredient.item(), 1);
            int have = WorkbenchService.count(player.getInventory(), stack.getItem());
            boolean enough = have >= ingredient.count() || player.getAbilities().instabuild && have > 0;
            MenuStyle.item(g, stack, detailX + 8, sy - 1, 10);
            MenuStyle.small(g, font, stack.getHoverName(), detailX + 22, sy + 1, MenuStyle.TEXT);
            Component amount = Component.literal(have + " / " + ingredient.count());
            int amountW = Math.round(font.width(amount) * MenuStyle.SMALL);
            MenuStyle.small(g, font, amount, detailX + detailW - 10 - amountW, sy + 1,
                    have >= ingredient.count() ? MenuStyle.GREEN : MenuStyle.RED);
            sy += 12;
        }

        boolean unlocked = MenuData.unlocked(recipe.unlockId());
        if (!unlocked) {
            MenuStyle.small(g, font, Component.translatable("kn8.workbench.needs_rank",
                    MenuData.rankName(MenuData.unlockingRank(recipe.unlockId()))), detailX + 6, sy + 3,
                    MenuStyle.RED);
        }
        updateNear(player);
        buttonX = detailX + detailW - BUTTON_W - 8;
        buttonY = bottom - BUTTON_H - 8;
        buttonEnabled = unlocked && near && hasAll(player, recipe);
        boolean hovered = MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H);
        MenuStyle.button(g, font, Component.translatable("kn8.workbench.craft"), buttonX, buttonY, BUTTON_W, BUTTON_H,
                buttonEnabled, hovered, MenuStyle.GREEN);
        if (!near) {
            MenuStyle.small(g, font, Component.translatable("kn8.workbench.need_bench"), detailX + 6, buttonY + 4,
                    MenuStyle.YELLOW);
        }
    }

    /** O criativo ignora a distancia da bancada (igual ao servidor); conferir a cada meio segundo basta. */
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
            if (player.level().getBlockState(pos).is(KN8Blocks.DEFENSE_WORKBENCH.get())) {
                near = true;
                return;
            }
        }
    }

    private static boolean hasAll(LocalPlayer player, WorkbenchRecipeDef recipe) {
        for (WorkbenchRecipeDef.Ingredient ingredient : recipe.ingredients()) {
            if (WorkbenchService.count(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item()))
                    < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack stack(ResourceLocation item, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(item), count);
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        List<Map.Entry<ResourceLocation, WorkbenchRecipeDef>> list = recipes();
        if (buttonEnabled && MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H)
                && selected < list.size()) {
            MenuStyle.click();
            PacketDistributor.sendToServer(new CraftC2S(list.get(selected).getKey()));
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
