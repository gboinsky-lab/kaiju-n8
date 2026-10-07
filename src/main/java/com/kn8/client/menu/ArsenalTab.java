package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SuitDef;
import com.kn8.common.data.def.WeaponDef;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Aba ARSENAL / TRAJE: armas e trajes do datapack com os numeros do JSON (dano, alcance, cadencia, peso; armadura e
 * bonus do traje) e a fabricacao da bancada (Etapa 3, {@link CraftingSection}).
 */
final class ArsenalTab implements MenuTab {

    private static final String[] SECTIONS = {"weapons", "suits", "crafting"};
    private static final int ROW_HEIGHT = 22;
    private static final float TICKS_PER_SECOND = 20.0F;

    /** Secao que abre na proxima vez (a bancada abre direto na fabricacao). */
    static int requestedSection = -1;

    private final CraftingSection crafting = new CraftingSection();
    private int section;
    private int selected;
    private int sectionX;
    private int sectionY;
    private int sectionW;
    private int listX;
    private int listY;
    private int listW;
    private int rows;

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        if (requestedSection >= 0) {
            section = requestedSection;
            requestedSection = -1;
        }
        MenuStyle.panel(g, x, y, w, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.tab.arsenal"), x + 6, y + 6);
        sectionX = x + w / 3;
        sectionY = y + 4;
        sectionW = (w - w / 3 - 8) / SECTIONS.length;
        for (int i = 0; i < SECTIONS.length; i++) {
            boolean hovered = MenuStyle.inside(mouseX, mouseY, sectionX + i * sectionW, sectionY, sectionW - 2, 13);
            MenuStyle.button(g, font, Component.translatable("kn8.menu.arsenal." + SECTIONS[i]),
                    sectionX + i * sectionW, sectionY, sectionW - 2, 13, true, hovered || i == section,
                    i == section ? MenuStyle.ACCENT : MenuStyle.OUTLINE_LIGHT);
        }
        listX = x + 6;
        listY = y + 22;
        listW = Math.max(110, w * 2 / 5);
        int detailX = listX + listW + 6;
        int detailW = x + w - 6 - detailX;
        int bottom = y + h - 6;
        rows = Math.max(1, (bottom - listY) / (ROW_HEIGHT + 2));
        switch (section) {
            case 0 -> renderWeapons(g, font, detailX, detailW, bottom);
            case 1 -> renderSuits(g, font, detailX, detailW, bottom);
            default -> crafting.render(g, font, x, y, w, detailX, detailW, bottom, mouseX, mouseY, listX, listY,
                    listW);
        }
    }

    // --- armas -------------------------------------------------------------------------------------------------

    private static List<WeaponDef> weapons() {
        List<WeaponDef> list = new ArrayList<>(KN8Data.WEAPON.client().values());
        list.sort(Comparator.comparing(def -> def.item().getPath()));
        return list;
    }

    private void renderWeapons(GuiGraphics g, Font font, int detailX, int detailW, int bottom) {
        List<WeaponDef> list = weapons();
        if (list.isEmpty()) {
            return;
        }
        selected = Math.min(selected, list.size() - 1);
        for (int i = 0; i < rows && i < list.size(); i++) {
            WeaponDef weapon = list.get(i);
            int ry = listY + i * (ROW_HEIGHT + 2);
            ItemStack stack = stack(weapon.item());
            MenuStyle.card(g, listX, ry, listW, ROW_HEIGHT, i == selected);
            MenuStyle.item(g, stack, listX + 3, ry + 3, 16);
            MenuStyle.small(g, font, stack.getHoverName(), listX + 24, ry + 4, MenuStyle.TEXT);
            MenuStyle.scaled(g, font, style(weapon), listX + 24, ry + 13, MenuStyle.TEXT_DIM, 0.6F);
        }
        WeaponDef weapon = list.get(selected);
        ItemStack stack = stack(weapon.item());
        MenuStyle.card(g, detailX, listY, detailW, bottom - listY, false);
        g.drawString(font, stack.getHoverName(), detailX + 6, listY + 5, MenuStyle.TEXT, false);
        MenuStyle.small(g, font, style(weapon), detailX + 6, listY + 15, MenuStyle.TEXT_DIM);
        float iconSize = Math.min(56, (bottom - listY) / 3.0F);
        MenuStyle.item(g, stack, detailX + detailW - iconSize - 8, listY + 6, iconSize);

        int sy = listY + 24 + Math.max(0, Math.round(iconSize) - 24);
        boolean firearm = weapon.style() == WeaponDef.Style.FIREARM || weapon.style() == WeaponDef.Style.CANNON;
        WeaponDef.Action light = weapon.actions().get("light");
        if (light == null && !weapon.actions().isEmpty()) {
            light = weapon.actions().values().iterator().next();
        }
        float perSecond = light == null ? 0 : TICKS_PER_SECOND / light.durationTicks();
        int barW = detailW - 70;
        sy = stat(g, font, detailX + 6, sy, barW, "damage", String.format(Locale.ROOT, "%.0f", weapon.baseDamage()),
                weapon.baseDamage() / 20.0F);
        sy = stat(g, font, detailX + 6, sy, barW, "reach", String.format(Locale.ROOT, "%.1f", weapon.reach()),
                weapon.reach() / (firearm ? 64.0F : 6.0F));
        sy = stat(g, font, detailX + 6, sy, barW, "rate", String.format(Locale.ROOT, "%.1f/s", perSecond),
                perSecond / 3.0F);
        sy = stat(g, font, detailX + 6, sy, barW, "weight", "", weight(weapon.style()));
        WeaponDef.Action heavy = weapon.actions().get("heavy");
        if (heavy != null) {
            MenuStyle.small(g, font, Component.translatable("kn8.menu.arsenal.heavy", String.format(Locale.ROOT,
                    "%.1f", heavy.multiplier())), detailX + 6, sy + 2, MenuStyle.TEXT_DIM);
            sy += 8;
        }
        if (!weapon.combo().isEmpty()) {
            MenuStyle.small(g, font, Component.translatable("kn8.menu.arsenal.combo", weapon.combo().size()),
                    detailX + 6, sy + 2, MenuStyle.TEXT_DIM);
            sy += 8;
        }
        MenuStyle.small(g, font, Component.translatable("kn8.menu.arsenal.rank",
                MenuData.rankName(MenuData.requiredRank(weapon.item()))), detailX + 6, sy + 2, MenuStyle.TEXT_ACCENT);
    }

    private static int stat(GuiGraphics g, Font font, int x, int y, int barW, String key, String value,
            float fraction) {
        MenuStyle.small(g, font, Component.translatable("kn8.menu.arsenal.stat." + key), x, y, MenuStyle.TEXT_DIM);
        MenuStyle.bar(g, x + 36, y + 1, barW - 10, 4, fraction, MenuStyle.ACCENT_DARK, MenuStyle.ACCENT);
        MenuStyle.small(g, font, Component.literal(value), x + 30 + barW, y, MenuStyle.TEXT);
        return y + 9;
    }

    private static Component style(WeaponDef weapon) {
        return Component.translatable("kn8.menu.arsenal.style." + weapon.style().getSerializedName());
    }

    private static float weight(WeaponDef.Style style) {
        return switch (style) {
            case BLADE -> 0.3F;
            case FIREARM -> 0.5F;
            case HEAVY -> 0.8F;
            case CANNON -> 1.0F;
        };
    }

    // --- trajes ------------------------------------------------------------------------------------------------

    private void renderSuits(GuiGraphics g, Font font, int detailX, int detailW, int bottom) {
        List<Map.Entry<ResourceLocation, SuitDef>> list = new ArrayList<>(KN8Data.SUIT.client().entrySet());
        list.sort(Comparator.comparingDouble(entry -> entry.getValue().armor()));
        if (list.isEmpty()) {
            return;
        }
        selected = Math.min(selected, list.size() - 1);
        for (int i = 0; i < rows && i < list.size(); i++) {
            int ry = listY + i * (ROW_HEIGHT + 2);
            MenuStyle.card(g, listX, ry, listW, ROW_HEIGHT, i == selected);
            MenuStyle.small(g, font, Component.translatable("kn8.suit." + list.get(i).getKey().getPath()),
                    listX + 6, ry + 4, MenuStyle.TEXT);
            MenuStyle.scaled(g, font, Component.translatable("kn8.menu.arsenal.rank",
                    MenuData.rankName(MenuData.requiredRank(list.get(i).getKey()))), listX + 6, ry + 13,
                    MenuStyle.TEXT_DIM, 0.6F);
        }
        SuitDef suit = list.get(selected).getValue();
        MenuStyle.card(g, detailX, listY, detailW, bottom - listY, false);
        g.drawString(font, Component.translatable("kn8.suit." + list.get(selected).getKey().getPath()), detailX + 6,
                listY + 5, MenuStyle.TEXT, false);
        int sy = listY + 20;
        int barW = detailW - 70;
        sy = stat(g, font, detailX + 6, sy, barW, "armor", String.format(Locale.ROOT, "%.0f", suit.armor()),
                suit.armor() / 20.0F);
        sy = stat(g, font, detailX + 6, sy, barW, "toughness", String.format(Locale.ROOT, "%.1f", suit.toughness()),
                suit.toughness() / 4.0F);
        sy = stat(g, font, detailX + 6, sy, barW, "release_bonus", "+" + suit.releaseCapBonus() + "%",
                suit.releaseCapBonus() / 30.0F);
        stat(g, font, detailX + 6, sy, barW, "heat_resistance", String.format(Locale.ROOT, "%.0f%%",
                suit.heatResistance() * 100), suit.heatResistance());
        MenuStyle.small(g, font, Component.translatable("kn8.menu.arsenal.suit_soon"), detailX + 6, bottom - 12,
                MenuStyle.TEXT_DIM);
    }

    private static ItemStack stack(ResourceLocation item) {
        return new ItemStack(BuiltInRegistries.ITEM.get(item));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < SECTIONS.length; i++) {
            if (MenuStyle.inside(mouseX, mouseY, sectionX + i * sectionW, sectionY, sectionW - 2, 13)) {
                section = i;
                selected = 0;
                return true;
            }
        }
        if (section == 2) {
            return crafting.mouseClicked(mouseX, mouseY);
        }
        for (int i = 0; i < rows; i++) {
            if (MenuStyle.inside(mouseX, mouseY, listX, listY + i * (ROW_HEIGHT + 2), listW, ROW_HEIGHT)) {
                selected = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return section == 2 && crafting.mouseScrolled(mouseX, mouseY, scroll);
    }
}
