// src/main/java/com/kn8/gametest/No9FormsGameTests.java
package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.7-E (formas do No. 9, Biblioteca v22 secao 33): as tres formas carregam do JSON; o No. 9 vira a
 * forma preta perdendo; absorve o No. 10 (fusao) e a formiga (forma formiga); a pele endurecida reduz o dano; os
 * ataques a distancia novos acertam. As trocas de forma ficam num lote proprio.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class No9FormsGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String BATCH = "kn8_no9_forms";
    // So a propria estrutura do teste (os testes do lote rodam lado a lado).
    private static final double AREA = 6.0;

    private No9FormsGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void blackFormLoadsFromData(GameTestHelper helper) {
        check(helper, "kaiju_no9_black", 0.8F, 2200.0F);
    }

    @GameTest(template = TEMPLATE)
    public static void fusionFormLoadsFromData(GameTestHelper helper) {
        check(helper, "kaiju_no9_fusion", 2.0F, 5400.0F);
    }

    @GameTest(template = TEMPLATE)
    public static void antFormLoadsFromData(GameTestHelper helper) {
        check(helper, "kaiju_no9_camponotus", 4.0F, 3200.0F);
    }

    /** Abaixo de 40% da vida o No. 9 muta para a forma preta (transform do numbered/kaiju_no9.json). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void no9TurnsBlackWhenLosing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        no9.setNoAi(true);
        helper.runAfterDelay(5, () -> no9.setHealth(no9.getMaxHealth() * 0.3F));
        helper.runAfterDelay(15, () -> {
            List<KaijuEntity> black = find(level, center, "kaiju_no9_black");
            helper.assertTrue(no9.isRemoved(), "O No. 9 deveria sumir ao mudar de forma");
            helper.assertTrue(black.size() == 1, "Deveria surgir 1 forma preta, vieram " + black.size());
            helper.assertTrue(black.get(0).getHealth() == black.get(0).getMaxHealth(),
                    "A forma preta nasce com a vida cheia");
            cleanup(level, center);
            helper.succeed();
        });
    }

    /**
     * Com a vida abaixo de 75%, o No. 9 puxa o No. 10 com tentaculos e vira a fusao; o No. 10 some. Cada absorcao num
     * lote so dela: o No. 9 procura presas a 24 blocos e pegaria a do teste vizinho.
     */
    @GameTest(template = TEMPLATE, batch = BATCH + "_no10", timeoutTicks = 200)
    public static void no9AbsorbsNo10IntoFusion(GameTestHelper helper) {
        absorbs(helper, "kaiju_no10_small", 0.7F, "kaiju_no9_fusion");
    }

    /** Com a vida abaixo de 60%, o No. 9 absorve uma formiga e vira a forma formiga. */
    @GameTest(template = TEMPLATE, batch = BATCH + "_ant", timeoutTicks = 200)
    public static void no9AbsorbsAntWhenHurt(GameTestHelper helper) {
        absorbs(helper, "camponotus", 0.5F, "kaiju_no9_camponotus");
    }

    /** Com a vida cheia o No. 9 nao absorve a formiga (self_health_below 0,6). */
    @GameTest(template = TEMPLATE, batch = BATCH + "_healthy", timeoutTicks = 120)
    public static void healthyNo9IgnoresTheAnt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        KaijuEntity ant = KaijuSpawner.spawn(level, KN8Constants.id("camponotus"), center.east(4)).orElseThrow();
        no9.setNoAi(true);
        ant.setNoAi(true);
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(!no9.isRemoved() && !ant.isRemoved()
                    && !((KaijuNo9Entity) no9).isAbsorbing(), "Com a vida cheia o No. 9 nao deveria absorver");
            cleanup(level, center);
            helper.succeed();
        });
    }

    /** Pele endurecida: o mesmo golpe tira cerca de metade (reduction 0,5 da forma preta). */
    @GameTest(template = TEMPLATE)
    public static void hardenedSkinTakesLessDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuEntity hard = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9_black"),
                helper.absolutePos(new BlockPos(2, 1, 4))).orElseThrow();
        KaijuEntity soft = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9_black"),
                helper.absolutePos(new BlockPos(6, 1, 4))).orElseThrow();
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 1));
        hard.setNoAi(true);
        soft.setNoAi(true);
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            ((KaijuNo9Entity) hard).hardenFor(100);
            float hardBefore = hard.getHealth();
            float softBefore = soft.getHealth();
            hard.hurt(level.damageSources().mobAttack(golem), 200.0F);
            soft.hurt(level.damageSources().mobAttack(golem), 200.0F);
            float hardLoss = hardBefore - hard.getHealth();
            float softLoss = softBefore - soft.getHealth();
            helper.assertTrue(softLoss > 0 && hardLoss < softLoss * 0.6F,
                    "Endurecido deveria perder ~metade: " + hardLoss + " contra " + softLoss);
            hard.discard();
            soft.discard();
            helper.succeed();
        });
    }

    /**
     * Analise (v1.2): o mesmo golpe repetido (mesmo atacante, mesmo tipo) tira cada vez menos; um golpe de outro tipo
     * (outra especie atacando) volta a tirar o dano cheio. No No. 9 base: ele nao tem pele endurecida, que e sorteada
     * e misturaria os numeros.
     */
    @GameTest(template = TEMPLATE)
    public static void no9AdaptsToRepeatedHits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"),
                helper.absolutePos(new BlockPos(4, 1, 4))).orElseThrow();
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 1, 1));
        Zombie other = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 1, 1));
        no9.setNoAi(true);
        golem.setNoAi(true);
        other.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            float[] losses = new float[9];
            for (int i = 0; i < 8; i++) {
                float before = no9.getHealth();
                no9.hurt(level.damageSources().mobAttack(golem), 60.0F);
                losses[i] = before - no9.getHealth();
            }
            float before = no9.getHealth();
            no9.hurt(level.damageSources().mobAttack(other), 60.0F);
            losses[8] = before - no9.getHealth();
            helper.assertTrue(losses[0] > 0 && losses[7] < losses[0] * 0.8F,
                    "O oitavo golpe igual deveria tirar menos: " + losses[0] + " -> " + losses[7]);
            helper.assertTrue(Math.abs(losses[8] - losses[0]) < 0.5F,
                    "Golpe de outro tipo deveria tirar o dano cheio: " + losses[0] + " -> " + losses[8]);
            no9.discard();
            helper.succeed();
        });
    }

    /** Multi-Finger Gun (v1.2): a forma preta dispara 3 tiros de uma vez (behavior.hits). */
    @GameTest(template = "empty_9x7x33", timeoutTicks = 60)
    public static void blackMultiFingerGunFiresThreeShots(GameTestHelper helper) {
        KaijuEntity black = facingSouth(helper, "kaiju_no9_black");
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 16));
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> helper.assertTrue(black.startAbility(KN8Constants.id(
                "no9_black_multi_finger_gun"), golem), "A rajada deveria comecar"));
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(KaijuProjectile.class,
                new AABB(black.blockPosition()).inflate(20)).size() >= 3, "Deveriam sair 3 tiros"));
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void fusionHybridBeamHitsAtRange(GameTestHelper helper) {
        KaijuEntity fusion = facingSouth(helper, "kaiju_no9_fusion");
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 20));
        golem.setNoAi(true);
        float[] before = new float[1];
        helper.runAfterDelay(2, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(fusion.startAbility(KN8Constants.id("no9_fusion_hybrid_beam"), golem),
                    "O raio hibrido deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.getHealth() < before[0],
                    "O raio hibrido deveria atingir o golem a 18 blocos"));
        });
    }

    @GameTest(template = "empty_9x7x33", timeoutTicks = 160)
    public static void blackTendrilSlowsTheTarget(GameTestHelper helper) {
        KaijuEntity black = facingSouth(helper, "kaiju_no9_black");
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 14));
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(black.startAbility(KN8Constants.id("no9_black_tendril"), golem),
                    "O tentaculo deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "O tentaculo deveria deixar o alvo lento"));
        });
    }

    private static void absorbs(GameTestHelper helper, String prey, float selfHealth, String form) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        KaijuEntity target = KaijuSpawner.spawn(level, KN8Constants.id(prey), center.east(5)).orElseThrow();
        no9.setNoAi(true);
        target.setNoAi(true);
        helper.runAfterDelay(3, () -> no9.setHealth(no9.getMaxHealth() * selfHealth));
        helper.succeedWhen(() -> {
            List<KaijuEntity> fused = find(level, center, form);
            helper.assertTrue(fused.size() == 1, "Deveria surgir 1 " + form + ", vieram " + fused.size());
            helper.assertTrue(no9.isRemoved() && target.isRemoved(), "O No. 9 e a presa deveriam sumir");
            cleanup(level, center);
        });
    }

    private static KaijuEntity facingSouth(GameTestHelper helper, String species) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id(species),
                helper.absolutePos(new BlockPos(4, 1, 2))).orElseThrow();
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        return kaiju;
    }

    private static void check(GameTestHelper helper, String species, float width, float health) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id(species),
                helper.absolutePos(new BlockPos(4, 1, 4))).orElseThrow();
        kaiju.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(kaiju instanceof KaijuNo9Entity, species + " deveria ser um KaijuNo9Entity");
            KaijuDef def = kaiju.def().orElse(null);
            helper.assertTrue(def != null && def.kaijuClass() == KaijuClass.NUMBERED, "Sem kaiju/" + species);
            helper.assertTrue(Math.abs(kaiju.getBbWidth() - width) < 0.01F, "Hitbox: " + kaiju.getBbWidth());
            helper.assertTrue(Math.abs(kaiju.getMaxHealth() - health) < 0.5F, "Vida: " + kaiju.getMaxHealth());
            helper.assertTrue(KN8Data.NUMBERED.get(kaiju.kaijuId(), false).isPresent(), "Sem numbered/" + species);
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
