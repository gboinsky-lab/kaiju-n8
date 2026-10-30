package com.kn8.common.training;

import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.host.KaijuFormService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.core.power.BodyStat;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Treino de combate contra soldado (TREINO, Miguel 2026-10-30: "combate contra soldados"). Shift + clique de mao vazia
 * num soldado comum comeca o treino: os dois podem se ferir (o mesmo caminho do duelo de teste,
 * {@link SoldierEntity#isDuelFoe}), ninguem morre (o golpe para em 1 de vida) e quem cair a
 * {@code [exercise] sparYieldFraction} da vida perde. O soldado bate {@code sparPlayerDamageFactor} do dano normal.
 * Bater no parceiro treina a forca, apanhar treina a resistencia (esquiva/parry ja treinam a agilidade) e vencer da
 * XP em todos os atributos. Termina por tempo, distancia, kaiju por perto (luta de verdade) ou outro Shift + clique.
 * Estado no soldado ({@link #TAG}: UUID do jogador e tick do inicio) e no jogador (UUID do soldado), nos dados
 * persistentes; o soldado decide o fim, o jogador so confere se o par ainda existe.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class SparringService {

    /** Nos dados persistentes do soldado: parceiro, inicio e quando pode treinar de novo. */
    public static final String TAG = "kn8_spar";
    private static final String START = "kn8_spar_start";
    private static final String READY = "kn8_spar_ready";
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int TICKS_PER_SECOND = 20;

    private SparringService() {
    }

    /** Os dois estao num treino entre si (jogador e soldado, em qualquer ordem). */
    public static boolean isPartner(Entity first, Entity second) {
        return first instanceof ServerPlayer player && second instanceof SoldierEntity soldier
                && partnerOf(soldier) != null && partnerOf(soldier).equals(player.getUUID())
                || second instanceof ServerPlayer && first instanceof SoldierEntity && isPartner(second, first);
    }

    /** UUID do jogador com quem o soldado treina, ou null. */
    public static UUID partnerOf(SoldierEntity soldier) {
        CompoundTag data = soldier.getPersistentData();
        return data.hasUUID(TAG) ? data.getUUID(TAG) : null;
    }

    /** So soldado comum: os especiais tem tecnicas e trocas de forma que nao cabem num treino [SUPOSICAO]. */
    public static boolean canSpar(SoldierEntity soldier) {
        return !(soldier instanceof HoshinaEntity) && soldier.canJoinSquad();
    }

    /** Shift + clique de mao vazia: comeca o treino com o soldado ou, se ja treinam, encerra. */
    public static void toggle(ServerPlayer player, SoldierEntity soldier) {
        if (isPartner(player, soldier)) {
            end(soldier, player, "stopped", false);
            return;
        }
        long now = soldier.level().getGameTime();
        String refusal = null;
        if (!canSpar(soldier)) {
            refusal = "special";
        } else if (player.getPersistentData().hasUUID(TAG) || partnerOf(soldier) != null
                || soldier.duelOpponent().isPresent()) {
            refusal = "busy";
        } else if (KaijuFormService.isTransformed(player)) {
            refusal = "form";
        } else if (soldier.getTarget() instanceof KaijuEntity || kaijuNear(soldier)) {
            refusal = "kaiju";
        } else if (soldier.getPersistentData().contains(READY) && now < soldier.getPersistentData().getLong(READY)) {
            refusal = "tired";
        }
        if (refusal != null) {
            player.displayClientMessage(Component.translatable("kn8.spar.refused." + refusal)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        soldier.getPersistentData().putUUID(TAG, player.getUUID());
        soldier.getPersistentData().putLong(START, now);
        player.getPersistentData().putUUID(TAG, soldier.getUUID());
        soldier.setTarget(player);
        player.sendSystemMessage(Component.translatable("kn8.spar.start", soldier.getDisplayName(),
                ServerConfig.SPAR_MAX_TICKS.get() / TICKS_PER_SECOND,
                Math.round(ServerConfig.SPAR_YIELD_FRACTION.get() * 100.0)).withStyle(ChatFormatting.AQUA));
    }

    /**
     * Chamado pelo goal de alvo do soldado em treino: devolve o parceiro (alvo) ou null, encerrando o treino se ele
     * saiu, foi longe, passou do tempo ou um kaiju chegou perto.
     */
    public static LivingEntity tickSoldier(SoldierEntity soldier) {
        UUID partner = partnerOf(soldier);
        if (partner == null || !(soldier.level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(partner);
        if (player == null || player.level() != level || !player.isAlive() || player.isSpectator()) {
            end(soldier, player, "gone", false);
            return null;
        }
        if (soldier.tickCount % CHECK_INTERVAL_TICKS == 0) {
            long started = soldier.getPersistentData().getLong(START);
            if (soldier.distanceTo(player) > ServerConfig.SPAR_MAX_DISTANCE.get()) {
                end(soldier, player, "far", false);
                return null;
            }
            if (level.getGameTime() - started > ServerConfig.SPAR_MAX_TICKS.get()) {
                end(soldier, player, "timeout", false);
                return null;
            }
            if (kaijuNear(soldier) || KaijuFormService.isTransformed(player)) {
                end(soldier, player, "interrupted", false);
                return null;
            }
        }
        return player;
    }

    /** Fim do treino: tira as marcas dos dois, cura o soldado e conta ao jogador (vitoria da o bonus). */
    private static void end(SoldierEntity soldier, ServerPlayer player, String reason, boolean playerWon) {
        soldier.getPersistentData().remove(TAG);
        soldier.getPersistentData().remove(START);
        soldier.getPersistentData().putLong(READY, soldier.level().getGameTime()
                + ServerConfig.SPAR_COOLDOWN_TICKS.get());
        soldier.setTarget(null);
        soldier.setHealth(soldier.getMaxHealth());
        if (player == null) {
            return;
        }
        player.getPersistentData().remove(TAG);
        if (playerWon) {
            int xp = ServerConfig.SPAR_WIN_XP.get();
            for (BodyStat stat : BodyStat.values()) {
                PowerService.addBodyXp(player, stat, xp);
            }
            player.sendSystemMessage(Component.translatable("kn8.spar.win", soldier.getDisplayName(), xp)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("kn8.spar.end." + reason, soldier.getDisplayName())
                    .withStyle(reason.equals("lose") ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
    }

    private static boolean kaijuNear(SoldierEntity soldier) {
        double range = ServerConfig.SPAR_KAIJU_RANGE.get();
        return range > 0.0 && !soldier.level().getEntitiesOfClass(KaijuEntity.class,
                soldier.getBoundingBox().inflate(range), KaijuEntity::isAlive).isEmpty();
    }

    /** Ninguem morre no treino: o golpe para em 1 de vida, e o soldado bate mais fraco no jogador. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();
        if (!ServerConfig.SPEC.isLoaded() || attacker == null || !isPartner(attacker, victim)) {
            return;
        }
        float damage = event.getNewDamage();
        if (victim instanceof ServerPlayer) {
            damage *= ServerConfig.SPAR_PLAYER_DAMAGE_FACTOR.get().floatValue();
        }
        event.setNewDamage(Math.max(0.0F, Math.min(damage, victim.getHealth() - 1.0F)));
    }

    /** XP do treino e quem caiu ate o limite perde. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();
        if (!ServerConfig.SPEC.isLoaded() || attacker == null || event.getNewDamage() <= 0.0F
                || !isPartner(attacker, victim)) {
            return;
        }
        boolean yielded = victim.getHealth() <= victim.getMaxHealth() * ServerConfig.SPAR_YIELD_FRACTION.get();
        if (attacker instanceof ServerPlayer player && victim instanceof SoldierEntity soldier) {
            if (event.getSource().getDirectEntity() == player) {
                PowerService.addBodyXp(player, BodyStat.STRENGTH,
                        event.getNewDamage() * ServerConfig.SPAR_STRENGTH_XP_PER_DAMAGE.get());
            }
            if (yielded) {
                end(soldier, player, "win", true);
            }
        } else if (victim instanceof ServerPlayer player && attacker instanceof SoldierEntity soldier) {
            PowerService.addBodyXp(player, BodyStat.RESISTANCE,
                    event.getNewDamage() * ServerConfig.SPAR_RESISTANCE_XP_PER_DAMAGE.get());
            if (yielded) {
                end(soldier, player, "lose", false);
            }
        }
    }

    /** Marca velha no jogador (o soldado sumiu ou ja encerrou): some sem aviso. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % TICKS_PER_SECOND != 0
                || !player.getPersistentData().hasUUID(TAG) || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        Entity soldier = player.serverLevel().getEntity(player.getPersistentData().getUUID(TAG));
        if (!(soldier instanceof SoldierEntity partner) || !isPartner(player, partner)) {
            player.getPersistentData().remove(TAG);
        }
    }
}
