package com.kn8.client.combat;

import com.kn8.KN8Constants;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Corrida sem folego (Etapa 1 da 0.2). Quem decide e o servidor ({@code PowerService.sprint}, enviado em
 * {@code PowerView.winded}); aqui o cliente do dono so obedece: o movimento do jogador local e calculado no cliente,
 * entao sem isto ele continuaria correndo na propria tela.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class SprintLimiter {

    private SprintLimiter() {
    }

    /** Antes do tick do jogador: solta a tecla de correr para o vanilla nao comecar a corrida neste tick. */
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!winded(minecraft.player)) {
            return;
        }
        KeyMapping sprint = minecraft.options.keySprint;
        if (sprint.isDown()) {
            // No modo "alternar", setDown(true) inverte o estado (equivale a apertar de novo); no modo "segurar",
            // setDown(false) solta ate o proximo evento da tecla.
            sprint.setDown(minecraft.options.toggleSprint().get());
        }
    }

    /** Depois do tick: corta a corrida iniciada por toque duplo no "andar para a frente". */
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (winded(player) && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private static boolean winded(LocalPlayer player) {
        return player != null && !player.isCreative() && !player.isSpectator()
                && player.getData(KN8Attachments.POWER_VIEW).winded();
    }
}
