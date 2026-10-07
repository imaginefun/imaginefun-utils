package net.imaginefun.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ForcedLookSmoother {

    private static final double DEFAULT_INTERVAL_MS = 50;
    private static final double MIN_INTERVAL_MS = 20;
    private static final double MAX_INTERVAL_MS = 150;
    private static final double INTERVAL_SMOOTHING = 0.2;
    private static final double MAX_TRACKED_GAP_MS = 500;
    private static final double MAX_FRAME_MS = 100;
    private static final float SNAP_DEGREES = 30;

    private static double intervalMs = DEFAULT_INTERVAL_MS;
    private static double lastLookAt;
    private static double lastFrameAt;
    private static double pendingYaw;
    private static double pendingPitch;
    private static double yawPerMs;
    private static double pitchPerMs;

    private ForcedLookSmoother() {
    }

    public static void onForcedLook(float deltaYaw, float deltaPitch) {
        double now = nowMs();
        if (lastLookAt > 0 && now - lastLookAt < MAX_TRACKED_GAP_MS) {
            double gap = now - lastLookAt;
            intervalMs = clamp(intervalMs + (gap - intervalMs) * INTERVAL_SMOOTHING, MIN_INTERVAL_MS, MAX_INTERVAL_MS);
        }
        lastLookAt = now;

        if (Math.abs(deltaYaw) > SNAP_DEGREES || Math.abs(deltaPitch) > SNAP_DEGREES) {
            turn(pendingYaw + deltaYaw, pendingPitch + deltaPitch);
            reset();
            return;
        }

        pendingYaw += deltaYaw;
        pendingPitch += deltaPitch;
        yawPerMs = pendingYaw / intervalMs;
        pitchPerMs = pendingPitch / intervalMs;
    }

    public static void onFrame() {
        double now = nowMs();
        double elapsed = lastFrameAt > 0 ? Math.min(MAX_FRAME_MS, now - lastFrameAt) : 0;
        lastFrameAt = now;
        if (pendingYaw == 0 && pendingPitch == 0) {
            return;
        }
        double yawStep = take(pendingYaw, yawPerMs * elapsed);
        double pitchStep = take(pendingPitch, pitchPerMs * elapsed);
        pendingYaw -= yawStep;
        pendingPitch -= pitchStep;
        turn(yawStep, pitchStep);
    }

    public static void reset() {
        pendingYaw = 0;
        pendingPitch = 0;
        yawPerMs = 0;
        pitchPerMs = 0;
    }

    private static double take(double pending, double step) {
        return Math.abs(step) >= Math.abs(pending) ? pending : step;
    }

    // adjusts the previous rotation too, like mouse turning, so the partial-tick interpolation doesn't fight the change
    private static void turn(double yaw, double pitch) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float newPitch = (float) clamp(player.getXRot() + pitch, -90, 90);
        float appliedPitch = newPitch - player.getXRot();
        player.setYRot(player.getYRot() + (float) yaw);
        player.yRotO += (float) yaw;
        player.setXRot(newPitch);
        player.xRotO += appliedPitch;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double nowMs() {
        return System.nanoTime() / 1_000_000.0;
    }
}
