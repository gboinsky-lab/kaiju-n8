package com.kn8.core.soldier;

/**
 * Pontuacao de um ponto de cobertura (MOV-3, plano de mobilidade v3 §5: "bloqueio de visao, protecao contra o ataque
 * atual, distancia do alvo, linha de tiro, espaco para aliados, risco de destruicao"). O mundo (visao, chao, bloco que
 * corta a visao) e medido no {@code CoverFinder}; aqui so a conta, testada no JUnit. Cobertura nao e esconderijo
 * magico: so vale o ponto onde um bloco solido corta a visao do kaiju ate o soldado.
 */
public final class CoverScore {

    /** Ponto recusado. */
    public static final double REJECTED = Double.NEGATIVE_INFINITY;

    private static final double BASE = 10.0;
    /** Ponto ao lado com linha de tiro (sair para atirar). */
    private static final double PEEK_BONUS = 6.0;
    /** Por bloco de diferenca entre a distancia ao kaiju e a distancia que o atirador quer manter. */
    private static final double RANGE_WEIGHT = 0.5;
    /** Por bloco andado ate o ponto. */
    private static final double TRAVEL_WEIGHT = 0.6;
    /** Por aliado (ou cobertura de aliado) no mesmo ponto. */
    private static final double CROWD_WEIGHT = 4.0;
    /** Resistencia do bloco que corta a visao (explosao): ate {@link #STRENGTH_CAP} conta. */
    private static final double STRENGTH_WEIGHT = 0.4;
    private static final double STRENGTH_CAP = 10.0;

    private CoverScore() {
    }

    /**
     * Um ponto avaliado.
     *
     * @param blocksSight  um bloco solido corta a visao do kaiju ate o soldado (olhos e peito)
     * @param hasPeek      ha ponto ao lado, a um passo, com linha de tiro ate o kaiju
     * @param threatDistance distancia do ponto ate a borda do kaiju
     * @param travel       distancia do soldado ate o ponto
     * @param crowd        aliados (ou coberturas escolhidas por aliados) no ponto
     * @param strength     resistencia a explosao do bloco que corta a visao
     * @param dangerous    dentro da area de um golpe pesado sendo preparado
     */
    public record Candidate(boolean blocksSight, boolean hasPeek, double threatDistance, double travel, int crowd,
            double strength, boolean dangerous) {
    }

    /**
     * Pontos maiores sao melhores; {@link #REJECTED} se nao corta a visao, esta na area de golpe pesado ou perto
     * demais do kaiju ({@code minThreatDistance}: ali ele chega andando, a cobertura nao ajuda).
     */
    public static double score(Candidate c, double preferredDistance, double minThreatDistance) {
        if (!c.blocksSight() || c.dangerous() || c.threatDistance() < minThreatDistance) {
            return REJECTED;
        }
        return BASE
                + (c.hasPeek() ? PEEK_BONUS : 0.0)
                - Math.abs(c.threatDistance() - preferredDistance) * RANGE_WEIGHT
                - c.travel() * TRAVEL_WEIGHT
                - c.crowd() * CROWD_WEIGHT
                + Math.min(Math.max(c.strength(), 0.0), STRENGTH_CAP) * STRENGTH_WEIGHT;
    }

    /** Melhor indice ({@code -1} se todos recusados). */
    public static int best(double[] scores) {
        int best = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] != REJECTED && (best < 0 || scores[i] > scores[best])) {
                best = i;
            }
        }
        return best;
    }
}
