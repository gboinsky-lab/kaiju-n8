package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTest da 0.3: dois atacantes no mesmo tick causam os dois danos (a invulnerabilidade vanilla de 10 ticks
 * engolia o segundo golpe: lutar em grupo quase nao ajudava).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GroupDamageGameTests {

    private static final float HIT = 4.0F;
    private static final float TOLERANCE = 0.5F;

    private GroupDamageGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(("kn8_group:" + name)
                .getBytes(StandardCharsets.UTF_8)), "kn8_g" + name));
    }

    @GameTest(template = "empty_9x7x9")
    public static void twoAttackersInTheSameTickBothHit(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 4));
        kaiju.setNoAi(true);
        FakePlayer first = player(helper, "a");
        FakePlayer second = player(helper, "b");
        helper.runAfterDelay(2, () -> {
            float start = kaiju.getHealth();
            kaiju.hurt(kaiju.damageSources().playerAttack(first), HIT);
            float afterFirst = kaiju.getHealth();
            kaiju.hurt(kaiju.damageSources().playerAttack(second), HIT);
            float afterSecond = kaiju.getHealth();
            helper.assertTrue(afterFirst < start - TOLERANCE, "O primeiro golpe deveria causar dano");
            helper.assertTrue(afterSecond < afterFirst - TOLERANCE,
                    "O segundo atacante no mesmo tick tambem deveria causar dano (invulnerabilidade vanilla)");
            kaiju.discard();
            helper.succeed();
        });
    }
}
