package net.imaginefun.mixins.client;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.components.SubtitleOverlay;

@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayMixin {

    @Shadow
    private boolean isListening;

    @Shadow
    private List<SubtitleOverlay.Subtitle> subtitles;

    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/SubtitleOverlay;isListening:Z",
            opcode = Opcodes.GETFIELD,
            ordinal = 2))
    private boolean imaginefunutils$renderServerCaptions(SubtitleOverlay overlay) {
        return isListening || !subtitles.isEmpty();
    }
}
