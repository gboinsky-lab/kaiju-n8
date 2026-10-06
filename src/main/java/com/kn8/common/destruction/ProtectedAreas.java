// src/main/java/com/kn8/common/destruction/ProtectedAreas.java
package com.kn8.common.destruction;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Areas protegidas da destruicao (Etapa E), por dimensao, salvas no mundo. Dentro de uma area protegida nenhum
 * ataque quebra blocos (cidades, arenas). Gerenciadas por {@code /kn8 destruction protect|unprotect|list}.
 */
public final class ProtectedAreas extends SavedData {

    private static final String DATA_NAME = "kn8_protected_areas";
    private final Map<String, BoundingBox> areas = new LinkedHashMap<>();

    public static ProtectedAreas get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ProtectedAreas::new, ProtectedAreas::load, null),
                DATA_NAME);
    }

    public boolean isProtected(BlockPos pos) {
        for (BoundingBox box : areas.values()) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    public void add(String name, BlockPos from, BlockPos to) {
        areas.put(name, BoundingBox.fromCorners(from, to));
        setDirty();
    }

    public boolean remove(String name) {
        boolean removed = areas.remove(name) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Map<String, BoundingBox> areas() {
        return Map.copyOf(areas);
    }

    private static ProtectedAreas load(CompoundTag tag, HolderLookup.Provider registries) {
        ProtectedAreas data = new ProtectedAreas();
        for (String name : tag.getAllKeys()) {
            int[] c = tag.getIntArray(name);
            if (c.length == 6) {
                data.areas.put(name, new BoundingBox(c[0], c[1], c[2], c[3], c[4], c[5]));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        areas.forEach((name, box) -> tag.putIntArray(name, new int[] {box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ()}));
        return tag;
    }
}
