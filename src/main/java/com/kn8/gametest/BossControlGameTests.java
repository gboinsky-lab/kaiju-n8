package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Etapa 7-C (Miguel, 2026-11-06): chefes resistem cada vez mais a gelo, veneno e atordoamento; No. 15 ilude e
 * confunde; No. 11 puxa; No. 1 preve o golpe repetido e erra contra a finta; Myxogasterocarp se divide e o dano no
 * nucleo interrompe.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossControlGameTests {

    private static final String TEMPLATE = "empty_15x12x15";

    private BossControlGameTests() {
    }

    private static KaijuEntity kaiju(GameTestHelper helper, String species, BlockPos pos) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id(species), helper.absolutePos(pos))
                .orElseThrow();
        kaiju.setNoAi(true);
        return kaiju;
    }

    private static int slowFor(KaijuEntity kaiju) {
        kaiju.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        kaiju.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        return kaiju.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_7c_resist")
    public static void bossesResistRepeatedFreeze(GameTestHelper helper) {
        KaijuEntity boss = kaiju(helper, "kaiju_no13", new BlockPos(4, 1, 7));
        KaijuEntity common = kaiju(helper, "primigenius", new BlockPos(11, 1, 7));
        helper.runAfterDelay(3, () -> {
            int first = slowFor(boss);
            int second = slowFor(boss);
            int third = slowFor(boss);
            helper.assertTrue(first == 100 && second == 75 && third == 50,
                    "Gelo cada vez mais curto no chefe: " + first + ", " + second + ", " + third);
            helper.assertTrue(slowFor(common) == 100 && slowFor(common) == 100, "Kaiju comum sem resistencia");
            boss.poison(4.0F, 80, null);
            helper.assertTrue(boss.controlApplications(1) == 1, "Veneno conta");
            boss.discard();
            common.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "kn8_7c_no15")
    public static void no15IllusionAndMentalAttack(GameTestHelper helper) {
        KaijuEntity no15 = kaiju(helper, "kaiju_no15", new BlockPos(7, 1, 7));
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(2, 1, 2));
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(12, 1, 12));
        husk.setNoAi(true);
        soldier.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(no15.castIllusion() >= 2, "Ilusao pegou o husk e o soldado");
            helper.assertTrue(no15.isIllusioning(husk), "Husk iludido");
            int hits = 0;
            for (int i = 0; i < 40; i++) {
                no15.invulnerableTime = 0;
                if (no15.hurt(no15.damageSources().mobAttack(husk), 1.0F)) {
                    hits++;
                }
            }
            helper.assertTrue(hits > 4 && hits < 36, "Metade dos golpes do iludido acerta a imagem falsa: " + hits);
            soldier.setTarget(husk);
            no15.applyAbilityEffects(soldier, KN8Data.ABILITY.server().get(KN8Constants.id("no15_psychic_link"))
                    .behavior());
            helper.assertTrue(soldier.getTarget() == null && soldier.hasEffect(MobEffects.WEAKNESS),
                    "Ataque mental: larga o alvo e fica fraco");
            no15.discard();
            husk.discard();
            soldier.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_7c_no11")
    public static void no11WaterJetPulls(GameTestHelper helper) {
        KaijuEntity no11 = kaiju(helper, "kaiju_no11", new BlockPos(3, 1, 7));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(12, 1, 7));
        golem.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            no11.applyAbilityEffects(golem, KN8Data.ABILITY.server().get(KN8Constants.id("no11_water_jet"))
                    .behavior());
            Vec3 toward = no11.position().subtract(golem.position());
            helper.assertTrue(golem.getDeltaMovement().dot(toward) > 0.0, "Puxado para o No. 11");
            no11.discard();
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_7c_no1")
    public static void no1ForeseesRepeatsButNotFeints(GameTestHelper helper) {
        KaijuEntity no1 = kaiju(helper, "kaiju_no1", new BlockPos(7, 1, 7));
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(2, 1, 2));
        husk.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            no1.invulnerableTime = 0;
            helper.assertTrue(no1.hurt(no1.damageSources().mobAttack(husk), 2.0F), "Primeiro golpe passa");
            no1.invulnerableTime = 0;
            helper.assertFalse(no1.hurt(no1.damageSources().mobAttack(husk), 2.0F), "Golpe repetido e previsto");
            Snowball ball = new Snowball(helper.getLevel(), husk);
            no1.invulnerableTime = 0;
            helper.assertTrue(no1.hurt(no1.damageSources().thrown(ball, husk), 2.0F), "Finta (a distancia) passa");
            helper.assertTrue(no1.vulnerability() > 1.2F, "Finta deixa o No. 1 vulneravel: " + no1.vulnerability());
            no1.discard();
            husk.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "kn8_7c_split")
    public static void myxogasterocarpSplitsUnlessTheCoreIsHit(GameTestHelper helper) {
        KaijuEntity first = kaiju(helper, "myxogasterocarp", new BlockPos(4, 1, 4));
        KaijuEntity second = kaiju(helper, "myxogasterocarp", new BlockPos(11, 1, 11));
        BlockPos center = helper.absolutePos(new BlockPos(7, 1, 7));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(13, 1, 2));
        golem.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            first.setHealth(first.getMaxHealth() * 0.6F);
            second.setHealth(second.getMaxHealth() * 0.6F);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(first.isSplitting() && second.isSplitting(), "Os dois comecaram a se dividir");
            // Golpe no nucleo do segundo: perde mais que 15% do nucleo.
            java.util.Arrays.stream(second.kaijuParts()).filter(part -> part.isCore()).findFirst().ifPresent(core -> {
                second.invulnerableTime = 0;
                core.hurt(second.damageSources().mobAttack(golem), second.maxCoreHealth() * 0.3F / 3.0F);
            });
        });
        helper.runAfterDelay(80, () -> {
            helper.assertFalse(second.isSplitting(), "Dano no nucleo interrompeu");
            long children = helper.getLevel().getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(
                    center).inflate(24), kaiju -> kaiju.getType() == KN8Entities.PHANEROPLASMODIUM.get()).size();
            helper.assertTrue(children == 2, "So o primeiro se dividiu em 2: " + children);
            helper.getLevel().getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(center)
                    .inflate(24)).forEach(KaijuEntity::discard);
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_7d_apoio")
    public static void supportRoleNeedsAnUnlockedRangedAttack(GameTestHelper helper) {
        KaijuEntity abyss = kaiju(helper, "kaiju_no9_no11", new BlockPos(7, 1, 7));
        helper.runAfterDelay(3, () -> {
            helper.assertFalse(abyss.hasUsableRangedAttack(), "Abissal com a vida cheia nao tem tiro liberado");
            abyss.setHealth(abyss.getMaxHealth() * 0.5F);
            helper.assertTrue(abyss.hasUsableRangedAttack(), "Abaixo de 65% o canhao e a prisao liberam");
            abyss.discard();
            helper.succeed();
        });
    }
}
