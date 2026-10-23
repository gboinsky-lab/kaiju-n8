package com.kn8.common.site;

import java.util.LinkedHashMap;
import java.util.Map;

import com.kn8.core.world.RegionAlertMath;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estado de alerta de cada regiao (PM-9), por dimensao, salvo no mundo. Regiao = uma estrutura de local da obra ja
 * gerada (duas Shinagawa no mesmo mundo sao duas regioes): chave = id do local + chunk da peca inicial. So guarda as
 * regioes que ja sairam do normal; a que volta ao normal sai da lista.
 */
public final class RegionAlerts extends SavedData {

    private static final String DATA_NAME = "kn8_region_alerts";

    /** Uma regiao fora do normal: local, caixa da estrutura, estado, prazo e causa (id do kaiju ou da invasao). */
    public static final class Region {
        private final ResourceLocation site;
        private final BoundingBox box;
        private RegionAlertMath.Status status = RegionAlertMath.Status.NORMAL;
        private String cause = "";

        Region(ResourceLocation site, BoundingBox box) {
            this.site = site;
            this.box = box;
        }

        public ResourceLocation site() {
            return site;
        }

        public BoundingBox box() {
            return box;
        }

        public RegionAlertMath.Status status() {
            return status;
        }

        public String cause() {
            return cause;
        }

        void set(RegionAlertMath.Status value, String newCause) {
            status = value;
            cause = newCause;
        }
    }

    private final Map<String, Region> regions = new LinkedHashMap<>();

    public static RegionAlerts get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(RegionAlerts::new, RegionAlerts::load, null),
                DATA_NAME);
    }

    public Map<String, Region> regions() {
        return regions;
    }

    Region getOrCreate(String key, ResourceLocation site, BoundingBox box) {
        return regions.computeIfAbsent(key, k -> new Region(site, box));
    }

    public static RegionAlerts load(CompoundTag tag, HolderLookup.Provider registries) {
        RegionAlerts data = new RegionAlerts();
        for (String key : tag.getAllKeys()) {
            CompoundTag entry = tag.getCompound(key);
            ResourceLocation site = ResourceLocation.tryParse(entry.getString("site"));
            int[] b = entry.getIntArray("box");
            if (site == null || b.length != 6) {
                continue;
            }
            Region region = new Region(site, new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
            region.set(new RegionAlertMath.Status(RegionAlertMath.State.byIndex(entry.getInt("state")),
                    entry.getLong("until")), entry.getString("cause"));
            data.regions.put(key, region);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        regions.forEach((key, region) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("site", region.site.toString());
            BoundingBox b = region.box;
            entry.putIntArray("box", new int[] {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            entry.putInt("state", region.status.state().ordinal());
            entry.putLong("until", region.status.until());
            entry.putString("cause", region.cause);
            tag.put(key, entry);
        });
        return tag;
    }
}
