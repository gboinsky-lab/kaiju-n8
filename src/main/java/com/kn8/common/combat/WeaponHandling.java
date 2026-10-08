// src/main/java/com/kn8/common/combat/WeaponHandling.java
package com.kn8.common.combat;

import java.util.List;
import java.util.Optional;

import com.kn8.common.anim.AnimationBridge;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.data.def.WeaponProfileDef;
import com.kn8.core.combat.ActionTimeline;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Manuseio da arma pelo perfil da familia dela (0.5.0-D, {@code weapon_profile/<id>.json}): saque ao trocar de arma,
 * guarda, pente e recarga por etapas. Ocupa a mesma {@code ActionTimeline} dos golpes, entao nada sai durante o
 * saque ou a recarga; o servidor decide e a animacao vai para todos pela ponte de sempre.
 */
public final class WeaponHandling {

    static final String DRAW = "draw";
    static final String RELOAD = "reload";
    private static final float HANDLING_VOLUME = 0.7F;

    private WeaponHandling() {
    }

    /** Perfil da arma (servidor ou cliente, pela foto de cada lado). */
    public static Optional<WeaponProfileDef> profile(WeaponDef weapon, boolean clientSide) {
        return weapon.profile().flatMap(id -> KN8Data.WEAPON_PROFILE.get(id, clientSide));
    }

    /** Todo tick do jogador: saque quando a arma na mao muda e etapas da recarga em andamento. */
    static void tick(ServerPlayer player, CombatState state, long now) {
        ResourceLocation held = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem());
        if (!held.equals(state.lastHeldItem)) {
            boolean first = state.lastHeldItem == null;
            state.lastHeldItem = held;
            onSwitch(player, state, now, first);
        }
        if (state.reloadStartTick != CombatState.NEVER && state.reloadProfile != null) {
            tickReload(player, state, now);
        } else if (state.pendingReload && !state.timeline.isActive(now)) {
            state.pendingReload = false;
            CombatService.heldWeapon(player).ifPresent(weapon -> startReload(player, state, weapon, now));
        }
    }

    private static void onSwitch(ServerPlayer player, CombatState state, long now, boolean first) {
        // Trocar de arma no meio da recarga ou de um golpe cancela os dois (o golpe nao sai com a arma guardada).
        if (state.timeline.isActive(now)) {
            state.timeline.cancel();
            state.currentWeapon = null;
        }
        state.reloadStartTick = CombatState.NEVER;
        state.reloadProfile = null;
        state.pendingReload = false;
        Optional<WeaponDef> weapon = CombatService.heldWeapon(player);
        sendAmmo(player, state, weapon, now);
        if (first || weapon.isEmpty()) {
            return;
        }
        Optional<WeaponProfileDef> profile = profile(weapon.get(), false);
        int ticks = profile.map(def -> def.draw().ticks()).orElse(0);
        if (ticks <= 0) {
            return;
        }
        state.timeline.tryStart(now, DRAW, ticks, ActionTimeline.NO_IMPACT);
        AnimationBridge.playPlayer(player, AnimationBridge.profileAction(weapon.get().profile().get(), DRAW));
        profile.get().draw().sound().ifPresent(sound -> play(player, sound));
    }

    /** Guarda (bloqueio) com a animacao do perfil; sem perfil, a guarda generica. */
    static ResourceLocation guardAnimation(Optional<WeaponDef> weapon) {
        return weapon.flatMap(WeaponDef::profile).map(id -> AnimationBridge.profileAction(id, "guard"))
                .orElse(AnimationBridge.PLAYER_BLOCK);
    }

    /** Tiros que restam no pente (cheio na primeira vez que a arma aparece). */
    static int rounds(CombatState state, WeaponDef weapon, WeaponProfileDef.Reload reload) {
        return state.rounds.getOrDefault(weapon.item(), reload.magazine());
    }

    /**
     * Antes de um tiro: com o pente vazio comeca a recarga e recusa o tiro ({@code false}). Arma sem pente sempre
     * pode atirar.
     */
    static boolean canFire(ServerPlayer player, CombatState state, WeaponDef weapon, long now) {
        Optional<WeaponProfileDef.Reload> reload = profile(weapon, false).flatMap(WeaponProfileDef::reload);
        if (reload.isEmpty() || rounds(state, weapon, reload.get()) > 0) {
            return true;
        }
        startReload(player, state, weapon, now);
        return false;
    }

    /** Depois de um tiro aceito: gasta um do pente; vazio, a recarga comeca quando o tiro terminar. */
    static void spendRound(ServerPlayer player, CombatState state, WeaponDef weapon, long now) {
        profile(weapon, false).flatMap(WeaponProfileDef::reload).ifPresent(reload -> {
            int left = Math.max(0, rounds(state, weapon, reload) - 1);
            state.rounds.put(weapon.item(), left);
            state.pendingReload = left == 0;
            sendAmmo(player, state, Optional.of(weapon), now);
        });
    }

    /** Tiros no pente da arma na mao (-1 = sem pente); publico para os GameTests e comandos. */
    public static int rounds(ServerPlayer player) {
        CombatState state = CombatService.state(player);
        return CombatService.heldWeapon(player).flatMap(weapon -> profile(weapon, false)
                .flatMap(WeaponProfileDef::reload).map(reload -> rounds(state, weapon, reload))).orElse(-1);
    }

    /** Ajusta o pente da arma na mao (GameTests). */
    public static void setRounds(ServerPlayer player, int rounds) {
        CombatService.heldWeapon(player).ifPresent(weapon -> CombatService.state(player).rounds.put(weapon.item(),
                rounds));
    }

    /** Recarga manual (tecla R numa arma de fogo sem especial). */
    static boolean manualReload(ServerPlayer player, CombatState state, WeaponDef weapon, long now) {
        return profile(weapon, false).flatMap(WeaponProfileDef::reload).isPresent()
                && startReload(player, state, weapon, now);
    }

    static boolean startReload(ServerPlayer player, CombatState state, WeaponDef weapon, long now) {
        Optional<WeaponProfileDef> profile = profile(weapon, false);
        Optional<WeaponProfileDef.Reload> reload = profile.flatMap(WeaponProfileDef::reload);
        if (reload.isEmpty() || state.reloadStartTick != CombatState.NEVER
                || rounds(state, weapon, reload.get()) >= reload.get().magazine()) {
            return false;
        }
        if (!state.timeline.tryStart(now, RELOAD, reload.get().totalTicks(), ActionTimeline.NO_IMPACT)) {
            return false;
        }
        state.reloadStartTick = now;
        state.reloadProfile = reload.get();
        state.reloadItem = weapon.item();
        state.reloadStage = -1;
        AnimationBridge.playPlayer(player, AnimationBridge.profileAction(weapon.profile().get(), RELOAD));
        tickReload(player, state, now);
        return true;
    }

    /** Som no comeco de cada etapa; no fim, pente cheio. */
    private static void tickReload(ServerPlayer player, CombatState state, long now) {
        long elapsed = now - state.reloadStartTick;
        List<WeaponProfileDef.Stage> stages = state.reloadProfile.stages();
        int stageStart = 0;
        for (int index = 0; index < stages.size(); index++) {
            if (elapsed >= stageStart && index > state.reloadStage) {
                state.reloadStage = index;
                stages.get(index).sound().ifPresent(sound -> play(player, sound));
            }
            stageStart += stages.get(index).ticks();
        }
        if (elapsed >= state.reloadProfile.totalTicks()) {
            state.rounds.put(state.reloadItem, state.reloadProfile.magazine());
            state.reloadStartTick = CombatState.NEVER;
            state.reloadProfile = null;
            sendAmmo(player, state, CombatService.heldWeapon(player), now);
        } else if (state.reloadStage == 0 && elapsed == 0) {
            sendAmmo(player, state, CombatService.heldWeapon(player), now);
        }
    }

    /** Estado do pente da arma na mao para a HUD do dono. */
    static void sendAmmo(ServerPlayer player, CombatState state, Optional<WeaponDef> weapon, long now) {
        Optional<WeaponProfileDef.Reload> reload = weapon.flatMap(def -> profile(def, false))
                .flatMap(WeaponProfileDef::reload);
        if (reload.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new AmmoS2C(0, 0, 0, 0));
            return;
        }
        int total = reload.get().totalTicks();
        int left = state.reloadStartTick == CombatState.NEVER ? 0
                : (int) Math.max(0, total - (now - state.reloadStartTick));
        PacketDistributor.sendToPlayer(player, new AmmoS2C(rounds(state, weapon.get(), reload.get()),
                reload.get().magazine(), left, total));
    }

    private static void play(ServerPlayer player, ResourceLocation id) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id)
                .orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS,
                HANDLING_VOLUME, 1.0F);
    }
}
