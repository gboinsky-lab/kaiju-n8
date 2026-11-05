package com.kn8.gametest;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.data.def.SlashSpec;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * AJUSTES-2 (decisoes do Miguel de 2026-11-05): No. 8 sem controle como kaiju inimigo (pequeno de 3 m e colossal de
 * 22 m, arte do No. 8 com a textura vermelha), aviso de ameaca grande nas invasoes com numerado e som do voo dos
 * projeteis.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AjustesTwoGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private AjustesTwoGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_ajustes2_rampage")
    public static void rampageNo8IsARedEnemyKaijuInTwoSizes(GameTestHelper helper) {
        KaijuEntity small = helper.spawn(KN8Entities.KAIJU_NO8_RAMPAGE.get(), new BlockPos(2, 1, 4));
        KaijuEntity giant = helper.spawn(KN8Entities.KAIJU_NO8_RAMPAGE_GIANT.get(), new BlockPos(6, 1, 4));
        small.setNoAi(true);
        giant.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            for (KaijuEntity kaiju : List.of(small, giant)) {
                ResourceLocation id = kaiju.kaijuId();
                helper.assertTrue(KN8Entities.modelSpecies(id).equals(KN8Constants.id("kaiju_no8")),
                        "Arte do No. 8: " + id);
                helper.assertTrue(KN8Entities.textureSpecies(id).equals(KN8Constants.id("kaiju_no8_rampage")),
                        "Textura vermelha: " + id);
                KaijuDef def = kaiju.def().orElseThrow();
                helper.assertTrue(def.kaijuClass() == KaijuClass.NUMBERED, "Numerado: " + id);
                helper.assertTrue(def.abilities().size() == 6, "Seis golpes do No. 8: " + def.abilities());
                def.abilities().forEach(ability -> helper.assertTrue(KN8Data.ABILITY.get(ability, false)
                        .isPresent(), "Golpe sem dados: " + ability));
            }
            helper.assertTrue(Math.abs(small.getBbHeight() - 3.0F) < 0.05F, "Pequeno com 3 m: " + small
                    .getBbHeight());
            helper.assertTrue(Math.abs(giant.getBbHeight() - 22.0F) < 0.2F, "Gigante com 22 m: " + giant
                    .getBbHeight());
            helper.assertTrue(small.getMaxHealth() == 1800.0F && giant.getMaxHealth() == 4500.0F,
                    "Vida " + small.getMaxHealth() + " / " + giant.getMaxHealth());
            small.discard();
            giant.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3")
    public static void bigInvasionsWithNumberedSoundTheMajorThreat(GameTestHelper helper) {
        for (String big : List.of("no10_assault", "mass_resurrection", "ariake_assault", "tachikawa_raid",
                "shinagawa_ants")) {
            helper.assertTrue(InvasionService.isMajorThreat(KN8Data.INVASION.server().get(KN8Constants.id(big))),
                    "Sem aviso de ameaca grande: " + big);
        }
        for (String small : List.of("honju_assault", "no9_resurrection", "spider_swarm", "diclonius_attack",
                "kaiju_horde")) {
            helper.assertFalse(InvasionService.isMajorThreat(KN8Data.INVASION.server().get(KN8Constants.id(small))),
                    "Aviso de ameaca grande sem precisar: " + small);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_ajustes2_travel")
    public static void projectilesCarryTheirFlightSound(GameTestHelper helper) {
        HoshinaEntity hoshina = helper.spawnWithNoFreeWill(KN8Entities.HOSHINA.get(), new BlockPos(1, 2, 1));
        HoshinaEntity mina = helper.spawnWithNoFreeWill(KN8Entities.MINA.get(), new BlockPos(3, 2, 1));
        HoshinaEntity isao = helper.spawnWithNoFreeWill(KN8Entities.ISAO.get(), new BlockPos(5, 2, 1));
        HoshinaEntity reno = helper.spawnWithNoFreeWill(KN8Entities.RENO.get(), new BlockPos(7, 2, 1));
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(4, 1, 6));
        honju.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            SlashSpec arc = new SlashSpec(1, 0.0F, 0.0F, 1.0F, 20.0F, 1.2F, 0xB070FF, false, 0.0F, 0, 1, 0, 1.0F,
                    0.0F);
            SlashSpec shell = new SlashSpec(1, 0.0F, 0.0F, 2.5F, 44.0F, 0.6F, 0x3FCF5A, true, 2.5F, 0, 1, 0, 1.0F,
                    0.0F);
            SlashSpec bullet = new SlashSpec(1, 0.0F, 0.0F, 3.0F, 40.0F, 0.2F, 0xFFFFFF, true, 0.0F, 0, 1, 0, 1.0F,
                    0.0F);
            Vec3 up = new Vec3(0, 1, 0);
            SlashProjectile.fire(hoshina, up, arc, 1.0F, 1.0F, 0, 0.0F, "kuuchi");
            SlashProjectile.fire(mina, up, shell, 1.0F, 1.0F, 0, 0.0F, "cannon_shot");
            SlashProjectile.fire(isao, up, arc, 1.0F, 1.0F, 0, 0.0F, "spiral_blast");
            SlashProjectile.fire(reno, up, bullet, 1.0F, 1.0F, 0, 0.0F, "precision_shot");
            assertTravel(helper, hoshina, Optional.of("travel.slash"));
            assertTravel(helper, mina, Optional.of("travel.shell"));
            assertTravel(helper, isao, Optional.of("travel.spiral"));
            assertTravel(helper, reno, Optional.empty());
            ResourceLocation beamId = KN8Constants.id("energy_blast");
            AbilityDef beam = KN8Data.ABILITY.get(beamId, false).orElseThrow();
            KaijuProjectile shot = KaijuProjectile.shoot(honju, beamId, beam, honju.getEyePosition(), up, 1.0F);
            helper.assertTrue(shot.travelSound().map(KN8Constants.id("travel.energy_beam")::equals).orElse(false),
                    "Raio do Honju com zumbido no voo: " + shot.travelSound());
            shot.discard();
            helper.getLevel().getEntitiesOfClass(SlashProjectile.class, honju.getBoundingBox().inflate(16.0))
                    .forEach(SlashProjectile::discard);
            honju.discard();
            helper.succeed();
        });
    }

    private static void assertTravel(GameTestHelper helper, HoshinaEntity owner, Optional<String> expected) {
        SlashProjectile shot = helper.getLevel().getEntitiesOfClass(SlashProjectile.class,
                owner.getBoundingBox().inflate(8.0)).stream().filter(slash -> slash.getOwner() == owner)
                .findFirst().orElseThrow();
        Optional<ResourceLocation> wanted = expected.map(KN8Constants::id);
        helper.assertTrue(shot.travelSound().equals(wanted), "Voo do tiro de " + owner.activeProfileId() + ": "
                + shot.travelSound() + " (esperado " + wanted + ")");
    }
}
