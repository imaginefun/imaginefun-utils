package net.imaginefun.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.imaginefun.extensions.GameTestBlockHighlightRendererExtension;
import net.minecraft.client.Minecraft;

public class ClientCustomPacketListenerImpl implements ClientCustomPacketListener {

    public ClientCustomPacketListenerImpl() {
        ClientPlayNetworking.registerGlobalReceiver(GameTestAddMarkerPayload.TYPE, this::handleGameTestAddMarker);
    }

    @Override
    public void handleGameTestAddMarker(GameTestAddMarkerPayload gameTestAddMarkerPayload, ClientPlayNetworking.Context context) {
        ((GameTestBlockHighlightRendererExtension) Minecraft.getInstance().levelRenderer.gameTestBlockHighlightRenderer).imaginefunutils$highlightPos(
                gameTestAddMarkerPayload.pos(),
                gameTestAddMarkerPayload.color(),
                gameTestAddMarkerPayload.text(),
                gameTestAddMarkerPayload.duration()
        );
    }
}
