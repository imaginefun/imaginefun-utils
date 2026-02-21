package net.imaginefun.servers;

import net.imaginefun.ImaginefunUtilsConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

public class ServerListPopulator {

    public static void populate() {
        var serverList = new ServerList(Minecraft.getInstance());
        serverList.load();
        if (serverList.size() == 0) {
            var serverData = new ServerData(ImaginefunUtilsConstants.serverName, ImaginefunUtilsConstants.serverAddress, ServerData.Type.OTHER);
            serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            serverList.add(serverData, false);
            serverList.save();
        }
    }
}
