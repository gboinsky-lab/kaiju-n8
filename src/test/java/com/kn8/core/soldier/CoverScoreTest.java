package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kn8.core.soldier.CoverScore.Candidate;

class CoverScoreTest {

    private static Candidate ok() {
        return new Candidate(true, true, 14.0, 3.0, 0, 6.0, false);
    }

    @Test
    void pointsThatDoNotHideAreRejected() {
        assertEquals(CoverScore.REJECTED, CoverScore.score(new Candidate(false, true, 14.0, 3.0, 0, 6.0, false),
                14.0, 6.0));
        assertEquals(CoverScore.REJECTED, CoverScore.score(new Candidate(true, true, 14.0, 3.0, 0, 6.0, true),
                14.0, 6.0), "area de golpe pesado");
        assertEquals(CoverScore.REJECTED, CoverScore.score(new Candidate(true, true, 4.0, 3.0, 0, 6.0, false),
                14.0, 6.0), "kaiju perto demais");
    }

    @Test
    void peekCloseAndAloneIsBetter() {
        double base = CoverScore.score(ok(), 14.0, 6.0);
        assertTrue(base > CoverScore.score(new Candidate(true, false, 14.0, 3.0, 0, 6.0, false), 14.0, 6.0));
        assertTrue(base > CoverScore.score(new Candidate(true, true, 14.0, 8.0, 0, 6.0, false), 14.0, 6.0));
        assertTrue(base > CoverScore.score(new Candidate(true, true, 14.0, 3.0, 1, 6.0, false), 14.0, 6.0));
        assertTrue(base > CoverScore.score(new Candidate(true, true, 22.0, 3.0, 0, 6.0, false), 14.0, 6.0));
        assertTrue(base > CoverScore.score(new Candidate(true, true, 14.0, 3.0, 0, 0.3, false), 14.0, 6.0),
                "vidro protege menos que pedra");
    }

    @Test
    void bestSkipsRejected() {
        assertEquals(-1, CoverScore.best(new double[] {CoverScore.REJECTED, CoverScore.REJECTED}));
        assertEquals(2, CoverScore.best(new double[] {1.0, CoverScore.REJECTED, 3.0, 2.0}));
    }
}
