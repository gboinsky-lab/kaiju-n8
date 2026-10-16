// src/main/java/com/kn8/gametest/KaijuSpeciesAiGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-4: perfil tatico por especie (janela do Honju depois do golpe pesado) e furia salva no mundo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuSpeciesAiGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 2;
    /** heavy_punch: preparo 12 + ativo 2. */
    private static final int PUNCH_TICKS = 14;

    private KaijuSpeciesAiGameTests() {
    }

    /** Honju: depois do golpe pesado fica parado (janela de reacao) e depois volta a lutar. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void honjuStandsStillAfterAHeavyHit(GameTestHelper helper) {
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(7, 1, 3));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 13));
        golem.setNoAi(true);
        Vec3[] at = new Vec3[1];
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(KN8Data.KAIJU_AI.get(honju.kaijuId(), false).isPresent(),
                    "O Honju deveria ter perfil tatico (kaiju_ai)");
            honju.setTarget(golem);
            helper.assertTrue(honju.startAbility(KN8Constants.id("heavy_punch"), golem), "O soco deveria comecar");
        });
        helper.runAfterDelay(SETTLE + PUNCH_TICKS + 3, () -> at[0] = honju.position());
        helper.runAfterDelay(SETTLE + PUNCH_TICKS + 15, () -> {
            double moved = honju.position().subtract(at[0]).horizontalDistance();
            helper.assertTrue(moved < 0.3, "Na janela depois do golpe pesado o Honju deveria ficar parado (andou "
                    + moved + ")");
            helper.assertTrue(!honju.isUsingAbility(), "Na janela o Honju nao deveria atacar");
        });
        helper.runAfterDelay(SETTLE + PUNCH_TICKS + 70, () -> {
            // Volta a lutar: anda ate o golem ou atira nele (o raio tambem e pesado e abre outra janela).
            boolean back = honju.isUsingAbility() || honju.position().subtract(at[0]).horizontalDistance() > 0.5
                    || golem.getHealth() < golem.getMaxHealth() || honju.getTarget() != golem;
            helper.assertTrue(back, "Depois da janela o Honju deveria voltar a lutar");
            honju.discard();
            golem.discard();
            helper.succeed();
        });
    }

    /** Bug #22 da auditoria: a furia nao era salva e o kaiju rugia de novo ao recarregar o mundo. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void rageSurvivesSaveAndLoad(GameTestHelper helper) {
        KaijuEntity revived = helper.spawn(KN8Entities.PRIMIGENIUS_REVIVED.get(), new BlockPos(7, 1, 7));
        revived.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> revived.setHealth(revived.getMaxHealth() * 0.1F));
        helper.runAfterDelay(SETTLE + 25, () -> {
            helper.assertTrue(revived.isEnraged(), "Com 10% da vida o revivido deveria estar enfurecido");
            CompoundTag tag = new CompoundTag();
            revived.saveWithoutId(tag);
            KaijuEntity copy = KN8Entities.PRIMIGENIUS_REVIVED.get().create(helper.getLevel());
            helper.assertTrue(copy != null, "Copia deveria existir");
            copy.load(tag);
            helper.assertTrue(copy.isEnraged(), "A furia deveria voltar depois de salvar e carregar");
            copy.discard();
            revived.discard();
            helper.succeed();
        });
    }
}
