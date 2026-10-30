package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.ExamKit;
import com.kn8.common.career.ExamTargets;
import com.kn8.common.career.MissionService;
import com.kn8.common.combat.MagazineItem;
import com.kn8.common.combat.WeaponHandling;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da EXAME (Miguel, 2026-10-30): o exame de admissao so e aceito no Campo do Exame de Admissao, entrega
 * traje, armas com pentes cheios, municao e itens no inicio e os recolhe no fim; os soldados nao lutam com os kaiju
 * da prova de outro jogador. A estrutura gerada (e o local) so pelo teste em jogo: o GameTest nao tem estrutura.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdmissionExamGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation EXAM = KN8Constants.id("exam");

    private AdmissionExamGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_admission:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_adm_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        player.getInventory().clearContent();
        CareerService.data(player).active().clear();
        CareerService.data(player).completed().clear();
        CareerService.data(player).cooldowns().clear();
        CareerService.setRank(player, KN8Constants.id("candidato"));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void admissionExamOnlyAtTheExamGrounds(GameTestHelper helper) {
        FakePlayer player = player(helper, "site");
        DefenseOfficerEntity officer = helper.spawn(KN8Entities.DEFENSE_OFFICER.get(), new BlockPos(5, 1, 4));
        officer.setGiver(DefenseOfficerEntity.EXAM_OFFICER);
        MissionDef exam = KN8Data.MISSION.server().get(EXAM);
        helper.assertTrue(exam != null && exam.arena().flatMap(MissionDef.Arena::site).isPresent(),
                "O exame de admissao deveria ter local proprio");
        helper.assertTrue(MissionService.canAccept(player, EXAM) == MissionService.Result.NEEDS_SITE,
                "Fora do Campo do Exame a prova deveria ser recusada: " + MissionService.canAccept(player, EXAM));
        helper.assertTrue(MissionService.canAccept(player, KN8Constants.id("course_strength"))
                == MissionService.Result.OK, "Curso sem local continua aceito com o Oficial de Provas");
        officer.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void examKitIsIssuedFullAndReturned(GameTestHelper helper) {
        FakePlayer player = player(helper, "kit");
        MissionDef exam = KN8Data.MISSION.server().get(EXAM);
        MissionDef.Arena arena = exam.arena().orElseThrow();
        ItemStack own = new ItemStack(Items.BREAD, 3);
        player.getInventory().add(own);
        int given = ExamKit.give(player, EXAM, arena.kit());
        helper.assertTrue(given >= 9, "Kit deveria ter traje, armas, pentes e itens: " + given);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(KN8Items.MK1.get()),
                "O traje deveria ser vestido");
        int magazines = 0;
        int capacity = WeaponHandling.magazineCapacity(KN8Items.RIFLE_MAGAZINE.get(), false);
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(KN8Items.RIFLE_MAGAZINE.get())) {
                magazines++;
                helper.assertTrue(capacity > 0 && MagazineItem.rounds(stack) == capacity,
                        "Pente do kit deveria vir cheio: " + MagazineItem.rounds(stack) + "/" + capacity);
                helper.assertTrue(ExamKit.missionOf(stack).isPresent(), "Pente do kit sem a marca");
            }
        }
        helper.assertTrue(magazines == 3, "Esperava 3 pentes, veio " + magazines);
        helper.assertTrue(player.getInventory().countItem(KN8Items.RIFLE.get()) == 1, "Rifle ausente");
        int taken = ExamKit.take(player, EXAM);
        helper.assertTrue(taken == given, "Recolheu " + taken + " de " + given);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "Traje do kit deveria voltar");
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) == 3,
                "O pao do proprio jogador fica (so o do kit sai): " + player.getInventory().countItem(Items.BREAD));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "kn8_admission_soldier")
    public static void soldiersLeaveExamKaijuToTheCandidate(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 2));
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(6, 1, 6));
        kaiju.setNoAi(true);
        ExamTargets.mark(kaiju, UUID.randomUUID(), EXAM);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(soldier.getTarget() != kaiju, "Soldado nao deveria mirar o kaiju da prova de outro");
            ExamTargets.unmark(kaiju);
        });
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(soldier.getTarget() == kaiju, "Sem a marca o soldado deveria mirar o kaiju");
            kaiju.discard();
            soldier.discard();
            helper.succeed();
        });
    }
}
