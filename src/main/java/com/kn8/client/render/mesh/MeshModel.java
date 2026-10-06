// src/main/java/com/kn8/client/render/mesh/MeshModel.java
package com.kn8.client.render.mesh;

import java.util.Map;

/**
 * Malha de um modelo dividida por osso (Etapa A/C: modelos do Meshy presos aos ossos da GeckoLib). Cada osso guarda
 * seus triangulos como um vetor plano: por vertice {@code x, y, z, u, v, nx, ny, nz} (coordenadas do modelo em
 * blocos, UV ja no sentido da textura do Minecraft).
 */
public record MeshModel(Map<String, float[]> bones) {

    public static final int FLOATS_PER_VERTEX = 8;

    public float[] bone(String name) {
        return bones.get(name);
    }
}
