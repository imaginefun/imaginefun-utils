package net.imaginefun;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.networking.ClientCustomPacketListener;
import net.imaginefun.networking.ClientCustomPacketListenerImpl;
import net.imaginefun.networking.HandshakePayload;
import net.imaginefun.servers.ServerListPopulator;
import net.imaginefun.windowicon.DockIconHandler;
import net.minecraft.client.multiplayer.ServerData;

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

			ServerData server = client.getCurrentServer();
			if (server != null && server.ip != null
				&& server.ip.toLowerCase().endsWith(".imaginefun.net")) {
				DockIconHandler.apply();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DockIconHandler.reset());

	}

    public ClientCustomPacketListener getClientCustomPacketListener() {
        return clientCustomPacketListener;
    }
}
