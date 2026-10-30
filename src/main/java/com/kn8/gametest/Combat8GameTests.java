package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WeaponDef;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da COMBATE-8 (regressao do combate): o jogador usa, uma a uma, todas as tecnicas de toda arma com mais de
 * uma tecnica (X troca, R usa). Cada uma tem de sair; so as regras dela recusam: o Falling Thunder no chao (vem de
 * cima). Vida baixa e ultimo estagio sao montados antes da tecnica que pede. Os punhos da forma do No. 8 (so
 * transformado) e as armas de fogo ficam de fora.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Combat8GameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final Vec3 PLAYER_POS = new Vec3(4.5, 1.0, 4.5);
    private static final int SETTLE = 2;
    /** Saque ao trocar a arma na mao (o mais longo dos perfis) com folga: o saque cancela o golpe. */
    private static final int DRAW = 25;
    /** Folga depois do fim da tecnica antes da seguinte. */
    private static final int GAP = 6;
    private static final String FORM_FIST = "kaiju_no8_fist";

    private Combat8GameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 6000, batch = "kn8_combate8_all")
    public static void everyPlayerTechniqueFires(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("kn8_combate8:all".getBytes(
                StandardCharsets.UTF_8)), "kn8_c8all");
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        Vec3 at = helper.absoluteVec(PLAYER_POS);
        player.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        player.setGameMode(GameType.CREATIVE);
        PowerService.setCapOverride(player, 100);
        PowerService.setTrainedRelease(player, 40);
        helper.onEachTick(() -> CombatService.tick(player));

        List<WeaponDef> weapons = new ArrayList<>();
        KN8Data.WEAPON.server().forEach((id, weapon) -> {
            if (weapon.allTechniques().size() > 1 && weapon.style() != WeaponDef.Style.FIREARM
                    && !weapon.item().getPath().equals(FORM_FIST)) {
                weapons.add(weapon);
            }
        });
        weapons.sort(Comparator.comparing(weapon -> weapon.item().toString()));
        helper.assertTrue(weapons.size() >= 6, "Armas com tecnicas: " + weapons.size());
        int[] fired = new int[1];
        int time = SETTLE;
        for (WeaponDef weapon : weapons) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(weapon.item()));
            boolean dual = weapon.item().getPath().equals("hoshina_sword");
            helper.runAfterDelay(time, () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                player.setItemInHand(InteractionHand.OFF_HAND, dual ? stack.copy() : ItemStack.EMPTY);
            });
            time += DRAW;
            for (WeaponDef.Special technique : weapon.allTechniques()) {
                helper.runAfterDelay(time, () -> fire(helper, player, weapon, technique, fired));
                time += technique.durationTicks() + GAP;
            }
        }
        int total = weapons.stream().mapToInt(weapon -> weapon.allTechniques().size()).sum();
        helper.runAfterDelay(time, () -> {
            helper.assertTrue(fired[0] == total, "Tecnicas conferidas: " + fired[0] + " de " + total);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.setGameMode(GameType.SURVIVAL);
            PowerService.setActive(player, 0);
            player.setHealth(player.getMaxHealth());
            helper.getLevel().getEntitiesOfClass(SlashProjectile.class,
                    new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48)).forEach(Entity::discard);
            helper.succeed();
        });
    }

    private static void fire(GameTestHelper helper, FakePlayer player, WeaponDef weapon,
            WeaponDef.Special technique, int[] fired) {
        String name = weapon.item().getPath() + "/" + technique.id();
        // Vida baixa (Thunder Emperor) e ultimo estagio (Juni-hitoe) so para a tecnica que pede.
        Float below = weapon.lowHealth().get(technique.id());
        player.setHealth(below == null ? player.getMaxHealth() : player.getMaxHealth() * below * 0.5F);
        WeaponDef.Stage stage = weapon.stages().get(technique.id());
        player.setItemSlot(EquipmentSlot.CHEST, stage == null ? ItemStack.EMPTY : stage.suit()
                .map(suit -> new ItemStack(BuiltInRegistries.ITEM.get(suit))).orElse(ItemStack.EMPTY));
        PowerService.setActive(player, stage != null && stage.fullRelease() ? 40 : 0);
        PowerService.setStamina(player, 100);
        player.setOnGround(true);
        int size = weapon.allTechniques().size();
        for (int i = 0; i < size && !CombatService.selectedTechnique(player, weapon)
                .map(t -> t.id().equals(technique.id())).orElse(false); i++) {
            CombatService.cycleTechnique(player, 1);
        }
        helper.assertTrue(CombatService.selectedTechnique(player, weapon).map(t -> t.id().equals(technique.id()))
                .orElse(false), "X deveria chegar a " + name);
        boolean fromAbove = technique.type() == WeaponDef.SpecialType.DIVE_SLAM;
        boolean result = CombatService.special(player);
        helper.assertTrue(result != fromAbove, fromAbove ? "No chao, " + name + " deveria ser recusado"
                : name + " deveria sair");
        fired[0]++;
    }
}
