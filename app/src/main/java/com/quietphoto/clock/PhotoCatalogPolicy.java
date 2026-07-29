package com.quietphoto.clock;

/** Memory-aware limits for the in-memory photo catalog. Pure Java so it stays testable. */
public final class PhotoCatalogPolicy {
    private PhotoCatalogPolicy() { }

    public static int maxPhotoFiles(boolean lowRamDevice, long maxMemoryBytes) {
        if (lowRamDevice || maxMemoryBytes > 0L && maxMemoryBytes <= 96L * 1024L * 1024L) {
            return 15_000;
        }
        if (maxMemoryBytes > 0L && maxMemoryBytes <= 160L * 1024L * 1024L) {
            return 25_000;
        }
        return 50_000;
    }
}
