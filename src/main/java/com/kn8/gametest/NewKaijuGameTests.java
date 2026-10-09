// src/main/java/com/kn8/gametest/NewKaijuGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.7-A/B: os kaiju novos do Miguel (Philinosoma, Diclonius, Camponotus e a revivida; cogumelos
 * Phaneroplasmodium e Myxogasterocarp; larva voadora) carregam com a
 * categoria, a hitbox, a vida e as partes do JSON (nucleo marcado), todas as habilidades deles existem e os ataques
 * a distancia novos (sopro, espinhos, acido) acertam o alvo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NewKaijuGameTests {

    private NewKaijuGameTests() {
    }

    @GameTest(template = "empty_9x7x9")
    public static void philinosomaLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.PHILINOSOMA.get(), KaijuClass.HONJU, 6.0F, 380.0F, 5);
    }

    @GameTest(template = "empty_9x7x9")
    public static void dicloniusLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.DICLONIUS.get(), KaijuClass.HONJU, 5.0F, 430.0F, 5);
    }

    @GameTest(template = "empty_9x7x9")
    public static void camponotusLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.CAMPONOTUS.get(), KaijuClass.YOJU, 4.0F, 200.0F, 3);
    }

    @GameTest(template = "empty_9x7x9")
    public static void camponotusRebornLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.CAMPONOTUS_REBORN.get(), KaijuClass.YOJU, 4.0F, 290.0F, 3);
    }

    @GameTest(template = "empty_9x7x9")
    public static void phaneroplasmodiumLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.PHANEROPLASMODIUM.get(), KaijuClass.YOJU, 4.4F, 240.0F, 3);
    }

    @GameTest(template = "empty_9x7x9")
    public static void myxogasterocarpLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.MYXOGASTEROCARP.get(), KaijuClass.HONJU, 7.0F, 420.0F, 4);
    }

    /** A larva nao tem partes (e pequena demais): so categoria, hitbox, vida e habilidades. */
    @GameTest(template = "empty_9x7x9")
    public static void larvaLoadsFromData(GameTestHelper helper) {
        check(helper, KN8Entities.KAIJU_LARVA.get(), KaijuClass.NUMBERED, 0.5F, 20.0F, 0);
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void myxogasterocarpSporeBombHitsAtRange(GameTestHelper helper) {
        rangedHits(helper, KN8Entities.MYXOGASTEROCARP.get(), "myxogasterocarp_spore_bomb", 16);
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void phaneroplasmodiumSporesSlowTheTarget(GameTestHelper helper) {
        KaijuEntity mushroom = facingSouth(helper, KN8Entities.PHANEROPLASMODIUM.get());
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 12));
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(mushroom.startAbility(KN8Constants.id("phaneroplasmodium_spore_shot"), golem),
                    "A nuvem de esporos deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "Os esporos deveriam deixar o alvo lento"));
        });
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void dicloniusBreathHitsAtRange(GameTestHelper helper) {
        rangedHits(helper, KN8Entities.DICLONIUS.get(), "diclonius_atomic_breath", 18);
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void philinosomaSpinesHitAtRange(GameTestHelper helper) {
        rangedHits(helper, KN8Entities.PHILINOSOMA.get(), "philinosoma_spine_shot", 14);
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void camponotusAcidSlowsTheTarget(GameTestHelper helper) {
        KaijuEntity ant = facingSouth(helper, KN8Entities.CAMPONOTUS.get());
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 12));
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(ant.startAbility(KN8Constants.id("camponotus_acid_spray"), golem),
                    "O jato de acido deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "O acido deveria deixar o alvo lento"));
        });
    }

    private static void rangedHits(GameTestHelper helper, EntityType<KaijuEntity> type, String ability, int distance) {
        KaijuEntity kaiju = facingSouth(helper, type);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 2 + distance));
        golem.setNoAi(true);
        float[] before = new float[1];
        helper.runAfterDelay(2, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(kaiju.startAbility(KN8Constants.id(ability), golem), ability + " deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.getHealth() < before[0],
                    ability + " deveria atingir o golem a " + distance + " blocos"));
        });
    }

    private static KaijuEntity facingSouth(GameTestHelper helper, EntityType<KaijuEntity> type) {
        KaijuEntity kaiju = helper.spawn(type, new BlockPos(4, 1, 2));
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        return kaiju;
    }

    private static void check(GameTestHelper helper, EntityType<KaijuEntity> type, KaijuClass kaijuClass, float width,
            float health, int parts) {
        KaijuEntity kaiju = helper.spawn(type, new BlockPos(4, 1, 4));
        kaiju.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            KaijuDef def = kaiju.def().orElse(null);
            helper.assertTrue(def != null, "Sem kaiju/*.json para " + type);
            helper.assertTrue(def.kaijuClass() == kaijuClass, "Categoria errada: " + def.kaijuClass());
            helper.assertTrue(Math.abs(kaiju.getBbWidth() - width) < 0.01F, "Hitbox deveria ter " + width
                    + " de largura, veio " + kaiju.getBbWidth());
            helper.assertTrue(Math.abs(kaiju.getMaxHealth() - health) < 0.5F, "Vida deveria ser " + health
                    + ", veio " + kaiju.getMaxHealth());
            boolean core = false;
            for (KaijuPart part : kaiju.kaijuParts()) {
                core |= part.isCore();
            }
            helper.assertTrue(kaiju.kaijuParts().length == parts && (core || parts == 0), "Deveria ter " + parts
                    + " partes com nucleo, veio " + kaiju.kaijuParts().length);
            for (ResourceLocation ability : def.abilities()) {
                helper.assertTrue(KN8Data.ABILITY.get(ability, false).isPresent(), "Habilidade sem JSON: " + ability);
            }
            kaiju.discard();
            helper.succeed();
        });
    }
}
