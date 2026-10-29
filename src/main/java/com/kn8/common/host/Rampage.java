package com.kn8.common.host;

import javax.annotation.Nullable;

import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * T-3: o hospedeiro passou do tempo limite na forma e perdeu o controle (Miguel 2026-10-24: "perca o controle do corpo
 * e saia atacando todos em volta"). Tudo no servidor: escolhe o vivo mais perto ({@code [kaijuForm] rampageRadius}:
 * jogadores fora do criativo, soldados, civis, kaiju, animais), vira a camera do jogador para ele, corre ate ele e
 * soca com os golpes da forma (o mesmo caminho de dano do jogador, no tick de impacto do JSON). O cliente do dono trava
 * a propria entrada de movimento e vira a camera para o alvo publicado ({@code kn8:rampage_target}); os pedidos de
 * combate e a tecla da forma sao ignorados no servidor.
 */
final class Rampage {

    private static final String TAG_TARGET = "kn8_rampage_target";
    private static final String TAG_PUNCHES = "kn8_rampage_punches";
    private static final String TAG_LAST_X = "kn8_rampage_x";
    private static final String TAG_LAST_Z = "kn8_rampage_z";
    private static final String TAG_STUCK = "kn8_rampage_stuck";
    private static final String TAG_SKIP = "kn8_rampage_skip";
    /** Andou menos que isso entre duas viradas: esta travado (pula). */
    private static final double STUCK_MOVE = 0.15;
    /** Travado por tanto tempo: desiste deste alvo e vai atras de outro. */
    private static final int GIVE_UP_TICKS = 40;
    private static final int RETARGET_TICKS = 10;
    private static final int STEER_TICKS = 4;
    private static final double REACH_SLACK = 0.5;
    private static final double JUMP_SPEED = 0.55;
    /** A cada quantos socos sai um golpe pesado. */
    private static final int HEAVY_EVERY = 3;

    private Rampage() {
    }

    static void tick(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }
        LivingEntity target = target(player);
        if (target == null) {
            return;
        }
        if (player.tickCount % STEER_TICKS != 0) {
            return;
        }
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        // O golpe sai do olhar do servidor: vira ja aqui. O cliente do dono vira a propria camera para o mesmo alvo
        // (kn8:rampage_target), sem teleporte: teleportar a cada passo fazia o servidor ignorar o movimento do cliente
        // ate a confirmacao e o jogador congelava.
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        double reach = CombatService.heldWeapon(player).map(weapon -> (double) weapon.reach()).orElse(3.0);
        double edge = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
        CompoundTag tag = player.getPersistentData();
        if (edge > reach - REACH_SLACK) {
            // Travado (parede, carcaca): pula; travado demais, larga o alvo e procura outro.
            double moved = Math.hypot(player.getX() - tag.getDouble(TAG_LAST_X),
                    player.getZ() - tag.getDouble(TAG_LAST_Z));
            int stuck = moved < STUCK_MOVE ? tag.getInt(TAG_STUCK) + STEER_TICKS : 0;
            tag.putDouble(TAG_LAST_X, player.getX());
            tag.putDouble(TAG_LAST_Z, player.getZ());
            tag.putInt(TAG_STUCK, stuck);
            if (stuck >= GIVE_UP_TICKS) {
                tag.putInt(TAG_SKIP, target.getId());
                tag.putInt(TAG_TARGET, -1);
                tag.putInt(TAG_STUCK, 0);
                return;
            }
            Vec3 flat = new Vec3(to.x, 0, to.z).normalize().scale(ServerConfig.FORM_RAMPAGE_SPEED.get());
            double up = stuck > 0 && player.onGround() ? JUMP_SPEED : player.getDeltaMovement().y;
            player.setDeltaMovement(flat.x, up, flat.z);
            player.hurtMarked = true;
            return;
        }
        tag.putInt(TAG_STUCK, 0);
        int punches = tag.getInt(TAG_PUNCHES);
        boolean heavy = punches % HEAVY_EVERY == HEAVY_EVERY - 1;
        if (CombatService.attack(player, heavy ? "heavy" : "light")) {
            tag.putInt(TAG_PUNCHES, punches + 1);
        }
    }

    /** Alvo atual (id guardado nos dados do jogador) ou o vivo mais perto, reavaliado de tempos em tempos. */
    @Nullable
    private static LivingEntity target(ServerPlayer player) {
        Entity current = player.level().getEntity(player.getPersistentData().getInt(TAG_TARGET));
        boolean valid = current instanceof LivingEntity living && valid(player, living)
                && living.distanceTo(player) <= ServerConfig.FORM_RAMPAGE_RADIUS.get() * 1.5;
        if (valid && player.tickCount % RETARGET_TICKS != 0) {
            return (LivingEntity) current;
        }
        double radius = ServerConfig.FORM_RAMPAGE_RADIUS.get();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        int skip = player.getPersistentData().getInt(TAG_SKIP);
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), living -> valid(player, living) && living.getId() != skip)) {
            double distance = living.distanceToSqr(player);
            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        int id = best == null ? -1 : best.getId();
        player.getPersistentData().putInt(TAG_TARGET, id);
        if (player.getData(KN8Attachments.RAMPAGE_TARGET.get()) != id) {
            player.setData(KN8Attachments.RAMPAGE_TARGET.get(), id);
        }
        if (best == null) {
            // Ninguem mais por perto: o alvo largado volta a valer na proxima procura.
            player.getPersistentData().remove(TAG_SKIP);
        }
        return best;
    }

    static boolean valid(ServerPlayer player, LivingEntity living) {
        if (living == player || !living.isAlive() || living.isInvulnerable() || living instanceof ArmorStand
                || LarvaService.isHostLarva(living)) {
            return false;
        }
        return !(living instanceof Player other) || !(other.isSpectator() || other.isCreative());
    }
}
