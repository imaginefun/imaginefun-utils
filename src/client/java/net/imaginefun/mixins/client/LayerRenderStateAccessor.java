package net.imaginefun.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import org.joml.Matrix4f;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {

    @Accessor("itemTransform")
    ItemTransform imaginefunutils$getItemTransform();

    @Accessor("localTransform")
    Matrix4f imaginefunutils$getLocalTransform();
}
