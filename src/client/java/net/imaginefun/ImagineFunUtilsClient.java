package net.imaginefun;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.gui.ImagineFunButton;
import net.imaginefun.networking.ClientCustomPacketListener;
import net.imaginefun.networking.ClientCustomPacketListenerImpl;
import net.imaginefun.networking.HandshakePayload;
import net.imaginefun.servers.ServerListPopulator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

public class ImagineFunUtilsClient implements ClientModInitializer {

    private ClientCustomPacketListener clientCustomPacketListener;

	@Override
	public void onInitializeClient() {
        clientCustomPacketListener = new ClientCustomPacketListenerImpl();
		ServerListPopulator.populate();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String version = FabricLoader.getInstance()
				.getModContainer(ImagineFunUtils.MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
			ClientPlayNetworking.send(new HandshakePayload(version));
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof TitleScreen)) return;

			// Screens.getWidgets() is the Fabric API-provided modifiable widget list.
			// Adding to it is the correct way to register widgets from outside a Screen
			// subclass (addRenderableWidget is protected). This is the same approach
			// used by ModMenu.
			var widgets = Screens.getWidgets(screen);

			// Find the Singleplayer button dynamically so we anchor to its actual
			// position after all other mods (e.g. ModMenu) have already shifted things
			// in their own AFTER_INIT or @Inject TAIL handlers.
			int singleplayerIndex = -1;
			AbstractWidget singleplayerButton = null;
			String singleplayerText = Component.translatable("menu.singleplayer").getString();
			for (int i = 0; i < widgets.size(); i++) {
				AbstractWidget widget = widgets.get(i);
				if (singleplayerText.equals(widget.getMessage().getString())) {
					singleplayerIndex = i;
					singleplayerButton = widget;
					break;
				}
			}
			if (singleplayerButton == null) return;

			int y = singleplayerButton.getY() + 24;
			int x = screen.width / 2 - 100;

			for (AbstractWidget widget : widgets) {
				if (widget != singleplayerButton && widget.getY() >= y) {
					widget.setY(widget.getY() + 24);
				}
			}

			widgets.add(singleplayerIndex + 1, new ImagineFunButton(
				x, y, 200, 20,
				Component.literal("Join " + ImaginefunUtilsConstants.serverName),
				button -> {
					ServerData serverData = new ServerData(
						ImaginefunUtilsConstants.serverName,
						ImaginefunUtilsConstants.serverAddress,
						ServerData.Type.OTHER
					);
					serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
					ConnectScreen.startConnecting(screen, client, ServerAddress.parseString(ImaginefunUtilsConstants.serverAddress), serverData, false, null);
				}
			));
		});

	}

    public ClientCustomPacketListener getClientCustomPacketListener() {
        return clientCustomPacketListener;
    }
}
