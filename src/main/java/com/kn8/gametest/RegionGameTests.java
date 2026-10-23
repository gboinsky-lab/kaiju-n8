package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.base.BaseServices;
import com.kn8.common.site.RegionAlerts;
import com.kn8.common.site.RegionService;
import com.kn8.core.world.RegionAlertMath.Event;
import com.kn8.core.world.RegionAlertMath.State;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-9: alerta por regiao. A regiao e criada direto pela caixa da area do teste (gerar um local da
 * obra inteiro num GameTest e caro); o que vale e a maquina de estados, os servicos fechados e o save.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegionGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation SITE = KN8Constants.id("tachikawa_base");

    private RegionGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void regionRaisesByCauseClosesServicesAndRecovers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos min = helper.absolutePos(BlockPos.ZERO);
        BlockPos max = helper.absolutePos(new BlockPos(8, 6, 8));
        BoundingBox box = BoundingBox.fromCorners(min, max);
        String key = "test@" + min.toShortString();
        BlockPos inside = helper.absolutePos(new BlockPos(4, 1, 4));
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(
                "kn8_region".getBytes(StandardCharsets.UTF_8)), "kn8_region"));
        player.moveTo(Vec3.atBottomCenterOf(inside));
        long now = level.getGameTime();

        helper.assertTrue(RegionService.stateAt(level, inside) == State.NORMAL, "Comeca normal");
        RegionService.reportRegion(level, key, SITE, box, Event.KAIJU_SEEN, "kn8:primigenius");
        helper.assertTrue(RegionService.stateAt(level, inside) == State.ALERT, "Kaiju visto = alerta");
        helper.assertFalse(RegionService.servicesClosed(level, inside), "Alerta nao fecha os servicos");
        RegionService.reportRegion(level, key, SITE, box, Event.FIGHTING, "kn8:primigenius");
        helper.assertTrue(RegionService.stateAt(level, inside) == State.COMBAT, "Kaiju lutando = combate");
        helper.assertTrue(RegionService.servicesClosed(level, inside), "Combate fecha os servicos");
        helper.assertTrue(BaseServices.useStation(level, inside, player) == BaseServices.StationResult.NONE,
                "Estacao fechada no combate");
        helper.assertTrue(RegionService.stateAt(level, helper.absolutePos(new BlockPos(4, 1, 4)).offset(40, 0, 0))
                == State.NORMAL, "Fora da regiao continua normal");

        // Sem evento novo: combate -> recuperacao -> normal (sai da lista).
        RegionService.expire(level, now + 100_000);
        helper.assertTrue(RegionService.stateAt(level, inside) == State.RECOVERY, "Depois do combate, recuperacao");
        helper.assertFalse(RegionService.servicesClosed(level, inside), "Recuperacao reabre os servicos");
        RegionService.expire(level, now + 1_000_000);
        helper.assertTrue(RegionService.stateAt(level, inside) == State.NORMAL
                && !RegionAlerts.get(level).regions().containsKey(key), "Volta ao normal e sai da lista");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regionAlertsSave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos min = helper.absolutePos(BlockPos.ZERO);
        BoundingBox box = BoundingBox.fromCorners(min, min.offset(8, 6, 8));
        String key = "save@" + min.toShortString();
        RegionService.reportRegion(level, key, SITE, box, Event.INVASION, RegionService.INVASION_CAUSE
                + "kn8:tachikawa_raid");
        CompoundTag tag = RegionAlerts.get(level).save(new CompoundTag(), level.registryAccess());
        RegionAlerts loaded = RegionAlerts.load(tag, level.registryAccess());
        RegionAlerts.Region region = loaded.regions().get(key);
        helper.assertTrue(region != null && region.status().state() == State.COMBAT && region.box().equals(box)
                && region.cause().equals("!kn8:tachikawa_raid"), "Regiao nao voltou do save");
        RegionAlerts.get(level).regions().remove(key);
        helper.succeed();
    }
}
