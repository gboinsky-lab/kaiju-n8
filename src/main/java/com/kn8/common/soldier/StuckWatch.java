// src/main/java/com/kn8/common/soldier/StuckWatch.java
package com.kn8.common.soldier;

import java.util.Optional;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.parkour.ParkourProbe;
import com.kn8.core.soldier.LandingMath;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Travamento de movimento (MOV-1, plano de mobilidade v3 §4 "deteccao de travamento"): o soldado quer andar e, por
 * {@code [soldierAi] stuckTicks}, sai menos de {@code stuckMinMove} blocos do lugar. Saida, sem teleporte: obstaculo
 * baixo a frente → pula; senao anda para um ponto ao lado onde da para ficar em pe ({@link MoveSafety#standable}),
 * tentando os dois lados. Um por soldado (estado dentro do goal de combate).
 */
public final class StuckWatch {

    /** Tempo correndo para o lado antes de voltar a lutar. */
    private static final int ASIDE_TICKS = 30;
    private static final double ASIDE_SPEED = 1.2;
    /** Obstaculo a frente: distancia conferida e altura maxima que o pulo vanilla vence (blocos). */
    private static final double OBSTACLE_REACH = 0.8;
    private static final double JUMPABLE = 1.2;
    /** Conferida rapida do salto: andou menos que isto no intervalo = barrado. */
    private static final double QUICK_MIN_MOVE = 0.5;
    /** Volta por onde veio: tantas vezes o passo de destravar. */
    private static final double BACK_FACTOR = 2.0;

    private Vec3 from;
    private int ticks;
    private int asideTicks;
    private int side = 1;
    /** Travamentos seguidos sem sair do lugar entre eles: do segundo em diante tenta voltar por onde veio. */
    private int strikes;
    private Vec3 strikeAt;
    /** MOV-2: salto tatico e a conferida rapida (quase parado por {@code leapCheckTicks}). */
    private final SoldierLeap leap = new SoldierLeap();
    private Vec3 quickFrom;
    private int quickTicks;

    /** Verdadeiro enquanto salta ou esta saindo para o lado (o goal nao mexe na navegacao). */
    public boolean busy(Mob mob) {
        if (leap.active()) {
            leap.tickAir(mob);
            return true;
        }
        if (asideTicks > 0) {
            asideTicks--;
            return true;
        }
        return false;
    }

    /**
     * Chamado todo tick pelo goal de combate com a intencao do tick ({@code wantsToMove}) e o ponto para onde quer
     * ir. Ao travar, tenta sair e devolve verdadeiro.
     */
    public boolean tick(Mob mob, boolean wantsToMove, Vec3 toward) {
        if (!wantsToMove || from == null) {
            from = mob.position();
            quickFrom = from;
            ticks = 0;
            quickTicks = 0;
            return false;
        }
        // MOV-2: quase parado querendo andar (caminho do vanilla nao passa: muro, vao, plataforma) → salto, sem
        // esperar o travamento inteiro.
        if (++quickTicks >= ServerConfig.SOLDIER_LEAP_CHECK_TICKS.get()) {
            boolean blocked = mob.position().distanceTo(quickFrom) < QUICK_MIN_MOVE;
            quickFrom = mob.position();
            quickTicks = 0;
            if (blocked && leap.tryLeap(mob, toward)) {
                from = null;
                return true;
            }
        }
        if (++ticks < ServerConfig.SOLDIER_STUCK_TICKS.get()) {
            return false;
        }
        double moved = mob.position().distanceTo(from);
        from = mob.position();
        ticks = 0;
        if (!LandingMath.stuck(moved, ServerConfig.SOLDIER_STUCK_MIN_MOVE.get(), true)) {
            return false;
        }
        double unstuck = ServerConfig.SOLDIER_UNSTUCK_STEP.get();
        // Saiu para o lado e travou de novo perto do mesmo lugar (beco sem saida): conta mais um.
        strikes = strikeAt != null && mob.position().distanceTo(strikeAt) < unstuck * 2.0 ? strikes + 1 : 0;
        strikeAt = mob.position();
        return getOut(mob, toward);
    }

    private boolean getOut(Mob mob, Vec3 toward) {
        Vec3 dir = new Vec3(toward.x - mob.getX(), 0.0, toward.z - mob.getZ());
        if (dir.lengthSqr() < 1.0E-4) {
            return false;
        }
        dir = dir.normalize();
        if (leap.tryLeap(mob, toward)) {
            return true;
        }
        double height = ParkourProbe.obstacleHeight(mob, dir, OBSTACLE_REACH, JUMPABLE);
        if (height > 0.0 && mob.onGround()) {
            mob.getJumpControl().jump();
            AiTuning.logMove(mob, "travado, pulando obstaculo de " + String.format("%.1f", height));
            return true;
        }
        Vec3 right = new Vec3(-dir.z, 0.0, dir.x);
        double step = ServerConfig.SOLDIER_UNSTUCK_STEP.get();
        // Primeiro os dois lados (um pouco para a frente); travando de novo no mesmo lugar, volta por onde veio
        // (sair do beco e deixar o caminho ser refeito de fora) e depois os lados.
        Vec3[] wantedSpots = new Vec3[3];
        Vec3 back = mob.position().subtract(dir.scale(step * BACK_FACTOR));
        Vec3 sideA = mob.position().add(right.scale(step * side)).add(dir.scale(step * 0.5));
        Vec3 sideB = mob.position().add(right.scale(-step * side)).add(dir.scale(step * 0.5));
        side = -side;
        if (strikes > 0) {
            wantedSpots[0] = back;
            wantedSpots[1] = sideA;
            wantedSpots[2] = sideB;
        } else {
            wantedSpots[0] = sideA;
            wantedSpots[1] = sideB;
            wantedSpots[2] = back;
        }
        for (Vec3 wanted : wantedSpots) {
            Optional<Vec3> spot = MoveSafety.standable(mob, wanted);
            if (spot.isPresent() && mob.getNavigation().moveTo(spot.get().x, spot.get().y, spot.get().z,
                    ASIDE_SPEED)) {
                asideTicks = wanted == back ? ASIDE_TICKS * 2 : ASIDE_TICKS;
                AiTuning.logMove(mob, wanted == back ? "travado de novo, voltando por onde veio"
                        : "travado, saindo para o lado");
                return true;
            }
        }
        AiTuning.logMove(mob, "travado sem saida");
        return false;
    }
}
