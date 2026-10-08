// src/main/java/com/kn8/gametest/ReleaseGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.core.power.BodyStat;
import com.kn8.core.power.HeatStage;
import com.kn8.core.power.TalentParams;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.5.0-A (Biblioteca v21 Prioridade 1, decisoes do Miguel): talento sorteado uma vez na faixa do
 * config, Release so com traje e pela tecla, excesso que aquece ate o maximo sem baixar a %, teto do golpe comum em
 * kaiju (so o especial mata de uma vez) e atributos do corpo.
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
        // Tempo para subir 10% com a velocidade do config (Miguel: 2% por segundo = 100 ticks).
        int tenPercent = (int) Math.ceil(10 * 20 / ServerConfig.RELEASE_RAISE_PER_SECOND.get());
        ticks(bare, tenPercent);
        ticks(suited, tenPercent);
        helper.assertTrue(PowerService.effectiveRelease(bare) == 0, "Sem traje nao ha Release: "
                + PowerService.effectiveRelease(bare));
        helper.assertTrue(PowerService.activeRelease(suited) == 10, "Com traje a tecla deveria subir 10%: "
                + PowerService.activeRelease(suited));
        PowerService.releaseInput(suited, 0);
        ticks(suited, 10);
        helper.assertTrue(PowerService.activeRelease(suited) == 10, "Soltou a tecla: a % fica onde parou");
        PowerService.releaseInput(suited, -1);
        ticks(suited, 20);
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
        // No maximo 20 acima do limite (Miguel: limite 40 -> ate 60%).
        PowerService.setActive(player, 60);
        helper.assertTrue(PowerService.activeRelease(player) == 30, "Teto = limite + 20: "
                + PowerService.activeRelease(player));
        PowerService.setHeat(player, 95);
        ticks(player, 60);
        helper.assertTrue(PowerService.heatStage(player) == HeatStage.PANIC, "Com 20 acima do limite o calor"
                + " deveria chegar ao maximo: " + PowerService.heatStage(player));
        helper.assertTrue(PowerService.activeRelease(player) == 30, "No calor maximo a % nao pode cair (v21): "
                + PowerService.activeRelease(player));
        helper.assertTrue(PowerService.excess(player) == 20, "Excesso = ativa - limite");
        // Voltou para dentro do limite: fadiga (sem Release e quase parado).
        PowerService.setActive(player, 5);
        ticks(player, 1);
        helper.assertTrue(PowerService.fatigued(player), "Depois de passar do limite vem a fadiga");
        helper.assertTrue(PowerService.activeRelease(player) == 0 && PowerService.effectiveRelease(player) == 0,
                "Na fadiga o Release desliga (nem o desespero conta)");
        PowerService.releaseInput(player, 1);
        ticks(player, 40);
        helper.assertTrue(PowerService.activeRelease(player) == 0, "Na fadiga a tecla nao sobe a %");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regularHitsNeverOneShotAKaijuButSpecialsCan(GameTestHelper helper) {
        FakePlayer player = player(helper, "cap", true);
        KaijuEntity first = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(2, 1, 4));
        KaijuEntity second = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(6, 1, 4));
        first.setNoAi(true);
        second.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            float max = first.getMaxHealth();
            first.hurt(player.damageSources().playerAttack(player), 100_000.0F);
            float lost = max - first.getHealth();
            float cap = max * ServerConfig.MAX_LIGHT_HIT_FRACTION.get().floatValue();
            helper.assertTrue(first.isAlive() && lost <= cap + 0.01F, "Golpe comum tirou " + lost + " de " + max
                    + " (teto " + cap + ")");
            second.hurt(CombatService.specialDamage(player), 100_000.0F);
            helper.assertTrue(!second.isAlive(), "O golpe especial pode matar de uma vez");
            // A carcaca do morto ficaria no mundo e entraria na ressurreicao em massa de outro lote.
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
            helper.getLevel().getEntitiesOfClass(Entity.class, area, entity -> entity instanceof CarcassEntity
                    || entity instanceof KaijuEntity).forEach(Entity::discard);
            helper.succeed();
        });
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
