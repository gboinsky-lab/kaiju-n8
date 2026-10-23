package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.base.BaseBuilder;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.base.PlayerBases;
import com.kn8.common.career.CareerService;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.destruction.ProtectedAreas;
import com.kn8.common.registry.KN8Blocks;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-8D: base modular do jogador. O nucleo fica 60 blocos acima da area do teste para os lotes (que sao
 * limpos ao construir) nao tocarem nos testes vizinhos. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerBaseGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation WORKSHOP = KN8Constants.id("workshop");
    private static final ResourceLocation COMMAND = KN8Constants.id("command");

    private PlayerBaseGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name, BlockPos at) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_base:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_base_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.moveTo(Vec3.atBottomCenterOf(at.above()));
        CareerService.setRank(player, KN8Constants.id("oficial_senior"));
        return player;
    }

    private static void give(FakePlayer player, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            player.getInventory().add(stack);
        }
    }

    @GameTest(template = TEMPLATE, batch = "kn8_player_base")
    public static void baseBuildsAndDemolishesModules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(4, 60, 4));
        FakePlayer player = player(helper, "builder", core);
        FakePlayer stranger = player(helper, "stranger", core);
        PlayerBases.get(level).of(player.getUUID()).ifPresent(old -> PlayerBases.get(level).removeAt(old.core()));
        level.setBlockAndUpdate(core, KN8Blocks.BASE_CORE.get().defaultBlockState());
        helper.assertTrue(BaseBuilder.found(player, core) == BaseBuilder.Result.OK, "Base nao fundada");
        helper.assertTrue(BaseBuilder.found(player, core.east(40)) == BaseBuilder.Result.ALREADY_HAS_BASE,
                "Uma base por jogador");
        helper.assertTrue(ProtectedAreas.get(level).areas().containsKey(BaseBuilder.areaName(player.getUUID())),
                "A base deveria ficar protegida");

        helper.assertTrue(BaseBuilder.check(player, core, 1, WORKSHOP) == BaseBuilder.Result.MISSING,
                "Sem material a oficina nao sai");
        helper.assertTrue(BaseBuilder.check(player, core, 2, COMMAND) == BaseBuilder.Result.MISSING_MODULE,
                "O comando pede a oficina");
        give(player, new ItemStack(Items.STONE_BRICKS, 64), new ItemStack(Items.IRON_INGOT, 16),
                new ItemStack(Items.GOLD_INGOT, 4));
        helper.assertTrue(BaseBuilder.build(player, core, 1, WORKSHOP) == BaseBuilder.Result.OK, "Oficina nao saiu");
        helper.assertTrue(WorkbenchService.count(player.getInventory(), Items.IRON_INGOT) == 8,
                "A oficina deveria gastar 8 ferros");
        BlockPos bench = BaseBuilder.cellOrigin(core, 1).offset(3, 1, 3);
        helper.assertTrue(level.getBlockState(bench).is(KN8Blocks.DEFENSE_WORKBENCH.get()),
                "A bancada da oficina deveria estar no lote " + bench.toShortString());
        helper.assertTrue(BaseBuilder.check(player, core, 1, WORKSHOP) == BaseBuilder.Result.OCCUPIED,
                "Lote ocupado");
        helper.assertTrue(BaseBuilder.check(stranger, core, 3, WORKSHOP) == BaseBuilder.Result.NOT_OWNER,
                "Outro jogador nao constroi na base");

        helper.assertTrue(BaseBuilder.build(player, core, 2, COMMAND) == BaseBuilder.Result.OK, "Comando nao saiu");
        BlockPos origin = BaseBuilder.cellOrigin(core, 2);
        AABB cell = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + BaseBuilder.CELL,
                origin.getY() + BaseBuilder.HEIGHT, origin.getZ() + BaseBuilder.CELL);
        helper.assertTrue(level.getEntitiesOfClass(DefenseOfficerEntity.class, cell).size() == 2,
                "O comando deveria trazer os dois oficiais");
        helper.assertTrue(BaseBuilder.demolish(player, core, 2) == BaseBuilder.Result.OK, "Demolir falhou");
        helper.assertTrue(level.getEntitiesOfClass(DefenseOfficerEntity.class, cell).isEmpty()
                && PlayerBases.get(level).of(player.getUUID()).get().slot(2).isEmpty(),
                "Demolir tira os oficiais e libera o lote");

        BaseBuilder.demolish(player, core, 1);
        player.moveTo(Vec3.atCenterOf(core).add(60, 0, 0));
        helper.assertTrue(BaseBuilder.check(player, core, 3, WORKSHOP) == BaseBuilder.Result.TOO_FAR,
                "Longe do nucleo nao constroi");
        level.setBlockAndUpdate(core, Blocks.AIR.defaultBlockState());
        helper.assertTrue(PlayerBases.get(level).of(player.getUUID()).isEmpty()
                && !ProtectedAreas.get(level).areas().containsKey(BaseBuilder.areaName(player.getUUID())),
                "Quebrar o nucleo acaba com a base e a protecao");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playerBasesSave(GameTestHelper helper) {
        PlayerBases bases = new PlayerBases();
        UUID owner = UUID.nameUUIDFromBytes("kn8_base:save".getBytes(StandardCharsets.UTF_8));
        BlockPos core = new BlockPos(10, 64, -20);
        bases.found(owner, core);
        bases.setSlot(owner, 4, WORKSHOP);
        CompoundTag tag = bases.save(new CompoundTag(), helper.getLevel().registryAccess());
        PlayerBases loaded = PlayerBases.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.of(owner).map(base -> base.core().equals(core)
                && base.slot(4).filter(WORKSHOP::equals).isPresent() && base.slot(0).isEmpty()).orElse(false),
                "Base nao voltou do save");
        helper.succeed();
    }
}
