package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerForceLookPayload(float deltaYaw, float deltaPitch) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "player_force_look");
    public static final CustomPacketPayload.Type<PlayerForceLookPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerForceLookPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, PlayerForceLookPayload::deltaYaw,
        ByteBufCodecs.FLOAT, PlayerForceLookPayload::deltaPitch,
        PlayerForceLookPayload::new
    );

    @Override
    public CustomPacketPayload.Type<PlayerForceLookPayload> type() {
        return TYPE;
    }
}
