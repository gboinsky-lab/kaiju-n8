package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.host.KaijuHostData;
import com.kn8.common.host.LarvaService;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.world.KaijuSpawner;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da T-1 (larva rara e infeccao). O hospedeiro e um so por mundo (dado global), entao tudo que mexe nele
 * fica num teste so, num lote proprio, e volta o mundo ao estado sem hospedeiro no fim.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HostGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private HostGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_host:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_host_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        return player;
    }

    @GameTest(template = TEMPLATE, batch = "kn8_host")
    public static void larvaCirclesDivesAndInfectsOnlyOnePlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        data.setHost(null);
        data.setLarva(null);
        FakePlayer first = player(helper, "first");
        FakePlayer second = player(helper, "second");
        LarvaService.clear(first);
        LarvaService.clear(second);
        try {
            helper.assertTrue(LarvaService.canAppearFor(first), "Mundo sem hospedeiro: a larva pode aparecer");
            KaijuEntity larva = LarvaService.spawnFor(first).orElseThrow();
            helper.assertTrue(LarvaService.isHostLarva(larva) && larva.isInvulnerable(),
                    "Larva marcada e invulneravel");
            helper.assertFalse(LarvaService.canAppearFor(second), "Uma larva voando por vez no mundo");
            long circle = ServerConfig.LARVA_CIRCLE_TICKS.get();
            double closest = Double.MAX_VALUE;
            for (long age = 0; age < circle; age++) {
                LarvaService.steer(larva, first, age);
                closest = Math.min(closest, larva.distanceTo(first));
            }
            helper.assertFalse(LarvaService.isHost(first), "Circulando ainda nao infecta");
            helper.assertTrue(closest > ServerConfig.LARVA_INFECT_RADIUS.get(), "Circula longe: " + closest);
            for (long age = circle; age < circle + 400 && !larva.isRemoved(); age++) {
                LarvaService.steer(larva, first, age);
            }
            helper.assertTrue(larva.isRemoved(), "A larva entrou no corpo e sumiu");
            helper.assertTrue(LarvaService.isHost(first), "O primeiro virou hospedeiro");
            helper.assertTrue(first.getUUID().equals(data.host()) && data.larva() == null,
                    "Mundo guarda o hospedeiro e nenhuma larva");
            helper.assertFalse(LarvaService.canAppearFor(second), "Com hospedeiro, nenhuma larva nova");
            helper.assertFalse(LarvaService.infect(second), "So um jogador por mundo");
            helper.assertFalse(LarvaService.isHost(second), "O segundo continua humano");
            KaijuHostData loaded = KaijuHostData.load(data.save(new CompoundTag(), level.registryAccess()),
                    level.registryAccess());
            helper.assertTrue(first.getUUID().equals(loaded.host()), "Hospedeiro salvo no mundo");
        } finally {
            LarvaService.clear(first);
            LarvaService.clear(second);
            data.setHost(null);
            data.setLarva(null);
        }
        helper.assertFalse(LarvaService.isHost(first), "Clear desfaz");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_host_place")
    public static void larvaPlaceNeedsRemainsOrSite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos far = center.offset(0, 0, 400);
        helper.assertFalse(LarvaService.inLarvaPlace(level, far), "Campo vazio nao e lugar de larva");
        KaijuEntity kaiju = KaijuSpawner.spawn(level, KN8Constants.id("primigenius"), center).orElseThrow();
        CarcassEntity carcass = CarcassEntity.from(kaiju).orElseThrow();
        kaiju.discard();
        level.addFreshEntity(carcass);
        helper.assertTrue(LarvaService.inLarvaPlace(level, center), "Carcaca por perto: restos de kaiju");
        carcass.discard();
        helper.succeed();
    }
}
