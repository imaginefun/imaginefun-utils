package net.imaginefun.polaroid;

import java.util.function.Supplier;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.renderer.texture.AbstractTexture;

final class PolaroidTexture extends AbstractTexture {

    PolaroidTexture(Supplier<String> label, NativeImage[] mipLevels) {
        GpuDevice device = RenderSystem.getDevice();
        NativeImage base = mipLevels[0];
        this.texture = device.createTexture(
            label,
            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
            GpuFormat.RGBA8_UNORM,
            base.getWidth(),
            base.getHeight(),
            1,
            mipLevels.length);
        this.textureView = device.createTextureView(this.texture);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);

        CommandEncoder encoder = device.createCommandEncoder();
        for (int level = 0; level < mipLevels.length; level++) {
            encoder.writeToTexture(this.texture, mipLevels[level], level, 0, 0, 0);
            mipLevels[level].close();
        }
    }
}
