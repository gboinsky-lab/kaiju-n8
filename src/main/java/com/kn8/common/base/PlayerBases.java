package com.kn8.common.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Bases dos jogadores (PM-8D), por dimensao, salvas no mundo: uma por jogador, com a posicao do Nucleo da Base e o
 * modulo de cada um dos {@link #SLOTS} lotes em volta dele (vazio = sem modulo).
 */
public final class PlayerBases extends SavedData {

    public static final int SLOTS = 8;
    private static final String DATA_NAME = "kn8_player_bases";

    /** Base de um jogador: nucleo e um id de modulo por lote (null = vazio). */
    public static final class Base {
        private final BlockPos core;
        private final ResourceLocation[] slots = new ResourceLocation[SLOTS];

        Base(BlockPos core) {
            this.core = core.immutable();
        }

        public BlockPos core() {
            return core;
        }

        public Optional<ResourceLocation> slot(int index) {
            return Optional.ofNullable(slots[index]);
        }

        /** Ids dos modulos por lote ("" = vazio), na ordem dos lotes (para o menu). */
        public List<String> slotIds() {
            List<String> list = new ArrayList<>();
            for (ResourceLocation id : slots) {
                list.add(id == null ? "" : id.toString());
            }
            return list;
        }

        public boolean has(ResourceLocation module) {
            return Arrays.asList(slots).contains(module);
        }
    }

    private final Map<UUID, Base> bases = new LinkedHashMap<>();

    public static PlayerBases get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(PlayerBases::new, PlayerBases::load, null),
                DATA_NAME);
    }

    public Optional<Base> of(UUID owner) {
        return Optional.ofNullable(bases.get(owner));
    }

    /** Dono do nucleo nesta posicao. */
    public Optional<UUID> ownerAt(BlockPos core) {
        for (Map.Entry<UUID, Base> entry : bases.entrySet()) {
            if (entry.getValue().core.equals(core)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public Base found(UUID owner, BlockPos core) {
        Base base = new Base(core);
        bases.put(owner, base);
        setDirty();
        return base;
    }

    public void setSlot(UUID owner, int index, ResourceLocation module) {
        Base base = bases.get(owner);
        if (base != null) {
            base.slots[index] = module;
            setDirty();
        }
    }

    public Optional<UUID> removeAt(BlockPos core) {
        Optional<UUID> owner = ownerAt(core);
        owner.ifPresent(id -> {
            bases.remove(id);
            setDirty();
        });
        return owner;
    }

    public static PlayerBases load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerBases data = new PlayerBases();
        for (String key : tag.getAllKeys()) {
            CompoundTag entry = tag.getCompound(key);
            UUID owner;
            try {
                owner = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            int[] c = entry.getIntArray("core");
            if (c.length != 3) {
                continue;
            }
            Base base = new Base(new BlockPos(c[0], c[1], c[2]));
            ListTag slots = entry.getList("slots", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(SLOTS, slots.size()); i++) {
                String id = slots.getString(i);
                base.slots[i] = id.isEmpty() ? null : ResourceLocation.tryParse(id);
            }
            data.bases.put(owner, base);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        bases.forEach((owner, base) -> {
            CompoundTag entry = new CompoundTag();
            entry.putIntArray("core", new int[] {base.core.getX(), base.core.getY(), base.core.getZ()});
            ListTag slots = new ListTag();
            for (String id : base.slotIds()) {
                slots.add(StringTag.valueOf(id));
            }
            entry.put("slots", slots);
            tag.put(owner.toString(), entry);
        });
        return tag;
    }
}
