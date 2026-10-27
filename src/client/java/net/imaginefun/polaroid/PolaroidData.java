package net.imaginefun.polaroid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PolaroidData {

    private static final String IMAGE_KEY = "polaroid_image";
    private static final String TIME_KEY = "polaroid_time";

    private PolaroidData() {
    }

    public static PolaroidPhoto read(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }

        String imageId = customData.tag.getStringOr(IMAGE_KEY, "");
        if (imageId.isEmpty()) {
            return null;
        }
        return new PolaroidPhoto(imageId, customData.tag.getLongOr(TIME_KEY, 0L));
    }
}
