package net.imaginefun.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.imaginefun.ImagineFunUtils;
import net.imaginefun.api.ImagineFunClientEvents;
import net.imaginefun.camera.ForcedLookSmoother;
import net.imaginefun.extensions.GameTestBlockHighlightRendererExtension;
import net.imaginefun.session.ApiSession;
import net.imaginefun.session.ServerSession;
import net.minecraft.client.Minecraft;

public class ClientCustomPacketListenerImpl implements ClientCustomPacketListener {

    public ClientCustomPacketListenerImpl() {
        ClientPlayNetworking.registerGlobalReceiver(GameTestAddMarkerPayload.TYPE, this::handleGameTestAddMarker);
        ClientPlayNetworking.registerGlobalReceiver(PlayerForceLookPayload.TYPE, this::handlePlayerForceLook);
        ClientPlayNetworking.registerGlobalReceiver(ApiSessionPayload.TYPE, this::handleApiSession);
        ClientPlayNetworking.registerGlobalReceiver(RideStatusPayload.TYPE, this::handleRideStatus);
        ClientPlayNetworking.registerGlobalReceiver(ServerInfoPayload.TYPE, this::handleServerInfo);
    }

    @Override
    public void handleGameTestAddMarker(GameTestAddMarkerPayload gameTestAddMarkerPayload, ClientPlayNetworking.Context context) {
        ((GameTestBlockHighlightRendererExtension) Minecraft.getInstance().levelExtractor.gameTestBlockHighlightRenderer).imaginefunutils$highlightPos(
                gameTestAddMarkerPayload.pos(),
                gameTestAddMarkerPayload.color(),
                gameTestAddMarkerPayload.text(),
                gameTestAddMarkerPayload.duration()
        );
    }

    @Override
    public void handlePlayerForceLook(PlayerForceLookPayload playerForceLookPayload, ClientPlayNetworking.Context context) {
        ForcedLookSmoother.onForcedLook(playerForceLookPayload.deltaYaw(), playerForceLookPayload.deltaPitch());
    }

    @Override
    public void handleApiSession(ApiSessionPayload apiSessionPayload, ClientPlayNetworking.Context context) {
        if (!ApiSession.isTrustedBaseUrl(apiSessionPayload.baseUrl())) {
            ImagineFunUtils.LOGGER.warn("Ignoring API session with untrusted base url {}", apiSessionPayload.baseUrl());
            return;
        }
        ApiSession.update(apiSessionPayload.token(), apiSessionPayload.baseUrl());
        ImagineFunClientEvents.SESSION_UPDATED.invoker().onSessionUpdated(apiSessionPayload);
    }

    @Override
    public void handleRideStatus(RideStatusPayload rideStatusPayload, ClientPlayNetworking.Context context) {
        ImagineFunClientEvents.RIDE_STATUS.invoker().onRideStatus(rideStatusPayload);
    }

    @Override
    public void handleServerInfo(ServerInfoPayload serverInfoPayload, ClientPlayNetworking.Context context) {
        ServerSession.update(serverInfoPayload.serverId(), serverInfoPayload.network(), serverInfoPayload.protocolVersion());
        ImagineFunClientEvents.SERVER_INFO.invoker().onServerInfo(serverInfoPayload);
    }
}
