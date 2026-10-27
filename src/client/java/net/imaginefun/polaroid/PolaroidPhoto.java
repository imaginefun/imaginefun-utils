package net.imaginefun.polaroid;

import net.minecraft.util.Mth;

public record PolaroidPhoto(String imageId, long takenAtEpochMs) {

    private static final long DEVELOP_DURATION_MS = 60L * 60L * 1000L;

    public float development(long nowEpochMs) {
        if (takenAtEpochMs <= 0L) {
            return 1.0F;
        }
        return Mth.clamp((nowEpochMs - takenAtEpochMs) / (float) DEVELOP_DURATION_MS, 0.0F, 1.0F);
    }
}
