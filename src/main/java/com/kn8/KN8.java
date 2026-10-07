// src/main/java/com/kn8/KN8.java
package com.kn8;

import com.kn8.common.config.ClientConfig;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuSpawning;
import com.kn8.common.network.KN8Network;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.AttributeLimits;
import com.kn8.common.registry.KN8Registration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Ponto de entrada comum (cliente e servidor). So registra coisas; nenhuma logica de jogo mora aqui.
 */
@Mod(KN8Constants.MOD_ID)
public final class KN8 {

    private static final String SERVER_CONFIG_FILE = KN8Constants.MOD_ID + "-server.toml";
    private static final String CLIENT_CONFIG_FILE = KN8Constants.MOD_ID + "-client.toml";

    public KN8(IEventBus modEventBus, ModContainer modContainer) {
        KN8Registration.register(modEventBus);
        // SERVER: gerado em <instancia>/config/ (world/serverconfig/ so serve para override por mundo) e sincronizado
        // com os clientes. CLIENT: so na maquina do jogador (ignorado no servidor dedicado).
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, SERVER_CONFIG_FILE);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, CLIENT_CONFIG_FILE);
        modEventBus.addListener(KN8Network::register);
        modEventBus.addListener(KN8Entities::registerAttributes);
        modEventBus.addListener(KaijuSpawning::registerPlacements);
        KN8Network.registerPrivateChannels();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 0.6-D: kaiju com mais de 1024 de vida (No. 10, chefes); ver AttributeLimits.
        AttributeLimits.raiseMaxHealthCap();
        KN8Constants.LOGGER.info("[kn8] Setup comum concluido.");
    }
}
