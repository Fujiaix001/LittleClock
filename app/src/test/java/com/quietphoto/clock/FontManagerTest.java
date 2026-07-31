package com.quietphoto.clock;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FontManagerTest {
    @Test
    public void latinOnlyDateFontUsesEnglishDate() {
        assertTrue(FontManager.usesLatinDate("asset:font_oxanium.ttf"));
    }

    @Test
    public void syntheticBoldStoropiaUsesEnglishDate() {
        assertTrue(FontManager.usesLatinDate("synthetic:storopia-bold"));
    }

    @Test
    public void traditionalChineseDateFontUsesChineseDate() {
        assertFalse(FontManager.usesLatinDate("asset:font_huninn.ttf"));
    }
}
