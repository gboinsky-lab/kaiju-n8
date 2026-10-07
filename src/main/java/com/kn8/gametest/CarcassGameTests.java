// src/main/java/com/kn8/gametest/CarcassGameTests.java
package com.kn8.gametest;

import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M11b / 0.1-B: kaiju morto vira carcaca da mesma especie; segurar o clique direito com uma lamina
 * desmonta todas as etapas, solta os materiais (tecido sempre; nucleo intacto porque o nucleo nao foi destruido) e a
 * carcaca some.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CarcassGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("0d6c9a1e-3b7f-4c2d-8e5a-1f2b3c4d5e6f"), "kn8_dismantler");
    /** Trichonephila: 3 etapas de 30 ticks; cada interacao vale 4 ticks -> 8 por etapa. Folga incluida. */
    private static final int INTERACTIONS = 30;
    private static final int TIMEOUT = 200;

    private CarcassGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = TIMEOUT)
    public static void deadKaijuBecomesADismantlableCarcass(GameTestHelper helper) {
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 4));
        spider.setNoAi(true);
        // Passos agendados no nivel de cima, com atraso absoluto (runAfterDelay aninhado podia rodar duas vezes).
        helper.runAfterDelay(2, spider::kill);
        helper.runAfterDelay(5, () -> {
            List<CarcassEntity> carcasses = helper.getLevel().getEntitiesOfClass(CarcassEntity.class,
                    helper.getBounds());
            helper.assertTrue(carcasses.size() == 1, "Deveria haver uma carcaca, ha " + carcasses.size());
            CarcassEntity carcass = carcasses.get(0);
            helper.assertTrue(carcass.species().equals(KN8Constants.id("trichonephila")),
                    "A carcaca deveria ser da Trichonephila");
            FakePlayer player = FakePlayerFactory.get(helper.getLevel(), PROFILE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.COMBAT_KNIFE.get()));
            for (int i = 0; i < INTERACTIONS && carcass.isAlive(); i++) {
                carcass.interact(player, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(!carcass.isAlive(), "A carcaca deveria sumir depois de todas as etapas");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    helper.getBounds().inflate(4));
            boolean tissue = drops.stream().anyMatch(item -> item.getItem().is(KN8Items.KAIJU_TISSUE.get()));
            boolean core = drops.stream().anyMatch(item -> item.getItem().is(KN8Items.INTACT_CORE.get()));
            helper.assertTrue(tissue, "Deveria soltar tecido kaiju");
            helper.assertTrue(core, "Nucleo nao destruido deveria render o nucleo intacto");
            drops.forEach(ItemEntity::discard);
            helper.succeed();
        });
    }
}
