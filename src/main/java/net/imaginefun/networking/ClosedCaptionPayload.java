package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.10")
public record ClosedCaptionPayload(
    String textJson,
    int durationMs,
    boolean positional,
    double x,
    double y,
    double z
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "closed_caption");
    public static final CustomPacketPayload.Type<ClosedCaptionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClosedCaptionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ClosedCaptionPayload::textJson,
        ByteBufCodecs.VAR_INT, ClosedCaptionPayload::durationMs,
        ByteBufCodecs.BOOL, ClosedCaptionPayload::positional,
        ByteBufCodecs.DOUBLE, ClosedCaptionPayload::x,
        ByteBufCodecs.DOUBLE, ClosedCaptionPayload::y,
        ByteBufCodecs.DOUBLE, ClosedCaptionPayload::z,
        ClosedCaptionPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ClosedCaptionPayload> type() {
        return TYPE;
    }
}
