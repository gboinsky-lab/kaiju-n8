// src/main/java/com/kn8/gametest/PowerGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.core.power.PowerParams;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M5 (lado servidor): o config de poder carrega com os numeros do GDD e o tipo de dano do traje
 * superaquecido existe. As formulas sao testadas com JUnit ({@code PowerMathTest}); o comportamento com jogador
 * real (sync, morte, relog) fica no roteiro manual.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests {

    private static final String TEMPLATE = "empty_3x3";
    private static final double GDD_DIVISOR = 25.0;
    private static final int GDD_SURGE_MAX = 20;
    private static final double GDD_STAMINA_BASE = 100.0;
    private static final int GDD_HEAT_MAX = 100;

    private PowerGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void powerConfigMatchesGdd(GameTestHelper helper) {
        PowerParams params = ServerConfig.powerParams();
        helper.assertTrue(params.releaseDamageDivisor() == GDD_DIVISOR, "Divisor de dano diferente do GDD");
        helper.assertTrue(params.surgeMax() == GDD_SURGE_MAX, "Surto maximo diferente do GDD");
        helper.assertTrue(params.staminaBase() == GDD_STAMINA_BASE, "Stamina base diferente do GDD");
        helper.assertTrue(params.heatMax() == GDD_HEAT_MAX, "Calor maximo diferente do GDD");
        helper.assertTrue(params.heatWarmAt() < params.heatOverloadAt()
                && params.heatOverloadAt() < params.heatCriticalAt()
                && params.heatCriticalAt() < params.heatMax(), "Estagios de calor fora de ordem");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void suitOverheatDamageTypeExists(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .containsKey(PowerService.SUIT_OVERHEAT), "Tipo de dano kn8:suit_overheat nao registrado");
        helper.succeed();
    }
}
