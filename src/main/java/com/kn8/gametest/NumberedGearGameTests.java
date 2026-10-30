package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedGearDef;
import com.kn8.common.numbered.NumberedGearService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-11A: armas numeradas do jogador. Sem patente, pesquisa ou limite pessoal a arma nao ativa (golpe
 * recusado, nada gasto); com eles ativa. O dominio sobe com o dano em kaiju, leva o dano de 70% a 100% e e salvo na
 * carreira. PM-11B: a cauda do traje Numbers 10 corta o kaiju de tras e guarda o tiro vindo de fora da frente; o
 * machado Numbers 4 plana com Shift no ar. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NumberedGearGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation NUMBERS2 = KN8Constants.id("numbers2");
    /** Folga para o saque da baioneta (perfil do machado) terminar. */
    private static final int SAQUE = 25;
    private static final ResourceLocation THEORY = KN8Constants.id("numbered_weapon_theory");

    private NumberedGearGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_numbered:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_numbered_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerService.setRank(player, KN8Constants.id("candidato"));
        CareerService.data(player).research().clear();
        CareerService.data(player).mastery().clear();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.NUMBERS2_GAUNTLETS.get()));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void numberedWeaponNeedsRankResearchAndLimit(GameTestHelper helper) {
        FakePlayer player = player(helper, "rules");
        NumberedGearDef def = KN8Data.NUMBERED_GEAR.get(NUMBERS2, false).orElseThrow();
        helper.assertTrue(NumberedGearService.blocked(player, def).orElse("").equals("rank"), "Sem patente");
        helper.assertFalse(CombatService.attack(player, "light"), "Golpe recusado sem os requisitos");
        CareerService.setRank(player, KN8Constants.id("lider_pelotao"));
        helper.assertTrue(NumberedGearService.blocked(player, def).orElse("").equals("research"), "Sem pesquisa");
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 10);
        helper.assertTrue(NumberedGearService.blocked(player, def).orElse("").equals("limit"), "Limite baixo");
        PowerService.setTrainedRelease(player, 40);
        helper.assertTrue(NumberedGearService.blocked(player, def).isEmpty(), "Com tudo, a arma ativa");
        helper.assertTrue(CombatService.attack(player, "light"), "Golpe aceito com os requisitos");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void masteryRaisesDamageAndSaves(GameTestHelper helper) {
        FakePlayer player = player(helper, "mastery");
        CareerService.setRank(player, KN8Constants.id("lider_pelotao"));
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 40);
        helper.assertTrue(Math.abs(NumberedGearService.damageFactor(player) - 0.7F) < 1.0E-4F,
                "Sem dominio a arma tira 70%");
        NumberedGearService.onKaijuDamaged(player, 1000.0F);
        int xp = NumberedGearService.xp(player, NUMBERS2);
        helper.assertTrue(xp == 1000, "1000 de dano = 1000 XP, veio " + xp);
        float mid = NumberedGearService.damageFactor(player);
        helper.assertTrue(mid > 0.7F && mid < 1.0F, "Dominio no meio do caminho: " + mid);
        NumberedGearService.onKaijuDamaged(player, 5000.0F);
        helper.assertTrue(Math.abs(NumberedGearService.damageFactor(player) - 1.0F) < 1.0E-4F,
                "Dominio completo = dano cheio");
        helper.assertTrue(NumberedGearService.extraSpecialHeat(player) == 0.0, "Dominada nao esquenta a mais");
        Tag saved = CareerData.CODEC.encodeStart(NbtOps.INSTANCE, CareerService.data(player)).getOrThrow();
        CareerData loaded = CareerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.mastery().getOrDefault(NUMBERS2, 0) == 6000, "Dominio salvo na carreira");
        // Arma comum: sem fator nem XP de dominio.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.SWORD.get()));
        helper.assertTrue(NumberedGearService.damageFactor(player) == 1.0F, "Arma comum sem mudanca");
        helper.succeed();
    }

    private static void qualify(FakePlayer player) {
        CareerService.setRank(player, KN8Constants.id("vice_capitao"));
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 50);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void numbers10TailCutsAndGuards(GameTestHelper helper) {
        FakePlayer player = player(helper, "tail");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(KN8Items.NUMBERS10_SUIT.get()));
        player.setYRot(0.0F);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 1));
        kaiju.setNoAi(true);
        DamageSource shot = new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.ARROW), helper.absoluteVec(new Vec3(4.5, 1.5, 0.5)));
        DamageSource front = new DamageSource(helper.getLevel().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW),
                helper.absoluteVec(new Vec3(4.5, 1.5, 8.5)));
        helper.runAfterDelay(2, () -> {
            float before = kaiju.getHealth();
            NumberedGearService.tick(player);
            helper.assertTrue(kaiju.getHealth() == before, "Sem os requisitos a cauda nao corta");
            helper.assertTrue(NumberedGearService.guard(player, shot, 10.0F) == 10.0F, "Sem requisitos nao guarda");
            qualify(player);
            NumberedGearService.tick(player);
            helper.assertTrue(kaiju.getHealth() < before, "A cauda corta o kaiju atras");
            float after = kaiju.getHealth();
            NumberedGearService.tick(player);
            helper.assertTrue(kaiju.getHealth() == after, "A cauda espera a recarga");
            helper.assertTrue(NumberedGearService.guard(player, front, 10.0F) == 10.0F, "Tiro de frente passa");
            float guarded = NumberedGearService.guard(player, shot, 10.0F);
            helper.assertTrue(Math.abs(guarded - 5.1F) < 0.01F, "Sem dominio a guarda tira 49%: " + guarded);
            helper.assertTrue(NumberedGearService.guard(player, shot, 10.0F) == 10.0F, "Guarda espera a recarga");
            kaiju.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void numbers4WingsGlide(GameTestHelper helper) {
        FakePlayer player = player(helper, "wings");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.NUMBERS4_AXE.get()));
        qualify(player);
        PowerService.setStamina(player, PowerService.maxStamina(player));
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 5, 4.5)));
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setOnGround(false);
        player.setShiftKeyDown(true);
        player.setDeltaMovement(0, -0.5, 0);
        NumberedGearService.tick(player);
        helper.assertTrue(NumberedGearService.gliding(player), "Shift no ar caindo abre as asas");
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "Planando cai devagar");
        helper.assertTrue(player.getDeltaMovement().z > 0.4, "Impulso para a frente: " + player.getDeltaMovement());
        player.setShiftKeyDown(false);
        NumberedGearService.tick(player);
        helper.assertFalse(NumberedGearService.gliding(player), "Soltar o Shift fecha as asas");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.AXE.get()));
        player.setShiftKeyDown(true);
        player.setDeltaMovement(0, -0.5, 0);
        player.removeAllEffects();
        NumberedGearService.tick(player);
        helper.assertFalse(NumberedGearService.gliding(player), "Machado comum nao plana");
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    /**
     * COMBATE-4 (Miguel, 2026-10-29: "pode ser usado apenas com a vida baixa, vale tanto para o player quanto para o
     * npc"): o Thunder Emperor das manoplas so sai com a vida ate 40%; o do Isao com o Numbers 2 e o do No. 9 com o
     * Isao tambem.
     */
    @GameTest(template = TEMPLATE)
    public static void thunderEmperorOnlyWithLowHealth(GameTestHelper helper) {
        FakePlayer player = player(helper, "thunder");
        CareerService.setRank(player, KN8Constants.id("lider_pelotao"));
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 40);
        PowerService.setStamina(player, 100);
        player.setHealth(player.getMaxHealth());
        helper.assertTrue(CombatService.cycleTechnique(player, -1), "Shift + X volta para a ultima tecnica");
        helper.assertTrue(CombatService.selectedTechnique(player, CombatService.heldWeapon(player).orElseThrow())
                .map(t -> t.id().equals("thunder_emperor")).orElse(false), "A ultima tecnica e o Thunder Emperor");
        helper.assertFalse(CombatService.special(player), "Com a vida cheia o Thunder Emperor e recusado");
        player.setHealth(player.getMaxHealth() * 0.3F);
        helper.assertTrue(CombatService.special(player), "Com a vida baixa o Thunder Emperor sai");
        helper.assertTrue(KN8Data.SPECIAL_SOLDIER.get(KN8Constants.id("isao"), false).map(def ->
                !def.healthAllows("thunder_emperor", 500.0F, 700.0F) && def.healthAllows("thunder_emperor", 200.0F,
                        700.0F) && def.healthAllows("force_blast", 700.0F, 700.0F)).orElse(false),
                "O Isao NPC so usa o Thunder Emperor com a vida baixa");
        helper.assertTrue(KN8Data.ABILITY.get(KN8Constants.id("no9_isao_thunder_emperor"), false)
                .map(def -> def.behavior().healthBelow() <= 0.4F).orElse(false),
                "O No. 9 com o Isao tambem");
        player.setHealth(player.getMaxHealth());
        helper.succeed();
    }

    /**
     * COMBATE-6 (Miguel: o Numbers 1 "preve o futuro" e as tecnicas sempre acertam): o Eruption Slash da baioneta do
     * Numbers 1 acerta o golem ao lado (fora da frente); o da baioneta do Narumi, sem o Numbers 1, nao.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void numbers1BayonetAlwaysHits(GameTestHelper helper) {
        FakePlayer player = player(helper, "foresight");
        CareerService.setRank(player, KN8Constants.id("vice_capitao"));
        CareerService.data(player).research().add(THEORY);
        PowerService.setTrainedRelease(player, 40);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.NARUMI_BAYONET.get()));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(2, 1, 4));
        golem.setNoAi(true);
        float[] before = new float[1];
        helper.onEachTick(() -> CombatService.tick(player));
        // Trocar a arma na mao comeca o saque, que cancela o golpe em andamento: a tecnica so depois dele.
        helper.runAfterDelay(SAQUE, () -> {
            PowerService.setStamina(player, 100);
            before[0] = golem.getHealth();
            helper.assertTrue(CombatService.selectedTechnique(player, CombatService.heldWeapon(player).orElseThrow())
                    .map(t -> t.id().equals("eruption_slash")).orElse(false), "Baioneta do Narumi: Eruption Slash");
            helper.assertTrue(CombatService.special(player), "Eruption Slash sem o Numbers 1");
        });
        helper.runAfterDelay(SAQUE + 30, () -> {
            helper.assertTrue(golem.getHealth() >= before[0], "Sem o Numbers 1, o golem ao lado nao leva");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.NUMBERS1_BAYONET.get()));
        });
        helper.runAfterDelay(2 * SAQUE + 30, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(CombatService.cycleTechnique(player, 1), "X para o Eruption Slash do Numbers 1");
            helper.assertTrue(CombatService.special(player), "Eruption Slash do Numbers 1");
        });
        helper.runAfterDelay(2 * SAQUE + 60, () -> {
            helper.assertTrue(golem.getHealth() < before[0], "Com o Numbers 1 o corte acerta: " + golem.getHealth());
            golem.discard();
            helper.succeed();
        });
    }
}
