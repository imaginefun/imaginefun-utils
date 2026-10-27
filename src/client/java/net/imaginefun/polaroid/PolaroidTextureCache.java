package net.imaginefun.polaroid;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.ImagineFunUtils;
import net.imaginefun.api.ImagineFunApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class PolaroidTextureCache {

    private static final String IMAGE_PATH = "/v1/photopass/worldshot/";
    private static final int TEXTURE_WIDTH = 128;
    private static final int MAX_TEXTURES = 64;
    private static final long IDLE_EXPIRY_MS = 30_000L;
    private static final long RETRY_INTERVAL_MS = 5L * 60L * 1000L;
    private static final long SWEEP_INTERVAL_MS = 1_000L;
    private static final int HASH_BYTES = 16;
    private static final float TARGET_ASPECT = 13.0F / 12.0F;

    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static long lastSweep;

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(
        Thread.ofPlatform().name("ImagineFun-Polaroid-IO", 1).daemon().factory());

    private PolaroidTextureCache() {
    }

    private static final class Entry {
        volatile PolaroidImage image;
        volatile long lastUsed;
        volatile long retryAt;

        boolean failed() {
            return image == null && retryAt != 0L;
        }

        boolean shouldRetry(long now) {
            return failed() && now >= retryAt;
        }
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> expireIdle());
    }

    public static PolaroidImage get(String imageId) {
        long now = Util.getMillis();
        Entry entry = ENTRIES.compute(imageId, (id, existing) ->
            existing == null || existing.shouldRetry(now) ? startLoad(id) : existing);
        entry.lastUsed = now;
        return entry.image;
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        ENTRIES.values().forEach(entry -> {
            PolaroidImage image = entry.image;
            if (image != null) {
                minecraft.getTextureManager().release(image.texture());
            }
        });
        ENTRIES.clear();
    }

    private static Entry startLoad(String imageId) {
        Entry entry = new Entry();
        entry.lastUsed = Util.getMillis();
        String hash = hash(imageId);
        Identifier textureId = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "polaroid/" + hash);
        Path cacheFile = cacheDirectory().resolve(hash);

        fetch(imageId, cacheFile)
            .thenApplyAsync(PolaroidTextureCache::decode, IO_EXECUTOR)
            .thenAccept(image -> Minecraft.getInstance().execute(() -> register(imageId, entry, textureId, image)))
            .exceptionally(throwable -> {
                entry.retryAt = Util.getMillis() + RETRY_INTERVAL_MS;
                ImagineFunUtils.LOGGER.warn("Failed to load polaroid image {}, retrying in {}s",
                    imageId, RETRY_INTERVAL_MS / 1000L, throwable);
                return null;
            });

        return entry;
    }

    private static NativeImage decode(byte[] bytes) {
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (decoded == null) {
            throw new IllegalStateException("Unsupported polaroid image format");
        }

        BufferedImage scaled = downscale(decoded);
        int width = scaled.getWidth();
        int height = scaled.getHeight();
        int[] argb = scaled.getRGB(0, 0, width, height, null, 0, width);
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, argb[y * width + x] | 0xFF000000);
            }
        }
        return image;
    }

    private static BufferedImage downscale(BufferedImage source) {
        if (source.getWidth() <= TEXTURE_WIDTH) {
            return source;
        }
        int height = Math.max(1, Math.round(source.getHeight() * (float) TEXTURE_WIDTH / source.getWidth()));
        BufferedImage scaled = new BufferedImage(TEXTURE_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, TEXTURE_WIDTH, height, null);
        graphics.dispose();
        return scaled;
    }

    private static CompletableFuture<byte[]> fetch(String imageId, Path cacheFile) {
        return CompletableFuture.supplyAsync(() -> readCached(cacheFile), IO_EXECUTOR)
            .thenCompose(cached -> cached != null
                ? CompletableFuture.completedFuture(cached)
                : download(imageId, cacheFile));
    }

    private static CompletableFuture<byte[]> download(String imageId, Path cacheFile) {
        return ImagineFunApi.getBytes(ImagineFunUtils.MOD_ID, IMAGE_PATH + imageId)
            .thenApplyAsync(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException(
                        "ImagineFun API returned " + response.statusCode() + " for polaroid image " + imageId);
                }
                byte[] body = response.body();
                writeCached(cacheFile, body);
                return body;
            }, IO_EXECUTOR);
    }

    private static void register(String imageId, Entry entry, Identifier textureId, NativeImage image) {
        if (ENTRIES.get(imageId) != entry) {
            image.close();
            return;
        }
        Minecraft.getInstance().getTextureManager()
            .register(textureId, new DynamicTexture(textureId::toString, image));
        entry.image = centerCrop(textureId, image.getWidth(), image.getHeight());
        evictLeastRecentlyUsed();
    }

    private static void expireIdle() {
        long now = Util.getMillis();
        if (now - lastSweep < SWEEP_INTERVAL_MS) {
            return;
        }
        lastSweep = now;
        ENTRIES.entrySet().removeIf(candidate -> {
            Entry entry = candidate.getValue();
            if (now - entry.lastUsed < IDLE_EXPIRY_MS) {
                return false;
            }
            if (entry.image != null) {
                Minecraft.getInstance().getTextureManager().release(entry.image.texture());
                return true;
            }
            return entry.failed();
        });
    }

    private static PolaroidImage centerCrop(Identifier textureId, int width, int height) {
        float aspect = (float) width / (float) height;
        if (aspect > TARGET_ASPECT) {
            float inset = (1.0F - TARGET_ASPECT / aspect) / 2.0F;
            return new PolaroidImage(textureId, inset, 0.0F, 1.0F - inset, 1.0F);
        }
        float inset = (1.0F - aspect / TARGET_ASPECT) / 2.0F;
        return new PolaroidImage(textureId, 0.0F, inset, 1.0F, 1.0F - inset);
    }

    private static void evictLeastRecentlyUsed() {
        while (ENTRIES.size() > MAX_TEXTURES) {
            String oldestId = null;
            long oldestUse = Long.MAX_VALUE;
            for (Map.Entry<String, Entry> candidate : ENTRIES.entrySet()) {
                Entry entry = candidate.getValue();
                if (entry.image != null && entry.lastUsed < oldestUse) {
                    oldestUse = entry.lastUsed;
                    oldestId = candidate.getKey();
                }
            }

            if (oldestId == null) {
                return;
            }

            Entry evicted = ENTRIES.remove(oldestId);
            if (evicted != null && evicted.image != null) {
                Minecraft.getInstance().getTextureManager().release(evicted.image.texture());
            }
        }
    }

    private static byte[] readCached(Path cacheFile) {
        try {
            return Files.exists(cacheFile) ? Files.readAllBytes(cacheFile) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeCached(Path cacheFile, byte[] data) {
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, data);
        } catch (IOException e) {
            ImagineFunUtils.LOGGER.warn("Failed to cache polaroid image at {}", cacheFile, e);
        }
    }

    private static Path cacheDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve(ImagineFunUtils.MOD_ID).resolve("polaroids");
    }

    private static String hash(String imageId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(imageId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, HASH_BYTES);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
