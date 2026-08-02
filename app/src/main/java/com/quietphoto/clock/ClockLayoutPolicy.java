package com.quietphoto.clock;

/** Separates the clock's arrangement from the effect used while scaling it. */
public final class ClockLayoutPolicy {
    public static final int ORIGINAL = 0;
    public static final int FREE = 1;

    public static final int ORIGINAL_SCALE_LINKED = 0;
    public static final int ORIGINAL_SCALE_AVOID = 1;

    private ClockLayoutPolicy() {
    }

    public static int normalizeLayout(int mode) {
        return mode == FREE ? FREE : ORIGINAL;
    }

    public static int normalizeOriginalScale(int mode) {
        return mode == ORIGINAL_SCALE_AVOID
                ? ORIGINAL_SCALE_AVOID : ORIGINAL_SCALE_LINKED;
    }

    public static int layoutFromLegacySizeMode(int legacyMode) {
        return legacyMode == ClockSizeModePolicy.OVERLAP ? FREE : ORIGINAL;
    }

    public static int originalScaleFromLegacySizeMode(int legacyMode) {
        return legacyMode == ClockSizeModePolicy.AVOID
                ? ORIGINAL_SCALE_AVOID : ORIGINAL_SCALE_LINKED;
    }

    public static int legacySizeMode(int layoutMode, int originalScaleMode) {
        if (normalizeLayout(layoutMode) == FREE) {
            return ClockSizeModePolicy.OVERLAP;
        }
        return normalizeOriginalScale(originalScaleMode) == ORIGINAL_SCALE_AVOID
                ? ClockSizeModePolicy.AVOID : ClockSizeModePolicy.GROUP;
    }

    public static boolean isOriginal(int layoutMode) {
        return normalizeLayout(layoutMode) == ORIGINAL;
    }

    public static boolean isFree(int layoutMode) {
        return normalizeLayout(layoutMode) == FREE;
    }

    public static boolean isLinkedOriginal(int layoutMode, int originalScaleMode) {
        return isOriginal(layoutMode)
                && normalizeOriginalScale(originalScaleMode) == ORIGINAL_SCALE_LINKED;
    }
}
