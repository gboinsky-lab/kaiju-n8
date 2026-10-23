package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class HabitReaderTest {

    private static final HabitReader.Params P = new HabitReader.Params(200, 10.0, 2.5, 0.7, 8, 3, 4, 100, 160);

    @Test
    void keepingDistanceBecomesAHabitThenWaitsAndForgets() {
        HabitReader reader = new HabitReader();
        for (int t = 0; t < 70; t += 10) {
            reader.sampleDistance(t, 14.0, P);
            assertTrue(reader.update(t, P).isEmpty(), "poucas amostras ainda");
        }
        reader.sampleDistance(70, 14.0, P);
        assertEquals(Optional.of(HabitReader.Habit.KEEPS_DISTANCE), reader.update(70, P));
        assertEquals(Optional.of(HabitReader.Habit.KEEPS_DISTANCE), reader.active(120));
        for (int t = 80; t < 330; t += 10) {
            reader.sampleDistance(t, 14.0, P);
            assertTrue(reader.update(t, P).isEmpty(), "resposta e espera: nada novo ate 330 (" + t + ")");
        }
        assertTrue(reader.active(171).isEmpty(), "resposta acabou em 170");
        reader.sampleDistance(330, 14.0, P);
        assertEquals(Optional.of(HabitReader.Habit.KEEPS_DISTANCE), reader.update(330, P), "repetiu: volta");
        for (int t = 340; t < 1000; t += 10) {
            reader.sampleDistance(t, 1.0, P);
        }
        assertTrue(reader.sampleCount() <= 21, "memoria limitada pela janela");
    }

    @Test
    void evadesAndRepeatsWinOverDistanceAndMixedDistanceIsNothing() {
        HabitReader reader = new HabitReader();
        for (int t = 0; t < 100; t += 10) {
            reader.sampleDistance(t, t % 20 == 0 ? 14.0 : 1.0, P);
        }
        assertTrue(reader.update(100, P).isEmpty(), "distancia misturada nao e habito");
        reader.attackResult(101, false, P);
        reader.attackResult(102, true, P);
        reader.attackResult(103, false, P);
        assertTrue(reader.update(104, P).isEmpty(), "duas esquivas so");
        reader.attackResult(105, false, P);
        assertEquals(Optional.of(HabitReader.Habit.EVADES), reader.update(106, P));
        HabitReader repeats = new HabitReader();
        for (int i = 0; i < 4; i++) {
            repeats.hitBy(i * 20, i == 1 ? "melee" : "heavy", P);
        }
        assertTrue(repeats.update(80, P).isEmpty(), "tipo trocado no meio zera a sequencia");
        for (int i = 0; i < 4; i++) {
            repeats.hitBy(100 + i * 20, "heavy", P);
        }
        assertEquals(Optional.of(HabitReader.Habit.REPEATS), repeats.update(180, P));
        repeats.reset();
        assertTrue(repeats.active(181).isEmpty(), "troca de alvo zera");
    }
}
