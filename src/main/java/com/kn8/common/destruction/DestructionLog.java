// src/main/java/com/kn8/common/destruction/DestructionLog.java
package com.kn8.common.destruction;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Registro do que a destruicao quebrou (posicao + estado do bloco), por dimensao, salvo no mundo: base da
 * restauracao ({@code /kn8 destruction restore}, gradual pelo {@code destruction.rebuildBlocksPerTick}). Tamanho
 * limitado ({@code destruction.logLimit}): os registros mais antigos saem primeiro.
 */
public final class DestructionLog extends SavedData {

    private static final String DATA_NAME = "kn8_destruction_log";
    private static final String TAG_ENTRIES = "entries";
    private static final String TAG_POS = "pos";
    private static final String TAG_STATE = "state";

    /** Uma quebra registrada. */
    public record Entry(BlockPos pos, BlockState state) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    public static DestructionLog get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(DestructionLog::new, DestructionLog::load, null),
                DATA_NAME);
    }

    public void record(BlockPos pos, BlockState state, int limit) {
        entries.addLast(new Entry(pos.immutable(), state));
        while (entries.size() > limit) {
            entries.removeFirst();
        }
        setDirty();
    }

    /** Tira o registro mais recente (restauracao: desfaz do mais novo para o mais antigo). */
    public Entry pollNewest() {
        Entry entry = entries.pollLast();
        if (entry != null) {
            setDirty();
        }
        return entry;
    }

    public int size() {
        return entries.size();
    }

    private static DestructionLog load(CompoundTag tag, HolderLookup.Provider registries) {
        DestructionLog log = new DestructionLog();
        ListTag list = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),
                    item.getCompound(TAG_STATE));
            log.entries.addLast(new Entry(BlockPos.of(item.getLong(TAG_POS)), state));
        }
        return log;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, entry.pos().asLong());
            item.put(TAG_STATE, NbtUtils.writeBlockState(entry.state()));
            list.add(item);
        }
        tag.put(TAG_ENTRIES, list);
        return tag;
    }
}
