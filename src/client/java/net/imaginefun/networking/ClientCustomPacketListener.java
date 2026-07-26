package net.imaginefun.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public interface ClientCustomPacketListener {

    void handleGameTestAddMarker(GameTestAddMarkerPayload gameTestAddMarkerPayload, ClientPlayNetworking.Context context);

    void handlePlayerForceLook(PlayerForceLookPayload playerForceLookPayload, ClientPlayNetworking.Context context);

    void handleApiSession(ApiSessionPayload apiSessionPayload, ClientPlayNetworking.Context context);

    void handleRideStatus(RideStatusPayload rideStatusPayload, ClientPlayNetworking.Context context);

    void handleServerInfo(ServerInfoPayload serverInfoPayload, ClientPlayNetworking.Context context);
}
