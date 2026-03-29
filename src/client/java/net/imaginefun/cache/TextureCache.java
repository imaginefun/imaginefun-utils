package net.imaginefun.cache;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Permanent local cache for skin textures, stored as a single binary file.
 *
 * File format (append-friendly, no header):
 *   Sequence of entries, each:
 *     - hash length (2 bytes, unsigned short)
 *     - hash (UTF-8 string)
 *     - data length (4 bytes, int)
 *     - PNG data (raw bytes)
 *
 * On read, all entries are loaded into memory. On write, new entries are appended.
 */
public class TextureCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();
    private static Path cacheFile;
    private static boolean initialized = false;
    private static final Object writeLock = new Object();

    public static void init(Path gameDir) {
        Path cacheDir = gameDir.resolve("imaginefunutils");
        cacheFile = cacheDir.resolve("head_textures.dat");

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache directory", e);
            return;
        }

        if (Files.isRegularFile(cacheFile)) {
            try {
                loadFromFile();
            } catch (IOException e) {
                LOGGER.warn("Failed to load texture cache, starting fresh", e);
                cache.clear();
            }
        }

        initialized = true;
        LOGGER.info("Texture cache initialized with {} entries", cache.size());
    }

    public static boolean has(String hash) {
        return cache.containsKey(hash);
    }

    public static byte[] get(String hash) {
        return cache.get(hash);
    }

    public static void putAsync(String hash, byte[] data) {
        if (!initialized || cache.containsKey(hash)) {
            return;
        }
        cache.put(hash, data);

        // Append to file on a background thread to avoid blocking the caller
        Thread.ofVirtual().name("texture-cache-write").start(() -> {
            synchronized (writeLock) {
                try {
                    appendToFile(hash, data);
                } catch (IOException e) {
                    LOGGER.warn("Failed to persist texture {} to cache", hash, e);
                }
            }
        });
    }

    public static int size() {
        return cache.size();
    }

    private static void loadFromFile() throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(cacheFile)))) {
            while (true) {
                try {
                    int hashLen = in.readUnsignedShort();
                    byte[] hashBytes = new byte[hashLen];
                    in.readFully(hashBytes);
                    String hash = new String(hashBytes, java.nio.charset.StandardCharsets.UTF_8);

                    int dataLen = in.readInt();
                    byte[] data = new byte[dataLen];
                    in.readFully(data);

                    cache.put(hash, data);
                } catch (EOFException e) {
                    // Reached end of file (or partial entry from a crash) — stop reading
                    break;
                }
            }
        }
    }

    private static void appendToFile(String hash, byte[] data) throws IOException {
        byte[] hashBytes = hash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (OutputStream raw = Files.newOutputStream(cacheFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             DataOutputStream out = new DataOutputStream(raw)) {
            out.writeShort(hashBytes.length);
            out.write(hashBytes);
            out.writeInt(data.length);
            out.write(data);
        }
    }
}
