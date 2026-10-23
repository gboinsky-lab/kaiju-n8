package com.kn8.core.career;

import java.util.ArrayList;
import java.util.List;

/**
 * Nota da avaliacao pos-missao e resultado das provas de promocao (PM-7, Plano Mestre secoes 9 e 12). Logica pura:
 * so usa o que o jogo mediu durante a missao (tiros dados e acertados, dano causado e recebido, aliados do esquadrao
 * perdidos, tempo). Metrica sem dado suficiente (ex.: precisao com menos tiros que o minimo) nao entra na nota.
 *
 * <p>A nota e a media das partes que valem (0-100 cada): tempo contra o ideal, precisao, dano recebido em relacao a
 * vida maxima e aliados perdidos. A prova passa quando todos os requisitos batem e a nota alcanca a minima.</p>
 */
public final class MissionGrader {

    private MissionGrader() {
    }

    public enum Grade {
        S, A, B, C, D;

        /** Esta nota e pelo menos {@code other} (S > A > B > C > D). */
        public boolean atLeast(Grade other) {
            return ordinal() <= other.ordinal();
        }
    }

    /** O que foi medido na missao. {@code maxHealth} = vida maxima do jogador ao concluir. */
    public record Stats(int shots, int hits, float damageDealt, float damageTaken, float maxHealth, int alliesLost,
            long ticks) {
        public double accuracy() {
            return shots <= 0 ? 0.0 : (double) hits / shots;
        }
    }

    /**
     * Requisitos da prova (todos opcionais: -1 = nao exige). {@code maxDamageTaken} e fracao da vida maxima;
     * {@code minShots} = tiros para a precisao contar (como requisito e na nota).
     */
    public record Requirements(double minAccuracy, int minShots, double maxDamageTaken, int maxAlliesLost,
            long parTicks, Grade minGrade) {
        public static final Requirements NONE = new Requirements(-1, -1, -1, -1, -1, Grade.D);
    }

    /** Contas da nota (config {@code [career]}). */
    public record Params(int gradeS, int gradeA, int gradeB, int gradeC, double accuracyForFullScore,
            double damageTakenForZeroScore, int allyLostPenalty, int minShotsForAccuracy) {
    }

    /** Uma linha da prova: requisito, valor medido, limite e se passou. */
    public record Check(String key, boolean ok, double value, double limit) {
    }

    /** Parte da nota (para o resumo e a sugestao): chave e pontos 0-100. */
    public record Part(String key, int score) {
    }

    public record Result(Grade grade, int score, List<Part> parts, List<Check> checks, boolean passed) {
        /** Parte com menos pontos (sugestao do resumo); vazio se todas estao no maximo. */
        public String weakest() {
            Part worst = null;
            for (Part part : parts) {
                if (part.score() < 100 && (worst == null || part.score() < worst.score())) {
                    worst = part;
                }
            }
            return worst == null ? "" : worst.key();
        }
    }

    public static Result grade(Stats stats, Requirements req, Params params) {
        List<Part> parts = new ArrayList<>();
        if (req.parTicks() > 0 && stats.ticks() > 0) {
            parts.add(new Part("time", stats.ticks() <= req.parTicks() ? 100
                    : clamp((int) Math.round(100.0 * req.parTicks() / stats.ticks()))));
        }
        int minShots = req.minShots() > 0 ? req.minShots() : params.minShotsForAccuracy();
        boolean accuracyCounts = stats.shots() >= Math.max(1, minShots);
        if (accuracyCounts) {
            parts.add(new Part("accuracy", clamp((int) Math.round(100.0 * stats.accuracy()
                    / Math.max(0.01, params.accuracyForFullScore())))));
        }
        double taken = takenFraction(stats);
        parts.add(new Part("damage", clamp((int) Math.round(100.0 * (1.0 - taken
                / Math.max(0.01, params.damageTakenForZeroScore()))))));
        parts.add(new Part("allies", clamp(100 - stats.alliesLost() * params.allyLostPenalty())));

        int sum = 0;
        for (Part part : parts) {
            sum += part.score();
        }
        int score = Math.round((float) sum / parts.size());
        Grade grade = score >= params.gradeS() ? Grade.S : score >= params.gradeA() ? Grade.A
                : score >= params.gradeB() ? Grade.B : score >= params.gradeC() ? Grade.C : Grade.D;

        List<Check> checks = new ArrayList<>();
        if (req.minAccuracy() >= 0) {
            // Sem tiros suficientes a precisao nao foi demonstrada: falha.
            checks.add(new Check("accuracy", accuracyCounts && stats.accuracy() >= req.minAccuracy(),
                    stats.accuracy(), req.minAccuracy()));
        }
        if (req.maxDamageTaken() >= 0) {
            checks.add(new Check("damage", taken <= req.maxDamageTaken(), taken, req.maxDamageTaken()));
        }
        if (req.maxAlliesLost() >= 0) {
            checks.add(new Check("allies", stats.alliesLost() <= req.maxAlliesLost(), stats.alliesLost(),
                    req.maxAlliesLost()));
        }
        checks.add(new Check("grade", grade.atLeast(req.minGrade()), grade.ordinal(), req.minGrade().ordinal()));
        boolean passed = true;
        for (Check check : checks) {
            passed &= check.ok();
        }
        return new Result(grade, score, List.copyOf(parts), List.copyOf(checks), passed);
    }

    /** Dano recebido como fracao da vida maxima. */
    public static double takenFraction(Stats stats) {
        return stats.maxHealth() <= 0 ? 0.0 : stats.damageTaken() / stats.maxHealth();
    }

    /** Merito extra pela nota (S e A); a recompensa base nao muda. */
    public static int meritBonus(int merit, Grade grade, double bonusS, double bonusA) {
        double factor = grade == Grade.S ? bonusS : grade == Grade.A ? bonusA : 0.0;
        return (int) Math.round(merit * factor);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
