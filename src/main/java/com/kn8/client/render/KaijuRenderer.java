// src/main/java/com/kn8/client/render/KaijuRenderer.java
package com.kn8.client.render;

import com.kn8.client.render.mesh.MeshRenderLayer;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib generico de kaiju: um por especie, apontando para {@code geo/entity/<especie>.geo.json},
 * {@code animations/entity/<especie>.animation.json} e {@code textures/entity/<especie>.png}. Trocar a arte de uma
 * especie e so substituir esses tres arquivos (mesmos nomes de ossos e animacoes do kit de arte).
 *
 * <p>Especies com malha do Meshy ({@code meshes/<especie>.json}) tem o .geo.json so com ossos e a malha desenhada
 * pelo {@link MeshRenderLayer}; as demais (cubos) nao mudam.</p>
 */
public final class KaijuRenderer extends GeoEntityRenderer<KaijuEntity> {

    public KaijuRenderer(EntityRendererProvider.Context context, ResourceLocation species, float shadowRadius) {
        super(context, new DefaultedEntityGeoModel<>(species, false));
        this.shadowRadius = shadowRadius;
        addRenderLayer(new MeshRenderLayer<>(this, species));
    }
}
