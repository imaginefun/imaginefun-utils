package net.imaginefun.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public interface ClientCustomPacketListener {

    void handleGameTestAddMarker(GameTestAddMarkerPayload gameTestAddMarkerPayload, ClientPlayNetworking.Context context);
}
