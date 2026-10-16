// src/main/java/com/kn8/common/soldier/SquadService.java
package com.kn8.common.soldier;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kn8.common.career.CareerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.core.soldier.SquadRules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Esquadrao do jogador (IA-5, Miguel: "ao chegar certo nivel o jogador possa montar um esquadrao com soldados normais
 * ou ate especiais dependendo da ocasiao"). O tamanho vem da patente ({@code npc_squad_size} do {@code rank/*.json}:
 * Oficial Senior 2, Lider de Pelotao 4, Vice-Capitao 6, Capitao 8); soldado especial so a partir da patente de ordem
 * {@code [squad] specialRankOrder}. O servidor confere tudo (distancia, patente, dono); o cliente so pede.
 */
public final class SquadService {

    private SquadService() {
    }

    /** Membros do esquadrao do jogador carregados no mundo dele. */
    public static List<? extends SoldierEntity> members(ServerPlayer player) {
        return player.serverLevel().getEntities(EntityTypeTest.forClass(SoldierEntity.class),
                soldier -> soldier.isAlive() && soldier.isLedBy(player));
    }

    public static void recruit(ServerPlayer player, int entityId) {
        if (!(player.serverLevel().getEntity(entityId) instanceof SoldierEntity soldier) || !soldier.isAlive()) {
            return;
        }
        double range = ServerConfig.SQUAD_RECRUIT_RANGE.get();
        if (soldier.distanceToSqr(player) > range * range) {
            tell(player, "kn8.squad.too_far", ChatFormatting.RED);
            return;
        }
        if (soldier.isLedBy(player)) {
            tell(player, "kn8.squad.already", ChatFormatting.YELLOW);
            return;
        }
        int size;
        int order;
        if (player.isCreative()) {
            // Criativo: o maior esquadrao das patentes e especiais liberados (como o bestiario completo).
            List<Map.Entry<ResourceLocation, RankDef>> ranks = CareerService.ranks();
            size = ranks.stream().mapToInt(entry -> entry.getValue().npcSquadSize()).max().orElse(0);
            order = Integer.MAX_VALUE;
        } else {
            RankDef rank = CareerService.rank(player).map(Map.Entry::getValue).orElse(null);
            size = rank != null ? rank.npcSquadSize() : 0;
            order = rank != null ? rank.order() : 0;
        }
        boolean taken = soldier.squadLeader().isPresent() || soldier.assignedKaiju() != null;
        SquadRules.Recruit result = SquadRules.canRecruit(size, members(player).size(),
                soldier instanceof HoshinaEntity, order, ServerConfig.SQUAD_SPECIAL_RANK_ORDER.get(), taken);
        if (result != SquadRules.Recruit.OK) {
            tell(player, "kn8.squad.recruit." + result.name().toLowerCase(Locale.ROOT),
                    ChatFormatting.RED, size);
            return;
        }
        soldier.setTarget(null);
        soldier.joinSquad(player.getUUID());
        tell(player, "kn8.squad.recruit.ok", ChatFormatting.GREEN, members(player).size(), size);
    }

    public static void dismiss(ServerPlayer player, int entityId) {
        if (player.serverLevel().getEntity(entityId) instanceof SoldierEntity soldier && soldier.isLedBy(player)) {
            soldier.leaveSquad();
            tell(player, "kn8.squad.dismissed", ChatFormatting.YELLOW);
        }
    }

    public static void order(ServerPlayer player, SquadRules.Order order) {
        List<? extends SoldierEntity> members = members(player);
        if (members.isEmpty()) {
            tell(player, "kn8.squad.empty", ChatFormatting.RED);
            return;
        }
        for (SoldierEntity soldier : members) {
            soldier.setSquadOrder(order);
        }
        tell(player, "kn8.squad.ordered", ChatFormatting.AQUA,
                Component.translatable("kn8.menu.squad.order." + order.name().toLowerCase(Locale.ROOT)),
                members.size());
    }

    private static void tell(ServerPlayer player, String key, ChatFormatting color, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }
}
