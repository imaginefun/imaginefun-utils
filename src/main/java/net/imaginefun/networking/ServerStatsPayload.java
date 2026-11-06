package net.imaginefun.networking;

import net.imaginefun.ImagineFunUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Since("0.0.13")
public record ServerStatsPayload(List<Category> categories, List<Stat> stats) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "server_stats");
    public static final CustomPacketPayload.Type<ServerStatsPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public record Category(String id, String titleJson) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Category> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Category::id,
            ByteBufCodecs.STRING_UTF8, Category::titleJson,
            Category::new
        );
    }

    public record Stat(
        String id,
        String categoryId,
        String titleJson,
        long value,
        String displayValue,
        ItemStack icon
    ) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Stat> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Stat::id,
            ByteBufCodecs.STRING_UTF8, Stat::categoryId,
            ByteBufCodecs.STRING_UTF8, Stat::titleJson,
            ByteBufCodecs.VAR_LONG, Stat::value,
            ByteBufCodecs.STRING_UTF8, Stat::displayValue,
            ItemStack.OPTIONAL_STREAM_CODEC, Stat::icon,
            Stat::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerStatsPayload> STREAM_CODEC = StreamCodec.composite(
        Category.STREAM_CODEC.apply(ByteBufCodecs.list()), ServerStatsPayload::categories,
        Stat.STREAM_CODEC.apply(ByteBufCodecs.list()), ServerStatsPayload::stats,
        ServerStatsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ServerStatsPayload> type() {
        return TYPE;
    }
}
