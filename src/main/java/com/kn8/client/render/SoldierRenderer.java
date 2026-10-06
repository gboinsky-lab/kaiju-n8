package com.kn8.client.render;

import org.joml.Quaternionf;

import com.kn8.KN8Constants;
import com.kn8.client.render.mesh.MeshRenderLayer;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Soldado (0.1-B / Etapa F): esqueleto GeckoLib so de ossos ({@code geo/entity/soldier.geo.json}), malha do Meshy
 * presa aos ossos ({@link MeshRenderLayer}, {@code meshes/soldier.json}) e a arma da mao principal desenhada no osso
 * {@code item_right} com o modelo de item que ja existe (o mesmo da mao do jogador).
 *
 * <p>Arma na mao (correcao do teste manual): a cadeia e a mesma do jogador vanilla (ItemInHandLayer), convertida
 * para o espaco da GeckoLib (Y para cima): o vanilla faz Rx(-90) Ry(180) com o braco de Y para baixo, que aqui vira
 * so Rx(-90). Antes de girar, o item e alinhado a linha ombro -> mao do braco (a malha do Meshy tem os bracos
 * abertos em "A", nao retos como o braco vanilla) e posto no ponto da mao equivalente ao do vanilla. Conferido fora
 * do jogo com {@code tools/art/preview_held_items.py}.</p>
 */
public class SoldierRenderer extends GeoEntityRenderer<SoldierEntity> {

    private static final float SHADOW_RADIUS = 0.4F;
    private static final String HAND_BONE = "item_right";
    /** Ponto de origem do item no vanilla: 1 px abaixo do centro do punho e 2 px a frente (superficie do braco). */
    private static final float HAND_DOWN = 1.0F / 16.0F;
    private static final float HAND_FORWARD = 2.0F / 16.0F;
    private static final float PIXEL = 16.0F;

    public SoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new SoldierModel());
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

            /**
             * A camada padrao gira o item de novo pela rotacao do proprio osso (a pose ja vem com ela aplicada);
             * aqui so volta ao pivo do osso da mao.
             */
            @Override
            public void renderForBone(PoseStack poseStack, SoldierEntity animatable, GeoBone bone,
                    RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                    int packedLight, int packedOverlay) {
                ItemStack stack = getStackForBone(bone, animatable);
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                poseStack.pushPose();
                poseStack.translate(bone.getPivotX() / PIXEL, bone.getPivotY() / PIXEL, bone.getPivotZ() / PIXEL);
                renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight,
                        packedOverlay);
                // Igual a camada original: o item fechou o lote da entidade; reabre para os proximos ossos.
                bufferSource.getBuffer(renderType);
                poseStack.popPose();
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                    SoldierEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight,
                    int packedOverlay) {
                GeoBone arm = bone.getParent();
                if (arm != null) {
                    // Eixo do braco em repouso (ombro -> mao) no lugar do "para baixo" do braco vanilla.
                    float dx = bone.getPivotX() - arm.getPivotX();
                    float dy = bone.getPivotY() - arm.getPivotY();
                    float dz = bone.getPivotZ() - arm.getPivotZ();
                    if (dx * dx + dy * dy + dz * dz > 1.0E-4F) {
                        poseStack.mulPose(new Quaternionf().rotationTo(0.0F, -1.0F, 0.0F, dx, dy, dz));
                    }
                }
                poseStack.translate(0.0F, -HAND_DOWN, -HAND_FORWARD);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick,
                        packedLight, packedOverlay);
            }
        });
    }

    /**
     * Modelo com a cabeca seguindo o olhar (osso "head") e, com alvo e arma de fogo, os bracos seguindo a mira
     * (inclinacao e giro da cabeca somados a pose de mira da animacao).
     */
    private static final class SoldierModel extends DefaultedEntityGeoModel<SoldierEntity> {

        private static final String[] AIMING_ARMS = {"arm_right", "arm_left"};

        SoldierModel() {
            super(KN8Constants.id("soldier"), true);
        }

        @Override
        public void setCustomAnimations(SoldierEntity animatable, long instanceId,
                AnimationState<SoldierEntity> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            // Pausado a animacao nao roda de novo: somar aqui acumularia a cada quadro.
            if (!animatable.isAggressive() || !animatable.isFirearmPose() || Minecraft.getInstance().isPaused()) {
                return;
            }
            EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            if (data == null) {
                return;
            }
            float pitch = data.headPitch() * Mth.DEG_TO_RAD;
            float yaw = data.netHeadYaw() * Mth.DEG_TO_RAD;
            for (String name : AIMING_ARMS) {
                GeoBone arm = getAnimationProcessor().getBone(name);
                if (arm != null) {
                    arm.setRotX(arm.getRotX() + pitch);
                    arm.setRotY(arm.getRotY() + yaw);
                }
            }
        }
    }
}
