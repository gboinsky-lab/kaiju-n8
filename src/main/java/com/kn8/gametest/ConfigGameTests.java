// src/main/java/com/kn8/gametest/ConfigGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.config.KN8Difficulty;
import com.kn8.common.config.ServerConfig;
import com.kn8.core.math.FortitudeCurve;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M2: o config de servidor carrega no servidor e os padroes reproduzem o GDD.
 * Template: data/kn8/structure/empty_3x3.nbt (gerado por tools/gen_empty_structure.py).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigGameTests {

    private static final String TEMPLATE = "empty_3x3";
    private static final double PRIMIGENIUS_FORTITUDE = 5.4;
    private static final double PRIMIGENIUS_HEALTH = 211.0;
    private static final double TOLERANCE = 0.5;

    private ConfigGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void serverConfigIsLoadedWithGddDefaults(GameTestHelper helper) {
        helper.assertTrue(ServerConfig.SPEC.isLoaded(), "kn8-server.toml nao carregou no servidor");
        FortitudeCurve curve = ServerConfig.fortitudeCurve();
        helper.assertTrue(Math.abs(curve.health(PRIMIGENIUS_FORTITUDE) - PRIMIGENIUS_HEALTH) < TOLERANCE,
                "Curva padrao nao reproduz a vida do Primigenius do GDD (211)");
        helper.assertTrue(ServerConfig.DIFFICULTY.get() == KN8Difficulty.NORMAL,
                "Dificuldade padrao deveria ser NORMAL");
        helper.assertTrue(ServerConfig.difficultyHealthMultiplier() == 1.0,
                "NORMAL deveria multiplicar a vida por 1.0");
        helper.assertTrue(ServerConfig.RANK_CAPS.get().size() == 6, "Deveria haver 6 tetos de patente");
        helper.succeed();
    }
}
