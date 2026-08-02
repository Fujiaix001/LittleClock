package com.quietphoto.clock;

/** Pure policy for selecting bounded visual-performance levels. */
final class PerformanceModePolicy {
    static final int ECO = 0;
    static final int STANDARD = 1;
    static final int SHOWCASE = 2;

    static final long STANDARD_FRAME_INTERVAL_MS = 67L;
    static final long SHOWCASE_FRAME_INTERVAL_MS = 34L;
    static final float STANDARD_DOWNGRADE_TEMPERATURE_C = 40.0f;
    static final float ECO_DOWNGRADE_TEMPERATURE_C = 43.0f;
    static final float SHOWCASE_RECOVERY_TEMPERATURE_C = 38.0f;

    private PerformanceModePolicy() {
    }

    static int normalize(int mode) {
        return mode < ECO || mode > SHOWCASE ? ECO : mode;
    }

    static int fromLegacy(boolean lowPowerMode) {
        return lowPowerMode ? ECO : STANDARD;
    }

    static boolean isEco(int mode) {
        return normalize(mode) == ECO;
    }

    static boolean isShowcase(int mode) {
        return normalize(mode) == SHOWCASE;
    }

    static long frameIntervalMs(int mode) {
        return isShowcase(mode)
                ? SHOWCASE_FRAME_INTERVAL_MS : STANDARD_FRAME_INTERVAL_MS;
    }

    /**
     * Applies thermal and memory-pressure limits without changing the saved user preference.
     * Temperature values are in Celsius; NaN means the device did not report one.
     */
    static int resolveEffectiveMode(
            int selectedMode,
            int currentEffectiveMode,
            float batteryTemperatureC,
            boolean memoryPressure) {
        int selected = normalize(selectedMode);
        if (selected != SHOWCASE) return selected;

        int current = normalize(currentEffectiveMode);
        if (memoryPressure) {
            return current == ECO ? ECO : STANDARD;
        }
        if (Float.isNaN(batteryTemperatureC)) {
            return current == ECO ? ECO : STANDARD;
        }
        if (batteryTemperatureC >= ECO_DOWNGRADE_TEMPERATURE_C) return ECO;
        if (batteryTemperatureC >= STANDARD_DOWNGRADE_TEMPERATURE_C) {
            return STANDARD;
        }
        if (current == ECO && batteryTemperatureC > SHOWCASE_RECOVERY_TEMPERATURE_C) {
            return ECO;
        }
        if (current == STANDARD && batteryTemperatureC > SHOWCASE_RECOVERY_TEMPERATURE_C) {
            return STANDARD;
        }
        return SHOWCASE;
    }
}
