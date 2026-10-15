// src/main/java/com/kn8/common/parkour/ParkourService.java
package com.kn8.common.parkour;

import com.kn8.KN8Constants;
import com.kn8.common.anim.AnimationBridge;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.KN8Ids;
import com.kn8.core.parkour.ParkourMath;
import com.kn8.core.power.BodyStat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Parkour do jogador no servidor (0.5.0-E, Biblioteca v22 Prioridade 4). O cliente manda a intencao
 * ({@link ParkourInputC2S}); aqui se confere a geometria ({@link ParkourProbe}, com a folga {@code probeReach}), a
 * stamina (com o fator de agilidade do corpo) e o estado, e se aplica o movimento:
 * <ul>
 *   <li>impulsos (salto longo, vault, escalada curta, wall-jump, subir a borda): velocidade nova com
 *   {@code hurtMarked}, como o dash;</li>
 *   <li>estados continuos (wall-run, wall-slide, segurar a borda, slide): aprovados aqui e enviados ao dono
 *   ({@link ParkourStateS2C}); o cliente aplica a fisica tick a tick e o servidor encerra por tempo, stamina ou perda
 *   da parede, zerando a queda enquanto o jogador esta na parede;</li>
 *   <li>rolamento: so no servidor, pelo Shift na hora da queda ({@link #onFall}).</li>
 * </ul>
 */
public final class ParkourService {

    private ParkourService() {
    }

    public static ParkourData data(ServerPlayer player) {
        return player.getData(KN8Attachments.PARKOUR);
    }

    /** Animacao de parkour do jogador: {@code kn8:player.parkour.<nome>} (player_animations/parkour.json). */
    public static ResourceLocation animation(String name) {
        return KN8Constants.id(KN8Ids.animationName("player", "parkour", name));
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }

    /** Jogador pode fazer parkour agora (vivo, no corpo, sem voar, nadar ou montar). */
    static boolean canParkour(ServerPlayer player) {
        return ServerConfig.PARKOUR_ENABLED.get() && player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isInWater() && !player.isSleeping() && !player.onClimbable();
    }

    // --- intencoes ----------------------------------------------------------------------------------------------

    public static void handle(ServerPlayer player, ParkourMove move, int sideHint) {
        ParkourData data = data(player);
        if (move == ParkourMove.STOP) {
            if (data.state != ParkourState.NONE) {
                end(player, data, true);
            }
            return;
        }
        if (!canParkour(player)) {
            return;
        }
        switch (move) {
            case LONG_JUMP -> longJump(player, data);
            case VAULT, CLIMB -> vaultOrClimb(player, data);
            case WALL_RUN -> wallRun(player, data, sideHint);
            case WALL_SLIDE -> wallSlide(player, data);
            case LEDGE_GRAB -> ledgeGrab(player, data);
            case WALL_JUMP -> wallJump(player, data);
            case LEDGE_CLIMB -> ledgeClimb(player, data);
            case SLIDE -> slide(player, data);
            default -> {
            }
        }
    }

    private static boolean ready(ServerPlayer player, ParkourData data) {
        return now(player) >= data.readyTick;
    }

    /** Gasta a stamina do movimento (com o fator de agilidade); false = recusado. */
    private static boolean pay(ServerPlayer player, double base) {
        return PowerService.tryConsumeStamina(player, ParkourMath.cost(base, PowerService.agilityCostFactor(player)));
    }

    /** Impulso aceito: velocidade nova para o cliente, animacao, som, poeira e treino da agilidade. */
    private static void impulse(ServerPlayer player, ParkourData data, Vec3 velocity, String animation) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();
        data.readyTick = now(player) + ServerConfig.PARKOUR_IMPULSE_COOLDOWN_TICKS.get();
        effects(player, animation, velocity);
    }

    private static void effects(ServerPlayer player, String animation, Vec3 direction) {
        AnimationBridge.playPlayer(player, animation(animation));
        player.level().playSound(null, player.blockPosition(), KN8Sounds.DASH.get(), SoundSource.PLAYERS, 0.6F,
                1.2F);
        VfxService.play((ServerLevel) player.level(), VfxService.DUST, player.position(),
                new Vec3(direction.x, 0.0, direction.z), 0.3F, 0.0F);
        PowerService.addBodyXp(player, BodyStat.AGILITY, ServerConfig.BODY_XP_AGILITY_PER_ACTION.get());
    }

    /** Correndo no chao: salto longo para a frente. */
    static boolean longJump(ServerPlayer player, ParkourData data) {
        if (!player.onGround() || !recentlySprinting(player, data) || !ready(player, data)
                || !pay(player, ServerConfig.PARKOUR_LONG_JUMP_COST.get())) {
            return false;
        }
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        Vec3 velocity = forward.scale(ServerConfig.PARKOUR_LONG_JUMP_SPEED.get())
                .add(0.0, ServerConfig.PARKOUR_LONG_JUMP_LIFT.get(), 0.0);
        impulse(player, data, velocity, "long_jump");
        return true;
    }

    /**
     * Obstaculo a frente: ate {@code vaultMaxHeight} passa por cima (vault, so correndo); ate {@code climbMaxHeight}
     * sobe (escalada curta). Quem decide entre os dois e a altura medida aqui, nao o pedido do cliente.
     */
    static boolean vaultOrClimb(ServerPlayer player, ParkourData data) {
        if (!player.onGround() || !ready(player, data)) {
            return false;
        }
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        double height = ParkourProbe.obstacleHeight(player, forward, ServerConfig.PARKOUR_PROBE_REACH.get(),
                ServerConfig.PARKOUR_CLIMB_MAX_HEIGHT.get());
        ParkourMath.Obstacle obstacle = ParkourMath.classify(height, player.maxUpStep(),
                ServerConfig.PARKOUR_VAULT_MAX_HEIGHT.get(), ServerConfig.PARKOUR_CLIMB_MAX_HEIGHT.get());
        boolean vault = obstacle == ParkourMath.Obstacle.VAULT && recentlySprinting(player, data);
        if (obstacle == ParkourMath.Obstacle.NONE || !pay(player, vault ? ServerConfig.PARKOUR_VAULT_COST.get()
                : ServerConfig.PARKOUR_CLIMB_COST.get())) {
            return false;
        }
        double up = ParkourMath.launchSpeedFor(height, ServerConfig.PARKOUR_LAUNCH_MARGIN.get());
        double ahead = vault ? ServerConfig.PARKOUR_VAULT_FORWARD.get() : ServerConfig.PARKOUR_CLIMB_FORWARD.get();
        impulse(player, data, forward.scale(ahead).add(0.0, up, 0.0), vault ? "vault" : "climb");
        return true;
    }

    /** No ar, correndo, com parede ao lado: corre pela parede. */
    static boolean wallRun(ServerPlayer player, ParkourData data, int sideHint) {
        if (player.onGround() || data.state != ParkourState.NONE || !recentlySprinting(player, data)
                || data.wallJumpsInAir >= ServerConfig.PARKOUR_MAX_WALL_JUMPS.get()) {
            return false;
        }
        double reach = ServerConfig.PARKOUR_PROBE_REACH.get();
        int side = ParkourProbe.wallSide(player, reach, sideHint);
        if (side == 0) {
            return false;
        }
        Vec3 normal = ParkourProbe.axisAligned(ParkourProbe.right(player.getYRot()).scale(-side));
        Vec3 along = ParkourProbe.alongWall(ParkourProbe.forward(player.getYRot()), normal);
        // Sem chao no meio, a mesma parede nao conta de novo (segurar C nao vira corrida infinita); outra vale.
        if (along == Vec3.ZERO || normal.x * data.lastRunNormalX + normal.z * data.lastRunNormalZ > 0.9
                || !pay(player, ServerConfig.PARKOUR_WALL_RUN_COST_PER_SECOND.get() / 4.0)) {
            return false;
        }
        data.lastRunNormalX = normal.x;
        data.lastRunNormalZ = normal.z;
        data.wallJumpsInAir++;
        start(player, data, ParkourState.WALL_RUN, along, normal, ServerConfig.PARKOUR_WALL_RUN_SPEED.get(),
                ServerConfig.PARKOUR_WALL_RUN_LIFT.get(), 0.0, ServerConfig.PARKOUR_WALL_RUN_MAX_TICKS.get());
        data.side = side;
        effects(player, side > 0 ? "wall_run_right" : "wall_run_left", along);
        return true;
    }

    /** No ar, de frente para a parede: escorrega devagar (a queda nao conta). */
    static boolean wallSlide(ServerPlayer player, ParkourData data) {
        if (player.onGround() || data.state != ParkourState.NONE) {
            return false;
        }
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        if (!ParkourProbe.wallAt(player, forward, ServerConfig.PARKOUR_PROBE_REACH.get())) {
            return false;
        }
        start(player, data, ParkourState.WALL_SLIDE, Vec3.ZERO, ParkourProbe.axisAligned(forward.scale(-1.0)), 0.0,
                ServerConfig.PARKOUR_WALL_SLIDE_MAX_FALL.get(), 0.0, ServerConfig.PARKOUR_WALL_SLIDE_MAX_TICKS.get());
        player.resetFallDistance();
        AnimationBridge.playPlayer(player, animation("wall_slide"));
        return true;
    }

    /** No ar, com uma borda a frente na altura das maos: segura a borda. */
    static boolean ledgeGrab(ServerPlayer player, ParkourData data) {
        if (player.onGround() || data.state == ParkourState.LEDGE_HANG || data.state == ParkourState.SLIDE) {
            return false;
        }
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        double height = ParkourProbe.obstacleHeight(player, forward, ServerConfig.PARKOUR_PROBE_REACH.get(),
                ServerConfig.PARKOUR_LEDGE_MAX_HEIGHT.get());
        if (height < ServerConfig.PARKOUR_LEDGE_MIN_HEIGHT.get()) {
            return false;
        }
        if (data.state != ParkourState.NONE) {
            end(player, data, false);
        }
        double hangDrop = ServerConfig.PARKOUR_LEDGE_HANG_DROP.get();
        double hangY = player.getY() + height - hangDrop;
        start(player, data, ParkourState.LEDGE_HANG, Vec3.ZERO, ParkourProbe.axisAligned(forward.scale(-1.0)), 0.0,
                hangY, 0.0, ServerConfig.PARKOUR_LEDGE_MAX_TICKS.get());
        data.hangY = hangY;
        data.ledgeHeight = hangDrop;
        player.resetFallDistance();
        AnimationBridge.playPlayer(player, animation("ledge_hang"));
        return true;
    }

    /** Pulo na parede (ou logo depois de sair dela): empurra para longe da parede e para cima. */
    static boolean wallJump(ServerPlayer player, ParkourData data) {
        long now = now(player);
        boolean onWall = data.state == ParkourState.WALL_RUN || data.state == ParkourState.WALL_SLIDE;
        boolean coyote = (data.leftWallState == ParkourState.WALL_RUN || data.leftWallState == ParkourState.WALL_SLIDE)
                && now - data.leftWallTick <= ServerConfig.PARKOUR_WALL_JUMP_COYOTE_TICKS.get();
        if (!onWall && !coyote || data.wallJumpsInAir >= ServerConfig.PARKOUR_MAX_WALL_JUMPS.get()
                || !pay(player, ServerConfig.PARKOUR_WALL_JUMP_COST.get())) {
            return false;
        }
        Vec3 normal = new Vec3(data.normalX, 0.0, data.normalZ);
        Vec3 along = new Vec3(data.dirX, 0.0, data.dirZ);
        if (onWall) {
            end(player, data, false);
        }
        data.leftWallState = ParkourState.NONE;
        data.wallJumpsInAir++;
        Vec3 velocity = normal.scale(ServerConfig.PARKOUR_WALL_JUMP_PUSH.get())
                .add(along.scale(ServerConfig.PARKOUR_WALL_JUMP_CARRY.get()))
                .add(0.0, ServerConfig.PARKOUR_WALL_JUMP_LIFT.get(), 0.0);
        impulse(player, data, velocity, "wall_jump");
        return true;
    }

    /** Pendurado: sobe a borda. */
    static boolean ledgeClimb(ServerPlayer player, ParkourData data) {
        if (data.state != ParkourState.LEDGE_HANG || !pay(player, ServerConfig.PARKOUR_LEDGE_CLIMB_COST.get())) {
            return false;
        }
        Vec3 into = new Vec3(-data.normalX, 0.0, -data.normalZ);
        double height = data.hangY + data.ledgeHeight - player.getY();
        end(player, data, false);
        data.leftWallState = ParkourState.NONE;
        double up = ParkourMath.launchSpeedFor(Math.max(0.0, height), ServerConfig.PARKOUR_LAUNCH_MARGIN.get());
        impulse(player, data, into.scale(ServerConfig.PARKOUR_CLIMB_FORWARD.get()).add(0.0, up, 0.0), "ledge_climb");
        return true;
    }

    /** Correndo no chao, agachar: desliza (pose deitada, passa por vao de 1 bloco). */
    static boolean slide(ServerPlayer player, ParkourData data) {
        if (!player.onGround() || data.state != ParkourState.NONE || !recentlySprinting(player, data)
                || !ready(player, data) || !pay(player, ServerConfig.PARKOUR_SLIDE_COST.get())) {
            return false;
        }
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        start(player, data, ParkourState.SLIDE, forward, Vec3.ZERO, ServerConfig.PARKOUR_SLIDE_SPEED.get(), 0.0,
                ServerConfig.PARKOUR_SLIDE_FRICTION.get(), ServerConfig.PARKOUR_SLIDE_TICKS.get());
        player.setForcedPose(Pose.SWIMMING);
        effects(player, "slide", forward);
        return true;
    }

    /** Correndo agora ou ate {@code sprintGraceTicks} atras (agachar e pular cortam a corrida antes do pacote). */
    private static boolean recentlySprinting(ServerPlayer player, ParkourData data) {
        return player.isSprinting()
                || now(player) - data.lastSprintTick <= ServerConfig.PARKOUR_SPRINT_GRACE_TICKS.get();
    }

    // --- estados continuos --------------------------------------------------------------------------------------

    private static void start(ServerPlayer player, ParkourData data, ParkourState state, Vec3 dir, Vec3 normal,
            double speed, double vertical, double friction, int maxTicks) {
        long now = now(player);
        data.state = state;
        data.dirX = dir.x;
        data.dirZ = dir.z;
        data.normalX = normal.x;
        data.normalZ = normal.z;
        data.startTick = now;
        data.lastValidTick = now;
        PacketDistributor.sendToPlayer(player, new ParkourStateS2C(state.ordinal(), (float) dir.x, (float) dir.z,
                (float) normal.x, (float) normal.z, (float) speed, vertical, (float) friction, maxTicks));
    }

    /** Encerra o estado continuo; {@code stopAnimation} para a animacao em laco (sem impulso que a substitua). */
    static void end(ServerPlayer player, ParkourData data, boolean stopAnimation) {
        ParkourState old = data.state;
        if (old == ParkourState.NONE) {
            return;
        }
        data.state = ParkourState.NONE;
        if (old.onWall()) {
            data.leftWallState = old;
            data.leftWallTick = now(player);
        }
        if (old == ParkourState.SLIDE) {
            player.setForcedPose(null);
            data.readyTick = now(player) + ServerConfig.PARKOUR_IMPULSE_COOLDOWN_TICKS.get();
        }
        PacketDistributor.sendToPlayer(player, ParkourStateS2C.NONE);
        if (stopAnimation) {
            AnimationBridge.stopPlayer(player);
        }
    }

    /** Tick do servidor: corrida recente, contagem de wall-jumps e manutencao do estado continuo. */
    public static void tick(ServerPlayer player) {
        ParkourData data = data(player);
        long now = now(player);
        if (player.isSprinting()) {
            data.lastSprintTick = now;
        }
        // Chao de verdade: alguns ticks seguidos (no fim do arco do wall-run o pe encosta 1 tick no chao e a mesma
        // parede nao pode valer de novo por isso).
        data.groundTicks = player.onGround() ? data.groundTicks + 1 : 0;
        if (data.groundTicks >= ServerConfig.PARKOUR_LANDING_TICKS.get()) {
            data.wallJumpsInAir = 0;
            data.lastRunNormalX = 0.0;
            data.lastRunNormalZ = 0.0;
        }
        if (data.state == ParkourState.NONE) {
            return;
        }
        if (!canParkour(player)) {
            end(player, data, true);
            return;
        }
        double reach = ServerConfig.PARKOUR_PROBE_REACH.get();
        Vec3 toWall = new Vec3(-data.normalX, 0.0, -data.normalZ);
        boolean valid = switch (data.state) {
            case WALL_RUN, WALL_SLIDE -> !player.onGround() && ParkourProbe.wallAt(player, toWall, reach);
            case LEDGE_HANG -> ParkourProbe.wallAt(player, toWall, reach);
            case SLIDE -> player.onGround();
            case NONE -> false;
        };
        if (valid) {
            data.lastValidTick = now;
        }
        int maxTicks = switch (data.state) {
            case WALL_RUN -> ServerConfig.PARKOUR_WALL_RUN_MAX_TICKS.get();
            case WALL_SLIDE -> ServerConfig.PARKOUR_WALL_SLIDE_MAX_TICKS.get();
            case LEDGE_HANG -> ServerConfig.PARKOUR_LEDGE_MAX_TICKS.get();
            default -> ServerConfig.PARKOUR_SLIDE_TICKS.get();
        };
        // Folga de alguns ticks: a posicao do servidor chega atrasada em relacao ao cliente.
        if (now - data.lastValidTick > ServerConfig.PARKOUR_GRACE_TICKS.get() || now - data.startTick > maxTicks) {
            end(player, data, true);
            return;
        }
        double perSecond = switch (data.state) {
            case WALL_RUN -> ServerConfig.PARKOUR_WALL_RUN_COST_PER_SECOND.get();
            case WALL_SLIDE -> ServerConfig.PARKOUR_WALL_SLIDE_COST_PER_SECOND.get();
            case LEDGE_HANG -> ServerConfig.PARKOUR_LEDGE_COST_PER_SECOND.get();
            default -> 0.0;
        };
        if (perSecond > 0.0 && !pay(player, perSecond / 20.0)) {
            end(player, data, true);
            return;
        }
        if (data.state.onWall()) {
            player.resetFallDistance();
        }
    }

    /** Morte, troca de dimensao, saida: some o estado (o cliente recebe NONE se ainda estiver conectado). */
    public static void reset(ServerPlayer player) {
        ParkourData data = data(player);
        if (data.state == ParkourState.SLIDE) {
            player.setForcedPose(null);
        }
        data.state = ParkourState.NONE;
        data.wallJumpsInAir = 0;
        data.lastRunNormalX = 0.0;
        data.lastRunNormalZ = 0.0;
        PacketDistributor.sendToPlayer(player, ParkourStateS2C.NONE);
    }

    // --- rolamento ----------------------------------------------------------------------------------------------

    /**
     * Rolamento: Shift segurado ao cair de {@code rollMinFall} blocos ou mais. Ate {@code rollSafeFall} nao ha dano;
     * acima, o dano cai {@code rollDamageReduction}. Devolve o multiplicador do dano (1 = sem rolamento).
     */
    public static float onFall(ServerPlayer player, float distance) {
        if (!canParkour(player) || !player.isShiftKeyDown() || distance < ServerConfig.PARKOUR_ROLL_MIN_FALL.get()
                || !pay(player, ServerConfig.PARKOUR_ROLL_COST.get())) {
            return 1.0F;
        }
        Vec3 forward = ParkourProbe.forward(player.getYRot());
        Vec3 velocity = forward.scale(ServerConfig.PARKOUR_ROLL_FORWARD.get());
        player.setDeltaMovement(velocity.x, player.getDeltaMovement().y, velocity.z);
        player.hurtMarked = true;
        effects(player, "roll", forward);
        return ParkourMath.rollDamageMultiplier(distance, ServerConfig.PARKOUR_ROLL_SAFE_FALL.get().floatValue(),
                ServerConfig.PARKOUR_ROLL_DAMAGE_REDUCTION.get().floatValue());
    }
}
