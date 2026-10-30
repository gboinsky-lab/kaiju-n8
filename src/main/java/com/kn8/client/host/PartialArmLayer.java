package com.kn8.client.host;

import com.kn8.client.render.mesh.MeshModel;
import com.kn8.client.render.mesh.MeshModels;
import com.kn8.common.host.KaijuFormService;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * HOST: transformacao parcial em terceira pessoa. O braco direito do jogador (escondido no
 * {@link KaijuFormClient#onRenderLiving}) e trocado pela malha do braco do No. 8, presa a {@code rightArm} do modelo
 * do jogador: segue andar, golpes e a PAL como o braco de verdade. Mesma conta do braco em primeira pessoa.
 */
public class PartialArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PartialArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (!KaijuFormService.isPartial(player) || player.isInvisible()) {
            return;
        }
        MeshModel model = MeshModels.get(KaijuFormClient.SPECIES).orElse(null);
        if (model == null) {
            return;
        }
        poseStack.pushPose();
        getParentModel().rightArm.translateAndRotate(poseStack);
        KaijuFormClient.toArmPivot(poseStack);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(KaijuFormClient.TEXTURE));
        KaijuFormClient.drawArm(poseStack.last(), buffer, model, packedLight);
        poseStack.popPose();
    }
}
