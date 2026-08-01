package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClockSizeModePolicyTest {
    @Test
    public void legacyLinkedPreferenceMapsToFixedPositionMode() {
        assertEquals(ClockSizeModePolicy.OVERLAP,
                ClockSizeModePolicy.fromLegacyLinked(true));
        assertEquals(ClockSizeModePolicy.AVOID,
                ClockSizeModePolicy.fromLegacyLinked(false));
    }

    @Test
    public void invalidModesFallBackToTheCompatibleDefault() {
        assertEquals(ClockSizeModePolicy.OVERLAP, ClockSizeModePolicy.normalize(-1));
        assertEquals(ClockSizeModePolicy.OVERLAP, ClockSizeModePolicy.normalize(99));
        assertEquals(ClockSizeModePolicy.GROUP,
                ClockSizeModePolicy.normalize(ClockSizeModePolicy.GROUP));
    }

    @Test
    public void onlyAvoidModeUsesIndependentScaleValues() {
        assertTrue(ClockSizeModePolicy.usesLinkedScale(ClockSizeModePolicy.OVERLAP));
        assertFalse(ClockSizeModePolicy.usesLinkedScale(ClockSizeModePolicy.AVOID));
        assertTrue(ClockSizeModePolicy.usesLinkedScale(ClockSizeModePolicy.GROUP));
    }
}
