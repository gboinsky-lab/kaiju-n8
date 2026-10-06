// src/main/java/com/kn8/gametest/CombatGameTests.java
package com.kn8.gametest;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.combat.MeleeRaycast;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M10a: o raycast de golpe acerta a parte para onde se mira (o nucleo na frente do torso, a cabeca la
 * em cima) e o golpe pesado expoe o nucleo (x1,5 do config sobre o x3,0 da parte).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final BlockPos KAIJU_POS = new BlockPos(4, 1, 2);
    private static final BlockPos ATTACKER_POS = new BlockPos(4, 1, 8);
    private static final int SETTLE_TICKS = 2;
    private static final double REACH = 8.0;
    private static final float HIT = 10.0F;
    private static final float TOLERANCE = 0.05F;

    private CombatGameTests() {
    }

    private static KaijuEntity spawnFacingSouth(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), KAIJU_POS);
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
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

    /** Mira do ponto do atacante no centro de uma parte e confere qual entidade o raycast devolve. */
    private static Optional<Entity> aimAt(GameTestHelper helper, Entity attacker, KaijuPart target) {
        Vec3 from = new Vec3(target.getBoundingBox().getCenter().x, target.getBoundingBox().getCenter().y,
                attacker.getZ());
        Vec3 direction = target.getBoundingBox().getCenter().subtract(from);
        return MeleeRaycast.findTarget(helper.getLevel(), attacker, from, direction, REACH);
    }

    @GameTest(template = TEMPLATE)
    public static void raycastHitsThePartBeingAimedAt(GameTestHelper helper) {
        KaijuEntity kaiju = spawnFacingSouth(helper);
        ArmorStand attacker = helper.spawn(EntityType.ARMOR_STAND, ATTACKER_POS);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            Optional<Entity> core = aimAt(helper, attacker, part(kaiju, "core"));
            helper.assertTrue(core.isPresent() && core.get() instanceof KaijuPart hit
                    && hit.partName().equals("core"), "Mirando no nucleo deveria acertar o nucleo, veio " + core);
            Optional<Entity> head = aimAt(helper, attacker, part(kaiju, "head"));
            helper.assertTrue(head.isPresent() && head.get() instanceof KaijuPart hit
                    && hit.partName().equals("head"), "Mirando na cabeca deveria acertar a cabeca, veio " + head);
            kaiju.discard();
            attacker.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void heavyHitExposesTheCore(GameTestHelper helper) {
        KaijuEntity kaiju = spawnFacingSouth(helper);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            KaijuPart core = part(kaiju, "core");
            float coreBefore = kaiju.coreHealth();
            kaiju.exposeCore(100);
            helper.assertTrue(kaiju.isCoreExposed(), "O nucleo deveria estar exposto");
            kaiju.invulnerableTime = 0;
            core.hurt(helper.getLevel().damageSources().generic(), HIT);
            float expected = coreBefore - HIT * 3.0F * 1.5F;
            helper.assertTrue(Math.abs(kaiju.coreHealth() - expected) < TOLERANCE,
                    "Nucleo exposto deveria levar x3,0 x1,5: esperado " + expected + ", veio " + kaiju.coreHealth());
            kaiju.discard();
            helper.succeed();
        });
    }
}
