package com.kn8.client.render;

import com.kn8.KN8Constants;
import com.kn8.common.evacuation.CivilianEntity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Civil da evacuacao (PM-10A): modelo do jogador (largo) com uma pele de roupa comum por variante, gerada por
 * {@code tools/art/gen_civilian_skins.py} [SUPOSICAO provisorio: o Miguel manda o modelo depois].
 */
public class CivilianRenderer extends HumanoidMobRenderer<CivilianEntity, PlayerModel<CivilianEntity>> {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[CivilianEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = KN8Constants.id("textures/entity/civilian/civilian_" + i + ".png");
        }
    }

    public CivilianRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CivilianEntity civilian) {
        return TEXTURES[Math.floorMod(civilian.variant(), TEXTURES.length)];
    }
}
