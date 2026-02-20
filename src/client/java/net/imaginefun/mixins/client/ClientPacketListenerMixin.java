package net.imaginefun.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private static boolean setValuesFromPositionPacket(PositionMoveRotation change, Set<Relative> relatives, Entity entity, boolean bl) {
        return true;
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void onHandleMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.isPassenger()) {
            ci.cancel();
            setValuesFromPositionPacket(packet.change(), packet.relatives(), player, false);
        }
    }
}
