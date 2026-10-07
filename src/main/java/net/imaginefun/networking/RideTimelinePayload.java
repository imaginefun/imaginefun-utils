package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

@Since("0.0.10")
public record RideTimelinePayload(
    String rideId,
    String displayName,
    int firstFrame,
    int lastFrame,
    int fps,
    boolean interpolated,
    List<Integer> triggerFrames
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "ride_timeline");
    public static final CustomPacketPayload.Type<RideTimelinePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RideTimelinePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RideTimelinePayload::rideId,
        ByteBufCodecs.STRING_UTF8, RideTimelinePayload::displayName,
        ByteBufCodecs.VAR_INT, RideTimelinePayload::firstFrame,
        ByteBufCodecs.VAR_INT, RideTimelinePayload::lastFrame,
        ByteBufCodecs.VAR_INT, RideTimelinePayload::fps,
        ByteBufCodecs.BOOL, RideTimelinePayload::interpolated,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), RideTimelinePayload::triggerFrames,
        RideTimelinePayload::new
    );

    @Override
    public CustomPacketPayload.Type<RideTimelinePayload> type() {
        return TYPE;
    }
}
