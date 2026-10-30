package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.base.BaseServices;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.combat.SpecialAttacks;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.host.KaijuFormService;
import com.kn8.common.host.KaijuHostData;
import com.kn8.common.host.LarvaService;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da T-4 (reacao da Forca de Defesa ao hospedeiro transformado): abaixo da patente de confianca os soldados
 * o tratam como kaiju (alvo, golpes e tiros), a transformacao vista custa merito e a base nao o atende; a partir da
 * patente so o descontrole vira alvo. A escolha de alvo com um jogador de verdade so em jogo (o jogador falso do
 * GameTest nao esta na lista de entidades do mundo).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForceReactionGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private ForceReactionGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_host_force")
    public static void defenseForceTreatsTheTransformedHostByRank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        data.setHost(null);
        data.setLarva(null);
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("kn8_host:force".getBytes(
                StandardCharsets.UTF_8)), "kn8_host_force");
        FakePlayer host = FakePlayerFactory.get(level, profile);
        host.setGameMode(GameType.SURVIVAL);
        host.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        LarvaService.clear(host);
        host.getPersistentData().remove("kn8_form_ready");
        CareerService.data(host).active().clear();
        CareerService.data(host).cooldowns().clear();
        CareerService.setRank(host, KN8Constants.id("candidato"));
        CareerService.addMerit(host, 300 - CareerService.data(host).merit());
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(1, 1, 1));
        soldier.setNoAi(true);
        try {
            helper.assertTrue(LarvaService.infect(host), "Hospedeiro");
            helper.assertFalse(KaijuFormService.hostileToForce(host), "Humano nao e alvo");
            helper.assertFalse(soldier.canAttack(host), "Soldado nao mira o jogador humano");
            helper.assertTrue(KaijuFormService.transform(host) == KaijuFormService.Result.TRANSFORMED, "Transforma");
            int penalty = ServerConfig.FORM_WITNESS_MERIT_PENALTY.get();
            helper.assertTrue(CareerService.data(host).merit() == 300 - penalty,
                    "Visto pelo soldado: -" + penalty + " de merito (ficou " + CareerService.data(host).merit() + ")");
            helper.assertTrue(KaijuFormService.hostileToForce(host), "Candidato transformado e kaiju para a Forca");
            helper.assertTrue(soldier.canAttack(host), "Soldado mira o hospedeiro transformado");
            helper.assertFalse(SpecialAttacks.isAlly(soldier, host), "Cortes e tiros do soldado o atingem");
            helper.assertTrue(MissionService.canAccept(host, KN8Constants.id("extermination"))
                    == MissionService.Result.TRANSFORMED, "Sem missoes transformado");
            helper.assertTrue(BaseServices.useStation(level, helper.absolutePos(new BlockPos(4, 1, 4)), host)
                    == BaseServices.StationResult.NONE, "Estacao nao atende");
            // A partir da patente de confianca: no controle os soldados nao atacam; sem controle, sim.
            CareerService.setRank(host, KN8Constants.id("vice_capitao"));
            helper.assertFalse(KaijuFormService.hostileToForce(host), "Vice-Capitao transformado nao e alvo");
            helper.assertFalse(soldier.canAttack(host), "Soldado nao mira o Vice-Capitao");
            helper.assertTrue(KaijuFormService.expire(host), "Leva ao limite");
            KaijuFormService.tick(host);
            helper.assertTrue(KaijuFormService.isRampaging(host), "Sem controle");
            helper.assertTrue(KaijuFormService.hostileToForce(host) && soldier.canAttack(host),
                    "Sem controle e alvo em qualquer patente");
            KaijuFormService.revert(host);
            helper.assertFalse(KaijuFormService.hostileToForce(host), "De volta a forma humana");
            helper.assertTrue(MissionService.canAccept(host, KN8Constants.id("extermination"))
                    == MissionService.Result.OK, "Humano volta a receber missoes");
            // Vice-Capitao se transformando na frente do soldado: ninguem estranha.
            KaijuFormService.resetWait(host);
            int before = CareerService.data(host).merit();
            helper.assertTrue(KaijuFormService.transform(host) == KaijuFormService.Result.TRANSFORMED, "De novo");
            helper.assertTrue(CareerService.data(host).merit() == before, "Patente de confianca nao perde merito");
            KaijuFormService.revert(host);
        } finally {
            LarvaService.clear(host);
            CareerService.setRank(host, KN8Constants.id("candidato"));
            data.setHost(null);
            data.setLarva(null);
        }
        helper.succeed();
    }
}
