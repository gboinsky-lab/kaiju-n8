package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Etapa 6 (dossie de fusoes do Miguel, 2026-11-06): o No. 9 absorve o No. 11, o No. 15 ou o No. 14 derrotado e vira
 * o Abissal, o Neural ou o Couracado, com a vida do dossie; o Couracado barra de frente (couraca + Muralha Negra) e
 * nao pelas costas. Cada absorcao num lote so dela (o No. 9 procura presas a 20 blocos).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class No9FusionGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final double AREA = 16.0;

    private No9FusionGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_fusao_abissal", timeoutTicks = 200)
    public static void no9AbsorbsNo11IntoAbyssal(GameTestHelper helper) {
        absorbs(helper, "kaiju_no11", "kaiju_no9_no11", 7200.0F);
    }

    @GameTest(template = TEMPLATE, batch = "kn8_fusao_neural", timeoutTicks = 200)
    public static void no9AbsorbsNo15IntoNeural(GameTestHelper helper) {
        absorbs(helper, "kaiju_no15", "kaiju_no9_no15", 6800.0F);
    }

    @GameTest(template = TEMPLATE, batch = "kn8_fusao_couracado", timeoutTicks = 200)
    public static void no9AbsorbsNo14IntoIronclad(GameTestHelper helper) {
        absorbs(helper, "kaiju_no14", "kaiju_no9_no14", 8400.0F);
    }

    @GameTest(template = TEMPLATE, batch = "kn8_fusao_muralha")
    public static void ironcladBlocksFromTheFrontOnly(GameTestHelper helper) {
        KaijuEntity ironclad = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("kaiju_no9_no14"),
                helper.absolutePos(new BlockPos(7, 1, 7))).orElseThrow();
        ironclad.setNoAi(true);
        IronGolem front = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 2));
        IronGolem back = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 12));
        front.setNoAi(true);
        back.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            ironclad.setYRot(180.0F);
            ironclad.setYBodyRot(180.0F);
            ironclad.yBodyRotO = 180.0F;
            // Muralha Negra fechada (70 ticks do ciclo) e couraca frontal.
            ironclad.tickCount = 10;
            helper.assertTrue(ironclad.isShielded() && !ironclad.isShieldDown(), "Muralha fechada e sem brecha");
            float before = ironclad.getHealth();
            ironclad.invulnerableTime = 0;
            ironclad.hurt(helper.getLevel().damageSources().mobAttack(front), 200.0F);
            float fromFront = before - ironclad.getHealth();
            float middle = ironclad.getHealth();
            ironclad.invulnerableTime = 0;
            ironclad.hurt(helper.getLevel().damageSources().mobAttack(back), 200.0F);
            float fromBack = middle - ironclad.getHealth();
            // 0,82 x 0,35 = 0,29 de frente; a pele endurecida das formas do No. 9 pode tirar mais de um dos dois.
            helper.assertTrue(fromBack > 0.0F && fromFront > 0.0F && fromFront < fromBack * 0.45F,
                    "De frente leva bem menos: frente " + fromFront + ", costas " + fromBack);
            ironclad.tickCount = 100;
            helper.assertTrue(!ironclad.isShielded() && !ironclad.isShieldDown(),
                    "Muralha aberta nao vira brecha");
            ironclad.discard();
            front.discard();
            back.discard();
            helper.succeed();
        });
    }

    private static void absorbs(GameTestHelper helper, String prey, String form, float health) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 1, 4));
        KaijuEntity no9 = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no9"), center).orElseThrow();
        KaijuEntity target = KaijuSpawner.spawn(level, KN8Constants.id(prey), center.east(6)).orElseThrow();
        no9.setNoAi(true);
        target.setNoAi(true);
        // A presa precisa estar derrotada (abaixo de 30% da vida).
        helper.runAfterDelay(3, () -> target.setHealth(target.getMaxHealth() * 0.2F));
        helper.succeedWhen(() -> {
            List<KaijuEntity> fused = level.getEntitiesOfClass(KaijuEntity.class, new AABB(center).inflate(AREA),
                    kaiju -> kaiju.isAlive() && kaiju.kaijuId().equals(KN8Constants.id(form)));
            helper.assertTrue(fused.size() == 1, "Deveria surgir 1 " + form + ", vieram " + fused.size());
            KaijuEntity fusion = fused.get(0);
            helper.assertTrue(fusion instanceof KaijuNo9Entity, form + " deveria ser forma do No. 9");
            helper.assertTrue(Math.abs(fusion.getMaxHealth() - health) < 1.0F, "Vida: " + fusion.getMaxHealth());
            KaijuDef def = fusion.def().orElseThrow();
            def.abilities().forEach(ability -> helper.assertTrue(KN8Data.ABILITY.server().containsKey(ability),
                    "Habilidade sem JSON: " + ability));
            helper.assertTrue(KN8Data.NUMBERED.get(fusion.kaijuId(), false).isPresent(), "Sem numbered/" + form);
            helper.assertTrue(no9.isRemoved() && target.isRemoved(), "O No. 9 e a presa deveriam sumir");
            level.getEntitiesOfClass(Entity.class, new AABB(center).inflate(AREA),
                    entity -> entity instanceof KaijuEntity).forEach(Entity::discard);
        });
    }
}
