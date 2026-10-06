// src/main/java/com/kn8/client/render/CarcassRenderer.java
package com.kn8.client.render;

import com.kn8.client.render.mesh.MeshRenderLayer;
import com.kn8.common.kaiju.CarcassEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Carcaca (M11b / 0.1-B): desenha o modelo da especie guardada na carcaca (cubos ou malha do Meshy), parado. O
 * modelo e escolhido por instancia, entao um so renderer serve para todas as especies.
 *
 * <p>0.2 (pedido do Miguel): a carcaca tomba ao morrer. Especie mais larga que alta (aranha) vira de costas;
 * bipede cai de lado. A queda dura {@link #FALL_TICKS} e so muda o desenho: a hitbox continua em pe (e nela que o
 * jogador mira para desmontar).</p>
 */
public class CarcassRenderer extends GeoEntityRenderer<CarcassEntity> {

    private static final float SHADOW_RADIUS = 1.5F;
    private static final float FALL_TICKS = 14.0F;

    public CarcassRenderer(EntityRendererProvider.Context context) {
        super(context, new SpeciesModel());
        this.shadowRadius = SHADOW_RADIUS;
        addRenderLayer(new MeshRenderLayer<>(this, CarcassEntity::species));
    }

    @Override
    protected void applyRotations(CarcassEntity carcass, PoseStack poseStack, float ageInTicks, float rotationYaw,
            float partialTick, float nativeScale) {
        super.applyRotations(carcass, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        float fall = Mth.clamp((carcass.tickCount + partialTick) / FALL_TICKS, 0.0F, 1.0F);
        // Queda acelerando (como um corpo pesado), sem quicar.
        float eased = fall * fall;
        float width = carcass.getBbWidth();
        float height = carcass.getBbHeight();
        if (width > height) {
            // De costas: gira 180 graus no eixo do comprimento e sobe a altura para ficar apoiada no chao.
            poseStack.translate(0.0F, height * eased, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F * eased));
        } else {
            // De lado: gira 90 graus em volta do pe e sobe meia largura (o corpo nao afunda no chao).
            poseStack.translate(0.0F, width * 0.5F * eased, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F * eased));
        }
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
