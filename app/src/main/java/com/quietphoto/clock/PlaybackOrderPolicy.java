package com.quietphoto.clock;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure ordering rules for the photo catalog; it never reads EXIF data. */
final class PlaybackOrderPolicy {
    static final int RANDOM = 0;
    static final int NAME_ASCENDING = 1;
    static final int NAME_DESCENDING = 2;
    static final int NEWEST = 3;
    static final int OLDEST = 4;

    interface Item {
        String sortName();

        long sortTimeMs();

        String stableKey();
    }

    private PlaybackOrderPolicy() {
    }

    static int normalize(int order) {
        return order < RANDOM || order > OLDEST ? RANDOM : order;
    }

    static <T extends Item> void sort(List<T> items, int order) {
        int normalized = normalize(order);
        if (normalized == RANDOM || items == null || items.size() < 2) return;
        final int direction = normalized == NAME_DESCENDING ? -1 : 1;
        final boolean byTime = normalized == NEWEST || normalized == OLDEST;
        final boolean newestFirst = normalized == NEWEST;
        Collections.sort(items, new Comparator<T>() {
            @Override
            public int compare(T left, T right) {
                if (byTime) {
                    long leftTime = left == null ? 0L : left.sortTimeMs();
                    long rightTime = right == null ? 0L : right.sortTimeMs();
                    boolean leftKnown = leftTime > 0L;
                    boolean rightKnown = rightTime > 0L;
                    if (leftKnown != rightKnown) return leftKnown ? -1 : 1;
                    if (leftKnown && leftTime != rightTime) {
                        int timeResult = leftTime < rightTime ? -1 : 1;
                        return newestFirst ? -timeResult : timeResult;
                    }
                } else {
                    String leftName = lowerName(left);
                    String rightName = lowerName(right);
                    int nameResult = leftName.compareTo(rightName);
                    if (nameResult != 0) return nameResult * direction;
                }
                return stableKey(left).compareTo(stableKey(right));
            }
        });
    }

    private static String lowerName(Item item) {
        String name = item == null ? "" : item.sortName();
        return name == null ? "" : name.toLowerCase(Locale.US);
    }

    private static String stableKey(Item item) {
        if (item == null || item.stableKey() == null) return "";
        return item.stableKey();
    }
}
