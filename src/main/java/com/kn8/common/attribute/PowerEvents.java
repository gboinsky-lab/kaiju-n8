// src/main/java/com/kn8/common/attribute/PowerEvents.java
package com.kn8.common.attribute;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.core.power.HeatStage;
import com.kn8.core.power.PowerMath;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga as regras de poder aos eventos do jogo (game bus, so no servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class PowerEvents {

    private PowerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()) {
            PowerService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerService.reapply(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerService.reapply(player);
        }
    }

    /**
     * Reducao de dano pela % liberada (GDD secao 5) para quem recebe, bonus de Sobrecarga (secao 8) para quem bate,
     * e marca de combate para os dois. O dano do proprio traje superaquecido nao e reduzido.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!ServerConfig.SPEC.isLoaded()) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player && attacker != event.getEntity()) {
            PowerService.markCombat(player);
            if (PowerService.heatStage(player) == HeatStage.OVERLOAD) {
                event.setAmount(event.getAmount() * (1.0F + ServerConfig.OVERLOAD_DAMAGE_BONUS.get().floatValue()));
            }
        }
        if (event.getEntity() instanceof ServerPlayer target && !event.getSource().is(PowerService.SUIT_OVERHEAT)) {
            PowerService.markCombat(target);
            double reduction = PowerMath.damageReduction(PowerService.effectiveRelease(target), PowerService.params());
            event.setAmount((float) (event.getAmount() * (1.0 - reduction)));
        }
    }
}
