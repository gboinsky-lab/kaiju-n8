// src/main/java/com/kn8/client/combat/WeaponRecoil.java
package com.kn8.client.combat;

import com.kn8.KN8Constants;
import com.kn8.common.combat.CombatService;
import com.kn8.common.combat.WeaponHandling;
import com.kn8.common.data.def.WeaponProfileDef;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Coice da camera de quem atira (0.5.0-D, {@code recoil} do perfil da arma): sobe a mira e desvia um pouco para o
 * lado no tiro aceito pelo servidor e devolve parte da subida em {@code recover_ticks}. So a camera deste cliente;
 * o coice do corpo vai na animacao de tiro, que todos veem.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class WeaponRecoil {

    /** Parte da subida que volta sozinha (o resto o jogador corrige com o mouse). */
    private static final float RECOVER_FRACTION = 0.6F;

    private static float pendingRecover;
    private static int recoverTicksLeft;

    private WeaponRecoil() {
    }

    static void onShot() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        CombatService.heldWeapon(player).flatMap(weapon -> WeaponHandling.profile(weapon, true))
                .flatMap(WeaponProfileDef::recoil).ifPresent(recoil -> {
                    float yaw = (player.getRandom().nextFloat() * 2.0F - 1.0F) * recoil.yaw();
                    player.setXRot(player.getXRot() - recoil.pitch());
                    player.setYRot(player.getYRot() + yaw);
                    pendingRecover = recoil.pitch() * RECOVER_FRACTION;
                    recoverTicksLeft = Math.max(1, recoil.recoverTicks());
                });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || recoverTicksLeft <= 0) {
            return;
        }
        float step = pendingRecover / recoverTicksLeft;
        player.setXRot(player.getXRot() + step);
        pendingRecover -= step;
        recoverTicksLeft--;
    }
}
