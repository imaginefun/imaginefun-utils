package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Since("0.0.13")
public record OpenAdvancementsPayload() implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "open_advancements");
    public static final CustomPacketPayload.Type<OpenAdvancementsPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdvancementsPayload> STREAM_CODEC = StreamCodec.unit(new OpenAdvancementsPayload());

    @Override
    public CustomPacketPayload.Type<OpenAdvancementsPayload> type() {
        return TYPE;
    }
}
