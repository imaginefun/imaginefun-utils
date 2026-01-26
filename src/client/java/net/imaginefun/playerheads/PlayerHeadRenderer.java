package net.imaginefun.playerheads;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public class PlayerHeadRenderer {
    private static final int MARKER_R = 2;
    private static final int MARKER_G = 1;
    private static final int MARKER_B = 3;
    private static final int MARKER_X = 63;
    private static final int MARKER_Y = 0;

    private static final int CONTROL_X = 63;
    private static final int CONTROL_Y = 1;

    private static final String NEW_NAMESPACE = "processed_images";
    private static final Map<Identifier, Identifier> processedTextures = new HashMap<>();
    
    public static boolean render(
        Identifier skinTexture,
        PoseStack matrixStack,
        Direction direction,
        int light,
        float yaw
    ) {
        try {
            if (net.irisshaders.iris.api.v0.IrisApi.getInstance().isRenderingShadowPass()) {
                return false;
            }
        } catch (NoClassDefFoundError e) {
            // Iris not installed, continue normally
        }

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(skinTexture);

        NativeImage image = null;
        if (texture instanceof DynamicTexture dynamicTexture) {
            image = dynamicTexture.getPixels();
        } else {
            image = getTextureImageViaReflection(texture);
        }
        
        if(image == null) return false;

        int pixel = image.getPixel(MARKER_X, MARKER_Y);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = (pixel >> 0) & 0xFF;

        if (r != MARKER_R || g != MARKER_G || b != MARKER_B) {
            return false;
        }

        int controlPixel = image.getPixel(CONTROL_X, CONTROL_Y);
        int controlR = (controlPixel >> 16) & 0xFF;
        int controlG = (controlPixel >> 8) & 0xFF;
        int controlB = (controlPixel >> 0) & 0xFF;
        int controlA = (controlPixel >> 24) & 0xFF;

        renderCustomSkull(direction, light, yaw, matrixStack, skinTexture,
            image,
            controlA,
            controlR,
            controlG,
            controlB
        );
        return true;
    }

    public static Identifier getNewImage(Identifier original, NativeImage input) {
        if(processedTextures.containsKey(original)) {
            return processedTextures.get(original);
        }

        NativeImage output = new NativeImage(
            input.getWidth(),
            input.getHeight(),
            true // ensure RGBA
        );

        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int rgba = input.getPixel(x, y);

                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = (rgba >> 0) & 0xFF;

                int a = (r == 2 && g == 1 && b == 3) ? 0 : 255;

                int newRGBA =
                        (a << 24) |
                        (r << 16) |
                        (g << 8) |
                        b;
                output.setPixel(x, y, newRGBA);
            }
        }

        Identifier newId = Identifier.fromNamespaceAndPath(
            NEW_NAMESPACE,
            original.getNamespace() + "/" + original.getPath()
        );

        DynamicTexture texture = new DynamicTexture(newId::toString, output);
        Minecraft.getInstance().getTextureManager().register(newId, texture);
        processedTextures.put(original, newId);
        return newId;
    }

    /**
     * Renders a custom skull with the given texture and scale.
     * 
     * @param skullBlockEntityRenderState The render state for the skull block entity
     * @param matrixStack The matrix stack for transformations
     * @param skinTexture The texture identifier for the skin
     * @param scale The scale factor for the skull (based on alpha channel of marker pixel)
     */
    public static void renderCustomSkull(
        Direction direction,
        int light,
        float yaw,
        PoseStack matrixStack,
        Identifier skinTexture,
        NativeImage image,
        int controlA,
        int controlR,
        int controlG,
        int controlB
    ) {
        matrixStack.pushPose();

        if (direction == null) {
            matrixStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            matrixStack.translate(0.5F - direction.getStepX() * 0.25F, 0.25F, 0.5F - direction.getStepZ() * 0.25F);
        }
		matrixStack.scale(-1.1875F, -1.1875F, 1.1875F);
        matrixStack.mulPose(Axis.YP.rotationDegrees(yaw));
        matrixStack.translate(0.0F, -0.211F, -0.211F);

        int overlay = OverlayTexture.NO_OVERLAY;
        
        float scaleX = 0;
        float scaleY = 0;

        if(controlB != 0) {
            scaleX = (float)controlA / 64.0f;
            scaleY = (float)controlA / 64.0f;
        } else {
            scaleX = (float)controlR / 16.0F - 0.0625F;
            scaleY = (float)controlG / 16.0F - 0.0625F;
        }

        Identifier actualIdentifier = null;
        if(controlB != 0) {
            actualIdentifier = getNewImage(skinTexture, image);
        } else {
            actualIdentifier = skinTexture;
        }

        // UV coordinates - skip the leftmost pixel column (contains control data)
        float minU = 0.0F;
        float maxU = 63.0F / 64.0F; // stop one column short

        float z = 0.0f;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(actualIdentifier));

        Matrix4f matrix4f = matrixStack.last().pose();
        Pose pose = matrixStack.last();

        vertexConsumer.addVertex(matrix4f, -scaleX, -scaleY, z)
            .setColor(255, 255, 255, 255)
            .setUv(minU, 0)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, 0, -1);
        
        vertexConsumer.addVertex(matrix4f, scaleX, -scaleY, z)
            .setColor(255, 255, 255, 255)
            .setUv(maxU, 0)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, 0, -1);
        
        vertexConsumer.addVertex(matrix4f, scaleX, scaleY, z)
            .setColor(255, 255, 255, 255)
            .setUv(maxU, 1)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, 0, -1);
        
        vertexConsumer.addVertex(matrix4f, -scaleX, scaleY, z)
            .setColor(255, 255, 255, 255)
            .setUv(minU, 1)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, 0, -1);

        matrixStack.popPose();
    }

    private static NativeImage getTextureImageViaReflection(AbstractTexture texture) {
        try {
            Class<?> clazz = texture.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getType() == NativeImage.class) {
                        field.setAccessible(true);
                        NativeImage img = (NativeImage) field.get(texture);
                        if (img != null) {
                            return img;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return null;
    }
}
