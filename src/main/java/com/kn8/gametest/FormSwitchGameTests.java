package com.kn8.gametest;

import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.soldier.special.KafkaEntity;
import com.kn8.common.soldier.special.KaijuNo8Entity;
import com.kn8.core.soldier.SquadRules;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da MOV-5A (troca de perfil, bug 10 da MOV-0): o Kafka vira o Kaiju No. 8 e volta, e o estado passa de uma
 * forma para a outra (esquadrao, posto, kaiju designado, dados persistentes, municao do Kafka) sem herdar tecnicas nem
 * o papel de atirador.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FormSwitchGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final int SETTLE = 3;
    private static final int ROUNDS = 7;

    private FormSwitchGameTests() {
    }

    private static <T extends HoshinaEntity> List<T> around(GameTestHelper helper, Class<T> type) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        return helper.getLevel().getEntitiesOfClass(type, box, HoshinaEntity::isAlive);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 500, batch = "kn8_mov5a_form")
    public static void formSwitchKeepsSquadAndAmmoWithoutSharingAbilities(GameTestHelper helper) {
        KafkaEntity kafka = helper.spawn(KN8Entities.KAFKA.get(), new BlockPos(4, 1, 4));
        kafka.setNoAi(true);
        UUID leader = UUID.randomUUID();
        UUID kaiju = UUID.randomUUID();
        int[] reserve = new int[1];
        BlockPos[] post = new BlockPos[1];
        KaijuNo8Entity[] no8 = new KaijuNo8Entity[1];
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kafka.getMainHandItem().is(KN8Items.RIFLE.get()), "Kafka com o rifle");
            kafka.joinSquad(leader);
            kafka.setSquadOrder(SquadRules.Order.STAY);
            post[0] = kafka.squadPost();
            kafka.setAssignedKaiju(kaiju);
            kafka.getPersistentData().putBoolean(InvasionService.TAG, true);
            kafka.setSpareMagazines(1);
            kafka.setRounds(ROUNDS);
            reserve[0] = kafka.reserveRounds();
            kafka.setHealth(kafka.getMaxHealth() * 0.3F);
        });
        helper.succeedWhen(() -> {
            if (no8[0] == null) {
                helper.assertTrue(kafka.isRemoved(), "O Kafka deveria virar o No. 8");
                List<KaijuNo8Entity> found = around(helper, KaijuNo8Entity.class);
                helper.assertTrue(!found.isEmpty(), "O No. 8 deveria surgir");
                KaijuNo8Entity next = found.get(0);
                next.setNoAi(true);
                helper.assertTrue(next.squadLeader().filter(leader::equals).isPresent(), "No. 8 no mesmo esquadrao");
                helper.assertTrue(next.squadOrder() == SquadRules.Order.STAY, "No. 8 com a mesma ordem");
                helper.assertTrue(post[0].equals(next.squadPost()), "No. 8 com o mesmo posto");
                helper.assertTrue(kaiju.equals(next.assignedKaiju()), "No. 8 com o mesmo kaiju designado");
                helper.assertTrue(next.getPersistentData().getBoolean(InvasionService.TAG), "Marca da invasao");
                helper.assertTrue(next.activeProfileId().getPath().equals("kaiju_no8"), "Perfil ativo do No. 8");
                helper.assertTrue(!next.isShooter(), "O No. 8 nao luta como atirador");
                helper.assertTrue(!next.getMainHandItem().is(KN8Items.RIFLE.get()), "O No. 8 nao fica com o rifle");
                helper.assertTrue(next.storedForms().contains("kn8:kafka"), "Arma do Kafka guardada");
                helper.assertTrue(!next.startTechnique("punch_combo", next), "O No. 8 nao herda tecnica do Kafka");
                no8[0] = next;
                helper.fail("esperando o No. 8 voltar a ser o Kafka");
            }
            helper.assertTrue(no8[0].isRemoved(), "Sem alvo, o No. 8 deveria voltar a ser o Kafka");
            List<KafkaEntity> back = around(helper, KafkaEntity.class);
            helper.assertTrue(!back.isEmpty(), "O Kafka deveria voltar");
            KafkaEntity again = back.get(0);
            helper.assertTrue(again.getMainHandItem().is(KN8Items.RIFLE.get()), "Kafka de volta com o rifle");
            helper.assertTrue(again.rounds() == ROUNDS, "Pente do Kafka mantido: " + again.rounds());
            helper.assertTrue(again.reserveRounds() == reserve[0], "Reserva do Kafka mantida: "
                    + again.reserveRounds() + " de " + reserve[0]);
            helper.assertTrue(again.squadLeader().filter(leader::equals).isPresent(), "Kafka no mesmo esquadrao");
            helper.assertTrue(post[0].equals(again.squadPost()), "Kafka com o mesmo posto");
            helper.assertTrue(again.activeProfileId().getPath().equals("kafka"), "Perfil ativo do Kafka");
            helper.assertTrue(!again.storedForms().contains("kn8:kafka"), "Memoria do Kafka usada");
            helper.assertTrue(again.storedForms().contains("kn8:kaiju_no8"), "Memoria do No. 8 guardada");
            again.setNoAi(true);
            helper.assertTrue(!again.startTechnique("heavy_punch", again), "O Kafka nao herda tecnica do No. 8");
            again.discard();
        });
    }
}
