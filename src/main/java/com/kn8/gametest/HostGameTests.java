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
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.world.KaijuSpawner;
import com.kn8.core.power.BodyStat;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
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

    @GameTest(template = TEMPLATE, batch = "kn8_host_form")
    public static void hostTransformsWithBodyStrengthAndTurnsBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        data.setHost(null);
        data.setLarva(null);
        FakePlayer host = player(helper, "form");
        FakePlayer human = player(helper, "human");
        LarvaService.clear(host);
        LarvaService.clear(human);
        host.getPersistentData().remove("kn8_form_ready");
        try {
            helper.assertTrue(KaijuFormService.transform(human) == KaijuFormService.Result.NOT_HOST,
                    "Quem nao foi infectado nao transforma");
            helper.assertTrue(LarvaService.infect(host), "Hospedeiro");
            PowerService.setBodyLevel(host, BodyStat.RESISTANCE, 0);
            float humanHealth = host.getMaxHealth();
            double humanHeight = host.getBbHeight();
            helper.assertTrue(KaijuFormService.toggle(host) == KaijuFormService.Result.TRANSFORMED, "Transforma");
            helper.assertTrue(KaijuFormService.isTransformed(host), "Estado publico ligado");
            double bonus = ServerConfig.FORM_HEALTH_BONUS.get();
            helper.assertTrue(Math.abs(host.getMaxHealth() - (humanHealth + bonus)) < 0.01,
                    "Vida da forma: " + host.getMaxHealth());
            helper.assertTrue(host.getHealth() == host.getMaxHealth(), "Nasce com a vida cheia");
            helper.assertTrue(Math.abs(host.getBbHeight() - ServerConfig.FORM_HEIGHT.get()) < 0.05,
                    "Altura do No. 8: " + host.getBbHeight() + " (humano " + humanHeight + ")");
            helper.assertTrue(host.getAttributeValue(Attributes.ARMOR) >= ServerConfig.FORM_ARMOR.get(),
                    "Armadura da forma");
            // Release nao muda a forma; o corpo treinado sim (Miguel 2026-10-24).
            PowerService.setActive(host, 60);
            helper.assertTrue(PowerService.effectiveRelease(host) == 0, "Transformado a % efetiva e 0");
            helper.assertTrue(KaijuFormService.PROFILE.equals(PowerService.auraOf(host)), "Aura do No. 8");
            PowerService.setBodyLevel(host, BodyStat.RESISTANCE, 50);
            host.tickCount = 0;
            KaijuFormService.tick(host);
            double trained = humanHealth + bonus + 50 * ServerConfig.FORM_HEALTH_PER_RESISTANCE.get();
            helper.assertTrue(Math.abs(host.getMaxHealth() - trained) < 0.01,
                    "Resistencia treinada sobe a vida: " + host.getMaxHealth());
            helper.assertTrue(KaijuFormService.toggle(host) == KaijuFormService.Result.REVERTED, "Volta");
            helper.assertTrue(Math.abs(host.getMaxHealth() - humanHealth) < 0.01
                    && host.getHealth() <= host.getMaxHealth(), "Vida humana de volta");
            helper.assertTrue(Math.abs(host.getBbHeight() - humanHeight) < 0.01, "Altura humana de volta");
            helper.assertTrue(KaijuFormService.toggle(host) == KaijuFormService.Result.COOLDOWN,
                    "Espera depois de voltar");
            helper.assertFalse(KaijuFormService.isTransformed(host), "Recusado nao transforma");
        } finally {
            PowerService.setBodyLevel(host, BodyStat.RESISTANCE, 0);
            LarvaService.clear(host);
            LarvaService.clear(human);
            data.setHost(null);
            data.setLarva(null);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_host_fists")
    public static void formFightsWithFistsAndLosesControlAfterTheLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        data.setHost(null);
        data.setLarva(null);
        FakePlayer host = player(helper, "fists");
        LarvaService.clear(host);
        host.getPersistentData().remove("kn8_form_ready");
        host.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.SWORD.get()));
        helper.assertTrue(CombatService.heldWeapon(host).map(w -> !w.item().equals(KaijuFormService.FIST))
                .orElse(false), "Humano luta com a espada");
        helper.assertTrue(LarvaService.infect(host)
                && KaijuFormService.transform(host) == KaijuFormService.Result.TRANSFORMED, "Transforma");
        helper.assertTrue(CombatService.heldWeapon(host).map(w -> w.item().equals(KaijuFormService.FIST))
                .orElse(false), "Na forma so os punhos, mesmo com a espada na mao");
        helper.assertTrue(CombatService.heldWeapon(host).flatMap(w -> w.special()).isPresent(),
                "Punho da forma com o Golpe no Chao");
        helper.assertTrue(KaijuFormService.damageFactor(host) == ServerConfig.FORM_DAMAGE_MULTIPLIER.get()
                .floatValue(), "Forca da forma pelo config, nao pelo Release");
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 6));
        float huskHealth = husk.getHealth();
        // Passou do tempo limite: no proximo tick perde o controle e vai socar quem estiver perto.
        host.getPersistentData().putLong("kn8_form_since", level.getGameTime() - ServerConfig.FORM_LIMIT_TICKS.get());
        helper.onEachTick(() -> {
            if (KaijuFormService.isTransformed(host)) {
                host.tickCount++;
                KaijuFormService.tick(host);
                CombatService.tick(host);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(KaijuFormService.isRampaging(host), "Sem controle depois do limite");
            helper.assertTrue(husk.getHealth() < huskHealth, "Sem controle soca quem esta perto");
            // Fim do surto: volta humano, exausto e com espera longa.
            host.getPersistentData().putLong("kn8_form_since", level.getGameTime()
                    - ServerConfig.FORM_LIMIT_TICKS.get() - ServerConfig.FORM_RAMPAGE_TICKS.get());
            KaijuFormService.tick(host);
            helper.assertFalse(KaijuFormService.isTransformed(host), "Volta humano no fim do surto");
            helper.assertTrue(host.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Exausto depois do surto");
            helper.assertTrue(KaijuFormService.readyTick(host) >= level.getGameTime()
                    + ServerConfig.FORM_RAMPAGE_COOLDOWN_TICKS.get(), "Espera longa depois do surto");
            LarvaService.clear(host);
            data.setHost(null);
            host.removeAllEffects();
            host.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
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
