package com.quietphoto.clock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    public static final String CLOCK_X_RATIO = "clock_x_ratio";
    public static final String CLOCK_Y_RATIO = "clock_y_ratio";
    public static final String CLOCK_BACKGROUND_ENABLED = "clock_bg_enabled";
    public static final String CLOCK_FONT_STYLE = "clock_font_style";
    public static final String CLOCK_FONT_ID = "clock_font_id";
    public static final String NIGHT_MODE_ENABLED = "night_mode_enabled";
    public static final String NIGHT_START_HOUR = "night_start_hour";
    public static final String NIGHT_END_HOUR = "night_end_hour";
    public static final String TRANSITION_TYPE = "transition_type";
    public static final String LOW_POWER_MODE = "low_power_mode";
    public static final String PHOTO_DISPLAY_MODE = "photo_display_mode";
    public static final String BURN_IN_ENABLED = "burn_in_enabled";
    public static final String AUTO_BRIGHTNESS_ENABLED = "auto_brightness_enabled";
    public static final String FAVORITES_ONLY = "favorites_only";
    public static final String FAVORITE_PHOTOS = "favorite_photos";
    public static final String HIDDEN_PHOTOS = "hidden_photos";
    public static final String WEATHER_ENABLED = "weather_enabled";
    public static final String WEATHER_SHOW_LOCATION = "weather_show_location";
    public static final String WEATHER_COMPACT_MODE = "weather_compact_mode";
    public static final String WEATHER_LOCATION_NAME = "weather_location_name";
    public static final String WEATHER_LATITUDE = "weather_latitude";
    public static final String WEATHER_LONGITUDE = "weather_longitude";
    public static final String WEATHER_TIMEZONE = "weather_timezone";
    public static final String WEATHER_TEMPERATURE = "weather_temperature";
    public static final String WEATHER_CODE = "weather_code";
    public static final String WEATHER_IS_DAY = "weather_is_day";
    public static final String WEATHER_UPDATED_AT = "weather_updated_at";

    // 3 大進階視覺特效 Key
    public static final String ADAPTIVE_COLOR_ENABLED = "adaptive_color_enabled";
    public static final String POLAROID_FRAME_ENABLED = "polaroid_frame_enabled";
    public static final String SMART_FOCUS_ENABLED = "smart_focus_enabled";

    public static final int DEFAULT_INTERVAL_SECONDS = 45;
    private static final int MIN_INTERVAL = 5;
    private static final int MAX_INTERVAL = 600;

    private static final int BACKGROUND = Color.rgb(11, 15, 20);
    private static final int PANEL = Color.rgb(24, 31, 40);
    private static final int PRIMARY = Color.rgb(242, 238, 230);
    private static final int SECONDARY = Color.rgb(143, 152, 163);
    private static final int ACCENT = Color.rgb(72, 184, 199);
    private static final int ACTIVE_CHIP = Color.rgb(37, 124, 137);

    private final Set<String> selectedFolders = new LinkedHashSet<String>();
    private int selectedInterval;
    private boolean clockBgEnabled;
    private int selectedFontStyle;
    private String selectedFontId;
    private List<FontManager.FontOption> fontOptions;
    private boolean nightModeEnabled;
    private int nightStartHour;
    private int nightEndHour;
    private int selectedTransition;
    private int selectedDisplayMode;

    private boolean adaptiveColorEnabled;
    private boolean polaroidFrameEnabled;
    private boolean smartFocusEnabled;
    private boolean lowPowerMode;
    private boolean burnInEnabled;
    private boolean autoBrightnessEnabled;
    private boolean favoritesOnly;
    private boolean weatherEnabled;
    private boolean weatherShowLocation;
    private boolean weatherCompactMode = true;
    private String weatherLocationName = "";
    private String weatherTimezone = "auto";
    private double weatherLatitude = Double.NaN;
    private double weatherLongitude = Double.NaN;
    private boolean weatherLocationChanged;

    private File currentDirectory;

    private TextView pathText;
    private TextView selectionText;
    private TextView intervalDisplay;
    private CheckBox nightModeCheck;
    private CheckBox clockBgCheck;
    private CheckBox adaptiveColorCheck;
    private CheckBox polaroidFrameCheck;
    private CheckBox smartFocusCheck;
    private CheckBox lowPowerCheck;
    private CheckBox burnInCheck;
    private CheckBox autoBrightnessCheck;
    private CheckBox favoritesOnlyCheck;
    private CheckBox currentFolderCheck;
    private CheckBox weatherEnabledCheck;
    private CheckBox weatherLocationCheck;
    private CheckBox weatherCompactCheck;
    private TextView nightScheduleText;
    private TextView weatherLocationText;
    private LinearLayout folderList;

    private Spinner fontSpinner;
    private Spinner transitionSpinner;
    private Spinner displayModeSpinner;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        registerPredictiveBackCallback();

        currentDirectory = null;

        android.content.SharedPreferences prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        selectedInterval = prefs.getInt(PHOTO_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS);
        clockBgEnabled = prefs.getBoolean(CLOCK_BACKGROUND_ENABLED, false);
        selectedFontStyle = prefs.getInt(CLOCK_FONT_STYLE, 0);
        selectedFontId = FontManager.normalizeId(this, prefs.getString(
                CLOCK_FONT_ID, FontManager.getIdForLegacyIndex(selectedFontStyle)));
        fontOptions = FontManager.getOptions(this);
        nightModeEnabled = prefs.getBoolean(NIGHT_MODE_ENABLED, false);
        nightStartHour = prefs.getInt(NIGHT_START_HOUR, 23);
        nightEndHour = prefs.getInt(NIGHT_END_HOUR, 7);
        selectedTransition = prefs.getInt(TRANSITION_TYPE, 0);
        selectedDisplayMode = prefs.getInt(PHOTO_DISPLAY_MODE, 0);
        adaptiveColorEnabled = prefs.getBoolean(ADAPTIVE_COLOR_ENABLED, true);
        polaroidFrameEnabled = prefs.getBoolean(POLAROID_FRAME_ENABLED, false);
        smartFocusEnabled = prefs.getBoolean(SMART_FOCUS_ENABLED, true);
        lowPowerMode = prefs.getBoolean(LOW_POWER_MODE, true);
        burnInEnabled = prefs.getBoolean(BURN_IN_ENABLED, true);
        autoBrightnessEnabled = prefs.getBoolean(AUTO_BRIGHTNESS_ENABLED, false);
        favoritesOnly = prefs.getBoolean(FAVORITES_ONLY, false);
        weatherEnabled = prefs.getBoolean(WEATHER_ENABLED, false);
        weatherShowLocation = prefs.getBoolean(WEATHER_SHOW_LOCATION, false);
        weatherCompactMode = prefs.getBoolean(WEATHER_COMPACT_MODE, true);
        weatherLocationName = prefs.getString(WEATHER_LOCATION_NAME, "");
        weatherTimezone = prefs.getString(WEATHER_TIMEZONE, "auto");
        weatherLatitude = parseDouble(prefs.getString(WEATHER_LATITUDE, null));
        weatherLongitude = parseDouble(prefs.getString(WEATHER_LONGITUDE, null));

        Set<String> saved = prefs.getStringSet(PHOTO_FOLDERS, null);
        if (saved != null && !saved.isEmpty()) {
            selectedFolders.addAll(new HashSet<String>(saved));
        }

        setContentView(buildInterface());
        showDirectory();
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
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
        ScrollView outerScroll = new ScrollView(this);
        outerScroll.setFillViewport(true);
        outerScroll.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        applySystemBarInsets(root);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("設定", 23, PRIMARY);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        selectionText = text("", 14, ACCENT);
        selectionText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        Button cancel = button("取消", PANEL);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button save = button("套用", ACTIVE_CHIP);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });

        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        titleRow.addView(cancel, new LinearLayout.LayoutParams(dp(80), dp(44)));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(80), dp(44));
        saveParams.setMargins(dp(10), 0, 0, 0);
        titleRow.addView(save, saveParams);
        root.addView(titleRow);

        // 常用播放與顯示設定。
        LinearLayout mainSection = new LinearLayout(this);
        mainSection.setOrientation(LinearLayout.VERTICAL);
        mainSection.setPadding(dp(12), dp(10), dp(12), dp(10));
        mainSection.setBackground(panelBackground());

        LinearLayout intervalHeader = new LinearLayout(this);
        intervalHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView intervalTitle = text("相片停留時間", 17, PRIMARY);
        intervalTitle.setTypeface(Typeface.DEFAULT_BOLD);

        intervalDisplay = text(selectedInterval + " 秒", 18, ACCENT);
        intervalDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        intervalDisplay.setGravity(Gravity.END);

        intervalHeader.addView(intervalTitle, new LinearLayout.LayoutParams(0, dp(32), 1));
        intervalHeader.addView(intervalDisplay, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)));
        mainSection.addView(intervalHeader);

        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setGravity(Gravity.CENTER_VERTICAL);
        presetsRow.setPadding(0, dp(4), 0, dp(6));

        final int[] presets = { 15, 30, 45, 60, 120 };
        for (final int seconds : presets) {
            Button chip = button(seconds + "秒", PANEL);
            chip.setTextSize(14);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setInterval(seconds);
                }
            });
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    0, dp(38), 1);
            chipParams.setMargins(0, 0, dp(6), 0);
            presetsRow.addView(chip, chipParams);
        }

        Button minus = button("－", Color.rgb(45, 55, 70));
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInterval(selectedInterval - 5);
            }
        });
        Button plus = button("＋", Color.rgb(45, 55, 70));
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInterval(selectedInterval + 5);
            }
        });

        presetsRow.addView(minus, new LinearLayout.LayoutParams(dp(44), dp(38)));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dp(44), dp(38));
        plusParams.setMargins(dp(4), 0, 0, 0);
        presetsRow.addView(plus, plusParams);
        mainSection.addView(presetsRow);

        transitionSpinner = addSpinner(
                mainSection,
                "相片轉場",
                new String[] { "淡入", "水平滑動", "垂直滑動", "縮放", "翻轉", "旋轉" },
                Math.max(0, Math.min(5, selectedTransition)));

        fontSpinner = addSpinner(
                mainSection,
                "時鐘字型",
                FontManager.getDisplayNames(fontOptions),
                FontManager.findOptionIndex(fontOptions, selectedFontId));

        displayModeSpinner = addSpinner(
                mainSection,
                "相片顯示",
                new String[] { "填滿畫面", "完整顯示", "柔和背景（選用）" },
                Math.max(0, Math.min(2, selectedDisplayMode)));

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
            }
        });
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

        lowPowerCheck = checkBox("低耗電模式（建議）", lowPowerMode);
        mainSection.addView(lowPowerCheck);

        clockBgCheck = checkBox("時間底板", clockBgEnabled);
        mainSection.addView(clockBgCheck);

        nightModeCheck = checkBox("排程暗屏", nightModeEnabled);
        mainSection.addView(nightModeCheck);

        nightScheduleText = text("", 15, ACCENT);
        nightScheduleText.setTypeface(Typeface.DEFAULT_BOLD);
        nightScheduleText.setPadding(dp(8), dp(4), dp(8), 0);
        mainSection.addView(nightScheduleText, new LinearLayout.LayoutParams(
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
        nightControls.addView(startMinus, new LinearLayout.LayoutParams(dp(44), dp(36)));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(dp(44), dp(36));
        p1.setMargins(dp(4), 0, 0, 0);
        nightControls.addView(startPlus, p1);

        LinearLayout.LayoutParams endLabelParams = new LinearLayout.LayoutParams(0, dp(36), 1);
        endLabelParams.setMargins(dp(16), 0, 0, 0);
        nightControls.addView(endLabel, endLabelParams);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(dp(44), dp(36));
        p2.setMargins(dp(12), 0, 0, 0);
        nightControls.addView(endMinus, p2);

        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(dp(44), dp(36));
        p3.setMargins(dp(4), 0, 0, 0);
        nightControls.addView(endPlus, p3);

        mainSection.addView(nightControls);
        updateNightText();

        final LinearLayout advancedOptions = new LinearLayout(this);
        advancedOptions.setOrientation(LinearLayout.VERTICAL);
        advancedOptions.setVisibility(View.GONE);

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
            advancedOptions.addView(autoBrightnessCheck);
        }

        favoritesOnlyCheck = checkBox("只播放收藏相片", favoritesOnly);
        advancedOptions.addView(favoritesOnlyCheck);

        Button clearHidden = button("重新顯示已隱藏相片", PANEL);
        clearHidden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                        .edit().remove(HIDDEN_PHOTOS).apply();
                Toast.makeText(SettingsActivity.this,
                        "已清除隱藏清單", Toast.LENGTH_SHORT).show();
            }
        });
        advancedOptions.addView(clearHidden, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        final Button advancedToggle = button("顯示進階設定", PANEL);
        advancedToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean show = advancedOptions.getVisibility() != View.VISIBLE;
                advancedOptions.setVisibility(show ? View.VISIBLE : View.GONE);
                advancedToggle.setText(show ? "收合進階設定" : "顯示進階設定");
            }
        });
        LinearLayout.LayoutParams advancedParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(38));
        advancedParams.setMargins(0, dp(6), 0, dp(4));
        mainSection.addView(advancedToggle, advancedParams);
        mainSection.addView(advancedOptions);

        LinearLayout.LayoutParams secParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        secParams.setMargins(0, dp(6), 0, dp(10));
        root.addView(mainSection, secParams);

        // 相簿資料夾選擇。
        LinearLayout albumHeader = new LinearLayout(this);
        albumHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView albumTitle = text("相簿", 17, PRIMARY);
        albumTitle.setTypeface(Typeface.DEFAULT_BOLD);
        albumHeader.addView(albumTitle, new LinearLayout.LayoutParams(0, dp(36), 1));
        albumHeader.addView(selectionText, new LinearLayout.LayoutParams(0, dp(36), 2));
        root.addView(albumHeader);

        LinearLayout pathRow = new LinearLayout(this);
        pathRow.setGravity(Gravity.CENTER_VERTICAL);
        Button up = button("上一層", PANEL);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        pathText = text("", 15, SECONDARY);
        pathText.setPadding(dp(14), 0, 0, 0);
        pathRow.addView(up, new LinearLayout.LayoutParams(dp(100), dp(40)));
        pathRow.addView(pathText, new LinearLayout.LayoutParams(0, dp(40), 1));
        root.addView(pathRow);

        currentFolderCheck = new CheckBox(this);
        currentFolderCheck.setText("使用目前資料夾中的照片（包含子目錄）");
        currentFolderCheck.setTextColor(PRIMARY);
        currentFolderCheck.setTextSize(16);
        currentFolderCheck.setPadding(dp(8), dp(4), dp(8), dp(4));
        currentFolderCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentDirectory != null) {
                    setSelected(currentDirectory, currentFolderCheck.isChecked());
                }
            }
        });
        root.addView(currentFolderCheck, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        folderList = new LinearLayout(this);
        folderList.setOrientation(LinearLayout.VERTICAL);
        root.addView(folderList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        outerScroll.addView(root);
        updateIntervalDisplay();
        return outerScroll;
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
        input.setPadding(dp(18), dp(8), dp(18), dp(8));
        new AlertDialog.Builder(this)
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
                .show();
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
        new AlertDialog.Builder(this)
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
                .show();
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
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = text(label, 16, PRIMARY);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new CompactSpinnerAdapter(items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(items.length - 1, selectedIndex)));

        row.addView(labelView, new LinearLayout.LayoutParams(0, dp(38), 1));
        row.addView(spinner, new LinearLayout.LayoutParams(0, dp(38), 2));
        parent.addView(row);
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
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setMinHeight(0);
                textView.setMinimumHeight(0);
                textView.setPadding(dp(dropDown ? 12 : 8), 0, dp(12), 0);
            }
            if (dropDown) {
                view.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
            }
        }
    }

    private CheckBox checkBox(String label, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setTextColor(PRIMARY);
        checkBox.setTextSize(15);
        checkBox.setChecked(checked);
        checkBox.setPadding(dp(4), dp(2), dp(4), dp(2));
        return checkBox;
    }

    private boolean hasLightSensor() {
        SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        return manager != null && manager.getDefaultSensor(Sensor.TYPE_LIGHT) != null;
    }

    private void setInterval(int value) {
        selectedInterval = Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, value));
        updateIntervalDisplay();
    }

    private void updateIntervalDisplay() {
        if (intervalDisplay != null) {
            intervalDisplay.setText(String.format(Locale.TAIWAN, "%d 秒", selectedInterval));
        }
    }

    private void showDirectory() {
        folderList.removeAllViews();

        if (currentDirectory == null) {
            pathText.setText("選擇儲存裝置");
            currentFolderCheck.setVisibility(View.GONE);

            List<StorageVolumeItem> volumes = getAvailableStorageVolumes();
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
        } else {
            currentFolderCheck.setVisibility(View.VISIBLE);
            pathText.setText(currentDirectory.getAbsolutePath());
            currentFolderCheck.setChecked(selectedFolders.contains(currentDirectory.getAbsolutePath()));

            File[] entries = currentDirectory.listFiles();
            List<File> directories = new ArrayList<File>();
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
        }

        updateSelectionSummary();
    }

    private View storageVolumeRow(final StorageVolumeItem volume) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));

        final CheckBox check = new CheckBox(this);
        check.setChecked(selectedFolders.contains(volume.rootDir.getAbsolutePath()));
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSelected(volume.rootDir, check.isChecked());
            }
        });
        row.addView(check, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView name = text(volume.label, 17, ACCENT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setPadding(dp(10), dp(8), dp(10), dp(8));
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
        row.setPadding(dp(8), dp(4), dp(8), dp(4));

        final CheckBox check = new CheckBox(this);
        check.setChecked(selectedFolders.contains(directory.getAbsolutePath()));
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSelected(directory, check.isChecked());
            }
        });
        row.addView(check, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView name = text(directory.getName(), 17, PRIMARY);
        name.setPadding(dp(10), dp(6), dp(10), dp(6));
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

    private void setSelected(File directory, boolean selected) {
        String path = directory.getAbsolutePath();
        if (selected) {
            selectedFolders.add(path);
        } else {
            selectedFolders.remove(path);
        }
        if (currentDirectory != null) {
            currentFolderCheck.setChecked(selectedFolders.contains(currentDirectory.getAbsolutePath()));
        }
        updateSelectionSummary();
    }

    private void updateSelectionSummary() {
        if (selectedFolders.isEmpty()) {
            selectionText.setText("所有可存取相片");
        } else {
            selectionText.setText(String.format(
                    Locale.TAIWAN, "已選 %d 個相簿", selectedFolders.size()));
        }
    }

    private void saveSettings() {
        int fontIndex = Math.max(0, Math.min(
                fontOptions.size() - 1, fontSpinner.getSelectedItemPosition()));
        selectedFontId = fontOptions.get(fontIndex).id;
        selectedTransition = Math.max(0, Math.min(5, transitionSpinner.getSelectedItemPosition()));
        selectedDisplayMode = Math.max(0, Math.min(2, displayModeSpinner.getSelectedItemPosition()));
        if (weatherEnabledCheck.isChecked()
                && (Double.isNaN(weatherLatitude) || Double.isNaN(weatherLongitude))) {
            Toast.makeText(this, "請先選擇天氣地點", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.SharedPreferences.Editor editor =
                getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putInt(PHOTO_INTERVAL_SECONDS, selectedInterval)
                .putBoolean(CLOCK_BACKGROUND_ENABLED, clockBgCheck.isChecked())
                .putString(CLOCK_FONT_ID, selectedFontId)
                .putBoolean(NIGHT_MODE_ENABLED, nightModeCheck.isChecked())
                .putInt(NIGHT_START_HOUR, nightStartHour)
                .putInt(NIGHT_END_HOUR, nightEndHour)
                .putInt(TRANSITION_TYPE, selectedTransition)
                .putInt(PHOTO_DISPLAY_MODE, selectedDisplayMode)
                .putBoolean(LOW_POWER_MODE, lowPowerCheck.isChecked())
                .putBoolean(ADAPTIVE_COLOR_ENABLED, adaptiveColorCheck.isChecked())
                .putBoolean(POLAROID_FRAME_ENABLED, polaroidFrameCheck.isChecked())
                .putBoolean(SMART_FOCUS_ENABLED, smartFocusCheck.isChecked())
                .putBoolean(BURN_IN_ENABLED, burnInCheck.isChecked())
                .putBoolean(AUTO_BRIGHTNESS_ENABLED,
                        autoBrightnessCheck != null && autoBrightnessCheck.isChecked())
                .putBoolean(FAVORITES_ONLY, favoritesOnlyCheck.isChecked())
                .putBoolean(WEATHER_ENABLED, weatherEnabledCheck.isChecked())
                .putBoolean(WEATHER_SHOW_LOCATION, weatherLocationCheck.isChecked())
                .putBoolean(WEATHER_COMPACT_MODE, weatherCompactCheck.isChecked())
                .putString(WEATHER_LOCATION_NAME, weatherLocationName)
                .putString(WEATHER_LATITUDE, Double.toString(weatherLatitude))
                .putString(WEATHER_LONGITUDE, Double.toString(weatherLongitude))
                .putString(WEATHER_TIMEZONE, weatherTimezone)
                .putStringSet(PHOTO_FOLDERS, new HashSet<String>(selectedFolders));
        if (weatherLocationChanged) {
            editor.remove(WEATHER_TEMPERATURE)
                    .remove(WEATHER_CODE)
                    .remove(WEATHER_IS_DAY)
                    .remove(WEATHER_UPDATED_AT);
        }
        editor.apply();
        finish();
    }

    private TextView text(String content, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(PRIMARY);
        button.setTextSize(15);
        button.setAllCaps(false);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        button.setBackground(drawable);
        return button;
    }

    private GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(PANEL);
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
