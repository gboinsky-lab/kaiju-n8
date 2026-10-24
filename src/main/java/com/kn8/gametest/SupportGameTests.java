package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.support.SupportService;
import com.kn8.common.support.SupportService.Result;
import com.kn8.common.support.SupportService.Tier;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-10B: apoio militar. So em operacao (missao de campo ou invasao), com patente, usos por operacao e
 * espera; reconhecimento sem contato nao gasta; bombardeio so fere kaiju, com teto; suprimentos soltam os itens e
 * curam aliados. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SupportGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation PATROL = KN8Constants.id("patrol");

    private SupportGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_support:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_support_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerService.data(player).active().clear();
        CareerService.data(player).cooldowns().clear();
        CareerService.data(player).completed().remove(PATROL);
        player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove("kn8_support");
        CareerService.setRank(player, KN8Constants.id("oficial"));
        return player;
    }

    @GameTest(template = TEMPLATE, batch = "kn8_support")
    public static void supportNeedsOperationRankAndUses(GameTestHelper helper) {
        FakePlayer player = player(helper, "rules");
        // Uma invasao que sobrou de outro teste (falho ou lento) poria o jogador falso em operacao.
        InvasionService.stop(helper.getLevel());
        helper.assertTrue(SupportService.check(player, Tier.RECON) == Result.NO_OPERATION,
                "Fora de operacao o apoio e recusado");
        helper.assertTrue(MissionService.accept(player, PATROL) == MissionService.Result.OK, "Patrulha nao aceita");
        helper.assertTrue(SupportService.check(player, Tier.SUPPLY) == Result.OK, "Oficial pede suprimentos");
        helper.assertTrue(SupportService.check(player, Tier.STRIKE) == Result.RANK_TOO_LOW,
                "Bombardeio pede Oficial Senior");
        CareerService.setRank(player, KN8Constants.id("oficial_senior"));
        helper.assertTrue(SupportService.check(player, Tier.STRIKE) == Result.OK, "Oficial Senior pede bombardeio");

        BlockPos ground = helper.absolutePos(new BlockPos(4, 0, 4));
        String op = SupportService.operation(player).orElseThrow();
        helper.assertTrue(SupportService.callAt(player, Tier.RECON, ground) == Result.NO_CONTACT,
                "Reconhecimento sem kaiju nao tem contato");
        helper.assertTrue(SupportService.used(player, Tier.RECON, op) == 0, "Chamada sem contato nao gasta");
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(6, 1, 6));
        kaiju.setNoAi(true);
        helper.assertTrue(SupportService.callAt(player, Tier.RECON, ground) == Result.OK, "Reconhecimento com contato");
        helper.assertTrue(kaiju.hasEffect(MobEffects.GLOWING), "Kaiju marcado pelo reconhecimento");
        helper.assertTrue(SupportService.used(player, Tier.RECON, op) == 1, "Um uso gasto");
        helper.assertTrue(SupportService.callAt(player, Tier.RECON, ground) == Result.COOLDOWN,
                "Segunda chamada esbarra na espera");
        helper.assertTrue(SupportService.used(player, Tier.RECON, op) == 1, "Recusada nao gasta");
        // Supply ja gasto nesta operacao: o uso unico acaba.
        helper.assertTrue(SupportService.callAt(player, Tier.SUPPLY, ground) == Result.OK, "Suprimentos chamados");
        CompoundTag tag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(
                "kn8_support");
        tag.remove("c1");
        helper.assertTrue(SupportService.check(player, Tier.SUPPLY) == Result.NO_USES, "Um suprimento por operacao");
        // Operacao nova (missao aceita de novo) zera os usos.
        MissionService.abandon(player, PATROL);
        helper.assertTrue(SupportService.check(player, Tier.SUPPLY) == Result.NO_OPERATION,
                "Sem missao, sem operacao");
        kaiju.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void strikeHitsOnlyKaijuWithACap(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 4));
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 4));
        CivilianEntity civilian = helper.spawn(KN8Entities.CIVILIAN.get(), new BlockPos(6, 1, 4));
        kaiju.setNoAi(true);
        soldier.setNoAi(true);
        civilian.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            float kaijuBefore = kaiju.getHealth();
            float soldierBefore = soldier.getHealth();
            float civilianBefore = civilian.getHealth();
            int hit = SupportService.detonate(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
            float lost = kaijuBefore - kaiju.getHealth();
            helper.assertTrue(hit == 1 && lost > 0, "O bombardeio deveria ferir o kaiju: " + lost);
            helper.assertTrue(lost <= kaiju.getMaxHealth() * 0.08F + 0.01F, "Dano acima da fracao: " + lost);
            helper.assertTrue(soldier.getHealth() == soldierBefore && civilian.getHealth() == civilianBefore,
                    "O bombardeio nao fere aliados");
            kaiju.discard();
            soldier.discard();
            civilian.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void supplyDropsItemsAndHealsAllies(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(3, 1, 4));
        soldier.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
        SupportService.dropSupply(helper.getLevel(), at);
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(3));
        int rifleAmmo = items.stream().filter(e -> e.getItem().is(KN8Items.RIFLE_AMMO.get()))
                .mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(rifleAmmo == 60, "A caixa deveria ter 60 balas de rifle, veio " + rifleAmmo);
        helper.assertTrue(soldier.hasEffect(MobEffects.REGENERATION), "Soldado perto da caixa recupera vida");
        items.forEach(ItemEntity::discard);
        soldier.discard();
        helper.succeed();
    }
}
