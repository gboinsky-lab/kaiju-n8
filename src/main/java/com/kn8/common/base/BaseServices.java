package com.kn8.common.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SquadService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Instalacoes da base (PM-8): o oficial de missoes ({@link DefenseOfficerEntity}) e a estacao de preparacao do
 * esquadrao ({@link SquadStationBlock}). O servidor decide tudo; o cliente so abre o quadro de missoes que ja existe.
 */
public final class BaseServices {

    /** Tick do jogo a partir do qual o jogador pode usar uma estacao de novo (dados persistentes do jogador). */
    private static final String TAG_STATION_READY = "kn8_station_ready";

    private BaseServices() {
    }

    // --- oficial de missoes --------------------------------------------------------------------------------------

    /** Briefing da proxima patente no chat e o quadro de missoes do oficial no menu. */
    public static void talk(ServerPlayer player, DefenseOfficerEntity officer) {
        player.sendSystemMessage(Component.translatable("kn8.officer.greeting", officer.getDisplayName())
                .withStyle(ChatFormatting.AQUA));
        Optional<Map.Entry<ResourceLocation, RankDef>> next = CareerService.nextRank(player);
        CareerData data = CareerService.data(player);
        if (next.isEmpty()) {
            player.sendSystemMessage(Component.translatable("kn8.officer.top").withStyle(ChatFormatting.GRAY));
        } else {
            RankDef rank = next.get().getValue();
            player.sendSystemMessage(Component.translatable("kn8.officer.next",
                    Component.translatable("kn8.rank." + next.get().getKey().getPath()), data.merit(),
                    rank.meritRequired()).withStyle(ChatFormatting.GRAY));
            rank.promotionMission().ifPresent(exam -> player.sendSystemMessage(Component.translatable(
                    data.completed().contains(exam) ? "kn8.officer.exam_done" : "kn8.officer.exam",
                    MissionService.name(exam)).withStyle(ChatFormatting.YELLOW)));
        }
        officer.playSound(SoundEvents.VILLAGER_AMBIENT, 0.6F, 0.8F);
        PacketDistributor.sendToPlayer(player, new OpenMissionBoardS2C(officer.giver().toString()));
    }

    // --- estacao de preparacao -----------------------------------------------------------------------------------

    /** Resultado do uso (GameTests): membros atendidos, curados e balas entregues; quantas faltaram no inventario. */
    public record StationResult(int members, int healed, int rounds, int missing) {
        public static final StationResult NONE = new StationResult(0, 0, 0, 0);
    }

    public static StationResult useStation(ServerLevel level, BlockPos pos, ServerPlayer player) {
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        if (!player.isCreative() && now < data.getLong(TAG_STATION_READY)) {
            long seconds = (data.getLong(TAG_STATION_READY) - now + 19) / 20;
            player.displayClientMessage(Component.translatable("kn8.station.cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return StationResult.NONE;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        double radius = ServerConfig.STATION_RADIUS.get();
        List<? extends SoldierEntity> members = SquadService.members(player).stream()
                .filter(soldier -> soldier.distanceToSqr(center) <= radius * radius).toList();
        if (members.isEmpty()) {
            player.displayClientMessage(Component.translatable("kn8.station.no_squad", (int) radius)
                    .withStyle(ChatFormatting.RED), true);
            return StationResult.NONE;
        }
        int healed = 0;
        int rounds = 0;
        int missing = 0;
        for (SoldierEntity soldier : members) {
            if (soldier.getHealth() < soldier.getMaxHealth()) {
                soldier.setHealth(soldier.getMaxHealth());
                healed++;
            }
            // So efeitos ruins (lentidao, veneno...); os bons de quem tem escalada/aura ficam.
            List<MobEffectInstance> harmful = soldier.getActiveEffects().stream()
                    .filter(effect -> !effect.getEffect().value().isBeneficial()).toList();
            harmful.forEach(effect -> soldier.removeEffect(effect.getEffect()));
            int need = soldier.missingRounds();
            if (need <= 0) {
                continue;
            }
            Optional<Item> ammo = soldier.ammoItem().map(BuiltInRegistries.ITEM::get);
            int paid = player.isCreative() ? need : ammo.map(item -> take(player, item, need)).orElse(0);
            soldier.resupply(paid);
            rounds += paid;
            missing += need - paid;
        }
        data.putLong(TAG_STATION_READY, now + ServerConfig.STATION_COOLDOWN_TICKS.get());
        player.sendSystemMessage(Component.translatable("kn8.station.done", members.size(), healed, rounds)
                .withStyle(ChatFormatting.GREEN));
        if (missing > 0) {
            player.sendSystemMessage(Component.translatable("kn8.station.short", missing)
                    .withStyle(ChatFormatting.YELLOW));
        }
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4F, 1.6F);
        return new StationResult(members.size(), healed, rounds, missing);
    }

    /** Tira ate {@code amount} itens do inventario do jogador; devolve quantos tirou. */
    private static int take(ServerPlayer player, Item item, int amount) {
        int taken = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (taken >= amount) {
                break;
            }
            if (stack.is(item)) {
                int part = Math.min(stack.getCount(), amount - taken);
                stack.shrink(part);
                taken += part;
            }
        }
        return taken;
    }
}
