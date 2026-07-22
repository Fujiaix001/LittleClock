package com.quietphoto.clock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

public final class SettingsActivity extends Activity {
    public static final String PREFERENCES = "quietphotoclock";
    public static final String PHOTO_FOLDERS = "photo_folders";
    public static final String PHOTO_INTERVAL_SECONDS = "photo_interval_seconds";
    public static final String CLOCK_X_RATIO = "clock_x_ratio";
    public static final String CLOCK_Y_RATIO = "clock_y_ratio";
    public static final String CLOCK_BACKGROUND_ENABLED = "clock_bg_enabled";
    public static final String CLOCK_FONT_STYLE = "clock_font_style";
    public static final String AUTO_START_CHARGING = "auto_start_charging";
    public static final String NIGHT_MODE_ENABLED = "night_mode_enabled";
    public static final String NIGHT_START_HOUR = "night_start_hour";
    public static final String NIGHT_END_HOUR = "night_end_hour";
    public static final String TRANSITION_TYPE = "transition_type";

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
    private boolean autoStartCharging;
    private boolean nightModeEnabled;
    private int nightStartHour;
    private int nightEndHour;
    private int selectedTransition;

    private boolean adaptiveColorEnabled;
    private boolean polaroidFrameEnabled;
    private boolean smartFocusEnabled;

    private File currentDirectory;

    private TextView pathText;
    private TextView selectionText;
    private TextView intervalDisplay;
    private CheckBox autoStartCheck;
    private CheckBox nightModeCheck;
    private CheckBox clockBgCheck;
    private CheckBox adaptiveColorCheck;
    private CheckBox polaroidFrameCheck;
    private CheckBox smartFocusCheck;
    private CheckBox currentFolderCheck;
    private TextView nightScheduleText;
    private LinearLayout folderList;

    private final List<Button> fontStyleButtons = new ArrayList<Button>();
    private final List<Button> transitionButtons = new ArrayList<Button>();

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

        currentDirectory = null;

        android.content.SharedPreferences prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        selectedInterval = prefs.getInt(PHOTO_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS);
        clockBgEnabled = prefs.getBoolean(CLOCK_BACKGROUND_ENABLED, false);
        selectedFontStyle = prefs.getInt(CLOCK_FONT_STYLE, 0);
        autoStartCharging = prefs.getBoolean(AUTO_START_CHARGING, true);
        nightModeEnabled = prefs.getBoolean(NIGHT_MODE_ENABLED, false);
        nightStartHour = prefs.getInt(NIGHT_START_HOUR, 23);
        nightEndHour = prefs.getInt(NIGHT_END_HOUR, 7);
        selectedTransition = prefs.getInt(TRANSITION_TYPE, 0);
        adaptiveColorEnabled = prefs.getBoolean(ADAPTIVE_COLOR_ENABLED, true);
        polaroidFrameEnabled = prefs.getBoolean(POLAROID_FRAME_ENABLED, false);
        smartFocusEnabled = prefs.getBoolean(SMART_FOCUS_ENABLED, true);

        Set<String> saved = prefs.getStringSet(PHOTO_FOLDERS, null);
        if (saved != null && !saved.isEmpty()) {
            selectedFolders.addAll(new HashSet<String>(saved));
        }

        setContentView(buildInterface());
        showDirectory();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (currentDirectory != null) {
            if (isRootStorageVolume(currentDirectory)) {
                currentDirectory = null;
                showDirectory();
                return;
            }
            File parent = currentDirectory.getParentFile();
            if (parent != null) {
                currentDirectory = parent;
                showDirectory();
                return;
            }
        }
        super.onBackPressed();
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
            volumes.add(new StorageVolumeItem("📱 內建儲存空間 (Internal Storage)", primary));
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
                            volumes.add(new StorageVolumeItem("💳 外接 MicroSD 卡 (" + rootDir.getName() + ")", rootDir));
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
                            volumes.add(new StorageVolumeItem("📱 內建儲存空間 (Internal Storage)", primary0));
                        }
                    } else {
                        String cPath = subDir.getAbsolutePath();
                        if (!addedPaths.contains(cPath)) {
                            addedPaths.add(cPath);
                            volumes.add(new StorageVolumeItem("💳 外接 MicroSD 卡 (" + name + ")", subDir));
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

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("⚙️ 設定", 23, PRIMARY);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        selectionText = text("", 14, ACCENT);
        selectionText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

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

        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(48), 2));
        titleRow.addView(selectionText, new LinearLayout.LayoutParams(0, dp(48), 1));
        titleRow.addView(cancel, new LinearLayout.LayoutParams(dp(95), dp(44)));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(95), dp(44));
        saveParams.setMargins(dp(10), 0, 0, 0);
        titleRow.addView(save, saveParams);
        root.addView(titleRow);

        // --- 區塊一：播放、自動化與夜間休眠 ---
        LinearLayout mainSection = new LinearLayout(this);
        mainSection.setOrientation(LinearLayout.VERTICAL);
        mainSection.setPadding(dp(12), dp(10), dp(12), dp(10));
        mainSection.setBackground(panelBackground());

        LinearLayout intervalHeader = new LinearLayout(this);
        intervalHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView intervalTitle = text("⏱️ 單張圖片停留時間", 17, PRIMARY);
        intervalTitle.setTypeface(Typeface.DEFAULT_BOLD);

        intervalDisplay = text(selectedInterval + " 秒", 18, ACCENT);
        intervalDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        intervalDisplay.setGravity(Gravity.RIGHT);

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

        // --- 進階視覺特效開關 ---
        TextView fxHeader = text("✨ 進階視覺特效", 16, ACCENT);
        fxHeader.setTypeface(Typeface.DEFAULT_BOLD);
        fxHeader.setPadding(0, dp(8), 0, dp(4));
        mainSection.addView(fxHeader);

        adaptiveColorCheck = new CheckBox(this);
        adaptiveColorCheck.setText("🎨 開啟時鐘底板/文字光暈動態適應相片主色調");
        adaptiveColorCheck.setTextColor(PRIMARY);
        adaptiveColorCheck.setTextSize(15);
        adaptiveColorCheck.setChecked(adaptiveColorEnabled);
        adaptiveColorCheck.setPadding(dp(4), dp(2), dp(4), dp(2));
        mainSection.addView(adaptiveColorCheck);

        polaroidFrameCheck = new CheckBox(this);
        polaroidFrameCheck.setText("📷 開啟復古拍立得相框與立體陰影");
        polaroidFrameCheck.setTextColor(PRIMARY);
        polaroidFrameCheck.setTextSize(15);
        polaroidFrameCheck.setChecked(polaroidFrameEnabled);
        polaroidFrameCheck.setPadding(dp(4), dp(2), dp(4), dp(2));
        mainSection.addView(polaroidFrameCheck);

        smartFocusCheck = new CheckBox(this);
        smartFocusCheck.setText("🔍 開啟圖片智慧視覺重心 (Entropy Focal) 微幅推近");
        smartFocusCheck.setTextColor(PRIMARY);
        smartFocusCheck.setTextSize(15);
        smartFocusCheck.setChecked(smartFocusEnabled);
        smartFocusCheck.setPadding(dp(4), dp(2), dp(4), dp(2));
        mainSection.addView(smartFocusCheck);

        // --- 相片切換轉場選單 ---
        TextView transitionHeader = text("🎞️ 相片切換轉場特效", 16, PRIMARY);
        transitionHeader.setTypeface(Typeface.DEFAULT_BOLD);
        transitionHeader.setPadding(0, dp(8), 0, dp(4));
        mainSection.addView(transitionHeader);

        LinearLayout transRow1 = new LinearLayout(this);
        transRow1.setGravity(Gravity.CENTER_VERTICAL);
        transRow1.setPadding(0, 0, 0, dp(4));

        LinearLayout transRow2 = new LinearLayout(this);
        transRow2.setGravity(Gravity.CENTER_VERTICAL);
        transRow2.setPadding(0, 0, 0, dp(6));

        final String[] transNames = { "經典淡入", "左右推頁", "上下推頁", "藝廊縮放", "3D翻轉", "傾斜旋轉" };
        transitionButtons.clear();
        for (int i = 0; i < transNames.length; i++) {
            final int tIndex = i;
            Button tBtn = button(transNames[i], tIndex == selectedTransition ? ACTIVE_CHIP : PANEL);
            tBtn.setTextSize(13);
            tBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setTransition(tIndex);
                }
            });
            transitionButtons.add(tBtn);
            LinearLayout.LayoutParams tParams = new LinearLayout.LayoutParams(0, dp(36), 1);

            if (i < 3) {
                if (i < 2) tParams.setMargins(0, 0, dp(6), 0);
                transRow1.addView(tBtn, tParams);
            } else {
                if (i < 5) tParams.setMargins(0, 0, dp(6), 0);
                transRow2.addView(tBtn, tParams);
            }
        }
        mainSection.addView(transRow1);
        mainSection.addView(transRow2);

        // --- 時鐘字體風格選單 ---
        TextView fontHeader = text("🔤 時鐘字體風格", 16, PRIMARY);
        fontHeader.setTypeface(Typeface.DEFAULT_BOLD);
        fontHeader.setPadding(0, dp(4), 0, dp(4));
        mainSection.addView(fontHeader);

        LinearLayout fontRow1 = new LinearLayout(this);
        fontRow1.setGravity(Gravity.CENTER_VERTICAL);
        fontRow1.setPadding(0, 0, 0, dp(4));

        LinearLayout fontRow2 = new LinearLayout(this);
        fontRow2.setGravity(Gravity.CENTER_VERTICAL);
        fontRow2.setPadding(0, 0, 0, dp(4));

        LinearLayout fontRow3 = new LinearLayout(this);
        fontRow3.setGravity(Gravity.CENTER_VERTICAL);
        fontRow3.setPadding(0, 0, 0, dp(4));

        LinearLayout fontRow4 = new LinearLayout(this);
        fontRow4.setGravity(Gravity.CENTER_VERTICAL);
        fontRow4.setPadding(0, 0, 0, dp(6));

        final String[] fontNames = {
                "預設粗體", "經典電子鐘", "經典黑體",
                "經典宋體", "柔和圓體", "文雅楷體",
                "重磅厚黑", "Orbitron", "Audiowide",
                "Oxanium", "Saira Stencil", "Zen Dots"
        };
        fontStyleButtons.clear();
        for (int i = 0; i < fontNames.length; i++) {
            final int styleIndex = i;
            Button fontBtn = button(fontNames[i], styleIndex == selectedFontStyle ? ACTIVE_CHIP : PANEL);
            fontBtn.setTextSize(13);
            fontBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFontStyle(styleIndex);
                }
            });
            fontStyleButtons.add(fontBtn);
            LinearLayout.LayoutParams fontParams = new LinearLayout.LayoutParams(0, dp(36), 1);

            if (i < 3) {
                if (i < 2) fontParams.setMargins(0, 0, dp(6), 0);
                fontRow1.addView(fontBtn, fontParams);
            } else if (i < 6) {
                if (i < 5) fontParams.setMargins(0, 0, dp(6), 0);
                fontRow2.addView(fontBtn, fontParams);
            } else if (i < 9) {
                if (i < 8) fontParams.setMargins(0, 0, dp(6), 0);
                fontRow3.addView(fontBtn, fontParams);
            } else {
                if (i < 11) fontParams.setMargins(0, 0, dp(6), 0);
                fontRow4.addView(fontBtn, fontParams);
            }
        }
        mainSection.addView(fontRow1);
        mainSection.addView(fontRow2);
        mainSection.addView(fontRow3);
        mainSection.addView(fontRow4);

        clockBgCheck = new CheckBox(this);
        clockBgCheck.setText("顯示時間日期區塊半透明底板");
        clockBgCheck.setTextColor(PRIMARY);
        clockBgCheck.setTextSize(15);
        clockBgCheck.setChecked(clockBgEnabled);
        clockBgCheck.setPadding(dp(4), dp(2), dp(4), dp(2));
        mainSection.addView(clockBgCheck);

        autoStartCheck = new CheckBox(this);
        autoStartCheck.setText("🔌 插上電源/充電時自動啟動數位相框");
        autoStartCheck.setTextColor(PRIMARY);
        autoStartCheck.setTextSize(15);
        autoStartCheck.setChecked(autoStartCharging);
        autoStartCheck.setPadding(dp(4), dp(2), dp(4), dp(2));
        mainSection.addView(autoStartCheck);

        // --- 夜間定時暗屏時段控制 ---
        nightModeCheck = new CheckBox(this);
        nightModeCheck.setText("🌙 啟動夜間護眼定時暗屏休眠");
        nightModeCheck.setTextColor(PRIMARY);
        nightModeCheck.setTextSize(15);
        nightModeCheck.setChecked(nightModeEnabled);
        nightModeCheck.setPadding(dp(4), dp(2), dp(4), dp(2));
        mainSection.addView(nightModeCheck);

        LinearLayout nightTimeRow = new LinearLayout(this);
        nightTimeRow.setGravity(Gravity.CENTER_VERTICAL);
        nightTimeRow.setPadding(dp(8), dp(4), dp(8), dp(6));

        nightScheduleText = text("", 15, ACCENT);
        nightScheduleText.setTypeface(Typeface.DEFAULT_BOLD);

        Button startMinus = button("始－", PANEL);
        startMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightStartHour = (nightStartHour + 23) % 24;
                updateNightText();
            }
        });

        Button startPlus = button("始＋", PANEL);
        startPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightStartHour = (nightStartHour + 1) % 24;
                updateNightText();
            }
        });

        Button endMinus = button("終－", PANEL);
        endMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightEndHour = (nightEndHour + 23) % 24;
                updateNightText();
            }
        });

        Button endPlus = button("終＋", PANEL);
        endPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nightEndHour = (nightEndHour + 1) % 24;
                updateNightText();
            }
        });

        nightTimeRow.addView(nightScheduleText, new LinearLayout.LayoutParams(0, dp(38), 1));
        nightTimeRow.addView(startMinus, new LinearLayout.LayoutParams(dp(54), dp(36)));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(dp(54), dp(36));
        p1.setMargins(dp(4), 0, 0, 0);
        nightTimeRow.addView(startPlus, p1);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(dp(54), dp(36));
        p2.setMargins(dp(12), 0, 0, 0);
        nightTimeRow.addView(endMinus, p2);

        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(dp(54), dp(36));
        p3.setMargins(dp(4), 0, 0, 0);
        nightTimeRow.addView(endPlus, p3);

        mainSection.addView(nightTimeRow);
        updateNightText();

        LinearLayout.LayoutParams secParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        secParams.setMargins(0, dp(6), 0, dp(10));
        root.addView(mainSection, secParams);

        // --- 區塊二：相簿資料夾選擇 ---
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

    private void updateNightText() {
        if (nightScheduleText != null) {
            nightScheduleText.setText(String.format(Locale.TAIWAN,
                    "時段：%02d:00 ~ %02d:00", nightStartHour, nightEndHour));
        }
    }

    private void setTransition(int tIndex) {
        selectedTransition = tIndex;
        for (int i = 0; i < transitionButtons.size(); i++) {
            Button btn = transitionButtons.get(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(i == selectedTransition ? ACTIVE_CHIP : PANEL);
            bg.setCornerRadius(dp(8));
            btn.setBackground(bg);
        }
    }

    private void setFontStyle(int styleIndex) {
        selectedFontStyle = styleIndex;
        for (int i = 0; i < fontStyleButtons.size(); i++) {
            Button btn = fontStyleButtons.get(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(i == selectedFontStyle ? ACTIVE_CHIP : PANEL);
            bg.setCornerRadius(dp(8));
            btn.setBackground(bg);
        }
    }

    private void setInterval(int value) {
        selectedInterval = Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, value));
        updateIntervalDisplay();
    }

    private void updateIntervalDisplay() {
        if (intervalDisplay != null) {
            intervalDisplay.setText(selectedInterval + " 秒");
        }
    }

    private void showDirectory() {
        folderList.removeAllViews();

        if (currentDirectory == null) {
            pathText.setText("💾 儲存裝置選擇");
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

        TextView name = text("📁  " + directory.getName(), 17, PRIMARY);
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
        selectionText.setText("已選 " + selectedFolders.size() + " 個相簿");
    }

    private void saveSettings() {
        if (selectedFolders.isEmpty()) {
            Toast.makeText(this, "請至少勾選一個照片資料夾", Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .edit()
                .putInt(PHOTO_INTERVAL_SECONDS, selectedInterval)
                .putBoolean(CLOCK_BACKGROUND_ENABLED, clockBgCheck.isChecked())
                .putInt(CLOCK_FONT_STYLE, selectedFontStyle)
                .putBoolean(AUTO_START_CHARGING, autoStartCheck.isChecked())
                .putBoolean(NIGHT_MODE_ENABLED, nightModeCheck.isChecked())
                .putInt(NIGHT_START_HOUR, nightStartHour)
                .putInt(NIGHT_END_HOUR, nightEndHour)
                .putInt(TRANSITION_TYPE, selectedTransition)
                .putBoolean(ADAPTIVE_COLOR_ENABLED, adaptiveColorCheck.isChecked())
                .putBoolean(POLAROID_FRAME_ENABLED, polaroidFrameCheck.isChecked())
                .putBoolean(SMART_FOCUS_ENABLED, smartFocusCheck.isChecked())
                .putStringSet(PHOTO_FOLDERS, new HashSet<String>(selectedFolders))
                .apply();
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
