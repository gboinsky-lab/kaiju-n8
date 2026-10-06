// src/main/java/com/kn8/client/render/CarcassRenderer.java
package com.kn8.client.render;

import com.kn8.client.render.mesh.MeshRenderLayer;
import com.kn8.common.kaiju.CarcassEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Carcaca (M11b / 0.1-B): desenha o modelo da especie guardada na carcaca (cubos ou malha do Meshy), parado. O
 * modelo e escolhido por instancia, entao um so renderer serve para todas as especies.
 */
public class CarcassRenderer extends GeoEntityRenderer<CarcassEntity> {

    private static final float SHADOW_RADIUS = 1.5F;

    public CarcassRenderer(EntityRendererProvider.Context context) {
        super(context, new SpeciesModel());
        this.shadowRadius = SHADOW_RADIUS;
        addRenderLayer(new MeshRenderLayer<>(this, CarcassEntity::species));
    }

    /** Arquivos da especie da carcaca: geo/, textures/ e animations/ do kaiju. */
    private static final class SpeciesModel extends GeoModel<CarcassEntity> {

        @Override
        public ResourceLocation getModelResource(CarcassEntity animatable) {
            return animatable.species().withPath(path -> "geo/entity/" + path + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(CarcassEntity animatable) {
            return animatable.species().withPath(path -> "textures/entity/" + path + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(CarcassEntity animatable) {
            return animatable.species().withPath(path -> "animations/entity/" + path + ".animation.json");
        }
    }
}
