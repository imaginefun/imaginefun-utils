package net.imaginefun.session;

public final class ServerSession {

    private static volatile String serverId;
    private static volatile String network;
    private static volatile int protocolVersion;

    private ServerSession() {
    }

    public static boolean isConnected() {
        return serverId != null;
    }

    public static String getServerId() {
        return serverId;
    }

    public static String getNetwork() {
        return network;
    }

    public static int getProtocolVersion() {
        return protocolVersion;
    }

    public static void update(String newServerId, String newNetwork, int newProtocolVersion) {
        serverId = newServerId;
        network = newNetwork;
        protocolVersion = newProtocolVersion;
    }

    public static void clear() {
        serverId = null;
        network = null;
        protocolVersion = 0;
    }
}
