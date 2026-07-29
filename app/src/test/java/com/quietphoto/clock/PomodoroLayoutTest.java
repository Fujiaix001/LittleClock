package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PomodoroLayoutTest {
    @Test public void keepsTargetWhenThereIsRoom() {
        assertEquals(100.0f, PomodoroLayout.fitTextSize(100.0f, 400.0f, 480.0f), 0.01f);
    }

    @Test public void shrinksInsteadOfOverflowing() {
        assertEquals(75.0f, PomodoroLayout.fitTextSize(100.0f, 400.0f, 300.0f), 0.01f);
    }
}
