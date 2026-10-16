package com.kn8.common.soldier;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.parkour.ParkourProbe;
import com.kn8.core.soldier.LeapMath;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Salto tatico do soldado (MOV-2, plano de mobilidade v3 §4): sobre obstaculo de 1,2 a {@code [soldierAi]
 * leapMaxHeight} blocos (para cima dele ou por cima, se for so um muro), sobre vao e para plataforma ao alcance.
 * Antes de saltar confere no mundo onde pousa ({@link MoveSafety#restsAt}) e se o arco cabe (teto); no ar segura o
 * controle ate pousar (a navegacao vanilla nao recalcula no meio do salto). Duas fases como o vault do jogador: sobe
 * ate passar da altura e so entao vai para a frente (nao bate na quina). Um por soldado, dentro do {@link StuckWatch}.
 */
public final class SoldierLeap {

    /** Distancia conferida do obstaculo a frente (blocos). */
    private static final double OBSTACLE_REACH = 0.8;
    /** Ate esta altura o pulo vanilla resolve (o {@link StuckWatch} usa o pulo comum). */
    private static final double JUMPABLE = 1.2;
    /** Folga acima do obstaculo no arco. */
    private static final double MARGIN = 0.3;
    /** Passo das distancias conferidas para o pouso (blocos). */
    private static final double SAMPLE = 0.5;
    /** Quanto o pouso entra alem da quina (blocos). */
    private static final double INWARD = 1.0;
    /** Corrida ate a beira do vao que o salto aceita (blocos). */
    private static final double RUN_UP = 1.5;
    /** Tempo maximo segurando o controle no ar. */
    private static final int MAX_AIR_TICKS = 40;

    private Vec3 landing;
    private double clearY;
    private boolean forward;
    private int airTicks;
    private long readyAt;

    /** Salto em andamento: o goal de combate nao mexe na navegacao. */
    public boolean active() {
        return landing != null;
    }

    /** Chamado todo tick enquanto {@link #active()}: segunda fase, fim no chao ou por tempo. */
    public void tickAir(Mob mob) {
        if (landing == null) {
            return;
        }
        airTicks++;
        // Salto controlado: a queda dele nao conta como dano (pouso ja conferido, no maximo landingMaxDrop).
        mob.fallDistance = 0.0F;
        if (!forward && mob.getY() >= clearY) {
            forward = true;
            Vec3 velocity = mob.getDeltaMovement();
            Vec3 flat = new Vec3(landing.x - mob.getX(), 0.0, landing.z - mob.getZ());
            int rest = LeapMath.ticksToReach(velocity.y, landing.y - mob.getY());
            double speed = Math.min(LeapMath.MAX_HORIZONTAL, LeapMath.horizontalFor(flat.length(), rest));
            Vec3 push = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize().scale(speed);
            mob.setDeltaMovement(push.x, velocity.y, push.z);
            mob.hurtMarked = true;
        }
        if ((airTicks > 2 && mob.onGround()) || airTicks > MAX_AIR_TICKS) {
            AiTuning.logMove(mob, mob.onGround() && mob.position().distanceTo(landing) < 1.5
                    ? "salto pousou" : "salto terminou fora do pouso");
            landing = null;
        }
    }

    /**
     * Procura um salto na direcao de {@code toward} e salta. Ordem: obstaculo alto a frente (pousa em cima ou atras
     * dele) e, sem obstaculo, vao ou plataforma a frente. Falso se nao ha salto seguro ou ainda esta na espera.
     */
    public boolean tryLeap(Mob mob, Vec3 toward) {
        long now = mob.level().getGameTime();
        // Mob sem IA nao anda pela velocidade: o salto ficaria parado no ar.
        if (now < readyAt || !mob.onGround() || mob.isNoGravity() || mob.isNoAi() || mob.isPassenger()) {
            return false;
        }
        Vec3 dir = new Vec3(toward.x - mob.getX(), 0.0, toward.z - mob.getZ());
        if (dir.lengthSqr() < 1.0E-4) {
            return false;
        }
        dir = dir.normalize();
        double maxHeight = ServerConfig.SOLDIER_LEAP_MAX_HEIGHT.get();
        double maxDistance = ServerConfig.SOLDIER_LEAP_MAX_DISTANCE.get();
        Vec3 base = mob.position();
        double height = ParkourProbe.obstacleHeight(mob, dir, OBSTACLE_REACH, maxHeight);
        if (height < 0.0) {
            return false;
        }
        if (height > JUMPABLE) {
            // Em cima do obstaculo; se for so um muro, do outro lado na mesma altura ou abaixo.
            for (double d = OBSTACLE_REACH + SAMPLE; d <= maxDistance + 1.0E-6; d += SAMPLE) {
                Vec3 top = base.add(dir.scale(d)).add(0.0, height, 0.0);
                if (MoveSafety.restsAt(mob, top)) {
                    return launch(mob, inward(mob, top, dir), height, "subindo " + String.format("%.1f", height));
                }
                for (int drop = 0; drop <= ServerConfig.SOLDIER_LANDING_MAX_DROP.get(); drop++) {
                    Vec3 behind = new Vec3(top.x, Math.floor(base.y) - drop, top.z);
                    if (d > OBSTACLE_REACH + 1.0 && MoveSafety.restsAt(mob, behind)
                            && MoveSafety.fits(mob, behind.add(0.0, height + MARGIN, 0.0))) {
                        return launch(mob, behind, height, "por cima do muro de " + String.format("%.1f", height));
                    }
                }
            }
            return false;
        }
        if (height > 0.0) {
            // Degrau baixo: andar resolve.
            return false;
        }
        // Beira do vao ate RUN_UP blocos a frente (o caminho vanilla nem sempre leva ate a beira); sem vao, andar
        // resolve.
        double edge = -1.0;
        for (double d = SAMPLE; d <= RUN_UP + 1.0E-6; d += SAMPLE) {
            if (!walkable(mob, base.add(dir.scale(d)))) {
                edge = d;
                break;
            }
        }
        if (edge < 0.0) {
            return false;
        }
        // Vao ou plataforma: primeiro ponto com chao depois da beira, ate a queda maxima abaixo e o salto maximo acima.
        // Primeiro so pousos no mesmo nivel ou acima (o outro lado do vao), depois os mais baixos (fundo da vala).
        for (int pass = 0; pass < 2; pass++) {
            int lowest = pass == 0 ? -1 : -ServerConfig.SOLDIER_LANDING_MAX_DROP.get();
            if (leapGap(mob, dir, base, edge, maxHeight, maxDistance, lowest)) {
                return true;
            }
        }
        return false;
    }

    /** Da para andar ate {@code p}: chao no mesmo nivel ou ate a queda maxima abaixo. */
    private static boolean walkable(Mob mob, Vec3 p) {
        for (int dy = 0; dy >= -ServerConfig.SOLDIER_LANDING_MAX_DROP.get(); dy--) {
            if (MoveSafety.restsAt(mob, new Vec3(p.x, Math.floor(mob.getY()) + dy, p.z))) {
                return true;
            }
        }
        return false;
    }

    private boolean leapGap(Mob mob, Vec3 dir, Vec3 base, double edge, double maxHeight, double maxDistance,
            int lowest) {
        for (double d = edge + SAMPLE; d <= edge + maxDistance + 1.0E-6; d += SAMPLE) {
            Vec3 ahead = base.add(dir.scale(d));
            for (double dy = Math.floor(maxHeight); dy >= lowest; dy--) {
                Vec3 spot = new Vec3(ahead.x, Math.floor(base.y) + dy, ahead.z);
                if (MoveSafety.restsAt(mob, spot)) {
                    double clearance = Math.max(0.0, dy);
                    Vec3 middle = base.add(dir.scale(d * 0.5));
                    double arc = LeapMath.peak(LeapMath.verticalFor(clearance, MARGIN));
                    if (MoveSafety.fits(mob, base.add(0.0, arc, 0.0))
                            && MoveSafety.fits(mob, middle.add(0.0, Math.max(dy, 0.0) + MARGIN, 0.0))) {
                        return launch(mob, inward(mob, spot, dir), clearance,
                                dy >= 1.0 ? "para a plataforma" : "por cima do vao");
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * O primeiro ponto com chao fica na quina (a caixa mal encosta): mira {@code INWARD} blocos para dentro quando ali
     * tambem tem chao (cenario "vao": um dos soldados pousava na beira e caia na vala).
     */
    private static Vec3 inward(Mob mob, Vec3 spot, Vec3 dir) {
        Vec3 deeper = spot.add(dir.scale(INWARD));
        return MoveSafety.restsAt(mob, deeper) ? deeper : spot;
    }

    private boolean launch(Mob mob, Vec3 spot, double clearance, String what) {
        double dy = spot.y - mob.getY();
        double distance = Math.hypot(spot.x - mob.getX(), spot.z - mob.getZ());
        if (!LeapMath.feasible(clearance, dy, distance, MARGIN)) {
            AiTuning.logMove(mob, "salto " + what + " longe demais");
            return false;
        }
        double vy = LeapMath.verticalFor(Math.max(clearance, dy), MARGIN);
        landing = spot;
        clearY = mob.getY() + clearance + 0.05;
        forward = clearance <= 0.0;
        airTicks = 0;
        readyAt = mob.level().getGameTime() + ServerConfig.SOLDIER_LEAP_COOLDOWN_TICKS.get();
        mob.getNavigation().stop();
        Vec3 push = Vec3.ZERO;
        if (forward) {
            Vec3 flat = new Vec3(spot.x - mob.getX(), 0.0, spot.z - mob.getZ()).normalize();
            int rest = LeapMath.ticksToReach(vy, dy);
            push = flat.scale(Math.min(LeapMath.MAX_HORIZONTAL, LeapMath.horizontalFor(distance, rest)));
        }
        mob.setDeltaMovement(push.x, vy, push.z);
        mob.hurtMarked = true;
        mob.setOnGround(false);
        if (mob instanceof SoldierEntity soldier) {
            soldier.triggerAnim("action", "vault");
        }
        AiTuning.logMove(mob, "salto " + what);
        return true;
    }
}
