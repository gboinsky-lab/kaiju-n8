// src/main/java/com/kn8/common/command/KN8Commands.java
package com.kn8.common.command;

import com.kn8.KN8Constants;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Raiz do comando {@code /kn8}. Todos os subcomandos exigem permissao de operador (nivel 2), como na Fase 4. */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class KN8Commands {

    static final int OPERATOR_LEVEL = 2;

    private KN8Commands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(KN8Constants.MOD_ID)
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(NetCommands.build())
                .then(DataCommands.build())
                .then(PowerCommands.power())
                .then(PowerCommands.release())
                .then(PowerCommands.heat())
                .then(PowerCommands.stamina())
                .then(PowerCommands.energy())
                .then(KaijuCommands.build())
                .then(AnimCommands.build())
                .then(DestructionCommands.build())
                .then(SoldierCommands.build())
                .then(CareerCommands.rank())
                .then(CareerCommands.merit())
                .then(CareerCommands.mission())
                .then(CareerCommands.boss())
                .then(InvasionCommands.invasion())
                .then(AuraCommands.aura())
                .then(DebugCommands.build()));
    }
}
