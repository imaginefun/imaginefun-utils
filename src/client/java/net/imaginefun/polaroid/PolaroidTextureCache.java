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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class PolaroidTextureCache {

    private static final String IMAGE_PATH = "/v1/photopass/worldshot/";
    private static final int MAX_TEXTURE_WIDTH = 520;
    private static final int MIP_LEVELS = 4;
    private static final int MAX_TEXTURES = 32;
    private static final long IDLE_EXPIRY_MS = 30_000L;
    private static final long RETRY_INTERVAL_MS = 5L * 60L * 1000L;
    private static final long SWEEP_INTERVAL_MS = 1_000L;
    private static final int HASH_BYTES = 16;
    private static final float TARGET_ASPECT = 13.0F / 12.0F;

    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final Set<String> VALIDATED_THIS_SESSION = ConcurrentHashMap.newKeySet();
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
        VALIDATED_THIS_SESSION.clear();
    }

    private static Entry startLoad(String imageId) {
        Entry entry = new Entry();
        entry.lastUsed = Util.getMillis();
        String hash = hash(imageId);
        Identifier textureId = Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, "polaroid/" + hash);
        Path cacheFile = cacheDirectory().resolve(hash);

        fetch(imageId, cacheFile)
            .thenApplyAsync(PolaroidTextureCache::decode, IO_EXECUTOR)
            .thenAccept(mipLevels -> Minecraft.getInstance().execute(() -> register(imageId, entry, textureId, mipLevels)))
            .exceptionally(throwable -> {
                entry.retryAt = Util.getMillis() + RETRY_INTERVAL_MS;
                ImagineFunUtils.LOGGER.warn("Failed to load polaroid image {}, retrying in {}s",
                    imageId, RETRY_INTERVAL_MS / 1000L, throwable);
                return null;
            });

        return entry;
    }

    private static NativeImage[] decode(byte[] bytes) {
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (decoded == null) {
            throw new IllegalStateException("Unsupported polaroid image format");
        }
        return buildMipChain(downscale(decoded));
    }

    private static BufferedImage downscale(BufferedImage source) {
        if (source.getWidth() <= MAX_TEXTURE_WIDTH) {
            return source;
        }
        int height = Math.max(1, Math.round(source.getHeight() * (float) MAX_TEXTURE_WIDTH / source.getWidth()));
        BufferedImage scaled = new BufferedImage(MAX_TEXTURE_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, MAX_TEXTURE_WIDTH, height, null);
        graphics.dispose();
        return scaled;
    }

    private static NativeImage[] buildMipChain(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        List<NativeImage> levels = new ArrayList<>();
        levels.add(toNativeImage(pixels, width, height));
        while (levels.size() < MIP_LEVELS && width > 1 && height > 1) {
            pixels = halve(pixels, width, height);
            width >>= 1;
            height >>= 1;
            levels.add(toNativeImage(pixels, width, height));
        }
        return levels.toArray(NativeImage[]::new);
    }

    private static NativeImage toNativeImage(int[] argb, int width, int height) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, argb[y * width + x] | 0xFF000000);
            }
        }
        return image;
    }

    private static int[] halve(int[] pixels, int width, int height) {
        int halfWidth = width >> 1;
        int halfHeight = height >> 1;
        int[] result = new int[halfWidth * halfHeight];
        for (int y = 0; y < halfHeight; y++) {
            int y0 = y * 2;
            int y1 = Math.min(y0 + 1, height - 1);
            for (int x = 0; x < halfWidth; x++) {
                int x0 = x * 2;
                int x1 = Math.min(x0 + 1, width - 1);
                result[y * halfWidth + x] = average(
                    pixels[y0 * width + x0], pixels[y0 * width + x1],
                    pixels[y1 * width + x0], pixels[y1 * width + x1]);
            }
        }
        return result;
    }

    private static int average(int a, int b, int c, int d) {
        int red = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF) + ((c >> 16) & 0xFF) + ((d >> 16) & 0xFF)) >> 2;
        int green = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF) + ((c >> 8) & 0xFF) + ((d >> 8) & 0xFF)) >> 2;
        int blue = ((a & 0xFF) + (b & 0xFF) + (c & 0xFF) + (d & 0xFF)) >> 2;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static CompletableFuture<byte[]> fetch(String imageId, Path cacheFile) {
        return CompletableFuture.supplyAsync(() -> readCached(cacheFile), IO_EXECUTOR)
            .thenCompose(cached -> {
                if (cached != null && VALIDATED_THIS_SESSION.contains(imageId)) {
                    return CompletableFuture.completedFuture(cached);
                }
                CompletableFuture<byte[]> downloaded = download(imageId, cacheFile);
                if (cached == null) {
                    return downloaded;
                }
                return downloaded.exceptionally(throwable -> {
                    ImagineFunUtils.LOGGER.warn("Using cached polaroid image {} after refresh failed", imageId, throwable);
                    return cached;
                });
            });
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
                VALIDATED_THIS_SESSION.add(imageId);
                return body;
            }, IO_EXECUTOR);
    }

    private static void register(String imageId, Entry entry, Identifier textureId, NativeImage[] mipLevels) {
        if (ENTRIES.get(imageId) != entry) {
            for (NativeImage level : mipLevels) {
                level.close();
            }
            return;
        }
        int width = mipLevels[0].getWidth();
        int height = mipLevels[0].getHeight();
        Minecraft.getInstance().getTextureManager()
            .register(textureId, new PolaroidTexture(textureId::toString, mipLevels));
        entry.image = centerCrop(textureId, width, height);
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
