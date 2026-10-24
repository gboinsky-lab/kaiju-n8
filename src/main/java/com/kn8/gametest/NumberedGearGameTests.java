package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedGearDef;
import com.kn8.common.numbered.NumberedGearService;
import com.kn8.common.registry.KN8Items;
import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-11A: armas numeradas do jogador. Sem patente, pesquisa ou limite pessoal a arma nao ativa (golpe
 * recusado, nada gasto); com eles ativa. O dominio sobe com o dano em kaiju, leva o dano de 70% a 100% e e salvo na
 * carreira. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NumberedGearGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation NUMBERS2 = KN8Constants.id("numbers2");
    private static final ResourceLocation THEORY = KN8Constants.id("numbered_weapon_theory");

    private NumberedGearGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_numbered:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_numbered_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerService.setRank(player, KN8Constants.id("candidato"));
        CareerService.data(player).research().clear();
        CareerService.data(player).mastery().clear();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.NUMBERS2_GAUNTLETS.get()));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void numberedWeaponNeedsRankResearchAndLimit(GameTestHelper helper) {
        FakePlayer player = player(helper, "rules");
        NumberedGearDef def = KN8Data.NUMBERED_GEAR.get(NUMBERS2, false).orElseThrow();
        helper.assertTrue(NumberedGearService.blocked(player, def).orElse("").equals("rank"), "Sem patente");
        helper.assertFalse(CombatService.attack(player, "light"), "Golpe recusado sem os requisitos");
        CareerService.setRank(player, KN8Constants.id("lider_pelotao"));
        helper.assertTrue(NumberedGearService.blocked(player, def).orElse("").equals("research"), "Sem pesquisa");
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 10);
        helper.assertTrue(NumberedGearService.blocked(player, def).orElse("").equals("limit"), "Limite baixo");
        PowerService.setTrainedRelease(player, 40);
        helper.assertTrue(NumberedGearService.blocked(player, def).isEmpty(), "Com tudo, a arma ativa");
        helper.assertTrue(CombatService.attack(player, "light"), "Golpe aceito com os requisitos");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void masteryRaisesDamageAndSaves(GameTestHelper helper) {
        FakePlayer player = player(helper, "mastery");
        CareerService.setRank(player, KN8Constants.id("lider_pelotao"));
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 40);
        helper.assertTrue(Math.abs(NumberedGearService.damageFactor(player) - 0.7F) < 1.0E-4F,
                "Sem dominio a arma tira 70%");
        NumberedGearService.onKaijuDamaged(player, 1000.0F);
        int xp = NumberedGearService.xp(player, NUMBERS2);
        helper.assertTrue(xp == 1000, "1000 de dano = 1000 XP, veio " + xp);
        float mid = NumberedGearService.damageFactor(player);
        helper.assertTrue(mid > 0.7F && mid < 1.0F, "Dominio no meio do caminho: " + mid);
        NumberedGearService.onKaijuDamaged(player, 5000.0F);
        helper.assertTrue(Math.abs(NumberedGearService.damageFactor(player) - 1.0F) < 1.0E-4F,
                "Dominio completo = dano cheio");
        helper.assertTrue(NumberedGearService.extraSpecialHeat(player) == 0.0, "Dominada nao esquenta a mais");
        Tag saved = CareerData.CODEC.encodeStart(NbtOps.INSTANCE, CareerService.data(player)).getOrThrow();
        CareerData loaded = CareerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.mastery().getOrDefault(NUMBERS2, 0) == 6000, "Dominio salvo na carreira");
        // Arma comum: sem fator nem XP de dominio.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.SWORD.get()));
        helper.assertTrue(NumberedGearService.damageFactor(player) == 1.0F, "Arma comum sem mudanca");
        helper.succeed();
    }
}
