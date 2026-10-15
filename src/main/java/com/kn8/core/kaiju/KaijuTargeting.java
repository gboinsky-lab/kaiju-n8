// src/main/java/com/kn8/core/kaiju/KaijuTargeting.java
package com.kn8.core.kaiju;

import java.util.List;

/**
 * Deteccao e escolha de alvo do kaiju (IA-1A) em Java puro. O servidor monta os candidatos a cada reavaliacao e
 * este codigo decide quem o kaiju percebe e quem ataca.
 *
 * <p>Deteccao (Miguel): soldados detectam com facilidade, kaiju comuns nao. Kaiju comum percebe quem ve dentro do
 * alcance e, sem ver, so bem perto (fracao "audicao" do alcance) ou quem o feriu ha pouco. Kaiju inteligente
 * (No. 9, No. 10) percebe todos no alcance, vendo ou nao.</p>
 *
 * <p>Escolha (Miguel): o comum ataca quem esta mais perto, sem perseguir um so alvo para sempre; o inteligente
 * elimina por funcao e ameaca (atirador e quem mais o feriu primeiro, mesmo de longe com alguem batendo de perto).
 * Pontuacao menor = melhor. O alvo atual so e trocado se outro for melhor pela margem (sem ficar indo e voltando
 * entre dois soldados a mesma distancia).</p>
 */
public final class KaijuTargeting {

    /** Numeros da IA (config {@code [kaijuAi]}). */
    public record Params(double range, double hearingFraction, boolean smart, double shooterBonus,
            double threatPerDamage, double maxThreatBonus, double switchMargin) {
    }

    /**
     * Um inimigo possivel. {@code recentDamage} = dano que causou ao kaiju dentro da memoria de ameaca;
     * {@code recentAttacker} = feriu (ou feriu um vizinho do bando) dentro da memoria.
     */
    public record Candidate(int id, double distance, boolean visible, boolean shooter, double recentDamage,
            boolean recentAttacker) {
    }

    private KaijuTargeting() {
    }

    /** O kaiju percebe este candidato? */
    public static boolean detects(Params params, Candidate candidate) {
        if (candidate.distance() > params.range()) {
            return false;
        }
        if (params.smart() || candidate.visible() || candidate.recentAttacker()) {
            return true;
        }
        return candidate.distance() <= params.range() * params.hearingFraction();
    }

    /** Pontuacao (menor = atacar primeiro). Comum: so a distancia. Inteligente: distancia menos ameaca e funcao. */
    public static double score(Params params, Candidate candidate) {
        if (!params.smart()) {
            return candidate.distance();
        }
        double threat = Math.min(params.maxThreatBonus(), candidate.recentDamage() * params.threatPerDamage());
        return candidate.distance() - threat - (candidate.shooter() ? params.shooterBonus() : 0.0);
    }

    /**
     * Escolhe o alvo entre os candidatos percebidos. {@code currentId} = alvo atual (-1 sem alvo); devolve o id do
     * alvo escolhido ou -1 se ninguem for percebido. O alvo atual fica se ainda for percebido e nenhum outro for
     * melhor por mais que a margem.
     */
    public static int choose(Params params, List<Candidate> candidates, int currentId) {
        Candidate best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        Candidate current = null;
        for (Candidate candidate : candidates) {
            if (!detects(params, candidate)) {
                continue;
            }
            double score = score(params, candidate);
            if (candidate.id() == currentId) {
                current = candidate;
            }
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best == null) {
            return -1;
        }
        if (current != null && score(params, current) - bestScore <= params.switchMargin()) {
            return current.id();
        }
        return best.id();
    }

    /**
     * Progresso da perseguicao entre duas checagens: o kaiju chegou mais perto (pelo menos {@code minProgress}) ou
     * feriu o alvo? Estar "ao alcance" nao basta: embaixo de um soldado no alto de um pilar a borda e a altura batem,
     * mas o golpe nao acerta. Sem progresso por tempo demais, o alvo e inalcancavel.
     */
    public static boolean madeProgress(double previousEdge, double edge, double minProgress, boolean hitTarget) {
        return hitTarget || previousEdge - edge >= minProgress;
    }
}
