package net.imaginefun.timeline;

import net.imaginefun.networking.RideTimelinePayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RideTimelineOverlay {

    private static final int MARGIN = 10;
    private static final int HEIGHT = 20;
    private static final int GAP = 2;
    private static final int SPEED_WIDTH = 100;
    private static final int TIMELINE_BORDER = 3;
    private static final int MARKER_HIT_RADIUS = 3;
    private static final int MIN_MAJOR_TICK_SPACING = 40;
    private static final int[] TICK_INTERVALS_SECONDS = {1, 2, 5, 10, 15, 30, 60, 120, 300, 600};
    private static final double[] INTERPOLATED_SPEEDS = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1.25, 1.5, 1.75, 2, 2.5, 3, 4};
    private static final double[] FIXED_SPEEDS = {1, 2, 3, 4};

    private static final int OUTLINE = 0xFF000000;
    private static final int FACE = 0xD0383838;
    private static final int FACE_HOVERED = 0xD0505050;
    private static final int HIGHLIGHT = 0x50FFFFFF;
    private static final int TRACK = 0xD0181818;
    private static final int PLAYED = 0x704F88DE;
    private static final int TICK = 0x90FFFFFF;
    private static final int MARKER = 0xFFEE609C;
    private static final int MARKER_HOVERED = 0xFFFFD166;
    private static final int PLAYHEAD = 0xFFFFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUBTLE_TEXT = 0xFFC8C8C8;
    private static final String CONTROLS_HINT = "[Space] Play/Pause    [Left/Right] Step frame    [Shift+Left/Right] Step 1 second    [,] [.] Previous/next trigger    [Esc] Close";

    private RideTimelineOverlay() {
    }

    public record Rect(int x0, int y0, int x1, int y1) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    public record Layout(Rect play, Rect speed, Rect timeline, int readoutY) {
        int trackLeft() {
            return timeline.x0 + TIMELINE_BORDER;
        }

        int trackRight() {
            return timeline.x1 - TIMELINE_BORDER;
        }
    }

    public static Layout layout(int guiWidth) {
        int top = MARGIN;
        int bottom = top + HEIGHT;
        Rect play = new Rect(MARGIN, top, MARGIN + HEIGHT, bottom);
        Rect speed = new Rect(play.x1 + GAP, top, play.x1 + GAP + SPEED_WIDTH, bottom);
        Rect timeline = new Rect(speed.x1 + GAP, top, guiWidth - MARGIN, bottom);
        return new Layout(play, speed, timeline, bottom + 3);
    }

    public static int xForFrame(Layout layout, double frame) {
        RideTimelinePayload timeline = RideTimeline.current();
        double span = Math.max(1, timeline.lastFrame() - timeline.firstFrame());
        double progress = (frame - timeline.firstFrame()) / span;
        return layout.trackLeft() + (int) Math.round(progress * (layout.trackRight() - layout.trackLeft()));
    }

    public static double frameAt(Layout layout, double x) {
        RideTimelinePayload timeline = RideTimeline.current();
        double progress = (x - layout.trackLeft()) / (double) (layout.trackRight() - layout.trackLeft());
        double frame = timeline.firstFrame() + progress * (timeline.lastFrame() - timeline.firstFrame());
        return RideTimeline.snap(RideTimeline.clamp(frame));
    }

    public static boolean isOverTimeline(Layout layout, double x, double y) {
        return layout.timeline().contains(x, y);
    }

    public static boolean isOverPlay(Layout layout, double x, double y) {
        return layout.play().contains(x, y);
    }

    public static boolean isOverSpeed(Layout layout, double x, double y) {
        return layout.speed().contains(x, y);
    }

    public static Integer markerAt(Layout layout, double x, double y) {
        if (!isOverTimeline(layout, x, y)) {
            return null;
        }
        Integer closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (int trigger : RideTimeline.current().triggerFrames()) {
            double distance = Math.abs(xForFrame(layout, trigger) - x);
            if (distance <= MARKER_HIT_RADIUS && distance < closestDistance) {
                closest = trigger;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public static double[] speedSteps() {
        return RideTimeline.current().interpolated() ? INTERPOLATED_SPEEDS : FIXED_SPEEDS;
    }

    public static double speedAt(Layout layout, double x) {
        double[] steps = speedSteps();
        Rect speed = layout.speed();
        double progress = (x - speed.x0 - 4) / (double) (speed.x1 - speed.x0 - 8);
        int index = (int) Math.round(Math.max(0, Math.min(1, progress)) * (steps.length - 1));
        return steps[index];
    }

    public static double neighbourSpeed(double current, int direction) {
        double[] steps = speedSteps();
        int index = nearestSpeedIndex(current);
        return steps[Math.max(0, Math.min(steps.length - 1, index + direction))];
    }

    private static int nearestSpeedIndex(double speed) {
        double[] steps = speedSteps();
        int nearest = 0;
        for (int i = 1; i < steps.length; i++) {
            if (Math.abs(steps[i] - speed) < Math.abs(steps[nearest] - speed)) {
                nearest = i;
            }
        }
        return nearest;
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, double frame, int mouseX, int mouseY, Integer hoveredMarker) {
        Layout layout = layout(graphics.guiWidth());
        drawPlayButton(graphics, layout.play(), isOverPlay(layout, mouseX, mouseY));
        drawSpeedSlider(graphics, font, layout.speed(), isOverSpeed(layout, mouseX, mouseY));
        drawTimeline(graphics, layout, frame, hoveredMarker);
        drawReadout(graphics, font, layout, frame);
    }

    private static void drawFrame(GuiGraphicsExtractor graphics, Rect rect, boolean hovered) {
        graphics.fill(rect.x0, rect.y0, rect.x1, rect.y1, OUTLINE);
        graphics.fill(rect.x0 + 1, rect.y0 + 1, rect.x1 - 1, rect.y1 - 1, hovered ? FACE_HOVERED : FACE);
        graphics.fill(rect.x0 + 1, rect.y0 + 1, rect.x1 - 1, rect.y0 + 2, HIGHLIGHT);
    }

    private static void drawPlayButton(GuiGraphicsExtractor graphics, Rect rect, boolean hovered) {
        drawFrame(graphics, rect, hovered);
        int centerX = (rect.x0 + rect.x1) / 2;
        int centerY = (rect.y0 + rect.y1) / 2;
        if (RideTimeline.isPlaying()) {
            graphics.fill(centerX - 4, centerY - 5, centerX - 1, centerY + 5, TEXT);
            graphics.fill(centerX + 1, centerY - 5, centerX + 4, centerY + 5, TEXT);
            return;
        }
        for (int column = 0; column < 7; column++) {
            int halfHeight = 6 - column;
            graphics.fill(centerX - 3 + column, centerY - halfHeight, centerX - 2 + column, centerY + halfHeight, TEXT);
        }
    }

    private static void drawSpeedSlider(GuiGraphicsExtractor graphics, Font font, Rect rect, boolean hovered) {
        drawFrame(graphics, rect, hovered);
        double[] steps = speedSteps();
        int index = nearestSpeedIndex(RideTimeline.speed());
        double progress = steps.length > 1 ? index / (double) (steps.length - 1) : 0;
        int handleX = rect.x0 + 2 + (int) Math.round(progress * (rect.x1 - rect.x0 - 10));
        graphics.fill(handleX, rect.y0 + 1, handleX + 6, rect.y1 - 1, OUTLINE);
        graphics.fill(handleX + 1, rect.y0 + 2, handleX + 5, rect.y1 - 2, hovered ? 0xFFA0A0A0 : 0xFF808080);
        String label = String.format("Speed: %sx", formatSpeed(RideTimeline.speed()));
        graphics.centeredText(font, label, (rect.x0 + rect.x1) / 2, rect.y0 + (rect.y1 - rect.y0 - 8) / 2, TEXT);
    }

    private static void drawTimeline(GuiGraphicsExtractor graphics, Layout layout, double frame, Integer hoveredMarker) {
        Rect rect = layout.timeline();
        drawFrame(graphics, rect, false);
        int left = layout.trackLeft();
        int right = layout.trackRight();
        int top = rect.y0 + TIMELINE_BORDER;
        int bottom = rect.y1 - TIMELINE_BORDER;
        graphics.fill(left, top, right, bottom, TRACK);

        int playheadX = xForFrame(layout, frame);
        graphics.fill(left, top, playheadX, bottom, PLAYED);

        drawTicks(graphics, layout, top, bottom);

        for (int trigger : RideTimeline.current().triggerFrames()) {
            int x = xForFrame(layout, trigger);
            int color = hoveredMarker != null && hoveredMarker == trigger ? MARKER_HOVERED : MARKER;
            graphics.fill(x - 2, bottom - 1, x + 3, bottom, color);
            graphics.fill(x - 1, bottom - 2, x + 2, bottom - 1, color);
            graphics.fill(x, bottom - 4, x + 1, bottom - 2, color);
        }

        graphics.fill(playheadX, top, playheadX + 1, bottom, PLAYHEAD);
        graphics.fill(playheadX - 2, rect.y0, playheadX + 3, rect.y0 + 2, PLAYHEAD);
        graphics.fill(playheadX - 1, rect.y0 + 2, playheadX + 2, rect.y0 + 3, PLAYHEAD);
    }

    private static void drawTicks(GuiGraphicsExtractor graphics, Layout layout, int top, int bottom) {
        RideTimelinePayload timeline = RideTimeline.current();
        int fps = Math.max(1, timeline.fps());
        double pixelsPerSecond = (layout.trackRight() - layout.trackLeft()) * fps / (double) Math.max(1, timeline.lastFrame() - timeline.firstFrame());
        int majorSeconds = TICK_INTERVALS_SECONDS[TICK_INTERVALS_SECONDS.length - 1];
        for (int interval : TICK_INTERVALS_SECONDS) {
            if (interval * pixelsPerSecond >= MIN_MAJOR_TICK_SPACING) {
                majorSeconds = interval;
                break;
            }
        }
        double minorFrames = majorSeconds * fps / 5.0;
        int height = bottom - top;
        for (int i = 0; ; i++) {
            double frame = timeline.firstFrame() + i * minorFrames;
            if (frame > timeline.lastFrame()) {
                break;
            }
            int x = xForFrame(layout, frame);
            int tickHeight = i % 5 == 0 ? height / 3 : height / 6;
            graphics.fill(x, top, x + 1, top + tickHeight, TICK);
        }
    }

    private static void drawReadout(GuiGraphicsExtractor graphics, Font font, Layout layout, double frame) {
        RideTimelinePayload timeline = RideTimeline.current();
        String name = shortName(timeline.displayName());
        if (!timeline.triggerFrames().isEmpty()) {
            name += "   " + timeline.triggerFrames().size() + " triggers";
        }
        String position = RideTimeline.formatTime(frame) + "   frame " + RideTimeline.formatFrame(frame) + " / " + timeline.lastFrame();
        int y = layout.readoutY();
        graphics.text(font, name, layout.timeline().x0, y, SUBTLE_TEXT, true);
        graphics.text(font, position, layout.timeline().x1 - font.width(position), y, TEXT, true);
        graphics.text(font, CONTROLS_HINT, layout.play().x0, y + font.lineHeight + 2, SUBTLE_TEXT, true);
    }

    private static String shortName(String displayName) {
        int detailsStart = displayName.indexOf(" (");
        return detailsStart > 0 ? displayName.substring(0, detailsStart) : displayName;
    }

    public static void drawTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, String text) {
        Layout layout = layout(graphics.guiWidth());
        int width = font.width(text);
        int x = Math.max(2, Math.min(graphics.guiWidth() - width - 4, mouseX - width / 2));
        int y = layout.readoutY() + (font.lineHeight + 2) * 2 + 3;
        graphics.fill(x - 3, y - 3, x + width + 3, y + font.lineHeight + 2, 0xF0100010);
        graphics.fill(x - 3, y - 3, x + width + 3, y - 2, 0x505000FF);
        graphics.text(font, text, x, y, TEXT, true);
    }

    public static String formatSpeed(double speed) {
        return speed == Math.floor(speed) ? String.format("%.1f", speed) : String.format("%.2f", speed);
    }
}
