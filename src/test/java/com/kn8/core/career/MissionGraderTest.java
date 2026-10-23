// src/test/java/com/kn8/core/career/MissionGraderTest.java
package com.kn8.core.career;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kn8.core.career.MissionGrader.Grade;
import com.kn8.core.career.MissionGrader.Requirements;
import com.kn8.core.career.MissionGrader.Stats;

class MissionGraderTest {

    private static final MissionGrader.Params PARAMS = new MissionGrader.Params(90, 75, 60, 40, 0.6, 2.0, 25, 5);

    @Test
    void cleanRunIsGradeS() {
        MissionGrader.Result result = MissionGrader.grade(new Stats(20, 15, 300, 0, 20, 0, 1000),
                new Requirements(-1, -1, -1, -1, 2000, Grade.D), PARAMS);
        assertEquals(Grade.S, result.grade());
        assertEquals(100, result.score());
        assertTrue(result.passed());
        assertEquals("", result.weakest());
    }

    @Test
    void fewShotsLeaveAccuracyOutOfTheGrade() {
        // 3 tiros (minimo 5): a precisao nao entra; so dano e aliados contam.
        MissionGrader.Result result = MissionGrader.grade(new Stats(3, 0, 100, 10, 20, 0, 0), Requirements.NONE,
                PARAMS);
        assertTrue(result.parts().stream().noneMatch(part -> part.key().equals("accuracy")));
        assertEquals("damage", result.weakest());
    }

    @Test
    void examFailsOnAccuracyEvenWithAGoodGrade() {
        Requirements exam = new Requirements(0.35, 15, 1.0, -1, -1, Grade.C);
        // 10 tiros com 9 acertos: precisao alta mas abaixo do minimo de tiros = nao demonstrada.
        MissionGrader.Result few = MissionGrader.grade(new Stats(10, 9, 200, 0, 20, 0, 0), exam, PARAMS);
        assertFalse(few.passed());
        MissionGrader.Result low = MissionGrader.grade(new Stats(30, 6, 200, 0, 20, 0, 0), exam, PARAMS);
        assertFalse(low.passed());
        MissionGrader.Result ok = MissionGrader.grade(new Stats(30, 12, 200, 0, 20, 0, 0), exam, PARAMS);
        assertTrue(ok.passed());
    }

    @Test
    void damageAlliesAndMinimumGrade() {
        Requirements exam = new Requirements(-1, -1, 0.8, 0, -1, Grade.B);
        assertFalse(MissionGrader.grade(new Stats(0, 0, 0, 20, 20, 0, 0), exam, PARAMS).passed());
        assertFalse(MissionGrader.grade(new Stats(0, 0, 0, 0, 20, 1, 0), exam, PARAMS).passed());
        // Dano 15/20 = 75% (dentro do limite), mas nota (63+100)/2 = 82 -> A: passa; com 2 vidas inteiras
        // de dano e um aliado a nota cai abaixo de B.
        assertTrue(MissionGrader.grade(new Stats(0, 0, 0, 15, 20, 0, 0), exam, PARAMS).passed());
        assertEquals(Grade.D, MissionGrader.grade(new Stats(0, 0, 0, 40, 20, 3, 0), Requirements.NONE, PARAMS)
                .grade());
    }

    @Test
    void meritBonusOnlyForSAndA() {
        assertEquals(20, MissionGrader.meritBonus(100, Grade.S, 0.2, 0.1));
        assertEquals(10, MissionGrader.meritBonus(100, Grade.A, 0.2, 0.1));
        assertEquals(0, MissionGrader.meritBonus(100, Grade.B, 0.2, 0.1));
    }
}
