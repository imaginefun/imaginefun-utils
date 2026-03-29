package net.imaginefun.mixins.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.NativeImage;

import net.imaginefun.cache.TextureCache;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.util.FileUtil;

/**
 * Intercepts skin texture downloads to serve from and populate our permanent cache.
 *
 * HEAD: Before Minecraft checks its own file cache, pre-populate the file from our DB if available.
 * RETURN: After a successful load (from file or download), persist the file bytes to our DB.
 */
@Mixin(SkinTextureDownloader.class)
public class SkinTextureDownloaderMixin {

    @Inject(method = "downloadSkin", at = @At("HEAD"))
    private void beforeDownloadSkin(Path localCopy, String url, CallbackInfoReturnable<NativeImage> cir) {
        if (Files.isRegularFile(localCopy)) {
            // File already exists — Minecraft will load from it, nothing to do
            return;
        }

        String hash = localCopy.getFileName().toString();
        byte[] cached = TextureCache.get(hash);
        if (cached != null) {
            try {
                FileUtil.createDirectoriesSafe(localCopy.getParent());
                Files.write(localCopy, cached);
            } catch (IOException e) {
                // Failed to pre-populate — let normal download proceed
            }
        }
    }

    @Inject(method = "downloadSkin", at = @At("RETURN"))
    private void afterDownloadSkin(Path localCopy, String url, CallbackInfoReturnable<NativeImage> cir) {
        String hash = localCopy.getFileName().toString();
        if (TextureCache.has(hash)) {
            return;
        }

        if (Files.isRegularFile(localCopy)) {
            try {
                byte[] data = Files.readAllBytes(localCopy);
                TextureCache.putAsync(hash, data);
            } catch (IOException e) {
                // Failed to read — skip caching
            }
        }
    }
}
