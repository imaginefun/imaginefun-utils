package net.imaginefun.polaroid;

import java.util.function.Consumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

public final class PolaroidPhotoRenderer implements SpecialModelRenderer<PolaroidPhoto> {

    public static final PolaroidPhotoRenderer INSTANCE = new PolaroidPhotoRenderer();

    private static final float MIN_X = 1.5F / 16.0F;
    private static final float MAX_X = 14.5F / 16.0F;
    private static final float MIN_Y = 5.0F / 16.0F;
    private static final float MAX_Y = 17.0F / 16.0F;
    private static final float DEPTH = 7.88F / 16.0F;

    private PolaroidPhotoRenderer() {
    }

    @Override
    public PolaroidPhoto extractArgument(ItemStack stack) {
        return PolaroidData.read(stack);
    }

    @Override
    public void submit(
        PolaroidPhoto photo,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        int overlay,
        boolean hasFoil,
        int outlineColor
    ) {
        PolaroidImage image = PolaroidTextureCache.get(photo.imageId());
        if (image == null) {
            return;
        }

        float development = photo.development(System.currentTimeMillis());
        if (development <= 0.0F) {
            return;
        }
        int alpha = Math.round(development * 255.0F);

        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityTranslucent(image.texture()),
            (pose, vertexConsumer) -> {
                vertex(vertexConsumer, pose, MIN_X, MIN_Y, image.maxU(), image.maxV(), light, alpha);
                vertex(vertexConsumer, pose, MAX_X, MIN_Y, image.minU(), image.maxV(), light, alpha);
                vertex(vertexConsumer, pose, MAX_X, MAX_Y, image.minU(), image.minV(), light, alpha);
                vertex(vertexConsumer, pose, MIN_X, MAX_Y, image.maxU(), image.minV(), light, alpha);
            }
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(MIN_X, MIN_Y, DEPTH));
        output.accept(new Vector3f(MAX_X, MAX_Y, DEPTH));
    }

    private static void vertex(
        VertexConsumer vertexConsumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float u,
        float v,
        int light,
        int alpha
    ) {
        vertexConsumer.addVertex(pose, x, y, DEPTH)
            .setColor(255, 255, 255, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
