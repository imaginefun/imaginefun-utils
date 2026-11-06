package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.13")
public record OpenStatsPayload() implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "open_stats");
    public static final CustomPacketPayload.Type<OpenStatsPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenStatsPayload> STREAM_CODEC = StreamCodec.unit(new OpenStatsPayload());

    @Override
    public CustomPacketPayload.Type<OpenStatsPayload> type() {
        return TYPE;
    }
}
