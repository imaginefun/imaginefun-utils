package net.imaginefun.timeline;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.imaginefun.networking.RideTimelineControlPayload;
import net.imaginefun.networking.RideTimelinePayload;
import net.imaginefun.networking.RideTimelineStatePayload;
import net.minecraft.client.Minecraft;

public final class RideTimeline {

    private static RideTimelinePayload timeline;
    private static double frame;
    private static boolean playing;
    private static double speed = 1;
    private static long stateReceivedAt;

    private RideTimeline() {
    }

    public static void open(RideTimelinePayload payload) {
        if (timeline == null || !timeline.rideId().equals(payload.rideId())) {
            frame = payload.firstFrame();
            playing = false;
            speed = 1;
            stateReceivedAt = System.currentTimeMillis();
        }
        timeline = payload;
    }

    public static void update(RideTimelineStatePayload payload) {
        if (timeline == null || !timeline.rideId().equals(payload.rideId())) {
            return;
        }
        frame = payload.frame();
        playing = payload.playing();
        speed = payload.speed();
        stateReceivedAt = System.currentTimeMillis();
    }

    public static void close(String rideId) {
        if (timeline != null && timeline.rideId().equals(rideId)) {
            clear();
        }
    }

    public static void clear() {
        timeline = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof RideTimelineScreen) {
            minecraft.gui.setScreen(null);
        }
    }

    public static boolean isActive() {
        return timeline != null;
    }

    public static RideTimelinePayload current() {
        return timeline;
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static double speed() {
        return speed;
    }

    public static double currentFrame() {
        if (timeline == null) {
            return 0;
        }
        if (!playing) {
            return frame;
        }
        double elapsedSeconds = (System.currentTimeMillis() - stateReceivedAt) / 1000.0;
        return clamp(frame + elapsedSeconds * timeline.fps() * speed);
    }

    public static double clamp(double value) {
        return Math.max(timeline.firstFrame(), Math.min(timeline.lastFrame(), value));
    }

    public static double snap(double value) {
        return timeline.interpolated() ? value : Math.round(value);
    }

    public static void play() {
        frame = currentFrame();
        playing = true;
        stateReceivedAt = System.currentTimeMillis();
        send(RideTimelineControlPayload.PLAY, 0);
    }

    public static void pause() {
        frame = currentFrame();
        playing = false;
        send(RideTimelineControlPayload.PAUSE, 0);
    }

    public static void togglePlaying() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    public static void seek(double target) {
        frame = snap(clamp(target));
        stateReceivedAt = System.currentTimeMillis();
        send(RideTimelineControlPayload.SEEK, frame);
    }

    public static void previewSeek(double target) {
        frame = snap(clamp(target));
        stateReceivedAt = System.currentTimeMillis();
    }

    public static void step(double frames) {
        seek(currentFrame() + frames);
    }

    public static void setSpeed(double target) {
        if (target == speed) {
            return;
        }
        frame = currentFrame();
        speed = target;
        stateReceivedAt = System.currentTimeMillis();
        send(RideTimelineControlPayload.SPEED, speed);
    }

    public static Integer previousTrigger(double from) {
        Integer found = null;
        for (int trigger : timeline.triggerFrames()) {
            if (trigger < Math.round(from) && (found == null || trigger > found)) {
                found = trigger;
            }
        }
        return found;
    }

    public static Integer nextTrigger(double from) {
        Integer found = null;
        for (int trigger : timeline.triggerFrames()) {
            if (trigger > Math.round(from) && (found == null || trigger < found)) {
                found = trigger;
            }
        }
        return found;
    }

    public static String formatTime(double frameNumber) {
        double seconds = Math.max(0, (frameNumber - timeline.firstFrame()) / Math.max(1, timeline.fps()));
        int minutes = (int) (seconds / 60);
        return String.format("%02d:%05.2f", minutes, seconds - minutes * 60);
    }

    public static String formatFrame(double frameNumber) {
        return timeline.interpolated() ? String.format("%.1f", frameNumber) : String.valueOf(Math.round(frameNumber));
    }

    private static void send(int action, double value) {
        if (timeline != null) {
            ClientPlayNetworking.send(new RideTimelineControlPayload(timeline.rideId(), action, value));
        }
    }
}
