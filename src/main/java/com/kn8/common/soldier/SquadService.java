// src/main/java/com/kn8/common/soldier/SquadService.java
package com.kn8.common.soldier;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.career.CareerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.core.soldier.SquadRules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
        if (order == SquadRules.Order.FOCUS) {
            focus(player, members);
            return;
        }
        for (SoldierEntity soldier : members) {
            soldier.setSquadOrder(order);
            if (order == SquadRules.Order.GUARD) {
                // PM-6: a area protegida e em volta de onde o lider esta ao dar a ordem (a mesma para todos).
                soldier.setSquadPost(player.blockPosition());
            }
        }
        if (order == SquadRules.Order.GUARD) {
            tell(player, "kn8.squad.guard.ok", ChatFormatting.AQUA, members.size(),
                    Math.round(ServerConfig.SQUAD_PROTECT_RADIUS.get()));
            return;
        }
        tell(player, "kn8.squad.ordered", ChatFormatting.AQUA,
                Component.translatable("kn8.menu.squad.order." + order.name().toLowerCase(Locale.ROOT)),
                members.size());
    }

    /**
     * PM-6: "focar" designa o kaiju que o lider esta mirando (a mira passa pela caixa dele ou chega a menos de
     * {@code [squad] focusConeDegrees}); sem kaiju na mira a ordem nao muda (avisa).
     */
    private static void focus(ServerPlayer player, List<? extends SoldierEntity> members) {
        KaijuEntity kaiju = aimedKaiju(player);
        if (kaiju == null) {
            tell(player, "kn8.squad.focus.none", ChatFormatting.RED);
            return;
        }
        AiTuning.logMove(kaiju, "designado pelo lider " + player.getScoreboardName() + " para " + members.size()
                + " soldados");
        for (SoldierEntity soldier : members) {
            soldier.setSquadOrder(SquadRules.Order.FOCUS);
            soldier.setAssignedKaiju(kaiju.getUUID());
            soldier.setTarget(kaiju);
        }
        tell(player, "kn8.squad.focus.ok", ChatFormatting.AQUA, members.size(), kaiju.getDisplayName());
    }

    /** Kaiju na mira do jogador (mais perto do centro da mira; empate, o mais perto). */
    @Nullable
    public static KaijuEntity aimedKaiju(ServerPlayer player) {
        double range = ServerConfig.SQUAD_FOCUS_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        List<KaijuEntity> kaiju = player.serverLevel().getEntitiesOfClass(KaijuEntity.class,
                player.getBoundingBox().inflate(range), KaijuEntity::isAlive);
        double[] angles = new double[kaiju.size()];
        double[] distances = new double[kaiju.size()];
        for (int i = 0; i < kaiju.size(); i++) {
            AABB box = kaiju.get(i).getBoundingBox();
            Vec3 center = box.getCenter();
            distances[i] = eye.distanceTo(center);
            if (box.contains(eye) || box.clip(eye, end).isPresent()) {
                angles[i] = 0.0;
            } else {
                // Angulo ate o ponto da caixa mais perto da linha da mira (o centro de um kaiju de 9 m fica longe da
                // mira mesmo com ela raspando o corpo dele).
                Vec3 along = eye.add(look.scale(distances[i]));
                Vec3 nearest = new Vec3(Mth.clamp(along.x, box.minX, box.maxX), Mth.clamp(along.y, box.minY,
                        box.maxY), Mth.clamp(along.z, box.minZ, box.maxZ));
                Vec3 to = nearest.subtract(eye).normalize();
                angles[i] = Math.toDegrees(Math.acos(Mth.clamp(look.dot(to), -1.0, 1.0)));
            }
        }
        int chosen = SquadRules.pickFocus(angles, distances, ServerConfig.SQUAD_FOCUS_CONE.get());
        return chosen >= 0 ? kaiju.get(chosen) : null;
    }

    /** PM-6: o kaiju do "focar" caiu (ou sumiu): o esquadrao volta a seguir o lider. */
    public static void focusEnded(SoldierEntity soldier) {
        soldier.setSquadOrder(SquadRules.Order.FOLLOW);
        if (soldier.squadLeaderPlayer() instanceof ServerPlayer leader) {
            tell(leader, "kn8.squad.focus.done", ChatFormatting.GREEN);
        }
    }

    private static void tell(ServerPlayer player, String key, ChatFormatting color, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }
}
