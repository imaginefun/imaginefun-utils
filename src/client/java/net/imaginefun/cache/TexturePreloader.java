package net.imaginefun.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;

/**
 * Pre-populates Minecraft's skin file cache with textures from our DB
 * for chunks near the player's current position.
 */
public class TexturePreloader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PRELOAD_RADIUS = 4; // chunks

    private static int lastChunkX = Integer.MIN_VALUE;
    private static int lastChunkZ = Integer.MIN_VALUE;
    private static String lastWorld = "";

    /**
     * Called each client tick. Checks if the player has moved to a new chunk
     * and triggers preloading if so.
     */
    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        int chunkX = client.player.getBlockX() >> 4;
        int chunkZ = client.player.getBlockZ() >> 4;
        String world = client.level.dimension().identifier().toString();

        if (chunkX == lastChunkX && chunkZ == lastChunkZ && world.equals(lastWorld)) {
            return;
        }

        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
        lastWorld = world;

        Thread.ofVirtual().name("texture-preloader").start(() -> preload(world, chunkX, chunkZ));
    }

    private static void preload(String world, int chunkX, int chunkZ) {
        List<TextureCache.CachedTexture> textures = TextureCache.getTexturesNear(world, chunkX, chunkZ, PRELOAD_RADIUS);
        if (textures.isEmpty()) {
            return;
        }

        Path skinsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("assets").resolve("skins");
        int preloaded = 0;

        for (TextureCache.CachedTexture texture : textures) {
            String hash = texture.hash();
            Path dir = skinsDir.resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx");
            Path file = dir.resolve(hash);

            if (Files.isRegularFile(file)) {
                continue; // already in Minecraft's cache
            }

            try {
                FileUtil.createDirectoriesSafe(dir);
                Files.write(file, texture.data());
                preloaded++;
            } catch (IOException e) {
                // Skip this texture
            }
        }

        if (preloaded > 0) {
            LOGGER.debug("Preloaded {} textures for chunks near ({}, {})", preloaded, chunkX, chunkZ);
        }
    }

    public static void reset() {
        lastChunkX = Integer.MIN_VALUE;
        lastChunkZ = Integer.MIN_VALUE;
        lastWorld = "";
    }
}
