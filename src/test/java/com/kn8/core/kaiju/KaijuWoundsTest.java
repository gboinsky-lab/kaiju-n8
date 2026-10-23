package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class KaijuWoundsTest {

    @Test
    void woundOpensAtTheThresholdAndDoesNotStackWhileOpen() {
        KaijuWounds wounds = new KaijuWounds();
        assertFalse(wounds.hit("legs", 20.0F, 220.0F, 0.12F, 400, 2, 0), "26,4 de limiar: ainda nao");
        assertTrue(wounds.hit("legs", 10.0F, 220.0F, 0.12F, 400, 2, 1), "passou do limiar: ferida");
        assertTrue(wounds.isWounded("legs", 100));
        assertFalse(wounds.hit("legs", 500.0F, 220.0F, 0.12F, 400, 2, 2), "ferida nao abre de novo");
        assertEquals(0.0F, wounds.accumulated("legs"), "ferida nao acumula");
        assertEquals(List.of("legs"), wounds.expire(401));
        assertFalse(wounds.isWounded("legs", 401), "sarou");
        assertFalse(wounds.hit("legs", 20.0F, 220.0F, 0.12F, 400, 2, 402), "conta recomeca do zero");
    }

    @Test
    void capHoldsTheDamageAtTheThresholdUntilAnotherHeals() {
        KaijuWounds wounds = new KaijuWounds();
        assertTrue(wounds.hit("head", 50.0F, 100.0F, 0.1F, 100, 1, 0));
        assertFalse(wounds.hit("legs", 50.0F, 100.0F, 0.1F, 100, 1, 1), "teto de 1 cheio");
        assertEquals(10.0F, wounds.accumulated("legs"), 1.0E-4, "fica no limiar");
        assertTrue(wounds.hit("legs", 1.0F, 100.0F, 0.1F, 100, 1, 101), "cabeca sarou: a perna abre");
        assertFalse(wounds.hit("tail", 99.0F, 100.0F, 0.0F, 100, 3, 0), "limiar 0 desliga a parte");
    }

    @Test
    void severalWoundsUseTheStrongestEffectOnly() {
        assertEquals(0.6F, KaijuWounds.speedMultiplier(List.of(0.8F, 0.6F, 1.0F)), 1.0E-6);
        assertEquals(1.0F, KaijuWounds.speedMultiplier(List.of()), 1.0E-6);
        assertEquals(1.5F, KaijuWounds.staggerMultiplier(List.of(1.2F, 1.5F)), 1.0E-6);
        assertEquals(1.0F, KaijuWounds.staggerMultiplier(List.of(0.5F)), 1.0E-6);
    }
}
