package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PhotoCatalogPolicyTest {
    @Test public void keepsFullCatalogOnNormalDevices() {
        assertEquals(50_000, PhotoCatalogPolicy.maxPhotoFiles(false, 256L * 1024L * 1024L));
    }

    @Test public void reducesCatalogForLowRamDevices() {
        assertEquals(15_000, PhotoCatalogPolicy.maxPhotoFiles(true, 256L * 1024L * 1024L));
        assertEquals(15_000, PhotoCatalogPolicy.maxPhotoFiles(false, 64L * 1024L * 1024L));
    }
}
