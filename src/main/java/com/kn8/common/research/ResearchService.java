package com.kn8.common.research;

import java.util.Optional;

import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.ResearchDef;
import com.kn8.common.data.def.WorkbenchRecipeDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Blocks;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Pesquisa do laboratorio da base (PM-8C, Plano Mestre secao 10). O cliente so pede "pesquisar X"; aqui o servidor
 * confere o terminal perto, a patente, os pre-requisitos e o material de kaiju no inventario, tira o material e marca
 * o fim pelo tempo de jogo. A pesquisa termina sozinha (onde o jogador estiver), libera os {@code unlocks} na bancada
 * e soma o {@code damage_bonus} contra os kaiju dos {@code targets}. Uma pesquisa por vez.
 */
public final class ResearchService {

    private ResearchService() {
    }

    public enum Result {
        OK, UNKNOWN, NO_TERMINAL, ALREADY_DONE, BUSY, RANK_TOO_LOW, MISSING_PREREQUISITE, MISSING
    }

    public static Result check(ServerPlayer player, ResourceLocation id) {
        Optional<ResearchDef> def = KN8Data.RESEARCH.get(id, false);
        if (def.isEmpty()) {
            return Result.UNKNOWN;
        }
        CareerData data = CareerService.data(player);
        if (data.research().contains(id)) {
            return Result.ALREADY_DONE;
        }
        if (data.researching().isPresent()) {
            return Result.BUSY;
        }
        if (def.get().rank().filter(rank -> !CareerService.hasRank(player, rank)).isPresent()) {
            return Result.RANK_TOO_LOW;
        }
        if (!data.research().containsAll(def.get().requires())) {
            return Result.MISSING_PREREQUISITE;
        }
        if (!nearTerminal(player)) {
            return Result.NO_TERMINAL;
        }
        if (!player.getAbilities().instabuild) {
            for (WorkbenchRecipeDef.Ingredient ingredient : def.get().cost()) {
                if (WorkbenchService.count(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item()))
                        < ingredient.count()) {
                    return Result.MISSING;
                }
            }
        }
        return Result.OK;
    }

    public static Result start(ServerPlayer player, ResourceLocation id) {
        Result result = check(player, id);
        if (result != Result.OK) {
            player.displayClientMessage(Component.translatable("kn8.research.refused." + result.name()
                    .toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.RED), true);
            return result;
        }
        ResearchDef def = KN8Data.RESEARCH.get(id, false).orElseThrow();
        if (!player.getAbilities().instabuild) {
            for (WorkbenchRecipeDef.Ingredient ingredient : def.cost()) {
                WorkbenchService.remove(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item()),
                        ingredient.count());
            }
        }
        long ticks = duration(def);
        CareerService.data(player).setResearching(new CareerData.Researching(id,
                player.level().getGameTime() + ticks));
        player.sendSystemMessage(Component.translatable("kn8.research.started", name(id),
                formatTime(ticks)).withStyle(ChatFormatting.AQUA));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                0.6F, 1.6F);
        CareerService.changed(player);
        return Result.OK;
    }

    /** Duracao real: a do JSON vezes o {@code [research] durationMultiplier} (minimo 1 tick). */
    public static long duration(ResearchDef def) {
        return Math.max(1L, Math.round(def.durationTicks() * ServerConfig.RESEARCH_DURATION_MULTIPLIER.get()));
    }

    /** Uma vez por segundo (CareerEvents): pesquisa pronta termina e avisa. */
    public static void tick(ServerPlayer player) {
        CareerData data = CareerService.data(player);
        Optional<CareerData.Researching> current = data.researching();
        if (current.isEmpty() || player.level().getGameTime() < current.get().finishTick()) {
            return;
        }
        complete(player, current.get().id());
    }

    /** Termina a pesquisa em andamento agora (tambem pelo comando e nos GameTests). */
    public static void complete(ServerPlayer player, ResourceLocation id) {
        CareerData data = CareerService.data(player);
        data.setResearching(null);
        data.research().add(id);
        player.sendSystemMessage(Component.translatable("kn8.research.done", name(id))
                .withStyle(ChatFormatting.GOLD));
        KN8Data.RESEARCH.get(id, false).ifPresent(def -> {
            for (ResourceLocation unlock : def.unlocks()) {
                Component item = BuiltInRegistries.ITEM.containsKey(unlock)
                        ? BuiltInRegistries.ITEM.get(unlock).getDescription() : Component.literal(unlock.toString());
                player.sendSystemMessage(Component.translatable("kn8.research.unlocked", item)
                        .withStyle(ChatFormatting.GREEN));
            }
            if (def.damageBonus() > 0 && !def.targets().isEmpty()) {
                player.sendSystemMessage(Component.translatable("kn8.research.bonus",
                        Math.round(def.damageBonus() * 100)).withStyle(ChatFormatting.GREEN));
            }
        });
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                0.7F, 1.2F);
        VfxService.play(player.serverLevel(), VfxService.SUIT_RELEASE, player.position(), Vec3.ZERO, 0.6F, 0.0F);
        CareerService.changed(player);
    }

    /** Multiplicador de dano do jogador contra este kaiju pelas pesquisas concluidas (bonus somados). */
    public static float damageFactor(ServerPlayer player, KaijuEntity kaiju) {
        CareerData data = CareerService.data(player);
        if (data.research().isEmpty()) {
            return 1.0F;
        }
        KaijuClass kaijuClass = kaiju.def().map(def -> def.kaijuClass()).orElse(KaijuClass.YOJU);
        double bonus = 0.0;
        for (ResourceLocation id : data.research()) {
            Optional<ResearchDef> def = KN8Data.RESEARCH.get(id, false);
            if (def.isPresent() && def.get().affects(kaiju.kaijuId(), kaijuClass)) {
                bonus += def.get().damageBonus();
            }
        }
        return (float) (1.0 + bonus);
    }

    private static boolean nearTerminal(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int reach = ServerConfig.RESEARCH_TERMINAL_RANGE.get();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach),
                center.offset(reach, reach, reach))) {
            if (player.level().getBlockState(pos).is(KN8Blocks.RESEARCH_TERMINAL.get())) {
                return true;
            }
        }
        return false;
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("kn8.research.name." + id.getPath());
    }

    /** "m:ss" do tempo de jogo. */
    public static String formatTime(long ticks) {
        long seconds = (ticks + 19) / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
