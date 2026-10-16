// src/test/java/com/kn8/core/soldier/SoldierTargetingTest.java
package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kn8.core.soldier.SoldierTargeting.Candidate;
import com.kn8.core.soldier.SoldierTargeting.Params;

class SoldierTargetingTest {

    private static final Params PARAMS = new Params(64.0, 12.0, 6.0, 3.0);

    private static Candidate plain(int id, double distance) {
        return new Candidate(id, distance, false, false);
    }

    @Test
    void picksTheNearestKaijuEvenFarAway() {
        assertEquals(2, SoldierTargeting.choose(PARAMS, List.of(plain(1, 60.0), plain(2, 40.0)), -1));
    }

    @Test
    void ignoresKaijuOutOfRange() {
        assertEquals(-1, SoldierTargeting.choose(PARAMS, List.of(plain(1, 65.0)), -1));
    }

    @Test
    void coversTheAllyWhoIsReloading() {
        // 18 - 12 = 6 contra 10 (diferenca 4 > margem 3): vai ajudar quem esta recarregando.
        Candidate threat = new Candidate(1, 18.0, true, false);
        assertEquals(1, SoldierTargeting.choose(PARAMS, List.of(threat, plain(2, 10.0)), 2));
    }

    @Test
    void hitsBackTheKaijuThatHurtIt() {
        Candidate attacker = new Candidate(1, 14.0, false, true);
        assertEquals(1, SoldierTargeting.choose(PARAMS, List.of(attacker, plain(2, 10.0)), -1));
    }

    @Test
    void minaPrefersTheBigKaijuAndRenoProtectsAllies() {
        SoldierTargeting.Params mina = new SoldierTargeting.Params(64.0, 12.0, 6.0, 3.0, 0.0, 12.0);
        Candidate small = plain(1, 8.0);
        Candidate big = new Candidate(2, 18.0, false, false, false, true);
        assertEquals(2, SoldierTargeting.choose(mina, List.of(small, big), -1));
        SoldierTargeting.Params reno = new SoldierTargeting.Params(64.0, 12.0, 6.0, 3.0, 10.0, 0.0);
        Candidate onAlly = new Candidate(2, 15.0, false, false, true, false);
        assertEquals(2, SoldierTargeting.choose(reno, List.of(small, onAlly), -1));
    }

    @Test
    void keepsTheCurrentTargetInsideTheMargin() {
        assertEquals(1, SoldierTargeting.choose(PARAMS, List.of(plain(1, 12.0), plain(2, 10.0)), 1));
    }
}
