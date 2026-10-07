package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.10")
public record RideTimelineClosePayload(String rideId) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "ride_timeline_close");
    public static final CustomPacketPayload.Type<RideTimelineClosePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RideTimelineClosePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RideTimelineClosePayload::rideId,
        RideTimelineClosePayload::new
    );

    @Override
    public CustomPacketPayload.Type<RideTimelineClosePayload> type() {
        return TYPE;
    }
}
