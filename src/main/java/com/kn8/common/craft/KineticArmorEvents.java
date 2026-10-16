package com.kn8.common.craft;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Armadura cinetica (Miguel, 2026-10-16): o traje da Forca de Defesa absorve a forca da queda. Soldados (comuns e
 * especiais, que sempre vestem o traje) e o jogador com um traje vestido ({@link SuitEvents#worn}) nao tomam dano de
 * queda. A queda continua contando para o resto (som, parkour), so o dano e zerado.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class KineticArmorEvents {

    private KineticArmorEvents() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!ServerConfig.SPEC.isLoaded() || event.getEntity().level().isClientSide()) {
            return;
        }
        boolean soldier = event.getEntity() instanceof SoldierEntity && ServerConfig.SOLDIER_NO_FALL_DAMAGE.get();
        boolean suited = event.getEntity() instanceof Player && ServerConfig.KINETIC_SUIT_NO_FALL_DAMAGE.get()
                && SuitEvents.worn(event.getEntity()).isPresent();
        if (soldier || suited) {
            event.setDamageMultiplier(0.0F);
        }
    }
}
