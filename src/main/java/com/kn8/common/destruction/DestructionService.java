// src/main/java/com/kn8/common/destruction/DestructionService.java
package com.kn8.common.destruction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.server.KN8Server;
import com.kn8.core.destruction.DestructionMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Destruicao controlada do ambiente (Etapa E). Quem ataca so informa centro, raio, forca e se abre cratera; este
 * servico decide o que quebra:
 * <ul>
 *   <li>liga/desliga: {@code destruction.enabled}; ataques de mob respeitam a gamerule {@code mobGriefing};</li>
 *   <li>areas protegidas ({@link ProtectedAreas}) nunca quebram;</li>
 *   <li>categoria do bloco: tags {@code kn8:destruction/<categoria>} mandam; sem tag, a dureza; blocos com block
 *   entity (baus, fornalhas...) sao indestrutiveis;</li>
 *   <li>forca do ataque (1-4) quebra as categorias abaixo dela ({@link DestructionMath#canBreak});</li>
 *   <li>fila por dimensao com orcamento {@code destruction.blocksPerTick}: um ataque grande se espalha por alguns
 *   ticks, sem travar o servidor; quebra sem drop por padrao ({@code destruction.dropItems}).</li>
 * </ul>
 * Toda quebra vai para o {@link DestructionLog} (restauracao). As particulas de quebra do proprio Minecraft fazem os
 * destrocos; poeira e onda de choque sao do VfxService.
 */
public final class DestructionService {

    public static final TagKey<Block> FRAGILE = tag("fragile");
    public static final TagKey<Block> NORMAL = tag("normal");
    public static final TagKey<Block> RESISTANT = tag("resistant");
    public static final TagKey<Block> VERY_RESISTANT = tag("very_resistant");
    public static final TagKey<Block> INDESTRUCTIBLE = tag("indestructible");

    /** Um pedido na fila: posicoes ainda por quebrar, com a forca do ataque. */
    public record Job(Deque<BlockPos> positions, int power, boolean fromMob) {
    }

    private DestructionService() {
    }

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, KN8Constants.id("destruction/" + name));
    }

    // --- pedidos --------------------------------------------------------------------------------------------

    /**
     * Impacto de area (slam, explosao): esfera acima do chao com borda irregular e, se {@code crater}, cratera abaixo
     * do centro. As posicoes mais perto do centro quebram primeiro.
     */
    public static void request(ServerLevel level, Vec3 center, AbilityDef.Destruction destruction, Entity source) {
        if (!allowed(level, source)) {
            return;
        }
        double radius = destruction.radius();
        int r = Mth.ceil(radius * 1.25);
        long seed = level.random.nextLong();
        BlockPos origin = BlockPos.containing(center);
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double depth = destruction.crater()
                        ? DestructionMath.craterDepth(dx, dz, radius, destruction.depth(), seed) : 0;
                double edge = DestructionMath.craterDepth(dx, dz, radius, radius, seed);
                for (int dy = -Mth.ceil(depth); dy <= r; dy++) {
                    boolean above = dy >= 0 && dy <= edge;
                    boolean crater = dy < 0 && -dy <= depth;
                    if (above || crater) {
                        positions.add(origin.offset(dx, dy, dz));
                    }
                }
            }
        }
        positions.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(center)));
        enqueue(level, positions, destruction.power(), source instanceof Mob);
    }

    /** Faixa logo a frente do corpo (investida, kaiju abrindo passagem), da altura do degrau ate o topo. */
    public static void requestFront(Mob mob, int power, double depth) {
        ServerLevel level = (ServerLevel) mob.level();
        if (!allowed(level, mob)) {
            return;
        }
        float yaw = mob.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(depth);
        AABB box = mob.getBoundingBox().move(forward).inflate(0.1, 0, 0.1)
                .setMinY(mob.getY() + mob.maxUpStep() * 0.5);
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.betweenClosedStream(box).forEach(pos -> positions.add(pos.immutable()));
        enqueue(level, positions, power, true);
    }

    /** Kaiju grande travado: abre passagem com a forca da categoria dele, com intervalo minimo entre pedidos. */
    public static void requestPathClear(KaijuEntity kaiju) {
        long now = kaiju.level().getGameTime();
        if (now < kaiju.nextPathClearTick()) {
            return;
        }
        kaiju.setNextPathClearTick(now + ServerConfig.PATH_CLEAR_COOLDOWN_TICKS.get());
        KaijuClass kaijuClass = kaiju.def().map(def -> def.kaijuClass()).orElse(KaijuClass.YOJU);
        int power = switch (kaijuClass) {
            case YOJU -> ServerConfig.PATH_POWER_YOJU.get();
            case HONJU, NUMBERED -> ServerConfig.PATH_POWER_HONJU.get();
            case DAIKAIJU -> ServerConfig.PATH_POWER_DAIKAIJU.get();
        };
        requestFront(kaiju, power, 1.0);
    }

    private static boolean allowed(ServerLevel level, Entity source) {
        if (!ServerConfig.SPEC.isLoaded() || !ServerConfig.DESTRUCTION_ENABLED.get()) {
            return false;
        }
        return !(source instanceof Mob) || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private static void enqueue(ServerLevel level, List<BlockPos> positions, int power, boolean fromMob) {
        KN8Server services = KN8Server.getOrNull(level.getServer());
        if (services != null && !positions.isEmpty()) {
            services.destructionJobs(level.dimension()).addLast(new Job(new ArrayDeque<>(positions), power, fromMob));
        }
    }

    // --- tick ----------------------------------------------------------------------------------------------

    /** Um tick da dimensao: quebra ate {@code destruction.blocksPerTick} blocos e restaura ate rebuildBlocksPerTick. */
    public static void tick(ServerLevel level) {
        KN8Server services = KN8Server.getOrNull(level.getServer());
        if (services == null || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        Deque<Job> jobs = services.destructionJobs(level.dimension());
        int budget = ServerConfig.DESTRUCTION_BLOCKS_PER_TICK.get();
        ProtectedAreas protectedAreas = ProtectedAreas.get(level);
        DestructionLog log = DestructionLog.get(level);
        while (budget > 0 && !jobs.isEmpty()) {
            Job job = jobs.peekFirst();
            BlockPos pos = job.positions().pollFirst();
            if (pos == null) {
                jobs.pollFirst();
                continue;
            }
            if (breakIfAllowed(level, pos, job.power(), protectedAreas, log)) {
                budget--;
            }
        }
        tickRestore(level, services, log);
    }

    private static boolean breakIfAllowed(ServerLevel level, BlockPos pos, int power, ProtectedAreas protectedAreas,
            DestructionLog log) {
        if (!level.isLoaded(pos) || protectedAreas.isProtected(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        // Ar e liquidos nao "quebram"; o resto depende da categoria do bloco e da forca do ataque.
        if (state.isAir() || !state.getFluidState().isEmpty()
                || !DestructionMath.canBreak(power, tierOf(level, pos, state))) {
            return false;
        }
        log.record(pos, state, ServerConfig.DESTRUCTION_LOG_LIMIT.get());
        level.destroyBlock(pos, ServerConfig.DESTRUCTION_DROP_ITEMS.get());
        return true;
    }

    /** Categoria do bloco: tag do kn8 > block entity (indestrutivel) > dureza. */
    public static int tierOf(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(INDESTRUCTIBLE) || state.hasBlockEntity()) {
            return DestructionMath.INDESTRUCTIBLE;
        }
        if (state.is(FRAGILE)) {
            return DestructionMath.FRAGILE;
        }
        if (state.is(NORMAL)) {
            return DestructionMath.NORMAL;
        }
        if (state.is(RESISTANT)) {
            return DestructionMath.RESISTANT;
        }
        if (state.is(VERY_RESISTANT)) {
            return DestructionMath.VERY_RESISTANT;
        }
        return DestructionMath.tierFromHardness(state.getDestroySpeed(level, pos));
    }

    // --- restauracao -------------------------------------------------------------------------------------------

    /** Comeca a restaurar tudo o que esta no registro da dimensao (do mais novo ao mais antigo). */
    public static int startRestore(ServerLevel level) {
        KN8Server services = KN8Server.getOrNull(level.getServer());
        if (services == null) {
            return 0;
        }
        services.setRestoring(level.dimension(), true);
        return DestructionLog.get(level).size();
    }

    private static void tickRestore(ServerLevel level, KN8Server services, DestructionLog log) {
        if (!services.isRestoring(level.dimension())) {
            return;
        }
        for (int i = 0; i < ServerConfig.REBUILD_BLOCKS_PER_TICK.get(); i++) {
            DestructionLog.Entry entry = log.pollNewest();
            if (entry == null) {
                services.setRestoring(level.dimension(), false);
                return;
            }
            if (level.isLoaded(entry.pos()) && level.getBlockState(entry.pos()).isAir()) {
                level.setBlockAndUpdate(entry.pos(), entry.state());
            }
        }
    }
}
