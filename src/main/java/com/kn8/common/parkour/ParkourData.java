// src/main/java/com/kn8/common/parkour/ParkourData.java
package com.kn8.common.parkour;

/**
 * Estado de parkour de UM jogador no servidor (attachment {@code kn8:parkour}, nunca salvo nem sincronizado: o dono
 * recebe so o {@link ParkourStateS2C} na mudanca). A logica fica no {@link ParkourService}.
 */
public final class ParkourData {

    static final long NEVER = Long.MIN_VALUE / 2;

    ParkourState state = ParkourState.NONE;
    /** Wall-run: -1 parede a esquerda, +1 a direita. */
    int side;
    /** Direcao horizontal do estado (corrida na parede, slide) ou normal da parede (para onde o wall-jump empurra). */
    double dirX;
    double dirZ;
    double normalX;
    double normalZ;
    /** Segurar a borda: Y dos pes enquanto pendurado e altura que falta para subir. */
    double hangY;
    double ledgeHeight;
    /** TREINO: altura do corpo no ultimo tick da escalada (XP por bloco subido). */
    double climbY = Double.NaN;
    long startTick = NEVER;
    /** Ultimo tick em que a parede/chao do estado foi confirmado (folga para o atraso da rede). */
    long lastValidTick = NEVER;
    /** Fim do ultimo estado na parede (o wall-jump ainda vale por alguns ticks: tempo do coiote). */
    long leftWallTick = NEVER;
    ParkourState leftWallState = ParkourState.NONE;
    /** Ultimo tick em que o servidor viu o jogador correndo (agachar/pular cortam a corrida antes). */
    long lastSprintTick = NEVER;
    /** Proximo tick em que um impulso de parkour pode sair de novo. */
    long readyTick = NEVER;
    /** Normal da parede do ultimo wall-run (a mesma parede so vale de novo depois de tocar o chao). */
    double lastRunNormalX;
    double lastRunNormalZ;
    /** Ticks seguidos no chao. */
    int groundTicks;
    /** Paredes ja usadas para wall-jump seguido (sem subir a mesma parede para sempre). */
    int wallJumpsInAir;

    public ParkourState state() {
        return state;
    }
}
