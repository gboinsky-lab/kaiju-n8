package com.kn8.common.trail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Rastro de destruicao (PM-9, Plano Mestre secao 16), por dimensao, salvo no mundo: so pontos de interesse (onde um
 * kaiju quebrou o caminho ou caiu), nunca copia de blocos; nenhum bloco do mundo e mexido para criar o rastro.
 * Limite de pontos e prazo no config; os mais velhos saem primeiro.
 */
public final class KaijuTrail extends SavedData {

    private static final String DATA_NAME = "kn8_kaiju_trail";

    public enum Kind {
        DESTRUCTION, DEATH
    }

    /** Uma pista: posicao, especie, kaiju (vazio = inconclusivo), tick, tipo e se ja foi investigada. */
    public static final class Point {
        final long id;
        final BlockPos pos;
        final ResourceLocation species;
        @Nullable
        final UUID kaiju;
        final long tick;
        final Kind kind;
        boolean investigated;

        Point(long id, BlockPos pos, ResourceLocation species, @Nullable UUID kaiju, long tick, Kind kind,
                boolean investigated) {
            this.id = id;
            this.pos = pos.immutable();
            this.species = species;
            this.kaiju = kaiju;
            this.tick = tick;
            this.kind = kind;
            this.investigated = investigated;
        }

        public BlockPos pos() {
            return pos;
        }

        public ResourceLocation species() {
            return species;
        }

        @Nullable
        public UUID kaiju() {
            return kaiju;
        }

        public long tick() {
            return tick;
        }

        public Kind kind() {
            return kind;
        }

        public boolean investigated() {
            return investigated;
        }
    }

    private final List<Point> points = new ArrayList<>();
    private long nextId;

    public static KaijuTrail get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(KaijuTrail::new, KaijuTrail::load, null),
                DATA_NAME);
    }

    public List<Point> points() {
        return points;
    }

    Point add(BlockPos pos, ResourceLocation species, @Nullable UUID kaiju, long tick, Kind kind, int max) {
        Point point = new Point(nextId++, pos, species, kaiju, tick, kind, false);
        points.add(point);
        while (points.size() > Math.max(1, max)) {
            points.remove(0);
        }
        setDirty();
        return point;
    }

    /** Tira as pistas vencidas; devolve quantas sairam. */
    public int expire(long now, long lifeTicks) {
        int before = points.size();
        points.removeIf(point -> now - point.tick >= lifeTicks);
        if (points.size() != before) {
            setDirty();
        }
        return before - points.size();
    }

    public static KaijuTrail load(CompoundTag tag, HolderLookup.Provider registries) {
        KaijuTrail data = new KaijuTrail();
        data.nextId = tag.getLong("next");
        for (Tag raw : tag.getList("points", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation species = ResourceLocation.tryParse(entry.getString("species"));
            int[] p = entry.getIntArray("pos");
            if (species == null || p.length != 3) {
                continue;
            }
            Kind kind = entry.getInt("kind") == 1 ? Kind.DEATH : Kind.DESTRUCTION;
            data.points.add(new Point(entry.getLong("id"), new BlockPos(p[0], p[1], p[2]), species,
                    entry.hasUUID("kaiju") ? entry.getUUID("kaiju") : null, entry.getLong("tick"), kind,
                    entry.getBoolean("investigated")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("next", nextId);
        ListTag list = new ListTag();
        for (Point point : points) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("id", point.id);
            entry.putIntArray("pos", new int[] {point.pos.getX(), point.pos.getY(), point.pos.getZ()});
            entry.putString("species", point.species.toString());
            if (point.kaiju != null) {
                entry.putUUID("kaiju", point.kaiju);
            }
            entry.putLong("tick", point.tick);
            entry.putInt("kind", point.kind.ordinal());
            entry.putBoolean("investigated", point.investigated);
            list.add(entry);
        }
        tag.put("points", list);
        return tag;
    }
}
