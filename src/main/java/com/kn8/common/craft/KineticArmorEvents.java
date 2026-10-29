package com.kn8.common.craft;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

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

    /**
     * Miguel (2026-10-29): "ao tomar dano o jogador fica sendo jogado para tras". O empurrao que o jogador leva ao ser
     * ferido e multiplicado por {@code [combat] playerKnockback} (0 = nenhum). So o empurrao do dano: impulsos do
     * proprio jogador (dash, parkour, asas) nao passam por aqui.
     */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!ServerConfig.SPEC.isLoaded() || !(event.getEntity() instanceof Player)) {
            return;
        }
        double factor = ServerConfig.PLAYER_KNOCKBACK.get();
        if (factor <= 0.0) {
            event.setCanceled(true);
        } else if (factor < 1.0) {
            event.setStrength((float) (event.getStrength() * factor));
        }
    }
}
