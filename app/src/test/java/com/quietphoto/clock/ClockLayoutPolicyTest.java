package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ClockLayoutPolicyTest {
    @Test
    public void legacyModesMapToSeparatedLayoutState() {
        assertEquals(ClockLayoutPolicy.FREE,
                ClockLayoutPolicy.layoutFromLegacySizeMode(ClockSizeModePolicy.OVERLAP));
        assertEquals(ClockLayoutPolicy.ORIGINAL,
                ClockLayoutPolicy.layoutFromLegacySizeMode(ClockSizeModePolicy.AVOID));
        assertEquals(ClockLayoutPolicy.ORIGINAL,
                ClockLayoutPolicy.layoutFromLegacySizeMode(ClockSizeModePolicy.GROUP));
        assertEquals(ClockLayoutPolicy.ORIGINAL_SCALE_AVOID,
                ClockLayoutPolicy.originalScaleFromLegacySizeMode(ClockSizeModePolicy.AVOID));
        assertEquals(ClockLayoutPolicy.ORIGINAL_SCALE_LINKED,
                ClockLayoutPolicy.originalScaleFromLegacySizeMode(ClockSizeModePolicy.GROUP));
    }

    @Test
    public void freeLayoutDoesNotUseAutomaticAvoidance() {
        assertEquals(ClockSizeModePolicy.OVERLAP,
                ClockLayoutPolicy.legacySizeMode(
                        ClockLayoutPolicy.FREE,
                        ClockLayoutPolicy.ORIGINAL_SCALE_AVOID));
        assertTrue(ClockLayoutPolicy.isLinkedOriginal(
                ClockLayoutPolicy.ORIGINAL,
                ClockLayoutPolicy.ORIGINAL_SCALE_LINKED));
    }
}
