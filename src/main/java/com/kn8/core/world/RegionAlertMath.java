package com.kn8.core.world;

/**
 * Estado de alerta de uma regiao (local da obra, PM-9, Plano Mestre secao 13). Logica pura: um evento real so sobe o
 * estado ou renova o prazo; o tempo so desce (combate -> recuperacao -> normal; alerta -> normal). Sem evento novo a
 * regiao sempre volta ao normal, entao um evento orfao nunca deixa a cidade em alerta para sempre.
 */
public final class RegionAlertMath {

    private RegionAlertMath() {
    }

    public enum State {
        NORMAL, ALERT, COMBAT, RECOVERY;

        public static State byIndex(int index) {
            State[] values = values();
            return index >= 0 && index < values.length ? values[index] : NORMAL;
        }
    }

    public enum Event {
        /** Kaiju dentro da regiao, sem lutar. */
        KAIJU_SEEN,
        /** Kaiju lutando dentro da regiao. */
        FIGHTING,
        /** Invasao com o centro dentro da regiao (renovada enquanto dura). */
        INVASION,
        /** A ameaca acabou (invasao terminou): combate passa a recuperacao. */
        ENDED
    }

    /** Prazos em ticks (config {@code [region]}). */
    public record Params(long alertTicks, long combatTicks, long recoveryTicks) {
    }

    /** Estado e o tick em que ele expira (normal nao expira). */
    public record Status(State state, long until) {
        public static final Status NORMAL = new Status(State.NORMAL, 0L);
    }

    public static Status apply(Status current, Event event, long now, Params params) {
        return switch (event) {
            case KAIJU_SEEN -> current.state() == State.COMBAT ? current
                    : new Status(State.ALERT, Math.max(now + params.alertTicks(),
                            current.state() == State.ALERT ? current.until() : 0L));
            case FIGHTING, INVASION -> new Status(State.COMBAT, Math.max(now + params.combatTicks(),
                    current.state() == State.COMBAT ? current.until() : 0L));
            case ENDED -> current.state() == State.COMBAT
                    ? new Status(State.RECOVERY, now + params.recoveryTicks()) : current;
        };
    }

    /** Desce um degrau quando o prazo vence. */
    public static Status expire(Status current, long now, Params params) {
        if (current.state() == State.NORMAL || now < current.until()) {
            return current;
        }
        return current.state() == State.COMBAT ? new Status(State.RECOVERY, now + params.recoveryTicks())
                : Status.NORMAL;
    }
}
