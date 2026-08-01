package com.quietphoto.clock;

/** Pure compatibility rules for the three clock size-change behaviors. */
public final class ClockSizeModePolicy {
    public static final int OVERLAP = 0;
    public static final int AVOID = 1;
    public static final int GROUP = 2;

    private ClockSizeModePolicy() {
    }

    public static int normalize(int mode) {
        if (mode < OVERLAP || mode > GROUP) {
            return OVERLAP;
        }
        return mode;
    }

    public static int fromLegacyLinked(boolean linked) {
        return linked ? OVERLAP : AVOID;
    }

    public static boolean usesLinkedScale(int mode) {
        return normalize(mode) != AVOID;
    }
}
