package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.kn8.common.base.BaseBuildC2S;
import com.kn8.common.base.BaseBuilder;
import com.kn8.common.base.OpenBaseS2C;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.BaseModuleDef;
import com.kn8.common.data.def.WorkbenchRecipeDef;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Aba BASE (PM-8D): a base do jogador vista de cima (grade 3 x 3 com o nucleo no meio e os 8 lotes), o modulo de
 * cada lote e, no lote escolhido, os modulos que da para construir (material tem/precisa, patente, modulos pedidos)
 * ou o botao de demolir. So mostra o que o servidor mandou ao usar o nucleo ({@link OpenBaseS2C}); o servidor confere
 * tudo de novo.
 */
final class BaseTab implements MenuTab {

    /** Indice do lote na grade 3 x 3 (o -1 e o nucleo). */
    private static final int[] GRID = {0, 1, 2, 3, -1, 4, 5, 6, 7};
    private static final int BUTTON_W = 80;
    private static final int BUTTON_H = 14;
    private static final int ROW_HEIGHT = 20;
    private static final double NEAR = 24.0;

    /** Ultimo estado mandado pelo servidor (null = o jogador ainda nao usou o nucleo nesta sessao). */
    private static BlockPos core;
    private static List<String> slots = List.of();

    private int selectedSlot;
    private int selectedModule;
    private int gridX;
    private int gridY;
    private int cell;
    private int listX;
    private int listY;
    private int listW;
    private int buttonX;
    private int buttonY;
    private boolean buttonEnabled;
    private boolean demolishArmed;

    static void update(OpenBaseS2C payload) {
        core = payload.core();
        slots = List.copyOf(payload.slots());
    }

    static List<Map.Entry<ResourceLocation, BaseModuleDef>> modules() {
        List<Map.Entry<ResourceLocation, BaseModuleDef>> list =
                new ArrayList<>(KN8Data.BASE_MODULE.client().entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, BaseModuleDef> e) -> e.getValue().order())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        MenuStyle.panel(g, x, y, w, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.tab.base"), x + 6, y + 6);
        LocalPlayer player = MenuData.player();
        if (core == null || player == null) {
            MenuStyle.comingSoon(g, font, Component.translatable("kn8.base.menu.use_core"), x, y + h / 2, w);
            return;
        }
        MenuStyle.small(g, font, Component.translatable("kn8.base.menu.core", core.toShortString()), x + 50, y + 8,
                MenuStyle.TEXT_DIM);

        // Grade 3 x 3 vista de cima (norte em cima).
        cell = Math.max(24, Math.min((h - 40) / 3, (w * 2 / 5) / 3));
        gridX = x + 8;
        gridY = y + 30;
        for (int i = 0; i < GRID.length; i++) {
            int cx = gridX + (i % 3) * (cell + 2);
            int cy = gridY + (i / 3) * (cell + 2);
            int slot = GRID[i];
            if (slot < 0) {
                g.fill(cx, cy, cx + cell, cy + cell, 0xE0203A55);
                MenuStyle.scaled(g, font, Component.translatable("kn8.base.menu.core_label"), cx + 3,
                        cy + cell / 2 - 3, MenuStyle.TEXT_ACCENT, 0.55F);
                continue;
            }
            String module = slot < slots.size() ? slots.get(slot) : "";
            MenuStyle.card(g, cx, cy, cell, cell, slot == selectedSlot);
            Component label = module.isEmpty() ? Component.translatable("kn8.base.menu.empty")
                    : BaseBuilder.moduleName(ResourceLocation.parse(module));
            MenuStyle.wrapped(g, font, label, cx + 3, cy + 3, cell - 6, module.isEmpty() ? MenuStyle.TEXT_DIM
                    : MenuStyle.GREEN, 3);
        }
        MenuStyle.small(g, font, Component.translatable("kn8.base.menu.north"), gridX + cell + cell / 2 - 3,
                gridY - 8, MenuStyle.TEXT_DIM);

        // Detalhe do lote escolhido.
        int detailX = gridX + 3 * (cell + 2) + 6;
        int detailW = x + w - 6 - detailX;
        int bottom = y + h - 6;
        MenuStyle.card(g, detailX, gridY, detailW, bottom - gridY, false);
        String built = selectedSlot < slots.size() ? slots.get(selectedSlot) : "";
        buttonX = detailX + detailW - BUTTON_W - 8;
        buttonY = bottom - BUTTON_H - 8;
        // So o aviso; quem decide e o [base] manageDistance do servidor (padrao 24).
        boolean near = player.getAbilities().instabuild || player.distanceToSqr(Vec3.atCenterOf(core))
                <= NEAR * NEAR;
        if (!built.isEmpty()) {
            ResourceLocation id = ResourceLocation.parse(built);
            g.drawString(font, BaseBuilder.moduleName(id), detailX + 6, gridY + 5, MenuStyle.TEXT, false);
            MenuStyle.wrapped(g, font, Component.translatable("kn8.base_module.desc." + id.getPath()), detailX + 6,
                    gridY + 18, detailW - 12, MenuStyle.TEXT_DIM, 4);
            buttonEnabled = near;
            boolean hovered = MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H);
            MenuStyle.button(g, font, Component.translatable(demolishArmed ? "kn8.base.menu.confirm"
                    : "kn8.base.menu.demolish"), buttonX, buttonY, BUTTON_W, BUTTON_H, buttonEnabled, hovered,
                    MenuStyle.RED);
            MenuStyle.small(g, font, Component.translatable("kn8.base.menu.demolish_hint"), detailX + 6,
                    buttonY - 10, MenuStyle.YELLOW);
            return;
        }
        demolishArmed = false;
        List<Map.Entry<ResourceLocation, BaseModuleDef>> list = modules();
        if (list.isEmpty()) {
            return;
        }
        selectedModule = Math.min(selectedModule, list.size() - 1);
        listX = detailX + 4;
        listY = gridY + 4;
        listW = Math.max(80, detailW * 2 / 5);
        for (int i = 0; i < list.size(); i++) {
            int ry = listY + i * (ROW_HEIGHT + 2);
            if (ry + ROW_HEIGHT > bottom - 4) {
                break;
            }
            ResourceLocation id = list.get(i).getKey();
            boolean exists = slots.contains(id.toString());
            MenuStyle.card(g, listX, ry, listW, ROW_HEIGHT, i == selectedModule);
            MenuStyle.small(g, font, BaseBuilder.moduleName(id), listX + 4, ry + 4, MenuStyle.TEXT);
            if (exists) {
                MenuStyle.scaled(g, font, Component.translatable("kn8.base.menu.already_built"), listX + 4, ry + 12,
                        MenuStyle.GREEN, 0.55F);
            }
        }
        Map.Entry<ResourceLocation, BaseModuleDef> chosen = list.get(selectedModule);
        int infoX = listX + listW + 6;
        int infoW = detailX + detailW - 6 - infoX;
        int sy = listY;
        sy += MenuStyle.wrapped(g, font, Component.translatable("kn8.base_module.desc." + chosen.getKey().getPath()),
                infoX, sy, infoW, MenuStyle.TEXT_DIM, 5) + 4;
        boolean hasAll = true;
        for (WorkbenchRecipeDef.Ingredient ingredient : chosen.getValue().cost()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ingredient.item()));
            int have = WorkbenchService.count(player.getInventory(), stack.getItem());
            hasAll &= have >= ingredient.count() || player.getAbilities().instabuild;
            MenuStyle.item(g, stack, infoX, sy - 1, 9);
            MenuStyle.small(g, font, Component.literal(have + "/" + ingredient.count() + " ")
                    .append(stack.getHoverName()), infoX + 12, sy, have >= ingredient.count() ? MenuStyle.GREEN
                    : MenuStyle.RED);
            sy += 10;
        }
        boolean rankOk = chosen.getValue().rank().map(rank -> KN8Data.RANK.client().get(rank) == null
                || MenuData.rankOrder() >= KN8Data.RANK.client().get(rank).order()).orElse(true);
        if (!rankOk) {
            MenuStyle.small(g, font, Component.translatable("kn8.workbench.needs_rank", MenuData.rankName(
                    chosen.getValue().rank().get())), infoX, sy + 1, MenuStyle.RED);
            sy += 9;
        }
        boolean requiresOk = true;
        for (ResourceLocation required : chosen.getValue().requires()) {
            if (!slots.contains(required.toString())) {
                requiresOk = false;
                MenuStyle.small(g, font, Component.translatable("kn8.base.menu.needs_module",
                        BaseBuilder.moduleName(required)), infoX, sy + 1, MenuStyle.RED);
                sy += 9;
            }
        }
        buttonEnabled = near && hasAll && rankOk && requiresOk;
        boolean hovered = MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H);
        MenuStyle.button(g, font, Component.translatable("kn8.base.menu.build"), buttonX, buttonY, BUTTON_W,
                BUTTON_H, buttonEnabled, hovered, MenuStyle.GREEN);
        if (!near) {
            MenuStyle.small(g, font, Component.translatable("kn8.base.menu.too_far"), infoX, buttonY - 10,
                    MenuStyle.YELLOW);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (core == null) {
            return false;
        }
        for (int i = 0; i < GRID.length; i++) {
            int cx = gridX + (i % 3) * (cell + 2);
            int cy = gridY + (i / 3) * (cell + 2);
            if (GRID[i] >= 0 && MenuStyle.inside(mouseX, mouseY, cx, cy, cell, cell)) {
                selectedSlot = GRID[i];
                demolishArmed = false;
                return true;
            }
        }
        String built = selectedSlot < slots.size() ? slots.get(selectedSlot) : "";
        if (buttonEnabled && MenuStyle.inside(mouseX, mouseY, buttonX, buttonY, BUTTON_W, BUTTON_H)) {
            MenuStyle.click();
            if (!built.isEmpty()) {
                // Demolir pede um segundo clique (nao devolve o material).
                if (!demolishArmed) {
                    demolishArmed = true;
                    return true;
                }
                demolishArmed = false;
                PacketDistributor.sendToServer(new BaseBuildC2S(core, selectedSlot, ""));
                return true;
            }
            List<Map.Entry<ResourceLocation, BaseModuleDef>> list = modules();
            if (selectedModule < list.size()) {
                PacketDistributor.sendToServer(new BaseBuildC2S(core, selectedSlot,
                        list.get(selectedModule).getKey().toString()));
            }
            return true;
        }
        if (built.isEmpty()) {
            List<Map.Entry<ResourceLocation, BaseModuleDef>> list = modules();
            for (int i = 0; i < list.size(); i++) {
                if (MenuStyle.inside(mouseX, mouseY, listX, listY + i * (ROW_HEIGHT + 2), listW, ROW_HEIGHT)) {
                    selectedModule = i;
                    return true;
                }
            }
        }
        return false;
    }
}
