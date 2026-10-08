package com.kn8.common.career;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.boss.BossService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.core.power.BodyStat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga carreira, missoes e chefes aos eventos do jogo (game bus, servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class CareerEvents {

    private static final int MISSION_TICK_INTERVAL = 20;

    private CareerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()
                && player.tickCount % MISSION_TICK_INTERVAL == 0) {
            MissionService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CareerService.applyRankEffects(player);
            CareerService.tryPromote(player);
            CareerService.changed(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CareerService.applyRankEffects(player);
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Dano de jogador em kaiju vira XP de treino (golpes nas partes chegam aqui pelo corpo). */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof KaijuEntity && event.getSource().getEntity() instanceof ServerPlayer player
                && ServerConfig.SPEC.isLoaded()) {
            CareerService.onKaijuDamaged(player, event.getNewDamage());
            // 0.5.0: golpe corpo a corpo (o jogador e a entidade direta, nao um projetil) treina a forca.
            if (event.getSource().getDirectEntity() == player) {
                PowerService.addBodyXp(player, BodyStat.STRENGTH,
                        event.getNewDamage() * ServerConfig.BODY_XP_STRENGTH_PER_DAMAGE.get());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!ServerConfig.SPEC.isLoaded()) {
            return;
        }
        if (event.getEntity() instanceof KaijuEntity kaiju && !kaiju.level().isClientSide()) {
            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                KaijuClass kaijuClass = kaiju.def().map(def -> def.kaijuClass()).orElse(KaijuClass.YOJU);
                CareerService.onKaijuKilled(player, kaiju.kaijuId(), kaijuClass);
            }
            if (kaiju.bossState() != null) {
                BossService.onDefeated(kaiju, kaiju.bossState(), event.getSource());
            }
        } else if (event.getEntity() instanceof ServerPlayer player) {
            MissionService.onDeath(player);
        }
    }
}
