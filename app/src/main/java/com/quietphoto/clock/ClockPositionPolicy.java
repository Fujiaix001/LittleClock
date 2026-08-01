package com.quietphoto.clock;

/** Pure coordinate rules for movable clock blocks. */
public final class ClockPositionPolicy {
    private ClockPositionPolicy() {
    }

    public static float centerFromRatio(float ratio, float containerSize) {
        if (!isFinite(containerSize) || containerSize <= 0.0f) {
            return 0.0f;
        }
        return validRatio(ratio, 0.5f) * containerSize;
    }

    public static float ratioFromCenter(float center, float containerSize) {
        if (!isFinite(containerSize) || containerSize <= 0.0f
                || !isFinite(center)) {
            return 0.5f;
        }
        return validRatio(center / containerSize, 0.5f);
    }

    public static float clampCenter(float center, float contentSize,
                                    float containerSize, float margin) {
        if (!isFinite(containerSize) || containerSize <= 0.0f) {
            return 0.0f;
        }
        if (!isFinite(center)) {
            center = containerSize / 2.0f;
        }
        if (!isFinite(contentSize) || contentSize < 0.0f) {
            contentSize = 0.0f;
        }
        if (!isFinite(margin) || margin < 0.0f) {
            margin = 0.0f;
        }

        float minCenter = margin + contentSize / 2.0f;
        float maxCenter = containerSize - margin - contentSize / 2.0f;
        if (minCenter > maxCenter) {
            return containerSize / 2.0f;
        }
        return Math.max(minCenter, Math.min(maxCenter, center));
    }

    public static float validRatio(float ratio, float fallback) {
        if (!isFinite(ratio)) {
            ratio = isFinite(fallback) ? fallback : 0.5f;
        }
        return Math.max(0.0f, Math.min(1.0f, ratio));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
