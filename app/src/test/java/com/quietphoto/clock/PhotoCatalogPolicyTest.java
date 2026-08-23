package com.quietphoto.clock;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PhotoCatalogPolicyTest {
    @Test public void keepsFullCatalogOnNormalDevices() {
        assertEquals(50_000, PhotoCatalogPolicy.maxPhotoFiles(false, 256L * 1024L * 1024L));
    }

    @Test public void reducesCatalogForLowRamDevices() {
        assertEquals(15_000, PhotoCatalogPolicy.maxPhotoFiles(true, 256L * 1024L * 1024L));
        assertEquals(15_000, PhotoCatalogPolicy.maxPhotoFiles(false, 64L * 1024L * 1024L));
    }

    @Test public void privateFolderSelectionSupportsAllSomeAndNone() {
        assertTrue(PhotoCatalogPolicy.includesPrivateFolder(null, "Camera"));
        Set<String> selected = new HashSet<String>(Collections.singleton("Camera"));
        assertTrue(PhotoCatalogPolicy.includesPrivateFolder(selected, "Camera"));
        assertFalse(PhotoCatalogPolicy.includesPrivateFolder(selected, "Screenshots"));
        assertFalse(PhotoCatalogPolicy.includesPrivateFolder(selected, null));
        assertFalse(PhotoCatalogPolicy.includesPrivateFolder(
                Collections.<String>emptySet(), "Camera"));
    }
}
