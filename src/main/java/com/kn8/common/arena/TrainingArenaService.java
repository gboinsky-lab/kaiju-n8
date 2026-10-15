// src/main/java/com/kn8/common/arena/TrainingArenaService.java
package com.kn8.common.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.destruction.ProtectedAreas;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Arena de treino com kaiju de verdade (construcoes C2). O servidor decide tudo: o painel
 * ({@link TrainingConsoleBlock}) pede, este servico solta o kaiju no centro da arena, protege a arena da destruicao
 * ({@link ProtectedAreas}, o kaiju nao quebra as paredes), segura o kaiju dentro dela e o tira se ninguem estiver
 * treinando. Kaiju de treino (marca {@link #TAG} nos dados persistentes) nao vira carcaca nem da merito.
 */
public final class TrainingArenaService {

    /** Marca do kaiju de treino: {console: long, box: int[6]}. Salva com a entidade. */
    public static final String TAG = "kn8_training";
    private static final String CONSOLE = "console";
    private static final String BOX = "box";
    private static final String IDLE = "idle";

    /** Geometria da arena em relacao ao painel (a frente aponta para o centro). */
    public record Geometry(int distance, int halfWidth, int halfLength, int height, int margin) {

        public static Geometry fromConfig() {
            return new Geometry(ServerConfig.ARENA_DISTANCE.get(), ServerConfig.ARENA_HALF_WIDTH.get(),
                    ServerConfig.ARENA_HALF_LENGTH.get(), ServerConfig.ARENA_HEIGHT.get(),
                    ServerConfig.ARENA_PROTECT_MARGIN.get());
        }

        public BlockPos center(BlockPos console, Direction facing) {
            return console.relative(facing, distance);
        }

        /** Area de luta: {@code halfLength} ao longo da frente do painel, {@code halfWidth} de lado. */
        public BoundingBox box(BlockPos console, Direction facing) {
            BlockPos c = center(console, facing);
            boolean alongX = facing.getAxis() == Direction.Axis.X;
            int hx = alongX ? halfLength : halfWidth;
            int hz = alongX ? halfWidth : halfLength;
            return new BoundingBox(c.getX() - hx, c.getY() - 2, c.getZ() - hz, c.getX() + hx, c.getY() + height,
                    c.getZ() + hz);
        }
    }

    private TrainingArenaService() {
    }

    /** Especies da lista do config que existem (id invalido e ignorado com aviso no log pelo spawner). */
    public static List<ResourceLocation> roster() {
        List<ResourceLocation> out = new ArrayList<>();
        for (String id : ServerConfig.ARENA_KAIJU.get()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && KaijuSpawner.type(rl).isPresent()) {
                out.add(rl);
            }
        }
        return out;
    }

    private static Optional<ResourceLocation> selected(BlockState state) {
        List<ResourceLocation> roster = roster();
        if (roster.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(roster.get(state.getValue(TrainingConsoleBlock.SELECTION) % roster.size()));
    }

    private static Component kaijuName(ResourceLocation id) {
        return Component.translatable("entity." + id.getNamespace() + "." + id.getPath());
    }

    /** Shift + usar: proximo kaiju da lista. */
    public static void cycle(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        List<ResourceLocation> roster = roster();
        if (roster.isEmpty()) {
            player.displayClientMessage(Component.translatable("kn8.arena.empty"), true);
            return;
        }
        int next = (state.getValue(TrainingConsoleBlock.SELECTION) + 1) % Math.min(roster.size(), 16);
        BlockState updated = state.setValue(TrainingConsoleBlock.SELECTION, next);
        level.setBlock(pos, updated, 3);
        player.displayClientMessage(Component.translatable("kn8.arena.selected", kaijuName(roster.get(next))), true);
    }

    /** Usar: encerra o treino em andamento deste painel ou comeca um novo com o kaiju escolhido. */
    public static void use(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        Direction facing = state.getValue(TrainingConsoleBlock.FACING);
        Geometry geometry = Geometry.fromConfig();
        List<KaijuEntity> running = active(level, pos, facing, geometry);
        if (!running.isEmpty()) {
            running.forEach(kaiju -> vanish(level, kaiju));
            player.displayClientMessage(Component.translatable("kn8.arena.ended"), true);
            return;
        }
        Optional<ResourceLocation> species = selected(state);
        if (species.isEmpty()) {
            player.displayClientMessage(Component.translatable("kn8.arena.empty"), true);
            return;
        }
        start(level, pos, facing, species.get(), geometry).ifPresentOrElse(
                kaiju -> player.displayClientMessage(Component.translatable("kn8.arena.started",
                        kaijuName(species.get())), true),
                () -> player.displayClientMessage(Component.translatable("kn8.arena.failed"), true));
    }

    /** Solta o kaiju no centro e protege a arena (paredes inclusas, com {@code margin}). */
    public static Optional<KaijuEntity> start(ServerLevel level, BlockPos console, Direction facing,
            ResourceLocation species, Geometry geometry) {
        BoundingBox box = geometry.box(console, facing);
        int m = geometry.margin();
        ProtectedAreas.get(level).add(areaName(console), new BlockPos(box.minX() - m, box.minY() - m, box.minZ() - m),
                new BlockPos(box.maxX() + m, box.maxY() + m, box.maxZ() + m));
        Optional<KaijuEntity> spawned = KaijuSpawner.spawn(level, species, geometry.center(console, facing));
        spawned.ifPresent(kaiju -> {
            CompoundTag tag = new CompoundTag();
            tag.putLong(CONSOLE, console.asLong());
            tag.putIntArray(BOX, new int[] {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            kaiju.getPersistentData().put(TAG, tag);
        });
        return spawned;
    }

    public static String areaName(BlockPos console) {
        return "arena_" + console.getX() + "_" + console.getY() + "_" + console.getZ();
    }

    /** Kaiju de treino deste painel ainda vivos. */
    public static List<KaijuEntity> active(ServerLevel level, BlockPos console, Direction facing, Geometry geometry) {
        AABB area = AABB.of(geometry.box(console, facing)).inflate(32.0);
        long key = console.asLong();
        return level.getEntitiesOfClass(KaijuEntity.class, area,
                kaiju -> kaiju.isAlive() && isTraining(kaiju)
                        && kaiju.getPersistentData().getCompound(TAG).getLong(CONSOLE) == key);
    }

    public static boolean isTraining(Entity entity) {
        return entity.getPersistentData().contains(TAG);
    }

    /** Some com fumaca (fim do treino, ninguem treinando): nada de carcaca. */
    public static void vanish(ServerLevel level, KaijuEntity kaiju) {
        level.sendParticles(ParticleTypes.CLOUD, kaiju.getX(), kaiju.getY() + kaiju.getBbHeight() / 2.0, kaiju.getZ(),
                30, kaiju.getBbWidth() / 2.0, kaiju.getBbHeight() / 2.0, kaiju.getBbWidth() / 2.0, 0.02);
        kaiju.discard();
    }

    /**
     * A cada {@code leashCheckTicks}: kaiju fora da arena volta ao centro; sem jogador na arena (com folga
     * {@code idleRange}) por {@code idleDespawnTicks}, o treino acaba sozinho.
     */
    public static void tick(KaijuEntity kaiju) {
        if (!(kaiju.level() instanceof ServerLevel level) || !isTraining(kaiju)
                || kaiju.tickCount % ServerConfig.ARENA_LEASH_CHECK_TICKS.get() != 0) {
            return;
        }
        CompoundTag tag = kaiju.getPersistentData().getCompound(TAG);
        int[] b = tag.getIntArray(BOX);
        if (b.length != 6) {
            return;
        }
        BoundingBox box = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        if (!box.isInside(kaiju.blockPosition())) {
            leash(kaiju, box);
        }
        AABB watch = AABB.of(box).inflate(ServerConfig.ARENA_IDLE_RANGE.get());
        boolean watched = !level.getEntitiesOfClass(Player.class, watch, p -> p.isAlive() && !p.isSpectator())
                .isEmpty();
        int idle = watched ? 0 : tag.getInt(IDLE) + ServerConfig.ARENA_LEASH_CHECK_TICKS.get();
        tag.putInt(IDLE, idle);
        if (idle >= ServerConfig.ARENA_IDLE_DESPAWN_TICKS.get()) {
            vanish(level, kaiju);
        }
    }

    /** Volta ao centro da arena (salto do kaiju, empurrao, fuga do No. 9). */
    public static void leash(KaijuEntity kaiju, BoundingBox box) {
        double x = (box.minX() + box.maxX()) / 2.0 + 0.5;
        double z = (box.minZ() + box.maxZ()) / 2.0 + 0.5;
        kaiju.teleportTo(x, box.minY() + 2.0, z);
        kaiju.setDeltaMovement(0.0, 0.0, 0.0);
        KaijuSpawner.placeOnFreeGround((ServerLevel) kaiju.level(), kaiju);
    }

    /** Caixa gravada no kaiju de treino (vazio se nao for de treino). */
    public static Optional<BoundingBox> boxOf(KaijuEntity kaiju) {
        int[] b = kaiju.getPersistentData().getCompound(TAG).getIntArray(BOX);
        return b.length == 6 ? Optional.of(new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5])) : Optional.empty();
    }
}
