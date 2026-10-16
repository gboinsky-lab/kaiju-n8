// src/main/java/com/kn8/core/soldier/SoldierTargeting.java
package com.kn8.core.soldier;

import java.util.List;

/**
 * Escolha de alvo dos soldados (IA-1B) em Java puro. Miguel: soldados detectam os kaiju independente de onde estejam
 * (sem precisar ver, dentro do alcance de deteccao) e cada um faz sua funcao: quem pode cobre o aliado que esta
 * recarregando (o kaiju que mira nele vira prioridade) e o kaiju que acabou de ferir o soldado tambem.
 *
 * <p>Pontuacao menor = melhor: distancia, menos o bonus de cobertura e o de revide (IA-3: e, no especial, os do
 * perfil tatico: kaiju que mira um aliado e kaiju grande). O alvo atual so e trocado se outro
 * for melhor pela margem.</p>
 */
public final class SoldierTargeting {

    /** Numeros do config {@code [soldierAi]}. */
    public record Params(double range, double coverBonus, double revengeBonus, double switchMargin, double allyBonus,
            double bigBonus, double assignedBonus) {

        /** Sem kaiju designado (IA-3). */
        public Params(double range, double coverBonus, double revengeBonus, double switchMargin, double allyBonus,
                double bigBonus) {
            this(range, coverBonus, revengeBonus, switchMargin, allyBonus, bigBonus, 0.0);
        }

        /** Soldado comum: sem os bonus do perfil tatico dos especiais (IA-3). */
        public Params(double range, double coverBonus, double revengeBonus, double switchMargin) {
            this(range, coverBonus, revengeBonus, switchMargin, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Um kaiju possivel. {@code threatensReloadingAlly} = o alvo dele e um aliado recarregando;
     * {@code hurtMe} = feriu este soldado ha pouco; {@code assigned} = o kaiju designado ao esquadrao (IA-5, invasao).
     */
    public record Candidate(int id, double distance, boolean threatensReloadingAlly, boolean hurtMe,
            boolean threatensAlly, boolean big, boolean assigned) {

        public Candidate(int id, double distance, boolean threatensReloadingAlly, boolean hurtMe,
                boolean threatensAlly, boolean big) {
            this(id, distance, threatensReloadingAlly, hurtMe, threatensAlly, big, false);
        }

        public Candidate(int id, double distance, boolean threatensReloadingAlly, boolean hurtMe) {
            this(id, distance, threatensReloadingAlly, hurtMe, false, false, false);
        }
    }

    private SoldierTargeting() {
    }

    public static double score(Params params, Candidate candidate) {
        return candidate.distance() - (candidate.threatensReloadingAlly() ? params.coverBonus() : 0.0)
                - (candidate.hurtMe() ? params.revengeBonus() : 0.0)
                - (candidate.threatensAlly() ? params.allyBonus() : 0.0)
                - (candidate.big() ? params.bigBonus() : 0.0)
                - (candidate.assigned() ? params.assignedBonus() : 0.0);
    }

    /** Id do alvo escolhido (-1 se nenhum kaiju no alcance); o atual fica se nao houver outro melhor pela margem. */
    public static int choose(Params params, List<Candidate> candidates, int currentId) {
        Candidate best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        Candidate current = null;
        for (Candidate candidate : candidates) {
            if (candidate.distance() > params.range()) {
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
}
