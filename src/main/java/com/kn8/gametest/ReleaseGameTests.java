// src/main/java/com/kn8/gametest/ReleaseGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.registry.KN8Items;
import com.kn8.core.power.BodyStat;
import com.kn8.core.power.HeatStage;
import com.kn8.core.power.TalentParams;
import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.5.0-A (Biblioteca v21 Prioridade 1, decisoes do Miguel): talento sorteado uma vez na faixa do
 * config, Release so com traje e pela tecla, excesso que aquece ate o maximo sem baixar a %, e atributos do corpo.
 * O FakePlayer nao recebe o tick de jogador: o teste chama {@link PowerService#tick} na mao. Um perfil por teste
 * (o mesmo perfil devolve a mesma instancia e os testes rodam em paralelo).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReleaseGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private ReleaseGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name, boolean suit) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_release:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_r_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4, 1, 4)));
        player.setItemSlot(EquipmentSlot.CHEST, suit ? new ItemStack(KN8Items.MK1.get()) : ItemStack.EMPTY);
        return player;
    }

    private static void ticks(FakePlayer player, int count) {
        for (int i = 0; i < count; i++) {
            PowerService.tick(player);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void talentIsRolledOnceInsideTheConfiguredRange(GameTestHelper helper) {
        FakePlayer player = player(helper, "talent", false);
        ticks(player, 1);
        TalentParams talent = ServerConfig.talentParams();
        int limit = PowerService.limit(player);
        boolean rare = PowerService.view(player).talentRare();
        boolean inside = rare ? limit >= talent.rareMin() && limit <= talent.rareMax()
                : limit >= talent.commonMin() && limit <= talent.commonMax();
        helper.assertTrue(inside, "Limite inicial fora da faixa do talento: " + limit + (rare ? " (raro)" : ""));
        ticks(player, 5);
        helper.assertTrue(PowerService.limit(player) == limit, "O talento so pode ser sorteado uma vez");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void releaseOnlyRisesWithTheSuitAndTheKey(GameTestHelper helper) {
        FakePlayer bare = player(helper, "bare", false);
        FakePlayer suited = player(helper, "suited", true);
        ticks(bare, 1);
        ticks(suited, 1);
        PowerService.releaseInput(bare, 1);
        PowerService.releaseInput(suited, 1);
        // 20/s no config: 10 ticks = +10%.
        ticks(bare, 10);
        ticks(suited, 10);
        helper.assertTrue(PowerService.effectiveRelease(bare) == 0, "Sem traje nao ha Release: "
                + PowerService.effectiveRelease(bare));
        helper.assertTrue(PowerService.activeRelease(suited) == 10, "Com traje a tecla deveria subir 10%: "
                + PowerService.activeRelease(suited));
        PowerService.releaseInput(suited, 0);
        ticks(suited, 10);
        helper.assertTrue(PowerService.activeRelease(suited) == 10, "Soltou a tecla: a % fica onde parou");
        PowerService.releaseInput(suited, -1);
        ticks(suited, 10);
        helper.assertTrue(PowerService.activeRelease(suited) == 0, "Shift + tecla desce ate 0");
        // Tirar o traje desliga.
        PowerService.setActive(suited, 30);
        suited.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ticks(suited, 1);
        helper.assertTrue(PowerService.effectiveRelease(suited) == 0, "Tirou o traje: Release desliga");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void overTheLimitHeatsUpButNeverDropsTheRelease(GameTestHelper helper) {
        FakePlayer player = player(helper, "overload", true);
        ticks(player, 1);
        PowerService.setTrainedRelease(player, 10);
        PowerService.setActive(player, 60);
        PowerService.setHeat(player, 95);
        ticks(player, 60);
        helper.assertTrue(PowerService.heatStage(player) == HeatStage.PANIC, "Com 50 acima do limite o calor"
                + " deveria chegar ao maximo: " + PowerService.heatStage(player));
        helper.assertTrue(PowerService.activeRelease(player) == 60, "No calor maximo a % nao pode cair (v21): "
                + PowerService.activeRelease(player));
        helper.assertTrue(PowerService.excess(player) == 50, "Excesso = ativa - limite");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bodyAttributesLevelUpAndGiveBonuses(GameTestHelper helper) {
        FakePlayer player = player(helper, "body", false);
        ticks(player, 1);
        float before = PowerService.strengthMultiplier(player);
        // xpBase 20 + xpPerLevel 4: 50 de XP = nivel 2.
        PowerService.addBodyXp(player, BodyStat.STRENGTH, 50);
        helper.assertTrue(PowerService.bodyLevel(player, BodyStat.STRENGTH) == 2, "50 de XP deveria dar o nivel 2: "
                + PowerService.bodyLevel(player, BodyStat.STRENGTH));
        helper.assertTrue(PowerService.strengthMultiplier(player) > before, "Forca maior, mais dano");
        PowerService.setBodyLevel(player, BodyStat.RESISTANCE, 50);
        PowerService.setBodyLevel(player, BodyStat.AGILITY, 50);
        helper.assertTrue(PowerService.resistanceFactor(player) < 1.0F, "Resistencia reduz o dano recebido");
        helper.assertTrue(PowerService.agilityCostFactor(player) < 1.0, "Agilidade barateia esquiva e dash");
        // Fracoes acumulam (velocidade ganha 0,2 por bloco).
        for (int i = 0; i < 10; i++) {
            PowerService.addBodyXp(player, BodyStat.SPEED, 0.2);
        }
        helper.assertTrue(PowerService.view(player).speed() == 0, "2 de XP ainda nao sobe nivel");
        helper.succeed();
    }
}
