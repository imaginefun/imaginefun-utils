package net.imaginefun.mixins.client;


import java.util.Map;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.imaginefun.playerheads.PlayerHeadRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;

import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
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

    @Unique
    private static Identifier getTextureFromRenderType(RenderType renderType) {
        if (renderType == null) {
            return null;
        }
        RenderTypeAccessor renderTypeAccessor = (RenderTypeAccessor) renderType;
        RenderSetup renderSetup = renderTypeAccessor.getState();
        if (renderSetup == null) {
            return null;
        }
        return getTextureLocation(renderSetup, "Sampler0");
    }

    /**
     * Caller 1: SkullBlockRenderer.submit (skull blocks placed in the world).
     *
     * In 1.21.11, submit passed state.direction and state.rotationDegrees to submitSkull,
     * which applied translate + scale(-1,-1,1) internally. In 26.1, those fields were replaced
     * by state.transformation, and submitSkull no longer applies any transforms.
     *
     * We inject at HEAD (before mulPose(transformation)), extract direction and yaw from
     * the transformation, and call PlayerHeadRenderer with the same interface as 1.21.11.
     * Cancelling submit prevents submitSkull from being called, so the submitSkull mixin
     * below won't double-fire.
     */
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
        Identifier texture = getTextureFromRenderType(state.renderType);
        if (texture == null) {
            return;
        }

        // In 26.1, state.direction/rotationDegrees were replaced by state.transformation.
        // Use Transformation's built-in decomposition to recover direction and yaw.
        // The transformation is constructed as T * R_Y(-yaw) * S(-1,-1,1).
        Transformation t = state.transformation;
        Vector3fc translation = t.translation();
        Direction direction;

        if (Math.abs(translation.y() - 0.25F) < 0.01F) {
            // Wall skull — direction from translation offsets
            direction = imaginefunutils$directionFromTranslation(translation.x(), translation.z());
        } else {
            // Ground skull
            direction = null;
        }

        // leftRotation is Axis.YP.rotationDegrees(-yaw), i.e. quaternion (0, sin(-yaw/2), 0, cos(-yaw/2)).
        // Extract yaw: for a pure Y-rotation quaternion, angle = 2*atan2(q.y, q.w), negate to undo the sign.
        Quaternionfc q = t.leftRotation();
        float yaw = -(float) Math.toDegrees(2.0 * Math.atan2(q.y(), q.w()));

        boolean success = PlayerHeadRenderer.render(
            texture,
            poseStack,
            direction,
            state.lightCoords,
            yaw
        );

        if (success) {
            ci.cancel();
        }
    }

    /**
     * Callers 2/3/4: CustomHeadLayer, PlayerHeadSpecialRenderer, SkullSpecialRenderer.
     *
     * In 1.21.11, these all passed direction=null and yaw=180.0F to submitSkull.
     * In 26.1, submitSkull lost those parameters but the callers' intent is unchanged.
     *
     * This injection only fires for callers 2/3/4 because when caller 1's submit mixin
     * succeeds, it cancels submit entirely (submitSkull is never called).
     */
    @Inject(
        method = "submitSkull(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onStaticRender(
        float animationValue,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        SkullModelBase model,
        RenderType renderType,
        int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay breakProgress,
        CallbackInfo ci
    ) {
        Identifier texture = getTextureFromRenderType(renderType);
        if (texture == null) {
            return;
        }

        // In 26.1, CustomHeadLayer changed two things vs 1.21.11:
        //   1. scale(1.1875, -1.1875, -1.1875) → scale(1.1875, 1.1875, 1.1875)
        //   2. Removed the translate(-0.5, 0, -0.5) that followed the scale
        // Additionally, 26.1's submitSkull no longer applies translate(0.5,0,0.5)
        // + scale(-1,-1,1) internally (those were in 1.21.11's submitSkull).
        //
        // Our renderCustomSkull was designed for the 1.21.11 poseStack state, which
        // had: scale(1.1875,-1.1875,-1.1875) * translate(-0.5,0,-0.5).
        // We restore that here by flipping Y/Z and adding back the translate.
        boolean fromCustomHeadLayer = imaginefunutils$isCalledFromCustomHeadLayer();
        if (fromCustomHeadLayer) {
            poseStack.pushPose();
            poseStack.scale(1.0F, -1.0F, -1.0F);
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }

        // In 1.21.11, callers 2/3/4 always passed direction=null, yaw=180.0F
        boolean success = PlayerHeadRenderer.render(
            texture,
            poseStack,
            null,
            lightCoords,
            180.0F
        );

        if (fromCustomHeadLayer) {
            poseStack.popPose();
        }

        if (success) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean imaginefunutils$isCalledFromCustomHeadLayer() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (e.getClassName().endsWith("CustomHeadLayer")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the wall skull direction from the translation offsets.
     * Wall translations: 0.5 - direction.getStepX() * 0.25, 0.25, 0.5 - direction.getStepZ() * 0.25
     */
    @Unique
    private static Direction imaginefunutils$directionFromTranslation(float tx, float tz) {
        // NORTH: stepX=0, stepZ=-1 -> tx=0.5, tz=0.75
        // SOUTH: stepX=0, stepZ=1  -> tx=0.5, tz=0.25
        // WEST:  stepX=-1, stepZ=0 -> tx=0.75, tz=0.5
        // EAST:  stepX=1, stepZ=0  -> tx=0.25, tz=0.5
        if (Math.abs(tz - 0.75F) < 0.01F) return Direction.NORTH;
        if (Math.abs(tz - 0.25F) < 0.01F) return Direction.SOUTH;
        if (Math.abs(tx - 0.75F) < 0.01F) return Direction.WEST;
        if (Math.abs(tx - 0.25F) < 0.01F) return Direction.EAST;
        return Direction.NORTH; // fallback
    }
}
