package com.kn8.core.world;

/**
 * Estado de alerta de uma regiao (local da obra, PM-9, Plano Mestre secao 13). Logica pura: um evento real so sobe o
 * estado ou renova o prazo; o tempo so desce (combate -> recuperacao -> normal; alerta/evacuacao -> normal). Sem
 * evento novo a regiao sempre volta ao normal, entao um evento orfao nunca deixa a cidade em alerta para sempre.
 *
 * <p>PM-10A: evacuacao (missao de evacuar civis em andamento na regiao) fica entre o alerta e o combate: kaiju visto
 * nao a derruba para alerta, combate passa por cima dela e, quando o combate desce, a evacuacao ainda em andamento
 * volta no proximo aviso (um estado so por regiao, sem dois estados contraditorios).</p>
 */
public final class RegionAlertMath {

    private RegionAlertMath() {
    }

    /** A ordem dos valores e a salva no mundo (novos so no fim); a gravidade e {@link #rank()}. */
    public enum State {
        NORMAL(0), ALERT(2), COMBAT(4), RECOVERY(1), EVACUATION(3);

        private final int rank;

        State(int rank) {
            this.rank = rank;
        }

        /** Gravidade: normal < recuperacao < alerta < evacuacao < combate. */
        public int rank() {
            return rank;
        }

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
        ENDED,
        /** PM-10A: evacuacao de civis em andamento na regiao (renovada enquanto a missao dura). */
        EVACUATING
    }

    /** Prazos em ticks (config {@code [region]} e {@code [evacuation]}). */
    public record Params(long alertTicks, long combatTicks, long recoveryTicks, long evacuationTicks) {
    }

    /** Estado e o tick em que ele expira (normal nao expira). */
    public record Status(State state, long until) {
        public static final Status NORMAL = new Status(State.NORMAL, 0L);
    }

    public static Status apply(Status current, Event event, long now, Params params) {
        return switch (event) {
            case KAIJU_SEEN -> current.state().rank() > State.ALERT.rank() ? current
                    : new Status(State.ALERT, Math.max(now + params.alertTicks(),
                            current.state() == State.ALERT ? current.until() : 0L));
            case FIGHTING, INVASION -> new Status(State.COMBAT, Math.max(now + params.combatTicks(),
                    current.state() == State.COMBAT ? current.until() : 0L));
            case ENDED -> current.state() == State.COMBAT
                    ? new Status(State.RECOVERY, now + params.recoveryTicks()) : current;
            case EVACUATING -> current.state() == State.COMBAT ? current
                    : new Status(State.EVACUATION, Math.max(now + params.evacuationTicks(),
                            current.state() == State.EVACUATION ? current.until() : 0L));
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
