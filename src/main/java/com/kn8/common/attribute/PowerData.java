// src/main/java/com/kn8/common/attribute/PowerData.java
package com.kn8.common.attribute;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Estado de poder de um jogador (attachment {@code kn8:power}, PRIVADO: nunca usa sync nativo). Fase 4 secao 4.1.
 *
 * <p>Persistido e copiado na morte: % treinada, XP de treino, teto forcado por comando, energia, Controle (C) e
 * aptidao. Volateis (zerados no login e na morte, como manda a Fase 4): Surto, stamina, calor, panico e marcas de
 * tempo. Stamina negativa significa "encher ate o maximo no proximo tick".</p>
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

    // Volateis
    private int surge;
    private double stamina = -1;
    private double heat;
    private long lastStaminaSpendTick = NEVER;
    private long lastCombatTick = NEVER;
    private long panicUntilTick = NEVER;
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
                    .forGetter(PowerData::aptitude)
    ).apply(i, PowerData::new));

    public PowerData() {
    }

    private PowerData(int trainedRelease, int releaseXp, int capOverride, double energy, int control, int controlXp,
            Map<String, Integer> aptitude) {
        this.trainedRelease = trainedRelease;
        this.releaseXp = releaseXp;
        this.capOverride = capOverride;
        this.energy = energy;
        this.control = control;
        this.controlXp = controlXp;
        this.aptitude.putAll(aptitude);
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

    public int surge() {
        return surge;
    }

    void setSurge(int value) {
        surge = Math.max(0, value);
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

    long panicUntilTick() {
        return panicUntilTick;
    }

    void setPanicUntilTick(long tick) {
        panicUntilTick = tick;
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
