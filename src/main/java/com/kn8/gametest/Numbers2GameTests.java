// src/main/java/com/kn8/gametest/Numbers2GameTests.java
package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.8-A: o Isao carrega o perfil (manoplas Numbers 2, vida, aura, altura de 1,90 m) e o Thunder Emperor
 * expoe o nucleo do kaiju; o No. 9 absorve o Isao derrotado e vira a forma com o Isao (e nao absorve um Isao
 * inteiro); a forma com o Isao e o No. 13 carregam do JSON com todas as habilidades. Absorcoes em lotes proprios
 * (o No. 9 de um teste puxaria a presa do vizinho, como nos testes das formas da 0.7-E).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Numbers2GameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String BATCH = "kn8_numbers2";
    private static final double AREA = 6.0;
    private static final int SETTLE = 3;

    private Numbers2GameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void isaoProfileGivesTheGauntlets(GameTestHelper helper) {
        HoshinaEntity isao = helper.spawn(KN8Entities.ISAO.get(), new BlockPos(4, 1, 4));
        isao.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(isao.getMainHandItem().is(KN8Items.NUMBERS2_GAUNTLET.get()),
                    "Manoplas na mao: " + isao.getMainHandItem());
            SpecialSoldierDef def = isao.profile().orElseThrow();
            helper.assertTrue(isao.getAttributeValue(Attributes.MAX_HEALTH) == def.health(),
                    "Vida do perfil: " + isao.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(isao.getData(KN8Attachments.AURA).equals(KN8Constants.id("isao_numbers2")),
                    "Aura: " + isao.getData(KN8Attachments.AURA));
            helper.assertTrue(Math.abs(isao.getBbHeight() - 1.9F) < 0.02F, "Altura: " + isao.getBbHeight());
            helper.succeed();
        });
    }

    /** Thunder Emperor (N.6): o ultimo golpe do soco duplo expoe o nucleo (expose_core_ticks do JSON). */
    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void thunderEmperorExposesTheCore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HoshinaEntity isao = helper.spawn(KN8Entities.ISAO.get(), new BlockPos(2, 1, 4));
        isao.setNoAi(true);
        KaijuEntity kaiju = KaijuSpawner.spawn(level, KN8Constants.id("primigenius"),
                helper.absolutePos(new BlockPos(6, 1, 4))).orElseThrow();
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            isao.lookAt(kaiju, 360.0F, 90.0F);
            helper.assertTrue(isao.startTechnique("thunder_emperor", kaiju), "Thunder Emperor comecou");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(kaiju.isCoreExposed(), "O nucleo deveria ficar exposto");
            kaiju.discard();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void isaoFormLoadsFromData(GameTestHelper helper) {
        check(helper, "kaiju_no9_isao", 7500.0F, true);
    }

    @GameTest(template = TEMPLATE)
    public static void no13LoadsFromData(GameTestHelper helper) {
        check(helper, "kaiju_no13", 2600.0F, false);
    }

    /** O No. 9 absorve o Isao derrotado (abaixo de 30% da vida) e vira a forma com o Isao no lugar. */
    @GameTest(template = TEMPLATE, batch = BATCH + "_absorb", timeoutTicks = 200)
    public static void no9AbsorbsADefeatedIsao(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        no9.setNoAi(true);
        HoshinaEntity isao = helper.spawn(KN8Entities.ISAO.get(), new BlockPos(7, 1, 4));
        isao.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> isao.setHealth(isao.getMaxHealth() * 0.2F));
        helper.succeedWhen(() -> {
            List<KaijuEntity> fused = find(level, center, "kaiju_no9_isao");
            helper.assertTrue(fused.size() == 1, "Deveria surgir 1 No. 9 com o Isao, vieram " + fused.size());
            helper.assertTrue(no9.isRemoved() && isao.isRemoved(), "O No. 9 e o Isao deveriam sumir");
            cleanup(level, center);
        });
    }

    /** Isao inteiro nao e absorvido: o No. 9 so absorve quem ja derrotou. */
    @GameTest(template = TEMPLATE, batch = BATCH + "_healthy", timeoutTicks = 120)
    public static void no9IgnoresAHealthyIsao(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        no9.setNoAi(true);
        HoshinaEntity isao = helper.spawn(KN8Entities.ISAO.get(), new BlockPos(7, 1, 4));
        isao.setNoAi(true);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(!no9.isRemoved() && !isao.isRemoved() && !((KaijuNo9Entity) no9).isAbsorbing(),
                    "Com o Isao inteiro o No. 9 nao deveria absorver");
            cleanup(level, center);
            isao.discard();
            helper.succeed();
        });
    }

    private static void check(GameTestHelper helper, String species, float health, boolean no9Form) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id(species),
                helper.absolutePos(new BlockPos(4, 1, 4))).orElseThrow();
        kaiju.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!no9Form || kaiju instanceof KaijuNo9Entity, species + " deveria ser forma do No. 9");
            KaijuDef def = kaiju.def().orElse(null);
            helper.assertTrue(def != null, "Sem kaiju/" + species);
            helper.assertTrue(Math.abs(kaiju.getMaxHealth() - health) < 0.5F, "Vida: " + kaiju.getMaxHealth());
            helper.assertTrue(!no9Form || KN8Data.NUMBERED.get(kaiju.kaijuId(), false).isPresent(),
                    "Sem numbered/" + species);
            for (ResourceLocation ability : def.abilities()) {
                helper.assertTrue(KN8Data.ABILITY.get(ability, false).isPresent(), "Habilidade sem JSON: " + ability);
            }
            kaiju.discard();
            helper.succeed();
        });
    }

    private static List<KaijuEntity> find(ServerLevel level, BlockPos center, String species) {
        return level.getEntitiesOfClass(KaijuEntity.class, new AABB(center).inflate(AREA),
                kaiju -> kaiju.isAlive() && kaiju.kaijuId().equals(KN8Constants.id(species)));
    }

    private static void cleanup(ServerLevel level, BlockPos center) {
        level.getEntitiesOfClass(Entity.class, new AABB(center).inflate(AREA),
                entity -> entity instanceof KaijuEntity).forEach(Entity::discard);
    }
}
