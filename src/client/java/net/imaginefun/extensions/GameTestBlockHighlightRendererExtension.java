package net.imaginefun.extensions;

import net.minecraft.core.BlockPos;

public interface GameTestBlockHighlightRendererExtension {

    void imaginefunutils$highlightPos(BlockPos blockPos, int color, String text, int durationMs);
}
