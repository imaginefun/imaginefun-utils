package net.imaginefun.timeline;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class RideTimelineScreen extends Screen {

    private static final long DRAG_SEEK_INTERVAL_MS = 75;

    private enum Drag { NONE, TIMELINE, SPEED }

    private Drag drag = Drag.NONE;
    private long lastDragSeekAt;

    public RideTimelineScreen() {
        super(Component.literal("Ride timeline"));
    }

    @Override
    public void tick() {
        if (!RideTimeline.isActive()) {
            onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!RideTimeline.isActive()) {
            return;
        }
        RideTimelineOverlay.Layout layout = RideTimelineOverlay.layout(width);
        Integer hoveredMarker = drag == Drag.NONE ? RideTimelineOverlay.markerAt(layout, mouseX, mouseY) : null;
        RideTimelineOverlay.draw(graphics, font, RideTimeline.currentFrame(), mouseX, mouseY, hoveredMarker);

        if (hoveredMarker != null) {
            RideTimelineOverlay.drawTooltip(graphics, font, mouseX, "Trigger at frame " + hoveredMarker + "  " + RideTimeline.formatTime(hoveredMarker));
        } else if (drag == Drag.TIMELINE || RideTimelineOverlay.isOverTimeline(layout, mouseX, mouseY)) {
            double frame = RideTimelineOverlay.frameAt(layout, mouseX);
            RideTimelineOverlay.drawTooltip(graphics, font, mouseX, RideTimeline.formatTime(frame) + "  frame " + RideTimeline.formatFrame(frame));
        } else if (RideTimelineOverlay.isOverPlay(layout, mouseX, mouseY)) {
            RideTimelineOverlay.drawTooltip(graphics, font, mouseX, RideTimeline.isPlaying() ? "Pause (Space)" : "Play (Space)");
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        RideTimelineOverlay.Layout layout = RideTimelineOverlay.layout(width);
        if (RideTimelineOverlay.isOverPlay(layout, event.x(), event.y())) {
            RideTimeline.togglePlaying();
            return true;
        }
        if (RideTimelineOverlay.isOverSpeed(layout, event.x(), event.y())) {
            drag = Drag.SPEED;
            RideTimeline.setSpeed(RideTimelineOverlay.speedAt(layout, event.x()));
            return true;
        }
        if (RideTimelineOverlay.isOverTimeline(layout, event.x(), event.y())) {
            Integer marker = RideTimelineOverlay.markerAt(layout, event.x(), event.y());
            if (marker != null) {
                RideTimeline.seek(marker);
                return true;
            }
            drag = Drag.TIMELINE;
            RideTimeline.seek(RideTimelineOverlay.frameAt(layout, event.x()));
            lastDragSeekAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        RideTimelineOverlay.Layout layout = RideTimelineOverlay.layout(width);
        switch (drag) {
            case SPEED -> RideTimeline.setSpeed(RideTimelineOverlay.speedAt(layout, event.x()));
            case TIMELINE -> {
                double frame = RideTimelineOverlay.frameAt(layout, event.x());
                long now = System.currentTimeMillis();
                if (now - lastDragSeekAt >= DRAG_SEEK_INTERVAL_MS) {
                    RideTimeline.seek(frame);
                    lastDragSeekAt = now;
                } else {
                    RideTimeline.previewSeek(frame);
                }
            }
            case NONE -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drag == Drag.TIMELINE) {
            RideTimeline.seek(RideTimelineOverlay.frameAt(RideTimelineOverlay.layout(width), event.x()));
        }
        boolean wasDragging = drag != Drag.NONE;
        drag = Drag.NONE;
        return wasDragging;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        RideTimelineOverlay.Layout layout = RideTimelineOverlay.layout(width);
        if (scrollY == 0) {
            return false;
        }
        int direction = (int) Math.signum(scrollY);
        if (RideTimelineOverlay.isOverSpeed(layout, mouseX, mouseY)) {
            RideTimeline.setSpeed(RideTimelineOverlay.neighbourSpeed(RideTimeline.speed(), direction));
            return true;
        }
        if (RideTimelineOverlay.isOverTimeline(layout, mouseX, mouseY)) {
            RideTimeline.step(direction);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (RideTimelineKeys.OPEN_TIMELINE.matches(event)) {
            onClose();
            return true;
        }
        double second = RideTimeline.current().fps();
        switch (event.key()) {
            case InputConstants.KEY_SPACE -> RideTimeline.togglePlaying();
            case InputConstants.KEY_LEFT -> RideTimeline.step(event.hasShiftDown() ? -second : -1);
            case InputConstants.KEY_RIGHT -> RideTimeline.step(event.hasShiftDown() ? second : 1);
            case InputConstants.KEY_HOME -> RideTimeline.seek(RideTimeline.current().firstFrame());
            case InputConstants.KEY_END -> RideTimeline.seek(RideTimeline.current().lastFrame());
            case InputConstants.KEY_COMMA -> jumpToTrigger(RideTimeline.previousTrigger(RideTimeline.currentFrame()));
            case InputConstants.KEY_PERIOD -> jumpToTrigger(RideTimeline.nextTrigger(RideTimeline.currentFrame()));
            case InputConstants.KEY_UP -> RideTimeline.setSpeed(RideTimelineOverlay.neighbourSpeed(RideTimeline.speed(), 1));
            case InputConstants.KEY_DOWN -> RideTimeline.setSpeed(RideTimelineOverlay.neighbourSpeed(RideTimeline.speed(), -1));
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    private void jumpToTrigger(Integer trigger) {
        if (trigger != null) {
            RideTimeline.seek(trigger);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
