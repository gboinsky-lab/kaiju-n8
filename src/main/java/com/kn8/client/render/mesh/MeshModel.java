// src/main/java/com/kn8/client/render/mesh/MeshModel.java
package com.kn8.client.render.mesh;

import java.util.Map;

/**
 * Malha de um modelo dividida por osso (Etapa A/C: modelos do Meshy presos aos ossos da GeckoLib). Cada osso guarda
 * seus triangulos como um vetor plano: por vertice {@code x, y, z, u, v, nx, ny, nz} (coordenadas do modelo em
 * blocos, UV ja no sentido da textura do Minecraft). {@code pivots} (opcional no JSON, EQUIP-1): ponto de giro de
 * uma peca solta presa a outra (cauda do traje Numbers 10 e asas do Numbers 4 no tronco), em blocos no espaco da
 * peca-mae.
 */
public record MeshModel(Map<String, float[]> bones, Map<String, float[]> pivots) {

    public static final int FLOATS_PER_VERTEX = 8;

    public MeshModel(Map<String, float[]> bones) {
        this(bones, Map.of());
    }

    public float[] bone(String name) {
        return bones.get(name);
    }

    public float[] pivot(String name) {
        return pivots.get(name);
    }
}
