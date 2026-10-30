// src/main/java/com/kn8/gametest/AuraGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Items;
import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.5: aura de poder (dados carregados, aura padrao, aura forcada por comando) e a % que sobe sozinha
 * com a vida baixa ({@code power.desperationHealth}/{@code desperationMaxPoints}).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AuraGameTests {

    private AuraGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_aura:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_a" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        PowerService.setCapOverride(player, 100);
        PowerService.setTrainedRelease(player, 30);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    @GameTest(template = "empty_9x7x9")
    public static void lowHealthRaisesRelease(GameTestHelper helper) {
        FakePlayer player = player(helper, "low");
        // 0.5.0: o Release so existe com traje e com a % liberada na tecla.
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(KN8Items.MK1.get()));
        PowerService.setActive(player, 30);
        int full = PowerService.effectiveRelease(player);
        player.setHealth(0.1F);
        int low = PowerService.effectiveRelease(player);
        player.setHealth(player.getMaxHealth());
        helper.assertTrue(full == 30, "Com vida cheia a % deveria ser a liberada (30), veio " + full);
        helper.assertTrue(low > full + 10, "Com a vida quase no fim a % deveria subir uns 15 pontos, veio " + low);
        helper.succeed();
    }

    @GameTest(template = "empty_9x7x9")
    public static void auraComesFromOverrideOrDefault(GameTestHelper helper) {
        FakePlayer player = player(helper, "override");
        helper.assertTrue(KN8Data.AURA.get(KN8Constants.id("violet_lightning"), false).isPresent(),
                "aura/violet_lightning.json deveria carregar");
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.empty());
        // Decisao do Miguel (2026-10-23): sem traje e sem comando, a aura sorteada do jogador (uma player_*), fixa.
        player.setData(KN8Attachments.PLAYER_AURA, "");
        ResourceLocation drawn = PowerService.auraOf(player);
        helper.assertTrue(drawn.getPath().startsWith("player_") && KN8Data.AURA.get(drawn, false).isPresent(),
                "Sem traje e sem comando a aura deveria ser uma das sorteaveis: " + drawn);
        helper.assertTrue(PowerService.auraOf(player).equals(drawn), "A aura sorteada deveria ficar a mesma");
        long pool = KN8Data.AURA.server().keySet().stream().filter(id -> id.getPath().startsWith("player_")).count();
        helper.assertTrue(pool >= 8, "Deveria haver varias auras para sortear, ha " + pool);
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.of(KN8Constants.id("violet_lightning")));
        helper.assertTrue(PowerService.auraOf(player).equals(KN8Constants.id("violet_lightning")),
                "A aura do comando deveria valer por cima da sorteada");
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.empty());
        helper.succeed();
    }

    /** AJUSTES (Miguel 2026-10-30): o jogador escolhe a cor da aura, passando pelas auras de jogador e voltando. */
    @GameTest(template = "empty_9x7x9")
    public static void playerChoosesTheAuraColor(GameTestHelper helper) {
        FakePlayer player = player(helper, "choice");
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.empty());
        ResourceLocation start = PowerService.auraOf(player);
        long pool = KN8Data.AURA.server().keySet().stream().filter(id -> id.getPath().startsWith("player_")).count();
        ResourceLocation next = PowerService.cyclePlayerAura(player, 1);
        helper.assertTrue(!next.equals(start) && next.getPath().startsWith("player_")
                && PowerService.auraOf(player).equals(next), "A proxima cor vale: " + start + " -> " + next);
        helper.assertTrue(PowerService.cyclePlayerAura(player, -1).equals(start), "A anterior volta a de antes");
        ResourceLocation around = start;
        for (int i = 0; i < pool; i++) {
            around = PowerService.cyclePlayerAura(player, 1);
        }
        helper.assertTrue(around.equals(start), "Uma volta inteira passa por todas e volta: " + around);
        helper.succeed();
    }
}
