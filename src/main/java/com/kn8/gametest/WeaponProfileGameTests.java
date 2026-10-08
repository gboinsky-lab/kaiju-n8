// src/main/java/com/kn8/gametest/WeaponProfileGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.WeaponHandling;
import com.kn8.common.data.KN8Data;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.5.0-D (Biblioteca v21/v22 Prioridade 3): perfil de cada familia de arma carregado, saque que segura
 * os golpes, pente e recarga por etapas da arma de fogo e soldado com a pose e o pente do perfil. O FakePlayer nao
 * recebe o tick de jogador: o teste chama {@link CombatService#tick} a cada tick. Um perfil por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeaponProfileGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private WeaponProfileGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name, ItemStack held) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_weapon:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_w_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4, 1, 4)));
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void everyWeaponHasALoadedProfile(GameTestHelper helper) {
        KN8Data.WEAPON.server().forEach((id, weapon) -> helper.assertTrue(weapon.profile()
                .filter(profile -> KN8Data.WEAPON_PROFILE.server().containsKey(profile)).isPresent(),
                "Arma sem perfil carregado: " + id));
        helper.assertTrue(KN8Data.WEAPON_PROFILE.server().get(KN8Constants.id("rifle")).reload().isPresent(),
                "O rifle deveria ter pente e recarga");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void drawingANewWeaponHoldsTheAttack(GameTestHelper helper) {
        FakePlayer player = player(helper, "draw", new ItemStack(KN8Items.COMBAT_KNIFE.get()));
        int draw = KN8Data.WEAPON_PROFILE.server().get(KN8Constants.id("sword")).draw().ticks();
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(2, () -> player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(KN8Items.SWORD.get())));
        helper.runAfterDelay(4, () -> helper.assertFalse(CombatService.attack(player, "light"),
                "Durante o saque o golpe nao deveria sair"));
        helper.runAfterDelay(4 + draw + 2, () -> {
            helper.assertTrue(CombatService.attack(player, "light"), "Depois do saque o golpe deveria sair");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void emptyMagazineReloadsInStages(GameTestHelper helper) {
        FakePlayer player = player(helper, "reload", new ItemStack(KN8Items.RIFLE.get()));
        int magazine = KN8Data.WEAPON_PROFILE.server().get(KN8Constants.id("rifle")).reload().get().magazine();
        int reload = KN8Data.WEAPON_PROFILE.server().get(KN8Constants.id("rifle")).reload().get().totalTicks();
        int shot = KN8Data.WEAPON.server().get(KN8Constants.id("rifle")).actions().get("light").durationTicks();
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(WeaponHandling.rounds(player) == magazine, "Pente deveria comecar cheio");
            WeaponHandling.setRounds(player, 1);
            helper.assertTrue(CombatService.attack(player, "light"), "O ultimo tiro do pente deveria sair");
            helper.assertTrue(WeaponHandling.rounds(player) == 0, "O pente deveria ficar vazio");
        });
        // Depois do tiro a recarga comeca sozinha; no meio dela nao atira.
        helper.runAfterDelay(2 + shot + 4, () -> helper.assertFalse(CombatService.attack(player, "light"),
                "Recarregando nao deveria atirar"));
        helper.runAfterDelay(2 + shot + reload + 4, () -> {
            helper.assertTrue(WeaponHandling.rounds(player) == magazine,
                    "Depois das etapas o pente deveria estar cheio, veio " + WeaponHandling.rounds(player));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void soldierUsesTheWeaponProfile(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 4));
        soldier.setNoAi(true);
        soldier.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.PISTOL.get()));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(SoldierEntity.POSE_PISTOL.equals(soldier.armPose()),
                    "A pose deveria vir do perfil da pistola, veio " + soldier.armPose());
            int magazine = KN8Data.WEAPON_PROFILE.server().get(KN8Constants.id("pistol")).reload().get().magazine();
            helper.assertTrue(soldier.rounds() == magazine, "Pente da pistola do soldado deveria estar cheio");
            soldier.discard();
            helper.succeed();
        });
    }
}
