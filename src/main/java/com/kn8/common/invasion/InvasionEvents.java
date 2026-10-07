package com.kn8.common.invasion;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo9Entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Liga as invasoes aos eventos do jogo (game bus, servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class InvasionEvents {

    private static final long DAY_TICKS = 24_000L;
    /** Inicio da noite (sol se pondo): hora do sorteio da invasao natural. */
    private static final long NIGHTFALL = 13_000L;

    private InvasionEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        InvasionService.tick(level);
        if (level.getDayTime() % DAY_TICKS == NIGHTFALL) {
            InvasionService.rollNatural(level);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof KaijuEntity kaiju && kaiju.level() instanceof ServerLevel level
                && kaiju.getPersistentData().getBoolean(InvasionService.TAG)) {
            InvasionService.onKaijuDeath(level, kaiju,
                    event.getSource().getEntity() instanceof ServerPlayer player ? player : null);
        }
    }

    /** 0.3: dano de jogador em kaiju (invasao: recompensa por contribuicao; No. 9: quem o fez fugir). */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof KaijuEntity kaiju) || !(kaiju.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (kaiju.getPersistentData().getBoolean(InvasionService.TAG)) {
            InvasionService.onKaijuDamaged(level, kaiju, player, event.getNewDamage());
        }
        if (kaiju instanceof KaijuNo9Entity no9) {
            no9.recordAttacker(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InvasionService.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InvasionService.syncTo(player);
        }
    }
}
