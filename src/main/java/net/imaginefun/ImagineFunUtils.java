package net.imaginefun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.networking.GameTestAddMarkerPayload;
import net.imaginefun.networking.HandshakePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagineFunUtils implements ModInitializer {

	public static final String MOD_ID = "imaginefunutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(GameTestAddMarkerPayload.TYPE, GameTestAddMarkerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.STREAM_CODEC);

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
