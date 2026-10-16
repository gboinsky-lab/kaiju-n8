// src/main/java/com/kn8/gametest/SquadGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerService;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SquadService;
import com.kn8.core.soldier.SquadRules;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da IA-5: esquadrao do jogador (recrutar pela patente, ordens "recuar" e "ficar") e esquadroes de
 * defensores de invasao (posto e kaiju designado). Lotes proprios: a deteccao de 64 blocos pegaria kaiju dos testes
 * vizinhos.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SquadGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final ResourceLocation RAID = KN8Constants.id("primigenius_raid");
    private static final ResourceLocation SENIOR_OFFICER = KN8Constants.id("oficial_senior");

    private SquadGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_squad:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(7, 1, 7)));
        return player;
    }

    /** Oficial Senior comanda 2: o terceiro nao entra e soldado especial so a partir de Vice-Capitao. */
    @GameTest(template = TEMPLATE, batch = "kn8_squad_recruit")
    public static void recruitFollowsTheRank(GameTestHelper helper) {
        FakePlayer player = player(helper, "recruit");
        CareerService.setRank(player, SENIOR_OFFICER);
        SoldierEntity first = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(5, 1, 7));
        SoldierEntity second = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(9, 1, 7));
        SoldierEntity third = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 5));
        SoldierEntity hoshina = helper.spawn(KN8Entities.HOSHINA.get(), new BlockPos(7, 1, 9));
        SquadService.recruit(player, hoshina.getId());
        helper.assertTrue(hoshina.squadLeader().isEmpty(), "Oficial Senior nao deveria recrutar soldado especial");
        SquadService.recruit(player, first.getId());
        SquadService.recruit(player, second.getId());
        SquadService.recruit(player, third.getId());
        helper.assertTrue(first.isLedBy(player) && second.isLedBy(player), "Os dois primeiros deveriam entrar");
        helper.assertTrue(third.squadLeader().isEmpty(), "O terceiro nao cabe no esquadrao do Oficial Senior (2)");
        SquadService.dismiss(player, first.getId());
        helper.assertTrue(first.squadLeader().isEmpty(), "Dispensado, sai do esquadrao");
        SquadService.order(player, SquadRules.Order.STAY);
        helper.assertTrue(second.squadOrder() == SquadRules.Order.STAY && second.squadPost() != null,
                "A ordem ficar deveria chegar ao membro, com o posto onde ele esta");
        List.of(first, second, third, hoshina).forEach(Entity::discard);
        helper.succeed();
    }

    /** Ordem "recuar": o membro larga o kaiju (antes as ordens eram so desenhadas no menu). */
    @GameTest(template = TEMPLATE, batch = "kn8_squad_retreat", timeoutTicks = 100)
    public static void retreatDropsTheTarget(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(7, 1, 12));
        kaiju.setNoAi(true);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 2));
        soldier.setVariant("rifle");
        UUID leader = UUID.nameUUIDFromBytes("kn8_squad:leader".getBytes(StandardCharsets.UTF_8));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(soldier.getTarget() == kaiju, "Antes da ordem, o soldado deveria mirar o kaiju");
            soldier.joinSquad(leader);
            soldier.setSquadOrder(SquadRules.Order.RETREAT);
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(soldier.getTarget() == null, "Recuando, o soldado nao deveria ter alvo");
            kaiju.discard();
            soldier.discard();
            helper.succeed();
        });
    }

    /** Ordem "ficar": sem alvo, o soldado afastado volta ao posto (raio de guarda). */
    @GameTest(template = TEMPLATE, batch = "kn8_squad_stay", timeoutTicks = 200)
    public static void stayReturnsToThePost(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 7));
        soldier.joinSquad(UUID.nameUUIDFromBytes("kn8_squad:stay".getBytes(StandardCharsets.UTF_8)));
        soldier.setSquadOrder(SquadRules.Order.STAY);
        BlockPos post = soldier.squadPost();
        Vec3 far = helper.absoluteVec(new Vec3(13.5, 1, 7.5));
        soldier.teleportTo(far.x, far.y, far.z);
        helper.succeedWhen(() -> {
            helper.assertTrue(soldier.position().distanceTo(Vec3.atBottomCenterOf(post)) < 4.0,
                    "O soldado deveria voltar ao posto (esta a "
                            + Math.round(soldier.position().distanceTo(Vec3.atBottomCenterOf(post))) + " blocos)");
            soldier.discard();
        });
    }

    /** Bug 13: defensores em esquadroes, cada um com posto e um kaiju da onda designado. */
    @GameTest(template = TEMPLATE, batch = "kn8_squad_invasion", timeoutTicks = 200)
    public static void invasionDefendersGetPostsAndKaiju(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(7, 1, 7));
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setChunkForced((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, true);
            }
        }
        helper.assertTrue(InvasionService.start(level, RAID, center) == InvasionService.Result.OK,
                "A invasao deveria comecar");
        AABB area = new AABB(center).inflate(96.0);
        List<SoldierEntity> defenders = level.getEntitiesOfClass(SoldierEntity.class, area,
                soldier -> soldier.getPersistentData().getBoolean(InvasionService.TAG));
        helper.assertTrue(defenders.size() == 4, "A invasao deveria trazer 4 defensores, vieram " + defenders.size());
        Set<BlockPos> posts = new HashSet<>();
        defenders.forEach(soldier -> posts.add(soldier.squadPost()));
        helper.assertTrue(!posts.contains(null) && posts.size() == 2,
                "4 defensores = 2 esquadroes (3 + 1), cada um com um posto; postos: " + posts);
        InvasionService.skip(level);
        helper.runAfterDelay(60, () -> {
            Invasion invasion = InvasionService.active(level).orElseThrow();
            for (SoldierEntity soldier : defenders) {
                helper.assertTrue(soldier.assignedKaiju() != null
                        && invasion.alive().contains(soldier.assignedKaiju()),
                        "Na luta, cada defensor deveria ter um kaiju da onda designado");
            }
            InvasionService.stop(level);
            level.getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity
                    || entity instanceof CarcassEntity || entity instanceof SoldierEntity).forEach(Entity::discard);
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.setChunkForced((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, false);
                }
            }
            helper.succeed();
        });
    }
}
