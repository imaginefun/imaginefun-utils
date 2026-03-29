package net.imaginefun.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;

/**
 * Permanent local cache for skin textures using H2 embedded database.
 * Data lives on disk; only queried rows are loaded into memory.
 *
 * Supports location-based preloading: tracks which texture hashes appear
 * near which chunk coordinates so nearby textures can be pre-populated
 * into Minecraft's file cache before the render pipeline requests them.
 */
public class TextureCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Connection connection;
    private static boolean initialized = false;

    public static void init(Path gameDir) {
        Path cacheDir = gameDir.resolve("imaginefunutils");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache directory", e);
            return;
        }

        String dbPath = cacheDir.resolve("head_textures").toAbsolutePath().toString();
        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection("jdbc:h2:" + dbPath + ";MODE=MySQL", "sa", "");
            createTables();
            initialized = true;

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM textures")) {
                rs.next();
                LOGGER.info("Texture cache initialized with {} entries", rs.getInt(1));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize texture cache database", e);
        }
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.warn("Failed to close texture cache database", e);
            }
        }
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS textures (
                    hash VARCHAR(128) PRIMARY KEY,
                    png_data BLOB NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS texture_locations (
                    hash VARCHAR(128) NOT NULL,
                    world VARCHAR(256) NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    PRIMARY KEY (hash, world, chunk_x, chunk_z)
                )
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_location
                ON texture_locations (world, chunk_x, chunk_z)
            """);
        }
    }

    // --- Texture data ---

    public static boolean has(String hash) {
        if (!initialized) return false;
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM textures WHERE hash = ?")) {
            ps.setString(1, hash);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            LOGGER.warn("Failed to check texture cache for {}", hash, e);
            return false;
        }
    }

    public static byte[] get(String hash) {
        if (!initialized) return null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT png_data FROM textures WHERE hash = ?")) {
            ps.setString(1, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBytes("png_data");
            }
            return null;
        } catch (SQLException e) {
            LOGGER.warn("Failed to read texture {} from cache", hash, e);
            return null;
        }
    }

    public static void putAsync(String hash, byte[] data) {
        if (!initialized) return;
        Thread.ofVirtual().name("texture-cache-write").start(() -> put(hash, data));
    }

    private static synchronized void put(String hash, byte[] data) {
        try (PreparedStatement ps = connection.prepareStatement(
                "MERGE INTO textures (hash, png_data) KEY (hash) VALUES (?, ?)")) {
            ps.setString(1, hash);
            ps.setBytes(2, data);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("Failed to write texture {} to cache", hash, e);
        }
    }

    // --- Location tracking ---

    public static void recordLocationAsync(String hash, String world, int chunkX, int chunkZ) {
        if (!initialized) return;
        Thread.ofVirtual().name("texture-cache-location").start(() -> recordLocation(hash, world, chunkX, chunkZ));
    }

    private static synchronized void recordLocation(String hash, String world, int chunkX, int chunkZ) {
        try (PreparedStatement ps = connection.prepareStatement(
                "MERGE INTO texture_locations (hash, world, chunk_x, chunk_z) KEY (hash, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, hash);
            ps.setString(2, world);
            ps.setInt(3, chunkX);
            ps.setInt(4, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("Failed to record texture location", e);
        }
    }

    /**
     * Returns all texture hashes associated with chunks in the given region.
     * Used for preloading textures when the player approaches an area.
     */
    public static List<CachedTexture> getTexturesNear(String world, int chunkX, int chunkZ, int radius) {
        if (!initialized) return List.of();
        List<CachedTexture> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT DISTINCT t.hash, t.png_data
                FROM textures t
                JOIN texture_locations l ON t.hash = l.hash
                WHERE l.world = ?
                  AND l.chunk_x BETWEEN ? AND ?
                  AND l.chunk_z BETWEEN ? AND ?
            """)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX - radius);
            ps.setInt(3, chunkX + radius);
            ps.setInt(4, chunkZ - radius);
            ps.setInt(5, chunkZ + radius);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new CachedTexture(rs.getString("hash"), rs.getBytes("png_data")));
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to query textures near ({}, {})", chunkX, chunkZ, e);
        }
        return results;
    }

    public record CachedTexture(String hash, byte[] data) {}
}
