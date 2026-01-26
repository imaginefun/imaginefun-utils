package net.imaginefun;

import net.fabricmc.api.ClientModInitializer;
import net.imaginefun.networking.ClientCustomPacketListener;
import net.imaginefun.networking.ClientCustomPacketListenerImpl;
import net.imaginefun.servers.ServerListPopulator;

public class ImagineFunUtilsClient implements ClientModInitializer {

    private ClientCustomPacketListener clientCustomPacketListener;

	@Override
	public void onInitializeClient() {
        clientCustomPacketListener = new ClientCustomPacketListenerImpl();
		ServerListPopulator.populate();
	}

    public ClientCustomPacketListener getClientCustomPacketListener() {
        return clientCustomPacketListener;
    }
}
