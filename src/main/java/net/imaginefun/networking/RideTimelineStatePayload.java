package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.10")
public record RideTimelineStatePayload(String rideId, double frame, boolean playing, double speed) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "ride_timeline_state");
    public static final CustomPacketPayload.Type<RideTimelineStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RideTimelineStatePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RideTimelineStatePayload::rideId,
        ByteBufCodecs.DOUBLE, RideTimelineStatePayload::frame,
        ByteBufCodecs.BOOL, RideTimelineStatePayload::playing,
        ByteBufCodecs.DOUBLE, RideTimelineStatePayload::speed,
        RideTimelineStatePayload::new
    );

    @Override
    public CustomPacketPayload.Type<RideTimelineStatePayload> type() {
        return TYPE;
    }
}
