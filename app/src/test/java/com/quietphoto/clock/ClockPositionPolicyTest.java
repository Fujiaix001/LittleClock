package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ClockPositionPolicyTest {
    @Test
    public void clampsCenterWithoutAllowingContentToLeaveMargins() {
        assertEquals(62.0f,
                ClockPositionPolicy.clampCenter(-10.0f, 80.0f, 400.0f, 22.0f),
                0.01f);
        assertEquals(338.0f,
                ClockPositionPolicy.clampCenter(500.0f, 80.0f, 400.0f, 22.0f),
                0.01f);
    }

    @Test
    public void centersOversizedContentInsteadOfReturningInvalidBounds() {
        assertEquals(200.0f,
                ClockPositionPolicy.clampCenter(20.0f, 500.0f, 400.0f, 12.0f),
                0.01f);
    }

    @Test
    public void convertsRatiosAcrossContainerSizes() {
        float center = ClockPositionPolicy.centerFromRatio(0.75f, 1200.0f);
        assertEquals(900.0f, center, 0.01f);
        assertEquals(0.75f,
                ClockPositionPolicy.ratioFromCenter(center, 1200.0f),
                0.01f);
    }

    @Test
    public void invalidRatiosUseSafeFallbacks() {
        assertEquals(0.5f,
                ClockPositionPolicy.validRatio(Float.NaN, 0.5f),
                0.01f);
        assertEquals(1.0f,
                ClockPositionPolicy.validRatio(4.0f, 0.5f),
                0.01f);
        assertEquals(0.0f,
                ClockPositionPolicy.validRatio(-2.0f, 0.5f),
                0.01f);
    }
}
