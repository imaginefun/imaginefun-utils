package net.imaginefun.text;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.imaginefun.ImagineFunUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public final class ComponentJson {

    private ComponentJson() {
    }

    public static Component parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            var ops = minecraft.level != null
                ? minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE)
                : JsonOps.INSTANCE;
            return ComponentSerialization.CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        } catch (RuntimeException e) {
            ImagineFunUtils.LOGGER.warn("Ignoring invalid text component {}", json, e);
            return null;
        }
    }
}
