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
import net.minecraft.client.resources.model.BakedModel;
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
public class SoldierRenderer<T extends SoldierEntity> extends GeoEntityRenderer<T> {

    private static final float SHADOW_RADIUS = 0.4F;
    private static final String HAND_BONE = "item_right";
    /** 0.6-D: segunda arma (mao esquerda) do Hoshina; o .geo.json do soldado comum nao tem este osso. */
    private static final String OFFHAND_BONE = "item_left";
    /** Ponto de origem do item no vanilla: 1 px abaixo do centro do punho e 2 px a frente (superficie do braco). */
    private static final float HAND_DOWN = 1.0F / 16.0F;
    private static final float HAND_FORWARD = 2.0F / 16.0F;
    private static final float PIXEL = 16.0F;
    /**
     * 0.2: o display de 3a pessoa das armas cresceu para o JOGADOR (held_scale por arma em
     * tools/art/meshy_assets.json, punho parado na mao). O soldado continua no tamanho de antes: divide pela escala
     * do display do item e multiplica por esta, a escala de 3a pessoa de antes (0,85 em todas as armas). 0.5.0-D4:
     * arma com display menor que isso (espada do Hoshina, encurtada pelas referencias do Miguel) fica do mesmo tamanho
     * que na mao do jogador.
     */
    private static final float SOLDIER_ITEM_SCALE = 0.85F;

    public SoldierRenderer(EntityRendererProvider.Context context) {
        this(context, "soldier");
    }

    /** 0.6-D: mesmo renderizador para os soldados especiais (esqueleto, malha e animacoes de {@code species}). */
    public SoldierRenderer(EntityRendererProvider.Context context, String species) {
        super(context, new SoldierModel<>(species));
        this.shadowRadius = SHADOW_RADIUS;
        addRenderLayer(new MeshRenderLayer<>(this, KN8Constants.id(species)));
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, T animatable) {
                if (HAND_BONE.equals(bone.getName())) {
                    return animatable.getMainHandItem();
                }
                return OFFHAND_BONE.equals(bone.getName()) ? animatable.getOffhandItem() : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                    T animatable) {
                // Tambem na mao esquerda: o vanilla espelha o display "lefthand" de novo (leftHand=true) e acaba
                // com a rotacao da direita; a GeckoLib desenha sem esse espelho e a lamina saia para baixo (0.6-D).
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            /**
             * A camada padrao gira o item de novo pela rotacao do proprio osso (a pose ja vem com ela aplicada);
             * aqui so volta ao pivo do osso da mao.
             */
            @Override
            public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone,
                    RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                    int packedLight, int packedOverlay) {
                ItemStack stack = getStackForBone(bone, animatable);
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                poseStack.pushPose();
                poseStack.translate(bone.getPivotX() / PIXEL, bone.getPivotY() / PIXEL, bone.getPivotZ() / PIXEL);
                // 0.5.0-D5: a rotacao animada do osso do item sai daqui e entra depois do Rx(-90), no mesmo lugar em
                // que a PAL gira o item do jogador: os mesmos numeros de animacao seguram a arma igual nos dois (o
                // giro fica em volta do punho, nao do pivo do osso).
                poseStack.mulPose(Axis.XP.rotation(-bone.getRotX()));
                poseStack.mulPose(Axis.YP.rotation(-bone.getRotY()));
                poseStack.mulPose(Axis.ZP.rotation(-bone.getRotZ()));
                renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight,
                        packedOverlay);
                // Igual a camada original: o item fechou o lote da entidade; reabre para os proximos ossos.
                bufferSource.getBuffer(renderType);
                poseStack.popPose();
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                    T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight,
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
                // Ordem e sinais da PAL (ItemInHandLayerMixin): Z por -Y, Y por -Z, X por -X da animacao. A GeckoLib
                // guarda X e Y do keyframe invertidos (rotX = -x, rotY = -y), dai os sinais abaixo.
                poseStack.mulPose(Axis.ZP.rotation(bone.getRotY()));
                poseStack.mulPose(Axis.YP.rotation(-bone.getRotZ()));
                poseStack.mulPose(Axis.XP.rotation(bone.getRotX()));
                BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, animatable.level(),
                        animatable, animatable.getId());
                float displayScale = model.getTransforms().getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        .scale.x();
                if (displayScale > 1.0E-4F) {
                    float scale = Math.min(SOLDIER_ITEM_SCALE, displayScale) / displayScale;
                    poseStack.scale(scale, scale, scale);
                }
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick,
                        packedLight, packedOverlay);
            }
        });
    }

    /**
     * Modelo com a cabeca seguindo o olhar (osso "head") e, com alvo e arma de fogo, os bracos seguindo a mira
     * (inclinacao e giro da cabeca somados a pose de mira da animacao).
     */
    private static final class SoldierModel<T extends SoldierEntity> extends DefaultedEntityGeoModel<T> {

        private static final String[] AIMING_ARMS = {"arm_right", "arm_left"};

        /** Soma o olhar a rotacao animada da cabeca (as animacoes desta especie sempre animam a cabeca). */
        private final boolean addLook;

        SoldierModel(String species) {
            // 0.5.0-D5: nos soldados especiais, sem o "turnsHead" da GeckoLib, que troca a rotacao animada da cabeca
            // pelo olhar (com o tronco inclinado a cabeca do Hoshina ficava olhando para o chao); o olhar e somado.
            // O soldado comum nao anima a cabeca e continua com o da GeckoLib.
            super(KN8Constants.id(species), "soldier".equals(species));
            this.addLook = !"soldier".equals(species);
        }

        @Override
        public void setCustomAnimations(T animatable, long instanceId,
                AnimationState<T> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            // Pausado a animacao nao roda de novo: somar aqui acumularia a cada quadro.
            EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            if (data == null || Minecraft.getInstance().isPaused()) {
                return;
            }
            float pitch = data.headPitch() * Mth.DEG_TO_RAD;
            float yaw = data.netHeadYaw() * Mth.DEG_TO_RAD;
            GeoBone head = addLook ? getAnimationProcessor().getBone("head") : null;
            if (head != null) {
                head.setRotX(head.getRotX() + pitch);
                head.setRotY(head.getRotY() + yaw);
            }
            if (!animatable.isAggressive() || !animatable.isFirearmPose()) {
                return;
            }
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
