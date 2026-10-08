// src/test/java/com/kn8/core/power/PowerMathTest.java
package com.kn8.core.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Confere as formulas contra os numeros do GDD (secoes 5 a 8). */
class PowerMathTest {

    // Mesmos padroes de ServerConfig (GDD).
    static final PowerParams GDD = new PowerParams(25, 0.004, 0.004, 0.4, 0.005, 20, 40,
            100, 0.5, 15, 20, 0.75,
            40, 70, 90, 100, 2.0, 0.67, 10, 3,
            100, 1.0,
            50, 10);
    private static final double DELTA = 1e-9;

    @Test
    void damageMatchesGddTable() {
        assertEquals(1.04, PowerMath.damageMultiplier(1, GDD), DELTA);
        assertEquals(3.0, PowerMath.damageMultiplier(50, GDD), DELTA);
        assertEquals(5.0, PowerMath.damageMultiplier(100, GDD), DELTA);
    }

    @Test
    void speedReductionAndKnockbackScale() {
        assertEquals(0.4, PowerMath.speedBonus(100, GDD), DELTA);
        assertEquals(0.2, PowerMath.damageReduction(50, GDD), DELTA);
        assertEquals(0.4, PowerMath.damageReduction(100, GDD), DELTA);
        assertEquals(0.5, PowerMath.knockbackResistance(100, GDD), DELTA);
    }

    @Test
    void staminaMaxAndRegen() {
        assertEquals(150, PowerMath.maxStamina(100, GDD), DELTA);
        // Antes do atraso de 20 ticks nao regenera.
        assertEquals(50, PowerMath.staminaAfterTick(50, 100, 10, HeatStage.NORMAL, GDD), DELTA);
        // 15 por segundo = 0,75 por tick.
        assertEquals(50.75, PowerMath.staminaAfterTick(50, 100, 20, HeatStage.NORMAL, GDD), DELTA);
        // WARM: 25% mais devagar.
        assertEquals(50.5625, PowerMath.staminaAfterTick(50, 100, 20, HeatStage.WARM, GDD), DELTA);
        assertEquals(100, PowerMath.staminaAfterTick(99.9, 100, 40, HeatStage.NORMAL, GDD), DELTA);
    }

    @Test
    void heatStagesFollowTheGdd() {
        assertEquals(HeatStage.NORMAL, PowerMath.heatStage(39.9, GDD));
        assertEquals(HeatStage.WARM, PowerMath.heatStage(40, GDD));
        assertEquals(HeatStage.OVERLOAD, PowerMath.heatStage(70, GDD));
        assertEquals(HeatStage.CRITICAL, PowerMath.heatStage(90, GDD));
        assertEquals(HeatStage.PANIC, PowerMath.heatStage(100, GDD));
    }

    @Test
    void excessOverTheLimitHeatsAndRestCools() {
        // 0.5.0: ativa 30 com limite 10 = 20 de excesso = 2 x 2/s = 4/s = 0,2 por tick.
        assertEquals(10.2, PowerMath.heatAfterTick(10, 30, 10, true, GDD), DELTA);
        // Desligado: fora de combate esfria 10/s = 0,5 por tick; em combate 3/s = 0,15.
        assertEquals(9.5, PowerMath.heatAfterTick(10, 0, 10, false, GDD), DELTA);
        assertEquals(9.85, PowerMath.heatAfterTick(10, 0, 10, true, GDD), DELTA);
        assertEquals(0, PowerMath.heatAfterTick(0.1, 0, 10, false, GDD), DELTA);
        // Acima do limite o calor chega ao maximo (a % nao cai: so o corpo desgasta).
        assertEquals(100, PowerMath.heatAfterTick(99.99, 30, 10, true, GDD), DELTA);
    }

    @Test
    void useInsideTheLimitOnlyWarmsAboutAMinute() {
        // Forca total (no limite): 0,67/s; abaixo do limite o traje nao aquece, esfria (Miguel).
        assertEquals(10 + 0.67 / 20, PowerMath.heatAfterTick(10, 10, 10, true, GDD), DELTA);
        assertEquals(9.85, PowerMath.heatAfterTick(10, 5, 10, true, GDD), DELTA);
        // Nunca passa de WARM (40) dentro do limite...
        assertEquals(40, PowerMath.heatAfterTick(39.99, 10, 10, true, GDD), DELTA);
        // ...e o que veio de cima esfria ate WARM.
        assertEquals(79.85, PowerMath.heatAfterTick(80, 10, 10, true, GDD), DELTA);
        assertEquals(40, PowerMath.heatAfterTick(40.1, 10, 10, true, GDD), DELTA);
        // ~60 s no limite para chegar a WARM.
        double heat = 0;
        int ticks = 0;
        while (heat < 40 && ticks < 10_000) {
            heat = PowerMath.heatAfterTick(heat, 10, 10, true, GDD);
            ticks++;
        }
        assertEquals(60, ticks / 20.0, 1.0);
    }

    @Test
    void releaseKeyRaisesAndLowersInsideZeroToHundred() {
        // 20/s = 1 por tick subindo; 40/s = 2 por tick descendo.
        assertEquals(11, PowerMath.releaseAfterInput(10, 1, GDD), DELTA);
        assertEquals(8, PowerMath.releaseAfterInput(10, -1, GDD), DELTA);
        assertEquals(10, PowerMath.releaseAfterInput(10, 0, GDD), DELTA);
        assertEquals(100, PowerMath.releaseAfterInput(99.5, 1, GDD), DELTA);
        assertEquals(0, PowerMath.releaseAfterInput(1, -1, GDD), DELTA);
        assertEquals(0, PowerMath.excess(8, 10));
        assertEquals(5, PowerMath.excess(15, 10));
    }

    @Test
    void effectiveReleaseIsActivePlusDesperation() {
        assertEquals(10, PowerMath.effectiveRelease(10, 0));
        assertEquals(25, PowerMath.effectiveRelease(10, 15));
        assertEquals(100, PowerMath.effectiveRelease(95, 15));
        assertEquals(0, PowerMath.effectiveRelease(0, -3));
    }

    @Test
    void talentRollIsCommonOrRareInsideItsRange() {
        TalentParams talent = new TalentParams(5, 10, 0.1, 15, 30);
        // Comum: chance >= 0,1.
        assertEquals(new TalentParams.Talent(5, false), talent.roll(0.5, 0.0));
        assertEquals(new TalentParams.Talent(10, false), talent.roll(0.5, 0.999));
        // Raro: chance < 0,1.
        assertEquals(new TalentParams.Talent(15, true), talent.roll(0.05, 0.0));
        assertEquals(new TalentParams.Talent(30, true), talent.roll(0.05, 0.9999));
        java.util.Random random = new java.util.Random(8);
        for (int i = 0; i < 1000; i++) {
            TalentParams.Talent roll = talent.roll(random.nextDouble(), random.nextDouble());
            int value = roll.limit();
            boolean inside = roll.rare() ? value >= 15 && value <= 30 : value >= 5 && value <= 10;
            assertEquals(true, inside, "fora da faixa: " + roll);
        }
    }

    @Test
    void bodyLevelsCostMoreAndStopAtTheMax() {
        BodyParams body = new BodyParams(100, 20, 4, 0.003, 0.0015, 0.002, 0.003);
        assertEquals(20, body.xpForNext(0));
        assertEquals(60, body.xpForNext(10));
        // 20 + 24 = 44 para o nivel 2; sobram 6.
        assertEquals(new PowerMath.Training(2, 6), body.convert(0, 50));
        // No maximo guarda no maximo um nivel de XP.
        assertEquals(new PowerMath.Training(100, 420), body.convert(100, 99_999));
    }

    @Test
    void trainingConvertsUpToTheCapAndKeepsOnlyOnePointInReserve() {
        // 0 -> 2: 50 + 60 = 110; sobram 90 (o ponto 3 custaria 70, entao converte mais um: sobram 20).
        assertEquals(new PowerMath.Training(3, 20), PowerMath.convertTraining(0, 200, 10, GDD));
        // No teto 10, 99050 XP viram reserva de um ponto (150).
        assertEquals(new PowerMath.Training(10, 150), PowerMath.convertTraining(10, 99_050, 10, GDD));
        // Teto sobe para 100: a reserva vira exatamente +1 ponto.
        assertEquals(new PowerMath.Training(11, 0), PowerMath.convertTraining(10, 150, 100, GDD));
    }

    @Test
    void trainingGetsMoreExpensive() {
        assertEquals(50, PowerMath.xpForNextPoint(0, GDD));
        assertEquals(150, PowerMath.xpForNextPoint(10, GDD));
    }

    @Test
    void sprintWindedHasHysteresis() {
        double cost = 5.0 / 20;
        // Com stamina, corre; zerou, fica sem folego.
        assertEquals(false, PowerMath.windedAfterTick(false, 50, cost, 20));
        assertEquals(true, PowerMath.windedAfterTick(false, 0.1, cost, 20));
        // Sem folego ate recuperar o minimo (nao volta a correr com 1 ponto).
        assertEquals(true, PowerMath.windedAfterTick(true, 10, cost, 20));
        assertEquals(false, PowerMath.windedAfterTick(true, 20, cost, 20));
        // Custo zero no config: corrida livre.
        assertEquals(false, PowerMath.windedAfterTick(false, 0, 0, 20));
    }

    @Test
    void desperationRaisesReleaseOnlyBelowTheThreshold() {
        // 0.5: limite 50% de vida, ate +15 pontos com a vida em 0.
        assertEquals(0, PowerMath.desperationBonus(1.0, 0.5, 15));
        assertEquals(0, PowerMath.desperationBonus(0.5, 0.5, 15));
        assertEquals(8, PowerMath.desperationBonus(0.25, 0.5, 15));
        assertEquals(15, PowerMath.desperationBonus(0.0, 0.5, 15));
        // Desligado no config.
        assertEquals(0, PowerMath.desperationBonus(0.1, 0.5, 0));
        assertEquals(0, PowerMath.desperationBonus(0.1, 0.0, 15));
    }
}
