// src/main/java/com/kn8/common/destruction/DestructionService.java
package com.kn8.common.destruction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.server.KN8Server;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.destruction.DestructionMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
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
    /** Chao natural (terra, pedra do mundo, areia...): o kaiju nao cava nele para descer (so construcoes). */
    public static final TagKey<Block> NATURAL_GROUND = tag("natural_ground");

    /**
     * Um pedido na fila: posicoes ainda por quebrar, com a forca do ataque. {@code blockEntities}: tambem quebra blocos
     * com dados (baus, pulpitos, camas), soltando o conteudo; so a descida do kaiju de cima de construcoes usa.
     */
    public record Job(Deque<BlockPos> positions, int power, boolean fromMob, boolean blockEntities) {
    }

    /** Lado (em potencia de 2) da celula que junta os pedacos desenhados de um tick: 1 << 3 = 8 blocos. */
    private static final int DEBRIS_CELL = 3;

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

    /**
     * Miguel (2026-10-16: "os kaijus nao podem ficar presos em meio as construcoes, eles devem destruir elas"): a
     * camada logo abaixo dos pes do kaiju, dentro da largura dele, se for construcao (nao o chao natural). Uma camada
     * por pedido; o kaiju cai e o proximo pedido pega a de baixo.
     */
    public static void requestBelow(KaijuEntity kaiju, int power) {
        ServerLevel level = (ServerLevel) kaiju.level();
        if (!allowed(level, kaiju) || power <= 0) {
            return;
        }
        AABB feet = kaiju.getBoundingBox();
        AABB layer = new AABB(feet.minX + 0.05, feet.minY - 1.0, feet.minZ + 0.05, feet.maxX - 0.05, feet.minY - 0.05,
                feet.maxZ - 0.05);
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.betweenClosedStream(layer).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(NATURAL_GROUND)) {
                positions.add(pos.immutable());
            }
        });
        // Um unico bloco com dados (pulpito, bau) embaixo segurava o corpo inteiro: a descida quebra esses tambem,
        // soltando o conteudo no chao.
        enqueue(level, positions, power, true, true);
    }

    /** O bloco embaixo do centro do kaiju e construcao (nem ar nem chao natural)? */
    public static boolean standsOnBuilt(KaijuEntity kaiju) {
        BlockPos below = BlockPos.containing(kaiju.getX(), kaiju.getBoundingBox().minY - 0.5, kaiju.getZ());
        BlockState state = kaiju.level().getBlockState(below);
        return !state.isAir() && !state.is(NATURAL_GROUND);
    }

    /**
     * Quantos blocos o pe do kaiju esta acima do chao natural, descendo pela coluna do centro (para no primeiro bloco
     * de {@link #NATURAL_GROUND}); conta no maximo {@code limit} (o que basta para decidir).
     */
    public static int heightAboveNaturalGround(KaijuEntity kaiju, int limit) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(kaiju.getX(), kaiju.getBoundingBox().minY - 0.5,
                kaiju.getZ()).mutable();
        for (int depth = 0; depth < limit; depth++) {
            if (pos.getY() <= kaiju.level().getMinBuildHeight()) {
                return depth;
            }
            if (kaiju.level().getBlockState(pos).is(NATURAL_GROUND)) {
                return depth;
            }
            pos.move(0, -1, 0);
        }
        return limit;
    }

    /** O mundo deixa este kaiju quebrar blocos agora (destruicao ligada e mobGriefing)? */
    public static boolean canBreak(KaijuEntity kaiju) {
        return kaiju.level() instanceof ServerLevel level && allowed(level, kaiju);
    }

    /**
     * Kaiju batendo em blocos enquanto anda (0.6: qualquer tamanho, passeando ou perseguindo) abre passagem com a
     * forca de andar da categoria dele ({@code destruction.walkPower*}); preso dentro de blocos
     * ({@code inside}), quebra o que ocupa o proprio corpo. Intervalo minimo entre pedidos.
     */
    public static void requestPathClear(KaijuEntity kaiju, boolean inside) {
        long now = kaiju.level().getGameTime();
        if (now < kaiju.nextPathClearTick()) {
            return;
        }
        kaiju.setNextPathClearTick(now + ServerConfig.PATH_CLEAR_COOLDOWN_TICKS.get());
        int power = walkPower(kaiju);
        if (power > 0) {
            requestFront(kaiju, power, inside ? 0.0 : 1.0);
        }
    }

    /** Forca de quebra da categoria do kaiju ({@code destruction.walkPower*}); serve tambem aos ataques (IA-2). */
    public static int walkPower(KaijuEntity kaiju) {
        KaijuClass kaijuClass = kaiju.def().map(def -> def.kaijuClass()).orElse(KaijuClass.YOJU);
        return switch (kaijuClass) {
            case YOJU -> ServerConfig.PATH_POWER_YOJU.get();
            case HONJU, NUMBERED -> ServerConfig.PATH_POWER_HONJU.get();
            case DAIKAIJU -> ServerConfig.PATH_POWER_DAIKAIJU.get();
        };
    }

    private static boolean allowed(ServerLevel level, Entity source) {
        if (!ServerConfig.SPEC.isLoaded() || !ServerConfig.DESTRUCTION_ENABLED.get()) {
            return false;
        }
        return !(source instanceof Mob) || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private static void enqueue(ServerLevel level, List<BlockPos> positions, int power, boolean fromMob) {
        enqueue(level, positions, power, fromMob, false);
    }

    private static void enqueue(ServerLevel level, List<BlockPos> positions, int power, boolean fromMob,
            boolean blockEntities) {
        KN8Server services = KN8Server.getOrNull(level.getServer());
        if (services != null && !positions.isEmpty()) {
            services.destructionJobs(level.dimension()).addLast(new Job(new ArrayDeque<>(positions), power, fromMob,
                    blockEntities));
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
        Map<Long, int[]> debris = new HashMap<>();
        while (budget > 0 && !jobs.isEmpty()) {
            Job job = jobs.peekFirst();
            BlockPos pos = job.positions().pollFirst();
            if (pos == null) {
                jobs.pollFirst();
                continue;
            }
            if (breakIfAllowed(level, pos, job.power(), job.blockEntities(), protectedAreas, log, debris)) {
                budget--;
            }
        }
        sendDebris(level, debris);
        tickRestore(level, services, log);
    }

    /**
     * Decisao do Miguel (2026-10-23: sem particulas, nem os pedacos de bloco): quebra como o {@code destroyBlock} do
     * vanilla (solta os itens, som de quebrar, evento de jogo), mas sem as particulas de bloco; os pedacos sao
     * desenhados no cliente ({@code kn8:debris}), um evento por celula de {@link #DEBRIS_CELL} blocos por tick.
     */
    private static void breakBlock(ServerLevel level, BlockPos pos, BlockState state, boolean drop,
            Map<Long, int[]> debris) {
        if (drop) {
            Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(null, state));
        SoundType sound = state.getSoundType(level, pos, null);
        level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        long cell = BlockPos.asLong(pos.getX() >> DEBRIS_CELL, pos.getY() >> DEBRIS_CELL, pos.getZ() >> DEBRIS_CELL);
        int[] entry = debris.computeIfAbsent(cell, key -> new int[] {0, 0, 0, 0, state.getMapColor(level, pos).col});
        entry[0]++;
        entry[1] += pos.getX();
        entry[2] += pos.getY();
        entry[3] += pos.getZ();
    }

    /** Um {@code kn8:debris} por celula: no meio dos blocos quebrados, com a cor do primeiro e a quantidade. */
    private static void sendDebris(ServerLevel level, Map<Long, int[]> debris) {
        for (int[] entry : debris.values()) {
            int n = entry[0];
            Vec3 at = new Vec3(entry[1] / (double) n + 0.5, entry[2] / (double) n + 0.5, entry[3] / (double) n + 0.5);
            VfxService.keyed(level, VfxService.DEBRIS, at, Vec3.ZERO, n, String.format("#%06X",
                    entry[4] & 0xFFFFFF), 0.0F);
        }
    }

    private static boolean breakIfAllowed(ServerLevel level, BlockPos pos, int power, boolean blockEntities,
            ProtectedAreas protectedAreas, DestructionLog log, Map<Long, int[]> debris) {
        if (!level.isLoaded(pos) || protectedAreas.isProtected(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        // Ar e liquidos nao "quebram"; o resto depende da categoria do bloco e da forca do ataque.
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (blockEntities && state.hasBlockEntity() && !state.is(INDESTRUCTIBLE)) {
            // Descida do kaiju: bloco com dados quebra pela dureza e sempre solta o que tinha (nada some); fica fora
            // do registro de restauracao (os dados nao voltariam).
            if (!DestructionMath.canBreak(power, DestructionMath.tierFromHardness(state.getDestroySpeed(level, pos)))) {
                return false;
            }
            breakBlock(level, pos, state, true, debris);
            return true;
        }
        if (!DestructionMath.canBreak(power, tierOf(level, pos, state))) {
            return false;
        }
        log.record(pos, state, ServerConfig.DESTRUCTION_LOG_LIMIT.get());
        breakBlock(level, pos, state, ServerConfig.DESTRUCTION_DROP_ITEMS.get(), debris);
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
