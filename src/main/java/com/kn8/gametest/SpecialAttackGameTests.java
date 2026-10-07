// src/main/java/com/kn8/gametest/SpecialAttackGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.core.kaiju.KaijuState;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.5: o ataque especial do machado (Golpe Sismico, {@code special} do axe.json) acerta todos na area a
 * frente de uma vez (kaiju e mob comum), atordoa o Yoju, poupa o soldado aliado, gasta stamina e entra em recarga.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialAttackGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final Vec3 PLAYER_POS = new Vec3(4.5, 1.0, 7.0);
    private static final float FACING_NORTH = 180.0F;
    private static final int SETTLE_TICKS = 2;
    /** impact_tick do special do machado (16) com folga. */
    private static final int IMPACT_MARGIN = 20;

    private SpecialAttackGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_special:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_s" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        Vec3 position = helper.absoluteVec(PLAYER_POS);
        player.moveTo(position.x, position.y, position.z, FACING_NORTH, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.AXE.get()));
        CombatService.setBlocking(player, false);
        PowerService.setCapOverride(player, 100);
        PowerService.setTrainedRelease(player, 0);
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void seismicCleaveHitsEveryoneInFrontAndSparesAllies(GameTestHelper helper) {
        FakePlayer player = player(helper, "cleave");
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 3));
        spider.setNoAi(true);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 5));
        zombie.setNoAi(true);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 5));
        soldier.setNoAi(true);
        float[] before = new float[3];
        double[] stamina = new double[1];
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            before[0] = spider.getHealth();
            before[1] = zombie.getHealth();
            before[2] = soldier.getHealth();
            helper.assertTrue(CombatService.special(player), "O especial do machado deveria comecar");
            stamina[0] = PowerService.view(player).stamina();
            helper.assertTrue(stamina[0] <= 100 - 35 + 0.5, "O especial deveria gastar a stamina do JSON (35)");
            helper.assertTrue(CombatService.specialCooldown(player) > 0, "O especial deveria entrar em recarga");
        });
        helper.runAfterDelay(SETTLE_TICKS + IMPACT_MARGIN, () -> {
            helper.assertTrue(spider.getHealth() < before[0], "A aranha na area deveria levar dano");
            helper.assertTrue(zombie.getHealth() < before[1], "O zumbi na area deveria levar dano");
            helper.assertTrue(soldier.getHealth() >= before[2], "O soldado aliado nao deveria levar dano");
            helper.assertTrue(spider.state() == KaijuState.STAGGER, "Yoju atingido deveria ficar atordoado");
            PowerService.setStamina(player, 100);
            helper.assertTrue(!CombatService.special(player), "Durante a recarga o especial deveria ser negado");
            spider.discard();
            zombie.discard();
            soldier.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void weaponWithoutSpecialIsDenied(GameTestHelper helper) {
        FakePlayer player = player(helper, "nospecial");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.COMBAT_KNIFE.get()));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(!CombatService.special(player), "A faca nao tem ataque especial");
            helper.succeed();
        });
    }
}
