// src/main/java/com/kn8/core/soldier/SquadRules.java
package com.kn8.core.soldier;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras dos esquadroes (IA-5) em Java puro: quem o jogador pode recrutar, como os defensores de uma invasao se
 * dividem em esquadroes e onde fica o posto de cada um.
 */
public final class SquadRules {

    /** Ordens do esquadrao (aba Esquadrao do menu). */
    public enum Order {
        /** Segue o lider e luta com os kaiju perto dele. */
        FOLLOW,
        /** Fica no ponto da ordem e luta so ali perto. */
        STAY,
        /** Caca livremente (alcance de deteccao inteiro). */
        ATTACK,
        /** Para de lutar e volta para o lider. */
        RETREAT;

        public static Order byIndex(int index) {
            Order[] values = values();
            return index >= 0 && index < values.length ? values[index] : FOLLOW;
        }
    }

    /** Resultado de um pedido de recrutamento (a mensagem que o jogador ve). */
    public enum Recruit {
        OK, FULL, NO_SQUAD, SPECIAL_RANK, TAKEN
    }

    private SquadRules() {
    }

    /**
     * Pode recrutar? {@code squadSize} = tamanho do esquadrao da patente ({@code npc_squad_size}); soldado especial
     * so a partir da patente {@code specialRankOrder} (ordem da patente); soldado de outro lider ou de invasao nao.
     */
    public static Recruit canRecruit(int squadSize, int members, boolean special, int rankOrder,
            int specialRankOrder, boolean taken) {
        if (squadSize <= 0) {
            return Recruit.NO_SQUAD;
        }
        if (taken) {
            return Recruit.TAKEN;
        }
        if (members >= squadSize) {
            return Recruit.FULL;
        }
        if (special && rankOrder < specialRankOrder) {
            return Recruit.SPECIAL_RANK;
        }
        return Recruit.OK;
    }

    /** Divide {@code count} defensores em esquadroes de ate {@code size} (o ultimo pode ficar menor). */
    public static List<Integer> partition(int count, int size) {
        List<Integer> squads = new ArrayList<>();
        int perSquad = Math.max(1, size);
        for (int left = count; left > 0; left -= perSquad) {
            squads.add(Math.min(perSquad, left));
        }
        return squads;
    }

    /**
     * Desvio do angulo de chegada de cada soldado (bug 12 da Fase 0: todos iam ao mesmo ponto), entre
     * -{@code maxRadians} e +{@code maxRadians}, pelo id da entidade (angulo de ouro: ids seguidos bem espalhados).
     */
    public static double spreadOffset(int entityId, double maxRadians) {
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        double fraction = (entityId * golden / (Math.PI * 2.0)) % 1.0;
        if (fraction < 0) {
            fraction += 1.0;
        }
        return (fraction * 2.0 - 1.0) * maxRadians;
    }

    /**
     * Kaiju de cada esquadrao de invasao (bug 13: os defensores vagavam). Quem ja tem um kaiju vivo
     * ({@code current[i]} &gt;= 0) fica com ele; os outros pegam, entre os menos disputados, o kaiju mais perto do
     * proprio posto: os esquadroes se dividem entre os kaiju antes de dobrar em um. {@code posts} e {@code kaiju}
     * sao pontos (x, z); devolve o indice do kaiju de cada esquadrao (-1 sem kaiju).
     */
    public static int[] assign(double[][] posts, double[][] kaiju, int[] current) {
        int[] result = new int[posts.length];
        int[] load = new int[kaiju.length];
        for (int i = 0; i < posts.length; i++) {
            result[i] = current[i] >= 0 && current[i] < kaiju.length ? current[i] : -1;
            if (result[i] >= 0) {
                load[result[i]]++;
            }
        }
        for (int i = 0; i < posts.length; i++) {
            if (result[i] >= 0 || kaiju.length == 0) {
                continue;
            }
            int best = -1;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int k = 0; k < kaiju.length; k++) {
                double dx = kaiju[k][0] - posts[i][0];
                double dz = kaiju[k][1] - posts[i][1];
                double distance = dx * dx + dz * dz;
                if (best < 0 || load[k] < load[best] || (load[k] == load[best] && distance < bestDistance)) {
                    best = k;
                    bestDistance = distance;
                }
            }
            result[i] = best;
            load[best]++;
        }
        return result;
    }

    /** Posto do esquadrao {@code index} de {@code total}: em volta do centro, a {@code distance} blocos (x, z). */
    public static double[] post(double centerX, double centerZ, int index, int total, double distance) {
        double angle = Math.PI * 2.0 * index / Math.max(1, total);
        return new double[] {centerX + Math.cos(angle) * distance, centerZ + Math.sin(angle) * distance};
    }
}
