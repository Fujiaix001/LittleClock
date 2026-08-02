package com.quietphoto.clock;

/** Pure policy for throttling the slow full-screen photo pan. */
final class PhotoPanPolicy {
    static final long FRAME_INTERVAL_MS = 67L;

    private PhotoPanPolicy() {
    }

    static boolean shouldRender(float progress, long lastFrameAt, long now) {
        return shouldRender(progress, lastFrameAt, now, FRAME_INTERVAL_MS);
    }

    static boolean shouldRender(
            float progress, long lastFrameAt, long now, long frameIntervalMs) {
        return progress >= 1.0f || lastFrameAt == 0L
                || now - lastFrameAt >= frameIntervalMs;
    }
}
