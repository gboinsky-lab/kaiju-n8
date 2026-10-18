// src/main/java/com/kn8/common/soldier/special/HoshinaCombatGoal.java
package com.kn8.common.soldier.special;

import java.util.EnumSet;
import java.util.Optional;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.No9Service;
import com.kn8.common.soldier.DangerAvoid;
import com.kn8.common.soldier.MoveSafety;
import com.kn8.common.soldier.ShooterCover;
import com.kn8.common.soldier.StuckWatch;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Combate do Hoshina (0.6-D), na ordem de prioridade do texto do Miguel: aproximar rapido (dash para a frente com o
 * alvo longe), atacar com as tecnicas do perfil (a de maior prioridade pronta e ao alcance) e, sem tecnica pronta,
 * golpes basicos com a espada. Esquiva, Kaeshi-uchi e parry sao reacoes da propria entidade (HoshinaEntity#react).
 *
 * <p>Atiradores com {@code ranged} no perfil (Mina, Reno; Miguel, 2026-10-15) nao param no alcance da arma: recuam
 * se o alvo chega perto, correm em volta dele na faixa de distancia para distrai-lo e perseguem se ele se afasta,
 * atirando em movimento ({@link #rangedTick}).</p>
 */
final class HoshinaCombatGoal extends Goal {

    private static final double SPEED = 1.15;
    private static final int REPATH_TICKS = 8;
    /** MOV-4: intervalo da conferida da area de golpe pesado. */
    private static final int DANGER_CHECK_TICKS = 2;
    /** Folga em volta da distancia de manter, em blocos (sem ficar indo e voltando na borda). */
    private static final double KEEP_SLACK = 3.0;
    /** Quanto a distancia precisa crescer entre duas reavaliacoes para contar como "o alvo esta se afastando". */
    private static final double LEAVING_DELTA = 0.6;
    /** Passo do giro em volta do alvo a cada reavaliacao (radianos, ~35 graus). */
    private static final double ORBIT_STEP = 0.6;

    /**
     * Luta travada (2026-10-15, No. 8 x forma preta): sem ninguem perder vida por este tempo com o alvo perto, sai
     * para o lado. Caso medido: o No. 8 numa cratera de 3 blocos e a forma preta de 5 m parada por cima dela, sem
     * caber no buraco; o soco horizontal nao alcanca quem esta em cima e ela nao alcanca quem esta no fundo.
     */
    private static final int STUCK_TICKS = 160;
    private static final double STUCK_RANGE = 8.0;
    /** Distancia do ponto para onde sai ao destravar (blocos, para o lado do alvo). */
    private static final double UNSTUCK_STEP = 5.0;
    private static final double UNSTUCK_SPEED = 1.3;
    /** Tempo correndo para o ponto ao lado antes de voltar a lutar (o golpe de perto pararia a navegacao). */
    private static final int UNSTUCK_RUN_TICKS = 30;
    /** Alvo acima disso (blocos, pe contra pe) e perto: pula para alcancar. */
    private static final double JUMP_ABOVE = 1.5;
    /** IA-5: a esta distancia da borda do alvo, passa a chegar pelo proprio angulo. */
    private static final double SPREAD_RANGE = 8.0;

    private final HoshinaEntity hoshina;
    /** MOV-1: travamento de movimento (a "luta travada" do unstick e outra coisa: perto e ninguem se fere). */
    private final StuckWatch stuck = new StuckWatch();
    private final ShooterCover cover = new ShooterCover();
    /** MOV-4: saida da area de golpe pesado, conferida a cada DANGER_CHECK_TICKS. */
    private Vec3 evade;
    private int dangerCheck;
    private int repath;
    private int stuckTicks;
    private int unstuckRun;
    private float lastOwnHealth = -1.0F;
    private float lastTargetHealth = -1.0F;
    private double lastEdge = -1.0;
    private int orbitSide = 1;
    /** IA-3: perfil tatico (reposicionar depois da tecnica, recuar com pouca vida). */
    private boolean wasUsingTechnique;
    private long repositionUntil;
    private long retreatUntil;
    private long retreatReadyAt;
    private int orbitTicks;

    HoshinaCombatGoal(HoshinaEntity hoshina) {
        this.hoshina = hoshina;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = hoshina.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        hoshina.setAggressive(true);
        // IA-5: metade orbita para cada lado (pelo id).
        orbitSide = (hoshina.getId() & 1) == 0 ? 1 : -1;
    }

    @Override
    public void stop() {
        hoshina.setAggressive(false);
        hoshina.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = hoshina.getTarget();
        if (target == null) {
            return;
        }
        hoshina.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (hoshina.isUsingTechnique()) {
            wasUsingTechnique = true;
            return;
        }
        if (stuck.busy(hoshina)) {
            return;
        }
        // MOV-4: dentro da area de um golpe pesado em preparo (sem esquiva pronta), sai andando dela.
        if (--dangerCheck <= 0) {
            dangerCheck = DANGER_CHECK_TICKS;
            evade = DangerAvoid.exitSpot(hoshina).orElse(null);
            if (evade != null) {
                AiTuning.logMove(hoshina, "saindo da area do golpe pesado");
            }
        }
        if (evade != null) {
            hoshina.getNavigation().moveTo(evade.x, evade.y, evade.z, ServerConfig.SOLDIER_DANGER_ESCAPE_SPEED.get());
            // MOV-5E: atirador atira enquanto sai da area (antes parava de atirar a cada golpe pesado do kaiju).
            if (hoshina.isShooter() && hoshina.profile().flatMap(SpecialSoldierDef::ranged).isPresent()
                    && hoshina.getSensing().hasLineOfSight(target) && hoshina.edgeTo(target) <= hoshina.attackReach()) {
                hoshina.startAttack(target);
            }
            return;
        }
        if (unstick(target)) {
            return;
        }
        // MOV-1: quer chegar ao alvo (corpo a corpo longe dele) e nao sai do lugar.
        boolean wantsToMove = !hoshina.isShooter() && hoshina.edgeTo(target) > hoshina.attackReach() * 0.9;
        if (stuck.tick(hoshina, wantsToMove, target.position())) {
            return;
        }
        if (tactics(target)) {
            return;
        }
        // IA-1B: atirador sem municao (arma branca na mao) luta de perto como os outros.
        Optional<SpecialSoldierDef.Ranged> ranged = hoshina.profile().flatMap(SpecialSoldierDef::ranged)
                .filter(value -> hoshina.isShooter());
        // MOV-5D (Miguel: "ela deve se ajustar de acordo com o combate"): cobertura so quando o combate pede: para
        // recarregar e, contra kaiju que atira, so sob pressao (vida abaixo de cover_health) e sem uma janela para
        // pressionar (alvo regenerando ou de guarda aberta: escondida ela daria tempo de cura ao No. 9). Escondida nao
        // comeca tecnica. Fora disso luta de longe (recuo e orbita) como antes.
        boolean covered = false;
        if (ranged.isPresent() && ServerConfig.SOLDIER_SPECIAL_COVER.get()) {
            covered = cover.tick(hoshina, target, hoshina.edgeTo(target),
                    hoshina.getSensing().hasLineOfSight(target), hoshina.wantsCoverFrom(target));
            if (covered && cover.hiding()) {
                return;
            }
        } else {
            cover.clear(hoshina);
        }
        Optional<String> technique = hoshina.chooseTechnique(target);
        if (technique.isPresent() && hoshina.startTechnique(technique.get(), target)) {
            return;
        }
        if (covered) {
            return;
        }
        if (ranged.isPresent()) {
            rangedTick(target, ranged.get());
            return;
        }
        double edge = hoshina.edgeTo(target);
        if (edge > hoshina.attackReach() * 0.9) {
            hoshina.gapClose(target);
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                // IA-5: perto, chega pelo proprio angulo (sem empilhar com os aliados).
                if (edge < SPREAD_RANGE) {
                    Vec3 spot = hoshina.approachSpot(target, hoshina.attackReach() * 0.6);
                    hoshina.getNavigation().moveTo(spot.x, spot.y, spot.z, SPEED);
                } else {
                    hoshina.getNavigation().moveTo(target, SPEED);
                }
            }
        } else {
            hoshina.getNavigation().stop();
            hoshina.startAttack(target);
        }
    }

    /**
     * IA-3: perfil tatico do personagem. Com pouca vida ({@code damage_tolerance}) recua ate {@code safety_distance};
     * no fim de uma tecnica, com chance 1 - {@code aggression}, reposiciona (passo para o lado ou volta a
     * {@code preferred_distance}) em vez de emendar outro golpe. Devolve true se o tick ja foi usado.
     */
    private boolean tactics(LivingEntity target) {
        SpecialSoldierDef.Tactics tactics = hoshina.profile().map(SpecialSoldierDef::tactics)
                .orElse(SpecialSoldierDef.Tactics.DEFAULT);
        long now = hoshina.level().getGameTime();
        if (wasUsingTechnique) {
            wasUsingTechnique = false;
            if (hoshina.getRandom().nextFloat() >= tactics.aggression()) {
                repositionUntil = now + tactics.repositionTicks();
                orbitSide = hoshina.getRandom().nextBoolean() ? 1 : -1;
            }
        }
        if (tactics.damageTolerance() > 0.0F && now >= retreatReadyAt
                && hoshina.getHealth() < hoshina.getMaxHealth() * tactics.damageTolerance()) {
            retreatUntil = now + tactics.retreatTicks();
            retreatReadyAt = now + tactics.retreatCooldownTicks();
        }
        if (now < retreatUntil) {
            double away = Math.max(tactics.safetyDistance(), hoshina.attackReach() + KEEP_SLACK);
            if (hoshina.edgeTo(target) < away && --repath <= 0) {
                repath = REPATH_TICKS;
                if (!moveAround(target, away, ORBIT_STEP * 0.5, SPEED * 1.2)) {
                    orbitSide = -orbitSide;
                }
            }
            return true;
        }
        if (now < repositionUntil && tactics.exploitOpenings() > 0 && target instanceof KaijuEntity open
                && open.isOpen()) {
            // MOV-5B: guarda aberta do kaiju: pressiona em vez de reposicionar (a tecnica que mais tira tem bonus).
            repositionUntil = 0;
            AiTuning.logMove(hoshina, "brecha do kaiju: pressiona");
        }
        if (now < repositionUntil && hoshina.profile().map(def -> def.rhythm().pressRegeneration() > 0).orElse(false)
                && No9Service.regenerating(target)) {
            // MOV-5C: o kaiju regenera nas pausas (a cura espera alguns ticks depois de cada golpe): sem recuar.
            repositionUntil = 0;
            AiTuning.logMove(hoshina, "alvo regenerando: pressiona");
        }
        if (now < repositionUntil) {
            double distance = Math.max(tactics.preferredDistance(), hoshina.attackReach() * 0.8);
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                if (!moveAround(target, distance, tactics.lateralStep() ? ORBIT_STEP : 0.0, SPEED)) {
                    orbitSide = -orbitSide;
                }
            }
            return true;
        }
        return false;
    }

    /** Atirador: recua, circula ou persegue pela distancia (borda a borda) e atira em movimento. */
    private void rangedTick(LivingEntity target, SpecialSoldierDef.Ranged ranged) {
        double edge = hoshina.edgeTo(target);
        boolean sees = hoshina.getSensing().hasLineOfSight(target);
        if (sees && edge <= hoshina.attackReach()) {
            hoshina.startAttack(target);
        }
        if (--repath > 0) {
            return;
        }
        repath = REPATH_TICKS;
        boolean leaving = lastEdge >= 0.0 && edge - lastEdge > LEAVING_DELTA;
        lastEdge = edge;
        double keep = ranged.keepDistance();
        if (hoshina.isReloading()) {
            // IA-1B (Miguel: "recuar e recarregar"): recarregando, se afasta ate a distancia de manter + o recuo.
            double away = keep + ServerConfig.SOLDIER_RELOAD_RETREAT.get();
            if (edge < away && !moveAround(target, away, ORBIT_STEP * 0.5, ranged.retreatSpeed())) {
                orbitSide = -orbitSide;
                moveAround(target, away, ORBIT_STEP * 0.5, ranged.retreatSpeed());
            }
            return;
        }
        if (!sees || edge > keep + KEEP_SLACK || (leaving && edge > ranged.retreatDistance())) {
            // Longe, sem visao ou o alvo indo embora: persegue (com o dash do perfil se estiver muito longe).
            hoshina.gapClose(target);
            hoshina.getNavigation().moveTo(target, ranged.chaseSpeed());
            return;
        }
        if (edge < ranged.retreatDistance()) {
            // Perto demais: recua na direcao oposta ao alvo, com um pouco de giro para nao correr em linha reta.
            if (!moveAround(target, keep, ORBIT_STEP * 0.5, ranged.retreatSpeed())) {
                orbitSide = -orbitSide;
                moveAround(target, keep, ORBIT_STEP * 0.5, ranged.retreatSpeed());
            }
            return;
        }
        if (!ranged.orbit()) {
            hoshina.getNavigation().stop();
            return;
        }
        orbitTicks += REPATH_TICKS;
        if (orbitTicks >= ranged.orbitSwitchTicks()) {
            orbitTicks = 0;
            orbitSide = -orbitSide;
        }
        // Na faixa de distancia: corre em volta do alvo (distrai e nao fica parado na frente dele).
        if (!moveAround(target, keep, ORBIT_STEP, ranged.orbitSpeed())) {
            orbitSide = -orbitSide;
            orbitTicks = 0;
            moveAround(target, keep, ORBIT_STEP, ranged.orbitSpeed());
        }
    }

    /**
     * Vai para o ponto a {@code keep} blocos da borda do alvo, girado {@code step} radianos em volta dele no lado
     * {@link #orbitSide}. Falso se nao ha caminho.
     */
    private boolean moveAround(LivingEntity target, double keep, double step, double speed) {
        Vec3 center = target.position();
        Vec3 from = hoshina.position().subtract(center);
        // IA-5: desvio proprio de cada um (atiradores nao orbitam todos no mesmo ponto).
        double angle = Math.atan2(from.z, from.x) + step * orbitSide + hoshina.spreadOffset() * 0.5;
        double radius = keep + target.getBbWidth() * 0.5;
        Vec3 spot = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return hoshina.getNavigation().moveTo(spot.x, hoshina.getY(), spot.z, speed);
    }

    /**
     * Destrava a luta: perto do alvo e sem ninguem perder vida por {@link #STUCK_TICKS}, pula e corre para um ponto
     * ao lado do alvo, na altura dele (sai da cratera ou de baixo dele). Alvo acima e perto: pula. Verdadeiro
     * enquanto esta saindo (o resto do tick fica para depois).
     */
    private boolean unstick(LivingEntity target) {
        if (unstuckRun > 0) {
            unstuckRun--;
            return true;
        }
        double edge = hoshina.edgeTo(target);
        if (target.getY() - hoshina.getY() > JUMP_ABOVE && edge < STUCK_RANGE && hoshina.onGround()) {
            hoshina.getJumpControl().jump();
        }
        boolean progress = hoshina.getHealth() != lastOwnHealth || target.getHealth() != lastTargetHealth;
        lastOwnHealth = hoshina.getHealth();
        lastTargetHealth = target.getHealth();
        if (progress || edge > STUCK_RANGE) {
            stuckTicks = 0;
            return false;
        }
        if (++stuckTicks < STUCK_TICKS) {
            return false;
        }
        stuckTicks = 0;
        Vec3 from = hoshina.position().subtract(target.position());
        double radius = UNSTUCK_STEP + target.getBbWidth() * 0.5;
        hoshina.getJumpControl().jump();
        // MOV-1: so para um ponto ao lado do alvo onde da para ficar em pe (chao, sem bloco no corpo); tenta os dois
        // lados. Antes ia para a altura do alvo as cegas (podia ser o meio do ar ou dentro de um bloco).
        for (int attempt = 0; attempt < 2; attempt++) {
            double angle = Math.atan2(from.z, from.x) + Math.PI * 0.5 * orbitSide;
            orbitSide = -orbitSide;
            Vec3 wanted = target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            Optional<Vec3> spot = MoveSafety.standable(hoshina, wanted);
            if (spot.isPresent()) {
                hoshina.getNavigation().moveTo(spot.get().x, spot.get().y, spot.get().z, UNSTUCK_SPEED);
                AiTuning.logMove(hoshina, "luta travada, saindo para o lado");
                unstuckRun = UNSTUCK_RUN_TICKS;
                return true;
            }
        }
        AiTuning.logMove(hoshina, "luta travada sem lugar para sair");
        return false;
    }
}
