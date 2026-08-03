package com.quietphoto.clock;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class PlaybackOrderPolicyTest {
    private static final class Item implements PlaybackOrderPolicy.Item {
        final String name;
        final long time;
        final String key;

        Item(String name, long time, String key) {
            this.name = name;
            this.time = time;
            this.key = key;
        }

        @Override public String sortName() { return name; }
        @Override public long sortTimeMs() { return time; }
        @Override public String stableKey() { return key; }
    }

    @Test
    public void sortsNamesCaseInsensitivelyWithStableTieBreak() {
        List<Item> items = new ArrayList<Item>(Arrays.asList(
                new Item("z.jpg", 1, "z"),
                new Item("A.jpg", 1, "a2"),
                new Item("a.jpg", 1, "a1")));
        PlaybackOrderPolicy.sort(items, PlaybackOrderPolicy.NAME_ASCENDING);
        assertEquals("a1", items.get(0).key);
        assertEquals("a2", items.get(1).key);
        assertEquals("z", items.get(2).key);
    }

    @Test
    public void sortsNewestAndOldestWithUnknownTimesLast() {
        List<Item> items = new ArrayList<Item>(Arrays.asList(
                new Item("unknown", 0, "u"),
                new Item("old", 100, "o"),
                new Item("new", 200, "n")));
        PlaybackOrderPolicy.sort(items, PlaybackOrderPolicy.NEWEST);
        assertEquals("n", items.get(0).key);
        assertEquals("o", items.get(1).key);
        assertEquals("u", items.get(2).key);
        PlaybackOrderPolicy.sort(items, PlaybackOrderPolicy.OLDEST);
        assertEquals("o", items.get(0).key);
        assertEquals("n", items.get(1).key);
        assertEquals("u", items.get(2).key);
    }

    @Test
    public void randomOrderLeavesTheCatalogUntouched() {
        List<Item> items = new ArrayList<Item>(Arrays.asList(
                new Item("b", 1, "b"), new Item("a", 2, "a")));
        PlaybackOrderPolicy.sort(items, PlaybackOrderPolicy.RANDOM);
        assertEquals("b", items.get(0).key);
        assertEquals("a", items.get(1).key);
    }
}
