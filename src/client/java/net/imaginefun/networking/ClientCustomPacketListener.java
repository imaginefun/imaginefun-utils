package net.imaginefun.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public interface ClientCustomPacketListener {

    void handleGameTestAddMarker(GameTestAddMarkerPayload gameTestAddMarkerPayload, ClientPlayNetworking.Context context);

    void handlePlayerForceLook(PlayerForceLookPayload playerForceLookPayload, ClientPlayNetworking.Context context);

    void handleApiSession(ApiSessionPayload apiSessionPayload, ClientPlayNetworking.Context context);

    void handleRideStatus(RideStatusPayload rideStatusPayload, ClientPlayNetworking.Context context);

    void handleServerInfo(ServerInfoPayload serverInfoPayload, ClientPlayNetworking.Context context);

    void handleRideTimeline(RideTimelinePayload rideTimelinePayload, ClientPlayNetworking.Context context);

    void handleRideTimelineState(RideTimelineStatePayload rideTimelineStatePayload, ClientPlayNetworking.Context context);

    void handleClosedCaption(ClosedCaptionPayload closedCaptionPayload, ClientPlayNetworking.Context context);

    void handleRideTimelineClose(RideTimelineClosePayload rideTimelineClosePayload, ClientPlayNetworking.Context context);

    void handleOpenAdvancements(OpenAdvancementsPayload openAdvancementsPayload, ClientPlayNetworking.Context context);

    void handleOpenStats(OpenStatsPayload openStatsPayload, ClientPlayNetworking.Context context);

    void handleServerStats(ServerStatsPayload serverStatsPayload, ClientPlayNetworking.Context context);

    void handleServerStatsUpdate(ServerStatsUpdatePayload serverStatsUpdatePayload, ClientPlayNetworking.Context context);
}
