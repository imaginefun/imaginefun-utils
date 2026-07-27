package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.8")
public record ServerInfoPayload(String serverId, String network, int protocolVersion) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "server_info");
    public static final CustomPacketPayload.Type<ServerInfoPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerInfoPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ServerInfoPayload::serverId,
        ByteBufCodecs.STRING_UTF8, ServerInfoPayload::network,
        ByteBufCodecs.VAR_INT, ServerInfoPayload::protocolVersion,
        ServerInfoPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ServerInfoPayload> type() {
        return TYPE;
    }
}
