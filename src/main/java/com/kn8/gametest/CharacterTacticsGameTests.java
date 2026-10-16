// src/main/java/com/kn8/gametest/CharacterTacticsGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-3: guarda contra golpe pesado (defende mas ainda leva dano) e perfil tatico na escolha de alvo
 * (Mina prefere o kaiju grande).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CharacterTacticsGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 2;

    private CharacterTacticsGameTests() {
    }

    /** Miguel: os mais fortes (Narumi) defendem o golpe pesado mas ainda tomam dano. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void narumiGuardsAHeavyHitButStillTakesDamage(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 3));
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        HoshinaEntity narumi = helper.spawn(KN8Entities.NARUMI.get(), new BlockPos(7, 1, 6));
        narumi.setNoAi(true);
        float[] before = new float[1];
        ResourceLocation blast = KN8Constants.id("energy_blast");
        helper.runAfterDelay(SETTLE, () -> {
            narumi.setYRot(180.0F);
            narumi.setYHeadRot(180.0F);
            before[0] = narumi.getHealth();
            // Tiro pesado de kaiju (raio de energia) vindo de frente: o Narumi nao esquiva de projetil.
            Vec3 from = kaiju.position().add(0, 2.0, 0);
            KN8Data.ABILITY.get(blast, false).ifPresent(ability -> KaijuProjectile.shoot(kaiju, blast, ability, from,
                    narumi.getBoundingBox().getCenter().subtract(from), 20.0F));
        });
        helper.runAfterDelay(SETTLE + 40, () -> {
            helper.assertTrue(narumi.heavyGuards() == 1, "O Narumi deveria defender o golpe pesado de frente");
            helper.assertTrue(narumi.getHealth() < before[0], "Mesmo defendendo, ele ainda leva dano");
            kaiju.discard();
            narumi.discard();
            helper.succeed();
        });
    }

    /** Perfil da Mina: prefere o kaiju grande (big_target) ao pequeno mais perto. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void minaPrefersTheBigKaiju(GameTestHelper helper) {
        KaijuEntity small = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(3, 1, 6));
        small.setNoAi(true);
        KaijuEntity big = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(9, 1, 12));
        big.setNoAi(true);
        HoshinaEntity mina = helper.spawn(KN8Entities.MINA.get(), new BlockPos(3, 1, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(mina.getTarget() == big, "A Mina deveria mirar o Honju, nao a aranha mais perto");
            small.discard();
            big.discard();
            mina.discard();
        });
    }
}
