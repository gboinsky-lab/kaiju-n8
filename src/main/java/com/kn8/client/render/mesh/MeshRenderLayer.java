// src/main/java/com/kn8/client/render/mesh/MeshRenderLayer.java
package com.kn8.client.render.mesh;

import java.util.Optional;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Desenha a malha de cada osso (modelos do Meshy) com a transformacao do osso ja aplicada pela GeckoLib: as
 * animacoes e os 4 controllers continuam valendo. Usa o mesmo buffer/render type e a mesma textura do modelo GeckoLib
 * (por isso o modelo dessas especies nao tem cubos): nada de trocar estado de render no meio do desenho.
 *
 * <p>As entidades usam quads: cada triangulo vira um quad degenerado (o ultimo vertice repetido).</p>
 */
public class MeshRenderLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    private static final int WHITE = 0xFFFFFFFF;

    private final Function<T, ResourceLocation> species;

    public MeshRenderLayer(GeoRenderer<T> renderer, ResourceLocation species) {
        this(renderer, animatable -> species);
    }

    /** Especie decidida por instancia (ex.: carcaca, que guarda a especie do kaiju morto). */
    public MeshRenderLayer(GeoRenderer<T> renderer, Function<T, ResourceLocation> species) {
        super(renderer);
        this.species = species;
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
            MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
            int packedOverlay) {
        Optional<MeshModel> model = MeshModels.get(species.apply(animatable));
        if (model.isEmpty() || bone.isHidden()) {
            return;
        }
        float[] data = model.get().bone(bone.getName());
        if (data == null) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        int stride = MeshModel.FLOATS_PER_VERTEX;
        for (int triangle = 0; triangle < data.length; triangle += stride * 3) {
            for (int corner = 0; corner < 4; corner++) {
                // Quad degenerado: vertices 0, 1, 2, 2.
                int i = triangle + Math.min(corner, 2) * stride;
                buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                        .setColor(WHITE)
                        .setUv(data[i + 3], data[i + 4])
                        .setOverlay(packedOverlay)
                        .setLight(packedLight)
                        .setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
            }
        }
    }
}
