package net.imaginefun.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.imaginefun.extensions.GameTestBlockHighlightRendererExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientCustomPacketListenerImpl implements ClientCustomPacketListener {

    public ClientCustomPacketListenerImpl() {
        ClientPlayNetworking.registerGlobalReceiver(GameTestAddMarkerPayload.TYPE, this::handleGameTestAddMarker);
        ClientPlayNetworking.registerGlobalReceiver(PlayerForceLookPayload.TYPE, this::handlePlayerForceLook);
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

    @Override
    public void handlePlayerForceLook(PlayerForceLookPayload playerForceLookPayload, ClientPlayNetworking.Context context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float deltaYaw = playerForceLookPayload.deltaYaw();
        float deltaPitch = playerForceLookPayload.deltaPitch();

        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        player.setYRot(player.getYRot() + deltaYaw);
        player.setXRot(player.getXRot() + deltaPitch);
    }
}
