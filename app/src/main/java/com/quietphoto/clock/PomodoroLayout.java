package com.quietphoto.clock;

/** Keeps a countdown readable without allowing it to exceed its available width. */
public final class PomodoroLayout {
    private PomodoroLayout() { }

    public static float fitTextSize(float targetSp, float sampleWidthAtTargetPx,
            float availableWidthPx) {
        if (targetSp <= 0.0f || sampleWidthAtTargetPx <= 0.0f || availableWidthPx <= 0.0f) {
            return targetSp;
        }
        return Math.max(1.0f, targetSp * Math.min(1.0f, availableWidthPx / sampleWidthAtTargetPx));
    }
}
