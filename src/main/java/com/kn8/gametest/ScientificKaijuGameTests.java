package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Etapa 5 (modelos do Miguel de 2026-11-05): Polyonax, Lacerta Fratris, Imitatio Mantidis (preto e verde), Maxilla
 * Cornuta e Anguilla Ambulans com os numeros do Balanceamento Completo dos Kaijus, habilidades, perfil visual e lugar
 * numa invasao; nascem com a hitbox do registro. A Imitatio Mantidis apara de frente (metade do dano), nao pelas
 * costas.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScientificKaijuGameTests {

    private static final List<String> IDS = List.of("polyonax", "lacerta_fratris", "imitatio_mantidis",
            "imitatio_mantidis_green", "maxilla_cornuta", "anguilla_ambulans");
    private static final double[] HEALTH = {520.0, 310.0, 390.0, 390.0, 650.0, 480.0};

    private ScientificKaijuGameTests() {
    }

    @GameTest(template = "empty_15x12x15", batch = "kn8_especies")
    public static void scientificKaijuHaveDataAndAppear(GameTestHelper helper) {
        List<EntityType<KaijuEntity>> types = List.of(KN8Entities.POLYONAX.get(), KN8Entities.LACERTA_FRATRIS.get(),
                KN8Entities.IMITATIO_MANTIDIS.get(), KN8Entities.IMITATIO_MANTIDIS_GREEN.get(),
                KN8Entities.MAXILLA_CORNUTA.get(), KN8Entities.ANGUILLA_AMBULANS.get());
        for (String name : IDS) {
            ResourceLocation id = KN8Constants.id(name);
            KaijuDef def = KN8Data.KAIJU.server().get(id);
            helper.assertTrue(def != null && def.abilities().size() >= 3, "Sem dados/habilidades: " + id);
            def.abilities().forEach(ability -> helper.assertTrue(KN8Data.ABILITY.server().containsKey(ability),
                    "Habilidade inexistente: " + ability));
            helper.assertTrue(KN8Data.VFX_PROFILE.server().containsKey(id), "Sem perfil visual: " + id);
            helper.assertTrue(KN8Data.INVASION.server().values().stream().anyMatch(invasion -> invasion.waves()
                    .stream().anyMatch(wave -> wave.kaiju().stream().anyMatch(k -> k.species().equals(id)))),
                    "Fora das invasoes: " + id);
        }
        List<KaijuEntity> spawned = types.stream().map(type -> {
            KaijuEntity kaiju = helper.spawn(type, new BlockPos(7, 1, 7));
            kaiju.setNoAi(true);
            return kaiju;
        }).toList();
        helper.runAfterDelay(3, () -> {
            for (int i = 0; i < spawned.size(); i++) {
                KaijuEntity kaiju = spawned.get(i);
                helper.assertTrue(Math.abs(kaiju.getMaxHealth() - HEALTH[i]) < 1.0,
                        IDS.get(i) + " com a vida do documento: " + kaiju.getMaxHealth());
                kaiju.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_15x12x15", batch = "kn8_especies_aparo")
    public static void mantisParriesFromTheFrontOnly(GameTestHelper helper) {
        KaijuEntity mantis = helper.spawn(KN8Entities.IMITATIO_MANTIDIS.get(), new BlockPos(7, 1, 7));
        mantis.setNoAi(true);
        Husk front = helper.spawn(EntityType.HUSK, new BlockPos(7, 1, 2));
        Husk back = helper.spawn(EntityType.HUSK, new BlockPos(7, 1, 12));
        front.setNoAi(true);
        back.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            // Virada para o husk da frente (Z menor = norte, giro 180).
            mantis.setYRot(180.0F);
            mantis.setYBodyRot(180.0F);
            mantis.yBodyRotO = 180.0F;
            Vec3 facing = Vec3.directionFromRotation(0.0F, mantis.yBodyRot);
            helper.assertTrue(facing.z < -0.9, "Imitatio virada para o norte: " + facing);
            float before = mantis.getHealth();
            mantis.invulnerableTime = 0;
            mantis.hurt(helper.getLevel().damageSources().mobAttack(front), 40.0F);
            float fromFront = before - mantis.getHealth();
            float middle = mantis.getHealth();
            mantis.invulnerableTime = 0;
            mantis.hurt(helper.getLevel().damageSources().mobAttack(back), 40.0F);
            float fromBack = middle - mantis.getHealth();
            helper.assertTrue(fromFront > 0.0F && fromBack > 0.0F, "Os dois ferem: " + fromFront + " " + fromBack);
            helper.assertTrue(Math.abs(fromFront - fromBack * 0.5F) < 0.5F,
                    "De frente leva metade: frente " + fromFront + ", costas " + fromBack);
            mantis.discard();
            front.discard();
            back.discard();
            helper.succeed();
        });
    }
}
