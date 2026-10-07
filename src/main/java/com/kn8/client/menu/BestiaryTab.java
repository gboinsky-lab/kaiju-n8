package com.kn8.client.menu;

import java.util.ArrayList;
import java.util.List;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.DismantleDef;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * Aba BESTIARIO: todas as especies de kaiju. Registrada = ja abatida pelo jogador (estatistica vanilla; no criativo
 * todas aparecem). Nao registrada = silhueta escura e "???". Mostra modelo 3D girando, categoria, ameaca, onde fica
 * o nucleo e as fraquezas do JSON, e os materiais do desmonte (tabela sincronizada desde a 0.2).
 */
final class BestiaryTab implements MenuTab {

    private static final int ROW_HEIGHT = 18;

    private int selected;
    private int scroll;
    private int listX;
    private int listY;
    private int listW;
    private int visible;
    private float spin;

    @Override
    public void tick() {
        MenuData.tickPreviews();
        spin += 0.04F;
    }

    private static List<EntityType<KaijuEntity>> species() {
        List<EntityType<KaijuEntity>> list = new ArrayList<>();
        KN8Entities.KAIJU.forEach(holder -> list.add(holder.get()));
        return list;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY,
            float partialTick) {
        List<EntityType<KaijuEntity>> all = species();
        int registered = (int) all.stream().filter(MenuData::registered).count();
        int listPanelW = Math.max(110, w * 3 / 10);
        MenuStyle.panel(g, x, y, w, h, MenuStyle.PANEL, MenuStyle.OUTLINE);
        MenuStyle.title(g, font, Component.translatable("kn8.menu.tab.bestiary"), x + 6, y + 6);
        Component record = Component.translatable("kn8.menu.bestiary.record", registered, all.size());
        MenuStyle.small(g, font, record, x + w - 8 - Math.round(font.width(record) * MenuStyle.SMALL), y + 7,
                MenuStyle.TEXT_DIM);

        listX = x + 6;
        listY = y + 20;
        listW = listPanelW - 6;
        visible = Math.max(1, (y + h - 6 - listY) / (ROW_HEIGHT + 2));
        scroll = Math.max(0, Math.min(scroll, all.size() - visible));
        selected = Math.min(selected, all.size() - 1);
        for (int i = 0; i < visible && i + scroll < all.size(); i++) {
            EntityType<KaijuEntity> type = all.get(i + scroll);
            int ry = listY + i * (ROW_HEIGHT + 2);
            boolean known = MenuData.registered(type);
            MenuStyle.card(g, listX, ry, listW, ROW_HEIGHT, i + scroll == selected);
            g.fill(listX + 2, ry + 2, listX + ROW_HEIGHT - 2, ry + ROW_HEIGHT - 2, 0xFF0A1220);
            MenuData.renderEntity(g, MenuData.preview(type), listX + 2, ry + 2, listX + ROW_HEIGHT - 2,
                    ry + ROW_HEIGHT - 2, 0.6F, 0.0F, !known);
            ResourceLocation id = MenuData.id(type);
            Component name = known ? MenuData.speciesName(id) : Component.literal("???");
            MenuStyle.small(g, font, name, listX + ROW_HEIGHT + 2, ry + 3, MenuStyle.TEXT);
            Component sub = known ? MenuData.kaijuDef(id).map(MenuData::kaijuClass).orElse(Component.empty())
                    : Component.translatable("kn8.menu.bestiary.unregistered");
            MenuStyle.scaled(g, font, sub, listX + ROW_HEIGHT + 2, ry + 10, MenuStyle.TEXT_DIM, 0.6F);
        }
        if (all.isEmpty()) {
            return;
        }
        drawEntry(g, font, all.get(selected), x + listPanelW + 4, y + 18, w - listPanelW - 10, h - 24);
    }

    private void drawEntry(GuiGraphics g, Font font, EntityType<KaijuEntity> type, int x, int y, int w, int h) {
        ResourceLocation id = MenuData.id(type);
        boolean known = MenuData.registered(type);
        KaijuDef def = MenuData.kaijuDef(id).orElse(null);
        int modelW = w * 9 / 20;
        g.fill(x, y, x + modelW, y + h, 0xC0060A12);
        MenuData.renderEntity(g, MenuData.preview(type), x + 2, y + 2, x + modelW - 2, y + h - 2,
                (float) Math.sin(spin) * 1.2F, -0.15F, !known);

        int tx = x + modelW + 6;
        int tw = w - modelW - 6;
        if (!known || def == null) {
            MenuStyle.scaled(g, font, Component.literal("???"), tx, y + 4, MenuStyle.TEXT, 1.5F);
            MenuStyle.wrapped(g, font, Component.translatable("kn8.menu.bestiary.locked"), tx, y + 22, tw,
                    MenuStyle.TEXT_DIM, 4);
            return;
        }
        MenuData.Threat threat = MenuData.threat(def);
        MenuStyle.scaled(g, font, MenuData.speciesName(id), tx, y + 2, MenuStyle.TEXT, 1.25F);
        int bx = tx + MenuStyle.badge(g, font, MenuData.kaijuClass(def), tx, y + 15, MenuStyle.ACCENT) + 3;
        MenuStyle.badge(g, font, Component.translatable("kn8.menu.bestiary.threat", threat.label()), bx, y + 15,
                threat.color);
        int ty = y + 27;
        ty += MenuStyle.wrapped(g, font, Component.translatable("kn8.bestiary." + id.getPath() + ".desc"), tx, ty,
                tw, MenuStyle.TEXT_DIM, 4) + 3;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.bestiary.stats", String.format("%.1f",
                def.fortitude()), String.format("%.1f", def.dimensions().height()), MenuData.kills(type)), tx, ty,
                MenuStyle.TEXT);
        ty += 11;
        // 0.6: ataques da especie, pelo nome (kn8.ability.<id>), numa linha.
        MutableComponent attacks = Component.empty();
        for (int i = 0; i < def.abilities().size(); i++) {
            if (i > 0) {
                attacks.append(", ");
            }
            attacks.append(Component.translatable("kn8.ability." + def.abilities().get(i).getPath()));
        }
        ty += MenuStyle.wrapped(g, font, Component.translatable("kn8.menu.bestiary.attacks", attacks), tx, ty, tw,
                MenuStyle.TEXT_DIM, 2) + 3;

        // Pontos fracos: nucleo (parte marcada no JSON), quando expoe, fraquezas.
        MenuStyle.small(g, font, Component.translatable("kn8.menu.bestiary.weak_points"), tx, ty,
                MenuStyle.YELLOW);
        ty += 8;
        String corePart = def.parts().stream().filter(KaijuDef.Part::core).map(KaijuDef.Part::name).findFirst()
                .orElse("torso");
        ty = bullet(g, font, Component.translatable("kn8.menu.bestiary.core_at",
                Component.translatable("kn8.part." + corePart)), tx, ty);
        for (String trigger : def.core().exposedOn()) {
            ty = bullet(g, font, Component.translatable("kn8.menu.bestiary.exposed." + trigger), tx, ty);
        }
        for (String weakness : def.weaknesses()) {
            ty = bullet(g, font, Component.translatable("kn8.weakness." + weakness), tx, ty);
        }

        // Materiais do desmonte.
        ty += 3;
        MenuStyle.small(g, font, Component.translatable("kn8.menu.bestiary.materials"), tx, ty, MenuStyle.TEXT);
        ty += 9;
        DismantleDef dismantle = def.dismantle().flatMap(key -> KN8Data.DISMANTLE.get(key, true)).orElse(null);
        if (dismantle == null) {
            MenuStyle.small(g, font, Component.literal("-"), tx, ty, MenuStyle.TEXT_DIM);
            return;
        }
        int cell = Math.min(34, tw / Math.max(1, dismantle.drops().size()));
        for (int i = 0; i < dismantle.drops().size(); i++) {
            DismantleDef.Drop drop = dismantle.drops().get(i);
            int cx = tx + i * cell;
            MenuStyle.card(g, cx, ty, cell - 2, 22, false);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(drop.item()));
            MenuStyle.item(g, stack, cx + (cell - 2 - 12) / 2.0F, ty + 2, 12);
            String amount = drop.minCount() == drop.maxCount() ? "x" + drop.minCount()
                    : "x" + drop.minCount() + "-" + drop.maxCount();
            MenuStyle.scaled(g, font, Component.literal(amount), cx + 2, ty + 15, MenuStyle.TEXT_DIM, 0.55F);
            if (drop.condition() != DismantleDef.Condition.ALWAYS) {
                g.fill(cx + cell - 6, ty + 1, cx + cell - 3, ty + 4,
                        drop.condition() == DismantleDef.Condition.CORE_INTACT ? MenuStyle.GREEN : MenuStyle.RED);
            }
        }
        MenuStyle.scaled(g, font, Component.translatable("kn8.menu.bestiary.core_legend"), tx, ty + 25,
                MenuStyle.TEXT_DIM, 0.55F);
    }

    private static int bullet(GuiGraphics g, Font font, Component text, int x, int y) {
        g.fill(x, y + 2, x + 3, y + 5, MenuStyle.YELLOW);
        MenuStyle.small(g, font, text, x + 6, y, MenuStyle.TEXT);
        return y + 8;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < visible; i++) {
            if (MenuStyle.inside(mouseX, mouseY, listX, listY + i * (ROW_HEIGHT + 2), listW, ROW_HEIGHT)) {
                selected = Math.min(i + scroll, species().size() - 1);
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
