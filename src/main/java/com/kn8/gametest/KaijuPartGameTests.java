// src/main/java/com/kn8/gametest/KaijuPartGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M7b: as regras de multipartes do PT4, agora na entidade definitiva e com as partes vindas do JSON
 * ({@code kaiju/primigenius.json}: head x1,2, torso x1,0, core x3,0 nucleo, legs x0,8).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuPartGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final BlockPos ATTACKER_POS = new BlockPos(1, 1, 1);
    private static final int SETTLE_TICKS = 2;
    private static final int PRIMIGENIUS_PARTS = 4;
    private static final float HIT = 10.0F;
    private static final float TOLERANCE = 0.05F;

    private KaijuPartGameTests() {
    }

    private static KaijuEntity spawn(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), CENTER);
        kaiju.setNoAi(true);
        return kaiju;
    }

    private static KaijuPart part(KaijuEntity kaiju, String name) {
        for (KaijuPart part : kaiju.kaijuParts()) {
            if (part.partName().equals(name)) {
                return part;
            }
        }
        throw new IllegalStateException("Parte inexistente: " + name);
    }

    @GameTest(template = TEMPLATE)
    public static void partsComeFromJsonWithConsecutiveIds(GameTestHelper helper) {
        KaijuEntity kaiju = spawn(helper);
        KaijuPart[] parts = kaiju.kaijuParts();
        helper.assertTrue(parts.length == PRIMIGENIUS_PARTS, "Esperava 4 partes do JSON, veio " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            helper.assertTrue(helper.getLevel().getPartEntities().contains(parts[i]),
                    "Parte " + parts[i].partName() + " nao registrada no nivel");
            helper.assertTrue(parts[i].getId() == kaiju.getId() + i + 1,
                    "ID da parte " + parts[i].partName() + " nao e consecutivo ao do pai");
        }
        helper.assertTrue(!kaiju.isPickable(), "Com partes, o corpo nao deveria ser miravel");
        kaiju.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void multipliersAndCoreHealthApply(GameTestHelper helper) {
        KaijuEntity kaiju = spawn(helper);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            DamageSource generic = helper.getLevel().damageSources().generic();
            float maxHealth = kaiju.getMaxHealth();
            float maxCore = kaiju.coreHealth();
            helper.assertTrue(Math.abs(maxCore - maxHealth * 0.25F) < TOLERANCE,
                    "Vida do nucleo deveria ser 25% da vida maxima (JSON health_fraction)");
            KaijuPart head = part(kaiju, "head");
            KaijuPart core = part(kaiju, "core");
            kaiju.invulnerableTime = 0;
            head.hurt(generic, HIT);
            helper.assertTrue(Math.abs(kaiju.getHealth() - (maxHealth - HIT * 1.2F)) < TOLERANCE,
                    "Multiplicador da cabeca (x1,2) nao aplicado");
            kaiju.invulnerableTime = 0;
            core.hurt(generic, HIT);
            helper.assertTrue(Math.abs(kaiju.getHealth() - (maxHealth - HIT * 1.2F - HIT * 3.0F)) < TOLERANCE,
                    "Multiplicador do nucleo (x3,0) nao aplicado na vida");
            helper.assertTrue(Math.abs(kaiju.coreHealth() - (maxCore - HIT * 3.0F)) < TOLERANCE,
                    "Vida do nucleo nao caiu pelo multiplicador");
            kaiju.discard();
            helper.succeed();
        });
    }

    /**
     * Usa dano de mob, que passa pela armadura como um golpe real (o {@code generic} ignora armadura e escondia o
     * bug do M7b).
     */
    @GameTest(template = TEMPLATE)
    public static void destroyingTheCoreKillsEvenThroughArmor(GameTestHelper helper) {
        KaijuEntity kaiju = spawn(helper);
        ArmorStand attacker = helper.spawn(EntityType.ARMOR_STAND, ATTACKER_POS);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            KaijuPart core = part(kaiju, "core");
            kaiju.invulnerableTime = 0;
            DamageSource hit = helper.getLevel().damageSources().mobAttack(attacker);
            // Dano bruto que zera o nucleo, mas que (com o multiplicador e a armadura) nao zeraria a vida total.
            core.hurt(hit, kaiju.coreHealth() / core.multiplier() + 1.0F);
            helper.assertTrue(kaiju.coreHealth() <= 0.0F, "O golpe deveria ter zerado o nucleo");
            helper.assertTrue(kaiju.isDeadOrDying(), "Destruir o nucleo deveria matar o kaiju mesmo com armadura");
            attacker.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void explosionCountsOncePerTickAndSparesTheCore(GameTestHelper helper) {
        KaijuEntity kaiju = spawn(helper);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            DamageSource explosion = helper.getLevel().damageSources().explosion(null, null);
            float maxHealth = kaiju.getMaxHealth();
            float maxCore = kaiju.coreHealth();
            kaiju.invulnerableTime = 0;
            kaiju.hurt(explosion, HIT);
            // A explosao passa pela armadura, entao o valor exato do 1o golpe nao e HIT: compara antes/depois.
            float afterFirst = kaiju.getHealth();
            helper.assertTrue(afterFirst < maxHealth, "A primeira explosao deveria causar dano");
            for (KaijuPart part : kaiju.kaijuParts()) {
                kaiju.invulnerableTime = 0;
                part.hurt(explosion, HIT);
            }
            helper.assertTrue(Math.abs(kaiju.getHealth() - afterFirst) < TOLERANCE,
                    "Explosao aplicada mais de uma vez no mesmo tick");
            helper.assertTrue(Math.abs(kaiju.coreHealth() - maxCore) < TOLERANCE,
                    "Explosao nao deveria atingir o nucleo");
            kaiju.discard();
            helper.succeed();
        });
    }

    /** M11a: o GDD poe o nucleo da Trichonephila na cabeca (antes ela nao tinha partes). */
    @GameTest(template = TEMPLATE)
    public static void trichonephilaCoreIsTheHead(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), CENTER);
        kaiju.setNoAi(true);
        helper.assertTrue(kaiju.isMultipartEntity(), "A Trichonephila agora tem partes");
        helper.assertTrue(!kaiju.isPickable(), "Com partes, o corpo nao deveria ser miravel");
        helper.assertTrue(part(kaiju, "head").isCore(), "O nucleo da Trichonephila e a cabeca (GDD)");
        kaiju.discard();
        helper.succeed();
    }
}
