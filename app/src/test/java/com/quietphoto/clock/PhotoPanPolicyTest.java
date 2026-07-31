package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PhotoPanPolicyTest {
    @Test
    public void allDevicesUseFifteenFpsTarget() {
        assertEquals(67L, PhotoPanPolicy.FRAME_INTERVAL_MS);
    }

    @Test
    public void firstAndFinalFramesAreAlwaysRendered() {
        assertTrue(PhotoPanPolicy.shouldRender(0.01f, 0L, 10L));
        assertTrue(PhotoPanPolicy.shouldRender(1.0f, 100L, 101L));
    }

    @Test
    public void intermediateFramesRespectInterval() {
        assertFalse(PhotoPanPolicy.shouldRender(0.5f, 100L, 166L));
        assertTrue(PhotoPanPolicy.shouldRender(0.5f, 100L, 167L));
    }
}
