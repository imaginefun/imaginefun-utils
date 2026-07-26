package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.8")
public record RideStatusPayload(String rideId, String displayName, boolean riding, long startedAtEpochMs, long durationMs) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "ride_status");
    public static final CustomPacketPayload.Type<RideStatusPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RideStatusPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RideStatusPayload::rideId,
        ByteBufCodecs.STRING_UTF8, RideStatusPayload::displayName,
        ByteBufCodecs.BOOL, RideStatusPayload::riding,
        ByteBufCodecs.VAR_LONG, RideStatusPayload::startedAtEpochMs,
        ByteBufCodecs.VAR_LONG, RideStatusPayload::durationMs,
        RideStatusPayload::new
    );

    @Override
    public CustomPacketPayload.Type<RideStatusPayload> type() {
        return TYPE;
    }
}
