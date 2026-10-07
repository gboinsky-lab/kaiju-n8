// src/main/java/com/kn8/gametest/AuraGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.registry.KN8Attachments;
import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
        int full = PowerService.effectiveRelease(player);
        player.setHealth(0.1F);
        int low = PowerService.effectiveRelease(player);
        player.setHealth(player.getMaxHealth());
        helper.assertTrue(full == 30, "Com vida cheia a % deveria ser a treinada (30), veio " + full);
        helper.assertTrue(low > full + 10, "Com a vida quase no fim a % deveria subir uns 15 pontos, veio " + low);
        helper.succeed();
    }

    @GameTest(template = "empty_9x7x9")
    public static void auraComesFromOverrideOrDefault(GameTestHelper helper) {
        FakePlayer player = player(helper, "override");
        helper.assertTrue(KN8Data.AURA.get(KN8Constants.id("violet_lightning"), false).isPresent(),
                "aura/violet_lightning.json deveria carregar");
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.empty());
        helper.assertTrue(PowerService.auraOf(player).equals(KN8Attachments.DEFAULT_AURA),
                "Sem traje e sem comando a aura deveria ser a padrao");
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.of(KN8Constants.id("violet_lightning")));
        helper.assertTrue(PowerService.auraOf(player).equals(KN8Constants.id("violet_lightning")),
                "A aura do comando deveria valer por cima da padrao");
        player.setData(KN8Attachments.AURA_OVERRIDE, Optional.empty());
        helper.succeed();
    }
}
