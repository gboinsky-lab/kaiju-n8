// src/main/java/com/kn8/client/render/suit/SuitLayer.java
package com.kn8.client.render.suit;

import java.util.Optional;

import com.kn8.client.render.mesh.MeshModel;
import com.kn8.client.render.mesh.MeshModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Traje 3D da Forca de Defesa (0.6-C): o traje vestido no peito, se tiver malha ({@code meshes/suit/<item>.json},
 * gerada por tools/art/rig_suit_mesh.py), e desenhado preso as partes do modelo humanoide (tronco, bracos, pernas)
 * com a transformacao de cada parte: segue andar, agachar, correr e as animacoes de combate. Textura em
 * {@code textures/models/suit/<item>.png}. Trajes sem malha (traje de treino) continuam com a armadura vanilla.
 * EQUIP-1: partes opcionais {@code head} (capacete/gola alta, presa a cabeca) e {@code tail} (cauda do Numbers 10,
 * presa ao tronco pelo pivo do JSON e balancando devagar, mais ao andar).
 */
public class SuitLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final int WHITE = 0xFFFFFFFF;
    static final String[] PARTS = {"body", "right_arm", "left_arm", "right_leg", "left_leg", "head"};
    /** Balanco da cauda: graus parado, graus a mais com a passada e velocidade (radianos por tick). */
    private static final float TAIL_SWAY_DEGREES = 4.0F;
    private static final float TAIL_WALK_DEGREES = 6.0F;
    private static final float TAIL_SWAY_SPEED = 0.07F;

    public SuitLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    /** Id da malha do traje vestido ({@code <ns>:suit/<item>}), se o item do peito tiver uma. */
    static Optional<ResourceLocation> suitOf(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(chest.getItem());
        ResourceLocation suit = item.withPrefix("suit/");
        return MeshModels.get(suit).isPresent() ? Optional.of(suit) : Optional.empty();
    }

    static RenderType renderType(ResourceLocation suit) {
        return RenderType.entityCutoutNoCull(suit.withPath(path -> "textures/models/" + path + ".png"));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        Optional<ResourceLocation> suit = suitOf(entity);
        if (suit.isEmpty()) {
            return;
        }
        MeshModel mesh = MeshModels.get(suit.get()).orElseThrow();
        VertexConsumer buffer = buffers.getBuffer(renderType(suit.get()));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        M model = getParentModel();
        for (String part : PARTS) {
            ModelPart modelPart = part(model, part);
            if (modelPart.visible) {
                drawPart(poseStack, buffer, mesh.bone(part), modelPart, packedLight, overlay);
            }
        }
        float[] tail = mesh.bone("tail");
        float[] root = mesh.pivot("tail");
        if (tail != null && root != null && model.body.visible) {
            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            poseStack.translate(root[0], root[1], root[2]);
            float sway = (float) Math.sin(ageInTicks * TAIL_SWAY_SPEED);
            float walk = Math.min(1.0F, limbSwingAmount) * (float) Math.sin(limbSwing * 0.6662F);
            poseStack.mulPose(Axis.YP.rotationDegrees(sway * TAIL_SWAY_DEGREES + walk * TAIL_WALK_DEGREES));
            poseStack.mulPose(Axis.XP.rotationDegrees(
                    (float) Math.cos(ageInTicks * TAIL_SWAY_SPEED * 0.7F) * TAIL_SWAY_DEGREES * 0.5F));
            drawMesh(poseStack.last(), buffer, tail, packedLight, overlay);
            poseStack.popPose();
        }
    }

    static ModelPart part(HumanoidModel<?> model, String part) {
        return switch (part) {
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            case "head" -> model.head;
            default -> model.body;
        };
    }

    /** Desenha a malha de uma parte no espaco local dela (translateAndRotate da ModelPart). */
    static void drawPart(PoseStack poseStack, VertexConsumer buffer, float[] data, ModelPart modelPart,
            int packedLight, int overlay) {
        if (data == null) {
            return;
        }
        poseStack.pushPose();
        modelPart.translateAndRotate(poseStack);
        drawMesh(poseStack.last(), buffer, data, packedLight, overlay);
        poseStack.popPose();
    }

    static void drawMesh(PoseStack.Pose pose, VertexConsumer buffer, float[] data, int packedLight, int overlay) {
        int stride = MeshModel.FLOATS_PER_VERTEX;
        for (int triangle = 0; triangle < data.length; triangle += stride * 3) {
            for (int corner = 0; corner < 4; corner++) {
                // Render type de entidade usa quads: cada triangulo vira um quad degenerado (0, 1, 2, 2).
                int i = triangle + Math.min(corner, 2) * stride;
                buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                        .setColor(WHITE)
                        .setUv(data[i + 3], data[i + 4])
                        .setOverlay(overlay)
                        .setLight(packedLight)
                        .setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
            }
        }
    }
}
