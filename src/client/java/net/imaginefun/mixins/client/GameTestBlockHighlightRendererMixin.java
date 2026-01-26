package net.imaginefun.mixins.client;

import java.util.Map;

import net.imaginefun.extensions.GameTestBlockHighlightRendererExtension;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.renderer.debug.GameTestBlockHighlightRenderer;
import net.minecraft.core.BlockPos;

@Mixin(GameTestBlockHighlightRenderer.class)
public abstract class GameTestBlockHighlightRendererMixin implements GameTestBlockHighlightRendererExtension {
    @Final
    @Shadow
    private Map<BlockPos, GameTestBlockHighlightRenderer.Marker> markers;

    @Override
    public void imaginefunutils$highlightPos(BlockPos blockPos, int color, String text, int durationMs) {
        this.markers.put(blockPos, new GameTestBlockHighlightRenderer.Marker(color, text, Util.getMillis() + durationMs));
    }
}
