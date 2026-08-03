package com.quietphoto.clock;

/** Pure policy for optional second-level clock updates. */
final class ClockSecondPolicy {
    static final int OFF = 0;
    static final int CHARGING_ONLY = 1;
    static final int ALWAYS = 2;

    private ClockSecondPolicy() {
    }

    static int normalize(int mode) {
        return mode < OFF || mode > ALWAYS ? OFF : mode;
    }

    static boolean shouldShowSeconds(int mode, boolean batteryPresent, boolean plugged) {
        int normalized = normalize(mode);
        if (normalized == ALWAYS) return true;
        if (normalized != CHARGING_ONLY) return false;
        return !batteryPresent || plugged;
    }

    static long delayToNextSecond(long wallClockMs) {
        long remainder = wallClockMs % 1000L;
        long delay = remainder == 0L ? 1000L : 1000L - remainder;
        return Math.max(200L, delay);
    }
}
