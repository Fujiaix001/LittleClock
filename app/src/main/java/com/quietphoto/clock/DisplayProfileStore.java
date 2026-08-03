package com.quietphoto.clock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Stores a small, explicit whitelist of display settings for four manual profiles. */
final class DisplayProfileStore {
    static final String DEFAULT = "default";
    static final String DESK = "desk";
    static final String BEDSIDE = "bedside";
    static final String SHOWCASE = "showcase";

    static final String ACTIVE_PROFILE = "active_display_profile";
    private static final String SCHEMA_VERSION = "display_profile_schema_version";
    private static final String PREFIX = "display_profile.";
    private static final int CURRENT_SCHEMA = 1;
    private static final String[] IDS = { DEFAULT, DESK, BEDSIDE, SHOWCASE };

    private static final String[] PROFILE_KEYS = {
            SettingsActivity.PHOTO_FOLDERS,
            SettingsActivity.PHOTO_INTERVAL_SECONDS,
            SettingsActivity.PHOTO_PLAYBACK_ORDER,
            SettingsActivity.TRANSITION_TYPE,
            SettingsActivity.PHOTO_DISPLAY_MODE,
            SettingsActivity.CLOCK_TIME_ENABLED,
            SettingsActivity.CLOCK_DATE_ENABLED,
            SettingsActivity.CLOCK_BACKGROUND_ENABLED,
            SettingsActivity.CLOCK_FONT_ID,
            SettingsActivity.DATE_FONT_ID,
            SettingsActivity.WEATHER_FONT_ID,
            SettingsActivity.NIGHT_MODE_ENABLED,
            SettingsActivity.NIGHT_START_HOUR,
            SettingsActivity.NIGHT_END_HOUR,
            SettingsActivity.PERFORMANCE_MODE,
            SettingsActivity.LOW_POWER_MODE,
            SettingsActivity.ADAPTIVE_COLOR_ENABLED,
            SettingsActivity.POLAROID_FRAME_ENABLED,
            SettingsActivity.SMART_FOCUS_ENABLED,
            SettingsActivity.BURN_IN_ENABLED,
            SettingsActivity.AUTO_BRIGHTNESS_ENABLED,
            SettingsActivity.FAVORITES_ONLY,
            SettingsActivity.WEATHER_ENABLED,
            SettingsActivity.WEATHER_SHOW_LOCATION,
            SettingsActivity.WEATHER_MINIMAL_LOCATION,
            SettingsActivity.WEATHER_COMPACT_MODE,
            SettingsActivity.WEATHER_EXTENDED_ENABLED,
            SettingsActivity.WEATHER_EXTENDED_FORECAST_ENABLED,
            SettingsActivity.WEATHER_EXTENDED_DAYLIGHT_ENABLED,
            SettingsActivity.CLOCK_SECONDS_MODE,
            SettingsActivity.BATTERY_DISPLAY_MODE,
            PhotoClockActivity.CLOCK_LAYOUT_MODE,
            PhotoClockActivity.CLOCK_ORIGINAL_SCALE_MODE,
            PhotoClockActivity.CLOCK_SIZE_MODE,
            PhotoClockActivity.CLOCK_SIZES_LINKED,
            "clock_scale_factor_portrait",
            "clock_scale_factor_landscape",
            "clock_time_scale_factor_portrait",
            "clock_time_scale_factor_landscape",
            "clock_date_scale_factor_portrait",
            "clock_date_scale_factor_landscape",
            "clock_weather_scale_factor_portrait",
            "clock_weather_scale_factor_landscape",
            "clock_time_pos_x_ratio_portrait",
            "clock_time_pos_x_ratio_landscape",
            "clock_time_pos_y_ratio_portrait",
            "clock_time_pos_y_ratio_landscape",
            "clock_date_pos_x_ratio_portrait",
            "clock_date_pos_x_ratio_landscape",
            "clock_date_pos_y_ratio_portrait",
            "clock_date_pos_y_ratio_landscape",
            "clock_weather_pos_x_ratio_portrait",
            "clock_weather_pos_x_ratio_landscape",
            "clock_weather_pos_y_ratio_portrait",
            "clock_weather_pos_y_ratio_landscape"
    };

    private DisplayProfileStore() {
    }

    static String[] ids() {
        return IDS.clone();
    }

    static String[] labels() {
        return new String[] { "預設", "桌面", "床頭", "展示" };
    }

    static String normalizeId(String id) {
        if (id == null) return DEFAULT;
        for (String candidate : IDS) {
            if (candidate.equals(id)) return candidate;
        }
        return DEFAULT;
    }

    static int indexOf(String id) {
        String normalized = normalizeId(id);
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(normalized)) return i;
        }
        return 0;
    }

    static String activeId(SharedPreferences prefs) {
        return normalizeId(prefs.getString(ACTIVE_PROFILE, DEFAULT));
    }

    static void ensureInitialized(SharedPreferences prefs) {
        int schema = prefs.getInt(SCHEMA_VERSION, 0);
        if (schema >= CURRENT_SCHEMA) return;
        SharedPreferences.Editor editor = prefs.edit();
        for (String id : IDS) {
            captureIntoEditor(prefs, editor, id, false);
        }
        editor.putString(ACTIVE_PROFILE, DEFAULT)
                .putInt(SCHEMA_VERSION, CURRENT_SCHEMA)
                .apply();
    }

    static void captureActive(SharedPreferences prefs) {
        captureIntoEditor(prefs, prefs.edit(), activeId(prefs), true);
    }

    static void capture(SharedPreferences prefs, String id) {
        captureIntoEditor(prefs, prefs.edit(), normalizeId(id), true);
    }

    static void apply(SharedPreferences prefs, String id) {
        String normalized = normalizeId(id);
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : PROFILE_KEYS) {
            String profileKey = profileKey(normalized, key);
            if (prefs.contains(profileKey)) {
                copyValue(prefs, editor, profileKey, key);
            }
        }
        editor.putString(ACTIVE_PROFILE, normalized).apply();
    }

    static boolean isReferencedByAnyProfile(SharedPreferences prefs, String source) {
        if (source == null) return false;
        for (String id : IDS) {
            String key = profileKey(id, SettingsActivity.PHOTO_FOLDERS);
            Set<String> folders = prefs.getStringSet(key, null);
            if (folders != null && folders.contains(source)) return true;
        }
        return false;
    }

    private static void captureIntoEditor(SharedPreferences prefs,
            SharedPreferences.Editor editor, String id, boolean apply) {
        String normalized = normalizeId(id);
        for (String key : PROFILE_KEYS) {
            String profileKey = profileKey(normalized, key);
            editor.remove(profileKey);
            if (prefs.contains(key)) copyValue(prefs, editor, key, profileKey);
        }
        if (apply) editor.apply();
    }

    private static String profileKey(String id, String key) {
        return PREFIX + normalizeId(id) + "." + key;
    }

    private static void copyValue(SharedPreferences source,
            SharedPreferences.Editor target, String sourceKey, String targetKey) {
        Object value = source.getAll().get(sourceKey);
        if (value instanceof Boolean) target.putBoolean(targetKey, (Boolean) value);
        else if (value instanceof Integer) target.putInt(targetKey, (Integer) value);
        else if (value instanceof Long) target.putLong(targetKey, (Long) value);
        else if (value instanceof Float) target.putFloat(targetKey, (Float) value);
        else if (value instanceof String) target.putString(targetKey, (String) value);
        else if (value instanceof Set) {
            Set<String> copied = new HashSet<String>();
            for (Object item : (Set<?>) value) {
                if (item != null) copied.add(String.valueOf(item));
            }
            target.putStringSet(targetKey, copied);
        }
    }
}
