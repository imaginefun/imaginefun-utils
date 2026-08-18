package net.imaginefun.playerheads;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class PlayerHeadRenderer {
    private static final int MARKER_R = 2;
    private static final int MARKER_G = 1;
    private static final int MARKER_B = 3;
    private static final int MARKER_X = 63;
    private static final int MARKER_Y = 0;

    private static final int CONTROL_X = 63;
    private static final int CONTROL_Y = 1;

    private static final float HEAD_SCALE = 1.1875F;
    private static final float FACE_CENTER_Y = -0.25F;
    private static final float FACE_Z = -0.2506F;

    private static final String NEW_NAMESPACE = "processed_images";
    private static final Map<Identifier, Identifier> processedTextures = new HashMap<>();
    
    public static boolean render(
        Identifier skinTexture,
        PoseStack matrixStack,
        SubmitNodeCollector collector,
        int light
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

        renderCustomSkull(collector, light, matrixStack, skinTexture,
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

    public static void renderCustomSkull(
        SubmitNodeCollector collector,
        int light,
        PoseStack matrixStack,
        Identifier skinTexture,
        NativeImage image,
        int controlA,
        int controlR,
        int controlG,
        int controlB
    ) {
        int overlay = OverlayTexture.NO_OVERLAY;

        float scaleX;
        float scaleY;

        if(controlB != 0) {
            scaleX = (float)controlA / 64.0f;
            scaleY = (float)controlA / 64.0f;
        } else {
            scaleX = (float)controlR / 16.0F - 0.0625F;
            scaleY = (float)controlG / 16.0F - 0.0625F;
        }

        Identifier actualIdentifier = controlB != 0 ? getNewImage(skinTexture, image) : skinTexture;

        float minU = 0.0F;
        float maxU = 63.0F / 64.0F;

        float halfWidth = scaleX * HEAD_SCALE;
        float halfHeight = scaleY * HEAD_SCALE;
        float bottom = FACE_CENTER_Y - halfHeight;
        float top = FACE_CENTER_Y + halfHeight;

        collector.submitCustomGeometry(
            matrixStack,
            RenderTypes.entityCutout(actualIdentifier),
            (pose, vertexConsumer) -> {
                vertex(vertexConsumer, pose, -halfWidth, bottom, minU, 0, light, overlay);
                vertex(vertexConsumer, pose, halfWidth, bottom, maxU, 0, light, overlay);
                vertex(vertexConsumer, pose, halfWidth, top, maxU, 1, light, overlay);
                vertex(vertexConsumer, pose, -halfWidth, top, minU, 1, light, overlay);
            }
        );
    }

    private static void vertex(
        VertexConsumer vertexConsumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float u,
        float v,
        int light,
        int overlay
    ) {
        vertexConsumer.addVertex(pose, x, y, FACE_Z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, 0, -1);
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
