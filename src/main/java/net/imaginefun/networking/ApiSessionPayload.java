package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.8")
public record ApiSessionPayload(String token, String baseUrl) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "api_session");
    public static final CustomPacketPayload.Type<ApiSessionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ApiSessionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ApiSessionPayload::token,
        ByteBufCodecs.STRING_UTF8, ApiSessionPayload::baseUrl,
        ApiSessionPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ApiSessionPayload> type() {
        return TYPE;
    }
}
