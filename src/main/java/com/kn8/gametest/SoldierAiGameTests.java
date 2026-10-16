// src/main/java/com/kn8/gametest/SoldierAiGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-1B: soldado detecta kaiju sem ver, recarrega quando nao luta e, sem pentes, parte para a arma
 * branca (a de apoio ou, para quem nao tem, a do config: Mina, Reno, Kafka).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoldierAiGameTests {

    private static final String TEMPLATE = "empty_15x12x15";

    private SoldierAiGameTests() {
    }

    /** Miguel: soldados detectam os kaiju independente de onde estejam (aqui, atras de um muro). */
    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void soldierDetectsKaijuBehindAWall(GameTestHelper helper) {
        for (int x = 0; x < 15; x++) {
            for (int y = 1; y < 8; y++) {
                helper.setBlock(new BlockPos(x, y, 7), Blocks.STONE);
            }
        }
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(7, 1, 11));
        kaiju.setNoAi(true);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 2));
        soldier.setVariant("rifle");
        helper.succeedWhen(() -> {
            helper.assertTrue(soldier.getTarget() == kaiju, "O soldado deveria detectar o kaiju atras do muro");
            kaiju.discard();
            soldier.discard();
        });
    }

    /** Miguel: recuar e recarregar; sem luta, o soldado completa o pente com a reserva (sem perder balas). */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void soldierReloadsWhenIdle(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 7));
        soldier.setVariant("rifle");
        soldier.setRounds(1);
        soldier.setSpareMagazines(3);
        helper.succeedWhen(() -> {
            // IA-2: completa o pente (29 balas) e as que sobravam no pente velho nao se perdem.
            helper.assertTrue(soldier.rounds() == 30 && soldier.reserveRounds() == 61 && !soldier.isReloading(),
                    "Sem alvo, o soldado deveria completar o pente com 29 balas da reserva (pente " + soldier.rounds()
                            + ", reserva " + soldier.reserveRounds() + ")");
            soldier.discard();
        });
    }

    /** Miguel: depois das 3 recargas, arma branca (o atirador comum fica com a faca de apoio). */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void soldierOutOfAmmoTakesTheKnife(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(7, 1, 13));
        kaiju.setNoAi(true);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 1));
        soldier.setVariant("rifle");
        soldier.setRounds(0);
        soldier.setSpareMagazines(0);
        soldier.setTarget(kaiju);
        helper.succeedWhen(() -> {
            helper.assertTrue(soldier.isOutOfAmmo(), "Sem pentes, o soldado deveria ficar sem municao");
            helper.assertTrue(soldier.getMainHandItem().is(KN8Items.COMBAT_KNIFE.get()),
                    "Sem municao, o soldado deveria pegar a faca");
            kaiju.discard();
            soldier.discard();
        });
    }

    /** Bug da Fase 0: Mina/Reno/Kafka ficavam de mao vazia; agora pegam a arma do config. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void specialShooterOutOfAmmoIsNotEmptyHanded(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(7, 1, 13));
        kaiju.setNoAi(true);
        SoldierEntity reno = helper.spawn(KN8Entities.RENO.get(), new BlockPos(7, 1, 1));
        helper.runAfterDelay(2, () -> {
            reno.setRounds(0);
            reno.setSpareMagazines(0);
            reno.setTarget(kaiju);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(reno.isOutOfAmmo(), "Sem pentes, o Reno deveria ficar sem municao");
            helper.assertTrue(reno.getMainHandItem().is(KN8Items.COMBAT_KNIFE.get()),
                    "Sem municao, o Reno deveria pegar a faca do config em vez de ficar de mao vazia");
            kaiju.discard();
            reno.discard();
        });
    }
}
