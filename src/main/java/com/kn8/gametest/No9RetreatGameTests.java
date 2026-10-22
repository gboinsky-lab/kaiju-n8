package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da retirada do No. 9 (Miguel, 2026-10-22: "ele deve tentar se retirar de algum jeito; caso nao consiga,
 * ele morre"): pego na retirada ele morre de verdade (carcaca); encurralado ate o fim do tempo, volta a lutar sem
 * fugir; durante a retirada nao ataca.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class No9RetreatGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    /** Um pouco abaixo do flee_health (0,15) do numbered/kaiju_no9.json. */
    private static final float LOW_HEALTH = 0.12F;
    /** max_ticks (300) do bloco retreat + folga. */
    private static final int RETREAT_END = 320;

    private No9RetreatGameTests() {
    }

    private static KaijuNo9Entity no9(GameTestHelper helper, String id, BlockPos pos) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id(id),
                helper.absolutePos(pos)).orElseThrow();
        helper.assertTrue(kaiju instanceof KaijuNo9Entity, "kaiju_no9 deveria ser o KaijuNo9Entity");
        return (KaijuNo9Entity) kaiju;
    }

    private static SoldierEntity soldier(GameTestHelper helper, BlockPos pos) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), pos);
        soldier.setNoAi(true);
        soldier.setInvulnerable(true);
        return soldier;
    }

    private static void cleanup(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(new BlockPos(7, 1, 7)))
                        .inflate(40), entity -> entity instanceof KaijuEntity || entity instanceof CarcassEntity
                        || entity instanceof SoldierEntity).forEach(Entity::discard);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_no9_retreat_a")
    public static void no9CaughtWhileRetreatingDies(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuNo9Entity no9 = no9(helper, "kaiju_no9", new BlockPos(7, 1, 7));
        SoldierEntity soldier = soldier(helper, new BlockPos(7, 1, 3));
        helper.runAfterDelay(SETTLE, () -> no9.setHealth(no9.getMaxHealth() * LOW_HEALTH));
        helper.runAfterDelay(SETTLE + 3, () -> {
            helper.assertTrue(no9.isRetreating(), "Com 12% da vida o No. 9 deveria estar em retirada");
            helper.assertFalse(no9.startAbility(KN8Constants.id("claw"), soldier),
                    "Em retirada ele nao ataca");
            no9.invulnerableTime = 0;
            no9.hurt(level.damageSources().mobAttack(soldier), no9.getMaxHealth() * 2.0F);
        });
        helper.runAfterDelay(SETTLE + 10, () -> {
            helper.assertTrue(no9.isDeadOrDying() || no9.getRemovalReason() == Entity.RemovalReason.KILLED,
                    "Pego na retirada ele deveria morrer (nao sumir)");
            helper.assertFalse(no9.getRemovalReason() == Entity.RemovalReason.DISCARDED,
                    "Nao pode ter fugido");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = RETREAT_END + 40, batch = "kn8_no9_retreat_b")
    public static void no9CorneredFightsToTheEnd(GameTestHelper helper) {
        // Cercado por bedrock (nao quebra) com um soldado dentro, sempre a vista: nao consegue escapar.
        for (int x = 1; x <= 13; x++) {
            for (int z = 1; z <= 13; z++) {
                if (x == 1 || x == 13 || z == 1 || z == 13) {
                    for (int y = 1; y <= 7; y++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.BEDROCK);
                    }
                }
            }
        }
        // Forma preta: o No. 9 normal viraria ela abaixo de 40% depois da retirada (transform) e o teste perderia a
        // entidade.
        KaijuNo9Entity no9 = no9(helper, "kaiju_no9_black", new BlockPos(7, 1, 9));
        soldier(helper, new BlockPos(7, 1, 4));
        float[] health = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            no9.setHealth(no9.getMaxHealth() * LOW_HEALTH);
            health[0] = no9.getHealth();
        });
        helper.runAfterDelay(SETTLE + 100, () -> {
            helper.assertTrue(no9.isRetreating() && no9.isAlive(), "Cercado ele ainda tenta escapar");
            helper.assertTrue(no9.getHealth() <= health[0] + 0.5F, "Na retirada ele nao regenera");
        });
        helper.runAfterDelay(RETREAT_END, () -> {
            helper.assertTrue(no9.isAlive() && !no9.isRemoved(), "Encurralado ele nao foge");
            helper.assertFalse(no9.isRetreating(), "Sem escapar no tempo da retirada, volta a lutar");
            cleanup(helper);
            helper.succeed();
        });
    }
}
