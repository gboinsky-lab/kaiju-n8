// src/main/java/com/kn8/core/combat/ActionTimeline.java
package com.kn8.core.combat;

/**
 * Linha do tempo de UMA acao de combate de um ator, em ticks do servidor (Java puro, testavel com JUnit). Validada
 * no PT7; no M8 passa a reger as habilidades de kaiju e, no M10, as acoes do jogador.
 *
 * <p>Garante as duas regras do fluxo de dano (Fase 4, secao 6.8):</p>
 * <ul>
 *   <li>uma acao nova so comeca quando a anterior terminou (pedido fora da janela e recusado);</li>
 *   <li>o impacto de cada acao e consumido uma unica vez, no tick definido nos dados, nunca pela animacao.</li>
 * </ul>
 */
public final class ActionTimeline {

    /** Valor de "nenhuma acao": evita subtrair de Long.MIN_VALUE. */
    private static final long NONE = Long.MIN_VALUE;
    /** Impacto ausente (ex.: esquiva). */
    public static final int NO_IMPACT = -1;

    private long startTick = NONE;
    private int durationTicks;
    private int impactTick = NO_IMPACT;
    private boolean impactConsumed;
    private String actionId = "";
    private long sequence;

    private long accepted;
    private long rejected;
    private long impacts;

    /** true se ha uma acao em andamento no tick {@code now}. */
    public boolean isActive(long now) {
        return startTick != NONE && now < startTick + durationTicks;
    }

    /**
     * Tenta iniciar uma acao. Recusa se outra ainda estiver ativa.
     *
     * @return true se a acao foi aceita
     */
    public boolean tryStart(long now, String id, int duration, int impact) {
        if (duration <= 0 || impact >= duration) {
            throw new IllegalArgumentException("Duracao/impacto invalidos: " + duration + "/" + impact);
        }
        if (isActive(now)) {
            rejected++;
            return false;
        }
        startTick = now;
        durationTicks = duration;
        impactTick = impact;
        impactConsumed = false;
        actionId = id;
        sequence++;
        accepted++;
        return true;
    }

    /**
     * Deve ser chamado todo tick. Retorna true exatamente uma vez por acao: no primeiro tick em que
     * {@code now >= inicio + impacto}. Acoes sem impacto nunca retornam true.
     */
    public boolean consumeImpact(long now) {
        if (startTick == NONE || impactConsumed || impactTick == NO_IMPACT) {
            return false;
        }
        if (now >= startTick + impactTick) {
            impactConsumed = true;
            impacts++;
            return true;
        }
        return false;
    }

    /** Cancela a acao atual sem impacto (ex.: kaiju atordoado no meio do preparo). */
    public void cancel() {
        impactConsumed = true;
        startTick = NONE;
    }

    public String actionId() {
        return actionId;
    }

    public long sequence() {
        return sequence;
    }

    public long startTick() {
        return startTick;
    }

    public long accepted() {
        return accepted;
    }

    public long rejected() {
        return rejected;
    }

    public long impacts() {
        return impacts;
    }
}
