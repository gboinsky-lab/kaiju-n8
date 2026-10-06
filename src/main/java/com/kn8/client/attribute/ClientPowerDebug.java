// src/main/java/com/kn8/client/attribute/ClientPowerDebug.java
package com.kn8.client.attribute;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerView;
import com.kn8.common.registry.KN8Attachments;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Diagnostico dos atributos no cliente (ate a HUD do M6):
 * <ul>
 *   <li>{@code /kn8client power}: a {@code PowerView} que ESTE cliente recebeu (deve bater com
 *   {@code /kn8 power});</li>
 *   <li>{@code /kn8client power <jogador>}: de outro jogador so a % efetiva publica; a visao privada deve ser "-".</li>
 * </ul>
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ClientPowerDebug {

    private ClientPowerDebug() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kn8client").then(Commands.literal("power")
                .executes(ClientPowerDebug::self)
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> other(ctx, StringArgumentType.getString(ctx, "player"))))));
    }

    private static int self(CommandContext<CommandSourceStack> ctx) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        PowerView view = player.getData(KN8Attachments.POWER_VIEW);
        ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.power.self", view.trained(),
                view.effective(), view.cap(), view.surge(),
                String.format("%.1f/%.1f", view.stamina(), view.maxStamina()), String.format("%.1f", view.heat()),
                Component.translatable("kn8.heat_stage." + view.heatStage()), String.format("%.1f", view.energy())),
                false);
        return view.effective();
    }

    private static int other(CommandContext<CommandSourceStack> ctx, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0;
        }
        for (AbstractClientPlayer candidate : minecraft.level.players()) {
            if (candidate.getGameProfile().getName().equalsIgnoreCase(name)) {
                Integer visual = candidate.getExistingDataOrNull(KN8Attachments.RELEASE_VISUAL);
                PowerView privateView = candidate.getExistingDataOrNull(KN8Attachments.POWER_VIEW);
                ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.power.other",
                        candidate.getName(), visual == null ? "-" : visual.toString(),
                        privateView == null ? "-" : privateView.toString()), false);
                return 1;
            }
        }
        ctx.getSource().sendFailure(Component.translatable("kn8.client.net.player_not_found", name));
        return 0;
    }
}
