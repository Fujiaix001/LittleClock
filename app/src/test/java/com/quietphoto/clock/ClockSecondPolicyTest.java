package com.quietphoto.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ClockSecondPolicyTest {
    @Test
    public void normalizesUnknownModesToOff() {
        assertEquals(ClockSecondPolicy.OFF, ClockSecondPolicy.normalize(-1));
        assertEquals(ClockSecondPolicy.OFF, ClockSecondPolicy.normalize(99));
    }

    @Test
    public void chargingModeHandlesBatteryAndTvBox() {
        assertTrue(ClockSecondPolicy.shouldShowSeconds(
                ClockSecondPolicy.CHARGING_ONLY, true, true));
        assertFalse(ClockSecondPolicy.shouldShowSeconds(
                ClockSecondPolicy.CHARGING_ONLY, true, false));
        assertTrue(ClockSecondPolicy.shouldShowSeconds(
                ClockSecondPolicy.CHARGING_ONLY, false, false));
    }

    @Test
    public void alwaysAndOffModesAreStable() {
        assertTrue(ClockSecondPolicy.shouldShowSeconds(
                ClockSecondPolicy.ALWAYS, true, false));
        assertFalse(ClockSecondPolicy.shouldShowSeconds(
                ClockSecondPolicy.OFF, false, true));
    }

    @Test
    public void nextSecondDelayIsBoundedAndAligned() {
        assertEquals(1000L, ClockSecondPolicy.delayToNextSecond(1_000L));
        assertEquals(999L, ClockSecondPolicy.delayToNextSecond(1_001L));
        assertEquals(200L, ClockSecondPolicy.delayToNextSecond(1_999L));
    }
}
