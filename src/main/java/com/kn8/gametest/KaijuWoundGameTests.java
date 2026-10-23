// src/main/java/com/kn8/gametest/KaijuWoundGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-2 (ferimentos por parte): perna ferida deixa lento e sem investida, sem acumular; cabeca ferida
 * atordoa e atordoa mais depois; nucleo nao fere; abdomen da aranha sem a teia; ferimento salvo no mundo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuWoundGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final BlockPos CENTER = new BlockPos(7, 1, 7);
    private static final int SETTLE = 2;
    /** Bem acima de qualquer limiar dos JSON, mas longe de matar (Primigenius 220, aranha 65). */
    private static final float BIG_HIT = 40.0F;

    private KaijuWoundGameTests() {
    }

    private static KaijuPart part(KaijuEntity kaiju, String name) {
        for (KaijuPart part : kaiju.kaijuParts()) {
            if (part.partName().equals(name)) {
                return part;
            }
        }
        throw new IllegalStateException("Parte inexistente: " + name);
    }

    private static DamageSource hit(GameTestHelper helper, IronGolem golem) {
        return helper.getLevel().damageSources().mobAttack(golem);
    }

    private static boolean blocks(KaijuEntity kaiju, String ability) {
        ResourceLocation id = KN8Constants.id(ability);
        AbilityDef def = KN8Data.ABILITY.get(id, false).orElseThrow();
        return kaiju.woundBlocks(id, def);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void legWoundSlowsBlocksChargeAndDoesNotStack(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), CENTER);
        kaiju.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 1, 1));
        golem.setNoAi(true);
        double[] speed = new double[2];
        helper.runAfterDelay(SETTLE, () -> {
            speed[0] = kaiju.getAttributeValue(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(!blocks(kaiju, "charge"), "Sem ferimento a investida sai");
            kaiju.invulnerableTime = 0;
            part(kaiju, "legs").hurt(hit(helper, golem), 4.0F);
            helper.assertTrue(kaiju.woundedParts().isEmpty(), "Um golpe fraco nao deveria ferir a perna");
            // So dano acumulado (sem ferimento) volta igual do mundo, sem abrir ferimento (bug do estouro do "left").
            CompoundTag piled = new CompoundTag();
            kaiju.saveWithoutId(piled);
            KaijuEntity before = KN8Entities.PRIMIGENIUS.get().create(helper.getLevel());
            helper.assertTrue(before != null, "Copia deveria existir");
            before.load(piled);
            helper.assertTrue(before.woundedParts().isEmpty(), "Dano acumulado salvo nao deveria virar ferimento");
            before.discard();
            part(kaiju, "legs").hurt(hit(helper, golem), BIG_HIT);
            helper.assertTrue(kaiju.woundedParts().contains("legs"), "Golpe forte na perna deveria feri-la");
            speed[1] = kaiju.getAttributeValue(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(speed[1] < speed[0] * 0.8, "Perna ferida deveria deixar o kaiju lento (" + speed[0]
                    + " -> " + speed[1] + ")");
            helper.assertTrue(blocks(kaiju, "charge"), "Perna ferida deveria tirar a investida");
            helper.assertTrue(!blocks(kaiju, "hooved_strike"), "Perna ferida nao tira o golpe de casco");
            part(kaiju, "legs").hurt(hit(helper, golem), BIG_HIT);
            helper.assertTrue(Math.abs(kaiju.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed[1]) < 1.0E-6,
                    "O ferimento nao acumula: mesma velocidade depois de outro golpe");
            helper.assertTrue(kaiju.syncedWoundedParts().contains("legs"), "A barra deveria saber da perna ferida");
            // Salvo no mundo: a copia volta ferida.
            CompoundTag tag = new CompoundTag();
            kaiju.saveWithoutId(tag);
            KaijuEntity copy = KN8Entities.PRIMIGENIUS.get().create(helper.getLevel());
            helper.assertTrue(copy != null, "Copia deveria existir");
            copy.load(tag);
            helper.assertTrue(copy.woundedParts().contains("legs"), "O ferimento deveria voltar depois de carregar");
            copy.discard();
        });
        helper.runAfterDelay(SETTLE + 2, () -> {
            kaiju.discard();
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void headWoundStunsAndStunsLonger(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), CENTER);
        kaiju.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 1, 1));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            part(kaiju, "head").hurt(hit(helper, golem), BIG_HIT);
            helper.assertTrue(kaiju.woundedParts().contains("head"), "Golpe forte na cabeca deveria feri-la");
            helper.assertTrue(kaiju.isStaggered(), "Cabeca ferida deveria atordoar na hora");
        });
        // stagger_ticks 30 do ferimento: acabou. Atordoar 20 com a cabeca ferida (x1,5) = 30 ticks.
        helper.runAfterDelay(SETTLE + 35, () -> {
            helper.assertTrue(!kaiju.isStaggered(), "O atordoamento do ferimento deveria ter acabado");
            kaiju.stagger(20);
        });
        helper.runAfterDelay(SETTLE + 35 + 25, () -> helper.assertTrue(kaiju.isStaggered(),
                "Com a cabeca ferida o atordoamento de 20 ticks deveria durar mais"));
        helper.runAfterDelay(SETTLE + 35 + 33, () -> {
            helper.assertTrue(!kaiju.isStaggered(), "Mas nao para sempre");
            kaiju.discard();
            golem.discard();
            helper.succeed();
        });
    }

    /** Todo ferimento dos JSON e de uma parte que a especie tem e que nao e o nucleo (a aranha: cabeca = nucleo). */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void woundPartsMatchTheSpecies(GameTestHelper helper) {
        int[] count = new int[1];
        KN8Data.KAIJU_AI.server().forEach((id, ai) -> {
            KaijuDef def = KN8Data.KAIJU.get(id, false).orElseThrow();
            ai.wounds().parts().forEach(wound -> {
                count[0]++;
                helper.assertTrue(def.parts().stream().anyMatch(part -> !part.core()
                        && part.name().equals(wound.part())), id + ": ferimento em parte inexistente ou no nucleo "
                        + wound.part());
            });
        });
        helper.assertTrue(count[0] >= 40, "Esperava os ferimentos das especies com partes, veio " + count[0]);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void coreDoesNotWoundAndSpiderAbdomenStopsTheWeb(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), CENTER);
        kaiju.setNoAi(true);
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(2, 1, 12));
        spider.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 1, 1));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            part(kaiju, "core").hurt(hit(helper, golem), BIG_HIT);
            helper.assertTrue(kaiju.woundedParts().isEmpty(), "O nucleo tem o sistema dele: nao abre ferimento");
            helper.assertTrue(!blocks(spider, "web_shot"), "Sem ferimento a aranha atira teia");
            part(spider, "abdomen").hurt(hit(helper, golem), 20.0F);
            helper.assertTrue(spider.woundedParts().contains("abdomen"), "Abdomen ferido");
            helper.assertTrue(blocks(spider, "web_shot"), "Abdomen ferido (fiandeiras) deveria tirar a teia");
            helper.assertTrue(!blocks(spider, "bite"), "Abdomen ferido nao tira a mordida");
        });
        helper.runAfterDelay(SETTLE + 2, () -> {
            kaiju.discard();
            spider.discard();
            golem.discard();
            helper.succeed();
        });
    }
}
