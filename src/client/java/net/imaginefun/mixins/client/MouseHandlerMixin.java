package net.imaginefun.mixins.client;

import net.imaginefun.camera.ForcedLookSmoother;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void imaginefunutils$applyForcedLook(CallbackInfo ci) {
        ForcedLookSmoother.onFrame();
    }
}
