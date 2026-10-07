package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.10")
public record RideTimelineControlPayload(String rideId, int action, double value) implements CustomPacketPayload {

    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int SEEK = 2;
    public static final int STEP = 3;
    public static final int SPEED = 4;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "ride_timeline_control");
    public static final CustomPacketPayload.Type<RideTimelineControlPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RideTimelineControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RideTimelineControlPayload::rideId,
        ByteBufCodecs.VAR_INT, RideTimelineControlPayload::action,
        ByteBufCodecs.DOUBLE, RideTimelineControlPayload::value,
        RideTimelineControlPayload::new
    );

    @Override
    public CustomPacketPayload.Type<RideTimelineControlPayload> type() {
        return TYPE;
    }
}
