package net.imaginefun.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.imaginefun.polaroid.PolaroidData;
import net.imaginefun.polaroid.PolaroidPhoto;
import net.imaginefun.polaroid.PolaroidPhotoRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {

    @Inject(method = "appendItemLayers", at = @At("TAIL"))
    private void imaginefunutils$appendPolaroidPhoto(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemDisplayContext displayContext,
        Level level,
        ItemOwner owner,
        int seed,
        CallbackInfo ci
    ) {
        PolaroidPhoto photo = PolaroidData.read(stack);
        if (photo == null) {
            return;
        }

        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) renderState;
        if (accessor.imaginefunutils$getActiveLayerCount() == 0) {
            return;
        }

        LayerRenderStateAccessor frameLayer = (LayerRenderStateAccessor) accessor.imaginefunutils$getLayers()[0];

        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        layer.setItemTransform(frameLayer.imaginefunutils$getItemTransform());
        layer.setLocalTransform(frameLayer.imaginefunutils$getLocalTransform());
        layer.setupSpecialModel(PolaroidPhotoRenderer.INSTANCE, photo);
    }
}
