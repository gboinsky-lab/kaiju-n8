package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.host.KaijuFormService;
import com.kn8.common.host.KaijuHostData;
import com.kn8.common.host.LarvaService;
import com.kn8.common.registry.KN8Items;
import com.kn8.core.power.BodyStat;
import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da HOST (Miguel 2026-10-30): o hospedeiro humano tem o traje travado entre 0,1% e 1% de Release (sobe com
 * o corpo treinado) e transforma so o braco direito (Shift + B) por um tempo, com o punho do No. 8, espera propria e
 * parte do tempo-limite da forma inteira gasto.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HostReleaseGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private HostReleaseGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_hostrel:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_hostrel_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        return player;
    }

    private static void setBody(FakePlayer player, int level) {
        for (BodyStat stat : BodyStat.values()) {
            PowerService.setBodyLevel(player, stat, level);
        }
    }

    /** Segura a tecla de Release por {@code ticks} ticks e devolve a % ativa em decimos. */
    private static int raise(FakePlayer player, int ticks) {
        PowerService.releaseInput(player, 1);
        for (int i = 0; i < ticks; i++) {
            PowerService.tick(player);
        }
        PowerService.releaseInput(player, 0);
        return PowerService.view(player).hostActiveTenths();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_host_release")
    public static void hostSuitReleaseIsStuckBetweenATenthAndOnePercent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        data.setHost(null);
        data.setLarva(null);
        FakePlayer host = player(helper, "host");
        FakePlayer human = player(helper, "human");
        LarvaService.clear(host);
        LarvaService.clear(human);
        host.setItemSlot(EquipmentSlot.CHEST, new ItemStack(KN8Items.MK1.get()));
        human.setItemSlot(EquipmentSlot.CHEST, new ItemStack(KN8Items.MK1.get()));
        try {
            helper.assertTrue(LarvaService.infect(host), "Hospedeiro");
            setBody(host, 0);
            setBody(human, 0);
            PowerService.setActive(host, 0);
            PowerService.setActive(human, 0);
            helper.assertTrue(Math.abs(PowerService.hostCap(host) - ServerConfig.HOST_RELEASE_MIN.get()) < 1.0E-6,
                    "Sem treino o teto e 0,1%: " + PowerService.hostCap(host));
            int tenths = raise(host, 200);
            helper.assertTrue(tenths == 1, "Segurando G o hospedeiro fica em 0,1%: " + tenths);
            helper.assertTrue(PowerService.effectiveRelease(host) == 0, "0,1% nao conta como 1% inteiro");
            raise(human, 200);
            helper.assertTrue(PowerService.effectiveRelease(human) > 1, "Quem nao e hospedeiro sobe normalmente");
            helper.assertTrue(PowerService.view(human).hostCapTenths() == 0, "Sem o campo do hospedeiro");
            setBody(host, ServerConfig.HOST_RELEASE_FULL_BODY_LEVEL.get() / 2);
            double half = PowerService.hostCap(host);
            helper.assertTrue(half > 0.4 && half < 0.7, "Metade do treino, perto de 0,55%: " + half);
            setBody(host, ServerConfig.HOST_RELEASE_FULL_BODY_LEVEL.get());
            tenths = raise(host, 200);
            helper.assertTrue(tenths == 10, "Corpo treinado chega a 1%: " + tenths);
            helper.assertTrue(PowerService.effectiveRelease(host) == 1, "1% inteiro");
            setBody(host, 100);
            raise(host, 200);
            helper.assertTrue(PowerService.view(host).hostActiveTenths() == 10, "Nunca passa de 1%");
        } finally {
            setBody(host, 0);
            setBody(human, 0);
            LarvaService.clear(host);
            data.setHost(null);
            data.setLarva(null);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_host_partial")
    public static void partialTransformationTurnsOnlyTheArm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        data.setHost(null);
        data.setLarva(null);
        FakePlayer host = player(helper, "partial");
        FakePlayer human = player(helper, "partial_human");
        LarvaService.clear(host);
        LarvaService.clear(human);
        KaijuFormService.resetWait(host);
        host.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.SWORD.get()));
        try {
            helper.assertTrue(KaijuFormService.startPartial(human) == KaijuFormService.Result.NOT_HOST,
                    "So o hospedeiro");
            helper.assertTrue(LarvaService.infect(host), "Hospedeiro");
            float health = host.getMaxHealth();
            helper.assertTrue(KaijuFormService.startPartial(host) == KaijuFormService.Result.TRANSFORMED, "Braco");
            helper.assertTrue(KaijuFormService.isPartial(host) && !KaijuFormService.isTransformed(host),
                    "So o braco: o corpo continua humano");
            helper.assertTrue(host.getMaxHealth() == health, "Vida humana");
            helper.assertTrue(CombatService.heldWeapon(host).map(w -> w.item().equals(KaijuFormService.FIST))
                    .orElse(false), "Golpes com o punho do No. 8, mesmo com a espada");
            helper.assertTrue(KaijuFormService.damageFactor(host)
                    == ServerConfig.PARTIAL_DAMAGE_MULTIPLIER.get().floatValue(), "Forca do braco");
            // Fim do tempo: volta sozinho e comeca a espera.
            host.getPersistentData().putLong("kn8_partial_until", level.getGameTime());
            KaijuFormService.tick(host);
            helper.assertFalse(KaijuFormService.isPartial(host), "Braco volta no fim do tempo");
            helper.assertTrue(KaijuFormService.startPartial(host) == KaijuFormService.Result.COOLDOWN,
                    "Espera depois da parcial");
            // A forma inteira logo depois ja comeca com parte do tempo-limite gasto.
            long debt = KaijuFormService.debt(host);
            helper.assertTrue(debt > 0 && debt <= ServerConfig.PARTIAL_LIMIT_COST_TICKS.get(),
                    "Tempo gasto pela parcial: " + debt);
            host.getPersistentData().remove("kn8_form_ready");
            helper.assertTrue(KaijuFormService.transform(host) == KaijuFormService.Result.TRANSFORMED, "Forma");
            helper.assertTrue(KaijuFormService.formTicks(host) >= debt, "Tempo-limite descontado: "
                    + KaijuFormService.formTicks(host));
            KaijuFormService.revert(host);
        } finally {
            KaijuFormService.endPartial(host);
            LarvaService.clear(host);
            data.setHost(null);
            data.setLarva(null);
        }
        helper.succeed();
    }
}
