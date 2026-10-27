package net.imaginefun.polaroid;

import net.minecraft.resources.Identifier;

public record PolaroidImage(Identifier texture, float minU, float minV, float maxU, float maxV) {
}
