package com.kn8.client.menu;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.kn8.common.attribute.PowerView;
import com.kn8.common.career.CareerView;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Aba PERFIL / STATUS: retrato, patente e merito, Release e treino, traje, armas e estatisticas. */
final class ProfileTab implements MenuTab {

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        LocalPlayer player = MenuData.player();
        if (player == null) {
            return;
        }
        PowerView view = player.getData(KN8Attachments.POWER_VIEW);
        int portraitW = Math.max(80, w / 4);
        int middleW = (w - portraitW) * 11 / 20;
        int rightX = x + portraitW + middleW + 8;
        int rightW = x + w - rightX;

        // Retrato: o proprio jogador, seguindo o mouse.
        MenuStyle.panel(g, x, y, portraitW, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        int scale = Math.max(10, Math.round((h - 30) / 2.3F));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 2, y + 14, x + portraitW - 2, y + h - 18, scale,
                0.0625F, mouseX, mouseY, player);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.profile.title"), x + 5, y + 4);
        Component name = player.getName();
        g.drawString(font, name, x + (portraitW - font.width(name)) / 2, y + h - 13, MenuStyle.TEXT, false);

        int cx = x + portraitW + 4;
        MenuStyle.panel(g, cx, y, middleW, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        drawCareer(g, font, view, cx + 6, y + 6, middleW - 12);
        MenuStyle.panel(g, rightX, y, rightW, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        drawLoadout(g, font, player, rightX + 6, y + 6, rightW - 12, h - 12);
    }

    private static void drawCareer(GuiGraphics g, Font font, PowerView view, int x, int y, int w) {
        // Patente (insignia de divisas) e merito.
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.rank"), x, y, MenuStyle.TEXT_DIM);
        drawInsignia(g, x, y + 8);
        Map.Entry<ResourceLocation, RankDef> rank = MenuData.currentRank().orElse(null);
        Component rankName = rank == null ? Component.literal("-") : MenuData.rankName(rank.getKey());
        g.drawString(font, rankName, x + 18, y + 11, MenuStyle.TEXT, false);

        CareerView career = MenuData.career();
        int merit = career.merit();
        int nextMerit = career.isTopRank() ? 0 : career.nextMerit();
        int my = y + 28;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.merit"), x, my, MenuStyle.TEXT);
        // Proxima patente sem merito exigido: a promocao vem de missao (ex.: Exame de Admissao).
        Component meritValue = nextMerit > 0 ? Component.literal(merit + " / " + nextMerit)
                : Component.translatable("kn8.menu.profile.by_mission");
        MenuStyle.small(g, font, meritValue, x + w - Math.round(font.width(meritValue) * MenuStyle.SMALL), my,
                MenuStyle.TEXT_DIM);
        MenuStyle.bar(g, x, my + 8, w, 3, nextMerit > 0 ? merit / (float) nextMerit : 0, MenuStyle.ACCENT_DARK,
                MenuStyle.ACCENT);
        Component next = MenuData.nextRank().map(entry -> (Component) Component.translatable(
                "kn8.menu.profile.next_rank", MenuData.rankName(entry.getKey())))
                .orElse(Component.translatable("kn8.menu.profile.top_rank"));
        MenuStyle.small(g, font, next, x, my + 14, MenuStyle.TEXT_DIM);
        // Missao de avaliacao exigida pela proxima patente (ex.: Exame de Admissao).
        if (!career.nextMission().isEmpty()) {
            ResourceLocation mission = ResourceLocation.tryParse(career.nextMission());
            if (mission != null) {
                MenuStyle.scaled(g, font, Component.translatable("kn8.menu.profile.needs_mission",
                        Component.translatable("kn8.mission." + mission.getPath() + ".name")), x, my + 21,
                        MenuStyle.YELLOW, 0.6F);
            }
        }

        // Release.
        int ry = my + 30;
        g.fill(x, ry - 4, x + w, ry - 3, MenuStyle.OUTLINE);
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.release"), x, ry, MenuStyle.TEXT);
        Component capLabel = Component.translatable("kn8.menu.profile.cap");
        MenuStyle.small(g, font, capLabel, x + w - Math.round(font.width(capLabel) * MenuStyle.SMALL), ry,
                MenuStyle.TEXT_DIM);
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.current"), x, ry + 10,
                MenuStyle.TEXT_DIM);
        Component current = Component.translatable("kn8.menu.profile.current");
        MenuStyle.scaled(g, font, Component.literal(view.effective() + "%"),
                x + Math.round(font.width(current) * MenuStyle.SMALL) + 4, ry + 9, MenuStyle.TEXT_ACCENT, 1.0F);
        MenuStyle.bar(g, x + 56, ry + 11, w - 90, 5, view.effective() / 100.0F, MenuStyle.ACCENT_DARK,
                MenuStyle.ACCENT);
        // 0.5.0: a direita fica o limite pessoal (o talento raro aparece com estrela).
        Component cap = Component.literal((view.talentRare() ? "\u2605" : "") + view.trained() + "%");
        g.drawString(font, cap, x + w - font.width(cap), ry + 9, MenuStyle.TEXT, false);

        int ty = ry + 24;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.training_xp"), x, ty, MenuStyle.TEXT_DIM);
        float xp = view.xpToNext() > 0 ? view.releaseXp() / (float) view.xpToNext() : 1.0F;
        MenuStyle.bar(g, x + 56, ty + 1, w - 90, 4, xp, MenuStyle.ACCENT_DARK, MenuStyle.ACCENT);
        Component level = Component.translatable("kn8.menu.profile.trained", view.trained());
        MenuStyle.small(g, font, level, x + w - Math.round(font.width(level) * MenuStyle.SMALL), ty,
                MenuStyle.TEXT);

        // Condicao do traje.
        int sy = ty + 14;
        g.fill(x, sy - 4, x + w, sy - 3, MenuStyle.OUTLINE);
        row(g, font, x, sy, w, "kn8.menu.profile.stamina",
                Math.round(view.stamina()) + " / " + Math.round(view.maxStamina()), MenuStyle.GREEN);
        row(g, font, x, sy + 9, w, "kn8.menu.profile.heat",
                Math.round(view.heat()) + " / " + view.heatMax(), view.heatStage() >= 3 ? MenuStyle.RED
                        : MenuStyle.ORANGE);
        row(g, font, x, sy + 18, w, "kn8.menu.profile.excess", view.suit() ? "+" + view.excess()
                : Component.translatable("kn8.menu.profile.no_suit").getString(), view.suit() ? MenuStyle.ORANGE
                : MenuStyle.TEXT_DIM);
        // 0.5.0: atributos do corpo (forca, velocidade, resistencia, agilidade).
        row(g, font, x, sy + 27, w, "kn8.menu.profile.body", Component.translatable("kn8.menu.profile.body_values",
                view.strength(), view.speed(), view.resistance(), view.agility()).getString(), MenuStyle.TEXT);
    }

    private static void row(GuiGraphics g, Font font, int x, int y, int w, String key, String value, int color) {
        MenuStyle.small(g, font, Component.translatable(key), x, y, MenuStyle.TEXT_DIM);
        Component text = Component.literal(value);
        MenuStyle.small(g, font, text, x + w - Math.round(font.width(text) * MenuStyle.SMALL), y, color);
    }

    /** Insignia: tres divisas em V, no estilo do concept. */
    private static void drawInsignia(GuiGraphics g, int x, int y) {
        for (int chevron = 0; chevron < 3; chevron++) {
            int cy = y + chevron * 4;
            for (int i = 0; i < 7; i++) {
                int dy = Math.abs(3 - i) < 3 ? 3 - Math.abs(3 - i) : 0;
                g.fill(x + 2 + i * 2, cy + 3 - dy, x + 4 + i * 2, cy + 5 - dy, MenuStyle.OUTLINE_LIGHT);
            }
        }
    }

    private static void drawLoadout(GuiGraphics g, Font font, LocalPlayer player, int x, int y, int w, int h) {
        // Traje equipado: o do peito, com armadura e resistencia a calor do suit/*.json (0.3: trajes vestiveis).
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.suit"), x, y, MenuStyle.TEXT);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        g.fill(x, y + 9, x + 22, y + 31, MenuStyle.CARD);
        if (!chest.isEmpty()) {
            MenuStyle.item(g, chest, x + 3, y + 12, 16);
        }
        Component suit = chest.isEmpty() ? Component.translatable("kn8.menu.profile.suit_default") : chest
                .getHoverName();
        MenuStyle.small(g, font, suit, x + 26, y + 12, MenuStyle.TEXT);
        Component note = KN8Data.SUIT.get(BuiltInRegistries.ITEM.getKey(chest.getItem()), true)
                .<Component>map(def -> Component.translatable("kn8.menu.profile.suit_stats", Math.round(def.armor()),
                        Math.round(def.heatResistance() * 100)))
                .orElse(Component.translatable("kn8.menu.profile.suit_none"));
        MenuStyle.small(g, font, note, x + 26, y + 21, MenuStyle.TEXT_DIM);

        // Armas (dados sincronizados); a da mao fica destacada.
        int wy = y + 38;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.weapons"), x, wy, MenuStyle.TEXT);
        List<WeaponDef> weapons = KN8Data.WEAPON.client().values().stream()
                .sorted(Comparator.comparing(def -> def.item().getPath())).toList();
        int slot = Math.min(24, (w - 4) / Math.max(1, weapons.size()) - 2);
        ItemStack held = player.getMainHandItem();
        for (int i = 0; i < weapons.size(); i++) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(weapons.get(i).item()));
            int sx = x + i * (slot + 2);
            MenuStyle.card(g, sx, wy + 9, slot, slot, held.is(stack.getItem()));
            MenuStyle.item(g, stack, sx + 2, wy + 11, slot - 4);
        }

        // Estatisticas.
        int st = wy + 9 + slot + 8;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.profile.stats"), x, st, MenuStyle.TEXT);
        CareerView.Stats stats = MenuData.career().stats();
        stat(g, font, x, st + 10, w, "kn8.menu.profile.kills", String.valueOf(Math.max(stats.kaijuKills(),
                MenuData.totalKaijuKills())));
        stat(g, font, x, st + 24, w, "kn8.menu.profile.dismantled", String.valueOf(stats.dismantled()));
        stat(g, font, x, st + 38, w, "kn8.menu.profile.missions_done", String.valueOf(stats.missionsDone()));
        stat(g, font, x, st + 52, w, "kn8.menu.profile.invasions", String.valueOf(stats.invasionsDefended()));
    }

    private static void stat(GuiGraphics g, Font font, int x, int y, int w, String key, String value) {
        MenuStyle.card(g, x, y, w, 12, false);
        MenuStyle.small(g, font, Component.translatable(key), x + 4, y + 3, MenuStyle.TEXT_DIM);
        Component text = Component.literal(value);
        g.drawString(font, text, x + w - 4 - font.width(text), y + 2, MenuStyle.TEXT, false);
    }
}
