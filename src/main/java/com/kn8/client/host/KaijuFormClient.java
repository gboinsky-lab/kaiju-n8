package com.kn8.client.host;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.kn8.KN8Constants;
import com.kn8.client.render.mesh.MeshModel;
import com.kn8.client.render.mesh.MeshModels;
import com.kn8.common.host.KaijuFormC2S;
import com.kn8.common.host.KaijuFormService;
import com.kn8.common.registry.KN8Attachments;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * T-2: lado do cliente da forma do Kaiju No. 8. Tecla B (a V ja e o bloqueio) manda so a intencao; o servidor decide.
 * Quem esta transformado (dado publico {@code kn8:kaiju_form}) e desenhado como o No. 8 por um fantoche
 * ({@link PuppetNo8}) em todos os clientes; em primeira pessoa a mao vazia vira o braco do No. 8 (malha do osso
 * {@code arm_right}, presa ao braco do jogador como o vanilla desenha a mao).
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class KaijuFormClient {

    public static final KeyMapping FORM_KEY = new KeyMapping("key.kn8.kaiju_form", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, "key.categories.kn8");

    private static final ResourceLocation SPECIES = KN8Constants.id("kaiju_no8");
    private static final ResourceLocation TEXTURE = KN8Constants.id("textures/entity/kaiju_no8.png");
    /** Sem controle do corpo: linhas e olhos vermelhos (mesma textura do fantoche). */
    private static final ResourceLocation RAMPAGE_TEXTURE = KN8Constants.id("textures/entity/kaiju_no8_rampage.png");
    /** RIG-2: o braco tem tres pecas (ombro, antebraco, mao); em repouso ficam no lugar, desenhadas juntas. */
    private static final String[] ARM_BONES = {"arm_right", "forearm_right", "hand_right"};
    /** Pivo do osso {@code arm_right} no espaco da GeckoLib (blocos; X ja invertido como a GeckoLib carrega). */
    private static final float ARM_PIVOT_X = 4.35F / 16.0F;
    private static final float ARM_PIVOT_Y = 24.975F / 16.0F;
    private static final float ARM_PIVOT_Z = -0.128F / 16.0F;
    /** Braco em primeira pessoa (posicao/escala na tela, conferidas em jogo). */
    private static final float ARM_SCALE = 0.85F;
    private static final int WHITE = 0xFFFFFFFF;
    private static final float RAMPAGE_TURN_DEGREES = 30.0F;

    private static final Map<UUID, PuppetNo8> PUPPETS = new HashMap<>();

    private KaijuFormClient() {
    }

    /** Mod bus. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(FORM_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            PUPPETS.clear();
            return;
        }
        while (FORM_KEY.consumeClick()) {
            if (minecraft.screen == null && minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(KaijuFormC2S.INSTANCE);
            }
        }
        PUPPETS.entrySet().removeIf(entry -> {
            Player player = level.getPlayerByUUID(entry.getKey());
            return player == null || !KaijuFormService.isTransformed(player) || entry.getValue().level() != level;
        });
        for (AbstractClientPlayer player : level.players()) {
            if (KaijuFormService.isTransformed(player)) {
                PUPPETS.computeIfAbsent(player.getUUID(), id -> new PuppetNo8(level, player)).follow(player);
            }
        }
        lookAtRampageTarget(minecraft.player);
    }

    /**
     * T-3: sem controle do corpo a camera vira sozinha para quem o servidor escolheu atacar (no maximo
     * {@link #RAMPAGE_TURN_DEGREES} por tick, para nao saltar). O golpe sai do olhar do servidor, que vira junto.
     */
    private static void lookAtRampageTarget(Player player) {
        if (!KaijuFormService.isRampaging(player)) {
            return;
        }
        Entity target = player.level().getEntity(player.getData(KN8Attachments.RAMPAGE_TARGET.get()));
        if (target == null) {
            return;
        }
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        float yawStep = Mth.clamp(Mth.wrapDegrees(yaw - player.getYRot()), -RAMPAGE_TURN_DEGREES,
                RAMPAGE_TURN_DEGREES);
        float pitchStep = Mth.clamp(pitch - player.getXRot(), -RAMPAGE_TURN_DEGREES, RAMPAGE_TURN_DEGREES);
        player.setYRot(player.getYRot() + yawStep);
        player.setXRot(Mth.clamp(player.getXRot() + pitchStep, -90.0F, 90.0F));
    }

    /** Jogador transformado: o fantoche do No. 8 no lugar do modelo do jogador (com o nome de quem e). */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !KaijuFormService.isTransformed(player)) {
            return;
        }
        event.setCanceled(true);
        if (player.isInvisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PuppetNo8 puppet = PUPPETS.computeIfAbsent(player.getUUID(),
                id -> new PuppetNo8((ClientLevel) player.level(), player));
        if (player != minecraft.player) {
            puppet.setCustomName(player.getDisplayName());
            puppet.setCustomNameVisible(true);
        }
        EntityRenderer<? super PuppetNo8> renderer = minecraft.getEntityRenderDispatcher().getRenderer(puppet);
        renderer.render(puppet, player.getYRot(), event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
    }

    /**
     * T-3: golpe do jogador transformado (animacao do servidor, {@code AnimTriggerS2C}) vira a animacao do No. 8 no
     * fantoche: soco do combo, soco pesado, Golpe no Chao (especial), investida (dash/esquiva). Em 1a pessoa o braco
     * balanca como o vanilla. Devolve se tratou (a PAL fica de fora).
     */
    public static boolean onAnimation(AbstractClientPlayer player, ResourceLocation animation, boolean stop) {
        if (!KaijuFormService.isTransformed(player)) {
            return false;
        }
        if (stop) {
            return true;
        }
        String path = animation.getPath();
        // COMBATE-7: outra tecnica dos punhos (special_<id>, soco de ruptura) toca a acao de mesmo nome do No. 8.
        int technique = path.lastIndexOf("special_");
        String name = technique >= 0 ? path.substring(technique + "special_".length())
                : path.endsWith("special") ? "ground_smash"
                : path.contains("heavy") || path.contains("charge") ? "heavy_punch"
                : path.contains("dash") || path.contains("dodge") ? "dash"
                : path.contains("light") || path.contains("shoot") ? "attack" : null;
        if (name == null) {
            return true;
        }
        PuppetNo8 puppet = PUPPETS.get(player.getUUID());
        if (puppet != null) {
            puppet.triggerAnim("action", name);
        }
        if (player == Minecraft.getInstance().player) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    /** T-3: sem controle do corpo o jogador nao anda nem pula pela propria vontade (o servidor o move). */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!KaijuFormService.isRampaging(event.getEntity())) {
            return;
        }
        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
    }

    /** Primeira pessoa: a mao vazia vira o braco do No. 8 (golpe e troca de item como o vanilla). */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !KaijuFormService.isTransformed(minecraft.player)) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND || minecraft.player.isInvisible()) {
            return;
        }
        MeshModel model = MeshModels.get(SPECIES).orElse(null);
        if (model == null || model.bone(ARM_BONES[0]) == null) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float swing = event.getSwingProgress();
        float equip = event.getEquipProgress();
        float root = Mth.sqrt(swing);
        // Mesma conta do ItemInHandRenderer#renderPlayerArm (mao direita), so com a malha no lugar do ModelPart.
        pose.translate(-0.3F * Mth.sin(root * Mth.PI) + 0.64F, 0.4F * Mth.sin(root * Mth.TWO_PI) - 0.6F
                - equip * 0.6F, -0.4F * Mth.sin(swing * Mth.PI) - 0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(root * Mth.PI) * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(swing * swing * Mth.PI) * -20.0F));
        pose.translate(-1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-135.0F));
        pose.translate(5.6F, 0.0F, 0.0F);
        // Ombro direito do jogador (ModelPart rightArm: -5, 2, 0 px; Y para baixo) e o braco da GeckoLib (Y para
        // cima, X invertido em relacao ao vanilla): vira de cabeca para baixo e poe o pivo do osso no ombro.
        pose.translate(-5.0F / 16.0F, 2.0F / 16.0F, 0.0F);
        pose.scale(-ARM_SCALE, -ARM_SCALE, ARM_SCALE);
        pose.translate(-ARM_PIVOT_X, -ARM_PIVOT_Y, -ARM_PIVOT_Z);
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(
                KaijuFormService.isRampaging(minecraft.player) ? RAMPAGE_TEXTURE : TEXTURE));
        for (String bone : ARM_BONES) {
            float[] mesh = model.bone(bone);
            if (mesh != null) {
                drawMesh(pose.last(), buffer, mesh, event.getPackedLight());
            }
        }
        pose.popPose();
    }

    private static void drawMesh(PoseStack.Pose pose, VertexConsumer buffer, float[] data, int light) {
        int stride = MeshModel.FLOATS_PER_VERTEX;
        for (int triangle = 0; triangle < data.length; triangle += stride * 3) {
            for (int corner = 0; corner < 4; corner++) {
                int i = triangle + Math.min(corner, 2) * stride;
                buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                        .setColor(WHITE)
                        .setUv(data[i + 3], data[i + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
            }
        }
    }
}
