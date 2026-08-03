package com.quietphoto.clock;

/** Pure power and low-battery policies shared by the clock UI. */
final class PowerStatePolicy {
    static final int KEEP_AWAKE_ALWAYS = 0;
    static final int KEEP_AWAKE_CHARGING = 1;
    static final int KEEP_AWAKE_SYSTEM = 2;

    private static final int ECO_THRESHOLD_PERCENT = 20;
    private static final int PAUSE_THRESHOLD_PERCENT = 10;
    private static final int RESUME_PAUSE_PERCENT = 15;
    private static final int RESUME_ECO_PERCENT = 25;

    private PowerStatePolicy() {
    }

    static int normalizeKeepAwakeMode(int mode) {
        return mode < KEEP_AWAKE_ALWAYS || mode > KEEP_AWAKE_SYSTEM
                ? KEEP_AWAKE_ALWAYS : mode;
    }

    static boolean shouldKeepScreenOn(
            int mode, boolean batteryPresent, boolean plugged, boolean stateKnown) {
        int normalized = normalizeKeepAwakeMode(mode);
        if (normalized == KEEP_AWAKE_ALWAYS) return true;
        if (normalized == KEEP_AWAKE_SYSTEM) return false;
        if (!stateKnown) return true;
        return !batteryPresent || plugged;
    }

    static boolean shouldForceEco(
            boolean guardEnabled, boolean batteryPresent, boolean plugged, int levelPercent) {
        return guardEnabled && batteryPresent && !plugged
                && levelPercent >= 0 && levelPercent <= ECO_THRESHOLD_PERCENT;
    }

    static boolean shouldPauseAutomaticPhotos(
            boolean guardEnabled, boolean batteryPresent, boolean plugged, int levelPercent) {
        return guardEnabled && batteryPresent && !plugged
                && levelPercent >= 0 && levelPercent <= PAUSE_THRESHOLD_PERCENT;
    }

    static boolean shouldResumeAutomaticPhotos(int levelPercent, boolean plugged) {
        return plugged || (levelPercent >= 0 && levelPercent >= RESUME_PAUSE_PERCENT);
    }

    static boolean shouldResumeForcedEco(int levelPercent, boolean plugged) {
        return plugged || (levelPercent >= 0 && levelPercent >= RESUME_ECO_PERCENT);
    }
}
