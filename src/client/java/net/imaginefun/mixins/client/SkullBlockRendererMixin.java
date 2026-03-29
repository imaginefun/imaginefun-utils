package net.imaginefun.mixins.client;


import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.imaginefun.playerheads.PlayerHeadRenderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {
    @Unique
    private static Identifier getTextureLocation(RenderSetup renderSetup, String textureKey) {
        Map<String, ?> textures = ((RenderSetupAccessor) (Object) renderSetup).imaginefunutils$getTextures();
        Object textureBinding = textures.get(textureKey);
        if (textureBinding == null) {
            return null;
        }

        return ((TextureBindingAccessor) textureBinding).invokeLocation();
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSubmit(
        SkullBlockRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        if (state.renderType == null) {
            return;
        }

        RenderTypeAccessor renderTypeAccessor = (RenderTypeAccessor) state.renderType;
        RenderSetup renderSetup = renderTypeAccessor.getState();
        if (renderSetup == null) {
            return;
        }

        Identifier texture = getTextureLocation(renderSetup, "Sampler0");
        if (texture == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(state.transformation);

        boolean success = PlayerHeadRenderer.render(
            texture,
            poseStack,
            state.lightCoords
        );

        poseStack.popPose();

        if (success) {
            ci.cancel();
        }
    }
}
