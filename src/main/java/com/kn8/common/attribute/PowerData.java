// src/main/java/com/kn8/common/attribute/PowerData.java
package com.kn8.common.attribute;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.kn8.core.power.BodyStat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Estado de poder de um jogador (attachment {@code kn8:power}, PRIVADO: nunca usa sync nativo). Fase 4 secao 4.1.
 *
 * <p>Persistido e copiado na morte: limite pessoal de Release (o "treinado"), XP de treino, teto forcado por comando,
 * talento sorteado, atributos do corpo (0.5.0), energia, Controle (C) e aptidao. Volateis (zerados no login e na
 * morte, como manda a Fase 4): % ativa, tecla de Release, stamina, calor e marcas de tempo. Stamina negativa
 * significa "encher ate o maximo no proximo tick".</p>
 */
public final class PowerData {

    /** Sem teto forcado: o teto vem da patente. */
    public static final int NO_CAP_OVERRIDE = -1;
    private static final long NEVER = Long.MIN_VALUE;

    // Persistidos
    private int trainedRelease;
    private int releaseXp;
    private int capOverride = NO_CAP_OVERRIDE;
    private double energy = -1;
    private int control = 1;
    private int controlXp;
    private final Map<String, Integer> aptitude = new HashMap<>();
    private boolean talentRolled;
    private boolean talentRare;
    private final Map<BodyStat, Integer> bodyLevel = new EnumMap<>(BodyStat.class);
    private final Map<BodyStat, Integer> bodyXp = new EnumMap<>(BodyStat.class);

    // Volateis
    private double active;
    private int releaseInput;
    private final Map<BodyStat, Double> bodyXpFraction = new EnumMap<>(BodyStat.class);
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;
    private long lastAlarmTick = NEVER;
    private double strain;
    private long fatigueUntilTick = NEVER;
    private double stamina = -1;
    private double heat;
    private long lastStaminaSpendTick = NEVER;
    private long lastCombatTick = NEVER;
    private boolean winded;
    private int lastAppliedRelease = -1;
    private PowerView lastSentView;

    public static final Codec<PowerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100).optionalFieldOf("trained_release", 0).forGetter(PowerData::trainedRelease),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("release_xp", 0).forGetter(PowerData::releaseXp),
            Codec.intRange(NO_CAP_OVERRIDE, 100).optionalFieldOf("cap_override", NO_CAP_OVERRIDE)
                    .forGetter(PowerData::capOverride),
            Codec.DOUBLE.optionalFieldOf("energy", -1.0).forGetter(PowerData::energy),
            Codec.intRange(1, 100).optionalFieldOf("control", 1).forGetter(PowerData::control),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("control_xp", 0).forGetter(PowerData::controlXp),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(0, 5)).optionalFieldOf("aptitude", Map.of())
                    .forGetter(PowerData::aptitude),
            Codec.BOOL.optionalFieldOf("talent_rolled", false).forGetter(PowerData::talentRolled),
            Codec.BOOL.optionalFieldOf("talent_rare", false).forGetter(PowerData::talentRare),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("body_level", Map.of())
                    .forGetter(data -> byKey(data.bodyLevel)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("body_xp", Map.of())
                    .forGetter(data -> byKey(data.bodyXp))
    ).apply(i, PowerData::new));

    public PowerData() {
    }

    private PowerData(int trainedRelease, int releaseXp, int capOverride, double energy, int control, int controlXp,
            Map<String, Integer> aptitude, boolean talentRolled, boolean talentRare, Map<String, Integer> bodyLevel,
            Map<String, Integer> bodyXp) {
        this.trainedRelease = trainedRelease;
        this.releaseXp = releaseXp;
        this.capOverride = capOverride;
        this.energy = energy;
        this.control = control;
        this.controlXp = controlXp;
        this.aptitude.putAll(aptitude);
        this.talentRolled = talentRolled;
        this.talentRare = talentRare;
        fromKey(bodyLevel, this.bodyLevel);
        fromKey(bodyXp, this.bodyXp);
    }

    private static Map<String, Integer> byKey(Map<BodyStat, Integer> values) {
        Map<String, Integer> out = new HashMap<>();
        values.forEach((stat, value) -> out.put(stat.key(), value));
        return out;
    }

    /** Chaves desconhecidas no save sao ignoradas (atributo removido numa versao futura nao derruba o load). */
    private static void fromKey(Map<String, Integer> values, Map<BodyStat, Integer> into) {
        for (BodyStat stat : BodyStat.values()) {
            Integer value = values.get(stat.key());
            if (value != null) {
                into.put(stat, Math.max(0, value));
            }
        }
    }

    public int trainedRelease() {
        return trainedRelease;
    }

    void setTrainedRelease(int value) {
        trainedRelease = Math.max(0, Math.min(100, value));
    }

    public int releaseXp() {
        return releaseXp;
    }

    void setReleaseXp(int value) {
        releaseXp = Math.max(0, value);
    }

    public int capOverride() {
        return capOverride;
    }

    void setCapOverride(int value) {
        capOverride = value < 0 ? NO_CAP_OVERRIDE : Math.min(100, value);
    }

    public double energy() {
        return energy;
    }

    void setEnergy(double value) {
        energy = value;
    }

    public int control() {
        return control;
    }

    public int controlXp() {
        return controlXp;
    }

    public Map<String, Integer> aptitude() {
        return Map.copyOf(aptitude);
    }

    /** O limite pessoal ja foi sorteado (0.5.0)? */
    public boolean talentRolled() {
        return talentRolled;
    }

    public boolean talentRare() {
        return talentRare;
    }

    void setTalent(boolean rare) {
        talentRolled = true;
        talentRare = rare;
    }

    public int bodyLevel(BodyStat stat) {
        return bodyLevel.getOrDefault(stat, 0);
    }

    public int bodyXp(BodyStat stat) {
        return bodyXp.getOrDefault(stat, 0);
    }

    void setBody(BodyStat stat, int level, int xp) {
        bodyLevel.put(stat, Math.max(0, level));
        bodyXp.put(stat, Math.max(0, xp));
    }

    /** Guarda a parte fracionaria do XP do corpo (ex.: 0,2 por bloco corrido) ate virar ponto inteiro. */
    int takeBodyXp(BodyStat stat, double amount) {
        double total = bodyXpFraction.getOrDefault(stat, 0.0) + Math.max(0.0, amount);
        int whole = (int) Math.floor(total);
        bodyXpFraction.put(stat, total - whole);
        return whole;
    }

    /** % de Release ativa (0.5.0): o que o jogador liberou na tecla; volatil (comeca desligado). */
    public double active() {
        return active;
    }

    void setActive(double value) {
        active = Math.max(0.0, Math.min(100.0, value));
    }

    /** Tecla de Release segurada: +1 sobe, -1 desce, 0 solta. */
    int releaseInput() {
        return releaseInput;
    }

    void setReleaseInput(int value) {
        releaseInput = Integer.signum(value);
    }

    double lastX() {
        return lastX;
    }

    double lastZ() {
        return lastZ;
    }

    void setLastPosition(double x, double z) {
        lastX = x;
        lastZ = z;
    }

    /** Desgaste acumulado acima do limite (vira fadiga ao voltar para dentro dele). */
    double strain() {
        return strain;
    }

    void setStrain(double value) {
        strain = Math.max(0.0, value);
    }

    long fatigueUntilTick() {
        return fatigueUntilTick;
    }

    void setFatigueUntilTick(long tick) {
        fatigueUntilTick = tick;
    }

    long lastAlarmTick() {
        return lastAlarmTick;
    }

    void setLastAlarmTick(long tick) {
        lastAlarmTick = tick;
    }

    public double stamina() {
        return stamina;
    }

    void setStamina(double value) {
        stamina = value;
    }

    public double heat() {
        return heat;
    }

    void setHeat(double value) {
        heat = Math.max(0, value);
    }

    long lastStaminaSpendTick() {
        return lastStaminaSpendTick;
    }

    void setLastStaminaSpendTick(long tick) {
        lastStaminaSpendTick = tick;
    }

    long lastCombatTick() {
        return lastCombatTick;
    }

    void setLastCombatTick(long tick) {
        lastCombatTick = tick;
    }

    /** Sem folego para correr (Etapa 1 da 0.2); nao vai para o save: recalculado a cada tick. */
    boolean winded() {
        return winded;
    }

    void setWinded(boolean value) {
        winded = value;
    }

    int lastAppliedRelease() {
        return lastAppliedRelease;
    }

    void setLastAppliedRelease(int value) {
        lastAppliedRelease = value;
    }

    PowerView lastSentView() {
        return lastSentView;
    }

    void setLastSentView(PowerView view) {
        lastSentView = view;
    }

    static boolean never(long tick) {
        return tick == NEVER;
    }
}
