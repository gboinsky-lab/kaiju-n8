// src/test/java/com/kn8/core/soldier/AmmoMathTest.java
package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmmoMathTest {

    @Test
    void reloadingWithRoundsLeftLosesNothing() {
        // Rifle com 10 balas e 4 pentes reserva (120): completa 20, sobram 100.
        assertEquals(new AmmoMath.Reload(30, 100), AmmoMath.topUp(10, 30, 120));
    }

    @Test
    void lastPartialMagazineIsUsed() {
        assertEquals(new AmmoMath.Reload(12, 0), AmmoMath.topUp(0, 30, 12));
    }

    @Test
    void totalShotsNeverChange() {
        // Recarregar a cada tiro ou so no fim: o total de tiros e sempre pente + reserva.
        int rounds = 30;
        int reserve = AmmoMath.initialReserve(4, 30);
        int shots = 0;
        while (rounds > 0) {
            rounds--;
            shots++;
            AmmoMath.Reload reload = AmmoMath.topUp(rounds, 30, reserve);
            rounds = reload.rounds();
            reserve = reload.reserve();
        }
        assertEquals(150, shots);
    }

    @Test
    void magazinesLeftRoundsUp() {
        assertEquals(3, AmmoMath.magazinesLeft(61, 30));
        assertEquals(0, AmmoMath.magazinesLeft(0, 30));
    }
}
