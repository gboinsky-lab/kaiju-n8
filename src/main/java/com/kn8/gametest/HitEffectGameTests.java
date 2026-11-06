package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.HitEffects;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.HitEffect;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.soldier.special.KonomiEntity;
import com.kn8.common.world.KaijuSpawner;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Etapa 7-B (Miguel, 2026-11-06): minigun da Rin superaquece e suprime, veneno de verdade do Haruichi, marcacao do
 * Narumi, marca do Ryo no proximo golpe, analise da Konomi e escudo da Tae.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HitEffectGameTests {

    private static final String TEMPLATE = "empty_15x12x15";

    private HitEffectGameTests() {
    }

    private static KaijuEntity kaiju(GameTestHelper helper, BlockPos pos) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("primigenius"),
                helper.absolutePos(pos)).orElseThrow();
        kaiju.setNoAi(true);
        return kaiju;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_7b_efeitos")
    public static void poisonMarksAndNextHit(GameTestHelper helper) {
        KaijuEntity kaiju = kaiju(helper, new BlockPos(7, 1, 7));
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(2, 1, 2));
        husk.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            HitEffect haruichi = KN8Data.SPECIAL_SOLDIER.server().get(KN8Constants.id("haruichi"))
                    .onHit("precision_snipe");
            helper.assertTrue(haruichi.poisonTicks() == 80 && haruichi.poisonPerSecond() > 0.0F,
                    "Flecha do Haruichi com veneno de 4 s");
            HitEffects.apply(kaiju, haruichi, husk);
            helper.assertTrue(kaiju.isPoisoned(), "Kaiju envenenado");
            HitEffect narumi = KN8Data.SPECIAL_SOLDIER.server().get(KN8Constants.id("narumi")).onHit("bayonet_shot");
            HitEffects.apply(kaiju, narumi, husk);
            helper.assertTrue(Math.abs(kaiju.vulnerability() - 1.15F) < 0.001F, "Marcacao do Narumi: +15%");
            HitEffect ryo = KN8Data.SPECIAL_SOLDIER.server().get(KN8Constants.id("ryo")).onHit("passionate_shot");
            HitEffects.apply(kaiju, ryo, husk);
            helper.assertTrue(kaiju.hasNextHitMark(), "Marca do Ryo no proximo golpe");
            HitEffects.apply(kaiju, KN8Data.WEAPON.server().get(KN8Constants.id("rin_minigun")).onHit(), husk);
            helper.assertTrue(kaiju.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Supressao da minigun: Lentidao");
        });
        float[] before = new float[1];
        helper.runAfterDelay(4, () -> {
            before[0] = kaiju.getHealth();
            kaiju.invulnerableTime = 0;
            kaiju.hurt(kaiju.damageSources().mobAttack(husk), 4.0F);
            helper.assertFalse(kaiju.hasNextHitMark(), "O golpe gastou a marca do Ryo");
            before[0] = kaiju.getHealth();
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(kaiju.getHealth() < before[0] - 6.0F, "Veneno tirou vida: " + before[0] + " -> "
                    + kaiju.getHealth());
            kaiju.discard();
            husk.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "kn8_7b_konomi")
    public static void konomiAnalysisMarksKaiju(GameTestHelper helper) {
        KonomiEntity konomi = helper.spawn(KN8Entities.KONOMI.get(), new BlockPos(3, 1, 3));
        KaijuEntity kaiju = kaiju(helper, new BlockPos(10, 1, 10));
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(kaiju.vulnerability() - 1.08F) < 0.001F, "Analise da Konomi: +8%");
            konomi.discard();
            kaiju.discard();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "kn8_7b_tae")
    public static void taeShieldsAndHealsAllies(GameTestHelper helper) {
        HoshinaEntity tae = helper.spawn(KN8Entities.TAE.get(), new BlockPos(5, 1, 3));
        SoldierEntity ally = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 4));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(5, 1, 9));
        tae.setNoAi(true);
        ally.setNoAi(true);
        golem.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(tae.startTechnique("heat_grenade", golem), "Granada de Calor comecou");
            helper.assertTrue(ally.hasEffect(MobEffects.ABSORPTION) && ally.hasEffect(MobEffects.REGENERATION),
                    "Aliado com escudo e cura");
            helper.assertTrue(tae.hasEffect(MobEffects.ABSORPTION), "Tae com escudo");
            tae.discard();
            ally.discard();
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260, batch = "kn8_7b_minigun")
    public static void minigunFiresAutomaticallyAndOverheats(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("kn8_7b:minigun".getBytes(
                StandardCharsets.UTF_8)), "kn8_7b_minigun");
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(7, 1, 7)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.RIN_MINIGUN.get()));
        int[] shots = new int[1];
        helper.onEachTick(() -> {
            CombatService.tick(player);
            if (!CombatService.overheated(player) && CombatService.attack(player, "light")) {
                shots[0]++;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(CombatService.overheated(player), "A minigun deveria superaquecer (" + shots[0]
                    + " tiros)");
            helper.assertTrue(shots[0] >= 40 && shots[0] <= 60, "Uns 50 tiros em 5 s antes de superaquecer: "
                    + shots[0]);
            helper.assertFalse(CombatService.attack(player, "light"), "Superaquecida nao atira");
        });
    }
}
