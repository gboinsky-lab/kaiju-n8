// src/main/java/com/kn8/common/site/SiteData.java
package com.kn8.common.site;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Locais cuja guarnicao ja apareceu (uma vez por mundo: os personagens da obra sao unicos), salvos no Overworld. */
public final class SiteData extends SavedData {

    private static final String DATA_NAME = "kn8_sites";
    private final Set<String> garrisoned = new LinkedHashSet<>();

    public static SiteData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SiteData::new, SiteData::load, null), DATA_NAME);
    }

    public boolean garrisoned(String site) {
        return garrisoned.contains(site);
    }

    public void markGarrisoned(String site) {
        if (garrisoned.add(site)) {
            setDirty();
        }
    }

    public void reset(String site) {
        if (garrisoned.remove(site)) {
            setDirty();
        }
    }

    private static SiteData load(CompoundTag tag, HolderLookup.Provider registries) {
        SiteData data = new SiteData();
        for (Tag entry : tag.getList("garrisoned", Tag.TAG_STRING)) {
            data.garrisoned.add(entry.getAsString());
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        garrisoned.forEach(site -> list.add(StringTag.valueOf(site)));
        tag.put("garrisoned", list);
        return tag;
    }
}
