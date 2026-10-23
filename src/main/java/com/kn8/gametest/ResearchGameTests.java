package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Blocks;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.research.ResearchService;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-8C: pesquisa no laboratorio. So perto do terminal e com o material de kaiju (que e gasto), uma por
 * vez, pre-requisitos e patente; pronta, libera receitas da bancada e aumenta o dano contra os kaiju dela; salva na
 * carreira. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation YOJU = KN8Constants.id("yoju_tissue");
    private static final ResourceLocation ARACHNID = KN8Constants.id("arachnid_study");
    private static final ResourceLocation MUSCLE = KN8Constants.id("muscle_mechanics");
    private static final ResourceLocation HONJU = KN8Constants.id("honju_anatomy");

    private ResearchGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_research:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_research_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerService.data(player).research().clear();
        CareerService.data(player).setResearching(null);
        CareerService.setRank(player, KN8Constants.id("candidato"));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void researchNeedsTerminalAndMaterial(GameTestHelper helper) {
        FakePlayer player = player(helper, "terminal");
        helper.assertTrue(ResearchService.check(player, YOJU) == ResearchService.Result.NO_TERMINAL,
                "Sem terminal a pesquisa deveria ser recusada");
        helper.setBlock(new BlockPos(4, 1, 6), KN8Blocks.RESEARCH_TERMINAL.get());
        helper.assertTrue(ResearchService.check(player, YOJU) == ResearchService.Result.MISSING,
                "Sem tecido de kaiju deveria faltar material");
        player.getInventory().add(new ItemStack(KN8Items.KAIJU_TISSUE.get(), 10));
        helper.assertTrue(ResearchService.start(player, YOJU) == ResearchService.Result.OK, "Pesquisa nao comecou");
        helper.assertTrue(WorkbenchService.count(player.getInventory(), KN8Items.KAIJU_TISSUE.get()) == 2,
                "A pesquisa deveria gastar 8 tecidos");
        helper.assertTrue(ResearchService.check(player, ARACHNID) == ResearchService.Result.BUSY,
                "So uma pesquisa por vez");
        ResearchService.tick(player);
        helper.assertFalse(CareerService.data(player).research().contains(YOJU), "Nao pode terminar antes do tempo");
        // Fim do tempo: a pesquisa termina sozinha no tick do jogador.
        CareerService.data(player).setResearching(new CareerData.Researching(YOJU,
                helper.getLevel().getGameTime()));
        ResearchService.tick(player);
        helper.assertTrue(CareerService.data(player).research().contains(YOJU)
                && CareerService.data(player).researching().isEmpty(), "A pesquisa deveria terminar no tempo");
        helper.assertTrue(ResearchService.check(player, YOJU) == ResearchService.Result.ALREADY_DONE,
                "Pesquisa concluida nao se repete");
        helper.assertTrue(ResearchService.check(player, ARACHNID) == ResearchService.Result.MISSING,
                "Com o pre-requisito pronto so falta o material");
        helper.assertTrue(ResearchService.check(player, MUSCLE) == ResearchService.Result.RANK_TOO_LOW,
                "Mecanica Muscular pede Oficial");
        CareerService.setRank(player, KN8Constants.id("oficial"));
        helper.assertTrue(ResearchService.check(player, HONJU) == ResearchService.Result.RANK_TOO_LOW,
                "Anatomia Honju pede Oficial Senior");
        CareerService.setRank(player, KN8Constants.id("oficial_senior"));
        helper.assertTrue(ResearchService.check(player, HONJU) == ResearchService.Result.MISSING_PREREQUISITE,
                "Anatomia Honju precisa da Mecanica Muscular");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "kn8_research_damage")
    public static void researchUnlocksAndBoostsDamage(GameTestHelper helper) {
        FakePlayer player = player(helper, "damage");
        ResourceLocation reinforced = KN8Constants.id("mk1_reinforced");
        helper.assertFalse(CareerService.isUnlocked(player, reinforced), "Candidato nao fabrica o Mk1 Reforcado");
        CareerService.data(player).research().add(MUSCLE);
        helper.assertTrue(CareerService.isUnlocked(player, reinforced),
                "A Mecanica Muscular libera o Mk1 Reforcado antes da patente");

        KaijuEntity before = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(2, 1, 2));
        KaijuEntity after = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(6, 1, 6));
        helper.assertTrue(Math.abs(ResearchService.damageFactor(player, before) - 1.0F) < 1.0E-4F,
                "Sem a analise de Yoju nao ha bonus");
        CareerService.data(player).research().add(HONJU);
        helper.assertTrue(Math.abs(ResearchService.damageFactor(player, before) - 1.0F) < 1.0E-4F,
                "Anatomia Honju nao vale contra Yoju");
        helper.runAfterDelay(2, () -> {
            float start = before.getHealth();
            CareerService.data(player).research().remove(YOJU);
            before.hurt(player.damageSources().playerAttack(player), 10.0F);
            float lossWithout = start - before.getHealth();
            CareerService.data(player).research().add(YOJU);
            helper.assertTrue(Math.abs(ResearchService.damageFactor(player, after) - 1.1F) < 1.0E-4F,
                    "Analise de Yoju deveria dar +10%");
            float startAfter = after.getHealth();
            after.hurt(player.damageSources().playerAttack(player), 10.0F);
            float lossWith = startAfter - after.getHealth();
            helper.assertTrue(lossWithout > 0 && lossWith > lossWithout * 1.05F,
                    "Golpe com a pesquisa deveria tirar mais: " + lossWithout + " -> " + lossWith);
            before.discard();
            after.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void researchSavesInCareer(GameTestHelper helper) {
        FakePlayer player = player(helper, "save");
        CareerData data = CareerService.data(player);
        data.research().add(YOJU);
        data.setResearching(new CareerData.Researching(ARACHNID, 1234L));
        Tag saved = CareerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        CareerData loaded = CareerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.research().contains(YOJU), "Pesquisa concluida nao voltou do save");
        helper.assertTrue(loaded.researching().map(r -> r.id().equals(ARACHNID) && r.finishTick() == 1234L)
                .orElse(false), "Pesquisa em andamento nao voltou do save");
        helper.succeed();
    }
}
