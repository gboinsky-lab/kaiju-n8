// src/main/java/com/kn8/client/render/mesh/MeshModels.java
package com.kn8.client.render.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kn8.KN8Constants;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Carrega (sob demanda) e guarda as malhas por especie: {@code assets/<ns>/meshes/<especie>.json} lista
 * {@code "bones": {"osso": "<ns>:meshes/<especie>/<osso>.obj"}}. Especie sem esse arquivo = sem malha (modelo so
 * GeckoLib, como os de cubos). Recarrega com F3+T. OBJ suportado: triangulos com v/vt/vn (como os gerados por
 * tools/art). Erro de leitura vira log e a especie fica sem malha, nunca crash.
 */
public final class MeshModels {

    private static final Map<ResourceLocation, Optional<MeshModel>> CACHE = new ConcurrentHashMap<>();

    private MeshModels() {
    }

    /** Mod bus: limpa o cache quando os recursos recarregam. */
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> CACHE.clear());
    }

    public static Optional<MeshModel> get(ResourceLocation species) {
        return CACHE.computeIfAbsent(species, MeshModels::load);
    }

    private static Optional<MeshModel> load(ResourceLocation species) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation indexPath = species.withPath(path -> "meshes/" + path + ".json");
        Optional<Resource> index = manager.getResource(indexPath);
        if (index.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = index.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, float[]> bones = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("bones").entrySet()) {
                ResourceLocation objPath = ResourceLocation.parse(entry.getValue().getAsString());
                Optional<Resource> obj = manager.getResource(objPath);
                if (obj.isEmpty()) {
                    KN8Constants.LOGGER.warn("[kn8] Malha {} do osso {} nao encontrada.", objPath, entry.getKey());
                    continue;
                }
                bones.put(entry.getKey(), readObj(obj.get()));
            }
            KN8Constants.LOGGER.info("[kn8] Malha de {} carregada: {} ossos.", species, bones.size());
            return Optional.of(new MeshModel(Map.copyOf(bones)));
        } catch (IOException | RuntimeException exception) {
            KN8Constants.LOGGER.error("[kn8] Malha de {} invalida: {}", species, exception.getMessage());
            return Optional.empty();
        }
    }

    /** Le um OBJ de triangulos com v/vt/vn para o vetor plano do {@link MeshModel}. */
    static float[] readObj(Resource resource) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> corners = new ArrayList<>();
        try (BufferedReader reader = resource.openAsReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(floats(parts));
                    case "vt" -> uvs.add(floats(parts));
                    case "vn" -> normals.add(floats(parts));
                    case "f" -> {
                        for (int i = 1; i <= 3; i++) {
                            String[] indices = parts[i].split("/");
                            corners.add(new int[] {Integer.parseInt(indices[0]) - 1,
                                    Integer.parseInt(indices[1]) - 1, Integer.parseInt(indices[2]) - 1});
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        float[] data = new float[corners.size() * MeshModel.FLOATS_PER_VERTEX];
        int offset = 0;
        for (int[] corner : corners) {
            float[] p = positions.get(corner[0]);
            float[] t = uvs.get(corner[1]);
            float[] n = normals.get(corner[2]);
            data[offset++] = p[0];
            data[offset++] = p[1];
            data[offset++] = p[2];
            data[offset++] = t[0];
            // OBJ tem V para cima; a textura do Minecraft tem V para baixo.
            data[offset++] = 1.0F - t[1];
            data[offset++] = n[0];
            data[offset++] = n[1];
            data[offset++] = n[2];
        }
        return data;
    }

    private static float[] floats(String[] parts) {
        float[] values = new float[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            values[i - 1] = Float.parseFloat(parts[i]);
        }
        return values;
    }
}
