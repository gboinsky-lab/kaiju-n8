// src/main/java/com/kn8/client/render/suit/SuitAccessoryLayer.java
package com.kn8.client.render.suit;

import java.util.Map;
import java.util.WeakHashMap;

import com.kn8.KN8Constants;
import com.kn8.client.render.mesh.MeshModel;
import com.kn8.client.render.mesh.MeshModels;
import com.kn8.common.registry.KN8Items;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * Acessorios 3D do jogador (EQUIP-1, modelos do Miguel): asas do Numbers 4 nas costas (traje Numbers 4 vestido ou
 * machado Numbers 4 na mao; abertas no ar, dobradas para tras no chao) e manoplas do Numbers 2 nos dois antebracos
 * (com as manoplas na mao; o item segurado fica escondido em 3a pessoa pelo display do modelo). Malhas por
 * tools/art/rig_accessory_mesh.py, no formato dos trajes ({@link SuitLayer#drawPart}).
 */
public class SuitAccessoryLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    static final ResourceLocation WINGS = KN8Constants.id("suit/numbers4_wings");
    static final ResourceLocation GAUNTLETS = KN8Constants.id("suit/numbers2_gauntlets");
    /** Asas dobradas: cada metade gira para tras em volta da mochila [SUPOSICAO angulos]. */
    private static final float FOLD_DEGREES = 62.0F;
    private static final float FOLD_TILT_DEGREES = 12.0F;
    /** Ticks para abrir/fechar inteiro. */
    private static final float FOLD_TICKS = 6.0F;
    /** Batida leve das asas abertas (graus, radianos por tick). */
    private static final float BEAT_DEGREES = 7.0F;
    private static final float BEAT_SPEED = 0.45F;

    /** Abertura (0 dobrada, 1 aberta) por entidade, avancada no tempo de jogo de cada uma. */
    private final Map<LivingEntity, float[]> openness = new WeakHashMap<>();

    public SuitAccessoryLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    static boolean hasWings(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(KN8Items.NUMBERS4_SUIT.get())
                || entity.getMainHandItem().is(KN8Items.NUMBERS4_AXE.get());
    }

    static boolean hasGauntlets(LivingEntity entity) {
        Item gauntlets = KN8Items.NUMBERS2_GAUNTLETS.get();
        return entity.getMainHandItem().is(gauntlets) || entity.getOffhandItem().is(gauntlets);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        M model = getParentModel();
        if (hasGauntlets(entity)) {
            MeshModels.get(GAUNTLETS).ifPresent(mesh -> {
                VertexConsumer buffer = buffers.getBuffer(SuitLayer.renderType(GAUNTLETS));
                if (model.rightArm.visible) {
                    SuitLayer.drawPart(poseStack, buffer, mesh.bone("right_arm"), model.rightArm, packedLight,
                            overlay);
                }
                if (model.leftArm.visible) {
                    SuitLayer.drawPart(poseStack, buffer, mesh.bone("left_arm"), model.leftArm, packedLight, overlay);
                }
            });
        }
        if (hasWings(entity) && model.body.visible) {
            MeshModels.get(WINGS).ifPresent(mesh -> renderWings(poseStack, buffers, mesh, entity, ageInTicks,
                    packedLight, overlay));
        }
    }

    private void renderWings(PoseStack poseStack, MultiBufferSource buffers, MeshModel mesh, T entity,
            float ageInTicks, int packedLight, int overlay) {
        float open = openness(entity, ageInTicks);
        VertexConsumer buffer = buffers.getBuffer(SuitLayer.renderType(WINGS));
        float beat = open * (float) Math.sin(ageInTicks * BEAT_SPEED) * BEAT_DEGREES;
        for (String part : new String[] {"wing_right", "wing_left"}) {
            float[] data = mesh.bone(part);
            float[] root = mesh.pivot(part);
            if (data == null || root == null) {
                continue;
            }
            // Direita do personagem em X negativo: a metade da direita gira no sentido contrario.
            float side = "wing_right".equals(part) ? 1.0F : -1.0F;
            float fold = (1.0F - open) * FOLD_DEGREES;
            poseStack.pushPose();
            getParentModel().body.translateAndRotate(poseStack);
            poseStack.translate(root[0], root[1], root[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * (fold + beat)));
            poseStack.mulPose(Axis.XP.rotationDegrees((1.0F - open) * FOLD_TILT_DEGREES));
            SuitLayer.drawMesh(poseStack.last(), buffer, data, packedLight, overlay);
            poseStack.popPose();
        }
    }

    /** Abre no ar (fora da agua), fecha no chao; a abertura anda {@code 1 / FOLD_TICKS} por tick de jogo. */
    private float openness(T entity, float ageInTicks) {
        float[] state = openness.computeIfAbsent(entity, e -> new float[] {0.0F, ageInTicks});
        float target = !entity.onGround() && !entity.isInWater() && !entity.isPassenger() ? 1.0F : 0.0F;
        float step = Math.max(0.0F, ageInTicks - state[1]) / FOLD_TICKS;
        state[0] = state[0] < target ? Math.min(target, state[0] + step) : Math.max(target, state[0] - step);
        state[1] = ageInTicks;
        return Mth.clamp(state[0], 0.0F, 1.0F);
    }
}
