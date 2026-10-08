// src/main/java/com/kn8/core/power/TalentParams.java
package com.kn8.core.power;

/**
 * Talento de Release (0.5.0, decisao do Miguel): o limite pessoal inicial e sorteado uma vez por jogador. A maioria
 * comeca entre {@code commonMin} e {@code commonMax}%; com chance {@code rareChance} o talento e raro e comeca entre
 * {@code rareMin} e {@code rareMax}%.
 */
public record TalentParams(int commonMin, int commonMax, double rareChance, int rareMin, int rareMax) {

    /** Resultado do sorteio. */
    public record Talent(int limit, boolean rare) {
    }

    /**
     * Sorteia o talento a partir de dois numeros em [0, 1) (o chamador passa o aleatorio; aqui fica so a regra, para
     * o JUnit).
     */
    public Talent roll(double chanceRoll, double valueRoll) {
        boolean rare = chanceRoll < rareChance;
        int min = rare ? rareMin : commonMin;
        int max = Math.max(min, rare ? rareMax : commonMax);
        int span = max - min + 1;
        int value = min + Math.min(span - 1, (int) Math.floor(Math.max(0.0, valueRoll) * span));
        return new Talent(Math.max(0, Math.min(100, value)), rare);
    }
}
