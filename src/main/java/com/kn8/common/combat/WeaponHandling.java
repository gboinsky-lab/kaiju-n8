// src/main/java/com/kn8/common/combat/WeaponHandling.java
package com.kn8.common.combat;

import java.util.List;
import java.util.Optional;

import com.kn8.common.anim.AnimationBridge;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.data.def.WeaponProfileDef;
import com.kn8.common.registry.KN8DataComponents;
import com.kn8.core.combat.ActionTimeline;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Manuseio da arma pelo perfil da familia dela (0.5.0-D, {@code weapon_profile/<id>.json}): saque ao trocar de arma,
 * guarda, pente e recarga por etapas. Ocupa a mesma {@code ActionTimeline} dos golpes, entao nada sai durante o
 * saque ou a recarga; o servidor decide e a animacao vai para todos pela ponte de sempre.
 *
 * <p>0.5.0-D2 (Miguel: sem municao infinita): os tiros ficam no proprio item da arma ({@code kn8:rounds}; sem o
 * componente = pente cheio de fabrica). A recarga troca o pente da arma pelo pente carregado da mochila com mais
 * tiros ({@link MagazineItem}); o pente que saiu volta para a mochila com o que sobrou. Criativo nao gasta.</p>
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

    /**
     * Arma de par ({@code hands: dual}) so vale as tecnicas com uma arma de par na outra mao (0.5.0-D3, Miguel: o
     * jogador luta como o Hoshina NPC, uma espada em cada mao). Arma de uma ou duas maos sempre passa.
     */
    public static boolean dualReady(Player player, WeaponDef weapon, boolean clientSide) {
        if (profile(weapon, clientSide).map(WeaponProfileDef::hands).orElse(null) != WeaponProfileDef.Hands.DUAL) {
            return true;
        }
        return WeaponIndex.find(player.getOffhandItem(), clientSide).flatMap(off -> profile(off, clientSide))
                .map(off -> off.hands() == WeaponProfileDef.Hands.DUAL).orElse(false);
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

    /** Tiros no pente que esta dentro da arma (item na mao). */
    public static int gunRounds(ItemStack gun, WeaponProfileDef.Reload reload) {
        return gun.getOrDefault(KN8DataComponents.ROUNDS.get(), reload.magazine());
    }

    /** Capacidade do pente avulso: a do perfil que aponta para ele (0 = nao e pente de nenhuma arma). */
    public static int magazineCapacity(Item item, boolean clientSide) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return KN8Data.WEAPON_PROFILE.forSide(clientSide).values().stream().flatMap(def -> def.reload().stream())
                .filter(reload -> reload.magazineItem().filter(id::equals).isPresent())
                .mapToInt(WeaponProfileDef.Reload::magazine).findFirst().orElse(0);
    }

    /** Pente carregado da mochila com mais tiros (vazio se nao ha). */
    private static Optional<ItemStack> bestMagazine(Player player, WeaponProfileDef.Reload reload) {
        if (reload.magazineItem().isEmpty()) {
            return Optional.empty();
        }
        Item magazine = BuiltInRegistries.ITEM.get(reload.magazineItem().get());
        ItemStack best = null;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(magazine) && MagazineItem.rounds(stack) > 0
                    && (best == null || MagazineItem.rounds(stack) > MagazineItem.rounds(best))) {
                best = stack;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Tiros nos pentes carregados da mochila (reserva da HUD; serve nos dois lados). */
    public static int spareRounds(Player player, WeaponProfileDef.Reload reload) {
        if (reload.magazineItem().isEmpty()) {
            return 0;
        }
        Item magazine = BuiltInRegistries.ITEM.get(reload.magazineItem().get());
        return player.getInventory().items.stream().filter(stack -> stack.is(magazine))
                .mapToInt(MagazineItem::rounds).sum();
    }

    /** Carrega o pente com a municao da mochila (criativo: enche). Devolve quantos tiros entraram. */
    public static int loadMagazine(Player player, ItemStack magazine, int capacity) {
        int missing = capacity - MagazineItem.rounds(magazine);
        if (missing <= 0) {
            return 0;
        }
        if (player.getAbilities().instabuild) {
            MagazineItem.setRounds(magazine, capacity);
            return missing;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(magazine.getItem());
        Optional<Item> ammo = KN8Data.WEAPON_PROFILE.forSide(false).values().stream()
                .flatMap(def -> def.reload().stream())
                .filter(reload -> reload.magazineItem().filter(id::equals).isPresent())
                .flatMap(reload -> reload.ammoItem().stream()).findFirst().map(BuiltInRegistries.ITEM::get);
        if (ammo.isEmpty()) {
            return 0;
        }
        int loaded = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (loaded >= missing) {
                break;
            }
            if (stack.is(ammo.get())) {
                int take = Math.min(stack.getCount(), missing - loaded);
                stack.shrink(take);
                loaded += take;
            }
        }
        MagazineItem.setRounds(magazine, MagazineItem.rounds(magazine) + loaded);
        return loaded;
    }

    /**
     * Antes de um tiro: {@code null} = pode atirar; pente vazio comeca a recarga ({@code RELOADING}) ou avisa que
     * nao ha pente carregado na mochila ({@code DENIED_NO_MAGAZINE}). Arma sem pente sempre pode atirar.
     */
    static CombatResult canFire(ServerPlayer player, CombatState state, WeaponDef weapon, long now) {
        Optional<WeaponProfileDef.Reload> reload = profile(weapon, false).flatMap(WeaponProfileDef::reload);
        if (reload.isEmpty() || gunRounds(player.getMainHandItem(), reload.get()) > 0) {
            return null;
        }
        return startReload(player, state, weapon, now) ? CombatResult.RELOADING : CombatResult.DENIED_NO_MAGAZINE;
    }

    /** Depois de um tiro aceito: gasta um do pente; vazio, a recarga comeca quando o tiro terminar. */
    static void spendRound(ServerPlayer player, CombatState state, WeaponDef weapon, long now) {
        profile(weapon, false).flatMap(WeaponProfileDef::reload).ifPresent(reload -> {
            ItemStack gun = player.getMainHandItem();
            int left = Math.max(0, gunRounds(gun, reload) - 1);
            gun.set(KN8DataComponents.ROUNDS.get(), left);
            state.pendingReload = left == 0;
            sendAmmo(player, state, Optional.of(weapon), now);
        });
    }

    /** Tiros no pente da arma na mao (-1 = sem pente); publico para os GameTests e comandos. */
    public static int rounds(ServerPlayer player) {
        return CombatService.heldWeapon(player).flatMap(weapon -> profile(weapon, false)
                .flatMap(WeaponProfileDef::reload).map(reload -> gunRounds(player.getMainHandItem(), reload)))
                .orElse(-1);
    }

    /** Ajusta o pente da arma na mao (GameTests). */
    public static void setRounds(ServerPlayer player, int rounds) {
        player.getMainHandItem().set(KN8DataComponents.ROUNDS.get(), rounds);
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
                || gunRounds(player.getMainHandItem(), reload.get()) >= reload.get().magazine()) {
            return false;
        }
        if (!player.getAbilities().instabuild && bestMagazine(player, reload.get()).isEmpty()) {
            player.displayClientMessage(Component.translatable("kn8.hud.combat.denied_no_magazine"), true);
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
            swapMagazine(player, state.reloadProfile);
            state.reloadStartTick = CombatState.NEVER;
            state.reloadProfile = null;
            sendAmmo(player, state, CombatService.heldWeapon(player), now);
        } else if (state.reloadStage == 0 && elapsed == 0) {
            sendAmmo(player, state, CombatService.heldWeapon(player), now);
        }
    }

    /** Fim da recarga: o pente carregado entra na arma e o que saiu volta para a mochila com o que sobrou. */
    private static void swapMagazine(ServerPlayer player, WeaponProfileDef.Reload reload) {
        ItemStack gun = player.getMainHandItem();
        if (player.getAbilities().instabuild) {
            gun.set(KN8DataComponents.ROUNDS.get(), reload.magazine());
            return;
        }
        bestMagazine(player, reload).ifPresent(magazine -> {
            int incoming = MagazineItem.rounds(magazine);
            MagazineItem.setRounds(magazine, gunRounds(gun, reload));
            gun.set(KN8DataComponents.ROUNDS.get(), incoming);
        });
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
        PacketDistributor.sendToPlayer(player, new AmmoS2C(gunRounds(player.getMainHandItem(), reload.get()),
                reload.get().magazine(), left, total));
    }

    private static void play(ServerPlayer player, ResourceLocation id) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id)
                .orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS,
                HANDLING_VOLUME, 1.0F);
    }
}
