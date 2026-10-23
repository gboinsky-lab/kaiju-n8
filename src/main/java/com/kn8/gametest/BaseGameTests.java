package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.base.BaseServices;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SquadService;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-8 (instalacoes da base): o oficial de missoes guarda o {@code giver}, e invulneravel e nao entra em
 * esquadrao; a estacao de preparacao cura e repoe a municao do esquadrao cobrando a municao do jogador, respeita o
 * raio e a espera. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private BaseGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_base:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_base_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        player.getInventory().clearContent();
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void officerKeepsGiverAndStaysOutOfSquads(GameTestHelper helper) {
        FakePlayer player = player(helper, "officer");
        DefenseOfficerEntity officer = helper.spawn(KN8Entities.DEFENSE_OFFICER.get(), new BlockPos(5, 1, 4));
        officer.setGiver(DefenseOfficerEntity.EXAM_OFFICER);
        helper.assertTrue(officer.isInvulnerable(), "Oficial deveria ser invulneravel");
        SquadService.recruit(player, officer.getId());
        helper.assertTrue(officer.squadLeader().isEmpty(), "Oficial nao pode entrar em esquadrao");
        BaseServices.talk(player, officer);
        CompoundTag saved = officer.saveWithoutId(new CompoundTag());
        DefenseOfficerEntity loaded = KN8Entities.DEFENSE_OFFICER.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "Oficial nao foi criado");
        loaded.load(saved);
        helper.assertTrue(DefenseOfficerEntity.EXAM_OFFICER.equals(loaded.giver()),
                "Giver nao voltou do save: " + loaded.giver());
        helper.assertTrue(loaded.isInvulnerable() && !loaded.canJoinSquad(), "Oficial carregado mudou de regra");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stationHealsAndResuppliesSquad(GameTestHelper helper) {
        FakePlayer player = player(helper, "station");
        BlockPos station = new BlockPos(4, 1, 4);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 2));
        soldier.setNoAi(true);
        soldier.setVariant("rifle");
        soldier.joinSquad(player.getUUID());
        soldier.setSpareMagazines(1);
        soldier.setHealth(5.0F);
        soldier.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
        soldier.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        int missing = soldier.missingRounds();
        helper.assertTrue(missing > 50, "Reserva com 1 pente deveria faltar balas, veio " + missing);
        player.getInventory().add(new ItemStack(KN8Items.RIFLE_AMMO.get(), 50));

        BaseServices.StationResult result = BaseServices.useStation(helper.getLevel(), helper.absolutePos(station),
                player);
        helper.assertTrue(result.members() == 1 && result.healed() == 1, "Estacao nao atendeu o soldado: " + result);
        helper.assertTrue(result.rounds() == 50 && result.missing() == missing - 50,
                "Deveria pagar so as 50 balas do inventario: " + result);
        helper.assertTrue(soldier.getHealth() >= soldier.getMaxHealth(), "Soldado nao foi curado");
        helper.assertTrue(!soldier.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                && soldier.hasEffect(MobEffects.DAMAGE_RESISTANCE), "So os efeitos ruins deveriam sair");
        helper.assertTrue(soldier.missingRounds() == missing - 50, "Reserva nao subiu 50");
        helper.assertTrue(player.getInventory().countItem(KN8Items.RIFLE_AMMO.get()) == 0,
                "A municao do jogador deveria ter sido gasta");

        // Espera entre usos: o segundo uso logo em seguida nao faz nada.
        player.getInventory().add(new ItemStack(KN8Items.RIFLE_AMMO.get(), 50));
        BaseServices.StationResult again = BaseServices.useStation(helper.getLevel(), helper.absolutePos(station),
                player);
        helper.assertTrue(again.members() == 0 && player.getInventory().countItem(KN8Items.RIFLE_AMMO.get()) == 50,
                "Estacao em espera nao poderia atender");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stationIgnoresSoldiersFarAwayOrOutsideTheSquad(GameTestHelper helper) {
        FakePlayer player = player(helper, "station_far");
        SoldierEntity stranger = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 2));
        stranger.setNoAi(true);
        stranger.setVariant("rifle");
        stranger.setHealth(5.0F);
        BaseServices.StationResult result = BaseServices.useStation(helper.getLevel(),
                helper.absolutePos(new BlockPos(4, 1, 4)), player);
        helper.assertTrue(result.members() == 0 && stranger.getHealth() < 6.0F,
                "Soldado sem esquadrao nao pode ser atendido");
        // Membro longe demais (60 blocos): fora do raio da estacao.
        stranger.joinSquad(player.getUUID());
        BaseServices.StationResult far = BaseServices.useStation(helper.getLevel(),
                helper.absolutePos(new BlockPos(4, 1, 4)).offset(60, 0, 0), player);
        helper.assertTrue(far.members() == 0, "Membro fora do raio nao pode ser atendido");
        helper.succeed();
    }
}
