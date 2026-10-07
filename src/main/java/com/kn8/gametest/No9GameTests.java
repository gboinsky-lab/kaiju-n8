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
        carcass(level, KN8Entities.PRIMIGENIUS.get().create(level), center.getX() + 8.5, center.getY(),
                center.getZ() + 0.5);
        carcass(level, KN8Entities.PRIMIGENIUS.get().create(level), center.getX() - 10.5, center.getY(),
                center.getZ() + 6.5);
        carcass(level, KN8Entities.PRIMIGENIUS_HONJU.get().create(level), center.getX() + 0.5, center.getY(),
                center.getZ() + 14.5);
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center.offset(0, 0, -6))
                .orElseThrow();
        no9.getPersistentData().putBoolean(No9Service.MASS_TAG, true);
        AABB area = new AABB(center).inflate(AREA);

        helper.runAfterDelay(170, () -> {
            helper.assertTrue(level.getEntitiesOfClass(CarcassEntity.class, area).isEmpty(),
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
            helper.succeed();
        });
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
}
