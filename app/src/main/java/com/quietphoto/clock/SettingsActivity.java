package com.quietphoto.clock;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SettingsActivity extends Activity {
    public static final String PREFERENCES = "quietphotoclock";
    public static final String PHOTO_FOLDERS = "photo_folders";
    public static final String PHOTO_INTERVAL_SECONDS = "photo_interval_seconds";
    public static final String PHOTO_PLAYBACK_ORDER = "photo_playback_order";
    public static final String CLOCK_SECONDS_MODE = "clock_seconds_mode";
    public static final String KEEP_SCREEN_MODE = "keep_screen_mode";
    public static final String LOW_BATTERY_GUARD = "low_battery_guard";
    public static final String BATTERY_DISPLAY_MODE = "battery_display_mode";
    public static final String CLOCK_X_RATIO = "clock_x_ratio";
    public static final String CLOCK_Y_RATIO = "clock_y_ratio";
    public static final String CLOCK_TIME_ENABLED = "clock_time_enabled";
    public static final String CLOCK_DATE_ENABLED = "clock_date_enabled";
    public static final String CLOCK_BACKGROUND_ENABLED = "clock_bg_enabled";
    public static final String CLOCK_FONT_STYLE = "clock_font_style";
    public static final String CLOCK_FONT_ID = "clock_font_id";
    public static final String DATE_FONT_ID = "date_font_id";
    public static final String WEATHER_FONT_ID = "weather_font_id";
    public static final String NIGHT_MODE_ENABLED = "night_mode_enabled";
    public static final String NIGHT_START_HOUR = "night_start_hour";
    public static final String NIGHT_END_HOUR = "night_end_hour";
    public static final String TRANSITION_TYPE = "transition_type";
    public static final String LOW_POWER_MODE = "low_power_mode";
    public static final String PERFORMANCE_MODE = "performance_mode";
    public static final String PHOTO_DISPLAY_MODE = "photo_display_mode";
    public static final String BURN_IN_ENABLED = "burn_in_enabled";
    public static final String AUTO_BRIGHTNESS_ENABLED = "auto_brightness_enabled";
    public static final String FAVORITES_ONLY = "favorites_only";
    public static final String FAVORITE_PHOTOS = "favorite_photos";
    public static final String HIDDEN_PHOTOS = "hidden_photos";
    public static final String WEATHER_ENABLED = "weather_enabled";
    public static final String WEATHER_SHOW_LOCATION = "weather_show_location";
    public static final String WEATHER_MINIMAL_LOCATION = "weather_minimal_location";
    public static final String WEATHER_COMPACT_MODE = "weather_compact_mode";
    public static final String WEATHER_EXTENDED_ENABLED = "weather_extended_enabled";
    public static final String WEATHER_EXTENDED_FORECAST_ENABLED =
            "weather_extended_forecast_enabled";
    public static final String WEATHER_EXTENDED_DAYLIGHT_ENABLED =
            "weather_extended_daylight_enabled";
    public static final String WEATHER_LOCATION_NAME = "weather_location_name";
    public static final String WEATHER_LATITUDE = "weather_latitude";
    public static final String WEATHER_LONGITUDE = "weather_longitude";
    public static final String WEATHER_TIMEZONE = "weather_timezone";
    public static final String WEATHER_TEMPERATURE = "weather_temperature";
    public static final String WEATHER_CODE = "weather_code";
    public static final String WEATHER_IS_DAY = "weather_is_day";
    public static final String WEATHER_UPDATED_AT = "weather_updated_at";
    public static final String WEATHER_EXTENDED_UPDATED_AT = "weather_extended_updated_at";
    public static final String WEATHER_EXTENDED_FORECAST = "weather_extended_forecast";
    public static final String WEATHER_EXTENDED_SUNRISE_AT = "weather_extended_sunrise_at";
    public static final String WEATHER_EXTENDED_SUNSET_AT = "weather_extended_sunset_at";
    public static final String FILE_SOURCE_PREFIX = "file:";
    public static final String TREE_SOURCE_PREFIX = "tree:";

    // 3 大進階視覺特效 Key
    public static final String ADAPTIVE_COLOR_ENABLED = "adaptive_color_enabled";
    public static final String POLAROID_FRAME_ENABLED = "polaroid_frame_enabled";
    public static final String SMART_FOCUS_ENABLED = "smart_focus_enabled";

    public static final int DEFAULT_INTERVAL_SECONDS = 40;
    private static final int REQUEST_PICK_PHOTO_TREE = 4101;
    private static final int[] INTERVAL_STEPS = { 15, 30, 40, 60, 120 };

    private static final int BACKGROUND = Color.rgb(9, 13, 18);
    private static final int PANEL = Color.rgb(19, 26, 35);
    private static final int PANEL_RAISED = Color.rgb(27, 37, 49);
    private static final int STROKE = Color.rgb(42, 56, 72);
    private static final int PRIMARY = Color.rgb(244, 247, 249);
    private static final int SECONDARY = Color.rgb(155, 169, 184);
    private static final int ACCENT = Color.rgb(104, 213, 216);
    private static final int ACTIVE_CHIP = Color.rgb(30, 118, 126);

    private final Set<String> selectedFolders = new LinkedHashSet<String>();
    private final Set<String> initialFolderSources = new LinkedHashSet<String>();
    private final Set<String> newlyGrantedTreeSources = new LinkedHashSet<String>();
    private int selectedInterval;
    private int playbackOrder = PlaybackOrderPolicy.RANDOM;
    private int secondsMode = ClockSecondPolicy.OFF;
    private int keepScreenMode = PowerStatePolicy.KEEP_AWAKE_ALWAYS;
    private boolean lowBatteryGuard;
    private int batteryDisplayMode;
    private boolean clockTimeEnabled = true;
    private boolean clockDateEnabled = true;
    private int clockLayoutMode = PhotoClockActivity.CLOCK_LAYOUT_MODE_ORIGINAL;
    private int originalScaleMode = PhotoClockActivity.CLOCK_ORIGINAL_SCALE_LINKED;
    private boolean clockBgEnabled;
    private int selectedFontStyle;
    private String selectedFontId;
    private String selectedDateFontId;
    private String selectedWeatherFontId;
    private List<FontManager.FontOption> fontOptions;
    private boolean nightModeEnabled;
    private int nightStartHour;
    private int nightEndHour;
    private int selectedTransition;
    private int selectedDisplayMode;

    private boolean adaptiveColorEnabled;
    private boolean polaroidFrameEnabled;
    private boolean smartFocusEnabled;
    private int performanceMode;
    private boolean burnInEnabled;
    private boolean autoBrightnessEnabled;
    private boolean favoritesOnly;
    private boolean weatherEnabled;
    private boolean weatherShowLocation;
    private boolean weatherMinimalLocation;
    private boolean weatherCompactMode = true;
    private boolean weatherExtendedEnabled;
    private boolean weatherExtendedForecastEnabled = true;
    private boolean weatherExtendedDaylightEnabled = true;
    private String weatherLocationName = "";
    private String weatherTimezone = "auto";
    private double weatherLatitude = Double.NaN;
    private double weatherLongitude = Double.NaN;
    private boolean weatherLocationChanged;
    private boolean clearHiddenRequested;
    private boolean resetClockLayoutRequested;
    private boolean settingsSaved;
    private boolean pendingGrantsReleased;
    private boolean awaitingExactAlarmPermission;

    private boolean alarmEnabled;
    private int alarmHour = 7;
    private int alarmMinute = 0;
    private boolean alarmRepeat = true;
    private int pomodoroFocusMinutes = PomodoroHelper.DEFAULT_FOCUS_MINUTES;
    private int pomodoroShortBreakMinutes = PomodoroHelper.DEFAULT_SHORT_BREAK_MINUTES;
    private int pomodoroLongBreakMinutes = PomodoroHelper.DEFAULT_LONG_BREAK_MINUTES;
    private int pomodoroDisplayMode = PomodoroHelper.DISPLAY_MODE_ORIGINAL;

    private File currentDirectory;

    private TextView pathText;
    private TextView selectionText;
    private TextView intervalDisplay;
    private SeekBar intervalSeekBar;
    private CheckBox nightModeCheck;
    private CheckBox clockTimeCheck;
    private CheckBox clockDateCheck;
    private CheckBox clockBgCheck;
    private CheckBox adaptiveColorCheck;
    private CheckBox polaroidFrameCheck;
    private CheckBox smartFocusCheck;
    private CheckBox burnInCheck;
    private CheckBox autoBrightnessCheck;
    private CheckBox favoritesOnlyCheck;
    private CheckBox currentFolderCheck;
    private CheckBox weatherEnabledCheck;
    private CheckBox weatherLocationCheck;
    private CheckBox weatherMinimalLocationCheck;
    private CheckBox weatherCompactCheck;
    private CheckBox weatherExtendedCheck;
    private CheckBox weatherExtendedForecastCheck;
    private CheckBox weatherExtendedDaylightCheck;
    private CheckBox alarmEnabledCheck;
    private CheckBox alarmRepeatCheck;
    private TextView nightScheduleText;
    private TextView weatherLocationText;
    private TextView alarmTimeDisplay;
    private LinearLayout folderList;

    private Spinner fontSpinner;
    private Spinner dateFontSpinner;
    private Spinner weatherFontSpinner;
    private Spinner transitionSpinner;
    private Spinner pomodoroDisplayModeSpinner;
    private Spinner displayModeSpinner;
    private Spinner performanceModeSpinner;
    private Spinner clockLayoutModeSpinner;
    private Spinner originalScaleModeSpinner;
    private Spinner playbackOrderSpinner;
    private Spinner secondsModeSpinner;
    private Spinner keepScreenModeSpinner;
    private Spinner batteryDisplayModeSpinner;
    private CheckBox lowBatteryGuardCheck;
    private Spinner profileSpinner;
    private boolean profileSpinnerReady;
    private String activeProfileId = DisplayProfileStore.DEFAULT;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private float displayDensity;
    private Typeface uiTypeface;

    private static class StorageVolumeItem {
        final String label;
        final File rootDir;

        StorageVolumeItem(String label, File rootDir) {
            this.label = label;
            this.rootDir = rootDir;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayDensity = getResources().getDisplayMetrics().density;
        uiTypeface = FontManager.getPomodoroChineseFont(this);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        registerPredictiveBackCallback();

        currentDirectory = null;

        android.content.SharedPreferences prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        DisplayProfileStore.ensureInitialized(prefs);
        activeProfileId = DisplayProfileStore.activeId(prefs);
        selectedInterval = nearestIntervalStep(
                prefs.getInt(PHOTO_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS));
        playbackOrder = PlaybackOrderPolicy.normalize(
                prefs.getInt(PHOTO_PLAYBACK_ORDER, PlaybackOrderPolicy.RANDOM));
        secondsMode = ClockSecondPolicy.normalize(
                prefs.getInt(CLOCK_SECONDS_MODE, ClockSecondPolicy.OFF));
        keepScreenMode = PowerStatePolicy.normalizeKeepAwakeMode(
                prefs.getInt(KEEP_SCREEN_MODE, PowerStatePolicy.KEEP_AWAKE_ALWAYS));
        lowBatteryGuard = prefs.getBoolean(LOW_BATTERY_GUARD, false);
        batteryDisplayMode = Math.max(0, Math.min(2,
                prefs.getInt(BATTERY_DISPLAY_MODE, 0)));
        clockTimeEnabled = prefs.getBoolean(CLOCK_TIME_ENABLED, true);
        clockDateEnabled = prefs.getBoolean(CLOCK_DATE_ENABLED, true);
        if (prefs.contains(PhotoClockActivity.CLOCK_LAYOUT_MODE)) {
            clockLayoutMode = ClockLayoutPolicy.normalizeLayout(prefs.getInt(
                    PhotoClockActivity.CLOCK_LAYOUT_MODE,
                    PhotoClockActivity.CLOCK_LAYOUT_MODE_ORIGINAL));
            originalScaleMode = ClockLayoutPolicy.normalizeOriginalScale(prefs.getInt(
                    PhotoClockActivity.CLOCK_ORIGINAL_SCALE_MODE,
                    PhotoClockActivity.CLOCK_ORIGINAL_SCALE_LINKED));
        } else if (prefs.contains(PhotoClockActivity.CLOCK_SIZE_MODE)) {
            int legacySizeMode = ClockSizeModePolicy.normalize(prefs.getInt(
                    PhotoClockActivity.CLOCK_SIZE_MODE,
                    PhotoClockActivity.CLOCK_SIZE_MODE_OVERLAP));
            clockLayoutMode = ClockLayoutPolicy.layoutFromLegacySizeMode(legacySizeMode);
            originalScaleMode = ClockLayoutPolicy.originalScaleFromLegacySizeMode(legacySizeMode);
        } else {
            boolean legacyLinked = prefs.getBoolean(PhotoClockActivity.CLOCK_SIZES_LINKED,
                    prefs.getBoolean(PhotoClockActivity.LEGACY_BIND_SCALE, true));
            clockLayoutMode = PhotoClockActivity.CLOCK_LAYOUT_MODE_ORIGINAL;
            originalScaleMode = legacyLinked
                    ? PhotoClockActivity.CLOCK_ORIGINAL_SCALE_LINKED
                    : PhotoClockActivity.CLOCK_ORIGINAL_SCALE_AVOID;
        }
        clockBgEnabled = prefs.getBoolean(CLOCK_BACKGROUND_ENABLED, false);
        selectedFontStyle = prefs.getInt(CLOCK_FONT_STYLE, 0);
        String defaultFont = BuildConfig.INCLUDE_STOROPIA ? "asset:font_storopia.ttf" : "asset:font_oxanium.ttf";
        selectedFontId = FontManager.normalizeId(this, prefs.getString(
                CLOCK_FONT_ID, selectedFontStyle == 0 ? defaultFont : FontManager.getIdForLegacyIndex(selectedFontStyle)));
        selectedDateFontId = FontManager.normalizeId(this, prefs.getString(
                DATE_FONT_ID, selectedFontId));
        selectedWeatherFontId = FontManager.normalizeId(this, prefs.getString(
                WEATHER_FONT_ID, selectedFontId));
        fontOptions = FontManager.getOptions(this);
        nightModeEnabled = prefs.getBoolean(NIGHT_MODE_ENABLED, false);
        nightStartHour = prefs.getInt(NIGHT_START_HOUR, 23);
        nightEndHour = prefs.getInt(NIGHT_END_HOUR, 7);
        selectedTransition = prefs.getInt(TRANSITION_TYPE, 0);
        selectedDisplayMode = prefs.getInt(PHOTO_DISPLAY_MODE, 0);
        adaptiveColorEnabled = prefs.getBoolean(ADAPTIVE_COLOR_ENABLED, true);
        polaroidFrameEnabled = prefs.getBoolean(POLAROID_FRAME_ENABLED, false);
        smartFocusEnabled = prefs.getBoolean(SMART_FOCUS_ENABLED, true);
        performanceMode = prefs.contains(PERFORMANCE_MODE)
                ? PerformanceModePolicy.normalize(prefs.getInt(
                        PERFORMANCE_MODE, PerformanceModePolicy.ECO))
                : PerformanceModePolicy.fromLegacy(prefs.getBoolean(LOW_POWER_MODE, true));
        burnInEnabled = prefs.getBoolean(BURN_IN_ENABLED, true);
        autoBrightnessEnabled = prefs.getBoolean(AUTO_BRIGHTNESS_ENABLED, false);
        favoritesOnly = prefs.getBoolean(FAVORITES_ONLY, false);
        weatherEnabled = prefs.getBoolean(WEATHER_ENABLED, false);
        weatherShowLocation = prefs.getBoolean(WEATHER_SHOW_LOCATION, false);
        weatherMinimalLocation = prefs.getBoolean(WEATHER_MINIMAL_LOCATION, false);
        weatherCompactMode = prefs.getBoolean(WEATHER_COMPACT_MODE, true);
        weatherExtendedEnabled = prefs.getBoolean(WEATHER_EXTENDED_ENABLED, false);
        weatherExtendedForecastEnabled = prefs.getBoolean(
                WEATHER_EXTENDED_FORECAST_ENABLED, true);
        weatherExtendedDaylightEnabled = prefs.getBoolean(
                WEATHER_EXTENDED_DAYLIGHT_ENABLED, true);
        weatherLocationName = prefs.getString(WEATHER_LOCATION_NAME, "");
        weatherTimezone = prefs.getString(WEATHER_TIMEZONE, "auto");
        weatherLatitude = parseDouble(prefs.getString(WEATHER_LATITUDE, null));
        weatherLongitude = parseDouble(prefs.getString(WEATHER_LONGITUDE, null));
        alarmEnabled = prefs.getBoolean(AlarmHelper.PREF_ALARM_ENABLED, false);
        alarmHour = prefs.getInt(AlarmHelper.PREF_ALARM_HOUR, 7);
        alarmMinute = prefs.getInt(AlarmHelper.PREF_ALARM_MINUTE, 0);
        alarmRepeat = prefs.getBoolean(AlarmHelper.PREF_ALARM_REPEAT, true);
        pomodoroFocusMinutes = prefs.getInt(PomodoroHelper.PREF_FOCUS_MINUTES,
                PomodoroHelper.DEFAULT_FOCUS_MINUTES);
        pomodoroShortBreakMinutes = prefs.getInt(PomodoroHelper.PREF_SHORT_BREAK_MINUTES,
                PomodoroHelper.DEFAULT_SHORT_BREAK_MINUTES);
        pomodoroLongBreakMinutes = prefs.getInt(PomodoroHelper.PREF_LONG_BREAK_MINUTES,
                PomodoroHelper.DEFAULT_LONG_BREAK_MINUTES);
        pomodoroDisplayMode = PomodoroHelper.normalizeDisplayMode(prefs.getInt(
                PomodoroHelper.PREF_DISPLAY_MODE, PomodoroHelper.DISPLAY_MODE_ORIGINAL));

        Set<String> saved = prefs.getStringSet(PHOTO_FOLDERS, null);
        if (saved != null && !saved.isEmpty()) {
            for (String source : saved) {
                String normalized = normalizeFolderSource(source);
                selectedFolders.add(normalized);
                initialFolderSources.add(normalized);
            }
        }

        setContentView(buildInterface());
        showDirectory();
    }

    @Override
    protected void onDestroy() {
        if (!settingsSaved) discardPendingTreeGrants();
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (awaitingExactAlarmPermission && AlarmHelper.canScheduleExactAlarms(this)
                && alarmEnabledCheck != null) {
            awaitingExactAlarmPermission = false;
            alarmEnabledCheck.setChecked(true);
            alarmEnabledCheck.performClick();
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    @Override
    @android.annotation.SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        if (!navigateToParentDirectory()) {
            discardPendingTreeGrants();
            super.onBackPressed();
        }
    }

    private void registerPredictiveBackCallback() {
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new android.window.OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            if (!navigateToParentDirectory()) {
                                discardPendingTreeGrants();
                                finish();
                            }
                        }
                    });
        }
    }

    private boolean navigateToParentDirectory() {
        if (currentDirectory != null) {
            if (isRootStorageVolume(currentDirectory)) {
                currentDirectory = null;
                showDirectory();
                return true;
            }
            File parent = currentDirectory.getParentFile();
            if (parent != null) {
                currentDirectory = parent;
                showDirectory();
                return true;
            }
        }
        return false;
    }

    private boolean isRootStorageVolume(File file) {
        if (file == null) return true;
        List<StorageVolumeItem> volumes = getAvailableStorageVolumes();
        String path = file.getAbsolutePath();
        for (StorageVolumeItem item : volumes) {
            if (item.rootDir.getAbsolutePath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private List<StorageVolumeItem> getAvailableStorageVolumes() {
        List<StorageVolumeItem> volumes = new ArrayList<StorageVolumeItem>();
        Set<String> addedPaths = new HashSet<String>();

        File internalAssets = new File(getFilesDir(), "數位風景");
        if (internalAssets.exists()) {
            volumes.add(new StorageVolumeItem("內建精選數位風景", internalAssets));
            addedPaths.add(internalAssets.getAbsolutePath());
        }

        File primary = Environment.getExternalStorageDirectory();
        if (primary != null && primary.exists()) {
            volumes.add(new StorageVolumeItem("內建儲存空間", primary));
            addedPaths.add(primary.getAbsolutePath());
        }

        if (Build.VERSION.SDK_INT >= 19) {
            File[] appDirs = getExternalFilesDirs(null);
            if (appDirs != null) {
                for (File appDir : appDirs) {
                    if (appDir == null) continue;
                    String path = appDir.getAbsolutePath();
                    int idx = path.indexOf("/Android/data/");
                    if (idx > 0) {
                        String rootPath = path.substring(0, idx);
                        File rootDir = new File(rootPath);
                        String cPath = rootDir.getAbsolutePath();
                        if (rootDir.exists() && !addedPaths.contains(cPath)) {
                            addedPaths.add(cPath);
                            volumes.add(new StorageVolumeItem("外接儲存空間（" + rootDir.getName() + "）", rootDir));
                        }
                    }
                }
            }
        }

        File storageDir = new File("/storage");
        if (storageDir.exists() && storageDir.isDirectory()) {
            File[] subDirs = storageDir.listFiles();
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    if (subDir == null || !subDir.isDirectory() || subDir.getName().startsWith(".")) continue;
                    String name = subDir.getName();
                    if ("emulated".equals(name) || "self".equals(name)) {
                        File primary0 = new File(subDir, "0");
                        String c0 = primary0.getAbsolutePath();
                        if (primary0.exists() && !addedPaths.contains(c0)) {
                            addedPaths.add(c0);
                            volumes.add(new StorageVolumeItem("內建儲存空間", primary0));
                        }
                    } else {
                        String cPath = subDir.getAbsolutePath();
                        if (!addedPaths.contains(cPath)) {
                            addedPaths.add(cPath);
                            volumes.add(new StorageVolumeItem("外接儲存空間（" + name + "）", subDir));
                        }
                    }
                }
            }
        }
        return volumes;
    }

    private View buildInterface() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BACKGROUND);
        applySystemBarInsets(screen);

        ScrollView outerScroll = new ScrollView(this);
        outerScroll.setFillViewport(true);
        outerScroll.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(18));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = text("LITTLECLOCK  ·  4.2", 11, ACCENT);
        if (Build.VERSION.SDK_INT >= 21) eyebrow.setLetterSpacing(0.18f);
        TextView title = text("設定", 28, PRIMARY);
        TextView subtitle = text("依類別展開，需要時再調整", 13, SECONDARY);
        titleStack.addView(eyebrow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(20)));
        titleStack.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        titleStack.addView(subtitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(24)));

        selectionText = text("", 14, ACCENT);
        selectionText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        titleRow.addView(titleStack, new LinearLayout.LayoutParams(0, dp(84), 1));
        root.addView(titleRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(88)));

        LinearLayout profileSection = addSettingsSection(
                root,
                "使用情境",
                "切換預設、桌面、床頭或展示設定",
                true);
        profileSpinner = addSpinner(
                profileSection,
                "目前情境",
                DisplayProfileStore.labels(),
                DisplayProfileStore.indexOf(activeProfileId));
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                if (!profileSpinnerReady) {
                    profileSpinnerReady = true;
                    return;
                }
                String[] ids = DisplayProfileStore.ids();
                if (position < 0 || position >= ids.length) return;
                String target = ids[position];
                if (target.equals(activeProfileId)) return;
                profileSpinner.setSelection(DisplayProfileStore.indexOf(activeProfileId));
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("切換情境")
                        .setMessage("切換後會立即載入此情境；目前尚未套用的設定將捨棄。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("切換", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                discardPendingTreeGrants();
                                android.content.SharedPreferences preferences =
                                        getSharedPreferences(PREFERENCES, MODE_PRIVATE);
                                DisplayProfileStore.apply(preferences, target);
                                activeProfileId = target;
                                settingsSaved = true;
                                finish();
                                startActivity(new Intent(SettingsActivity.this,
                                        SettingsActivity.class));
                            }
                        })
                        .show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        TextView profileHint = text(
                "切換情境會立即載入已保存內容，尚未儲存的修改將捨棄。",
                12, SECONDARY);
        profileHint.setPadding(dp(2), 0, dp(2), dp(4));
        profileSection.addView(profileHint);

        TextView categoryHint = text(
                "每個類別都可獨立展開；變更會在按下「儲存並返回」後一次套用。",
                13, SECONDARY);
        categoryHint.setPadding(dp(4), dp(4), dp(4), dp(8));
        root.addView(categoryHint);

        final LinearLayout albumSection = addSettingsSection(
                root,
                "相簿與播放",
                "相片來源、停留時間、順序、轉場與顯示方式",
                true);
        final LinearLayout clockSection = addSettingsSection(
                root,
                "時鐘與版面",
                "時間、日期、秒數、字型、底板與排版位置",
                false);
        final LinearLayout weatherSection = addSettingsSection(
                root,
                "天氣",
                "地點、字型、精簡排列與延伸資訊",
                false);
        final LinearLayout powerSection = addSettingsSection(
                root,
                "電源與顯示",
                "效能、亮屏、電量、亮度與夜間暗屏",
                false);
        final LinearLayout focusSection = addSettingsSection(
                root,
                "番茄鐘與鬧鐘",
                "專注時間、休息時間與每日鬧鐘",
                false);
        final LinearLayout effectsSection = addSettingsSection(
                root,
                "相片效果與維護",
                "智慧取景、色彩、相框、防烙印與隱藏相片",
                false);

        // 常用播放與顯示設定。
        LinearLayout mainSection = albumSection;

        LinearLayout intervalHeader = new LinearLayout(this);
        intervalHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView intervalTitle = text("相片停留時間", 17, PRIMARY);

        intervalDisplay = text(selectedInterval + " 秒", 18, ACCENT);
        intervalDisplay.setGravity(Gravity.END);

        intervalHeader.addView(intervalTitle, new LinearLayout.LayoutParams(0, dp(32), 1));
        intervalHeader.addView(intervalDisplay, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)));
        mainSection.addView(intervalHeader);

        intervalSeekBar = new SeekBar(this);
        intervalSeekBar.setMax(INTERVAL_STEPS.length - 1);
        intervalSeekBar.setProgress(intervalStepIndex(selectedInterval));
        intervalSeekBar.setPadding(dp(8), 0, dp(8), 0);
        intervalSeekBar.setProgressDrawable(intervalTrackDrawable());
        intervalSeekBar.setThumb(intervalThumbDrawable());
        if (Build.VERSION.SDK_INT >= 21) {
            intervalSeekBar.setSplitTrack(false);
        }
        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setInterval(INTERVAL_STEPS[Math.max(0,
                        Math.min(INTERVAL_STEPS.length - 1, progress))]);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mainSection.addView(intervalSeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(32)));

        LinearLayout intervalLabels = new LinearLayout(this);
        intervalLabels.setPadding(0, 0, 0, dp(4));
        for (int seconds : INTERVAL_STEPS) {
            TextView label = text(Integer.toString(seconds), 12, SECONDARY);
            label.setGravity(Gravity.CENTER);
            intervalLabels.addView(label, new LinearLayout.LayoutParams(0, dp(18), 1));
        }
        mainSection.addView(intervalLabels);

        transitionSpinner = addSpinner(
                mainSection,
                "相片轉場",
                new String[] { "淡入", "水平滑動", "垂直滑動", "縮放", "翻轉", "旋轉" },
                Math.max(0, Math.min(5, selectedTransition)));

        playbackOrderSpinner = addSpinner(
                mainSection,
                "播放順序",
                new String[] { "隨機（每輪不重複）", "檔名 A → Z", "檔名 Z → A",
                        "最新優先", "最舊優先" },
                playbackOrder);

        displayModeSpinner = addSpinner(
                mainSection,
                "相片顯示",
                new String[] { "填滿畫面", "完整顯示", "柔和背景（選用）" },
                Math.max(0, Math.min(2, selectedDisplayMode)));

        mainSection = weatherSection;

        weatherEnabledCheck = checkBox("顯示天氣（僅 Wi-Fi）", weatherEnabled);
        mainSection.addView(weatherEnabledCheck);

        final LinearLayout weatherOptions = new LinearLayout(this);
        weatherOptions.setOrientation(LinearLayout.VERTICAL);
        weatherOptions.setPadding(dp(8), 0, dp(4), dp(4));
        weatherOptions.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);

        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        weatherLocationText = text("", 14, SECONDARY);
        updateWeatherLocationText();
        Button chooseLocation = button("選擇地點", PANEL);
        chooseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWeatherLocationSearch();
            }
        });
        locationRow.addView(weatherLocationText, new LinearLayout.LayoutParams(0, dp(40), 1));
        locationRow.addView(chooseLocation, new LinearLayout.LayoutParams(dp(110), dp(38)));
        weatherOptions.addView(locationRow);

        weatherLocationCheck = checkBox("顯示英文地名", weatherShowLocation);
        weatherOptions.addView(weatherLocationCheck);

        weatherMinimalLocationCheck = checkBox(
                "地名只顯示最小單位", weatherMinimalLocation);
        weatherMinimalLocationCheck.setEnabled(weatherShowLocation);
        weatherMinimalLocationCheck.setAlpha(weatherShowLocation ? 1.0f : 0.45f);
        weatherOptions.addView(weatherMinimalLocationCheck);

        weatherFontSpinner = addSpinner(
                weatherOptions,
                "天氣字型",
                FontManager.getDisplayNames(fontOptions),
                FontManager.findOptionIndex(fontOptions, selectedWeatherFontId));

        weatherCompactCheck = checkBox(
                "精簡排列（無地名時與日期同列）", weatherCompactMode);
        weatherCompactCheck.setEnabled(!weatherShowLocation);
        weatherCompactCheck.setAlpha(weatherShowLocation ? 0.45f : 1.0f);
        weatherOptions.addView(weatherCompactCheck);
        weatherLocationCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean available = !weatherLocationCheck.isChecked();
                weatherCompactCheck.setEnabled(available);
                weatherCompactCheck.setAlpha(available ? 1.0f : 0.45f);
                boolean minimalAvailable = weatherLocationCheck.isChecked();
                weatherMinimalLocationCheck.setEnabled(minimalAvailable);
                weatherMinimalLocationCheck.setAlpha(minimalAvailable ? 1.0f : 0.45f);
            }
        });
        weatherExtendedCheck = checkBox("顯示延伸天氣資訊", weatherExtendedEnabled);
        weatherOptions.addView(weatherExtendedCheck);
        final LinearLayout weatherExtendedOptions = new LinearLayout(this);
        weatherExtendedOptions.setOrientation(LinearLayout.VERTICAL);
        weatherExtendedOptions.setPadding(dp(16), 0, 0, 0);
        weatherExtendedOptions.setVisibility(
                weatherExtendedEnabled ? View.VISIBLE : View.GONE);
        weatherExtendedForecastCheck = checkBox(
                "顯示未來三小時", weatherExtendedForecastEnabled);
        weatherExtendedDaylightCheck = checkBox(
                "顯示日出／日落", weatherExtendedDaylightEnabled);
        weatherExtendedOptions.addView(weatherExtendedForecastCheck);
        weatherExtendedOptions.addView(weatherExtendedDaylightCheck);
        weatherOptions.addView(weatherExtendedOptions);
        weatherExtendedCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                weatherExtendedOptions.setVisibility(
                        weatherExtendedCheck.isChecked() ? View.VISIBLE : View.GONE);
            }
        });
        TextView weatherExtendedHint = text(
                "可分別顯示未來三小時與日出／日落（額外資料）", 12, SECONDARY);
        weatherExtendedHint.setPadding(dp(4), 0, dp(4), dp(4));
        weatherOptions.addView(weatherExtendedHint);
        TextView weatherCredit = text("天氣資料：Open-Meteo", 12, SECONDARY);
        weatherCredit.setPadding(dp(4), 0, dp(4), dp(4));
        weatherOptions.addView(weatherCredit);
        mainSection.addView(weatherOptions);

        weatherEnabledCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                weatherOptions.setVisibility(
                        weatherEnabledCheck.isChecked() ? View.VISIBLE : View.GONE);
            }
        });

        mainSection = powerSection;
        performanceModeSpinner = addSpinner(
                mainSection,
                "效能模式",
                new String[] { "低耗電（建議）", "標準", "展演（高耗能）" },
                PerformanceModePolicy.normalize(performanceMode));
        addInlineSectionHeader(mainSection, "螢幕與電量", "POWER");

        mainSection = focusSection;
        addInlineSectionHeader(mainSection, "番茄鐘", "POMODORO");
        pomodoroDisplayModeSpinner = addSpinner(
                mainSection,
                "番茄鐘顯示",
                new String[] { "原始模式", "進階模式" },
                pomodoroDisplayMode);
        addPomodoroDurationRow(mainSection, "專注", 0);
        addPomodoroDurationRow(mainSection, "短休息", 1);
        addPomodoroDurationRow(mainSection, "長休息", 2);

        addDivider(mainSection);
        addInlineSectionHeader(mainSection, "鬧鐘", "ALARM");
        alarmEnabledCheck = checkBox("開啟鬧鐘", alarmEnabled);
        mainSection.addView(alarmEnabledCheck);

        final LinearLayout alarmOptions = new LinearLayout(this);
        alarmOptions.setOrientation(LinearLayout.VERTICAL);
        alarmOptions.setPadding(dp(8), 0, dp(8), dp(6));
        alarmOptions.setVisibility(alarmEnabled ? View.VISIBLE : View.GONE);

        alarmTimeDisplay = text("", 16, ACCENT);
        alarmTimeDisplay.setPadding(dp(8), dp(4), dp(8), dp(4));
        updateAlarmTimeText();

        LinearLayout alarmTimeControls = new LinearLayout(this);
        alarmTimeControls.setGravity(Gravity.CENTER_VERTICAL);
        alarmTimeControls.setPadding(dp(8), 0, dp(8), dp(6));

        TextView hourLabel = text("小時", 14, SECONDARY);
        Button hourMinus = button("−", PANEL);
        hourMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmHour = (alarmHour + 23) % 24;
                updateAlarmTimeText();
            }
        });
        Button hourPlus = button("+", PANEL);
        hourPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmHour = (alarmHour + 1) % 24;
                updateAlarmTimeText();
            }
        });

        TextView minLabel = text("分鐘", 14, SECONDARY);
        minLabel.setPadding(dp(12), 0, 0, 0);
        Button minMinus = button("−", PANEL);
        minMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmMinute = (alarmMinute + 55) % 60;
                updateAlarmTimeText();
            }
        });
        Button minPlus = button("+", PANEL);
        minPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmMinute = (alarmMinute + 5) % 60;
                updateAlarmTimeText();
            }
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        btnParams.setMargins(dp(4), 0, dp(4), 0);

        alarmTimeControls.addView(hourLabel);
        alarmTimeControls.addView(hourMinus, btnParams);
        alarmTimeControls.addView(hourPlus, btnParams);
        alarmTimeControls.addView(minLabel);
        alarmTimeControls.addView(minMinus, btnParams);
        alarmTimeControls.addView(minPlus, btnParams);

        alarmRepeatCheck = checkBox("每天重複響鈴", alarmRepeat);

        alarmOptions.addView(alarmTimeDisplay);
        alarmOptions.addView(alarmTimeControls);
        alarmOptions.addView(alarmRepeatCheck);

        mainSection.addView(alarmOptions);

        alarmEnabledCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alarmEnabledCheck.isChecked() && !AlarmHelper.canScheduleExactAlarms(SettingsActivity.this)) {
                    alarmEnabledCheck.setChecked(false);
                    awaitingExactAlarmPermission = true;
                    if (Build.VERSION.SDK_INT >= 31) {
                        try {
                            android.content.Intent intent = new android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception ignored) { }
                    }
                }
                if (alarmEnabledCheck.isChecked()) {
                    requestAlarmNotificationPermissionIfNeeded();
                    offerFullScreenAlarmAccessIfNeeded();
                }
                alarmOptions.setVisibility(
                        alarmEnabledCheck.isChecked() ? View.VISIBLE : View.GONE);
            }
        });

        mainSection = clockSection;
        clockTimeCheck = checkBox("顯示時間", clockTimeEnabled);
        mainSection.addView(clockTimeCheck);

        clockDateCheck = checkBox("顯示日期", clockDateEnabled);
        mainSection.addView(clockDateCheck);

        secondsModeSpinner = addSpinner(
                mainSection,
                "顯示秒數",
                new String[] { "不顯示", "充電時顯示", "永遠顯示" },
                secondsMode);

        fontSpinner = addSpinner(
                mainSection,
                "時間字型",
                FontManager.getDisplayNames(fontOptions),
                FontManager.findOptionIndex(fontOptions, selectedFontId));

        dateFontSpinner = addSpinner(
                mainSection,
                "日期字型",
                FontManager.getDisplayNames(fontOptions),
                FontManager.findOptionIndex(fontOptions, selectedDateFontId));

        batteryDisplayModeSpinner = addSpinner(
                powerSection,
                "電量顯示",
                new String[] { "不顯示", "永遠顯示", "只在未充電時顯示" },
                batteryDisplayMode);

        keepScreenModeSpinner = addSpinner(
                powerSection,
                "保持螢幕亮起",
                new String[] { "永遠保持亮屏", "只有充電時", "跟隨系統休眠" },
                keepScreenMode);

        lowBatteryGuardCheck = checkBox("低電量時降低相片耗電", lowBatteryGuard);
        powerSection.addView(lowBatteryGuardCheck);

        clockLayoutModeSpinner = addSpinner(
                mainSection,
                "排版方式",
                new String[] {
                        "原始排版（綁定）",
                        "自由排版（自行調整）"
                },
                clockLayoutMode);

        originalScaleModeSpinner = addSpinner(
                mainSection,
                "原始排版縮放效果",
                new String[] {
                        "整體綁定",
                        "自動避讓（原始效果）"
                },
                originalScaleMode);
        clockLayoutModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                updateOriginalScaleModeAvailability();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateOriginalScaleModeAvailability();
            }
        });
        updateOriginalScaleModeAvailability();

        clockBgCheck = checkBox("時間底板", clockBgEnabled);
        mainSection.addView(clockBgCheck);

        Button resetClockLayoutButton = button("恢復時鐘預設大小與位置", PANEL_RAISED);
        resetClockLayoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("恢復時鐘預設")
                        .setMessage("將恢復時間、日期、天氣的預設大小、比例與位置。字型選擇不會改變。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("恢復", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resetClockLayoutRequested = true;
                                clockLayoutModeSpinner.setSelection(
                                        PhotoClockActivity.CLOCK_LAYOUT_MODE_ORIGINAL);
                                originalScaleModeSpinner.setSelection(
                                        PhotoClockActivity.CLOCK_ORIGINAL_SCALE_LINKED);
                                Toast.makeText(SettingsActivity.this,
                                        "已標記恢復，按下儲存後套用", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        });
        mainSection.addView(resetClockLayoutButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));

        mainSection = powerSection;
        addDivider(mainSection);
        addInlineSectionHeader(mainSection, "夜間模式", "NIGHT");
        nightModeCheck = checkBox("排程暗屏", nightModeEnabled);
        mainSection.addView(nightModeCheck);

        final LinearLayout nightOptions = new LinearLayout(this);
        nightOptions.setOrientation(LinearLayout.VERTICAL);
        nightOptions.setVisibility(nightModeEnabled ? View.VISIBLE : View.GONE);

        nightScheduleText = text("", 15, ACCENT);
        nightScheduleText.setPadding(dp(8), dp(4), dp(8), 0);
        nightOptions.addView(nightScheduleText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(32)));

        LinearLayout nightControls = new LinearLayout(this);
        nightControls.setGravity(Gravity.CENTER_VERTICAL);
        nightControls.setPadding(dp(8), 0, dp(8), dp(6));

        TextView startLabel = text("開始", 14, SECONDARY);
        TextView endLabel = text("結束", 14, SECONDARY);

        Button startMinus = button("−", PANEL);
        startMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightStartHour = (nightStartHour + 23) % 24;
                updateNightText();
            }
        });

        Button startPlus = button("+", PANEL);
        startPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightStartHour = (nightStartHour + 1) % 24;
                updateNightText();
            }
        });

        Button endMinus = button("−", PANEL);
        endMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightEndHour = (nightEndHour + 23) % 24;
                updateNightText();
            }
        });

        Button endPlus = button("+", PANEL);
        endPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightEndHour = (nightEndHour + 1) % 24;
                updateNightText();
            }
        });

        nightControls.addView(startLabel, new LinearLayout.LayoutParams(0, dp(36), 1));
        nightControls.addView(startMinus, new LinearLayout.LayoutParams(dp(36), dp(32)));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(dp(36), dp(32));
        p1.setMargins(dp(4), 0, 0, 0);
        nightControls.addView(startPlus, p1);

        LinearLayout.LayoutParams endLabelParams = new LinearLayout.LayoutParams(0, dp(32), 1);
        endLabelParams.setMargins(dp(16), 0, 0, 0);
        nightControls.addView(endLabel, endLabelParams);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(dp(36), dp(32));
        p2.setMargins(dp(12), 0, 0, 0);
        nightControls.addView(endMinus, p2);

        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(dp(36), dp(32));
        p3.setMargins(dp(4), 0, 0, 0);
        nightControls.addView(endPlus, p3);

        nightOptions.addView(nightControls);
        mainSection.addView(nightOptions);
        nightModeCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightOptions.setVisibility(
                        nightModeCheck.isChecked() ? View.VISIBLE : View.GONE);
            }
        });
        updateNightText();

        final LinearLayout advancedOptions = effectsSection;

        adaptiveColorCheck = checkBox("依相片調整時間色彩", adaptiveColorEnabled);
        advancedOptions.addView(adaptiveColorCheck);

        polaroidFrameCheck = checkBox("白色相框", polaroidFrameEnabled);
        advancedOptions.addView(polaroidFrameCheck);

        smartFocusCheck = checkBox("智慧取景", smartFocusEnabled);
        advancedOptions.addView(smartFocusCheck);

        burnInCheck = checkBox("防烙印微移", burnInEnabled);
        advancedOptions.addView(burnInCheck);

        if (hasLightSensor()) {
            autoBrightnessCheck = checkBox("環境光自動亮度", autoBrightnessEnabled);
            powerSection.addView(autoBrightnessCheck);
        }

        favoritesOnlyCheck = checkBox("只播放收藏相片", favoritesOnly);
        albumSection.addView(favoritesOnlyCheck);

        Button clearHidden = button("重新顯示已隱藏相片", PANEL);
        clearHidden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearHiddenRequested = !clearHiddenRequested;
                clearHidden.setText(clearHiddenRequested
                        ? "已安排重新顯示（套用後生效）"
                        : "重新顯示已隱藏相片");
            }
        });
        advancedOptions.addView(clearHidden, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        // 相簿資料夾選擇。
        addDivider(albumSection);
        LinearLayout albumHeader = new LinearLayout(this);
        albumHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView albumTitle = text("相簿來源", 17, PRIMARY);
        albumHeader.addView(albumTitle, new LinearLayout.LayoutParams(0, dp(36), 1));
        albumHeader.addView(selectionText, new LinearLayout.LayoutParams(0, dp(36), 2));
        LinearLayout.LayoutParams albumHeaderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        albumHeaderParams.setMargins(dp(2), dp(8), dp(2), 0);
        albumSection.addView(albumHeader, albumHeaderParams);

        LinearLayout pathRow = new LinearLayout(this);
        pathRow.setGravity(Gravity.CENTER_VERTICAL);
        Button up = button(Build.VERSION.SDK_INT >= 21 ? "新增相簿" : "上一層", PANEL);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 21) {
                    choosePhotoTree();
                } else {
                    onBackPressed();
                }
            }
        });
        pathText = text("", 15, SECONDARY);
        pathText.setPadding(dp(14), 0, 0, 0);
        pathRow.addView(up, new LinearLayout.LayoutParams(
                Build.VERSION.SDK_INT >= 21 ? dp(132) : dp(100), dp(40)));
        pathRow.addView(pathText, new LinearLayout.LayoutParams(0, dp(40), 1));
        albumSection.addView(pathRow);

        currentFolderCheck = new CheckBox(this);
        currentFolderCheck.setText("使用目前資料夾中的照片（包含子目錄）");
        currentFolderCheck.setTextColor(PRIMARY);
        currentFolderCheck.setTextSize(16);
        currentFolderCheck.setTypeface(uiTypeface);
        currentFolderCheck.setPadding(dp(4), dp(3), dp(6), dp(3));
        currentFolderCheck.setMinHeight(dp(36));
        currentFolderCheck.setMinimumHeight(dp(36));
        tintCheckBox(currentFolderCheck);
        currentFolderCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentDirectory != null) {
                    boolean isChecked = selectedFolders.contains(
                            FILE_SOURCE_PREFIX + currentDirectory.getAbsolutePath());
                    boolean hasChildSelected = isAnyChildSelected(currentDirectory);
                    if (!isChecked && hasChildSelected) {
                        setSelected(currentDirectory, true);
                    } else {
                        setSelected(currentDirectory, currentFolderCheck.isChecked());
                    }
                }
            }
        });
        albumSection.addView(currentFolderCheck, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        if (Build.VERSION.SDK_INT >= 21) {
            currentFolderCheck.setVisibility(View.GONE);
        }

        folderList = new LinearLayout(this);
        folderList.setOrientation(LinearLayout.VERTICAL);
        folderList.setBackground(panelBackground());
        folderList.setPadding(0, dp(8), 0, dp(8));
        albumSection.addView(folderList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        outerScroll.addView(root);
        screen.addView(outerScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout actionBar = new LinearLayout(this);
        actionBar.setGravity(Gravity.CENTER_VERTICAL);
        actionBar.setPadding(dp(16), dp(8), dp(16), dp(8));
        actionBar.setBackgroundColor(PANEL);

        Button cancel = button("取消", PANEL_RAISED);
        cancel.setContentDescription("放棄變更並返回時鐘");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discardPendingTreeGrants();
                finish();
            }
        });

        Button save = button("儲存並返回", ACTIVE_CHIP);
        save.setContentDescription("儲存全部設定並返回時鐘");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });

        actionBar.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 2);
        saveParams.setMargins(dp(10), 0, 0, 0);
        actionBar.addView(save, saveParams);
        screen.addView(actionBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));

        updateIntervalDisplay();
        return screen;
    }

    private void applySystemBarInsets(final View view) {
        if (Build.VERSION.SDK_INT < 20) {
            return;
        }
        final int initialLeft = view.getPaddingLeft();
        final int initialTop = view.getPaddingTop();
        final int initialRight = view.getPaddingRight();
        final int initialBottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View target, WindowInsets insets) {
                target.setPadding(
                        initialLeft + insets.getSystemWindowInsetLeft(),
                        initialTop + insets.getSystemWindowInsetTop(),
                        initialRight + insets.getSystemWindowInsetRight(),
                        initialBottom + insets.getSystemWindowInsetBottom());
                return insets;
            }
        });
        view.requestApplyInsets();
    }

    private void updateNightText() {
        if (nightScheduleText != null) {
            nightScheduleText.setText(String.format(Locale.TAIWAN,
                    "時段：%02d:00 ~ %02d:00", nightStartHour, nightEndHour));
        }
    }

    private void updateWeatherLocationText() {
        if (weatherLocationText != null) {
            weatherLocationText.setText(weatherLocationName.length() == 0
                    ? "尚未選擇地點" : weatherLocationName);
        }
    }

    private void showWeatherLocationSearch() {
        if (!WeatherClient.isWifiConnected(this)) {
            Toast.makeText(this, "請先連接 Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("例如 Taipei、Tainan");
        input.setTextColor(PRIMARY);
        input.setHintTextColor(SECONDARY);
        input.setTypeface(uiTypeface);
        input.setBackground(fieldBackground());
        input.setPadding(dp(18), dp(8), dp(18), dp(8));
        AlertDialog searchDialog = new AlertDialog.Builder(this)
                .setTitle("搜尋英文地名")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("搜尋", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String query = input.getText().toString().trim();
                        if (query.length() < 2) {
                            Toast.makeText(SettingsActivity.this,
                                    "請輸入至少兩個字元", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        searchWeatherLocations(query);
                    }
                })
                .create();
        searchDialog.show();
        styleDialog(searchDialog);
    }

    private void searchWeatherLocations(final String query) {
        Toast.makeText(this, "正在搜尋地點…", Toast.LENGTH_SHORT).show();
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<WeatherClient.LocationResult> results =
                            WeatherClient.searchLocations(query);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            showWeatherLocationResults(results);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                Toast.makeText(SettingsActivity.this,
                                        "搜尋失敗，請確認 Wi-Fi 與系統時間",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showWeatherLocationResults(final List<WeatherClient.LocationResult> results) {
        if (results.isEmpty()) {
            Toast.makeText(this, "找不到地點", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[results.size()];
        for (int i = 0; i < results.size(); i++) names[i] = results.get(i).displayName;
        AlertDialog resultsDialog = new AlertDialog.Builder(this)
                .setTitle("選擇地點")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WeatherClient.LocationResult selected = results.get(which);
                        weatherLocationName = selected.displayName;
                        weatherLatitude = selected.latitude;
                        weatherLongitude = selected.longitude;
                        weatherTimezone = selected.timezone;
                        weatherLocationChanged = true;
                        updateWeatherLocationText();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        resultsDialog.show();
        styleDialog(resultsDialog);
    }

    private double parseDouble(String value) {
        if (value == null) return Double.NaN;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private Spinner addSpinner(LinearLayout parent, String label, String[] items, int selectedIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(4), 0, dp(8));

        TextView labelView = text(label, 14, SECONDARY);
        labelView.setPadding(dp(2), 0, dp(2), dp(4));
        Spinner spinner = new Spinner(this);
        spinner.setBackground(fieldBackground());
        spinner.setPadding(dp(12), 0, dp(10), 0);
        ArrayAdapter<String> adapter = new CompactSpinnerAdapter(items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(items.length - 1, selectedIndex)));

        row.addView(labelView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(24)));
        row.addView(spinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return spinner;
    }

    private final class CompactSpinnerAdapter extends ArrayAdapter<String> {
        CompactSpinnerAdapter(String[] items) {
            super(SettingsActivity.this, android.R.layout.simple_spinner_item, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            compact(view, false);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            compact(view, true);
            return view;
        }

        private void compact(View view, boolean dropDown) {
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                textView.setTextSize(15);
                textView.setTextColor(dropDown ? PRIMARY : ACCENT);
                textView.setTypeface(uiTypeface);
                textView.setBackgroundColor(dropDown ? PANEL_RAISED : Color.TRANSPARENT);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setMinHeight(0);
                textView.setMinimumHeight(0);
                textView.setPadding(dp(dropDown ? 12 : 8), 0, dp(12), 0);
            }
            if (dropDown) {
                view.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
            }
        }
    }

    private CheckBox checkBox(String label, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setTextColor(PRIMARY);
        checkBox.setTextSize(16);
        checkBox.setTypeface(uiTypeface);
        checkBox.setChecked(checked);
        checkBox.setPadding(dp(4), dp(4), dp(6), dp(4));
        checkBox.setMinHeight(dp(44));
        checkBox.setMinimumHeight(dp(44));
        tintCheckBox(checkBox);
        return checkBox;
    }

    private boolean hasLightSensor() {
        SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        return manager != null && manager.getDefaultSensor(Sensor.TYPE_LIGHT) != null;
    }

    private void setInterval(int value) {
        selectedInterval = nearestIntervalStep(value);
        updateIntervalDisplay();
    }

    private int nearestIntervalStep(int value) {
        int best = INTERVAL_STEPS[0];
        int bestDistance = Math.abs(value - best);
        for (int step : INTERVAL_STEPS) {
            int distance = Math.abs(value - step);
            if (distance < bestDistance) {
                best = step;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int intervalStepIndex(int value) {
        int normalized = nearestIntervalStep(value);
        for (int i = 0; i < INTERVAL_STEPS.length; i++) {
            if (INTERVAL_STEPS[i] == normalized) return i;
        }
        return 0;
    }

    private Drawable intervalTrackDrawable() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(STROKE);
        background.setCornerRadius(dp(1));
        background.setSize(1, dp(2));

        GradientDrawable progress = new GradientDrawable();
        progress.setColor(ACCENT);
        progress.setCornerRadius(dp(1));
        progress.setSize(1, dp(2));

        LayerDrawable layers = new LayerDrawable(new Drawable[] {
                background,
                new ClipDrawable(progress, Gravity.START, ClipDrawable.HORIZONTAL)
        });
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        return layers;
    }

    private Drawable intervalThumbDrawable() {
        GradientDrawable thumb = new GradientDrawable();
        thumb.setColor(Color.TRANSPARENT);
        thumb.setSize(dp(1), dp(1));
        return thumb;
    }

    private void updateIntervalDisplay() {
        if (intervalDisplay != null) {
            intervalDisplay.setText(String.format(Locale.TAIWAN, "%d 秒", selectedInterval));
        }
        if (intervalSeekBar != null
                && intervalSeekBar.getProgress() != intervalStepIndex(selectedInterval)) {
            intervalSeekBar.setProgress(intervalStepIndex(selectedInterval));
        }
    }

    static String normalizeFolderSource(String source) {
        if (source == null || source.length() == 0) return "";
        if (source.startsWith(FILE_SOURCE_PREFIX) || source.startsWith(TREE_SOURCE_PREFIX)) {
            return source;
        }
        if (source.startsWith("content://")) {
            return TREE_SOURCE_PREFIX + source;
        }
        return FILE_SOURCE_PREFIX + new File(source).getAbsolutePath();
    }

    static String filePathFromSource(String source) {
        if (source == null) return null;
        if (source.startsWith(FILE_SOURCE_PREFIX)) {
            return source.substring(FILE_SOURCE_PREFIX.length());
        }
        if (!source.startsWith(TREE_SOURCE_PREFIX) && !source.startsWith("content://")) {
            return source;
        }
        return null;
    }

    static Uri treeUriFromSource(String source) {
        if (source == null) return null;
        if (source.startsWith(TREE_SOURCE_PREFIX)) {
            return Uri.parse(source.substring(TREE_SOURCE_PREFIX.length()));
        }
        return source.startsWith("content://") ? Uri.parse(source) : null;
    }

    private void choosePhotoTree() {
        if (Build.VERSION.SDK_INT < 21) {
            Toast.makeText(this, "此系統請使用下方資料夾清單", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PICK_PHOTO_TREE);
        } catch (Exception error) {
            Toast.makeText(this, "這台裝置沒有可用的系統檔案選擇器", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @android.annotation.TargetApi(21)
    @android.annotation.SuppressLint("WrongConstant")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_PHOTO_TREE || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri treeUri = data.getData();
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        boolean persistent = true;
        try {
            getContentResolver().takePersistableUriPermission(treeUri, flags);
        } catch (SecurityException ignored) {
            persistent = false;
        }
        String source = TREE_SOURCE_PREFIX + treeUri.toString();
        if (persistent && !initialFolderSources.contains(source)) {
            newlyGrantedTreeSources.add(source);
        }
        selectedFolders.add(source);
        Toast.makeText(this,
                persistent
                        ? "已加入相簿資料夾，請按「套用」儲存"
                        : "已加入資料夾；此裝置重新開啟後可能需要再次選取",
                Toast.LENGTH_LONG).show();
        showDirectory();
    }

    private void requestAlarmNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 4201);
        }
    }

    private void offerFullScreenAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < 34 || AlarmHelper.canUseFullScreenIntent(this)) return;
        new AlertDialog.Builder(this)
                .setTitle("允許全螢幕鬧鐘")
                .setMessage("這台裝置目前不允許完整鬧鐘畫面。開啟後，鎖定時仍可直接顯示關閉與貪睡。")
                .setNegativeButton("稍後", null)
                .setPositiveButton("前往設定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(
                                    android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception ignored) {
                            Toast.makeText(SettingsActivity.this,
                                    "請到系統通知設定允許全螢幕鬧鐘", Toast.LENGTH_LONG).show();
                        }
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4201 && (grantResults.length == 0
                || grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "未允許通知時，鬧鐘仍會震動，但提醒不會顯示在通知列", Toast.LENGTH_LONG).show();
        }
    }

    private void discardPendingTreeGrants() {
        if (pendingGrantsReleased || Build.VERSION.SDK_INT < 21) return;
        pendingGrantsReleased = true;
        for (String source : newlyGrantedTreeSources) {
            releaseTreePermission(treeUriFromSource(source));
        }
        newlyGrantedTreeSources.clear();
    }

    @android.annotation.TargetApi(21)
    private void releaseTreePermission(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().releasePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
            try {
                getContentResolver().releasePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignoredAgain) {
                // The provider may not offer a persistent permission; there is nothing to release.
            }
        }
    }

    private void showDirectory() {
        if (Build.VERSION.SDK_INT >= 21) {
            showModernFolderSources();
            return;
        }
        folderList.removeAllViews();

        if (currentDirectory == null) {
            pathText.setText("選擇儲存裝置");
            currentFolderCheck.setVisibility(View.GONE);

            // 顯示載入提示
            final TextView loading = text("正在掃描儲存裝置…", 15, SECONDARY);
            loading.setGravity(Gravity.CENTER);
            folderList.addView(loading, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(60)));

            // 在背景執行緒掃描儲存裝置，避免阻塞主執行緒
            networkExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final List<StorageVolumeItem> volumes = getAvailableStorageVolumes();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            folderList.removeAllViews();
                            if (volumes.isEmpty()) {
                                TextView empty = text("無法偵測到可用的儲存裝置", 15, SECONDARY);
                                empty.setGravity(Gravity.CENTER);
                                folderList.addView(empty, new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, dp(60)));
                            } else {
                                for (final StorageVolumeItem volume : volumes) {
                                    folderList.addView(storageVolumeRow(volume));
                                }
                            }
                            updateSelectionSummary();
                        }
                    });
                }
            });
            return;
        }

        currentFolderCheck.setVisibility(View.VISIBLE);
        pathText.setText(currentDirectory.getAbsolutePath());
        boolean isChecked = selectedFolders.contains(
                FILE_SOURCE_PREFIX + currentDirectory.getAbsolutePath());
        boolean hasChildSelected = isAnyChildSelected(currentDirectory);
        currentFolderCheck.setChecked(isChecked || hasChildSelected);
        currentFolderCheck.setAlpha(isChecked ? 1.0f : (hasChildSelected ? 0.5f : 1.0f));

        // 顯示載入提示
        final TextView loading = text("正在讀取資料夾…", 15, SECONDARY);
        loading.setGravity(Gravity.CENTER);
        folderList.addView(loading, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(60)));

        // 在背景執行緒掃描子資料夾
        final File dir = currentDirectory;
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                File[] entries = dir.listFiles();
                final List<File> directories = new ArrayList<File>();
                if (entries != null) {
                    for (File entry : entries) {
                        if (entry.isDirectory() && !entry.getName().startsWith(".")) {
                            directories.add(entry);
                        }
                    }
                }
                Collections.sort(directories, new Comparator<File>() {
                    @Override
                    public int compare(File left, File right) {
                        return left.getName().compareToIgnoreCase(right.getName());
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        folderList.removeAllViews();
                        if (directories.isEmpty()) {
                            TextView empty = text("這個位置沒有子資料夾", 15, SECONDARY);
                            empty.setGravity(Gravity.CENTER);
                            folderList.addView(empty, new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, dp(60)));
                        } else {
                            for (final File directory : directories) {
                                folderList.addView(folderRow(directory));
                            }
                        }
                        updateSelectionSummary();
                    }
                });
            }
        });
    }

    private View storageVolumeRow(final StorageVolumeItem volume) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(2), dp(8), dp(2));

        final CheckBox check = new CheckBox(this);
        check.setTypeface(uiTypeface);
        tintCheckBox(check);
        final boolean isChecked = selectedFolders.contains(
                FILE_SOURCE_PREFIX + volume.rootDir.getAbsolutePath());
        final boolean hasChildSelected = isAnyChildSelected(volume.rootDir);
        check.setChecked(isChecked || hasChildSelected);
        check.setAlpha(isChecked ? 1.0f : (hasChildSelected ? 0.5f : 1.0f));
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isChecked && hasChildSelected) {
                    setSelected(volume.rootDir, true);
                } else {
                    setSelected(volume.rootDir, check.isChecked());
                }
            }
        });
        row.addView(check, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView name = text(volume.label, 17, ACCENT);
        name.setPadding(dp(10), dp(4), dp(10), dp(4));
        name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentDirectory = volume.rootDir;
                showDirectory();
            }
        });
        row.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View folderRow(final File directory) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(2), dp(8), dp(2));

        final CheckBox check = new CheckBox(this);
        check.setTypeface(uiTypeface);
        tintCheckBox(check);
        final boolean isChecked = selectedFolders.contains(
                FILE_SOURCE_PREFIX + directory.getAbsolutePath());
        final boolean hasChildSelected = isAnyChildSelected(directory);
        check.setChecked(isChecked || hasChildSelected);
        check.setAlpha(isChecked ? 1.0f : (hasChildSelected ? 0.5f : 1.0f));
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isChecked && hasChildSelected) {
                    setSelected(directory, true);
                } else {
                    setSelected(directory, check.isChecked());
                }
            }
        });
        row.addView(check, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView name = text(directory.getName(), 17, PRIMARY);
        name.setPadding(dp(10), dp(4), dp(10), dp(4));
        name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentDirectory = directory;
                showDirectory();
            }
        });
        row.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private boolean isAnyChildSelected(File directory) {
        if (directory == null) return false;
        String prefix = directory.getAbsolutePath() + File.separator;
        for (String selected : selectedFolders) {
            String selectedPath = filePathFromSource(selected);
            if (selectedPath != null && selectedPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void setSelected(File directory, boolean selected) {
        String path = directory.getAbsolutePath();
        String source = FILE_SOURCE_PREFIX + path;
        if (selected) {
            selectedFolders.add(source);
        } else {
            selectedFolders.remove(source);
        }
        
        java.util.Iterator<String> it = selectedFolders.iterator();
        String prefix = path + File.separator;
        while (it.hasNext()) {
            String selectedPath = filePathFromSource(it.next());
            if (selectedPath != null && selectedPath.startsWith(prefix)) {
                it.remove();
            }
        }
        
        if (currentDirectory != null) {
            boolean isChecked = selectedFolders.contains(
                    FILE_SOURCE_PREFIX + currentDirectory.getAbsolutePath());
            boolean hasChildSelected = isAnyChildSelected(currentDirectory);
            currentFolderCheck.setChecked(isChecked || hasChildSelected);
            currentFolderCheck.setAlpha(isChecked ? 1.0f : (hasChildSelected ? 0.5f : 1.0f));
        }
        updateSelectionSummary();
        showDirectory();
    }

    private void updateSelectionSummary() {
        if (selectedFolders.isEmpty()) {
            selectionText.setText("尚未選擇相簿");
        } else {
            selectionText.setText(String.format(
                    Locale.TAIWAN, "已選 %d 個相簿", selectedFolders.size()));
        }
    }

    private void updateOriginalScaleModeAvailability() {
        if (clockLayoutModeSpinner == null || originalScaleModeSpinner == null) return;
        boolean original = clockLayoutModeSpinner.getSelectedItemPosition()
                == PhotoClockActivity.CLOCK_LAYOUT_MODE_ORIGINAL;
        originalScaleModeSpinner.setEnabled(original);
        originalScaleModeSpinner.setAlpha(original ? 1.0f : 0.45f);
    }

    private void saveSettings() {
        int fontIndex = Math.max(0, Math.min(
                fontOptions.size() - 1, fontSpinner.getSelectedItemPosition()));
        selectedFontId = fontOptions.get(fontIndex).id;
        int dateFontIndex = Math.max(0, Math.min(
                fontOptions.size() - 1, dateFontSpinner.getSelectedItemPosition()));
        selectedDateFontId = fontOptions.get(dateFontIndex).id;
        int weatherFontIndex = Math.max(0, Math.min(
                fontOptions.size() - 1, weatherFontSpinner.getSelectedItemPosition()));
        selectedWeatherFontId = fontOptions.get(weatherFontIndex).id;
        selectedTransition = Math.max(0, Math.min(5, transitionSpinner.getSelectedItemPosition()));
        selectedDisplayMode = Math.max(0, Math.min(2, displayModeSpinner.getSelectedItemPosition()));
        playbackOrder = PlaybackOrderPolicy.normalize(
                playbackOrderSpinner.getSelectedItemPosition());
        secondsMode = ClockSecondPolicy.normalize(secondsModeSpinner.getSelectedItemPosition());
        keepScreenMode = PowerStatePolicy.normalizeKeepAwakeMode(
                keepScreenModeSpinner.getSelectedItemPosition());
        batteryDisplayMode = Math.max(0, Math.min(2,
                batteryDisplayModeSpinner.getSelectedItemPosition()));
        lowBatteryGuard = lowBatteryGuardCheck.isChecked();
        clockLayoutMode = ClockLayoutPolicy.normalizeLayout(
                clockLayoutModeSpinner.getSelectedItemPosition());
        originalScaleMode = ClockLayoutPolicy.normalizeOriginalScale(
                originalScaleModeSpinner.getSelectedItemPosition());
        int legacySizeMode = ClockLayoutPolicy.legacySizeMode(
                clockLayoutMode, originalScaleMode);
        if (weatherEnabledCheck.isChecked()
                && (Double.isNaN(weatherLatitude) || Double.isNaN(weatherLongitude))) {
            Toast.makeText(this, "請先選擇天氣地點", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.SharedPreferences.Editor editor =
                getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putInt(PHOTO_INTERVAL_SECONDS, selectedInterval)
                .putBoolean(CLOCK_TIME_ENABLED, clockTimeCheck.isChecked())
                .putBoolean(CLOCK_DATE_ENABLED, clockDateCheck.isChecked())
                .putInt(PhotoClockActivity.CLOCK_LAYOUT_MODE, clockLayoutMode)
                .putInt(PhotoClockActivity.CLOCK_ORIGINAL_SCALE_MODE, originalScaleMode)
                .putInt(PhotoClockActivity.CLOCK_SIZE_MODE, legacySizeMode)
                .putBoolean(PhotoClockActivity.CLOCK_SIZES_LINKED,
                        ClockLayoutPolicy.isLinkedOriginal(clockLayoutMode, originalScaleMode))
                .putBoolean(CLOCK_BACKGROUND_ENABLED, clockBgCheck.isChecked())
                .putString(CLOCK_FONT_ID, selectedFontId)
                .putString(DATE_FONT_ID, selectedDateFontId)
                .putString(WEATHER_FONT_ID, selectedWeatherFontId)
                .putBoolean(NIGHT_MODE_ENABLED, nightModeCheck.isChecked())
                .putInt(NIGHT_START_HOUR, nightStartHour)
                .putInt(NIGHT_END_HOUR, nightEndHour)
                .putInt(TRANSITION_TYPE, selectedTransition)
                .putInt(PHOTO_DISPLAY_MODE, selectedDisplayMode)
                .putInt(PHOTO_PLAYBACK_ORDER, playbackOrder)
                .putInt(CLOCK_SECONDS_MODE, secondsMode)
                .putInt(KEEP_SCREEN_MODE, keepScreenMode)
                .putBoolean(LOW_BATTERY_GUARD, lowBatteryGuard)
                .putInt(BATTERY_DISPLAY_MODE, batteryDisplayMode)
                .putInt(PERFORMANCE_MODE, PerformanceModePolicy.normalize(
                        performanceModeSpinner.getSelectedItemPosition()))
                .putBoolean(LOW_POWER_MODE, performanceModeSpinner.getSelectedItemPosition()
                        == PerformanceModePolicy.ECO)
                .putBoolean(ADAPTIVE_COLOR_ENABLED, adaptiveColorCheck.isChecked())
                .putBoolean(POLAROID_FRAME_ENABLED, polaroidFrameCheck.isChecked())
                .putBoolean(SMART_FOCUS_ENABLED, smartFocusCheck.isChecked())
                .putBoolean(BURN_IN_ENABLED, burnInCheck.isChecked())
                .putBoolean(AUTO_BRIGHTNESS_ENABLED,
                        autoBrightnessCheck != null && autoBrightnessCheck.isChecked())
                .putBoolean(FAVORITES_ONLY, favoritesOnlyCheck.isChecked())
                .putBoolean(WEATHER_ENABLED, weatherEnabledCheck.isChecked())
                .putBoolean(WEATHER_SHOW_LOCATION, weatherLocationCheck.isChecked())
                .putBoolean(WEATHER_MINIMAL_LOCATION,
                        weatherMinimalLocationCheck.isChecked())
                .putBoolean(WEATHER_COMPACT_MODE, weatherCompactCheck.isChecked())
                .putBoolean(WEATHER_EXTENDED_ENABLED, weatherExtendedCheck.isChecked())
                .putBoolean(WEATHER_EXTENDED_FORECAST_ENABLED,
                        weatherExtendedForecastCheck.isChecked())
                .putBoolean(WEATHER_EXTENDED_DAYLIGHT_ENABLED,
                        weatherExtendedDaylightCheck.isChecked())
                .putString(WEATHER_LOCATION_NAME, weatherLocationName)
                .putString(WEATHER_LATITUDE, Double.toString(weatherLatitude))
                .putString(WEATHER_LONGITUDE, Double.toString(weatherLongitude))
                .putString(WEATHER_TIMEZONE, weatherTimezone)
                .putBoolean(AlarmHelper.PREF_ALARM_ENABLED, alarmEnabledCheck.isChecked())
                .putInt(AlarmHelper.PREF_ALARM_HOUR, alarmHour)
                .putInt(AlarmHelper.PREF_ALARM_MINUTE, alarmMinute)
                .putBoolean(AlarmHelper.PREF_ALARM_REPEAT, alarmRepeatCheck.isChecked())
                .putInt(PomodoroHelper.PREF_FOCUS_MINUTES, pomodoroFocusMinutes)
                .putInt(PomodoroHelper.PREF_SHORT_BREAK_MINUTES, pomodoroShortBreakMinutes)
                .putInt(PomodoroHelper.PREF_LONG_BREAK_MINUTES, pomodoroLongBreakMinutes)
                .putInt(PomodoroHelper.PREF_DISPLAY_MODE,
                        PomodoroHelper.normalizeDisplayMode(
                                pomodoroDisplayModeSpinner.getSelectedItemPosition()))
                .putStringSet(PHOTO_FOLDERS, new HashSet<String>(selectedFolders));
        if (resetClockLayoutRequested) {
            PhotoClockActivity.resetClockLayoutPreferences(editor);
        }
        if (clearHiddenRequested) {
            editor.remove(HIDDEN_PHOTOS);
        }
        if (weatherLocationChanged) {
            editor.remove(WEATHER_TEMPERATURE)
                    .remove(WEATHER_CODE)
                    .remove(WEATHER_IS_DAY)
                    .remove(WEATHER_UPDATED_AT)
                    .remove(WEATHER_EXTENDED_UPDATED_AT)
                    .remove(WEATHER_EXTENDED_FORECAST)
                    .remove(WEATHER_EXTENDED_SUNRISE_AT)
                    .remove(WEATHER_EXTENDED_SUNSET_AT);
        }
        editor.apply();
        DisplayProfileStore.captureActive(getSharedPreferences(PREFERENCES, MODE_PRIVATE));
        if (Build.VERSION.SDK_INT >= 21) {
            for (String source : initialFolderSources) {
                if (!selectedFolders.contains(source)
                        && !DisplayProfileStore.isReferencedByAnyProfile(
                                getSharedPreferences(PREFERENCES, MODE_PRIVATE), source)) {
                    releaseTreePermission(treeUriFromSource(source));
                }
            }
            for (String source : newlyGrantedTreeSources) {
                if (!selectedFolders.contains(source)
                        && !DisplayProfileStore.isReferencedByAnyProfile(
                                getSharedPreferences(PREFERENCES, MODE_PRIVATE), source)) {
                    releaseTreePermission(treeUriFromSource(source));
                }
            }
        }
        newlyGrantedTreeSources.clear();
        settingsSaved = true;
        AlarmHelper.updateAlarmSchedule(this);
        finish();
    }

    private void updateAlarmTimeText() {
        if (alarmTimeDisplay != null) {
            alarmTimeDisplay.setText(String.format(
                    Locale.US, "響鈴時間：%02d:%02d", alarmHour, alarmMinute));
        }
    }

    private void showModernFolderSources() {
        currentDirectory = null;
        currentFolderCheck.setVisibility(View.GONE);
        pathText.setText("支援內部儲存空間與 SD 卡");
        folderList.removeAllViews();
        if (selectedFolders.isEmpty()) {
            TextView empty = text("尚未選擇相簿，請使用上方系統選擇器", 15, SECONDARY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(16), dp(12), dp(16));
            folderList.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            for (final String source : new ArrayList<String>(selectedFolders)) {
                folderList.addView(modernFolderRow(source));
            }
        }
        updateSelectionSummary();
    }

    private View modernFolderRow(final String source) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(7), dp(8), dp(7));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(folderSourceName(source), 16, PRIMARY);
        String filePath = filePathFromSource(source);
        boolean appInternal = filePath != null
                && (filePath.equals(getFilesDir().getAbsolutePath())
                        || filePath.startsWith(getFilesDir().getAbsolutePath() + File.separator));
        TextView kind = text(treeUriFromSource(source) != null
                ? "系統授權資料夾"
                : (appInternal ? "APP 內建相簿" : "舊版路徑（建議重新選取）"),
                12, SECONDARY);
        labels.addView(name);
        labels.addView(kind);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button remove = button("移除", PANEL);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFolders.remove(source);
                showModernFolderSources();
            }
        });
        row.addView(remove, new LinearLayout.LayoutParams(dp(72), dp(36)));
        return row;
    }

    @android.annotation.TargetApi(21)
    private String folderSourceName(String source) {
        String filePath = filePathFromSource(source);
        if (filePath != null) {
            File file = new File(filePath);
            return file.getName().length() == 0 ? filePath : file.getName();
        }
        Uri uri = treeUriFromSource(source);
        if (uri == null) return "相簿資料夾";
        try {
            String documentId = DocumentsContract.getTreeDocumentId(uri);
            int colon = documentId.lastIndexOf(':');
            String name = colon >= 0 ? documentId.substring(colon + 1) : documentId;
            if (name.length() > 0) return Uri.decode(name);
        } catch (Exception ignored) {
        }
        return "系統相簿資料夾";
    }

    private void addPomodoroDurationRow(LinearLayout parent, String label, final int type) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), dp(2));
        TextView name = text(label, 14, SECONDARY);
        final TextView value = text(pomodoroMinutesForType(type) + " 分鐘", 16, ACCENT);
        value.setGravity(Gravity.CENTER);
        Button minus = button("−", PANEL);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPomodoroMinutes(type, pomodoroMinutesForType(type) - pomodoroStepForType(type));
                value.setText(pomodoroMinutesForType(type) + " 分鐘");
            }
        });
        Button plus = button("+", PANEL);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPomodoroMinutes(type, pomodoroMinutesForType(type) + pomodoroStepForType(type));
                value.setText(pomodoroMinutesForType(type) + " 分鐘");
            }
        });
        row.addView(name, new LinearLayout.LayoutParams(0, dp(36), 1));
        row.addView(minus, new LinearLayout.LayoutParams(dp(36), dp(32)));
        row.addView(value, new LinearLayout.LayoutParams(dp(76), dp(32)));
        row.addView(plus, new LinearLayout.LayoutParams(dp(36), dp(32)));
        parent.addView(row);
    }

    private int pomodoroMinutesForType(int type) {
        return type == 1 ? pomodoroShortBreakMinutes
                : type == 2 ? pomodoroLongBreakMinutes : pomodoroFocusMinutes;
    }

    private int pomodoroStepForType(int type) {
        return type == 1 ? 1 : 5;
    }

    private void setPomodoroMinutes(int type, int minutes) {
        int bounded = Math.max(1, Math.min(180, minutes));
        if (type == 1) {
            pomodoroShortBreakMinutes = bounded;
        } else if (type == 2) {
            pomodoroLongBreakMinutes = bounded;
        } else {
            pomodoroFocusMinutes = bounded;
        }
    }

    private TextView text(String content, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(uiTypeface);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(PRIMARY);
        button.setTextSize(15);
        button.setTypeface(uiTypeface);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(13));
        drawable.setStroke(dp(1), color == ACTIVE_CHIP ? ACCENT : STROKE);
        button.setBackground(drawable);
        return button;
    }

    private GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(PANEL);
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), STROKE);
        return drawable;
    }

    private GradientDrawable fieldBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(PANEL_RAISED);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), STROKE);
        return drawable;
    }

    private LinearLayout addSettingsSection(LinearLayout parent, final String title,
            String summary, boolean expanded) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(panelBackground());

        final LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(12), dp(10));
        header.setClickable(true);
        header.setFocusable(true);
        header.setMinimumHeight(dp(72));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 18, PRIMARY);
        TextView summaryView = text(summary, 13, SECONDARY);
        summaryView.setMaxLines(2);
        labels.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));
        labels.addView(summaryView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        final TextView action = text(expanded ? "收合" : "展開", 13, ACCENT);
        action.setGravity(Gravity.CENTER);
        header.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(action, new LinearLayout.LayoutParams(dp(54), dp(44)));

        final View divider = new View(this);
        divider.setBackgroundColor(STROKE);
        divider.setVisibility(expanded ? View.VISIBLE : View.GONE);

        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(10), dp(16), dp(16));
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);

        header.setContentDescription(title + (expanded ? "，已展開" : "，已收合"));
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean show = body.getVisibility() != View.VISIBLE;
                body.setVisibility(show ? View.VISIBLE : View.GONE);
                divider.setVisibility(show ? View.VISIBLE : View.GONE);
                action.setText(show ? "收合" : "展開");
                header.setContentDescription(title + (show ? "，已展開" : "，已收合"));
            }
        });

        card.addView(header);
        card.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        card.addView(body);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(5), 0, dp(7));
        parent.addView(card, cardParams);
        return body;
    }

    private void addInlineSectionHeader(LinearLayout parent, String title, String caption) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = text(title, 17, PRIMARY);
        TextView captionView = text(caption, 10, ACCENT);
        captionView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        if (Build.VERSION.SDK_INT >= 21) captionView.setLetterSpacing(0.12f);
        header.addView(titleView, new LinearLayout.LayoutParams(0, dp(38), 1));
        header.addView(captionView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        parent.addView(header);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(STROKE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        params.setMargins(0, dp(14), 0, dp(8));
        parent.addView(divider, params);
    }

    private void tintCheckBox(CheckBox checkBox) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_checked },
                new CheckboxMarkDrawable(true));
        states.addState(new int[] {}, new CheckboxMarkDrawable(false));
        checkBox.setButtonDrawable(states);
    }

    private final class CheckboxMarkDrawable extends Drawable {
        private final boolean checked;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        CheckboxMarkDrawable(boolean checked) {
            this.checked = checked;
        }

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(checked ? ACCENT : Color.TRANSPARENT);
            canvas.drawRoundRect(bounds, dp(2), dp(2), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(checked ? ACCENT : SECONDARY);
            canvas.drawRoundRect(bounds, dp(2), dp(2), paint);
            if (checked) {
                paint.setColor(BACKGROUND);
                paint.setStrokeWidth(dp(1));
                paint.setStrokeCap(Paint.Cap.ROUND);
                float left = bounds.left;
                float top = bounds.top;
                float width = bounds.width();
                float height = bounds.height();
                canvas.drawLine(left + width * 0.22f, top + height * 0.53f,
                        left + width * 0.43f, top + height * 0.74f, paint);
                canvas.drawLine(left + width * 0.43f, top + height * 0.74f,
                        left + width * 0.80f, top + height * 0.28f, paint);
                paint.setStrokeCap(Paint.Cap.BUTT);
            }
        }

        @Override public int getIntrinsicWidth() { return dp(18); }
        @Override public int getIntrinsicHeight() { return dp(18); }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private void applyUiFont(View view) {
        if (view instanceof TextView) ((TextView) view).setTypeface(uiTypeface);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyUiFont(group.getChildAt(i));
        }
    }

    private void styleDialog(AlertDialog dialog) {
        if (dialog == null) return;
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(panelBackground());
            dialog.getWindow().setDimAmount(0.72f);
        }
        applyUiFont(dialog.getWindow() == null ? null : dialog.getWindow().getDecorView());
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) positive.setTextColor(ACCENT);
        if (negative != null) negative.setTextColor(SECONDARY);
    }

    private int dp(int value) {
        return (int) (value * displayDensity + 0.5f);
    }
}
