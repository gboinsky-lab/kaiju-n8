// src/main/java/com/kn8/gametest/BossAiGameTests.java
package com.kn8.gametest;

import java.util.HashMap;
import java.util.Map;

import com.kn8.KN8Constants;
import com.kn8.common.boss.BossService;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.No9Service;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-6: a fase do chefe muda os ataques favorecidos (bug 23 da Fase 0) e a memoria de adaptacao do
 * No. 9 e limitada e expira (Parte 12 do plano de IA).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossAiGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final ResourceLocation HONJU_BOSS = KN8Constants.id("honju");
    private static final ResourceLocation PHASE_SPEED = KN8Constants.id("boss_phase_speed");

    private BossAiGameTests() {
    }

    /** Honju chefe: acima de 50% soco/pisao/mordida; abaixo, investida e raio (boss/honju.json). */
    @GameTest(template = TEMPLATE, batch = "kn8_boss_phase", timeoutTicks = 100)
    public static void bossPhaseChangesFavoredAttacks(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, helper.getLevel().getServer());
        BlockPos pos = helper.absolutePos(new BlockPos(7, 1, 7));
        KaijuEntity boss = BossService.spawn(helper.getLevel(), HONJU_BOSS, pos).orElseThrow();
        boss.setNoAi(true);
        Map<ResourceLocation, Integer> first = boss.bossPhaseAttacks();
        helper.assertTrue(first.containsKey(KN8Constants.id("heavy_punch")) && !first.containsKey(
                KN8Constants.id("energy_blast")), "Fase 1 deveria favorecer o soco, nao o raio: " + first);
        helper.runAfterDelay(5, () -> boss.setHealth(boss.getMaxHealth() * 0.4F));
        helper.runAfterDelay(15, () -> {
            Map<ResourceLocation, Integer> second = boss.bossPhaseAttacks();
            helper.assertTrue(boss.bossState().phase() == 1, "Abaixo de 50% o chefe deveria estar na fase 2");
            helper.assertTrue(second.containsKey(KN8Constants.id("energy_blast"))
                    && !second.containsKey(KN8Constants.id("heavy_punch")),
                    "Fase 2 deveria favorecer o raio e a investida: " + second);
            AABB area = new AABB(pos).inflate(48.0);
            helper.getLevel().getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity
                    || entity instanceof CarcassEntity).forEach(Entity::discard);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,
                    helper.getLevel().getServer());
            helper.succeed();
        });
    }

    /**
     * PM-5: a troca de fase muda o chefe de verdade (velocidade, recargas, distancia, nucleo exposto) e, salva no
     * mundo, volta na mesma fase com os mesmos modificadores, sem repetir a troca (aviso, invocacao).
     */
    @GameTest(template = TEMPLATE, batch = "kn8_boss_phase_change", timeoutTicks = 100)
    public static void bossPhaseChangesRhythmAndSurvivesReload(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, helper.getLevel().getServer());
        BlockPos pos = helper.absolutePos(new BlockPos(7, 1, 7));
        KaijuEntity boss = BossService.spawn(helper.getLevel(), HONJU_BOSS, pos).orElseThrow();
        boss.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(boss.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(PHASE_SPEED) == null,
                    "Na fase 1 o chefe nao deveria ter o bonus de velocidade");
            helper.assertTrue(BossService.cooldownMultiplier(boss) == 1.0F
                    && BossService.preferredDistance(boss) == 0.0F, "Fase 1 sem mudanca de ritmo nem distancia");
            boss.setHealth(boss.getMaxHealth() * 0.4F);
            BossService.tick(boss, boss.bossState());
            AttributeModifier speed = boss.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(PHASE_SPEED);
            helper.assertTrue(boss.bossState().phase() == 1, "Abaixo de 50% o chefe deveria estar na fase 2");
            helper.assertTrue(speed != null && Math.abs(speed.amount() - 0.2) < 1.0E-4,
                    "Fase 2 deveria dar +20% de velocidade: " + speed);
            helper.assertTrue(Math.abs(BossService.cooldownMultiplier(boss) - 0.75F) < 1.0E-4,
                    "Fase 2 deveria encurtar as recargas para 0,75");
            helper.assertTrue(BossService.preferredDistance(boss) == 10.0F, "Fase 2 deveria segurar 10 blocos");
            helper.assertTrue(boss.isCoreExposed(), "Na troca de fase o nucleo deveria ficar exposto");
        });
        helper.runAfterDelay(6, () -> {
            AABB area = new AABB(pos).inflate(80.0);
            int minions = helper.getLevel().getEntitiesOfClass(KaijuEntity.class, area, k -> k != boss).size();
            CompoundTag tag = boss.saveWithoutId(new CompoundTag());
            tag.remove("UUID");
            KaijuEntity copy = (KaijuEntity) boss.getType().create(helper.getLevel());
            copy.load(tag);
            copy.setNoAi(true);
            copy.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            boss.discard();
            helper.getLevel().addFreshEntity(copy);
            BossService.tick(copy, copy.bossState());
            int after = helper.getLevel().getEntitiesOfClass(KaijuEntity.class, area, k -> k != copy
                    && !k.isRemoved()).size();
            helper.assertTrue(copy.bossState() != null && copy.bossState().phase() == 1,
                    "Depois de carregar o chefe deveria continuar na fase 2");
            helper.assertTrue(copy.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(PHASE_SPEED) != null,
                    "Depois de carregar o bonus de velocidade da fase deveria voltar");
            helper.assertTrue(after == minions, "Carregar nao deveria repetir a troca (invocou " + (after - minions)
                    + ")");
            helper.assertTrue(!copy.isCoreExposed(), "Carregar nao deveria abrir de novo a janela do nucleo");
            helper.getLevel().getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity
                    || entity instanceof CarcassEntity).forEach(Entity::discard);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,
                    helper.getLevel().getServer());
            helper.succeed();
        });
    }

    /** Adaptacao: tipos ja esquecidos saem e, cheia, a memoria esquece o tipo mais antigo (nunca cresce sem fim). */
    @GameTest(template = TEMPLATE)
    public static void adaptationMemoryIsLimited(GameTestHelper helper) {
        Map<String, long[]> memory = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            memory.put("kind" + i, new long[] {5, 1000 + i});
        }
        memory.put("expired", new long[] {1, 0});
        No9Service.forget(memory, "new", 1050, 100, 6);
        helper.assertTrue(!memory.containsKey("expired"), "Tipo sem marcas deveria ser esquecido");
        helper.assertTrue(memory.size() == 5 && !memory.containsKey("kind0"),
                "Com 6 tipos lembrados, o mais antigo sai para caber o novo: " + memory.keySet());
        memory.put("new", new long[] {1, 1050});
        No9Service.forget(memory, "new", 1060, 100, 6);
        helper.assertTrue(memory.containsKey("new"), "O tipo que esta chegando nunca e esquecido");
        helper.succeed();
    }

    /** IA-7 (decisao do Miguel): no Dificil o preparo do golpe pesado encurta (0,85 x 20 = 17 ticks). */
    @GameTest(template = TEMPLATE, batch = "kn8_windup_hard", timeoutTicks = 40)
    public static void hardDifficultyShortensHeavyWindup(GameTestHelper helper) {
        Difficulty before = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.HARD, true);
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(7, 1, 7));
        honju.setNoAi(true);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(7, 1, 12));
        helper.runAfterDelay(2, () -> {
            boolean started = honju.startAbility(KN8Constants.id("slam"), cow);
            int ticks = honju.ticksToImpact().orElse(-1);
            helper.getLevel().getServer().setDifficulty(before, true);
            helper.assertTrue(started, "O pisao deveria comecar");
            helper.assertTrue(ticks == 17, "No Dificil o pisao (20 ticks no JSON) deveria ter 17 de preparo, teve "
                    + ticks);
            honju.discard();
            cow.discard();
            helper.succeed();
        });
    }
}
