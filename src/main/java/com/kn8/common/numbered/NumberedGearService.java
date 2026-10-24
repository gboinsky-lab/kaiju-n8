package com.kn8.common.numbered;

import java.util.Map;
import java.util.Optional;

import com.kn8.common.attribute.PowerService;
import com.kn8.common.career.CareerService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedGearDef;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Compatibilidade e dominio dos equipamentos numerados do jogador (PM-11, Plano Mestre secao 11). O servidor decide:
 * a arma numerada so ativa (golpe, tiro, especial) com a patente, a pesquisa e o limite pessoal de Release do
 * {@code numbered_gear/*.json}; sem eles nada e gasto e o motivo vai para a barra. Com eles, o dominio (XP pelo dano
 * em kaiju com a propria arma) leva o dano de {@code effect_start} a 1,0, tira o calor extra do especial e, no
 * Numbers 1, alarga a esquiva perfeita. Nada some do inventario e as armas antigas continuam iguais.
 */
public final class NumberedGearService {

    private NumberedGearService() {
    }

    /** Equipamento numerado do item (o id do registro e o lado decidem; {@code clientSide} para o menu). */
    public static Optional<Map.Entry<ResourceLocation, NumberedGearDef>> of(ItemStack stack, boolean clientSide) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return KN8Data.NUMBERED_GEAR.forSide(clientSide).entrySet().stream()
                .filter(entry -> entry.getValue().item().equals(item)).findFirst();
    }

    private static Optional<Map.Entry<ResourceLocation, NumberedGearDef>> held(ServerPlayer player) {
        return of(player.getMainHandItem(), false);
    }

    /** Motivo da recusa ({@code rank}, {@code research}, {@code limit}) ou vazio se o jogador pode usar. */
    public static Optional<String> blocked(ServerPlayer player, NumberedGearDef def) {
        if (player.isCreative()) {
            return Optional.empty();
        }
        if (def.rank().filter(rank -> !CareerService.hasRank(player, rank)).isPresent()) {
            return Optional.of("rank");
        }
        if (def.research().filter(id -> !CareerService.data(player).research().contains(id)).isPresent()) {
            return Optional.of("research");
        }
        if (PowerService.limit(player) < def.minLimit()) {
            return Optional.of("limit");
        }
        return Optional.empty();
    }

    /**
     * Confere a arma na mao antes de um golpe/especial: falso (com o motivo na barra) quando e numerada e o jogador
     * ainda nao cumpre os requisitos.
     */
    public static boolean allowed(ServerPlayer player) {
        Optional<Map.Entry<ResourceLocation, NumberedGearDef>> gear = held(player);
        if (gear.isEmpty()) {
            return true;
        }
        Optional<String> reason = blocked(player, gear.get().getValue());
        reason.ifPresent(key -> player.displayClientMessage(Component.translatable("kn8.numbered.blocked." + key,
                requirement(gear.get().getValue(), key)).withStyle(ChatFormatting.RED), true));
        return reason.isEmpty();
    }

    private static Object requirement(NumberedGearDef def, String key) {
        return switch (key) {
            case "rank" -> Component.translatable("kn8.rank." + def.rank().map(ResourceLocation::getPath).orElse(""));
            case "research" -> Component.translatable("kn8.research.name." + def.research()
                    .map(ResourceLocation::getPath).orElse(""));
            default -> def.minLimit();
        };
    }

    public static int xp(ServerPlayer player, ResourceLocation gear) {
        return CareerService.data(player).mastery().getOrDefault(gear, 0);
    }

    /** Fracao do dano da arma numerada na mao pelo dominio (1,0 sem arma numerada ou com dominio completo). */
    public static float damageFactor(ServerPlayer player) {
        return held(player).map(entry -> {
            NumberedGearDef def = entry.getValue();
            double progress = def.progress(xp(player, entry.getKey()));
            return (float) (def.effectStart() + (1.0 - def.effectStart()) * progress);
        }).orElse(1.0F);
    }

    /** Calor extra do especial (instabilidade de quem ainda nao domina a arma). */
    public static double extraSpecialHeat(ServerPlayer player) {
        return held(player).map(entry -> entry.getValue().instabilityHeat()
                * (1.0 - entry.getValue().progress(xp(player, entry.getKey())))).orElse(0.0);
    }

    /** Ticks a mais da esquiva perfeita pelo dominio (Numbers 1). */
    public static int perfectDodgeBonus(ServerPlayer player) {
        return held(player).map(entry -> (int) Math.round(entry.getValue().perfectDodgeBonusTicks()
                * entry.getValue().progress(xp(player, entry.getKey())))).orElse(0);
    }

    /** Dano causado em kaiju com a arma numerada na mao vira XP de dominio; subir de nivel avisa. */
    public static void onKaijuDamaged(ServerPlayer player, float damage) {
        held(player).ifPresent(entry -> {
            NumberedGearDef def = entry.getValue();
            int gain = (int) Math.round(damage * def.xpPerDamage());
            if (gain <= 0 || blocked(player, def).isPresent()) {
                return;
            }
            int before = xp(player, entry.getKey());
            int after = before + gain;
            CareerService.data(player).mastery().put(entry.getKey(), after);
            if (def.level(after) > def.level(before)) {
                player.sendSystemMessage(Component.translatable("kn8.numbered.level_up", def.number(),
                        def.level(after), def.maxLevel()).withStyle(ChatFormatting.AQUA));
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.6F, 1.2F);
            }
            CareerService.changed(player);
        });
    }
}
