package net.imaginefun;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.cache.TextureCache;
import net.imaginefun.networking.ClientCustomPacketListener;
import net.imaginefun.networking.ClientCustomPacketListenerImpl;
import net.imaginefun.networking.HandshakePayload;
import net.imaginefun.servers.ServerListPopulator;

public class ImagineFunUtilsClient implements ClientModInitializer {

    private ClientCustomPacketListener clientCustomPacketListener;

	@Override
	public void onInitializeClient() {
        clientCustomPacketListener = new ClientCustomPacketListenerImpl();
		TextureCache.init(FabricLoader.getInstance().getGameDir());
		ServerListPopulator.populate();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String version = FabricLoader.getInstance()
				.getModContainer(ImagineFunUtils.MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
			ClientPlayNetworking.send(new HandshakePayload(version));
		});

	}

    public ClientCustomPacketListener getClientCustomPacketListener() {
        return clientCustomPacketListener;
    }
}
