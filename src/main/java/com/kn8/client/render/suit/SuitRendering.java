// src/main/java/com/kn8/client/render/suit/SuitRendering.java
package com.kn8.client.render.suit;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.client.render.mesh.MeshModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Liga o traje 3D (0.6-C): camada {@link SuitLayer} nos dois modelos de jogador (bracos largos e finos) e o braco
 * do traje em primeira pessoa (mesma pose que o vanilla usa para desenhar a mao: setupAnim parado, xRot 0).
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class SuitRendering {

    private SuitRendering() {
    }

    /** Mod bus. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new SuitLayer(player));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        Optional<ResourceLocation> suit = SuitLayer.suitOf(player);
        if (suit.isEmpty() || player.isInvisible()
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player)
                        instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot = 0.0F;
        float[] mesh = MeshModels.get(suit.get()).orElseThrow().bone(right ? "right_arm" : "left_arm");
        SuitLayer.drawPart(event.getPoseStack(),
                event.getMultiBufferSource().getBuffer(SuitLayer.renderType(suit.get())), mesh, arm,
                event.getPackedLight(), OverlayTexture.NO_OVERLAY);
    }
}
