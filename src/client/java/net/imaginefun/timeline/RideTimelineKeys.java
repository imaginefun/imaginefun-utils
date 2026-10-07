package net.imaginefun.timeline;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.imaginefun.ImagineFunUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class RideTimelineKeys {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ImagineFunUtils.MOD_ID, ImagineFunUtils.MOD_ID));

    public static final KeyMapping OPEN_TIMELINE = new KeyMapping("key.imaginefunutils.ride_timeline", InputConstants.KEY_J, CATEGORY);

    private RideTimelineKeys() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(OPEN_TIMELINE);
        ClientTickEvents.END_CLIENT_TICK.register(RideTimelineKeys::onTick);
    }

    private static void onTick(Minecraft minecraft) {
        while (OPEN_TIMELINE.consumeClick()) {
            if (RideTimeline.isActive() && minecraft.gui.screen() == null) {
                minecraft.gui.setScreen(new RideTimelineScreen());
            }
        }
    }
}
