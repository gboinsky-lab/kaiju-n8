package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.numbered.No9Service;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.2 (Etapa 8): o Kaiju No. 9 revive a carcaca de um Primigenius como Primigenius ressurgido e foge
 * (some sem morrer) quando a vida cai abaixo do {@code flee_health}. Lote proprio (kaiju de 6 m).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class No9GameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final double AREA = 40.0;

    private No9GameTests() {
    }

    /** Raio (em chunks) forcado em volta do teste da ressurreicao em massa: cobre as carcacas a ate 14 blocos. */
    private static final int FORCED_CHUNK_RADIUS = 2;

    private static CarcassEntity carcass(ServerLevel level, KaijuEntity dead, double x, double y, double z) {
        dead.moveTo(x, y, z, 0.0F, 0.0F);
        CarcassEntity carcass = CarcassEntity.from(dead).orElseThrow();
        level.addFreshEntity(carcass);
        return carcass;
    }

    /** 0.3: onda mass_revive — o No. 9 levanta todas as carcacas (o Honju volta como chefe com barra). */
    @GameTest(template = TEMPLATE, batch = "kn8_no9_mass", timeoutTicks = 300)
    public static void no9MassRevivesEverything(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        // As carcacas ficam ate 14 blocos fora da estrutura do teste; sem jogador, o chunk delas as vezes nao carrega
        // e o No. 9 nao as ve (falhava em ~1 de 3 rodadas, como o teste de invasao): chunks forcados durante o teste.
        forceChunks(level, center, true);
        // So as carcacas deste teste contam (os lotes reaproveitam posicoes).
        List<CarcassEntity> army = List.of(
                carcass(level, KN8Entities.PRIMIGENIUS.get().create(level), center.getX() + 8.5, center.getY(),
                        center.getZ() + 0.5),
                carcass(level, KN8Entities.PRIMIGENIUS.get().create(level), center.getX() - 10.5, center.getY(),
                        center.getZ() + 6.5),
                carcass(level, KN8Entities.PRIMIGENIUS_HONJU.get().create(level), center.getX() + 0.5, center.getY(),
                        center.getZ() + 14.5));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center.offset(0, 0, -6))
                .orElseThrow();
        no9.getPersistentData().putBoolean(No9Service.MASS_TAG, true);
        AABB area = new AABB(center).inflate(AREA);

        // Espera ate o exercito levantar (gesto de 100 ticks + uma carcaca a cada 4), sem instante fixo: em lote
        // o pensamento do No. 9 (a cada 10 ticks) pode cair alguns ticks depois.
        helper.succeedWhen(() -> {
            helper.assertTrue(army.stream().noneMatch(Entity::isAlive),
                    "Todas as carcacas deveriam ter sido revividas");
            List<KaijuEntity> resurrected = level.getEntitiesOfClass(KaijuEntity.class, area, kaiju -> kaiju
                    .kaijuId().equals(KN8Constants.id("primigenius_resurrected")));
            List<KaijuEntity> revivedBoss = level.getEntitiesOfClass(KaijuEntity.class, area, kaiju -> kaiju
                    .kaijuId().equals(KN8Constants.id("primigenius_revived")) && kaiju.bossState() != null);
            helper.assertTrue(resurrected.size() >= 2, "Deveriam levantar 2 Primigenius ressurgidos, vieram "
                    + resurrected.size());
            helper.assertTrue(revivedBoss.size() == 1, "O Honju deveria voltar como chefe revivido");
            level.getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity
                    || entity instanceof CarcassEntity).forEach(Entity::discard);
            forceChunks(level, center, false);
        });
    }

    private static void forceChunks(ServerLevel level, BlockPos center, boolean forced) {
        for (int dx = -FORCED_CHUNK_RADIUS; dx <= FORCED_CHUNK_RADIUS; dx++) {
            for (int dz = -FORCED_CHUNK_RADIUS; dz <= FORCED_CHUNK_RADIUS; dz++) {
                level.setChunkForced((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, forced);
            }
        }
    }

    @GameTest(template = TEMPLATE, batch = "kn8_no9", timeoutTicks = 260)
    public static void no9RevivesCarcassAndFlees(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity dead = KN8Entities.PRIMIGENIUS.get().create(level);
        helper.assertTrue(dead != null, "Primigenius deveria existir");
        dead.moveTo(center.getX() + 6.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
        CarcassEntity carcass = CarcassEntity.from(dead).orElseThrow();
        level.addFreshEntity(carcass);
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center.offset(-4, 0, 0))
                .orElseThrow();
        helper.assertTrue(no9 instanceof KaijuNo9Entity, "kaiju_no9 deveria ser o KaijuNo9Entity");
        AABB area = new AABB(center).inflate(AREA);

        helper.runAfterDelay(110, () -> {
            helper.assertTrue(!carcass.isAlive(), "A carcaca deveria ter sido consumida pelo No. 9");
            List<KaijuEntity> revived = level.getEntitiesOfClass(KaijuEntity.class, area, kaiju -> kaiju.kaijuId()
                    .equals(KN8Constants.id("primigenius_resurrected")));
            helper.assertTrue(revived.size() == 1, "Deveria surgir 1 Primigenius ressurgido, vieram "
                    + revived.size());
            no9.setHealth(no9.getMaxHealth() * 0.1F);
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(no9.isRemoved() && no9.getRemovalReason() == Entity.RemovalReason.DISCARDED,
                    "Com 10% da vida o No. 9 deveria fugir (sumir sem morrer)");
            level.getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity
                    || entity instanceof CarcassEntity).forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** 0.6-D (Miguel): abaixo de 50% da vida o No. 9 regenera (regeneration do numbered/kaiju_no9.json). */
    @GameTest(template = TEMPLATE, batch = "kn8_no9_regen", timeoutTicks = 120)
    public static void no9RegeneratesBelowHalfHealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        no9.setNoAi(true);
        float[] before = new float[1];
        helper.runAfterDelay(5, () -> {
            no9.setHealth(no9.getMaxHealth() * 0.4F);
            before[0] = no9.getHealth();
        });
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(no9.getHealth() > before[0] + no9.getMaxHealth() * 0.02F,
                    "Com 40% da vida o No. 9 deveria regenerar: " + before[0] + " -> " + no9.getHealth());
            no9.discard();
            helper.succeed();
        });
    }
}
