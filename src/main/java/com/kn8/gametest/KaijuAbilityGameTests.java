// src/main/java/com/kn8/gametest/KaijuAbilityGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M8: a habilidade {@code kn8:bite} do JSON da Trichonephila (preparo 6, ativo 2, recarga 20) causa o
 * dano no tick de impacto, nem antes nem depois; nao pode ser repetida durante a recarga; e o atordoamento cancela o
 * preparo. Alvo: vaca sem IA e sem armadura, para o dano ser exato.
 *
 * <p>M8-fix: {@link #combatGoalApproachesAndBites} usa a IA de verdade (goal de combate, navegacao e controle de
 * movimento), sem chamar {@code startAbility}: o bug era o kaiju parar antes do alcance e nunca morder.</p>
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuAbilityGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final BlockPos KAIJU_POS = new BlockPos(3, 1, 4);
    private static final BlockPos TARGET_POS = new BlockPos(5, 1, 4);
    private static final ResourceLocation BITE = KN8Constants.id("bite");
    private static final int SETTLE_TICKS = 2;
    private static final int BEFORE_IMPACT = 3;
    private static final int AFTER_IMPACT = 9;
    private static final int STAGGER_TICKS = 40;
    private static final float TOLERANCE = 0.05F;
    private static final BlockPos FAR_KAIJU_POS = new BlockPos(1, 1, 4);
    private static final BlockPos FAR_TARGET_POS = new BlockPos(7, 1, 4);
    private static final int REAL_COMBAT_TIMEOUT = 200;

    private KaijuAbilityGameTests() {
    }

    private static KaijuEntity spawnKaiju(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), KAIJU_POS);
        kaiju.setNoAi(true);
        return kaiju;
    }

    private static Cow spawnTarget(GameTestHelper helper) {
        return spawnTargetAt(helper, TARGET_POS);
    }

    private static Cow spawnTargetAt(GameTestHelper helper, BlockPos pos) {
        Cow cow = helper.spawn(EntityType.COW, pos);
        cow.setNoAi(true);
        return cow;
    }

    @GameTest(template = TEMPLATE)
    public static void biteHitsExactlyOnTheImpactTick(GameTestHelper helper) {
        KaijuEntity kaiju = spawnKaiju(helper);
        Cow cow = spawnTarget(helper);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            float healthBefore = cow.getHealth();
            float damage = (float) kaiju.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(kaiju.startAbility(BITE, cow), "A mordida deveria comecar");
            helper.assertTrue(!kaiju.startAbility(BITE, cow), "Nao pode iniciar outra durante a mordida");
            helper.runAfterDelay(BEFORE_IMPACT, () ->
                    helper.assertTrue(cow.getHealth() == healthBefore, "Dano antes do tick de impacto"));
            helper.runAfterDelay(AFTER_IMPACT, () -> {
                helper.assertTrue(Math.abs(cow.getHealth() - (healthBefore - damage)) < TOLERANCE,
                        "Esperava " + (healthBefore - damage) + " de vida, veio " + cow.getHealth());
                kaiju.discard();
                cow.discard();
                helper.succeed();
            });
        });
    }

    /** Kaiju com IA e alvo a ~6 blocos: o goal precisa se aproximar sozinho e a mordida precisa causar dano. */
    @GameTest(template = TEMPLATE, timeoutTicks = REAL_COMBAT_TIMEOUT)
    public static void combatGoalApproachesAndBites(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), FAR_KAIJU_POS);
        Cow cow = spawnTargetAt(helper, FAR_TARGET_POS);
        float healthBefore = cow.getHealth();
        kaiju.setTarget(cow);
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.getHealth() < healthBefore, "A Trichonephila ainda nao mordeu o alvo");
            kaiju.discard();
            cow.discard();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void staggerCancelsTheWindup(GameTestHelper helper) {
        KaijuEntity kaiju = spawnKaiju(helper);
        Cow cow = spawnTarget(helper);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            float healthBefore = cow.getHealth();
            helper.assertTrue(kaiju.startAbility(BITE, cow), "A mordida deveria comecar");
            kaiju.stagger(STAGGER_TICKS);
            helper.runAfterDelay(AFTER_IMPACT, () -> {
                helper.assertTrue(cow.getHealth() == healthBefore, "Atordoado no preparo nao deveria causar dano");
                helper.assertTrue(!kaiju.isUsingAbility(), "A habilidade deveria ter sido cancelada");
                kaiju.discard();
                cow.discard();
                helper.succeed();
            });
        });
    }
}
