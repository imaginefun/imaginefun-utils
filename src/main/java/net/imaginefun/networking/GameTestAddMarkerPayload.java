package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GameTestAddMarkerPayload(BlockPos pos, int color, String text, int duration) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "game_test_add_marker");
    public static final CustomPacketPayload.Type<GameTestAddMarkerPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, GameTestAddMarkerPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, GameTestAddMarkerPayload::pos,
        ByteBufCodecs.INT, GameTestAddMarkerPayload::color,
        ByteBufCodecs.STRING_UTF8, GameTestAddMarkerPayload::text,
        ByteBufCodecs.INT, GameTestAddMarkerPayload::duration,
        GameTestAddMarkerPayload::new
    );

    @Override
    public CustomPacketPayload.Type<GameTestAddMarkerPayload> type() {
        return TYPE;
    }
}
