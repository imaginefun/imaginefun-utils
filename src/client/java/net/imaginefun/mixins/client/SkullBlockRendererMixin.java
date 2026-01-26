package net.imaginefun.mixins.client;


import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.imaginefun.playerheads.PlayerHeadRenderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import net.minecraft.core.Direction;

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
        method = "submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onStaticRender(
        Direction facing,
        float yaw,
        float poweredTicks,
        PoseStack matrices,
        SubmitNodeCollector queue,
        int light,
        SkullModelBase model,
        RenderType renderLayer,
        int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        CallbackInfo ci
    ) {
        if (renderLayer == null) {
            return;
        }

        RenderTypeAccessor playerSkinCacheEntryAccessor = (RenderTypeAccessor) renderLayer;
        RenderSetup renderSetup = playerSkinCacheEntryAccessor.getState();
        if (renderSetup == null) {
            return;
        }

        Identifier texture = getTextureLocation(renderSetup, "Sampler0");
        if (texture == null) {
            return;
        }

        boolean success = PlayerHeadRenderer.render(
            texture,
            matrices,
            facing,
            light,
            yaw
        );

        if (success) {
            ci.cancel();
        }
    }
}
