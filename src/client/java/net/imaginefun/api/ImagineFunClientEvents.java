package net.imaginefun.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.imaginefun.networking.ApiSessionPayload;
import net.imaginefun.networking.RideStatusPayload;
import net.imaginefun.networking.ServerInfoPayload;

public final class ImagineFunClientEvents {

    public interface RideStatus {
        void onRideStatus(RideStatusPayload payload);
    }

    public interface SessionUpdated {
        void onSessionUpdated(ApiSessionPayload payload);
    }

    public interface ServerInfo {
        void onServerInfo(ServerInfoPayload payload);
    }

    public static final Event<RideStatus> RIDE_STATUS = EventFactory.createArrayBacked(RideStatus.class,
        callbacks -> payload -> {
            for (RideStatus callback : callbacks) {
                callback.onRideStatus(payload);
            }
        });

    public static final Event<SessionUpdated> SESSION_UPDATED = EventFactory.createArrayBacked(SessionUpdated.class,
        callbacks -> payload -> {
            for (SessionUpdated callback : callbacks) {
                callback.onSessionUpdated(payload);
            }
        });

    public static final Event<ServerInfo> SERVER_INFO = EventFactory.createArrayBacked(ServerInfo.class,
        callbacks -> payload -> {
            for (ServerInfo callback : callbacks) {
                callback.onServerInfo(payload);
            }
        });

    private ImagineFunClientEvents() {
    }
}
