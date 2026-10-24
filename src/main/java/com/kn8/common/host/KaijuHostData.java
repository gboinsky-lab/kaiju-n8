package com.kn8.common.host;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * T-1: hospedeiro do Kaiju No. 8 do mundo (Miguel 2026-10-24: "apenas um jogador"). Fica no Overworld, vale para o
 * servidor inteiro: quem a larva infectou (para sempre) e a larva que esta voando agora (no maximo uma; some quando
 * infecta, morre ou vai embora).
 */
public final class KaijuHostData extends SavedData {

    private static final String DATA_NAME = "kn8_kaiju_host";

    @Nullable
    private UUID host;
    @Nullable
    private UUID larva;

    public static KaijuHostData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(KaijuHostData::new,
                KaijuHostData::load, null), DATA_NAME);
    }

    @Nullable
    public UUID host() {
        return host;
    }

    public void setHost(@Nullable UUID value) {
        host = value;
        setDirty();
    }

    @Nullable
    public UUID larva() {
        return larva;
    }

    public void setLarva(@Nullable UUID value) {
        larva = value;
        setDirty();
    }

    public static KaijuHostData load(CompoundTag tag, HolderLookup.Provider registries) {
        KaijuHostData data = new KaijuHostData();
        data.host = tag.hasUUID("host") ? tag.getUUID("host") : null;
        data.larva = tag.hasUUID("larva") ? tag.getUUID("larva") : null;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (host != null) {
            tag.putUUID("host", host);
        }
        if (larva != null) {
            tag.putUUID("larva", larva);
        }
        return tag;
    }
}
