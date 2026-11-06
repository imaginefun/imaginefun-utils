package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

@Since("0.0.13")
public record ServerStatsUpdatePayload(List<Update> updates) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "server_stats_update");
    public static final CustomPacketPayload.Type<ServerStatsUpdatePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public record Update(String id, long value, String displayValue) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Update> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Update::id,
            ByteBufCodecs.VAR_LONG, Update::value,
            ByteBufCodecs.STRING_UTF8, Update::displayValue,
            Update::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerStatsUpdatePayload> STREAM_CODEC = StreamCodec.composite(
        Update.STREAM_CODEC.apply(ByteBufCodecs.list()), ServerStatsUpdatePayload::updates,
        ServerStatsUpdatePayload::new
    );

    @Override
    public CustomPacketPayload.Type<ServerStatsUpdatePayload> type() {
        return TYPE;
    }
}
