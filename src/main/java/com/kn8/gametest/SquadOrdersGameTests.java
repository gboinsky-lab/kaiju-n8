// src/main/java/com/kn8/gametest/SquadOrdersGameTests.java
package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.SquadService;
import com.kn8.core.soldier.SquadRules;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-6: ordens novas do esquadrao. "Focar" prende todos no kaiju que o lider mira (nao no mais perto)
 * e volta a seguir quando ele cai; "guarda" luta so dentro da area, primeiro com o kaiju mais dentro dela. Lotes
 * proprios: a deteccao de 64 blocos pegaria kaiju dos testes vizinhos.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SquadOrdersGameTests {

    private static final String TEMPLATE = "empty_15x12x15";

    private SquadOrdersGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_squad_pm6:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(7.5, 1, 7.5)));
        return player;
    }

    private static void forceChunks(ServerLevel level, BlockPos origin, boolean forced) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, forced);
            }
        }
    }

    /** Kaiju pequeno, parado e que nao morre (Resistencia V): continua sendo inimigo (invulneravel nao seria). */
    private static KaijuEntity dummy(GameTestHelper helper, BlockPos pos) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.KAIJU_LARVA.get(), pos);
        kaiju.setNoAi(true);
        kaiju.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2000, 4, false, false));
        return kaiju;
    }

    /**
     * Focar: o soldado fica no kaiju mirado mesmo com outro mais perto dele; salvo e carregado, continua focado; com o
     * kaiju fora, o esquadrao volta a seguir. Sem kaiju na mira, a ordem nao muda.
     */
    @GameTest(template = TEMPLATE, batch = "kn8_squad_focus", timeoutTicks = 200)
    public static void focusHoldsTheAimedKaiju(GameTestHelper helper) {
        FakePlayer player = player(helper, "focus");
        KaijuEntity aimed = dummy(helper, new BlockPos(13, 1, 13));
        KaijuEntity near = dummy(helper, new BlockPos(2, 1, 4));
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(2, 1, 2));
        soldier.setVariant("rifle");
        soldier.joinSquad(player.getUUID());
        player.lookAt(EntityAnchorArgument.Anchor.EYES, aimed.getBoundingBox().getCenter());
        player.setXRot(-60.0F);
        SquadService.order(player, SquadRules.Order.FOCUS);
        helper.assertTrue(soldier.squadOrder() == SquadRules.Order.FOLLOW,
                "Mirando o ceu nao ha alvo: a ordem nao deveria mudar");
        player.lookAt(EntityAnchorArgument.Anchor.EYES, aimed.getBoundingBox().getCenter());
        helper.assertTrue(SquadService.aimedKaiju(player) == aimed, "A mira deveria achar o kaiju mirado");
        SquadService.order(player, SquadRules.Order.FOCUS);
        helper.assertTrue(soldier.squadOrder() == SquadRules.Order.FOCUS
                && aimed.getUUID().equals(soldier.assignedKaiju()), "O soldado deveria focar o kaiju mirado");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(soldier.getTarget() == aimed, "Focado, nao troca pelo kaiju mais perto: "
                    + soldier.getTarget());
            CompoundTag tag = soldier.saveWithoutId(new CompoundTag());
            SoldierEntity copy = KN8Entities.SOLDIER.get().create(helper.getLevel());
            copy.load(tag);
            helper.assertTrue(copy.squadOrder() == SquadRules.Order.FOCUS
                    && aimed.getUUID().equals(copy.assignedKaiju()), "Salvo e carregado, continua focado");
            copy.discard();
            aimed.discard();
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(soldier.squadOrder() == SquadRules.Order.FOLLOW && soldier.assignedKaiju() == null,
                    "Com o kaiju fora, o esquadrao deveria voltar a seguir");
            helper.assertTrue(soldier.getTarget() == near, "Seguindo de novo, luta com o kaiju por perto");
            List.of(near, soldier).forEach(Entity::discard);
            helper.succeed();
        });
    }

    /**
     * Guarda: entre dois kaiju na area vale o mais dentro dela (nao o mais perto do soldado); o de fora da area nunca
     * e alvo, nem com a area vazia.
     */
    @GameTest(template = TEMPLATE, batch = "kn8_squad_guard", timeoutTicks = 260)
    public static void guardFightsInsideTheArea(GameTestHelper helper) {
        FakePlayer player = player(helper, "guard");
        // Sem jogador, so os chunks da estrutura do teste rodam: o soldado e os kaiju a 20-30 blocos ficariam
        // parados (achado: o soldado nunca escolhia alvo quando caia num chunk sem tick).
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(7, 1, 7));
        forceChunks(level, origin, true);
        KaijuEntity deep = dummy(helper, new BlockPos(7, 1, 9));
        KaijuEntity edge = dummy(helper, new BlockPos(7, 1, 19));
        KaijuEntity outside = dummy(helper, new BlockPos(7, 1, 30));
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 21));
        soldier.setVariant("rifle");
        soldier.joinSquad(player.getUUID());
        SquadService.order(player, SquadRules.Order.GUARD);
        helper.assertTrue(soldier.squadOrder() == SquadRules.Order.GUARD && soldier.squadPost() != null
                && soldier.squadPost().equals(player.blockPosition()), "A guarda e em volta do lider");
        helper.assertTrue(soldier.squadRole() == SquadRules.Role.SHOOTER, "Com rifle, a funcao e atirador");
        // Kaiju recem-criado tem vida 0 ate o primeiro tick e, longe de jogadores, a escolha seguinte demora 4x
        // (IA-7): a primeira conferida espera uma volta inteira da escolha.
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(soldier.getTarget() == deep, "Na guarda vale o kaiju mais dentro da area, nao o mais"
                    + " perto: " + soldier.getTarget());
            deep.discard();
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(soldier.getTarget() == edge, "Sem o de dentro, o outro da area: "
                    + soldier.getTarget());
            edge.discard();
        });
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(soldier.getTarget() != outside, "O kaiju fora da area nao deveria ser alvo");
            List.of(outside, soldier).forEach(Entity::discard);
            forceChunks(level, origin, false);
            helper.succeed();
        });
    }
}
