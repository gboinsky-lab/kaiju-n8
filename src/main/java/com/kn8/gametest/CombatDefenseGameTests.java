// src/main/java/com/kn8/gametest/CombatDefenseGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.MeleeRaycast;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.core.kaiju.KaijuState;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M10b (pedidos no teste do M10b), com um jogador de servidor falso do NeoForge: limite da stamina ao
 * baixar a %, parry (janela, stamina devolvida, Yoju atordoado, critico pronto), critico consumido no primeiro
 * acerto e alcance do rifle acima de 20 blocos.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatDefenseGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String LONG_TEMPLATE = "empty_9x7x33";
    private static final BlockPos KAIJU_POS = new BlockPos(4, 1, 2);
    private static final Vec3 PLAYER_POS = new Vec3(4.5, 1.0, 6.0);
    private static final float FACING_NORTH = 180.0F;
    private static final int SETTLE_TICKS = 2;
    private static final int PARRY_WINDOW = 3;
    private static final int LIGHT_IMPACT_MARGIN = 8;
    private static final double MIN_RIFLE_DISTANCE = 20.0;
    private static final float TOLERANCE = 0.05F;

    private CombatDefenseGameTests() {
    }

    /**
     * Jogador falso limpo: faca na mao, sem bloqueio, % zerada e teto livre. Um perfil por teste: os testes do lote
     * rodam em paralelo e o FakePlayerFactory devolve a mesma instancia para o mesmo perfil (o bloqueio de um teste
     * negava o golpe do outro).
     */
    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_test:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        Vec3 position = helper.absoluteVec(PLAYER_POS);
        player.moveTo(position.x, position.y, position.z, FACING_NORTH, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.COMBAT_KNIFE.get()));
        CombatService.setBlocking(player, false);
        PowerService.setCapOverride(player, 100);
        PowerService.setTrainedRelease(player, 0);
        return player;
    }

    private static KaijuEntity kaiju(GameTestHelper helper, EntityType<KaijuEntity> type, BlockPos pos) {
        KaijuEntity kaiju = helper.spawn(type, pos);
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        return kaiju;
    }

    @GameTest(template = TEMPLATE)
    public static void staminaStaysClampedWhenReleaseDrops(GameTestHelper helper) {
        FakePlayer player = player(helper, "clamp");
        PowerService.setTrainedRelease(player, 50);
        PowerService.setStamina(player, 1000);
        PowerService.setTrainedRelease(player, 0);
        double stamina = PowerService.view(player).stamina();
        double max = PowerService.maxStamina(player);
        helper.assertTrue(stamina <= max + TOLERANCE, "Stamina " + stamina + " passou do maximo " + max);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void parryWindowRefundsStaggersAndArmsCritical(GameTestHelper helper) {
        FakePlayer player = player(helper, "parry");
        KaijuEntity spider = kaiju(helper, KN8Entities.TRICHONEPHILA.get(), KAIJU_POS);
        PowerService.setStamina(player, 50);
        CombatService.setBlocking(player, true);
        helper.assertTrue(CombatService.isParry(player), "Bloqueio recem-iniciado deveria estar na janela de parry");
        CombatService.parry(player, spider);
        helper.assertTrue(PowerService.view(player).stamina() >= 60 - TOLERANCE, "Parry deveria devolver 10");
        helper.assertTrue(CombatService.hasCriticalReady(player), "Parry deveria deixar um critico pronto");
        helper.runAfterDelay(PARRY_WINDOW + 1, () -> {
            helper.assertTrue(!CombatService.isParry(player), "Fora da janela de 3 ticks nao e mais parry");
            helper.assertTrue(spider.state() == KaijuState.STAGGER, "Yoju deveria ficar atordoado pelo parry");
            CombatService.setBlocking(player, false);
            spider.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void criticalIsConsumedOnTheFirstHit(GameTestHelper helper) {
        FakePlayer player = player(helper, "critical");
        KaijuEntity brute = kaiju(helper, KN8Entities.PRIMIGENIUS.get(), KAIJU_POS);
        float[] healthBefore = new float[1];
        // Os dois passos sao agendados aqui em cima: um runAfterDelay dentro de outro mexe no mapa de tarefas do
        // GameTest durante a iteracao e o bloco de fora roda duas vezes (o segundo golpe era negado como ocupado).
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            CombatService.parry(player, null);
            healthBefore[0] = brute.getHealth();
            helper.assertTrue(CombatService.attack(player, "light"), "O golpe leve deveria comecar");
        });
        helper.runAfterDelay(SETTLE_TICKS + LIGHT_IMPACT_MARGIN, () -> {
            helper.assertTrue(brute.getHealth() < healthBefore[0], "O golpe deveria ter acertado o kaiju");
            helper.assertTrue(!CombatService.hasCriticalReady(player), "O critico deveria ser consumido");
            brute.discard();
            helper.succeed();
        });
    }

    @GameTest(template = LONG_TEMPLATE)
    public static void rifleReachesBeyondTwentyBlocks(GameTestHelper helper) {
        KaijuEntity brute = kaiju(helper, KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 1));
        ArmorStand shooter = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(4, 1, 31));
        helper.runAfterDelay(2, () -> {
            WeaponDef rifle = KN8Data.WEAPON.server().get(KN8Constants.id("rifle"));
            helper.assertTrue(rifle != null && rifle.reach() > MIN_RIFLE_DISTANCE, "Rifle sem alcance > 20 no JSON");
            KaijuPart core = null;
            for (KaijuPart part : brute.kaijuParts()) {
                if (part.isCore()) {
                    core = part;
                }
            }
            Vec3 target = core.getBoundingBox().getCenter();
            Vec3 from = new Vec3(target.x, target.y, shooter.getZ());
            helper.assertTrue(from.distanceTo(target) > MIN_RIFLE_DISTANCE, "Teste deveria atirar de > 20 blocos");
            Optional<Entity> hit = MeleeRaycast.findTarget(helper.getLevel(), shooter, from, target.subtract(from),
                    rifle.reach());
            helper.assertTrue(hit.isPresent() && hit.get() == core, "O tiro de longe deveria acertar o nucleo");
            brute.discard();
            shooter.discard();
            helper.succeed();
        });
    }
}
