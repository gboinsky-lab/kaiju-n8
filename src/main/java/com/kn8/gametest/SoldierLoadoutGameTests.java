package com.kn8.gametest;

import java.util.HashSet;
import java.util.Set;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.4 (soldados comuns): o atirador troca para a faca de apoio quando o kaiju chega perto, e o soldado
 * sem variante pedida sorteia so variantes comuns (nunca o machado, que e de soldado especial).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoldierLoadoutGameTests {

    private static final Set<String> COMMON = Set.of("rifle", "pistol", "sword", "knife");
    private static final int SAMPLES = 40;

    private SoldierLoadoutGameTests() {
    }

    @GameTest(template = "empty_9x7x9", timeoutTicks = 80)
    public static void rifleSoldierSwitchesToKnifeUpClose(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 6));
        kaiju.setNoAi(true);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 2));
        soldier.setVariant("rifle");
        helper.assertTrue(soldier.getMainHandItem().is(KN8Items.RIFLE.get()), "O atirador comeca com o rifle");
        helper.assertTrue(soldier.getOffhandItem().is(KN8Items.COMBAT_KNIFE.get()), "A faca fica na outra mao");
        soldier.setTarget(kaiju);
        helper.succeedWhen(() -> {
            helper.assertTrue(soldier.getMainHandItem().is(KN8Items.COMBAT_KNIFE.get()),
                    "Com o kaiju a menos de 3,5 blocos o atirador deveria trocar para a faca");
            helper.assertTrue(soldier.getOffhandItem().is(KN8Items.RIFLE.get()), "O rifle vai para a outra mao");
            soldier.discard();
            kaiju.discard();
        });
    }

    @GameTest(template = "empty_3x3")
    public static void randomVariantsAreOnlyCommonWeapons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < SAMPLES; i++) {
            SoldierEntity soldier = KN8Entities.SOLDIER.get().create(level);
            helper.assertTrue(soldier != null, "Soldado deveria existir");
            soldier.moveTo(helper.absoluteVec(Vec3.atCenterOf(new BlockPos(1, 1, 1))));
            soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(soldier.blockPosition()),
                    MobSpawnType.SPAWN_EGG, null);
            seen.add(soldier.variant());
            helper.assertTrue(!soldier.getMainHandItem().is(KN8Items.AXE.get()),
                    "Soldado comum nao pode sair com o machado (arma especial)");
            soldier.discard();
        }
        helper.assertTrue(COMMON.containsAll(seen), "Sorteio fora das variantes comuns: " + seen);
        helper.assertTrue(seen.size() >= 3, "O sorteio deveria variar (vieram so " + seen + ")");
        helper.succeed();
    }
}
