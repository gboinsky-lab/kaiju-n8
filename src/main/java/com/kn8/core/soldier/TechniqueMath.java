package com.kn8.core.soldier;

/**
 * Contas da escolha de tecnica dos soldados especiais (MOV-5B, plano de mobilidade v3 §7 e "Decisao inteligente de
 * uso"): nao comecar um golpe de perto se o alvo vai estar fora do alcance quando o primeiro corte sair, e aproveitar a
 * brecha do kaiju com a tecnica que mais tira. Java puro, testado no JUnit.
 */
public final class TechniqueMath {

    private TechniqueMath() {
    }

    /**
     * Distancia (borda a borda) prevista ate o alvo quando o primeiro golpe sair: a de agora mais o quanto o alvo se
     * afasta por tick ({@code recedePerTick}, componente da velocidade dele para longe de quem ataca; aproximando
     * nao conta) vezes os ticks ate o golpe.
     */
    public static double predictedEdge(double edge, double recedePerTick, double ticksToHit) {
        return edge + Math.max(0.0, recedePerTick) * Math.max(0.0, ticksToHit);
    }

    /** O alvo continua ao alcance da tecnica ({@code maxRange} mais a folga dos golpes que avancam). */
    public static boolean stillInReach(double predictedEdge, double maxRange, double slack) {
        return predictedEdge <= maxRange + slack;
    }

    /**
     * Na brecha do alvo, ganham o bonus as tecnicas com o maior dano total (soma dos multiplicadores dos golpes);
     * {@code totals} so das candidatas. Devolve o maior total ({@code -1} sem candidatas).
     */
    public static double bestTotal(double[] totals) {
        double best = -1.0;
        for (double total : totals) {
            best = Math.max(best, total);
        }
        return best;
    }

    /**
     * MOV-5F (Narumi com o Numbers 1: le o movimento antes de acontecer): o primeiro golpe da tecnica, no tick
     * {@code hitTick} do JSON dividido pela velocidade do Release ({@code speed}), sai antes do impacto do golpe que o
     * kaiju esta preparando ({@code impactTicks} a partir de agora) com pelo menos {@code margin} ticks de sobra.
     */
    public static boolean landsBefore(int hitTick, double speed, int impactTicks, int margin) {
        return Math.ceil(hitTick / Math.max(0.1, speed)) + margin <= impactTicks;
    }
}
