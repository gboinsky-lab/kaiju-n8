package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.DangerAvoid;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.core.kaiju.DangerZone;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-4 (plano de mobilidade v3 §6): a area do golpe pesado em preparo existe de verdade e o soldado
 * dentro dela acha para onde sair; tecnica de perto nao atravessa parede; tecnica para quando o alvo morre; aliado
 * na frente fecha a linha de tiro.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatSenseGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final String BATCH = "kn8_mov4_sense";
    private static final int SETTLE = 3;

    private CombatSenseGameTests() {
    }

    private static <T extends Mob> T still(T mob) {
        mob.setNoAi(true);
        return mob;
    }

    /** Pisao em preparo: a area (disco) existe, o soldado no meio dela esta dentro e ha um ponto fora para ir. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void soldierFindsTheWayOutOfASlam(GameTestHelper helper) {
        KaijuEntity kaiju = still(helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 4)));
        SoldierEntity soldier = still(helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 9)));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kaiju.startAbility(KN8Constants.id("slam"), soldier), "Pisao comecou");
            DangerZone zone = kaiju.dangerZone().orElse(null);
            helper.assertTrue(zone != null && zone.kind() == DangerZone.Kind.DISC, "Pisao com area de disco");
            soldier.moveTo(zone.cx(), soldier.getY(), zone.cz());
            helper.assertTrue(DangerAvoid.zoneAt(soldier).isPresent(), "Soldado no meio do pisao esta na area");
            Vec3 out = DangerAvoid.exitSpot(soldier).orElse(null);
            helper.assertTrue(out != null && !zone.contains(out.x, out.z, 0.0), "Ponto de saida fora da area: " + out);
            helper.succeed();
        });
    }

    /** Muro de bedrock entre o Hoshina e o golem colado: o Yae-uchi nao acerta atraves dele. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
    public static void techniqueDoesNotHitThroughAWall(GameTestHelper helper) {
        for (int y = 1; y <= 4; y++) {
            for (int z = 0; z < 15; z++) {
                helper.setBlock(new BlockPos(7, y, z), Blocks.BEDROCK);
            }
        }
        HoshinaEntity hoshina = still(helper.spawn(KN8Entities.HOSHINA.get(), new BlockPos(6, 1, 7)));
        // Golem (1,4 de largura) um bloco depois do muro: encostado nele, sufocava e perdia vida sozinho.
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(9, 1, 7)));
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            start[0] = golem.getHealth();
            helper.assertTrue(hoshina.chooseTechnique(golem).isEmpty(), "Nenhuma tecnica atraves da parede");
            helper.assertTrue(hoshina.startTechnique("yae_uchi", golem), "Yae-uchi comecou (forcado)");
        });
        helper.runAfterDelay(SETTLE + 40, () -> {
            helper.assertTrue(golem.getHealth() >= start[0], "Golpe atravessou a parede: " + golem.getHealth());
            helper.succeed();
        });
    }

    /** Alvo morto no meio da tecnica: ela para (antes seguia golpeando o ar ate o fim). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void techniqueStopsWhenTheTargetDies(GameTestHelper helper) {
        HoshinaEntity hoshina = still(helper.spawn(KN8Entities.HOSHINA.get(), new BlockPos(6, 1, 7)));
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(8, 1, 7)));
        helper.runAfterDelay(SETTLE, () -> helper.assertTrue(hoshina.startTechnique("ran_uchi", golem),
                "Ran-uchi comecou"));
        helper.runAfterDelay(SETTLE + 2, golem::kill);
        helper.runAfterDelay(SETTLE + 5, () -> {
            helper.assertFalse(hoshina.isUsingTechnique(), "A tecnica deveria parar com o alvo morto");
            helper.succeed();
        });
    }

    /** Outro soldado entre o atirador e o alvo fecha a linha de tiro; sem ele, a linha abre. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void allyInTheLineOfFire(GameTestHelper helper) {
        SoldierEntity shooter = still(helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 2)));
        SoldierEntity ally = still(helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 6)));
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 11)));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(DangerAvoid.allyInLine(shooter, golem), "Aliado na frente do tiro");
            ally.moveTo(ally.getX() + 4.0, ally.getY(), ally.getZ());
        });
        helper.runAfterDelay(SETTLE + 2, () -> {
            helper.assertFalse(DangerAvoid.allyInLine(shooter, golem), "Aliado saiu da frente");
            helper.succeed();
        });
    }
}
