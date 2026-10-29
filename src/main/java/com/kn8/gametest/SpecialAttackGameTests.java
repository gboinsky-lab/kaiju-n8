// src/main/java/com/kn8/gametest/SpecialAttackGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.combat.WeaponHandling;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.core.kaiju.KaijuState;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.5: o ataque especial do machado (Golpe Sismico, {@code special} do axe.json) acerta todos na area a
 * frente de uma vez (kaiju e mob comum), atordoa o Yoju, poupa o soldado aliado, gasta stamina e entra em recarga; a
 * espada do Hoshina so faz o especial com uma espada em cada mao (0.5.0-D3).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialAttackGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final Vec3 PLAYER_POS = new Vec3(4.5, 1.0, 7.0);
    private static final float FACING_NORTH = 180.0F;
    private static final int SETTLE_TICKS = 2;
    /** impact_tick do special do machado (16) com folga. */
    private static final int IMPACT_MARGIN = 20;
    /** impact_tick da sequencia de cortes da faca (JSON). */
    private static final int FLURRY_FIRST_CUT = 3;

    private SpecialAttackGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_special:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_s" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        Vec3 position = helper.absoluteVec(PLAYER_POS);
        player.moveTo(position.x, position.y, position.z, FACING_NORTH, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.AXE.get()));
        CombatService.setBlocking(player, false);
        PowerService.setCapOverride(player, 100);
        PowerService.setTrainedRelease(player, 0);
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void seismicCleaveHitsEveryoneInFrontAndSparesAllies(GameTestHelper helper) {
        FakePlayer player = player(helper, "cleave");
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 3));
        spider.setNoAi(true);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 5));
        zombie.setNoAi(true);
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 5));
        soldier.setNoAi(true);
        float[] before = new float[3];
        double[] stamina = new double[1];
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            before[0] = spider.getHealth();
            before[1] = zombie.getHealth();
            before[2] = soldier.getHealth();
            helper.assertTrue(CombatService.special(player), "O especial do machado deveria comecar");
            stamina[0] = PowerService.view(player).stamina();
            helper.assertTrue(stamina[0] <= 100 - 35 + 0.5, "O especial deveria gastar a stamina do JSON (35)");
            helper.assertTrue(CombatService.specialCooldown(player) > 0, "O especial deveria entrar em recarga");
        });
        helper.runAfterDelay(SETTLE_TICKS + IMPACT_MARGIN, () -> {
            helper.assertTrue(spider.getHealth() < before[0], "A aranha na area deveria levar dano");
            helper.assertTrue(zombie.getHealth() < before[1], "O zumbi na area deveria levar dano");
            helper.assertTrue(soldier.getHealth() >= before[2], "O soldado aliado nao deveria levar dano");
            helper.assertTrue(spider.state() == KaijuState.STAGGER, "Yoju atingido deveria ficar atordoado");
            PowerService.setStamina(player, 100);
            helper.assertTrue(!CombatService.special(player), "Durante a recarga o especial deveria ser negado");
            spider.discard();
            zombie.discard();
            soldier.discard();
            helper.succeed();
        });
    }

    /**
     * COMBATE-1: a espada do Hoshina tem duas tecnicas (Kuuchi e Kosa-uchi); X troca, R usa a escolhida, cada uma com
     * a propria recarga; o Kosa-uchi solta os dois cortes do X. Arma com uma tecnica so nao troca.
     */
    @GameTest(template = TEMPLATE)
    public static void playerTechniquesCycleWithOwnCooldowns(GameTestHelper helper) {
        FakePlayer player = player(helper, "techniques");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.HOSHINA_SWORD.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(KN8Items.HOSHINA_SWORD.get()));
        int[] slashes = new int[1];
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(CombatService.heldWeapon(player).map(w -> w.allTechniques().size()).orElse(0) == 2,
                    "A espada do Hoshina deveria ter duas tecnicas");
            helper.assertTrue(CombatService.selectedTechnique(player, CombatService.heldWeapon(player).get())
                    .map(t -> t.id().equals("kuuchi")).orElse(false), "A primeira tecnica deveria ser o Kuuchi");
            helper.assertTrue(CombatService.special(player), "O Kuuchi deveria sair");
            helper.assertTrue(CombatService.techniqueCooldown(player, "kuuchi") > 0, "O Kuuchi deveria recarregar");
            helper.assertTrue(CombatService.cycleTechnique(player, 1), "X deveria trocar a tecnica");
            helper.assertTrue(CombatService.specialCooldown(player) == 0,
                    "O Kosa-uchi tem recarga propria e deveria estar pronto");
        });
        // Depois do Kuuchi (14 ticks): o Kosa-uchi sai com a recarga do Kuuchi ainda correndo.
        helper.runAfterDelay(SETTLE_TICKS + 16, () -> {
            PowerService.setStamina(player, 100);
            slashes[0] = slashCount(helper);
            helper.assertTrue(CombatService.special(player), "O Kosa-uchi deveria sair com o Kuuchi em recarga");
        });
        helper.runAfterDelay(SETTLE_TICKS + 16 + 10, () -> {
            helper.assertTrue(slashCount(helper) >= slashes[0] + 2, "O Kosa-uchi deveria soltar os dois cortes do X");
            helper.assertTrue(CombatService.cycleTechnique(player, 1), "X de novo volta ao Kuuchi");
            PowerService.setStamina(player, 100);
            helper.assertTrue(CombatService.specialCooldown(player) > 0 && !CombatService.special(player),
                    "O Kuuchi continua na propria recarga");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.COMBAT_KNIFE.get()));
            helper.assertTrue(!CombatService.cycleTechnique(player, 1), "Arma com uma tecnica so nao troca");
            helper.getLevel().getEntitiesOfClass(SlashProjectile.class, new AABB(helper.absolutePos(BlockPos.ZERO))
                    .inflate(40)).forEach(Entity::discard);
            helper.succeed();
        });
    }

    /**
     * COMBATE-3: o machado tem o estilo da Kikoru depois do Golpe Sismico; o Falling Thunder (tecnica 2) salta para
     * cima e esmaga quem esta embaixo/na frente ao cair; o Daruma Dropper (tecnica 7) da quatro cortes.
     */
    @GameTest(template = TEMPLATE)
    public static void kikoruAxeStyleLeapsAndCuts(GameTestHelper helper) {
        FakePlayer player = player(helper, "axestyle");
        // Golem (100 de vida) aguenta o esmagamento e os cortes.
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 5));
        golem.setNoAi(true);
        float[] before = new float[2];
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(CombatService.heldWeapon(player).map(w -> w.allTechniques().size()).orElse(0) == 7,
                    "O machado deveria ter o Golpe Sismico e as seis tecnicas da Kikoru");
            CombatService.cycleTechnique(player, 1);
            helper.assertTrue(CombatService.selectedTechnique(player, CombatService.heldWeapon(player).get())
                    .map(t -> t.id().equals("falling_thunder")).orElse(false), "Tecnica 2 = Falling Thunder");
            before[0] = golem.getHealth();
            helper.assertTrue(CombatService.special(player), "O Falling Thunder deveria sair");
            helper.assertTrue(player.getDeltaMovement().y > 0.3, "O Falling Thunder deveria lancar para cima");
        });
        helper.runAfterDelay(SETTLE_TICKS + 26, () -> {
            helper.assertTrue(golem.getHealth() < before[0], "Quem esta na queda deveria ser esmagado");
            golem.invulnerableTime = 0;
            // Seis passos para a frente = tecnica 7 (Daruma Dropper).
            CombatService.cycleTechnique(player, 5);
            PowerService.setStamina(player, 100);
            before[1] = golem.getHealth();
            helper.assertTrue(CombatService.special(player), "O Daruma Dropper deveria sair");
        });
        helper.runAfterDelay(SETTLE_TICKS + 26 + 24, () -> {
            float lost = before[1] - golem.getHealth();
            helper.assertTrue(lost > 0, "O Daruma Dropper deveria cortar");
            golem.discard();
            helper.succeed();
        });
    }

    private static int slashCount(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(SlashProjectile.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40)).size();
    }

    @GameTest(template = TEMPLATE)
    public static void weaponWithoutSpecialIsDenied(GameTestHelper helper) {
        FakePlayer player = player(helper, "nospecial");
        // PM-1: a faca ganhou a sequencia de cortes; a baioneta do Narumi continua sem especial do jogador.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.NARUMI_BAYONET.get()));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(!CombatService.special(player), "A baioneta nao tem ataque especial");
            helper.succeed();
        });
    }

    /** PM-1: corte em avanco da espada: fere quem esta na faixa a frente, nao quem esta ao lado nem atras de parede. */
    @GameTest(template = TEMPLATE)
    public static void swordDashCutHitsTheLineOnly(GameTestHelper helper) {
        FakePlayer player = player(helper, "dashcut");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.SWORD.get()));
        // Muro atravessando a faixa em z = 3: o zumbi de tras dele fica dentro do alcance mas sem caminho livre.
        for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            }
        }
        // Husk: o zumbi comum queima no sol do GameTest e perderia vida sem golpe nenhum.
        Husk ahead = helper.spawn(EntityType.HUSK, new BlockPos(4, 1, 5));
        Husk aside = helper.spawn(EntityType.HUSK, new BlockPos(0, 1, 5));
        Husk walled = helper.spawn(EntityType.HUSK, new BlockPos(4, 1, 2));
        for (Husk zombie : new Husk[] {ahead, aside, walled}) {
            zombie.setNoAi(true);
        }
        float[] before = new float[3];
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            before[0] = ahead.getHealth();
            before[1] = aside.getHealth();
            before[2] = walled.getHealth();
            helper.assertTrue(CombatService.special(player), "O corte em avanco deveria comecar");
        });
        helper.runAfterDelay(SETTLE_TICKS + IMPACT_MARGIN, () -> {
            helper.assertTrue(ahead.getHealth() < before[0], "O zumbi na faixa deveria levar o corte");
            helper.assertTrue(aside.getHealth() >= before[1], "O zumbi ao lado ficou fora da faixa");
            helper.assertTrue(walled.getHealth() >= before[2], "O corte nao atravessa o muro");
            ahead.discard();
            aside.discard();
            walled.discard();
            helper.succeed();
        });
    }

    /** PM-1: sequencia de cortes da faca: varios cortes seguidos no mesmo alvo (cada um zera a invulnerabilidade). */
    @GameTest(template = TEMPLATE)
    public static void knifeFlurryCutsSeveralTimes(GameTestHelper helper) {
        FakePlayer player = player(helper, "flurry");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.COMBAT_KNIFE.get()));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 5));
        golem.setNoAi(true);
        float[] health = new float[3];
        int interval = ServerConfig.FLURRY_INTERVAL_TICKS.get();
        int cuts = ServerConfig.FLURRY_HITS.get();
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            health[0] = golem.getHealth();
            helper.assertTrue(CombatService.special(player), "A sequencia de cortes deveria comecar");
        });
        // impact_tick 3 da faca: o primeiro corte sai no tick 3; os outros a cada intervalo.
        helper.runAfterDelay(SETTLE_TICKS + FLURRY_FIRST_CUT + 1, () -> health[1] = golem.getHealth());
        helper.runAfterDelay(SETTLE_TICKS + FLURRY_FIRST_CUT + cuts * interval + 2, () -> {
            health[2] = golem.getHealth();
            helper.assertTrue(health[1] < health[0], "O primeiro corte deveria ferir o golem");
            float first = health[0] - health[1];
            helper.assertTrue(health[0] - health[2] > first * (cuts - 0.5F),
                    "Todos os " + cuts + " cortes deveriam ferir (primeiro " + first + ", total "
                            + (health[0] - health[2]) + ")");
            golem.discard();
            helper.succeed();
        });
    }

    /**
     * PM-1: esquiva perfeita: no comeco da esquiva o golpe abre o contra-ataque (critico), devolve stamina e atordoa
     * o Yoju; so uma vez por esquiva e so dentro da janela.
     */
    @GameTest(template = TEMPLATE)
    public static void perfectDodgeOpensCounterOnce(GameTestHelper helper) {
        FakePlayer player = player(helper, "perfect");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.SWORD.get()));
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 3));
        spider.setNoAi(true);
        double[] stamina = new double[1];
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(!CombatService.isPerfectDodge(player), "Sem esquiva nao ha esquiva perfeita");
            helper.assertTrue(CombatService.dodge(player, 1, 0), "A esquiva deveria sair");
            helper.assertTrue(CombatService.isPerfectDodge(player), "Logo no comeco a esquiva e perfeita");
            stamina[0] = PowerService.view(player).stamina();
            CombatService.perfectDodge(player, spider, true);
            helper.assertTrue(CombatService.hasCriticalReady(player), "A esquiva perfeita abre o contra-ataque");
            helper.assertTrue(PowerService.view(player).stamina() > stamina[0], "Devolve stamina");
            helper.assertTrue(!CombatService.isPerfectDodge(player), "So uma esquiva perfeita por esquiva");
        });
        // O estado do kaiju muda no tick dele.
        helper.runAfterDelay(SETTLE_TICKS + 2, () -> helper.assertTrue(spider.state() == KaijuState.STAGGER,
                "O Yoju do golpe esquivado fica atordoado"));
        helper.runAfterDelay(SETTLE_TICKS + ServerConfig.DODGE_DURATION_TICKS.get() + 1, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(CombatService.dodge(player, -1, 0), "A segunda esquiva deveria sair");
        });
        helper.runAfterDelay(SETTLE_TICKS + ServerConfig.DODGE_DURATION_TICKS.get() + 2
                + ServerConfig.PERFECT_DODGE_TICKS.get(), () -> {
            helper.assertTrue(!CombatService.isPerfectDodge(player), "Fora da janela a esquiva nao e perfeita");
            spider.discard();
            helper.succeed();
        });
    }

    /** PM-1: clique direito na arma de fogo = tiro preciso (acao "heavy" do JSON), gasta uma bala e fere. */
    @GameTest(template = TEMPLATE)
    public static void rifleRightClickIsAPreciseShot(GameTestHelper helper) {
        FakePlayer player = player(helper, "precise");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.RIFLE.get()));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 3));
        golem.setNoAi(true);
        float[] before = new float[1];
        int[] rounds = new int[1];
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            before[0] = golem.getHealth();
            rounds[0] = WeaponHandling.rounds(player);
            helper.assertTrue(CombatService.attack(player, "heavy"), "O tiro preciso deveria sair");
        });
        helper.runAfterDelay(SETTLE_TICKS + IMPACT_MARGIN, () -> {
            helper.assertTrue(golem.getHealth() < before[0], "O tiro preciso deveria ferir o golem");
            helper.assertTrue(rounds[0] < 0 || WeaponHandling.rounds(player) == rounds[0] - 1,
                    "O tiro preciso gasta uma bala");
            golem.discard();
            helper.succeed();
        });
    }

    /** 0.5.0-D3 (Miguel): a espada do Hoshina e de par, o Kuuchi so sai com uma espada em cada mao. */
    @GameTest(template = TEMPLATE)
    public static void pairedSwordSpecialNeedsBothHands(GameTestHelper helper) {
        FakePlayer player = player(helper, "dual");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KN8Items.HOSHINA_SWORD.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.onEachTick(() -> CombatService.tick(player));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            PowerService.setStamina(player, 100);
            helper.assertTrue(!CombatService.special(player), "Com uma espada so o Kuuchi deveria ser negado");
            helper.assertTrue(CombatService.specialCooldown(player) == 0, "Negado nao entra em recarga");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(KN8Items.HOSHINA_SWORD.get()));
            helper.assertTrue(CombatService.special(player), "Com uma espada em cada mao o Kuuchi deveria sair");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.succeed();
        });
    }
}
