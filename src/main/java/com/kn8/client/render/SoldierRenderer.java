// src/main/java/com/kn8/client/render/SoldierRenderer.java
package com.kn8.client.render;

import com.kn8.KN8Constants;
import com.kn8.client.render.mesh.MeshRenderLayer;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Soldado (0.1-B / Etapa F): esqueleto GeckoLib so de ossos ({@code geo/entity/soldier.geo.json}), malha do Meshy
 * presa aos ossos ({@link MeshRenderLayer}, {@code meshes/soldier.json}) e a arma da mao principal desenhada no osso
 * {@code item_right} com o modelo de item que ja existe (o mesmo da mao do jogador).
 */
public class SoldierRenderer extends GeoEntityRenderer<SoldierEntity> {

    private static final float SHADOW_RADIUS = 0.4F;
    private static final String HAND_BONE = "item_right";

    public SoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(KN8Constants.id("soldier"), false));
        this.shadowRadius = SHADOW_RADIUS;
        addRenderLayer(new MeshRenderLayer<>(this, KN8Constants.id("soldier")));
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, SoldierEntity animatable) {
                return HAND_BONE.equals(bone.getName()) ? animatable.getMainHandItem() : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                    SoldierEntity animatable) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                    SoldierEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight,
                    int packedOverlay) {
                // Mesma orientacao que o vanilla usa para itens na mao (ItemInHandLayer).
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick,
                        packedLight, packedOverlay);
            }
        });
    }
}
