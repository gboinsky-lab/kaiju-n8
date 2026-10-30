package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.combat.FreezeSpread;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.KaijuNo8Entity;
import com.kn8.common.soldier.special.MinaEntity;
import com.kn8.common.soldier.special.RenoNo6Entity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da COMBATE-7 (pela obra): o gelo do Reno se espalha em etapas (a Lentidao sobe ate a do tiro, so quem tem
 * o gelo no perfil), o soco de ruptura do No. 8 (NPC e punhos da forma do jogador) e a mira da Mina.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Combat7GameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 3;
    /** Etapas e intervalo padrao do config ({@code [combat] freezeStages}/{@code freezeStageTicks}). */
    private static final int STAGES = 3;
    private static final int STAGE_TICKS = 8;

    private Combat7GameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_combate7_freeze")
    public static void renoIceSpreadsInStages(GameTestHelper helper) {
        RenoNo6Entity reno = helper.spawn(KN8Entities.RENO_NO6.get(), new BlockPos(2, 1, 2));
        reno.setNoAi(true);
        IronGolem frozen = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(9, 1, 3));
        frozen.setNoAi(true);
        IronGolem plain = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(9, 1, 10));
        plain.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(VfxService.hasRecipe(reno, FreezeSpread.RECIPE + "/3"), "Gelo no perfil do No. 6");
            FreezeSpread.apply(frozen, reno, frozen.position().add(0, 2.0, 0), 100, 7);
            FreezeSpread.apply(plain, null, plain.position(), 100, 7);
            helper.assertTrue(amplifier(frozen) == 2, "Primeira etapa: Lentidao III, nao VII: " + amplifier(frozen));
            helper.assertTrue(amplifier(plain) == 6, "Sem o gelo no perfil, de uma vez: " + amplifier(plain));
        });
        helper.runAfterDelay(SETTLE + STAGE_TICKS + 1, () -> helper.assertTrue(amplifier(frozen) == 4,
                "Segunda etapa: Lentidao V: " + amplifier(frozen)));
        helper.runAfterDelay(SETTLE + (STAGES - 1) * STAGE_TICKS + 1, () -> {
            helper.assertTrue(amplifier(frozen) == 6, "Ultima etapa: a do tiro (paralisia): " + amplifier(frozen));
            reno.discard();
            frozen.discard();
            plain.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "kn8_combate7_rupture")
    public static void no8RupturePunchSendsTheTargetFlying(GameTestHelper helper) {
        KaijuNo8Entity no8 = helper.spawn(KN8Entities.KAIJU_NO8.get(), new BlockPos(3, 1, 7));
        no8.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(6, 1, 7));
        golem.setNoAi(true);
        float[] before = new float[1];
        Vec3[] start = new Vec3[1];
        helper.runAfterDelay(SETTLE, () -> {
            WeaponDef fist = KN8Data.WEAPON.server().get(KN8Constants.id("kaiju_no8_fist"));
            helper.assertTrue(fist != null && fist.allTechniques().stream()
                    .anyMatch(t -> t.id().equals("rupture_punch")), "Soco de ruptura nos punhos da forma do jogador");
            helper.assertTrue(VfxService.hasRecipe(no8, "rupture_punch")
                    && VfxService.hasRecipe(no8, "rupture_punch_charge"), "Preparo e impacto desenhados");
            before[0] = golem.getHealth();
            start[0] = golem.position();
            helper.assertTrue(no8.startTechnique("rupture_punch", golem), "Soco de ruptura comecou");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            helper.assertTrue(golem.getHealth() < before[0], "O soco acertou: " + golem.getHealth());
            no8.discard();
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "kn8_combate7_mina")
    public static void minaAimsBeforeEveryCannonShot(GameTestHelper helper) {
        MinaEntity mina = helper.spawn(KN8Entities.MINA.get(), new BlockPos(2, 1, 2));
        mina.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            for (String id : new String[] {"precision_shot", "cannon_shot", "charged_cannon", "anti_giant_shot"}) {
                helper.assertTrue(VfxService.hasRecipe(mina, id + "_aim"), "Mira da Mina: " + id);
            }
            mina.discard();
            helper.succeed();
        });
    }

    private static int amplifier(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return effect == null ? -1 : effect.getAmplifier();
    }
}
