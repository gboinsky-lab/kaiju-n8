// src/main/java/com/kn8/gametest/ParkourGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.parkour.ParkourMove;
import com.kn8.common.parkour.ParkourService;
import com.kn8.common.parkour.ParkourState;
import com.kn8.core.parkour.ParkourMath;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Parkour do jogador (0.5.0-E): o servidor confere a geometria e a stamina de cada intencao. Jogador falso virado
 * para o norte (-Z) no meio da area; a direita dele e +X.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParkourGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final float FACING_NORTH = 180.0F;

    private ParkourGameTests() {
    }

    /** Um perfil por teste (os testes do lote rodam em paralelo e o perfil repetido devolve a mesma instancia). */
    private static FakePlayer player(GameTestHelper helper, String name, double y, boolean onGround) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_parkour:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8p_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        Vec3 position = helper.absoluteVec(new Vec3(4.5, y, 4.5));
        player.moveTo(position.x, position.y, position.z, FACING_NORTH, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(onGround);
        player.setSprinting(true);
        player.setShiftKeyDown(false);
        player.setForcedPose(null);
        player.resetFallDistance();
        PowerService.setStamina(player, 1000);
        ParkourService.reset(player);
        return player;
    }

    private static void column(GameTestHelper helper, int x, int z, int height) {
        for (int y = 1; y <= height; y++) {
            helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        }
    }

    /** Muro de 1 bloco a frente, correndo: vault com impulso que passa do muro e gasta stamina. */
    @GameTest(template = TEMPLATE)
    public static void vaultClearsOneBlockWall(GameTestHelper helper) {
        column(helper, 4, 3, 1);
        FakePlayer player = player(helper, "vault", 1.0, true);
        double before = PowerService.view(player).stamina();
        ParkourService.handle(player, ParkourMove.VAULT, 0);
        Vec3 velocity = player.getDeltaMovement();
        helper.assertTrue(ParkourMath.peakHeight(velocity.y) >= 1.0, "O vault deveria passar de 1 bloco: " + velocity);
        helper.assertTrue(velocity.z < 0.0, "O vault deveria ir para a frente (norte): " + velocity);
        helper.assertTrue(PowerService.view(player).stamina() < before, "O vault deveria gastar stamina");
        helper.succeed();
    }

    /** Muro de 2 blocos, sem correr: escalada curta que chega acima de 2 blocos. */
    @GameTest(template = TEMPLATE)
    public static void climbReachesTwoBlockWall(GameTestHelper helper) {
        column(helper, 4, 3, 2);
        FakePlayer player = player(helper, "climb", 1.0, true);
        player.setSprinting(false);
        ParkourService.handle(player, ParkourMove.CLIMB, 0);
        double peak = ParkourMath.peakHeight(player.getDeltaMovement().y);
        helper.assertTrue(peak >= 2.0, "A escalada deveria subir 2 blocos, sobe " + peak);
        helper.succeed();
    }

    /** Muro de 4 blocos: alto demais, nada acontece. */
    @GameTest(template = TEMPLATE)
    public static void tooTallWallIsRefused(GameTestHelper helper) {
        column(helper, 4, 3, 4);
        FakePlayer player = player(helper, "tall", 1.0, true);
        ParkourService.handle(player, ParkourMove.CLIMB, 0);
        helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6, "Muro de 4 blocos nao se escala");
        helper.succeed();
    }

    /** Wall-run so com parede ao lado; o wall-jump empurra para longe dela (-X) e encerra o estado. */
    @GameTest(template = TEMPLATE)
    public static void wallRunAndWallJump(GameTestHelper helper) {
        FakePlayer player = player(helper, "wallrun", 2.0, false);
        ParkourService.handle(player, ParkourMove.WALL_RUN, 1);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "Sem parede nao ha wall-run");
        column(helper, 5, 4, 4);
        ParkourService.handle(player, ParkourMove.WALL_RUN, 1);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.WALL_RUN,
                "Com parede a direita deveria correr nela");
        ParkourService.handle(player, ParkourMove.WALL_JUMP, 0);
        Vec3 velocity = player.getDeltaMovement();
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "O wall-jump solta a parede");
        helper.assertTrue(velocity.x < -0.1 && velocity.y > 0.3, "O wall-jump vai para longe e para cima: "
                + velocity);
        helper.succeed();
    }

    /** Borda a 1,5 bloco acima dos pes: segura; pular sobe com impulso suficiente. */
    @GameTest(template = TEMPLATE)
    public static void ledgeGrabAndClimb(GameTestHelper helper) {
        column(helper, 4, 3, 2);
        FakePlayer player = player(helper, "ledge", 1.5, false);
        ParkourService.handle(player, ParkourMove.LEDGE_GRAB, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.LEDGE_HANG,
                "Deveria segurar a borda a 1,5 bloco");
        ParkourService.handle(player, ParkourMove.LEDGE_CLIMB, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "Subir solta a borda");
        helper.assertTrue(ParkourMath.peakHeight(player.getDeltaMovement().y) >= 1.5,
                "Subir a borda deveria levar acima dela: " + player.getDeltaMovement());
        helper.succeed();
    }

    /** Slide: pose deitada enquanto dura; parar devolve a pose livre. */
    @GameTest(template = TEMPLATE)
    public static void slideLaysThePlayerDown(GameTestHelper helper) {
        FakePlayer player = player(helper, "slide", 1.0, true);
        ParkourService.handle(player, ParkourMove.SLIDE, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.SLIDE, "Correndo, o slide comeca");
        helper.assertTrue(player.getForcedPose() == Pose.SWIMMING, "Slide deita o jogador");
        ParkourService.handle(player, ParkourMove.STOP, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "STOP encerra o slide");
        helper.assertTrue(player.getForcedPose() == null, "Ao levantar a pose volta a ser livre");
        helper.succeed();
    }

    /** Sem stamina o salto longo e recusado. */
    @GameTest(template = TEMPLATE)
    public static void longJumpNeedsStamina(GameTestHelper helper) {
        FakePlayer player = player(helper, "longjump", 1.0, true);
        PowerService.setStamina(player, 0);
        ParkourService.handle(player, ParkourMove.LONG_JUMP, 0);
        helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6, "Sem stamina nao ha salto longo");
        PowerService.setStamina(player, 1000);
        ParkourService.handle(player, ParkourMove.LONG_JUMP, 0);
        helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 0.5, "Com stamina o salto longo sai");
        helper.succeed();
    }

    /** Rolamento: Shift na queda de 6 blocos zera o dano; de 10, corta 60%; sem Shift, nada. */
    @GameTest(template = TEMPLATE)
    public static void rollCutsFallDamage(GameTestHelper helper) {
        FakePlayer player = player(helper, "roll", 1.0, true);
        helper.assertTrue(ParkourService.onFall(player, 6.0F) == 1.0F, "Sem Shift nao rola");
        player.setShiftKeyDown(true);
        helper.assertTrue(ParkourService.onFall(player, 6.0F) == 0.0F, "Rolando de 6 blocos nao ha dano");
        float big = ParkourService.onFall(player, 10.0F);
        helper.assertTrue(Math.abs(big - 0.4F) < 1.0E-4, "Rolando de 10 blocos o dano cai 60%: " + big);
        helper.succeed();
    }

    /** A parede some: o servidor encerra o wall-slide depois da folga. */
    @GameTest(template = TEMPLATE)
    public static void wallSlideEndsWithoutWall(GameTestHelper helper) {
        column(helper, 4, 3, 4);
        FakePlayer player = player(helper, "wallslide", 2.0, false);
        ParkourService.handle(player, ParkourMove.WALL_SLIDE, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.WALL_SLIDE, "De frente para a parede");
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(4, y, 3), Blocks.AIR);
        }
        helper.runAfterDelay(12, () -> {
            ParkourService.tick(player);
            helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE,
                    "Sem parede o wall-slide acaba");
            helper.succeed();
        });
    }

    /**
     * FIX-3 (Miguel: "escalar predios"): de frente para um muro alto, no chao, escala; gasta stamina por segundo;
     * sem parede e recusado; no topo a borda e agarrada sem a tecla e subir leva acima dela.
     */
    @GameTest(template = TEMPLATE)
    public static void wallClimbUpABuildingAndOverTheTop(GameTestHelper helper) {
        column(helper, 4, 3, 4);
        FakePlayer player = player(helper, "wallclimb", 1.0, true);
        player.setSprinting(false);
        player.setYRot(0.0F);
        ParkourService.handle(player, ParkourMove.WALL_CLIMB, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "Sem parede a frente, nada");
        player.setYRot(FACING_NORTH);
        ParkourService.handle(player, ParkourMove.WALL_CLIMB, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.CLIMB, "De frente para o muro, escala");
        double before = PowerService.view(player).stamina();
        for (int i = 0; i < 20; i++) {
            ParkourService.tick(player);
        }
        helper.assertTrue(PowerService.view(player).stamina() < before, "Escalar gasta stamina por segundo");
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.CLIMB, "Com o muro, continua escalando");
        // Perto do topo (muro ate y 5, pes em 3,6): a borda fica na altura das maos e e agarrada sem a tecla (o teto do
        // template fica em 7).
        Vec3 high = helper.absoluteVec(new Vec3(4.5, 3.6, 4.5));
        player.moveTo(high.x, high.y, high.z, FACING_NORTH, 0.0F);
        player.setOnGround(false);
        ParkourService.handle(player, ParkourMove.LEDGE_GRAB, -1);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.LEDGE_HANG,
                "No topo da escalada a borda e agarrada");
        ParkourService.handle(player, ParkourMove.LEDGE_CLIMB, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "Subir solta a borda");
        helper.assertTrue(ParkourMath.peakHeight(player.getDeltaMovement().y) >= 1.4,
                "Subir a borda do topo leva acima dela: " + player.getDeltaMovement());
        helper.succeed();
    }

    /** FIX-3: escalando, Espaco pula para longe da parede (o wall-jump vale na escalada). */
    @GameTest(template = TEMPLATE)
    public static void wallClimbJumpsAwayFromTheWall(GameTestHelper helper) {
        column(helper, 4, 3, 5);
        FakePlayer player = player(helper, "climbjump", 2.0, false);
        player.setSprinting(false);
        ParkourService.handle(player, ParkourMove.WALL_CLIMB, 0);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.CLIMB, "No ar de frente, escala");
        ParkourService.handle(player, ParkourMove.WALL_JUMP, 0);
        Vec3 velocity = player.getDeltaMovement();
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.NONE, "O pulo solta a parede");
        helper.assertTrue(velocity.z > 0.2 && velocity.y > 0.2, "Pula para tras (sul) e para cima: " + velocity);
        helper.succeed();
    }

    /** FIX-3: borda agarrada sem a tecla tambem caindo de um pulo; pendurado, a velocidade de lado vem do servidor. */
    @GameTest(template = TEMPLATE)
    public static void autoLedgeGrabWhileFalling(GameTestHelper helper) {
        column(helper, 4, 3, 3);
        FakePlayer player = player(helper, "autoledge", 2.4, false);
        player.setDeltaMovement(0.0, -0.3, 0.0);
        ParkourService.handle(player, ParkourMove.LEDGE_GRAB, -1);
        helper.assertTrue(ParkourService.data(player).state() == ParkourState.LEDGE_HANG,
                "Caindo de frente para a borda, agarra sem a tecla");
        helper.succeed();
    }
}
