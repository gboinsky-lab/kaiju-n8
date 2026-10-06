package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.registry.KN8Blocks;
import com.kn8.common.registry.KN8Items;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.2 (Etapa 3): a bancada so fabrica com a bancada por perto, com a patente que libera a receita e
 * com os materiais no inventario; os materiais saem e o resultado entra.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CraftGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private CraftGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void workbenchChecksBenchRankAndMaterials(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("kn8_craft:bench".getBytes(
                StandardCharsets.UTF_8)), "kn8_craft");
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4, 1, 4)));
        player.getInventory().clearContent();

        helper.assertTrue(WorkbenchService.craft(player, KN8Constants.id("combat_knife"))
                == WorkbenchService.Result.NO_WORKBENCH, "Sem bancada por perto nao pode fabricar");
        helper.setBlock(new BlockPos(4, 1, 6), KN8Blocks.DEFENSE_WORKBENCH.get());
        helper.assertTrue(WorkbenchService.craft(player, KN8Constants.id("combat_knife"))
                == WorkbenchService.Result.MISSING, "Sem materiais deveria recusar");
        helper.assertTrue(WorkbenchService.craft(player, KN8Constants.id("rifle"))
                == WorkbenchService.Result.LOCKED, "Candidato nao deveria fabricar o rifle (Oficial)");

        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 3));
        player.getInventory().add(new ItemStack(Items.STICK, 1));
        helper.assertTrue(WorkbenchService.craft(player, KN8Constants.id("combat_knife"))
                == WorkbenchService.Result.OK, "Com bancada, patente e materiais deveria fabricar");
        helper.assertTrue(WorkbenchService.count(player.getInventory(), KN8Items.COMBAT_KNIFE.get()) == 1,
                "A faca deveria estar no inventario");
        helper.assertTrue(WorkbenchService.count(player.getInventory(), Items.IRON_INGOT) == 1,
                "Deveriam sobrar 1 ferro (3 - 2)");
        helper.assertTrue(WorkbenchService.count(player.getInventory(), Items.STICK) == 0, "O graveto deveria sair");
        helper.succeed();
    }
}
