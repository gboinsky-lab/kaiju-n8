// src/test/java/com/kn8/core/soldier/SquadRulesTest.java
package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kn8.core.soldier.SquadRules.Recruit;

class SquadRulesTest {

    @Test
    void recruitFollowsTheRank() {
        assertEquals(Recruit.NO_SQUAD, SquadRules.canRecruit(0, 0, false, 1, 4, false));
        assertEquals(Recruit.OK, SquadRules.canRecruit(2, 1, false, 2, 4, false));
        assertEquals(Recruit.FULL, SquadRules.canRecruit(2, 2, false, 2, 4, false));
        assertEquals(Recruit.SPECIAL_RANK, SquadRules.canRecruit(4, 0, true, 3, 4, false));
        assertEquals(Recruit.OK, SquadRules.canRecruit(6, 0, true, 4, 4, false));
        assertEquals(Recruit.TAKEN, SquadRules.canRecruit(6, 0, false, 4, 4, true));
    }

    @Test
    void defendersSplitIntoSquadsOfThree() {
        assertEquals(List.of(3, 3), SquadRules.partition(6, 3));
        assertEquals(List.of(3, 3, 1), SquadRules.partition(7, 3));
        assertEquals(List.of(), SquadRules.partition(0, 3));
    }

    @Test
    void postsAreAroundTheCenter() {
        double[] first = SquadRules.post(0, 0, 0, 4, 10);
        double[] second = SquadRules.post(0, 0, 1, 4, 10);
        assertEquals(10.0, first[0], 1e-9);
        assertEquals(10.0, second[1], 1e-9);
    }

    @Test
    void squadsSplitBetweenKaijuBeforeDoublingUp() {
        double[][] posts = {{10, 0}, {-10, 0}, {0, 10}};
        double[][] kaiju = {{12, 0}, {-12, 0}};
        int[] chosen = SquadRules.assign(posts, kaiju, new int[] {-1, -1, -1});
        assertEquals(0, chosen[0]);
        assertEquals(1, chosen[1]);
        assertTrue(chosen[2] >= 0);
        // Quem ja tem um kaiju vivo fica com ele.
        int[] kept = SquadRules.assign(posts, kaiju, new int[] {1, -1, -1});
        assertEquals(1, kept[0]);
        assertEquals(0, kept[1]);
        // Sem kaiju, ninguem designado.
        assertEquals(-1, SquadRules.assign(posts, new double[0][], new int[] {-1, -1, -1})[0]);
    }

    @Test
    void focusPicksTheKaijuUnderTheAim() {
        // Mira passando pela caixa (0 grau) vence o que esta perto mas fora da mira.
        assertEquals(1, SquadRules.pickFocus(new double[] {6, 0, 3}, new double[] {5, 40, 10}, 12));
        // Empate de angulo: o mais perto.
        assertEquals(2, SquadRules.pickFocus(new double[] {4, 9, 4.5}, new double[] {30, 5, 12}, 12));
        // Nenhum dentro do cone: nenhum alvo (a ordem nao muda).
        assertEquals(-1, SquadRules.pickFocus(new double[] {20, 30}, new double[] {5, 5}, 12));
    }

    @Test
    void shootersGuardInsideTheAssaultRing() {
        assertTrue(SquadRules.Role.SHOOTER.guardRing() < SquadRules.Role.ASSAULT.guardRing());
        // Ordens novas no fim: os indices antigos (salvos no mundo) nao mudam.
        assertEquals(SquadRules.Order.RETREAT, SquadRules.Order.byIndex(3));
        assertEquals(SquadRules.Order.GUARD, SquadRules.Order.byIndex(5));
    }

    @Test
    void spreadOffsetStaysInsideTheLimit() {
        double max = Math.PI / 4;
        double first = SquadRules.spreadOffset(100, max);
        double second = SquadRules.spreadOffset(101, max);
        assertTrue(Math.abs(first) <= max && Math.abs(second) <= max);
        assertTrue(Math.abs(first - second) > 0.1);
    }
}
