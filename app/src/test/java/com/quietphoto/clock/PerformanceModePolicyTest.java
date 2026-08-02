package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PerformanceModePolicyTest {
    @Test
    public void legacyPreferenceMapsToEcoOrStandard() {
        assertEquals(PerformanceModePolicy.ECO, PerformanceModePolicy.fromLegacy(true));
        assertEquals(PerformanceModePolicy.STANDARD, PerformanceModePolicy.fromLegacy(false));
    }

    @Test
    public void invalidModesFallBackToEco() {
        assertEquals(PerformanceModePolicy.ECO, PerformanceModePolicy.normalize(-1));
        assertEquals(PerformanceModePolicy.ECO, PerformanceModePolicy.normalize(3));
    }

    @Test
    public void showcaseUsesThirtyFpsTarget() {
        assertEquals(34L,
                PerformanceModePolicy.frameIntervalMs(PerformanceModePolicy.SHOWCASE));
        assertEquals(67L,
                PerformanceModePolicy.frameIntervalMs(PerformanceModePolicy.STANDARD));
    }

    @Test
    public void hotDeviceDowngradesShowcase() {
        assertEquals(PerformanceModePolicy.STANDARD, PerformanceModePolicy.resolveEffectiveMode(
                PerformanceModePolicy.SHOWCASE,
                PerformanceModePolicy.SHOWCASE,
                40.0f,
                false));
        assertEquals(PerformanceModePolicy.ECO, PerformanceModePolicy.resolveEffectiveMode(
                PerformanceModePolicy.SHOWCASE,
                PerformanceModePolicy.STANDARD,
                43.0f,
                false));
    }

    @Test
    public void recoveryUsesHysteresis() {
        assertEquals(PerformanceModePolicy.STANDARD, PerformanceModePolicy.resolveEffectiveMode(
                PerformanceModePolicy.SHOWCASE,
                PerformanceModePolicy.STANDARD,
                39.0f,
                false));
        assertEquals(PerformanceModePolicy.SHOWCASE, PerformanceModePolicy.resolveEffectiveMode(
                PerformanceModePolicy.SHOWCASE,
                PerformanceModePolicy.STANDARD,
                38.0f,
                false));
    }

    @Test
    public void memoryPressureDoesNotKeepShowcaseMode() {
        assertEquals(PerformanceModePolicy.STANDARD, PerformanceModePolicy.resolveEffectiveMode(
                PerformanceModePolicy.SHOWCASE,
                PerformanceModePolicy.SHOWCASE,
                Float.NaN,
                true));
    }
}
