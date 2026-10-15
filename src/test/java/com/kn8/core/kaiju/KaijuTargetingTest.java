// src/test/java/com/kn8/core/kaiju/KaijuTargetingTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kn8.core.kaiju.KaijuTargeting.Candidate;
import com.kn8.core.kaiju.KaijuTargeting.Params;

class KaijuTargetingTest {

    private static final Params COMMON = new Params(32.0, 0.35, false, 12.0, 0.5, 16.0, 2.0);
    private static final Params SMART = new Params(48.0, 0.35, true, 12.0, 0.5, 16.0, 2.0);

    private static Candidate seen(int id, double distance) {
        return new Candidate(id, distance, true, false, 0.0, false);
    }

    private static Candidate hidden(int id, double distance) {
        return new Candidate(id, distance, false, false, 0.0, false);
    }

    @Test
    void commonKaijuOnlyHearsHiddenEnemiesUpClose() {
        // Audicao = 32 x 0,35 = 11,2 blocos.
        assertTrue(KaijuTargeting.detects(COMMON, hidden(1, 11.0)));
        assertFalse(KaijuTargeting.detects(COMMON, hidden(1, 12.0)));
        assertTrue(KaijuTargeting.detects(COMMON, seen(1, 30.0)));
        assertFalse(KaijuTargeting.detects(COMMON, seen(1, 33.0)));
    }

    @Test
    void commonKaijuNoticesWhoHurtItEvenUnseen() {
        assertTrue(KaijuTargeting.detects(COMMON, new Candidate(1, 25.0, false, true, 4.0, true)));
    }

    @Test
    void smartKaijuDetectsEveryoneInRange() {
        assertTrue(KaijuTargeting.detects(SMART, hidden(1, 47.0)));
        assertFalse(KaijuTargeting.detects(SMART, hidden(1, 49.0)));
    }

    @Test
    void commonKaijuAttacksTheNearest() {
        List<Candidate> list = List.of(seen(1, 10.0), seen(2, 4.0),
                new Candidate(3, 20.0, true, true, 30.0, true));
        assertEquals(2, KaijuTargeting.choose(COMMON, list, -1));
    }

    @Test
    void commonKaijuDoesNotChaseOneTargetForever() {
        // O alvo atual (1) ficou a 15; outro soldado chegou a 3: troca.
        assertEquals(2, KaijuTargeting.choose(COMMON, List.of(seen(1, 15.0), seen(2, 3.0)), 1));
    }

    @Test
    void switchMarginAvoidsFlipping() {
        // Diferenca de 1,5 < margem 2: fica no atual.
        assertEquals(1, KaijuTargeting.choose(COMMON, List.of(seen(1, 5.0), seen(2, 3.5)), 1));
    }

    @Test
    void smartKaijuGoesForTheShooterOverTheSwordsmanUpClose() {
        Candidate swordsman = seen(1, 2.0);
        // Atirador a 11: 11 - 12 = -1 contra 2 do espadachim (diferenca 3 > margem 2).
        Candidate shooter = new Candidate(2, 11.0, true, true, 0.0, false);
        assertEquals(2, KaijuTargeting.choose(SMART, List.of(swordsman, shooter), 1));
    }

    @Test
    void smartKaijuPrioritisesWhoHurtItMost() {
        Candidate weak = new Candidate(1, 3.0, true, false, 2.0, true);
        Candidate strong = new Candidate(2, 14.0, true, false, 40.0, true);
        // 14 - min(16, 20) = -2 contra 3 - 1 = 2.
        assertEquals(2, KaijuTargeting.choose(SMART, List.of(weak, strong), -1));
    }

    @Test
    void lostTargetIsDropped() {
        assertEquals(-1, KaijuTargeting.choose(COMMON, List.of(hidden(1, 20.0)), 1));
    }

    @Test
    void progressCountsGettingCloserOrHittingTheTarget() {
        assertTrue(KaijuTargeting.madeProgress(10.0, 8.0, 1.0, false));
        assertFalse(KaijuTargeting.madeProgress(10.0, 9.5, 1.0, false));
        assertTrue(KaijuTargeting.madeProgress(0.5, 0.5, 1.0, true));
    }
}
