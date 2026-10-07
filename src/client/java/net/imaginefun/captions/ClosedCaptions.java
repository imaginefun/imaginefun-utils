package net.imaginefun.captions;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.imaginefun.networking.ClosedCaptionPayload;
import net.imaginefun.text.ComponentJson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class ClosedCaptions {

    private static final float RANGE = 100_000;
    private static final int MIN_DURATION_MS = 4000;
    private static final int READING_MS_PER_CHARACTER = 70;
    private static final int MAX_DURATION_MS = 20_000;
    private static final int MIN_LINE_WIDTH = 160;
    // straight above the listener never lines up with the camera's right vector, so vanilla draws no direction arrows
    private static final Vec3 OVERHEAD = new Vec3(0, 10_000, 0);

    private static final List<ActiveCaption> active = new ArrayList<>();

    private ClosedCaptions() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClosedCaptions::tick);
    }

    public static void show(ClosedCaptionPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Component text = ComponentJson.parse(payload.textJson());
        if (text == null) {
            return;
        }

        if (!minecraft.options.showSubtitles().get()) {
            minecraft.gui.hud.getChat().addClientSystemMessage(text);
            return;
        }

        Vec3 source = payload.positional() ? new Vec3(payload.x(), payload.y(), payload.z()) : null;
        int readingMs = Math.max(MIN_DURATION_MS, text.getString().length() * READING_MS_PER_CHARACTER);
        int durationMs = Math.min(MAX_DURATION_MS, Math.max(payload.durationMs(), readingMs));
        long expiresAt = System.currentTimeMillis() + durationMs;

        // vanilla stacks newer subtitles above older ones, so add the last line first to keep reading order top to bottom
        for (Component line : wrap(minecraft, text).reversed()) {
            SubtitleOverlay.Subtitle subtitle = findOrAdd(minecraft, line, locationFor(minecraft, source));
            active.removeIf(caption -> caption.subtitle == subtitle);
            active.add(new ActiveCaption(subtitle, source, expiresAt));
        }
    }

    public static void clear() {
        active.clear();
    }

    private static List<Component> wrap(Minecraft minecraft, Component text) {
        int maxWidth = Math.max(MIN_LINE_WIDTH, minecraft.getWindow().getGuiScaledWidth() / 3);
        List<Component> lines = new ArrayList<>();
        for (FormattedText line : minecraft.font.getSplitter().splitLines(text, maxWidth, Style.EMPTY)) {
            MutableComponent component = Component.empty();
            line.visit((style, part) -> {
                component.append(Component.literal(part).withStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            lines.add(component);
        }
        return lines;
    }

    private static SubtitleOverlay.Subtitle findOrAdd(Minecraft minecraft, Component text, Vec3 location) {
        List<SubtitleOverlay.Subtitle> subtitles = minecraft.gui.hud.subtitleOverlay.subtitles;
        for (SubtitleOverlay.Subtitle subtitle : subtitles) {
            if (subtitle.getText().equals(text)) {
                subtitle.refresh(location);
                return subtitle;
            }
        }
        SubtitleOverlay.Subtitle subtitle = new SubtitleOverlay.Subtitle(text, RANGE, location);
        subtitles.add(subtitle);
        return subtitle;
    }

    // vanilla fades a subtitle out a few seconds after its last refresh, so keep refreshing until the caption's own duration ends
    private static void tick(Minecraft minecraft) {
        if (active.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<ActiveCaption> iterator = active.iterator();
        while (iterator.hasNext()) {
            ActiveCaption caption = iterator.next();
            if (now >= caption.expiresAt || !minecraft.gui.hud.subtitleOverlay.subtitles.contains(caption.subtitle)) {
                iterator.remove();
                continue;
            }
            caption.subtitle.refresh(locationFor(minecraft, caption.source));
        }
    }

    private static Vec3 locationFor(Minecraft minecraft, Vec3 source) {
        if (source != null) {
            return source;
        }
        return minecraft.getSoundManager().getListenerTransform().position().add(OVERHEAD);
    }

    private record ActiveCaption(SubtitleOverlay.Subtitle subtitle, Vec3 source, long expiresAt) {
    }
}
