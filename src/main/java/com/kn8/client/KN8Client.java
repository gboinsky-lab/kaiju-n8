// src/main/java/com/kn8/client/KN8Client.java
package com.kn8.client;

import com.kn8.KN8Constants;
import com.kn8.client.anim.PlayerAnimations;
import com.kn8.client.combat.CombatFeedback;
import com.kn8.client.combat.CombatInput;
import com.kn8.client.hud.KN8Hud;
import com.kn8.client.hud.KaijuHealthBar;
import com.kn8.client.net.ClientNetDebug;
import com.kn8.client.render.CarcassRenderer;
import com.kn8.client.render.HeldWeaponPoses;
import com.kn8.client.render.KaijuRenderer;
import com.kn8.client.render.SoldierRenderer;
import com.kn8.client.render.mesh.MeshModels;
import com.kn8.client.vfx.VfxEffects;
import com.kn8.common.registry.KN8Entities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Ponto de entrada so do cliente. Com {@code dist = Dist.CLIENT} esta classe nunca carrega no servidor dedicado,
 * entao pode referenciar classes de cliente com seguranca. Codigo comum nunca referencia este pacote.
 */
@Mod(value = KN8Constants.MOD_ID, dist = Dist.CLIENT)
public final class KN8Client {

    public KN8Client(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::clientSetup);
        // Tela de configuracao padrao do NeoForge (botao "Config" na lista de mods); edita client e server.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // Handlers S2C de cliente instalados na ponte comum (KN8ClientHooks).
        ClientNetDebug.install();
        CombatFeedback.install();
        VfxEffects.install();
        // M6: HUD de poder.
        modEventBus.addListener(KN8Hud::register);
        // 0.1-B: barra de vida do kaiju mirado.
        modEventBus.addListener(KaijuHealthBar::register);
        // M7a: renderers de kaiju.
        modEventBus.addListener(KN8Client::registerRenderers);
        // Etapa A/C: malhas do Meshy presas aos ossos (cache limpo no F3+T).
        modEventBus.addListener(MeshModels::registerReloadListener);
        // 0.1-B: pose de mira (dois bracos) ao segurar rifle/pistola.
        modEventBus.addListener(HeldWeaponPoses::register);
        // M9: camada de animacao do jogador na PAL.
        modEventBus.addListener(PlayerAnimations::onClientSetup);
        // M10: teclas de combate (bloqueio, esquiva).
        modEventBus.addListener(CombatInput::registerKeys);
    }

    /** Um renderer GeckoLib por especie; a sombra acompanha mais ou menos a largura da hitbox registrada. */
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        KN8Entities.KAIJU.forEach(type -> event.registerEntityRenderer(type.get(),
                context -> new KaijuRenderer(context, type.getId(), type.get().getWidth() / 2.0F)));
        event.registerEntityRenderer(KN8Entities.CARCASS.get(), CarcassRenderer::new);
        event.registerEntityRenderer(KN8Entities.SOLDIER.get(), SoldierRenderer::new);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        KN8Constants.LOGGER.info("[kn8] Setup do cliente concluido.");
    }
}
