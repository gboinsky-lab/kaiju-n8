package com.kn8.client.menu;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.attribute.PowerView;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedGearDef;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.numbered.NumberedGearService;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * PM-11: requisitos e dominio da arma numerada na dica do item (inventario, bancada, criativo), antes de usar: o que
 * falta aparece em vermelho com o mesmo criterio do servidor (patente, pesquisa, limite pessoal), e o dominio mostra o
 * nivel e quanto do dano ja sai. So le o que o cliente ja tem (carreira e poder do proprio jogador).
 */
public final class NumberedTooltip {

    private NumberedTooltip() {
    }

    public static void install() {
        NeoForge.EVENT_BUS.addListener(NumberedTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        LocalPlayer player = MenuData.player();
        Optional<Map.Entry<ResourceLocation, NumberedGearDef>> gear = NumberedGearService.of(event.getItemStack(),
                true);
        if (player == null || gear.isEmpty()) {
            return;
        }
        NumberedGearDef def = gear.get().getValue();
        List<Component> lines = event.getToolTip();
        lines.add(Component.translatable("kn8.numbered.tooltip.title", def.number()).withStyle(ChatFormatting.GOLD));
        def.rank().ifPresent(rank -> lines.add(check(rankOk(rank), Component.translatable("kn8.numbered.tooltip.rank",
                MenuData.rankName(rank)))));
        def.research().ifPresent(id -> lines.add(check(MenuData.career().research().done().contains(id),
                Component.translatable("kn8.numbered.tooltip.research", Component.translatable(
                        "kn8.research.name." + id.getPath())))));
        if (def.minLimit() > 0) {
            PowerView power = player.getData(KN8Attachments.POWER_VIEW);
            lines.add(check(power.trained() >= def.minLimit(), Component.translatable("kn8.numbered.tooltip.limit",
                    def.minLimit(), power.trained())));
        }
        int xp = MenuData.career().research().mastery().getOrDefault(gear.get().getKey(), 0);
        int level = def.level(xp);
        int damage = (int) Math.round(100 * (def.effectStart() + (1.0 - def.effectStart()) * def.progress(xp)));
        Component next = level < def.maxLevel() ? Component.translatable("kn8.numbered.tooltip.next", xp,
                def.mastery().get(level + 1)) : Component.translatable("kn8.numbered.tooltip.mastered");
        lines.add(Component.translatable("kn8.numbered.tooltip.mastery", level, def.maxLevel(), damage)
                .append(" ").append(next).withStyle(ChatFormatting.AQUA));
        if (def.instabilityHeat() > 0 && level < def.maxLevel()) {
            lines.add(Component.translatable("kn8.numbered.tooltip.unstable").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static boolean rankOk(ResourceLocation rank) {
        return KN8Data.RANK.client().entrySet().stream().filter(entry -> entry.getKey().equals(rank))
                .map(Map.Entry::getValue).map(RankDef::order).findFirst()
                .map(order -> MenuData.rankOrder() >= order).orElse(true);
    }

    private static Component check(boolean ok, Component text) {
        return Component.literal(ok ? " ✔ " : " ✘ ").append(text)
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
