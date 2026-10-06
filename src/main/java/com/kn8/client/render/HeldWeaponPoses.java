package com.kn8.client.render;

import com.kn8.common.registry.KN8Items;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Pose dos bracos do jogador segurando arma de fogo (0.1-B, teste manual): rifle e pistola usam a pose vanilla de
 * besta carregada ({@code CROSSBOW_HOLD}: os dois bracos para a frente, seguindo a mira). O modelo da arma e montado
 * com o cano ao longo do braco ({@code models/item/<arma>.json}, display "thirdperson_*"), entao a arma aponta para
 * onde o jogador olha. As animacoes da PAL (tiro, etc.) continuam por cima quando tocam.
 */
public final class HeldWeaponPoses {

    private static final IClientItemExtensions FIREARM = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    };

    private HeldWeaponPoses() {
    }

    /** Mod bus. */
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(FIREARM, KN8Items.RIFLE.get(), KN8Items.PISTOL.get());
    }
}
