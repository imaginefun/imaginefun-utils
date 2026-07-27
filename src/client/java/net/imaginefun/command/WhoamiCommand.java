package net.imaginefun.command;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.imaginefun.ImagineFunUtils;
import net.imaginefun.api.ImagineFunApi;
import net.imaginefun.session.ApiSession;
import net.minecraft.network.chat.Component;

public final class WhoamiCommand {

    private WhoamiCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("whoami").executes(context -> execute(context.getSource()))));
    }

    private static int execute(FabricClientCommandSource source) {
        if (!ApiSession.isActive()) {
            source.sendError(Component.literal("No active ImagineFun API session"));
            return 0;
        }

        ImagineFunApi.getSessionPlayer(ImagineFunUtils.MOD_ID).whenComplete((player, throwable) ->
            source.getClient().execute(() -> {
                if (throwable != null) {
                    source.sendError(Component.literal("Failed to fetch session info: " + throwable.getMessage()));
                    return;
                }
                source.sendFeedback(Component.literal("Username: " + player.username()));
                source.sendFeedback(Component.literal("UUID: " + player.uuid()));
                source.sendFeedback(Component.literal("Player ID: " + player.playerId()));
                source.sendFeedback(Component.literal("Session: " + player.sessionId()));
            }));

        return Command.SINGLE_SUCCESS;
    }
}
