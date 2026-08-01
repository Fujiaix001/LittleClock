package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WeatherClientTest {
    @Test
    public void minimalLocationNameKeepsTheMostLocalPart() {
        assertEquals("Xinyi District",
                WeatherClient.minimalLocationName("Xinyi District, Taipei City, Taiwan"));
        assertEquals("Taipei", WeatherClient.minimalLocationName("Taipei"));
        assertEquals("", WeatherClient.minimalLocationName("  "));
    }
}
