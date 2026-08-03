package com.quietphoto.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

public final class PlaybackNavigatorTest {
    @Test
    public void visitsEveryPhotoBeforeRepeating() {
        PlaybackNavigator navigator = new PlaybackNavigator(new Random(7));
        navigator.reset(8);
        Set<Integer> visited = new HashSet<Integer>();
        for (int i = 0; i < 8; i++) {
            visited.add(navigator.next());
        }
        assertEquals(8, visited.size());
        int last = navigator.current();
        assertNotEquals(last, navigator.next());
    }

    @Test
    public void previousAndNextFollowHistory() {
        PlaybackNavigator navigator = new PlaybackNavigator(new Random(3));
        navigator.reset(5);
        int first = navigator.next();
        int second = navigator.next();
        assertEquals(first, navigator.previous());
        assertEquals(second, navigator.next());
        assertTrue(navigator.current() >= 0);
    }

    @Test
    public void cachedStartupPhotoBecomesCurrentWithoutImmediateRepeat() {
        PlaybackNavigator navigator = new PlaybackNavigator(new Random(5));
        navigator.resetAt(6, 3);
        assertEquals(3, navigator.current());
        assertNotEquals(3, navigator.next());
        assertEquals(3, navigator.previous());
    }

    @Test
    public void sequentialDeckFollowsCatalogOrderAndWraps() {
        PlaybackNavigator navigator = new PlaybackNavigator(new Random(3));
        navigator.setShuffle(false);
        navigator.reset(3);
        assertEquals(0, navigator.next());
        assertEquals(1, navigator.next());
        assertEquals(2, navigator.next());
        assertEquals(0, navigator.next());
    }
}
