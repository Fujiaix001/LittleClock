package com.quietphoto.clock;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

public final class FontManager {
    private static final Map<String, Typeface> cache = new HashMap<String, Typeface>();

    public static Typeface getFont(Context context, String assetPath, Typeface fallback) {
        if (assetPath == null || assetPath.isEmpty()) {
            return fallback;
        }
        if (cache.containsKey(assetPath)) {
            Typeface tf = cache.get(assetPath);
            return tf != null ? tf : fallback;
        }
        try {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), assetPath);
            if (tf != null) {
                cache.put(assetPath, tf);
                return tf;
            }
        } catch (Throwable ignored) {
            // 防禦性回退
        }
        cache.put(assetPath, fallback);
        return fallback;
    }
}
