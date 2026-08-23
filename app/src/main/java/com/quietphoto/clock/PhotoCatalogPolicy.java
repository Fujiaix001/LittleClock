package com.quietphoto.clock;

import java.util.Set;

/** Pure Java rules for building the in-memory photo catalog. */
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

    /** A null selection preserves the legacy behavior of including every private folder. */
    public static boolean includesPrivateFolder(Set<String> selectedFolders, String folder) {
        return selectedFolders == null
                || folder != null && selectedFolders.contains(folder);
    }
}
