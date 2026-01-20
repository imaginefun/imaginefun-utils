package net.imaginefun.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;

@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Accessor("state")
    RenderSetup getState();
}
