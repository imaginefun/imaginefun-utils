package net.imaginefun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.networking.ApiSessionPayload;
import net.imaginefun.networking.GameTestAddMarkerPayload;
import net.imaginefun.networking.HandshakePayload;
import net.imaginefun.networking.PlayerForceLookPayload;
import net.imaginefun.networking.RideStatusPayload;
import net.imaginefun.networking.ServerInfoPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagineFunUtils implements ModInitializer {

	public static final String MOD_ID = "imaginefunutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(GameTestAddMarkerPayload.TYPE, GameTestAddMarkerPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PlayerForceLookPayload.TYPE, PlayerForceLookPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ApiSessionPayload.TYPE, ApiSessionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RideStatusPayload.TYPE, RideStatusPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ServerInfoPayload.TYPE, ServerInfoPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.STREAM_CODEC);

		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(mod ->
            ResourceLoader.registerBuiltinPack(
                    Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID),
                    mod,
                    Component.literal("ImagineFunUtils Resource Pack"),
                    PackActivationType.ALWAYS_ENABLED
            )
		);
	}
}
