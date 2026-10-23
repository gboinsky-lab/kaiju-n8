package com.kn8.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kn8.core.world.RegionAlertMath.Event;
import com.kn8.core.world.RegionAlertMath.State;
import com.kn8.core.world.RegionAlertMath.Status;

class RegionAlertMathTest {

    private static final RegionAlertMath.Params P = new RegionAlertMath.Params(100, 50, 200, 40);

    @Test
    void eventsOnlyRaiseAndTimeOnlyLowers() {
        Status s = RegionAlertMath.apply(Status.NORMAL, Event.KAIJU_SEEN, 0, P);
        assertEquals(State.ALERT, s.state());
        s = RegionAlertMath.apply(s, Event.FIGHTING, 10, P);
        assertEquals(new Status(State.COMBAT, 60), s);
        // Kaiju visto durante o combate nao baixa para alerta.
        assertEquals(s, RegionAlertMath.apply(s, Event.KAIJU_SEEN, 20, P));
        s = RegionAlertMath.expire(s, 60, P);
        assertEquals(new Status(State.RECOVERY, 260), s);
        s = RegionAlertMath.expire(s, 260, P);
        assertEquals(Status.NORMAL, s);
    }

    @Test
    void orphanAlertReturnsToNormalAndEndOnlyAffectsCombat() {
        Status s = RegionAlertMath.apply(Status.NORMAL, Event.KAIJU_SEEN, 0, P);
        assertEquals(s, RegionAlertMath.expire(s, 99, P));
        assertEquals(Status.NORMAL, RegionAlertMath.expire(s, 100, P));
        assertEquals(s, RegionAlertMath.apply(s, Event.ENDED, 5, P));
        Status combat = RegionAlertMath.apply(Status.NORMAL, Event.INVASION, 0, P);
        assertEquals(new Status(State.RECOVERY, 230), RegionAlertMath.apply(combat, Event.ENDED, 30, P));
        // Renovar nunca encurta o prazo.
        Status alert = new Status(State.ALERT, 500);
        assertEquals(500, RegionAlertMath.apply(alert, Event.KAIJU_SEEN, 0, P).until());
    }

    @Test
    void evacuationSitsBetweenAlertAndCombat() {
        Status s = RegionAlertMath.apply(Status.NORMAL, Event.EVACUATING, 0, P);
        assertEquals(new Status(State.EVACUATION, 40), s);
        // Kaiju visto nao derruba a evacuacao para alerta; luta passa por cima dela.
        assertEquals(s, RegionAlertMath.apply(s, Event.KAIJU_SEEN, 5, P));
        Status combat = RegionAlertMath.apply(s, Event.FIGHTING, 10, P);
        assertEquals(State.COMBAT, combat.state());
        assertEquals(combat, RegionAlertMath.apply(combat, Event.EVACUATING, 11, P));
        // Combate desce para recuperacao; a evacuacao ainda em andamento volta no proximo aviso.
        Status recovery = RegionAlertMath.expire(combat, 60, P);
        assertEquals(State.RECOVERY, recovery.state());
        assertEquals(new Status(State.EVACUATION, 101), RegionAlertMath.apply(recovery, Event.EVACUATING, 61, P));
        // Sem aviso, a evacuacao acaba sozinha.
        assertEquals(Status.NORMAL, RegionAlertMath.expire(s, 40, P));
        assertTrue(State.EVACUATION.rank() > State.ALERT.rank());
        assertTrue(State.EVACUATION.rank() < State.COMBAT.rank());
    }
}
