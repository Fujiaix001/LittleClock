package com.quietphoto.clock;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhotoClockActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(9, 13, 18);
    private static final int PANEL = Color.rgb(19, 26, 35);
    private static final int PANEL_RAISED = Color.rgb(27, 37, 49);
    private static final int STROKE = Color.rgb(42, 56, 72);
    private static final int PRIMARY = Color.rgb(244, 247, 249);
    private static final int SECONDARY = Color.rgb(155, 169, 184);
    private static final int WARNING = Color.rgb(239, 108, 108);
    private static final int POMODORO_RED = Color.rgb(255, 104, 104);

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private static final float PHOTO_PAN_TRAVEL_FRACTION = 0.20f;
    private static final String CLOCK_POS_X_RATIO = "clock_pos_x_ratio";
    private static final String CLOCK_POS_Y_RATIO = "clock_pos_y_ratio";
    private static final int CLOCK_LAYOUT_VERSION = 2;
    private static final String CLOCK_LAYOUT_VERSION_KEY = "clock_layout_version";
    private static final String CLOCK_TIME_POS_X_RATIO = "clock_time_pos_x_ratio";
    private static final String CLOCK_TIME_POS_Y_RATIO = "clock_time_pos_y_ratio";
    private static final String CLOCK_DATE_POS_X_RATIO = "clock_date_pos_x_ratio";
    private static final String CLOCK_DATE_POS_Y_RATIO = "clock_date_pos_y_ratio";
    private static final String CLOCK_WEATHER_POS_X_RATIO = "clock_weather_pos_x_ratio";
    private static final String CLOCK_WEATHER_POS_Y_RATIO = "clock_weather_pos_y_ratio";
    private static final String CLOCK_GROUP_PIVOT_X_RATIO = "clock_group_pivot_x_ratio";
    private static final String CLOCK_GROUP_PIVOT_Y_RATIO = "clock_group_pivot_y_ratio";
    private static final float DEFAULT_CLOCK_SCALE = 0.75f;
    private static final float DEFAULT_GROUP_PANEL_X_RATIO = 0.95f;
    private static final float DEFAULT_GROUP_PANEL_Y_RATIO = 0.90f;
    private static final float DEFAULT_TIME_X_RATIO = 0.86f;
    private static final float DEFAULT_TIME_Y_RATIO = 0.76f;
    private static final float DEFAULT_DATE_X_RATIO = 0.86f;
    private static final float DEFAULT_DATE_Y_RATIO = 0.88f;
    private static final float DEFAULT_WEATHER_X_RATIO = 0.86f;
    private static final float DEFAULT_WEATHER_Y_RATIO = 0.96f;
    public static final String CLOCK_SCALE_FACTOR = "clock_scale_factor";
    public static final String CLOCK_SIZES_LINKED = "clock_sizes_linked";
    public static final String CLOCK_SIZE_MODE = "clock_size_mode";
    public static final int CLOCK_SIZE_MODE_OVERLAP = ClockSizeModePolicy.OVERLAP;
    public static final int CLOCK_SIZE_MODE_AVOID = ClockSizeModePolicy.AVOID;
    public static final int CLOCK_SIZE_MODE_GROUP = ClockSizeModePolicy.GROUP;
    public static final String CLOCK_LAYOUT_MODE = "clock_layout_mode";
    public static final int CLOCK_LAYOUT_MODE_ORIGINAL = ClockLayoutPolicy.ORIGINAL;
    public static final int CLOCK_LAYOUT_MODE_FREE = ClockLayoutPolicy.FREE;
    public static final String CLOCK_ORIGINAL_SCALE_MODE = "clock_original_scale_mode";
    public static final int CLOCK_ORIGINAL_SCALE_LINKED =
            ClockLayoutPolicy.ORIGINAL_SCALE_LINKED;
    public static final int CLOCK_ORIGINAL_SCALE_AVOID =
            ClockLayoutPolicy.ORIGINAL_SCALE_AVOID;
    public static final String LEGACY_BIND_SCALE = "bind_scale";
    private static final String CLOCK_TIME_SCALE_FACTOR = "clock_time_scale_factor";
    private static final String CLOCK_DATE_SCALE_FACTOR = "clock_date_scale_factor";
    private static final String CLOCK_WEATHER_SCALE_FACTOR = "clock_weather_scale_factor";
    private static final String LEGACY_TIME_SCALE_FACTOR = "time_scale_factor";
    private static final String LEGACY_DATE_SCALE_FACTOR = "date_scale_factor";
    private static final String LEGACY_WEATHER_SCALE_FACTOR = "weather_scale_factor";
    private static final int SCALE_TARGET_NONE = 0;
    private static final int SCALE_TARGET_TIME = 1;
    private static final int SCALE_TARGET_DATE = 2;
    private static final int SCALE_TARGET_WEATHER = 3;
    private static final String LAST_PHOTO_KEY = "last_photo_key";
    private static final String PHOTO_DIRECTORY = "QuietPanel/Photos";
    private static final int MAX_PHOTO_FILES = 50000;
    private static final int MAX_PHOTO_DEPTH = 12;
    private static final long IMMERSIVE_TIMEOUT_MS = 5000L;
    private static final long PHOTO_ACTION_LONG_PRESS_MS = 900L;
    private static final int MANUAL_REFRESH_PULL_DISTANCE_DP = 72;
    private static final long MANUAL_REFRESH_MIN_INTERVAL_MS = 30000L;
    private static final long MANUAL_REFRESH_BOUNCE_DURATION_MS = 320L;
    private static final long BURN_IN_INTERVAL_MS = 180000L;
    private static final long MEDIA_REFRESH_DELAY_MS = 2000L;
    private static final long WEATHER_FRESH_NORMAL_MS = 60L * 60L * 1000L;
    private static final long WEATHER_FRESH_LOW_POWER_MS = 120L * 60L * 1000L;
    private static final long WEATHER_MAX_AGE_MS = 6L * 60L * 60L * 1000L;
    private static final long FOCUS_REMINDER_DURATION_MS = 3000L;
    private static final long PERFORMANCE_GUARD_INTERVAL_MS = 60000L;
    private static final long PERFORMANCE_MEMORY_PRESSURE_MS = 5L * 60L * 1000L;
    private static final long SAF_RESCAN_NORMAL_MS = 60L * 60L * 1000L;
    private static final long SAF_RESCAN_LOW_POWER_MS = 2L * 60L * 60L * 1000L;
    private static final int WALLPAPER_PACK_VERSION = 4;
    private static final String WALLPAPER_PACK_VERSION_KEY = "wallpaper_pack_version";

    private FrameLayout rootContainer;
    private FrameLayout polaroidContainer;
    private ImageView backgroundImage;
    private ImageView photoImage;
    private View showcaseColorOverlay;
    private TextView photoStatus;
    private AccessibleFrameLayout clockPanel;
    /** The legacy .13-style single surface used by the linked group mode. */
    private LinearLayout legacyClockPanel;
    private AccessibleFrameLayout timeBlock;
    private AccessibleFrameLayout dateBlock;
    private AccessibleFrameLayout weatherBlock;
    private TextView photoTime;
    private TextView photoDate;
    private BatteryStatusView batteryStatusView;
    private LinearLayout dateRow;
    private LinearLayout compactWeatherRow;
    private WeatherIconView compactWeatherIcon;
    private TextView compactWeatherTemperature;
    private LinearLayout weatherRow;
    private LinearLayout weatherContentPanel;
    private WeatherIconView weatherIcon;
    private TextView weatherTemperature;
    private TextView weatherLocation;
    private LinearLayout weatherExtendedPanel;
    private TextView weatherForecastText;
    private TextView daylightLabel;
    private DaylightProgressView daylightProgressView;
    private LinearLayout alarmRow;
    private AlarmIconView alarmIcon;
    private TextView alarmTimeText;
    private LinearLayout pomodoroRow;
    private TextView pomodoroAdvancedInfo;
    private TextView pomodoroLabel;
    private TextView pomodoroText;
    private PomodoroProgressView pomodoroProgressView;
    private FrameLayout pomodoroFocusPanel;
    private LinearLayout pomodoroInfoPanel;
    private Button settingsButton;
    private Button pomodoroButton;
    private StateListDrawable quickActionBackground;
    private FrameLayout focusReminderOverlay;
    private ImageView focusReminderImage;
    private TextView focusReminderMessage;
    private AlertDialog pomodoroDialog;
    private TextView pomodoroDialogStatus;
    private Button pomodoroStartPauseButton;

    private Bitmap photoBitmap;
    private Bitmap pendingPhotoBitmap;
    private Bitmap softBackgroundBitmap;
    private final List<PhotoSource> photoFiles = new ArrayList<PhotoSource>();
    private final PlaybackNavigator playbackNavigator = new PlaybackNavigator();
    private final Handler photoHandler = new Handler();
    private PowerStateMonitor powerStateMonitor;
    private final ExecutorService photoScanExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService photoDecodeExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
    private final Matrix photoMatrix = new Matrix();
    private final Date nowDate = new Date();
    private final AccelerateDecelerateInterpolator smoothInterpolator = new AccelerateDecelerateInterpolator();
    private final OvershootInterpolator manualRefreshBounceInterpolator =
            new OvershootInterpolator(1.4f);
    private SharedPreferences prefs;
    private boolean clockTimeEnabled = true;
    private boolean clockDateEnabled = true;
    private boolean pomodoroModeLayoutActive;
    private boolean pomodoroSettingsLocked;
    private boolean pomodoroAdvancedDisplay;
    private long lastAlarmFiredMinute = -1;
    private PhotoSource currentPhotoSource;

    private final SimpleDateFormat photoTimeFormat =
            new SimpleDateFormat("HH:mm", Locale.TAIWAN);
    private final SimpleDateFormat photoTimeSecondsFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormat =
            new SimpleDateFormat("M月d日 EEEE", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormatEn =
            new SimpleDateFormat("EEE, MMM d", Locale.US);
    private final SimpleDateFormat weatherSunTimeFormat =
            new SimpleDateFormat("HH:mm", Locale.US);

    private int photoFailures;
    private volatile int photoGeneration;
    private boolean photoLoading;
    private boolean photoScanInProgress;
    private boolean photoCatalogLoaded;
    private String photoFolderSignature = "";
    private boolean photoPanReverse = true;
    private long lastPhotoPanFrameAt;
    private boolean activityResumed;
    private boolean firstSlideshowStart = true;
    private boolean startupPhotoDisplayed;
    private int photoFileLimit = MAX_PHOTO_FILES;

    private long photoIntervalMs = 45000L;
    private long photoPanDurationMs = 43000L;
    private boolean clockBgEnabled;
    private String clockFontId = FontManager.DEFAULT_ID;
    private String dateFontId = FontManager.DEFAULT_ID;
    private String weatherFontId = FontManager.DEFAULT_ID;
    private boolean nightModeEnabled;
    private int nightStartHour = 23;
    private int nightEndHour = 7;
    private int transitionType;
    private int clockSecondsMode = ClockSecondPolicy.OFF;
    private int playbackOrder = PlaybackOrderPolicy.RANDOM;
    private int keepScreenMode = PowerStatePolicy.KEEP_AWAKE_ALWAYS;
    private boolean lowBatteryGuard;
    private int batteryDisplayMode;
    private PowerStateMonitor.State powerState = PowerStateMonitor.State.unknown();
    private boolean lowBatteryForcedEco;
    private boolean lowBatteryPaused;
    private boolean isNightSleepActive;
    private boolean isNightSleepWoken;
    private final Runnable nightSleepReDimRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (isNightSleepActive && isNightSleepWoken) {
                isNightSleepWoken = false;
                enterNightSleepMode();
            }
        }
    };

    // 3 大視覺特效標誌
    private boolean adaptiveColorEnabled = true;
    private boolean polaroidFrameEnabled = false;
    private boolean smartFocusEnabled = true;
    private int selectedPerformanceMode = PerformanceModePolicy.ECO;
    private int effectivePerformanceMode = PerformanceModePolicy.ECO;
    private float lastBatteryTemperatureC = Float.NaN;
    private long performanceMemoryPressureUntil;
    private boolean burnInEnabled = true;
    private boolean autoBrightnessEnabled;
    private boolean favoritesOnly;
    private int photoDisplayMode;
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
    private boolean weatherFetchInFlight;
    private WeatherRefreshCallback pendingWeatherRefreshCallback;
    private long weatherLastAttemptAt;
    private long lastManualRefreshAt;
    private boolean manualRefreshArmed;
    private boolean manualRefreshTriggered;
    private boolean manualRefreshFeedbackVisible;
    private float manualRefreshDownX;
    private float manualRefreshDownY;
    private final Set<String> favoritePhotos = new HashSet<String>();
    private final Set<String> hiddenPhotos = new HashSet<String>();

    private int currentDominantColor = BACKGROUND;
    private float focalX = 0.5f;
    private float focalY = 0.5f;

    private long nextPhotoAt;

    private boolean isDraggingClock;
    private boolean isScalingClock;
    private long lastClockDragEndTime;
    private long lastPhotoSwipeEndTime;
    private float touchDownRawX;
    private float touchDownRawY;
    private float clockScaleFactor = 1.0f;
    private boolean clockSizesLinked = true;
    private float timeScaleFactor = 1.0f;
    private float dateScaleFactor = 1.0f;
    private float weatherScaleFactor = 1.0f;
    private int activeScaleTarget = SCALE_TARGET_NONE;
    private View activeClockBlock;
    private float clockStartBlockX;
    private float clockStartBlockY;
    private float timeBlockBaseX;
    private float timeBlockBaseY;
    private float dateBlockBaseX;
    private float dateBlockBaseY;
    private float weatherBlockBaseX;
    private float weatherBlockBaseY;
    private int clockLayoutMode = CLOCK_LAYOUT_MODE_ORIGINAL;
    private int originalScaleMode = CLOCK_ORIGINAL_SCALE_LINKED;
    private boolean legacyClockPanelActive;
    private float legacyClockTranslationX;
    private float legacyClockTranslationY;
    private float legacyClockStartTranslationX;
    private float legacyClockStartTranslationY;
    private float burnInOffsetX;
    private float burnInOffsetY;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector photoGestureDetector;
    private boolean photoActionLongPressPending;
    private float photoActionDownX;
    private float photoActionDownY;
    private final Runnable hideManualRefreshStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (!manualRefreshFeedbackVisible) return;
            manualRefreshFeedbackVisible = false;
            resetManualRefreshBounce();
            if (photoStatus != null) photoStatus.setVisibility(View.GONE);
        }
    };
    private final Runnable photoActionLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!photoActionLongPressPending || pomodoroModeLayoutActive || photoLoading) return;
            photoActionLongPressPending = false;
            manualRefreshArmed = true;
        }
    };
    private final Random random = new Random();
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private float lastLightLevel = -1.0f;
    private ContentObserver mediaObserver;
    private boolean mediaObserverRegistered;

    private static final class PhotoSource implements PlaybackOrderPolicy.Item {
        final File file;
        final Uri uri;
        final String identity;
        final String displayName;
        final long lastModified;

        private PhotoSource(File file, Uri uri, String identity,
                String displayName, long lastModified) {
            this.file = file;
            this.uri = uri;
            this.identity = identity;
            this.displayName = displayName == null ? "" : displayName;
            this.lastModified = lastModified;
        }

        static PhotoSource fromFile(File file, String identity) {
            return fromFile(file, identity, true);
        }

        static PhotoSource fromFile(File file, String identity, boolean readModified) {
            return new PhotoSource(file, null, identity,
                    file == null ? "" : file.getName(),
                    !readModified || file == null ? 0L : file.lastModified());
        }

        static PhotoSource fromUri(Uri uri) {
            return fromUri(uri, uri == null ? "" : uri.toString(), 0L);
        }

        static PhotoSource fromUri(Uri uri, String displayName, long lastModified) {
            return new PhotoSource(null, uri, uri == null ? "" : uri.toString(),
                    displayName, lastModified);
        }

        String key() {
            return identity;
        }

        @Override
        public String sortName() {
            return displayName;
        }

        @Override
        public long sortTimeMs() {
            return lastModified;
        }

        @Override
        public String stableKey() {
            return identity;
        }
    }

    private interface PhotoDiscovery {
        void onPhotoDiscovered(PhotoSource source);
    }

    private interface WeatherRefreshCallback {
        void onComplete(boolean success);
    }

    private static final class AccessibleFrameLayout extends FrameLayout {
        AccessibleFrameLayout(android.content.Context context) {
            super(context);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }
    }

    private final Runnable hideImmersiveRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            hideSystemUI();
        }
    };

    private final Runnable photoTicker = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed || isFinishing() || isDestroyed()) {
                return;
            }
            updatePhotoClock();
            checkForegroundAlarm();
            checkPomodoroCompletion();
            checkNightSleepMode();
            if (!isNightSleepActive && !lowBatteryPaused && !photoLoading && !photoFiles.isEmpty()
                    && SystemClock.elapsedRealtime() >= nextPhotoAt) {
                loadNextPhoto();
            }
            schedulePhotoTicker();
        }
    };

    private final Runnable safCatalogRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed || isFinishing() || isDestroyed()) return;
            if (hasTreePhotoSource()) {
                invalidatePhotoCatalog();
                startPhotoSlideshow();
            }
            scheduleSafCatalogRefresh();
        }
    };

    private final Runnable pomodoroDialogTicker = new Runnable() {
        @Override
        public void run() {
            if (pomodoroDialog == null || !pomodoroDialog.isShowing()) return;
            refreshPomodoroDialog();
            photoHandler.postDelayed(this, 1000L);
        }
    };

    private final Runnable hideFocusReminderRunnable = new Runnable() {
        @Override
        public void run() {
            if (focusReminderOverlay != null) {
                focusReminderOverlay.setVisibility(View.GONE);
            }
            showSettingsButton();
            resetImmersiveTimeout();
        }
    };

    private android.animation.ValueAnimator photoPanAnimator;

    private final Runnable burnInRunnable = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed || !burnInEnabled || isFinishing() || isDestroyed()) {
                return;
            }
            int maxOffset = dp(3);
            burnInOffsetX = random.nextInt(maxOffset * 2 + 1) - maxOffset;
            burnInOffsetY = random.nextInt(maxOffset * 2 + 1) - maxOffset;
            applyClockTranslation();
            photoHandler.postDelayed(this, BURN_IN_INTERVAL_MS);
        }
    };

    /** Updates only the clock text when optional seconds are visible. */
    private final Runnable secondClockTicker = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed || isFinishing() || isDestroyed()) return;
            if (!shouldShowClockSeconds() || isNightSleepActive) return;
            updateClockTimeText();
            scheduleSecondClockTicker();
        }
    };

    private final Runnable performanceGuardRunnable = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed || isFinishing() || isDestroyed()) return;
            updatePerformanceGuard();
            schedulePerformanceGuard();
        }
    };

    private final Runnable mediaRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (activityResumed) {
                invalidatePhotoCatalog();
                startPhotoSlideshow();
            }
        }
    };

    private final Runnable weatherRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            requestWeatherRefresh();
        }
    };

    private final SensorEventListener lightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values.length > 0) {
                applyAmbientBrightness(event.values[0]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FontManager.prefetch(this.getApplicationContext());
        prefs = getSharedPreferences(SettingsActivity.PREFERENCES, MODE_PRIVATE);
        removeExpiredTreeSources();
        photoFileLimit = resolvePhotoFileLimit();
        migrateOrientationClockLayouts();
        powerStateMonitor = new PowerStateMonitor(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(buildInterface());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        setupPhotoGestures();
        createMediaObserver();
        extractDefaultWallpapers();
        AlarmHelper.updateAlarmSchedule(this);
        applyKeepScreenOnPolicy();
        checkAndRequestStoragePermission();
    }

    /** Auto Backup restores preference strings but not SAF grants; discard only invalid tree entries. */
    private void removeExpiredTreeSources() {
        if (Build.VERSION.SDK_INT < 21) return;
        Set<String> saved = prefs.getStringSet(SettingsActivity.PHOTO_FOLDERS, null);
        if (saved == null || saved.isEmpty()) return;
        Set<String> valid = new HashSet<String>();
        boolean changed = false;
        List<android.content.UriPermission> grants = getContentResolver().getPersistedUriPermissions();
        for (String raw : saved) {
            String source = SettingsActivity.normalizeFolderSource(raw);
            Uri treeUri = SettingsActivity.treeUriFromSource(source);
            if (treeUri == null) {
                valid.add(source);
                continue;
            }
            boolean readable = false;
            for (android.content.UriPermission grant : grants) {
                if (treeUri.equals(grant.getUri()) && grant.isReadPermission()) {
                    readable = true;
                    break;
                }
            }
            if (readable) {
                valid.add(source);
            } else {
                changed = true;
            }
        }
        if (changed) {
            prefs.edit().putStringSet(SettingsActivity.PHOTO_FOLDERS, valid).apply();
        }
    }

    private void extractDefaultWallpapers() {
        if (prefs.getInt(WALLPAPER_PACK_VERSION_KEY, 0) >= WALLPAPER_PACK_VERSION) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.io.File targetDir = new java.io.File(getFilesDir(), "數位風景");
                    if (!targetDir.exists()) {
                        targetDir.mkdirs();
                    }
                    String[] assets = getAssets().list("digital_landscapes");
                    if (assets != null) {
                        for (String asset : assets) {
                            java.io.File outFile = new java.io.File(targetDir, asset);
                            if (!outFile.exists()) {
                                try (java.io.InputStream in = getAssets().open("digital_landscapes/" + asset);
                                     java.io.OutputStream out = new java.io.FileOutputStream(outFile)) {
                                    byte[] buffer = new byte[4096];
                                    int read;
                                    while ((read = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, read);
                                    }
                                }
                            }
                        }
                    }
                    java.util.Set<String> folders = prefs.getStringSet(SettingsActivity.PHOTO_FOLDERS, new java.util.HashSet<String>());
                    if (folders == null || folders.isEmpty()) {
                        folders = new java.util.HashSet<String>();
                        folders.add(targetDir.getAbsolutePath());
                        prefs.edit().putStringSet(SettingsActivity.PHOTO_FOLDERS, folders).apply();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (activityResumed) {
                                    startPhotoSlideshow();
                                }
                            }
                        });
                    }
                    prefs.edit().putInt(WALLPAPER_PACK_VERSION_KEY, WALLPAPER_PACK_VERSION)
                            .remove("wallpaper_extracted_internal_v2").apply();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activityResumed) {
                                invalidatePhotoCatalog();
                                startPhotoSlideshow();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /** Makes portrait and landscape clock layouts independent while preserving old settings. */
    private void migrateOrientationClockLayouts() {
        String[] orientations = { "_portrait", "_landscape" };
        float legacyX = prefs.getFloat(SettingsActivity.CLOCK_X_RATIO, 0.95f);
        float legacyY = prefs.getFloat(SettingsActivity.CLOCK_Y_RATIO, 0.90f);
        float legacyScale = prefs.getFloat(CLOCK_SCALE_FACTOR, 0.75f);
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        for (String orientation : orientations) {
            String xKey = CLOCK_POS_X_RATIO + orientation;
            String yKey = CLOCK_POS_Y_RATIO + orientation;
            String scaleKey = CLOCK_SCALE_FACTOR + orientation;
            if (!prefs.contains(xKey)) {
                editor.putFloat(xKey, legacyX);
                changed = true;
            }
            if (!prefs.contains(yKey)) {
                editor.putFloat(yKey, legacyY);
                changed = true;
            }
            if (!prefs.contains(scaleKey)) {
                editor.putFloat(scaleKey, legacyScale);
                changed = true;
            }
        }
        if (changed) editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        hideSystemUI();
        loadSettingsConfig();
        if (powerStateMonitor == null) powerStateMonitor = new PowerStateMonitor(this);
        powerStateMonitor.start(new PowerStateMonitor.Listener() {
            @Override
            public void onPowerStateChanged(PowerStateMonitor.State state) {
                handlePowerStateChanged(state);
            }
        });
        handlePowerStateChanged(powerStateMonitor.getState());
        scheduleSecondClockTicker();
        schedulePerformanceGuard();
        startPhotoSlideshow();
        if (hasPhotoReadAccess()) {
            registerMediaObserver();
        }
        registerLightSensor();
        scheduleBurnIn();
        scheduleWeatherRefresh();
        scheduleSafCatalogRefresh();
        updateAlarmIndicator();
        updatePomodoroDisplay();
        rootContainer.post(new Runnable() {
            @Override
            public void run() {
                restoreClockPosition();
            }
        });
    }

    @Override
    protected void onPause() {
        cancelPhotoActionLongPress();
        photoHandler.removeCallbacks(hideManualRefreshStatusRunnable);
        manualRefreshFeedbackVisible = false;
        resetManualRefreshBounce();
        if (currentPhotoSource != null) {
            prefs.edit().putString(LAST_PHOTO_KEY, currentPhotoSource.key()).apply();
        }
        activityResumed = false;
        photoHandler.removeCallbacks(secondClockTicker);
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.removeCallbacks(burnInRunnable);
        photoHandler.removeCallbacks(performanceGuardRunnable);
        photoHandler.removeCallbacks(mediaRefreshRunnable);
        photoHandler.removeCallbacks(weatherRefreshRunnable);
        photoHandler.removeCallbacks(safCatalogRefreshRunnable);
        photoHandler.removeCallbacks(pomodoroDialogTicker);
        photoHandler.removeCallbacks(hideFocusReminderRunnable);
        if (focusReminderOverlay != null) {
            focusReminderOverlay.setVisibility(View.GONE);
        }
        unregisterMediaObserver();
        unregisterLightSensor();
        if (powerStateMonitor != null) powerStateMonitor.stop();
        stopPhotoSlideshow();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cancelPhotoActionLongPress();
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.removeCallbacks(burnInRunnable);
        photoHandler.removeCallbacks(performanceGuardRunnable);
        photoHandler.removeCallbacks(mediaRefreshRunnable);
        photoHandler.removeCallbacks(weatherRefreshRunnable);
        photoHandler.removeCallbacks(safCatalogRefreshRunnable);
        photoHandler.removeCallbacks(pomodoroDialogTicker);
        photoHandler.removeCallbacks(hideFocusReminderRunnable);
        photoHandler.removeCallbacks(secondClockTicker);
        unregisterMediaObserver();
        unregisterLightSensor();
        if (powerStateMonitor != null) powerStateMonitor.stop();
        stopPhotoSlideshow();
        photoScanExecutor.shutdownNow();
        photoDecodeExecutor.shutdownNow();
        weatherExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            performanceMemoryPressureUntil = SystemClock.elapsedRealtime()
                    + PERFORMANCE_MEMORY_PRESSURE_MS;
            updatePerformanceGuard();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        performanceMemoryPressureUntil = Long.MAX_VALUE;
        if (selectedPerformanceMode == PerformanceModePolicy.SHOWCASE) {
            applyEffectivePerformanceMode(PerformanceModePolicy.ECO, true);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadClockScalePreferences(1.0f);
        applyClockScale();
        if (photoImage != null && photoBitmap != null) {
            applyPhotoPresentation(photoBitmap);
        }
        if (rootContainer != null) {
            rootContainer.post(new Runnable() {
                @Override
                public void run() {
                    if (pomodoroModeLayoutActive) {
                        applyPomodoroFocusLayout();
                    } else {
                        restoreClockPosition();
                    }
                }
            });
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        hideSettingsButton();
    }

    private void resetImmersiveTimeout() {
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.postDelayed(hideImmersiveRunnable, IMMERSIVE_TIMEOUT_MS);
    }

    private void setupPhotoGestures() {
        photoGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return !isTouchOnClock(event);
                    }

                    @Override
                    public boolean onFling(
                            MotionEvent start, MotionEvent end, float velocityX, float velocityY) {
                        if (start == null || end == null || photoLoading || isNightSleepActive) {
                            return false;
                        }
                        float deltaX = end.getX() - start.getX();
                        float deltaY = end.getY() - start.getY();
                        if (Math.abs(deltaX) < dp(72)
                                || Math.abs(deltaX) < Math.abs(deltaY) * 1.2f
                                || Math.abs(velocityX) < dp(180)) {
                            return false;
                        }
                        lastPhotoSwipeEndTime = SystemClock.elapsedRealtime();
                        if (deltaX < 0) {
                            loadNextPhoto();
                        } else {
                            loadPreviousPhoto();
                        }
                        return true;
                    }

                });
        // The system default is about half a second and is too easy to trigger while swiping.
        photoGestureDetector.setIsLongpressEnabled(false);
    }

    private boolean handlePhotoActionLongPress(MotionEvent event) {
        if (event == null) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            manualRefreshArmed = false;
            manualRefreshTriggered = false;
            if (canStartBackgroundGesture(event)) {
                photoActionDownX = event.getRawX();
                photoActionDownY = event.getRawY();
                manualRefreshDownX = photoActionDownX;
                manualRefreshDownY = photoActionDownY;
                photoActionLongPressPending = true;
                photoHandler.removeCallbacks(photoActionLongPressRunnable);
                photoHandler.postDelayed(photoActionLongPressRunnable, PHOTO_ACTION_LONG_PRESS_MS);
            }
            return false;
        }

        if (manualRefreshTriggered) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                manualRefreshTriggered = false;
            }
            return true;
        }

        if (manualRefreshArmed) {
            if (action == MotionEvent.ACTION_MOVE) {
                if (event.getPointerCount() > 1) {
                    cancelPhotoActionLongPress();
                    return false;
                }
                float deltaX = event.getRawX() - manualRefreshDownX;
                float deltaY = event.getRawY() - manualRefreshDownY;
                float pullDistance = dp(MANUAL_REFRESH_PULL_DISTANCE_DP);
                if (deltaY >= pullDistance && deltaY > Math.abs(deltaX) * 1.2f) {
                    triggerManualRefresh();
                    return true;
                }
                int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
                if (deltaY < -touchSlop
                        || (Math.abs(deltaX) > touchSlop
                        && Math.abs(deltaX) > Math.abs(deltaY) * 1.2f)) {
                    cancelPhotoActionLongPress();
                }
                return false;
            }
            if (action == MotionEvent.ACTION_UP) {
                manualRefreshArmed = false;
                showPhotoActions();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL || event.getPointerCount() > 1) {
                cancelPhotoActionLongPress();
                return false;
            }
            return false;
        }

        if (!photoActionLongPressPending) return false;
        if (action == MotionEvent.ACTION_MOVE) {
            int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
            if (Math.abs(event.getRawX() - photoActionDownX) > touchSlop
                    || Math.abs(event.getRawY() - photoActionDownY) > touchSlop
                    || event.getPointerCount() > 1) {
                cancelPhotoActionLongPress();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || event.getPointerCount() > 1) {
            cancelPhotoActionLongPress();
        }
        return false;
    }

    private void cancelPhotoActionLongPress() {
        photoActionLongPressPending = false;
        manualRefreshArmed = false;
        manualRefreshTriggered = false;
        photoHandler.removeCallbacks(photoActionLongPressRunnable);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event != null && event.getAction() == MotionEvent.ACTION_DOWN && isNightSleepActive) {
            triggerNightSleepWakeup();
        }
        if (event != null && event.getAction() == MotionEvent.ACTION_DOWN
                && shouldShowFocusReminder(event)) {
            showFocusReminder();
            return true;
        }
        if (handlePhotoActionLongPress(event)) return true;
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }
        if (photoGestureDetector != null && event.getPointerCount() == 1
                && !isDraggingClock && !isScalingClock) {
            photoGestureDetector.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean canStartBackgroundGesture(MotionEvent event) {
        return !isTouchOnClock(event)
                && !isTouchOnView(settingsButton, event)
                && !isTouchOnView(pomodoroButton, event)
                && !isTouchOnView(pomodoroFocusPanel, event)
                && !isTouchOnView(focusReminderOverlay, event)
                && !pomodoroModeLayoutActive
                && !photoLoading;
    }

    private void triggerManualRefresh() {
        manualRefreshTriggered = true;
        manualRefreshArmed = false;
        photoActionLongPressPending = false;
        photoHandler.removeCallbacks(photoActionLongPressRunnable);

        updatePhotoClock();
        long now = SystemClock.elapsedRealtime();
        if (lastManualRefreshAt > 0L
                && now - lastManualRefreshAt < MANUAL_REFRESH_MIN_INTERVAL_MS) {
            showManualRefreshStatus("請稍候再更新", SECONDARY, 1200L);
            playManualRefreshBounce();
            return;
        }
        lastManualRefreshAt = now;

        if (!weatherEnabled || Double.isNaN(weatherLatitude)
                || Double.isNaN(weatherLongitude)) {
            showManualRefreshStatus("畫面已更新", SECONDARY, 1200L);
            playManualRefreshBounce();
            return;
        }
        showManualRefreshStatus("正在更新…", SECONDARY, 0L);
        playManualRefreshBounce();
        requestWeatherRefresh(true, new WeatherRefreshCallback() {
            @Override
            public void onComplete(boolean success) {
                if (!activityResumed) return;
                showManualRefreshStatus(
                        success ? "已更新" : "天氣更新失敗，保留舊資料",
                        success ? PRIMARY : WARNING,
                        1400L);
            }
        });
    }

    private void showManualRefreshStatus(String message, int color, long hideAfterMs) {
        if (photoStatus == null) return;
        photoHandler.removeCallbacks(hideManualRefreshStatusRunnable);
        manualRefreshFeedbackVisible = true;
        showPhotoStatus(message, color);
        if (hideAfterMs > 0L) {
            photoHandler.postDelayed(hideManualRefreshStatusRunnable, hideAfterMs);
        }
    }

    /** One short compositor-friendly animation; no layout pass or persistent animator is used. */
    private void playManualRefreshBounce() {
        if (photoStatus == null) return;
        photoStatus.animate().cancel();
        photoStatus.setAlpha(0.65f);
        photoStatus.setScaleX(0.84f);
        photoStatus.setScaleY(0.84f);
        photoStatus.setTranslationY(-dp(18));
        photoStatus.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .translationY(0.0f)
                .setDuration(MANUAL_REFRESH_BOUNCE_DURATION_MS)
                .setInterpolator(manualRefreshBounceInterpolator)
                .start();
    }

    private void resetManualRefreshBounce() {
        if (photoStatus == null) return;
        photoStatus.animate().cancel();
        photoStatus.setAlpha(1.0f);
        photoStatus.setScaleX(1.0f);
        photoStatus.setScaleY(1.0f);
        photoStatus.setTranslationY(0.0f);
    }

    private boolean shouldShowFocusReminder(MotionEvent event) {
        if (!activityResumed || isNightSleepActive || pomodoroDialog != null
                || focusReminderOverlay == null
                || focusReminderOverlay.getVisibility() == View.VISIBLE) {
            return false;
        }
        if (isTouchOnView(pomodoroButton, event)) {
            return false;
        }
        PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(this);
        return snapshot.running && PomodoroHelper.PHASE_FOCUS.equals(snapshot.phase);
    }

    private boolean isTouchOnView(View view, MotionEvent event) {
        if (view == null || view.getVisibility() != View.VISIBLE || event == null) return false;
        Rect bounds = new Rect();
        if (!view.getGlobalVisibleRect(bounds)) return false;
        return bounds.contains(Math.round(event.getRawX()), Math.round(event.getRawY()));
    }

    private void showFocusReminder() {
        if (focusReminderOverlay == null) return;
        if (focusReminderImage != null && focusReminderMessage != null) {
            if (random.nextBoolean()) {
                focusReminderImage.setImageResource(focusReminderDrawable(true));
                focusReminderMessage.setText("專心一點！");
            } else {
                focusReminderImage.setImageResource(focusReminderDrawable(false));
                focusReminderMessage.setText("怎麼這麼不專心？");
            }
        }
        photoHandler.removeCallbacks(hideFocusReminderRunnable);
        focusReminderOverlay.setVisibility(View.VISIBLE);
        vibrateFocusReminder();
        photoHandler.postDelayed(hideFocusReminderRunnable, FOCUS_REMINDER_DURATION_MS);
    }

    private int focusReminderDrawable(boolean angry) {
        boolean portrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        if (portrait) {
            return angry ? R.drawable.focus_reminder_ang_portrait
                    : R.drawable.focus_reminder_cry_portrait;
        }
        return angry ? R.drawable.focus_reminder_ang : R.drawable.focus_reminder_cry;
    }

    @SuppressWarnings("deprecation")
    private void vibrateFocusReminder() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[] { 0L, 180L, 90L, 220L },
                        new int[] { 0, 255, 0, 255 }, -1));
            } else {
                vibrator.vibrate(new long[] { 0L, 180L, 90L, 220L }, -1);
            }
        } catch (RuntimeException ignored) {
            // Some tablets do not expose a usable vibrator; the visual reminder remains available.
        }
    }

    @SuppressWarnings("deprecation")
    private void vibratePhaseFinished() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            long[] pattern = new long[] { 0L, 180L, 130L, 180L, 130L, 250L };
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern,
                        new int[] { 0, 255, 0, 255, 0, 255 }, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (RuntimeException ignored) {
            // Devices without a usable motor still show the phase notification.
        }
    }

    private void triggerNightSleepWakeup() {
        photoHandler.removeCallbacks(nightSleepReDimRunnable);
        if (!isNightSleepWoken) {
            isNightSleepWoken = true;
            restoreNormalMode();
        }
        photoHandler.postDelayed(nightSleepReDimRunnable, 30000L);
    }

    private boolean isTouchOnClock(MotionEvent event) {
        if (event == null) return false;
        return isTouchOnView(timeBlock, event)
                || isTouchOnView(dateBlock, event)
                || isTouchOnView(weatherBlock, event)
                || isTouchOnView(legacyClockPanel, event);
    }

    private void showPhotoActions() {
        final PhotoSource source = currentPhotoSource;
        if (source == null) {
            return;
        }
        final String key = source.key();
        final boolean favorite = favoritePhotos.contains(key);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(12));

        TextView title = new TextView(this);
        title.setText("相片操作");
        title.setTextColor(PRIMARY);
        title.setTextSize(18);
        title.setTypeface(FontManager.getPomodoroChineseFont(this));
        content.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));

        TextView favoriteAction = photoActionRow(favorite ? "取消收藏" : "加入收藏", PRIMARY, PANEL_RAISED);
        TextView hideAction = photoActionRow("隱藏此相片", WARNING, PANEL_RAISED);
        TextView cancelAction = photoActionRow("取消", SECONDARY, PANEL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
        rowParams.topMargin = dp(6);
        content.addView(favoriteAction, rowParams);
        LinearLayout.LayoutParams hideParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
        hideParams.topMargin = dp(6);
        content.addView(hideAction, hideParams);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
        cancelParams.topMargin = dp(6);
        content.addView(cancelAction, cancelParams);

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(content).create();
        favoriteAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favorite) favoritePhotos.remove(key);
                else favoritePhotos.add(key);
                prefs.edit().putStringSet(
                        SettingsActivity.FAVORITE_PHOTOS,
                        new HashSet<String>(favoritePhotos)).apply();
                Toast.makeText(PhotoClockActivity.this,
                        favorite ? "已取消收藏" : "已加入收藏",
                        Toast.LENGTH_SHORT).show();
                if (favoritesOnly && favorite) startPhotoSlideshow();
                dialog.dismiss();
            }
        });
        hideAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hiddenPhotos.add(key);
                prefs.edit().putStringSet(
                        SettingsActivity.HIDDEN_PHOTOS,
                        new HashSet<String>(hiddenPhotos)).apply();
                startPhotoSlideshow();
                dialog.dismiss();
            }
        });
        cancelAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
        styleModernDialog(dialog);
    }

    private TextView photoActionRow(String label, int textColor, int backgroundColor) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(textColor);
        row.setTextSize(14);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTypeface(FontManager.getPomodoroChineseFont(this));
        row.setPadding(dp(14), 0, dp(14), 0);
        row.setMinHeight(0);
        row.setMinimumHeight(0);
        row.setBackground(rounded(backgroundColor));
        return row;
    }

    private void createMediaObserver() {
        mediaObserver = new ContentObserver(photoHandler) {
            @Override
            public void onChange(boolean selfChange) {
                photoHandler.removeCallbacks(mediaRefreshRunnable);
                photoHandler.postDelayed(mediaRefreshRunnable, MEDIA_REFRESH_DELAY_MS);
            }
        };
    }

    private void registerMediaObserver() {
        if (!mediaObserverRegistered && mediaObserver != null) {
            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver);
            mediaObserverRegistered = true;
        }
    }

    private void unregisterMediaObserver() {
        if (mediaObserverRegistered && mediaObserver != null) {
            getContentResolver().unregisterContentObserver(mediaObserver);
            mediaObserverRegistered = false;
        }
    }

    private void scheduleBurnIn() {
        photoHandler.removeCallbacks(burnInRunnable);
        if (burnInEnabled) {
            photoHandler.postDelayed(burnInRunnable, BURN_IN_INTERVAL_MS);
        } else {
            burnInOffsetX = 0.0f;
            burnInOffsetY = 0.0f;
            applyClockTranslation();
        }
    }

    private void registerLightSensor() {
        if (autoBrightnessEnabled && sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(
                    lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void unregisterLightSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(lightListener);
        }
    }

    private void applyAmbientBrightness(float lux) {
        if (!autoBrightnessEnabled || isNightSleepActive) {
            return;
        }
        if (lastLightLevel >= 0.0f
                && Math.abs(lux - lastLightLevel) < Math.max(2.0f, lastLightLevel * 0.15f)) {
            return;
        }
        lastLightLevel = lux;
        float normalized = (float) (Math.log10(lux + 1.0f) / 4.0f);
        float brightness = 0.08f + Math.max(0.0f, Math.min(0.92f, normalized * 0.92f));
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = brightness;
        getWindow().setAttributes(params);
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT < 23 || !requiresBroadPhotoPermission()
                || hasPhotoReadAccess()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 34) {
            requestPermissions(new String[] {
                    "android.permission.READ_MEDIA_IMAGES",
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            }, PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[] { "android.permission.READ_MEDIA_IMAGES" },
                    PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPhotoReadAccess() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission("android.permission.READ_MEDIA_IMAGES")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSelectedPhotoAccess() {
        return Build.VERSION.SDK_INT >= 34
                && checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean requiresBroadPhotoPermission() {
        Set<String> configured = prefs.getStringSet(SettingsActivity.PHOTO_FOLDERS, null);
        if (configured == null || configured.isEmpty()) {
            return false;
        }
        String appFiles = getFilesDir().getAbsolutePath();
        for (String source : configured) {
            String path = SettingsActivity.filePathFromSource(source);
            if (path != null && !path.equals(appFiles)
                    && !path.startsWith(appFiles + File.separator)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasPhotoReadAccess()) {
                invalidatePhotoCatalog();
                startPhotoSlideshow();
            } else if (hasSelectedPhotoAccess() && requiresBroadPhotoPermission()) {
                showPhotoStatus("已選取部分相片；請到設定用「選擇資料夾」授權整個相簿", WARNING);
            } else if (requiresBroadPhotoPermission()) {
                showPhotoStatus("需要相簿讀取權限以播放照片", WARNING);
            }
        }
    }

    private void loadSettingsConfig() {
        int seconds = prefs.getInt(SettingsActivity.PHOTO_INTERVAL_SECONDS, SettingsActivity.DEFAULT_INTERVAL_SECONDS);
        photoIntervalMs = seconds * 1000L;
        photoPanDurationMs = Math.max(2000L, photoIntervalMs - 2000L);
        playbackOrder = PlaybackOrderPolicy.normalize(
                prefs.getInt(SettingsActivity.PHOTO_PLAYBACK_ORDER, PlaybackOrderPolicy.RANDOM));
        clockSecondsMode = ClockSecondPolicy.normalize(
                prefs.getInt(SettingsActivity.CLOCK_SECONDS_MODE, ClockSecondPolicy.OFF));
        keepScreenMode = PowerStatePolicy.normalizeKeepAwakeMode(
                prefs.getInt(SettingsActivity.KEEP_SCREEN_MODE,
                        PowerStatePolicy.KEEP_AWAKE_ALWAYS));
        lowBatteryGuard = prefs.getBoolean(SettingsActivity.LOW_BATTERY_GUARD, false);
        batteryDisplayMode = Math.max(0, Math.min(2,
                prefs.getInt(SettingsActivity.BATTERY_DISPLAY_MODE, 0)));
        clockBgEnabled = prefs.getBoolean(SettingsActivity.CLOCK_BACKGROUND_ENABLED, false);
        int legacyFontStyle = prefs.getInt(SettingsActivity.CLOCK_FONT_STYLE, 0);
        String defaultFont = BuildConfig.INCLUDE_STOROPIA ? "asset:font_storopia.ttf" : "asset:font_oxanium.ttf";
        clockFontId = FontManager.normalizeId(this, prefs.getString(
                SettingsActivity.CLOCK_FONT_ID,
                legacyFontStyle == 0 ? defaultFont : FontManager.getIdForLegacyIndex(legacyFontStyle)));
        dateFontId = FontManager.normalizeId(this, prefs.getString(
                SettingsActivity.DATE_FONT_ID, clockFontId));
        weatherFontId = FontManager.normalizeId(this, prefs.getString(
                SettingsActivity.WEATHER_FONT_ID, clockFontId));
        nightModeEnabled = prefs.getBoolean(SettingsActivity.NIGHT_MODE_ENABLED, false);
        nightStartHour = prefs.getInt(SettingsActivity.NIGHT_START_HOUR, 23);
        nightEndHour = prefs.getInt(SettingsActivity.NIGHT_END_HOUR, 7);
        transitionType = prefs.getInt(SettingsActivity.TRANSITION_TYPE, 0);
        clockTimeEnabled = prefs.getBoolean(SettingsActivity.CLOCK_TIME_ENABLED, true);
        clockDateEnabled = prefs.getBoolean(SettingsActivity.CLOCK_DATE_ENABLED, true);

        adaptiveColorEnabled = prefs.getBoolean(SettingsActivity.ADAPTIVE_COLOR_ENABLED, true);
        polaroidFrameEnabled = prefs.getBoolean(SettingsActivity.POLAROID_FRAME_ENABLED, false);
        smartFocusEnabled = prefs.getBoolean(SettingsActivity.SMART_FOCUS_ENABLED, true);
        selectedPerformanceMode = prefs.contains(SettingsActivity.PERFORMANCE_MODE)
                ? PerformanceModePolicy.normalize(prefs.getInt(
                        SettingsActivity.PERFORMANCE_MODE, PerformanceModePolicy.ECO))
                : PerformanceModePolicy.fromLegacy(prefs.getBoolean(
                        SettingsActivity.LOW_POWER_MODE, true));
        effectivePerformanceMode = selectedPerformanceMode;
        performanceMemoryPressureUntil = 0L;
        burnInEnabled = prefs.getBoolean(SettingsActivity.BURN_IN_ENABLED, true);
        autoBrightnessEnabled = prefs.getBoolean(SettingsActivity.AUTO_BRIGHTNESS_ENABLED, false);
        favoritesOnly = prefs.getBoolean(SettingsActivity.FAVORITES_ONLY, false);
        photoDisplayMode = Math.max(0, Math.min(2,
                prefs.getInt(SettingsActivity.PHOTO_DISPLAY_MODE, 0)));
        weatherEnabled = prefs.getBoolean(SettingsActivity.WEATHER_ENABLED, false);
        weatherShowLocation = prefs.getBoolean(SettingsActivity.WEATHER_SHOW_LOCATION, false);
        weatherMinimalLocation = prefs.getBoolean(
                SettingsActivity.WEATHER_MINIMAL_LOCATION, false);
        weatherCompactMode = prefs.getBoolean(SettingsActivity.WEATHER_COMPACT_MODE, true);
        weatherExtendedEnabled = prefs.getBoolean(
                SettingsActivity.WEATHER_EXTENDED_ENABLED, false);
        weatherExtendedForecastEnabled = prefs.getBoolean(
                SettingsActivity.WEATHER_EXTENDED_FORECAST_ENABLED, true);
        weatherExtendedDaylightEnabled = prefs.getBoolean(
                SettingsActivity.WEATHER_EXTENDED_DAYLIGHT_ENABLED, true);
        weatherLocationName = prefs.getString(SettingsActivity.WEATHER_LOCATION_NAME, "");
        weatherTimezone = prefs.getString(SettingsActivity.WEATHER_TIMEZONE, "auto");
        weatherLatitude = parseDouble(prefs.getString(SettingsActivity.WEATHER_LATITUDE, null));
        weatherLongitude = parseDouble(prefs.getString(SettingsActivity.WEATHER_LONGITUDE, null));
        pomodoroAdvancedDisplay = PomodoroHelper.normalizeDisplayMode(prefs.getInt(
                PomodoroHelper.PREF_DISPLAY_MODE, PomodoroHelper.DISPLAY_MODE_ORIGINAL))
                == PomodoroHelper.DISPLAY_MODE_ADVANCED;
        loadClockScalePreferences(0.75f);
        applyClockLayoutMode();

        favoritePhotos.clear();
        hiddenPhotos.clear();
        Set<String> savedFavorites = prefs.getStringSet(SettingsActivity.FAVORITE_PHOTOS, null);
        Set<String> savedHidden = prefs.getStringSet(SettingsActivity.HIDDEN_PHOTOS, null);
        if (savedFavorites != null) favoritePhotos.addAll(savedFavorites);
        if (savedHidden != null) hiddenPhotos.addAll(savedHidden);

        refreshPerformanceModePresentation();
        applyClockScale();
    }

    private boolean isLowPowerModeActive() {
        return PerformanceModePolicy.isEco(effectivePerformanceMode);
    }

    private boolean isShowcaseModeActive() {
        return PerformanceModePolicy.isShowcase(effectivePerformanceMode);
    }

    private void schedulePerformanceGuard() {
        photoHandler.removeCallbacks(performanceGuardRunnable);
        if (!activityResumed || selectedPerformanceMode != PerformanceModePolicy.SHOWCASE) {
            return;
        }
        updatePerformanceGuard();
        photoHandler.postDelayed(performanceGuardRunnable, PERFORMANCE_GUARD_INTERVAL_MS);
    }

    private void handlePowerStateChanged(PowerStateMonitor.State state) {
        if (state == null) return;
        powerState = state;
        applyKeepScreenOnPolicy();
        boolean shouldForceEco = PowerStatePolicy.shouldForceEco(
                lowBatteryGuard, state.batteryPresent, state.plugged, state.levelPercent);
        boolean shouldPause = PowerStatePolicy.shouldPauseAutomaticPhotos(
                lowBatteryGuard, state.batteryPresent, state.plugged, state.levelPercent);
        boolean wasPaused = lowBatteryPaused;
        boolean wasForcedEco = lowBatteryForcedEco;
        if (!lowBatteryGuard) {
            lowBatteryForcedEco = false;
            lowBatteryPaused = false;
        } else {
            lowBatteryForcedEco = shouldForceEco;
            lowBatteryPaused = shouldPause;
            if (wasPaused && !shouldPause
                    && !PowerStatePolicy.shouldResumeAutomaticPhotos(
                            state.levelPercent, state.plugged)) {
                lowBatteryPaused = true;
            }
            if (wasForcedEco && !shouldForceEco
                    && !PowerStatePolicy.shouldResumeForcedEco(
                            state.levelPercent, state.plugged)) {
                lowBatteryForcedEco = true;
            }
        }
        if (wasPaused != lowBatteryPaused) {
            if (lowBatteryPaused) {
                stopPhotoPan();
            } else if (photoBitmap != null && !isNightSleepActive) {
                startPhotoPan();
                nextPhotoAt = SystemClock.elapsedRealtime() + photoIntervalMs;
            }
            schedulePhotoTicker();
        }
        if (batteryStatusView != null) {
            batteryStatusView.setBatteryState(state);
            updateBatteryStatusVisibility();
        }
        updateClockTimeText();
        scheduleSecondClockTicker();
        updateEffectivePerformanceForPower();
    }

    private void applyKeepScreenOnPolicy() {
        PowerStateMonitor.State state = powerState == null
                ? PowerStateMonitor.State.unknown() : powerState;
        boolean keep = PowerStatePolicy.shouldKeepScreenOn(
                keepScreenMode, state.batteryPresent, state.plugged, state.stateKnown);
        if (keep) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void updateEffectivePerformanceForPower() {
        int desired = lowBatteryForcedEco
                ? PerformanceModePolicy.ECO : effectivePerformanceMode;
        if (desired != effectivePerformanceMode) {
            applyEffectivePerformanceMode(desired, false);
        } else if (!lowBatteryForcedEco && selectedPerformanceMode != PerformanceModePolicy.SHOWCASE
                && effectivePerformanceMode != selectedPerformanceMode) {
            applyEffectivePerformanceMode(selectedPerformanceMode, false);
        }
    }

    private void updateBatteryStatusVisibility() {
        if (batteryStatusView == null) return;
        PowerStateMonitor.State state = powerState == null
                ? PowerStateMonitor.State.unknown() : powerState;
        boolean visible = batteryDisplayMode == 1
                || (batteryDisplayMode == 2 && state.batteryPresent && !state.plugged);
        batteryStatusView.setVisibility(
                visible && state.stateKnown && state.batteryPresent
                        ? View.VISIBLE : View.GONE);
    }

    private void updatePerformanceGuard() {
        if (selectedPerformanceMode != PerformanceModePolicy.SHOWCASE) {
            applyEffectivePerformanceMode(
                    lowBatteryForcedEco ? PerformanceModePolicy.ECO : selectedPerformanceMode,
                    false);
            return;
        }
        if (lowBatteryForcedEco) {
            applyEffectivePerformanceMode(PerformanceModePolicy.ECO, false);
            return;
        }
        lastBatteryTemperatureC = readBatteryTemperature();
        boolean memoryPressure = SystemClock.elapsedRealtime() < performanceMemoryPressureUntil;
        int resolved = PerformanceModePolicy.resolveEffectiveMode(
                selectedPerformanceMode,
                effectivePerformanceMode,
                lastBatteryTemperatureC,
                memoryPressure);
        applyEffectivePerformanceMode(resolved, true);
    }

    private float readBatteryTemperature() {
        if (powerState != null && !Float.isNaN(powerState.temperatureCelsius)) {
            return powerState.temperatureCelsius;
        }
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null && battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
                return battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f;
            }
        } catch (RuntimeException ignored) {
            // Some old vendor builds do not expose the sticky battery broadcast.
        }
        return Float.NaN;
    }

    private void applyEffectivePerformanceMode(int mode, boolean notifyUser) {
        int normalized = PerformanceModePolicy.normalize(mode);
        if (effectivePerformanceMode == normalized) return;
        int previous = effectivePerformanceMode;
        effectivePerformanceMode = normalized;
        if (rootContainer == null || !activityResumed) return;

        refreshPerformanceModePresentation();
        if (notifyUser && selectedPerformanceMode == PerformanceModePolicy.SHOWCASE
                && previous == PerformanceModePolicy.SHOWCASE
                && normalized != PerformanceModePolicy.SHOWCASE) {
            String message = normalized == PerformanceModePolicy.ECO
                    ? "裝置溫度較高，已降低效果"
                    : "裝置資源緊張，已降低效果";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshPerformanceModePresentation() {
        applyPolaroidStyle();
        updateShowcaseColorOverlay();
        updateClockStyle();
        if (photoBitmap != null) {
            applyPhotoPresentation(photoBitmap);
            if (effectiveDisplayMode() == 0 && !isNightSleepActive) {
                startPhotoPan();
            }
        } else {
            stopPhotoPan();
        }
    }

    private void applyPolaroidStyle() {
        if (polaroidContainer == null) {
            return;
        }
        if (polaroidFrameEnabled && !isLowPowerModeActive()) {
            GradientDrawable polaroidBg = new GradientDrawable();
            polaroidBg.setColor(Color.rgb(250, 248, 245));
            polaroidBg.setCornerRadius(dp(8));
            polaroidContainer.setBackground(polaroidBg);
            polaroidContainer.setPadding(dp(12), dp(12), dp(12), dp(36));
        } else {
            polaroidContainer.setBackground(null);
            polaroidContainer.setPadding(0, 0, 0, 0);
        }
    }

    private void updateShowcaseColorOverlay() {
        if (showcaseColorOverlay == null) return;
        showcaseColorOverlay.animate().cancel();
        if (!isShowcaseModeActive() || !adaptiveColorEnabled) {
            showcaseColorOverlay.setAlpha(0.0f);
            showcaseColorOverlay.setBackground(null);
            showcaseColorOverlay.setVisibility(View.GONE);
            return;
        }
        int color = currentDominantColor;
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {
                        Color.argb(112, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
                });
        showcaseColorOverlay.setBackground(gradient);
        showcaseColorOverlay.setVisibility(View.VISIBLE);
        showcaseColorOverlay.setAlpha(0.0f);
        showcaseColorOverlay.animate().alpha(0.16f).setDuration(1600L)
                .setInterpolator(smoothInterpolator).start();
    }

    private void checkNightSleepMode() {
        if (!nightModeEnabled) {
            if (isNightSleepActive) {
                isNightSleepActive = false;
                isNightSleepWoken = false;
                photoHandler.removeCallbacks(nightSleepReDimRunnable);
                restoreNormalMode();
            }
            return;
        }

        @SuppressWarnings("deprecation")
        int currentHour = nowDate.getHours();
        boolean shouldSleep;
        if (nightStartHour > nightEndHour) {
            shouldSleep = currentHour >= nightStartHour || currentHour < nightEndHour;
        } else {
            shouldSleep = currentHour >= nightStartHour && currentHour < nightEndHour;
        }

        if (shouldSleep && !isNightSleepActive) {
            isNightSleepActive = true;
            isNightSleepWoken = false;
            enterNightSleepMode();
        } else if (!shouldSleep && isNightSleepActive) {
            isNightSleepActive = false;
            isNightSleepWoken = false;
            photoHandler.removeCallbacks(nightSleepReDimRunnable);
            restoreNormalMode();
        }
    }

    private void enterNightSleepMode() {
        photoHandler.removeCallbacks(secondClockTicker);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.02f;
        getWindow().setAttributes(lp);

        stopPhotoPan();
        if (photoImage != null) {
            photoImage.animate().cancel();
            photoImage.animate().alpha(0.0f).setDuration(1400).start();
        }
        if (backgroundImage != null) {
            backgroundImage.animate().cancel();
            backgroundImage.animate().alpha(0.0f).setDuration(1400).start();
        }
        if (photoTime != null) {
            photoTime.setTextColor(Color.argb(120, 100, 100, 100));
        }
        if (photoDate != null) {
            photoDate.setTextColor(Color.argb(120, 100, 100, 100));
        }
        if (compactWeatherRow != null) compactWeatherRow.setAlpha(0.35f);
        if (weatherRow != null) weatherRow.setAlpha(0.35f);
    }

    private void restoreNormalMode() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
        if (autoBrightnessEnabled && lastLightLevel >= 0.0f) {
            applyAmbientBrightness(lastLightLevel);
        }

        if (photoTime != null) {
            photoTime.setTextColor(Color.WHITE);
        }
        if (photoDate != null) {
            photoDate.setTextColor(Color.WHITE);
        }
        if (compactWeatherRow != null) compactWeatherRow.setAlpha(1.0f);
        if (weatherRow != null) weatherRow.setAlpha(1.0f);
        if (photoImage != null && photoBitmap != null) {
            photoImage.animate().cancel();
            photoImage.animate().alpha(1.0f).setDuration(1600).start();
            startPhotoPan();
        }
        scheduleSecondClockTicker();
        if (backgroundImage != null) {
            backgroundImage.animate().cancel();
            backgroundImage.animate().alpha(1.0f).setDuration(1600).start();
        }
    }

    private void updateClockStyle() {
        if (clockPanel == null) {
            return;
        }

        android.graphics.Typeface clockTypeface = FontManager.getFont(this, clockFontId);
        android.graphics.Typeface dateTypeface = FontManager.getFont(this, dateFontId);
        android.graphics.Typeface weatherTypeface = FontManager.getFont(this, weatherFontId);

        if (photoTime != null) {
            photoTime.setTypeface(clockTypeface);
            photoTime.setVisibility(clockTimeEnabled ? View.VISIBLE : View.GONE);
        }
        if (photoDate != null) {
            photoDate.setTypeface(dateTypeface);
            photoDate.setVisibility(clockDateEnabled ? View.VISIBLE : View.GONE);
        }
        if (weatherTemperature != null) weatherTemperature.setTypeface(weatherTypeface);
        if (compactWeatherTemperature != null) compactWeatherTemperature.setTypeface(weatherTypeface);
        if (weatherLocation != null) weatherLocation.setTypeface(weatherTypeface);
        if (weatherForecastText != null) weatherForecastText.setTypeface(weatherTypeface);
        if (daylightLabel != null) daylightLabel.setTypeface(weatherTypeface);
        if (alarmTimeText != null) alarmTimeText.setTypeface(clockTypeface);
        if (pomodoroLabel != null) pomodoroLabel.setTypeface(clockTypeface);
        if (pomodoroText != null) pomodoroText.setTypeface(clockTypeface);
        updateTimeBlockVisibility();
        updateDateBlockVisibility();
        updateWeatherBlockVisibility();
        updatePhotoClock();

        applyClockBlockBackgrounds();
    }

    private void updateDateBlockVisibility() {
        if (dateBlock == null) return;
        boolean visible = (photoDate != null && photoDate.getVisibility() == View.VISIBLE)
                || (alarmRow != null && alarmRow.getVisibility() == View.VISIBLE)
                || (legacyClockPanelActive && compactWeatherRow != null
                && compactWeatherRow.getVisibility() == View.VISIBLE);
        dateBlock.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateWeatherBlockVisibility() {
        if (weatherBlock == null) return;
        boolean extendedVisible = weatherExtendedPanel != null
                && weatherExtendedPanel.getVisibility() == View.VISIBLE;
        boolean visible = (compactWeatherRow != null
                && compactWeatherRow.getVisibility() == View.VISIBLE)
                || (weatherRow != null && weatherRow.getVisibility() == View.VISIBLE)
                || extendedVisible;
        if (legacyClockPanelActive) {
            // In the .13-style surface, compact weather lives beside the date.
            visible = (weatherRow != null && weatherRow.getVisibility() == View.VISIBLE)
                    || extendedVisible;
        }
        weatherBlock.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void applyClockBlockBackgrounds() {
        if (timeBlock == null || dateBlock == null || weatherBlock == null) return;
        if (legacyClockPanelActive) {
            clearClockBlockBackground(timeBlock);
            clearClockBlockBackground(dateBlock);
            clearClockBlockBackground(weatherBlock);
            applyLegacyClockPanelBackground();
            return;
        }
        if (!clockBgEnabled) {
            clearClockBlockBackground(timeBlock);
            clearClockBlockBackground(dateBlock);
            clearClockBlockBackground(weatherBlock);
            return;
        }

        int bgColor = isAdaptiveColorActive()
                ? Color.argb(85, Color.red(currentDominantColor),
                Color.green(currentDominantColor), Color.blue(currentDominantColor))
                : Color.argb(65, 0, 0, 0);
        applyClockBlockBackground(timeBlock, bgColor);
        applyClockBlockBackground(dateBlock, bgColor);
        applyClockBlockBackground(weatherBlock, bgColor);
    }

    private void applyLegacyClockPanelBackground() {
        if (legacyClockPanel == null) return;
        if (!clockBgEnabled) {
            legacyClockPanel.setBackground(null);
            legacyClockPanel.setPadding(0, 0, 0, 0);
            return;
        }
        int bgColor = isAdaptiveColorActive()
                ? Color.argb(85, Color.red(currentDominantColor),
                Color.green(currentDominantColor), Color.blue(currentDominantColor))
                : Color.argb(65, 0, 0, 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(bgColor);
        background.setCornerRadius(dp(10));
        legacyClockPanel.setBackground(background);
        legacyClockPanel.setPadding(dp(12), dp(4), dp(12), dp(6));
    }

    private void applyClockBlockBackground(View block, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(10));
        block.setBackground(background);
        block.setPadding(dp(12), dp(4), dp(12), dp(6));
    }

    private void clearClockBlockBackground(View block) {
        block.setBackground(null);
        block.setPadding(0, 0, 0, 0);
    }

    private void updateTimeBlockVisibility() {
        if (timeBlock == null) return;
        timeBlock.setVisibility(photoTime != null
                && photoTime.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    /**
     * Switches the linked-size mode to the original .13 single-panel model.
     *
     * The independent modes keep three frame blocks with saved coordinates. The linked mode
     * intentionally uses one wrap-content vertical layout instead, so scaling and dragging act
     * on the same surface that the original release used.
     */
    private void applyClockLayoutMode() {
        if (clockPanel == null || legacyClockPanel == null) return;
        boolean shouldUseLegacyPanel = isOriginalLayout();
        if (shouldUseLegacyPanel == legacyClockPanelActive) return;
        if (shouldUseLegacyPanel) {
            enterLegacyClockPanelMode();
        } else {
            exitLegacyClockPanelMode();
        }
    }

    private void enterLegacyClockPanelMode() {
        legacyClockPanelActive = true;
        moveClockBlocksToLegacyPanel();
        legacyClockTranslationX = 0.0f;
        legacyClockTranslationY = 0.0f;
        legacyClockPanel.setVisibility(View.VISIBLE);
        applyLegacyClockTranslation();
        updateDateBlockVisibility();
        updateWeatherBlockVisibility();
        applyClockBlockBackgrounds();
    }

    private void exitLegacyClockPanelMode() {
        legacyClockPanelActive = false;
        moveClockBlocksToIndependentPanel();
        legacyClockPanel.setVisibility(View.GONE);
        applyLegacyClockTranslation();
        updateDateBlockVisibility();
        updateWeatherBlockVisibility();
        applyClockBlockBackgrounds();
    }

    private void moveClockBlocksToLegacyPanel() {
        moveCompactWeatherRowToLegacyPanel();
        moveBlockToParent(timeBlock, legacyClockPanel);
        moveBlockToParent(dateBlock, legacyClockPanel);
        moveBlockToParent(weatherBlock, legacyClockPanel);
        resetBlockTranslation(timeBlock);
        resetBlockTranslation(dateBlock);
        resetBlockTranslation(weatherBlock);
    }

    private void moveClockBlocksToIndependentPanel() {
        moveCompactWeatherRowToIndependentPanel();
        moveBlockToParent(timeBlock, clockPanel);
        moveBlockToParent(dateBlock, clockPanel);
        moveBlockToParent(weatherBlock, clockPanel);
        applyClockTranslation();
    }

    private void moveBlockToParent(View block, ViewGroup parent) {
        if (block == null || parent == null) return;
        if (block.getParent() == parent) return;
        if (block.getParent() instanceof ViewGroup) {
            ((ViewGroup) block.getParent()).removeView(block);
        }
        if (parent == legacyClockPanel) {
            parent.addView(block, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            parent.addView(block, clockBlockLayoutParams());
        }
    }

    private void resetBlockTranslation(View block) {
        if (block == null) return;
        block.setTranslationX(0.0f);
        block.setTranslationY(0.0f);
    }

    private void moveCompactWeatherRowToLegacyPanel() {
        if (compactWeatherRow == null || dateRow == null) return;
        if (compactWeatherRow.getParent() == dateRow) return;
        if (compactWeatherRow.getParent() instanceof ViewGroup) {
            ((ViewGroup) compactWeatherRow.getParent()).removeView(compactWeatherRow);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        params.setMargins(0, 0, dp(9), dp(3));
        dateRow.addView(compactWeatherRow, 0, params);
    }

    private void moveCompactWeatherRowToIndependentPanel() {
        if (compactWeatherRow == null || weatherContentPanel == null) return;
        if (compactWeatherRow.getParent() == weatherContentPanel) return;
        if (compactWeatherRow.getParent() instanceof ViewGroup) {
            ((ViewGroup) compactWeatherRow.getParent()).removeView(compactWeatherRow);
        }
        weatherContentPanel.addView(compactWeatherRow, 0, compactWeatherBlockLayoutParams());
    }

    private LinearLayout.LayoutParams compactWeatherBlockLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private View buildInterface() {
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(BACKGROUND);
        rootContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastClockDragEndTime < 400
                        || now - lastPhotoSwipeEndTime < 400) {
                    return;
                }
                showSettingsButton();
                resetImmersiveTimeout();
            }
        });

        // 相片繪圖層
        polaroidContainer = new FrameLayout(this);
        backgroundImage = new ImageView(this);
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImage.setVisibility(View.GONE);
        polaroidContainer.addView(backgroundImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        photoImage = new ImageView(this);
        photoImage.setScaleType(ImageView.ScaleType.MATRIX);
        polaroidContainer.addView(photoImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        rootContainer.addView(polaroidContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        showcaseColorOverlay = new View(this);
        showcaseColorOverlay.setClickable(false);
        showcaseColorOverlay.setFocusable(false);
        showcaseColorOverlay.setVisibility(View.GONE);
        rootContainer.addView(showcaseColorOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        photoStatus = new TextView(this);
        photoStatus.setTextSize(17);
        photoStatus.setTextColor(SECONDARY);
        photoStatus.setGravity(Gravity.CENTER);
        photoStatus.setLineSpacing(0, 1.25f);
        rootContainer.addView(photoStatus, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        clockPanel = new AccessibleFrameLayout(this);
        clockPanel.setClipChildren(false);
        clockPanel.setClipToPadding(false);

        legacyClockPanel = new LinearLayout(this);
        legacyClockPanel.setOrientation(LinearLayout.VERTICAL);
        legacyClockPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        legacyClockPanel.setClipChildren(false);
        legacyClockPanel.setClipToPadding(false);
        legacyClockPanel.setVisibility(View.GONE);

        timeBlock = new AccessibleFrameLayout(this);
        timeBlock.setContentDescription("時間區塊");
        photoTime = new TextView(this);
        photoTime.setTextSize(64);
        photoTime.setTextColor(Color.WHITE);
        photoTime.setGravity(Gravity.CENTER_HORIZONTAL);
        photoTime.setTypeface(Typeface.DEFAULT_BOLD);
        photoTime.setIncludeFontPadding(false);
        photoTime.setShadowLayer(dp(3), dp(1), dp(1), Color.BLACK);
        timeBlock.addView(photoTime, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        clockPanel.addView(timeBlock, clockBlockLayoutParams());

        dateBlock = new AccessibleFrameLayout(this);
        dateBlock.setContentDescription("日期區塊");
        dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

        photoDate = new TextView(this);
        photoDate.setTextSize(24);
        photoDate.setTextColor(Color.WHITE);
        photoDate.setGravity(Gravity.CENTER_HORIZONTAL);
        photoDate.setIncludeFontPadding(false);
        photoDate.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        dateRow.addView(photoDate, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        alarmRow = new LinearLayout(this);
        alarmRow.setOrientation(LinearLayout.HORIZONTAL);
        alarmRow.setGravity(Gravity.BOTTOM);
        alarmRow.setVisibility(View.GONE);

        alarmIcon = new AlarmIconView(this);
        alarmRow.addView(alarmIcon, new LinearLayout.LayoutParams(dp(16), dp(16)));

        alarmTimeText = new TextView(this);
        alarmTimeText.setTextSize(14);
        alarmTimeText.setTextColor(0xFF4FC3F7);
        alarmTimeText.setIncludeFontPadding(false);
        alarmTimeText.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(dp(3), 0, 0, 0);
        alarmRow.addView(alarmTimeText, textParams);

        LinearLayout.LayoutParams alarmParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        alarmParams.gravity = Gravity.BOTTOM;
        alarmParams.setMargins(dp(4), 0, 0, dp(3));
        dateRow.addView(alarmRow, alarmParams);

        batteryStatusView = new BatteryStatusView(this);
        batteryStatusView.setVisibility(View.GONE);
        dateBlock.addView(dateRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        clockPanel.addView(dateBlock, clockBlockLayoutParams());

        weatherBlock = new AccessibleFrameLayout(this);
        weatherBlock.setContentDescription("天氣區塊");

        weatherContentPanel = new LinearLayout(this);
        weatherContentPanel.setOrientation(LinearLayout.VERTICAL);
        weatherContentPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        weatherBlock.addView(weatherContentPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        compactWeatherRow = new LinearLayout(this);
        compactWeatherRow.setOrientation(LinearLayout.HORIZONTAL);
        compactWeatherRow.setGravity(Gravity.BOTTOM);
        compactWeatherRow.setVisibility(View.GONE);

        compactWeatherIcon = new WeatherIconView(this);
        compactWeatherRow.addView(
                compactWeatherIcon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        compactWeatherTemperature = new TextView(this);
        compactWeatherTemperature.setTextSize(18);
        compactWeatherTemperature.setTextColor(Color.WHITE);
        compactWeatherTemperature.setIncludeFontPadding(false);
        compactWeatherTemperature.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        LinearLayout.LayoutParams compactTemperatureParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        compactTemperatureParams.setMargins(dp(3), 0, 0, 0);
        compactWeatherRow.addView(compactWeatherTemperature, compactTemperatureParams);
        weatherContentPanel.addView(compactWeatherRow, compactWeatherBlockLayoutParams());

        weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setGravity(Gravity.CENTER);
        weatherRow.setVisibility(View.GONE);

        weatherIcon = new WeatherIconView(this);
        weatherRow.addView(weatherIcon, new LinearLayout.LayoutParams(dp(30), dp(30)));

        weatherTemperature = new TextView(this);
        weatherTemperature.setTextSize(20);
        weatherTemperature.setTextColor(Color.WHITE);
        weatherTemperature.setIncludeFontPadding(false);
        weatherTemperature.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        LinearLayout.LayoutParams temperatureParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        temperatureParams.setMargins(dp(3), 0, 0, 0);
        weatherRow.addView(weatherTemperature, temperatureParams);

        weatherLocation = new TextView(this);
        weatherLocation.setTextSize(14);
        weatherLocation.setTextColor(Color.WHITE);
        weatherLocation.setIncludeFontPadding(false);
        weatherLocation.setSingleLine(true);
        weatherLocation.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        LinearLayout.LayoutParams weatherLocationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        weatherLocationParams.setMargins(dp(8), 0, 0, 0);
        weatherRow.addView(weatherLocation, weatherLocationParams);
        LinearLayout.LayoutParams weatherRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        weatherRowParams.gravity = Gravity.CENTER_HORIZONTAL;
        weatherRowParams.topMargin = dp(3);
        weatherContentPanel.addView(weatherRow, weatherRowParams);

        weatherExtendedPanel = new LinearLayout(this);
        weatherExtendedPanel.setOrientation(LinearLayout.VERTICAL);
        weatherExtendedPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        weatherExtendedPanel.setPadding(0, 0, 0, 0);
        weatherExtendedPanel.setVisibility(View.GONE);

        weatherForecastText = new TextView(this);
        weatherForecastText.setTextSize(12);
        weatherForecastText.setTextColor(Color.WHITE);
        weatherForecastText.setGravity(Gravity.CENTER);
        weatherForecastText.setIncludeFontPadding(false);
        weatherForecastText.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        weatherForecastText.setMaxLines(2);
        weatherExtendedPanel.addView(weatherForecastText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        daylightProgressView = new DaylightProgressView(this);
        LinearLayout.LayoutParams daylightProgressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(14));
        weatherExtendedPanel.addView(daylightProgressView, daylightProgressParams);

        daylightLabel = new TextView(this);
        daylightLabel.setTextSize(12);
        daylightLabel.setTextColor(Color.WHITE);
        daylightLabel.setGravity(Gravity.RIGHT);
        daylightLabel.setIncludeFontPadding(false);
        daylightLabel.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        weatherExtendedPanel.addView(daylightLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(18)));

        LinearLayout.LayoutParams extendedWeatherParams = new LinearLayout.LayoutParams(
                dp(220), LinearLayout.LayoutParams.WRAP_CONTENT);
        extendedWeatherParams.gravity = Gravity.CENTER_HORIZONTAL;
        extendedWeatherParams.topMargin = dp(4);
        weatherContentPanel.addView(weatherExtendedPanel, extendedWeatherParams);
        clockPanel.addView(weatherBlock, clockBlockLayoutParams());

        pomodoroRow = new LinearLayout(this);
        pomodoroRow.setOrientation(LinearLayout.VERTICAL);
        pomodoroRow.setGravity(Gravity.CENTER);
        pomodoroRow.setVisibility(View.GONE);
        pomodoroAdvancedInfo = new TextView(this);
        pomodoroAdvancedInfo.setTextSize(15);
        pomodoroAdvancedInfo.setTextColor(SECONDARY);
        pomodoroAdvancedInfo.setGravity(Gravity.CENTER_HORIZONTAL);
        pomodoroAdvancedInfo.setIncludeFontPadding(false);
        pomodoroAdvancedInfo.setVisibility(View.GONE);
        pomodoroAdvancedInfo.setTypeface(FontManager.getPomodoroChineseFont(this));
        pomodoroRow.addView(pomodoroAdvancedInfo, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        pomodoroLabel = new TextView(this);
        pomodoroLabel.setTextSize(24);
        pomodoroLabel.setTextColor(Color.WHITE);
        pomodoroLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        pomodoroLabel.setIncludeFontPadding(false);
        pomodoroLabel.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        LinearLayout.LayoutParams pomodoroLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pomodoroLabelParams.bottomMargin = -dp(4);
        pomodoroRow.addView(pomodoroLabel, pomodoroLabelParams);
        pomodoroText = new TextView(this);
        pomodoroText.setTextSize(112);
        pomodoroText.setTextColor(POMODORO_RED);
        pomodoroText.setGravity(Gravity.CENTER_HORIZONTAL);
        pomodoroText.setIncludeFontPadding(false);
        pomodoroText.setShadowLayer(dp(3), dp(1), dp(1), Color.BLACK);
        pomodoroRow.addView(pomodoroText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        pomodoroProgressView = new PomodoroProgressView(this);
        pomodoroProgressView.setVisibility(View.GONE);
        LinearLayout.LayoutParams pomodoroProgressParams = new LinearLayout.LayoutParams(
                dp(240), dp(10));
        pomodoroProgressParams.setMargins(dp(4), dp(8), dp(4), 0);
        pomodoroRow.addView(pomodoroProgressView, pomodoroProgressParams);

        rootContainer.addView(clockPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // 電量固定在主畫面右上角，不隨日期／時鐘區塊拖曳或版面切換移動。
        FrameLayout.LayoutParams batteryStatusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        batteryStatusParams.setMargins(0, dp(16), dp(16), 0);
        rootContainer.addView(batteryStatusView, batteryStatusParams);

        FrameLayout.LayoutParams legacyClockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        legacyClockParams.setMargins(dp(28), dp(28), dp(16), dp(16));
        rootContainer.addView(legacyClockPanel, legacyClockParams);

        pomodoroFocusPanel = new FrameLayout(this);
        pomodoroFocusPanel.setVisibility(View.GONE);
        pomodoroFocusPanel.setClickable(true);
        pomodoroFocusPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumePomodoroFromFocusScreen();
            }
        });
        pomodoroInfoPanel = new LinearLayout(this);
        pomodoroInfoPanel.setOrientation(LinearLayout.VERTICAL);
        pomodoroInfoPanel.setGravity(Gravity.END);
        pomodoroInfoPanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams pomodoroInfoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        pomodoroInfoParams.setMargins(dp(20), dp(20), dp(20), dp(20));
        pomodoroFocusPanel.addView(pomodoroInfoPanel, pomodoroInfoParams);
        rootContainer.addView(pomodoroFocusPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setupClockDragAndDrop();

        settingsButton = new Button(this);
        settingsButton.setText("設定");
        settingsButton.setTextColor(Color.WHITE);
        settingsButton.setTextSize(17);
        settingsButton.setAllCaps(false);
        settingsButton.setAlpha(0.0f);
        settingsButton.setVisibility(View.GONE);

        StateListDrawable folderBg = new StateListDrawable();
        folderBg.addState(
                new int[] { android.R.attr.state_pressed },
                rounded(Color.argb(110, 70, 90, 100)));
        folderBg.addState(
                new int[] {},
                rounded(Color.argb(68, 35, 52, 62)));
        quickActionBackground = folderBg;
        settingsButton.setBackground(folderBg);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoHandler.removeCallbacks(hideImmersiveRunnable);
                startActivity(new Intent(PhotoClockActivity.this, SettingsActivity.class));
            }
        });

        FrameLayout.LayoutParams folderParams = new FrameLayout.LayoutParams(
                dp(130), dp(48), Gravity.TOP | Gravity.START);
        folderParams.setMargins(dp(16), dp(16), 0, 0);
        rootContainer.addView(settingsButton, folderParams);

        pomodoroButton = new Button(this);
        pomodoroButton.setText("番茄鐘");
        pomodoroButton.setTextColor(Color.WHITE);
        pomodoroButton.setTextSize(17);
        pomodoroButton.setAllCaps(false);
        pomodoroButton.setAlpha(0.0f);
        pomodoroButton.setVisibility(View.GONE);
        pomodoroButton.setBackground(folderBg);
        pomodoroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoHandler.removeCallbacks(hideImmersiveRunnable);
                showPomodoroDialog();
            }
        });
        FrameLayout.LayoutParams pomodoroButtonParams = new FrameLayout.LayoutParams(
                dp(130), dp(48), Gravity.TOP | Gravity.START);
        pomodoroButtonParams.setMargins(dp(154), dp(16), 0, 0);
        rootContainer.addView(pomodoroButton, pomodoroButtonParams);

        focusReminderOverlay = new FrameLayout(this);
        focusReminderOverlay.setBackgroundColor(Color.BLACK);
        focusReminderOverlay.setClickable(true);
        focusReminderOverlay.setVisibility(View.GONE);
        focusReminderImage = new ImageView(this);
        focusReminderImage.setImageResource(focusReminderDrawable(true));
        focusReminderImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        focusReminderImage.setContentDescription("專注提醒圖片");
        focusReminderOverlay.addView(focusReminderImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        focusReminderMessage = new TextView(this);
        focusReminderMessage.setText("專心一點！");
        focusReminderMessage.setTextSize(20);
        focusReminderMessage.setTextColor(Color.WHITE);
        focusReminderMessage.setTypeface(FontManager.getPomodoroChineseFont(this));
        focusReminderMessage.setGravity(Gravity.CENTER);
        focusReminderMessage.setPadding(dp(16), dp(10), dp(16), dp(10));
        focusReminderMessage.setBackground(rounded(Color.argb(180, 0, 0, 0)));
        FrameLayout.LayoutParams reminderMessageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        reminderMessageParams.setMargins(dp(20), 0, dp(20), dp(28));
        focusReminderOverlay.addView(focusReminderMessage, reminderMessageParams);
        rootContainer.addView(focusReminderOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        updatePhotoClock();
        return rootContainer;
    }

    private void showPomodoroDialog() {
        if (isFinishing() || isDestroyed()) return;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));

        pomodoroDialogStatus = text("", 22, PRIMARY);
        pomodoroDialogStatus.setGravity(Gravity.CENTER);
        pomodoroDialogStatus.setTypeface(FontManager.getPomodoroChineseFont(this));
        content.addView(pomodoroDialogStatus, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));

        TextView phaseHint = text("切換階段會重設該階段的時間", 13, SECONDARY);
        phaseHint.setGravity(Gravity.CENTER);
        content.addView(phaseHint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));

        LinearLayout phases = new LinearLayout(this);
        phases.setGravity(Gravity.CENTER);
        addPomodoroPhaseButton(phases, "專注", PomodoroHelper.PHASE_FOCUS);
        addPomodoroPhaseButton(phases, "短休息", PomodoroHelper.PHASE_SHORT_BREAK);
        addPomodoroPhaseButton(phases, "長休息", PomodoroHelper.PHASE_LONG_BREAK);
        content.addView(phases, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        Button customDuration = button("自訂時間", PANEL_RAISED);
        customDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomPomodoroDurationDialog();
            }
        });
        LinearLayout.LayoutParams customDurationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
        customDurationParams.setMargins(0, dp(3), 0, 0);
        content.addView(customDuration, customDurationParams);

        pomodoroStartPauseButton = button("開始", ACTIVE_COLOR);
        pomodoroStartPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(PhotoClockActivity.this);
                if (snapshot.running) {
                    PomodoroHelper.pause(PhotoClockActivity.this);
                } else {
                    requestNotificationPermissionIfNeeded();
                    boolean exact = PomodoroHelper.startOrResume(PhotoClockActivity.this);
                    if (!exact) {
                        Toast.makeText(PhotoClockActivity.this,
                                "未允許精準鬧鐘時，背景提醒可能延遲", Toast.LENGTH_LONG).show();
                    }
                    if (pomodoroDialog != null) pomodoroDialog.dismiss();
                }
                updatePomodoroDisplay();
                refreshPomodoroDialog();
            }
        });
        Button skip = button("下一階段", PANEL_RAISED);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PomodoroHelper.skip(PhotoClockActivity.this);
                updatePomodoroDisplay();
                refreshPomodoroDialog();
            }
        });
        Button end = button("結束", Color.rgb(104, 50, 56));
        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PomodoroHelper.reset(PhotoClockActivity.this);
                updatePomodoroDisplay();
                refreshPomodoroDialog();
            }
        });
        content.addView(pomodoroStartPauseButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));
        LinearLayout secondaryControls = new LinearLayout(this);
        secondaryControls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(0, dp(36), 2);
        skipParams.setMargins(0, dp(4), dp(4), 0);
        secondaryControls.addView(skip, skipParams);
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(0, dp(36), 1);
        endParams.setMargins(dp(4), dp(4), 0, 0);
        secondaryControls.addView(end, endParams);
        content.addView(secondaryControls, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));

        Button leaveSettings = button("離開設定", ACTIVE_COLOR);
        leaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pomodoroDialog != null) pomodoroDialog.dismiss();
            }
        });
        LinearLayout.LayoutParams leaveSettingsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(32));
        leaveSettingsParams.setMargins(0, dp(2), 0, 0);
        content.addView(leaveSettings, leaveSettingsParams);

        ScrollView dialogScroll = new ScrollView(this);
        dialogScroll.setFillViewport(true);
        dialogScroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        pomodoroDialog = new AlertDialog.Builder(this)
                .setTitle("番茄鐘")
                .setView(dialogScroll)
                .create();
        pomodoroDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                photoHandler.removeCallbacks(pomodoroDialogTicker);
                pomodoroDialog = null;
                pomodoroDialogStatus = null;
                pomodoroStartPauseButton = null;
            }
        });
        pomodoroDialog.show();
        styleModernDialog(pomodoroDialog);
        refreshPomodoroDialog();
        photoHandler.removeCallbacks(pomodoroDialogTicker);
        photoHandler.postDelayed(pomodoroDialogTicker, 1000L);
    }

    private void showCustomPomodoroDurationDialog() {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setHint("分鐘（1–180）");
        input.setTextColor(PRIMARY);
        input.setHintTextColor(SECONDARY);
        input.setTypeface(FontManager.getPomodoroChineseFont(this));
        input.setBackground(rounded(PANEL_RAISED));
        input.setPadding(dp(14), dp(8), dp(14), dp(8));
        input.setText(String.valueOf(prefs.getInt(PomodoroHelper.PREF_FOCUS_MINUTES,
                PomodoroHelper.DEFAULT_FOCUS_MINUTES)));
        input.setSelectAllOnFocus(true);
        int padding = dp(20);
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(padding, 0, padding, 0);
        holder.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog customDialog = new AlertDialog.Builder(this)
                .setTitle("自訂番茄鐘時間")
                .setView(holder)
                .setNegativeButton("取消", null)
                .setPositiveButton("開始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int minutes;
                        try {
                            minutes = Integer.parseInt(input.getText().toString().trim());
                        } catch (NumberFormatException ignored) {
                            Toast.makeText(PhotoClockActivity.this, "請輸入 1 到 180 分鐘",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (minutes < 1 || minutes > 180) {
                            Toast.makeText(PhotoClockActivity.this, "請輸入 1 到 180 分鐘",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        requestNotificationPermissionIfNeeded();
                        boolean exact = PomodoroHelper.startCustom(PhotoClockActivity.this, minutes);
                        if (!exact) {
                            Toast.makeText(PhotoClockActivity.this,
                                    "未允許精準鬧鐘時，背景提醒可能延遲", Toast.LENGTH_LONG).show();
                        }
                        updatePomodoroDisplay();
                        refreshPomodoroDialog();
                        if (pomodoroDialog != null) pomodoroDialog.dismiss();
                    }
                })
                .create();
        customDialog.show();
        styleModernDialog(customDialog);
    }

    private static final int ACTIVE_COLOR = Color.rgb(37, 124, 137);

    private void addPomodoroPhaseButton(LinearLayout parent, String label, final String phase) {
        Button button = button(label, PANEL_RAISED);
        button.setTextSize(13);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PomodoroHelper.prepare(PhotoClockActivity.this, phase);
                updatePomodoroDisplay();
                refreshPomodoroDialog();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(30), 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(button, params);
    }

    private void refreshPomodoroDialog() {
        if (pomodoroDialogStatus == null || pomodoroStartPauseButton == null) return;
        PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(this);
        String status = PomodoroHelper.phaseLabel(snapshot.phase) + "  "
                + PomodoroHelper.formatRemaining(snapshot.remainingMs);
        if (!snapshot.running) status += snapshot.hasSession ? "（已暫停）" : "（尚未開始）";
        pomodoroDialogStatus.setText(status);
        pomodoroStartPauseButton.setText(snapshot.running ? "暫停" : "開始");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS },
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void loadClockScalePreferences(float defaultScale) {
        clockScaleFactor = clampClockScale(prefs.getFloat(
                orientationKey(CLOCK_SCALE_FACTOR), defaultScale));
        boolean hasLegacySizeMode = prefs.contains(CLOCK_SIZE_MODE);
        int legacySizeMode = ClockSizeModePolicy.normalize(
                prefs.getInt(CLOCK_SIZE_MODE, CLOCK_SIZE_MODE_OVERLAP));
        if (prefs.contains(CLOCK_LAYOUT_MODE)) {
            clockLayoutMode = ClockLayoutPolicy.normalizeLayout(
                    prefs.getInt(CLOCK_LAYOUT_MODE, CLOCK_LAYOUT_MODE_ORIGINAL));
            originalScaleMode = ClockLayoutPolicy.normalizeOriginalScale(
                    prefs.getInt(CLOCK_ORIGINAL_SCALE_MODE,
                            CLOCK_ORIGINAL_SCALE_LINKED));
        } else if (hasLegacySizeMode) {
            clockLayoutMode = ClockLayoutPolicy.layoutFromLegacySizeMode(legacySizeMode);
            originalScaleMode = ClockLayoutPolicy.originalScaleFromLegacySizeMode(legacySizeMode);
        } else {
            boolean legacyLinked = prefs.getBoolean(CLOCK_SIZES_LINKED,
                    prefs.getBoolean(LEGACY_BIND_SCALE, true));
            clockLayoutMode = CLOCK_LAYOUT_MODE_ORIGINAL;
            originalScaleMode = legacyLinked
                    ? CLOCK_ORIGINAL_SCALE_LINKED : CLOCK_ORIGINAL_SCALE_AVOID;
        }
        clockSizesLinked = isLinkedOriginalLayout();
        float loadedTimeScale = loadComponentScale(
                CLOCK_TIME_SCALE_FACTOR, LEGACY_TIME_SCALE_FACTOR, clockScaleFactor);
        float loadedDateScale = loadComponentScale(
                CLOCK_DATE_SCALE_FACTOR, LEGACY_DATE_SCALE_FACTOR, clockScaleFactor);
        float loadedWeatherScale = loadComponentScale(
                CLOCK_WEATHER_SCALE_FACTOR, LEGACY_WEATHER_SCALE_FACTOR, clockScaleFactor);
        if (clockSizesLinked) {
            // Averaging keeps switching from independent mode predictable while preserving
            // older linked preferences, whose three component values are identical.
            clockScaleFactor = clampClockScale(
                    (loadedTimeScale + loadedDateScale + loadedWeatherScale) / 3.0f);
            timeScaleFactor = clockScaleFactor;
            dateScaleFactor = clockScaleFactor;
            weatherScaleFactor = clockScaleFactor;
        } else {
            timeScaleFactor = loadedTimeScale;
            dateScaleFactor = loadedDateScale;
            weatherScaleFactor = loadedWeatherScale;
        }
    }

    private float loadComponentScale(String key, String legacyKey, float defaultScale) {
        String orientedKey = orientationKey(key);
        if (prefs.contains(orientedKey)) {
            return clampClockScale(prefs.getFloat(orientedKey, defaultScale));
        }
        return clampClockScale(prefs.getFloat(orientationKey(legacyKey), defaultScale));
    }

    private boolean isOriginalLayout() {
        return ClockLayoutPolicy.isOriginal(clockLayoutMode);
    }

    private boolean isLinkedOriginalLayout() {
        return ClockLayoutPolicy.isLinkedOriginal(clockLayoutMode, originalScaleMode);
    }

    private int legacySizeMode() {
        return ClockLayoutPolicy.legacySizeMode(clockLayoutMode, originalScaleMode);
    }

    private void applyClockScale() {
        if (clockPanel == null) return;
        if (pomodoroModeLayoutActive) {
            clockPanel.setScaleX(1.0f);
            clockPanel.setScaleY(1.0f);
            if (legacyClockPanel != null) {
                legacyClockPanel.setScaleX(1.0f);
                legacyClockPanel.setScaleY(1.0f);
            }
            return;
        }

        if (legacyClockPanelActive) {
            if (isLinkedOriginalLayout()) {
                // The linked mode deliberately scales the original wrap-content panel, not the
                // independent blocks' full-screen coordinate system.
                applyClockComponentSizes(1.0f, 1.0f, 1.0f);
                applyLegacyClockScale();
            } else {
                // The .13-style vertical layout naturally separates rows when one component
                // grows. This keeps the avoidance effect playful without moving free blocks.
                legacyClockPanel.setScaleX(1.0f);
                legacyClockPanel.setScaleY(1.0f);
                applyClockComponentSizes(timeScaleFactor, dateScaleFactor, weatherScaleFactor);
                applyLegacyClockTranslation();
                legacyClockPanel.requestLayout();
            }
            return;
        }

        clockPanel.setScaleX(1.0f);
        clockPanel.setScaleY(1.0f);
        applyClockComponentSizes(timeScaleFactor, dateScaleFactor, weatherScaleFactor);
    }

    /** Relayouts independently sized text instead of scaling it inside stale view bounds. */
    private void applyClockComponentSizes(float timeScale, float dateScale, float weatherScale) {
        if (photoTime != null) setClockTextSize(photoTime, 64.0f * timeScale);
        if (photoDate != null) setClockTextSize(photoDate, 24.0f * dateScale);
        if (compactWeatherTemperature != null) {
            setClockTextSize(compactWeatherTemperature, 18.0f * weatherScale);
        }
        if (weatherTemperature != null) {
            setClockTextSize(weatherTemperature, 20.0f * weatherScale);
        }
        if (weatherLocation != null) {
            setClockTextSize(weatherLocation, 14.0f * weatherScale);
        }
        if (weatherForecastText != null) {
            setClockTextSize(weatherForecastText, 12.0f * weatherScale);
        }
        if (daylightLabel != null) {
            setClockTextSize(daylightLabel, 12.0f * weatherScale);
        }
        int daylightWidth = Math.max(1, Math.round(220.0f * weatherScale));
        if (weatherExtendedPanel != null) {
            resizeView(weatherExtendedPanel, daylightWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (daylightProgressView != null) {
            resizeView(daylightProgressView, ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.max(1, Math.round(14.0f * weatherScale)));
        }
        if (daylightLabel != null) {
            resizeView(daylightLabel, ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.max(1, Math.round(18.0f * weatherScale)));
        }
        if (compactWeatherIcon != null) {
            int size = Math.max(1, Math.round(22.0f * weatherScale));
            resizeView(compactWeatherIcon, size, size);
        }
        if (weatherIcon != null) {
            int size = Math.max(1, Math.round(30.0f * weatherScale));
            resizeView(weatherIcon, size, size);
        }
    }

    private void setClockTextSize(TextView view, float sizeSp) {
        float expectedPx = sizeSp * getResources().getDisplayMetrics().scaledDensity;
        if (Math.abs(view.getTextSize() - expectedPx) > 0.1f) {
            view.setTextSize(sizeSp);
        }
    }

    private void saveClockScaleFactor() {
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit()
                    .putFloat(orientationKey(CLOCK_SCALE_FACTOR), clockScaleFactor)
                    .putInt(CLOCK_LAYOUT_MODE, clockLayoutMode)
                    .putInt(CLOCK_ORIGINAL_SCALE_MODE, originalScaleMode)
                    .putInt(CLOCK_SIZE_MODE, legacySizeMode())
                    .putBoolean(CLOCK_SIZES_LINKED, isLinkedOriginalLayout())
                    .putFloat(orientationKey(CLOCK_TIME_SCALE_FACTOR), timeScaleFactor)
                    .putFloat(orientationKey(CLOCK_DATE_SCALE_FACTOR), dateScaleFactor)
                    .putFloat(orientationKey(CLOCK_WEATHER_SCALE_FACTOR), weatherScaleFactor);
            editor.apply();
        }
    }

    private float clampClockScale(float scale) {
        return Math.max(0.4f, Math.min(6.0f, scale));
    }

    private boolean isPointInView(float x, float y, View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        Rect bounds = new Rect();
        if (!view.getGlobalVisibleRect(bounds)) return false;
        return bounds.contains(Math.round(x), Math.round(y));
    }

    private int findScaleTarget(float x, float y) {
        if (isPointInView(x, y, timeBlock)) return SCALE_TARGET_TIME;
        if (isPointInView(x, y, compactWeatherRow)
                || isPointInView(x, y, weatherRow)
                || isPointInView(x, y, weatherBlock)) {
            return SCALE_TARGET_WEATHER;
        }
        if (isPointInView(x, y, dateBlock)) return SCALE_TARGET_DATE;
        return SCALE_TARGET_NONE;
    }

    private void setupClockDragAndDrop() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (isLinkedOriginalLayout()) {
                    clockScaleFactor = clampClockScale(clockScaleFactor * factor);
                    timeScaleFactor = clockScaleFactor;
                    dateScaleFactor = clockScaleFactor;
                    weatherScaleFactor = clockScaleFactor;
                } else if (activeScaleTarget == SCALE_TARGET_TIME) {
                    timeScaleFactor = clampClockScale(timeScaleFactor * factor);
                } else if (activeScaleTarget == SCALE_TARGET_DATE) {
                    dateScaleFactor = clampClockScale(dateScaleFactor * factor);
                } else if (activeScaleTarget == SCALE_TARGET_WEATHER) {
                    weatherScaleFactor = clampClockScale(weatherScaleFactor * factor);
                }
                applyClockScale();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                if (pomodoroModeLayoutActive) return false;
                if (!isLinkedOriginalLayout()) {
                    activeScaleTarget = findScaleTarget(
                            detector.getFocusX(), detector.getFocusY());
                    if (activeScaleTarget == SCALE_TARGET_NONE) return false;
                }
                isScalingClock = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScalingClock = false;
                saveClockScaleFactor();
                activeScaleTarget = SCALE_TARGET_NONE;
            }
        });

        setupClockBlock(timeBlock, SCALE_TARGET_TIME);
        setupClockBlock(dateBlock, SCALE_TARGET_DATE);
        setupClockBlock(weatherBlock, SCALE_TARGET_WEATHER);
    }

    private void setupClockBlock(final AccessibleFrameLayout block, final int target) {
        if (block == null) return;
        block.setClickable(true);
        block.setLongClickable(true);
        block.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!isScalingClock && !pomodoroModeLayoutActive) {
                    isDraggingClock = true;
                    activeClockBlock = view;
                    if (legacyClockPanelActive) {
                        startLegacyClockDrag();
                    } else {
                        clockStartBlockX = getBlockBaseX(target);
                        clockStartBlockY = getBlockBaseY(target);
                        view.setAlpha(0.75f);
                    }
                    return true;
                }
                return false;
            }
        });

        block.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rootContainer.performClick();
            }
        });

        block.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                resetImmersiveTimeout();

                if (pomodoroModeLayoutActive) {
                    return true;
                }

                if (event.getPointerCount() > 1 || isScalingClock) {
                    if (isDraggingClock && activeClockBlock == view) {
                        finishClockDrag(view, target, true);
                    }
                    return true;
                }

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownRawX = event.getRawX();
                        touchDownRawY = event.getRawY();
                        activeClockBlock = view;
                        if (legacyClockPanelActive) {
                            legacyClockStartTranslationX = legacyClockTranslationX;
                            legacyClockStartTranslationY = legacyClockTranslationY;
                        } else {
                            clockStartBlockX = getBlockBaseX(target);
                            clockStartBlockY = getBlockBaseY(target);
                        }
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        if (isDraggingClock && activeClockBlock == view) {
                            float deltaX = event.getRawX() - touchDownRawX;
                            float deltaY = event.getRawY() - touchDownRawY;
                            if (legacyClockPanelActive) {
                                clampAndApplyLegacyTranslation(
                                        legacyClockStartTranslationX + deltaX,
                                        legacyClockStartTranslationY + deltaY);
                            } else {
                                float interactionScale = getClockInteractionScale();
                                clampAndApplyBlockTranslation(target,
                                        clockStartBlockX + deltaX / interactionScale,
                                        clockStartBlockY + deltaY / interactionScale);
                            }
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_UP:
                        if (!isDraggingClock) {
                            view.performClick();
                            return true;
                        }
                        finishClockDrag(view, target, true);
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        if (isDraggingClock) {
                            finishClockDrag(view, target, true);
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });
    }

    private void finishClockDrag(View view, int target, boolean save) {
        if (legacyClockPanelActive) {
            if (legacyClockPanel != null) legacyClockPanel.setAlpha(1.0f);
        } else if (view != null) {
            view.setAlpha(1.0f);
        }
        if (save) {
            if (legacyClockPanelActive) {
                saveLegacyClockPosition();
            } else {
                saveClockPosition(target);
            }
        }
        isDraggingClock = false;
        activeClockBlock = null;
        lastClockDragEndTime = SystemClock.elapsedRealtime();
    }

    private void startLegacyClockDrag() {
        legacyClockStartTranslationX = legacyClockTranslationX;
        legacyClockStartTranslationY = legacyClockTranslationY;
        if (legacyClockPanel != null) legacyClockPanel.setAlpha(0.75f);
    }

    private void applyLegacyClockScale() {
        if (legacyClockPanel == null) return;
        legacyClockPanel.setPivotX(legacyClockPanel.getWidth() / 2.0f);
        legacyClockPanel.setPivotY(legacyClockPanel.getHeight() / 2.0f);
        legacyClockPanel.setScaleX(clockScaleFactor);
        legacyClockPanel.setScaleY(clockScaleFactor);
        legacyClockPanel.setTranslationX(legacyClockTranslationX + burnInOffsetX);
        legacyClockPanel.setTranslationY(legacyClockTranslationY + burnInOffsetY);
    }

    private float getClockInteractionScale() {
        return 1.0f;
    }

    private FrameLayout.LayoutParams clockBlockLayoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
    }

    private AccessibleFrameLayout blockForTarget(int target) {
        if (target == SCALE_TARGET_TIME) return timeBlock;
        if (target == SCALE_TARGET_DATE) return dateBlock;
        if (target == SCALE_TARGET_WEATHER) return weatherBlock;
        return null;
    }

    private float getBlockBaseX(int target) {
        if (target == SCALE_TARGET_TIME) return timeBlockBaseX;
        if (target == SCALE_TARGET_DATE) return dateBlockBaseX;
        if (target == SCALE_TARGET_WEATHER) return weatherBlockBaseX;
        return 0.0f;
    }

    private float getBlockBaseY(int target) {
        if (target == SCALE_TARGET_TIME) return timeBlockBaseY;
        if (target == SCALE_TARGET_DATE) return dateBlockBaseY;
        if (target == SCALE_TARGET_WEATHER) return weatherBlockBaseY;
        return 0.0f;
    }

    private void setBlockBasePosition(int target, float x, float y) {
        if (target == SCALE_TARGET_TIME) {
            timeBlockBaseX = x;
            timeBlockBaseY = y;
        } else if (target == SCALE_TARGET_DATE) {
            dateBlockBaseX = x;
            dateBlockBaseY = y;
        } else if (target == SCALE_TARGET_WEATHER) {
            weatherBlockBaseX = x;
            weatherBlockBaseY = y;
        }
    }

    private void clampAndApplyBlockTranslation(int target, float targetLeft, float targetTop) {
        AccessibleFrameLayout block = blockForTarget(target);
        if (rootContainer == null || block == null) return;
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        int blockW = block.getWidth();
        int blockH = block.getHeight();
        if (rootW <= 0 || rootH <= 0) {
            setBlockBasePosition(target, targetLeft, targetTop);
            applyBlockTranslation(target);
            return;
        }

        if (blockW <= 0 || blockH <= 0) {
            setBlockBasePosition(target, targetLeft, targetTop);
            applyClockTranslation();
            return;
        }

        float centerX = ClockPositionPolicy.clampCenter(
                targetLeft + blockW / 2.0f, blockW, rootW, dp(12));
        float centerY = ClockPositionPolicy.clampCenter(
                targetTop + blockH / 2.0f, blockH, rootH, dp(12));
        setBlockBasePosition(target, centerX - blockW / 2.0f, centerY - blockH / 2.0f);
        applyBlockTranslation(target);
    }

    private void saveClockPosition(int target) {
        AccessibleFrameLayout block = blockForTarget(target);
        if (rootContainer == null || block == null || prefs == null) return;
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        if (rootW <= 0 || rootH <= 0) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        writeClockPosition(editor, target, rootW, rootH);
        editor.apply();
    }

    private void writeClockPosition(SharedPreferences.Editor editor, int target,
                                    int rootW, int rootH) {
        AccessibleFrameLayout block = blockForTarget(target);
        if (editor == null || block == null) return;
        editor.putFloat(orientationPositionKey(target, true),
                    ClockPositionPolicy.ratioFromCenter(
                            getBlockBaseX(target) + block.getWidth() / 2.0f, rootW))
                .putFloat(orientationPositionKey(target, false),
                        ClockPositionPolicy.ratioFromCenter(
                                getBlockBaseY(target) + block.getHeight() / 2.0f, rootH))
                .putInt(CLOCK_LAYOUT_VERSION_KEY, CLOCK_LAYOUT_VERSION);
    }

    private void restoreClockPosition() {
        if (rootContainer == null || clockPanel == null || prefs == null) {
            return;
        }
        if (legacyClockPanelActive) {
            restoreLegacyClockPosition();
            return;
        }
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        if (rootW <= 0 || rootH <= 0) {
            return;
        }

        if (!hasNewClockPositions()) {
            migrateLegacyClockPositions();
        }
        restoreBlockPosition(SCALE_TARGET_TIME, rootW, rootH);
        restoreBlockPosition(SCALE_TARGET_DATE, rootW, rootH);
        restoreBlockPosition(SCALE_TARGET_WEATHER, rootW, rootH);
        applyClockScale();
    }

    private float getLegacyClockDefaultLeft() {
        return rootContainer.getWidth() - legacyClockPanel.getWidth() - dp(16);
    }

    private float getLegacyClockDefaultTop() {
        return rootContainer.getHeight() - legacyClockPanel.getHeight() - dp(16);
    }

    private void clampAndApplyLegacyTranslation(float targetTransX, float targetTransY) {
        if (rootContainer == null || legacyClockPanel == null) return;
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        int panelW = legacyClockPanel.getWidth();
        int panelH = legacyClockPanel.getHeight();
        if (rootW <= 0 || rootH <= 0 || panelW <= 0 || panelH <= 0) {
            legacyClockTranslationX = targetTransX;
            legacyClockTranslationY = targetTransY;
            applyLegacyClockTranslation();
            return;
        }

        float defaultLeft = getLegacyClockDefaultLeft();
        float defaultTop = getLegacyClockDefaultTop();
        float minTransX = -defaultLeft + dp(12);
        float maxTransX = dp(16);
        float minTransY = -defaultTop + dp(12);
        float maxTransY = dp(16);
        legacyClockTranslationX = Math.max(minTransX, Math.min(maxTransX, targetTransX));
        legacyClockTranslationY = Math.max(minTransY, Math.min(maxTransY, targetTransY));
        applyLegacyClockTranslation();
    }

    private void saveLegacyClockPosition() {
        if (rootContainer == null || legacyClockPanel == null || prefs == null) return;
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        int panelW = legacyClockPanel.getWidth();
        int panelH = legacyClockPanel.getHeight();
        if (rootW <= panelW || rootH <= panelH) return;

        float currentLeft = getLegacyClockDefaultLeft() + legacyClockTranslationX;
        float currentTop = getLegacyClockDefaultTop() + legacyClockTranslationY;
        prefs.edit()
                .putFloat(orientationKey(CLOCK_POS_X_RATIO),
                        currentLeft / (float) (rootW - panelW))
                .putFloat(orientationKey(CLOCK_POS_Y_RATIO),
                        currentTop / (float) (rootH - panelH))
                .apply();
    }

    private void restoreLegacyClockPosition() {
        if (rootContainer == null || legacyClockPanel == null || prefs == null) return;
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        int panelW = legacyClockPanel.getWidth();
        int panelH = legacyClockPanel.getHeight();
        if (rootW <= panelW || rootH <= panelH) return;

        float ratioX = prefs.getFloat(orientationKey(CLOCK_POS_X_RATIO),
                DEFAULT_GROUP_PANEL_X_RATIO);
        float ratioY = prefs.getFloat(orientationKey(CLOCK_POS_Y_RATIO),
                DEFAULT_GROUP_PANEL_Y_RATIO);
        float defaultLeft = getLegacyClockDefaultLeft();
        float defaultTop = getLegacyClockDefaultTop();
        clampAndApplyLegacyTranslation(
                ratioX * (rootW - panelW) - defaultLeft,
                ratioY * (rootH - panelH) - defaultTop);
        applyLegacyClockScale();
    }

    private void applyClockTranslation() {
        if (legacyClockPanelActive) {
            applyLegacyClockTranslation();
            return;
        }
        applyBlockTranslation(SCALE_TARGET_TIME);
        applyBlockTranslation(SCALE_TARGET_DATE);
        applyBlockTranslation(SCALE_TARGET_WEATHER);
    }

    private void applyLegacyClockTranslation() {
        if (legacyClockPanel == null) return;
        float offsetX = pomodoroModeLayoutActive ? 0.0f : burnInOffsetX;
        float offsetY = pomodoroModeLayoutActive ? 0.0f : burnInOffsetY;
        legacyClockPanel.setTranslationX(legacyClockTranslationX + offsetX);
        legacyClockPanel.setTranslationY(legacyClockTranslationY + offsetY);
    }

    private void applyBlockTranslation(int target) {
        AccessibleFrameLayout block = blockForTarget(target);
        if (block == null) return;
        float offsetX = pomodoroModeLayoutActive ? 0.0f : burnInOffsetX;
        float offsetY = pomodoroModeLayoutActive ? 0.0f : burnInOffsetY;
        block.setTranslationX(getBlockBaseX(target) + offsetX);
        block.setTranslationY(getBlockBaseY(target) + offsetY);
    }

    private void restoreBlockPosition(int target, int rootW, int rootH) {
        AccessibleFrameLayout block = blockForTarget(target);
        if (block == null) return;
        float ratioX = prefs.getFloat(orientationPositionKey(target, true),
                defaultPositionRatio(target, true));
        float ratioY = prefs.getFloat(orientationPositionKey(target, false),
                defaultPositionRatio(target, false));
        float left = ClockPositionPolicy.centerFromRatio(ratioX, rootW)
                - block.getWidth() / 2.0f;
        float top = ClockPositionPolicy.centerFromRatio(ratioY, rootH)
                - block.getHeight() / 2.0f;
        clampAndApplyBlockTranslation(target, left, top);
    }

    private boolean hasNewClockPositions() {
        if (prefs.getInt(CLOCK_LAYOUT_VERSION_KEY, 0) >= CLOCK_LAYOUT_VERSION) {
            return true;
        }
        return prefs.contains(orientationPositionKey(SCALE_TARGET_TIME, true))
                || prefs.contains(orientationPositionKey(SCALE_TARGET_DATE, true))
                || prefs.contains(orientationPositionKey(SCALE_TARGET_WEATHER, true));
    }

    private void migrateLegacyClockPositions() {
        float legacyX = ClockPositionPolicy.validRatio(
                prefs.getFloat(orientationKey(CLOCK_POS_X_RATIO), DEFAULT_DATE_X_RATIO),
                DEFAULT_DATE_X_RATIO);
        float legacyY = ClockPositionPolicy.validRatio(
                prefs.getFloat(orientationKey(CLOCK_POS_Y_RATIO), DEFAULT_DATE_Y_RATIO),
                DEFAULT_DATE_Y_RATIO);
        prefs.edit()
                .putFloat(orientationPositionKey(SCALE_TARGET_TIME, true), legacyX)
                .putFloat(orientationPositionKey(SCALE_TARGET_TIME, false),
                        ClockPositionPolicy.validRatio(legacyY - 0.12f, DEFAULT_TIME_Y_RATIO))
                .putFloat(orientationPositionKey(SCALE_TARGET_DATE, true), legacyX)
                .putFloat(orientationPositionKey(SCALE_TARGET_DATE, false), legacyY)
                .putFloat(orientationPositionKey(SCALE_TARGET_WEATHER, true), legacyX)
                .putFloat(orientationPositionKey(SCALE_TARGET_WEATHER, false),
                        ClockPositionPolicy.validRatio(legacyY + 0.08f, DEFAULT_WEATHER_Y_RATIO))
                .putInt(CLOCK_LAYOUT_VERSION_KEY, CLOCK_LAYOUT_VERSION)
                .apply();
    }

    private String orientationPositionKey(int target, boolean x) {
        String base;
        if (target == SCALE_TARGET_TIME) {
            base = x ? CLOCK_TIME_POS_X_RATIO : CLOCK_TIME_POS_Y_RATIO;
        } else if (target == SCALE_TARGET_DATE) {
            base = x ? CLOCK_DATE_POS_X_RATIO : CLOCK_DATE_POS_Y_RATIO;
        } else {
            base = x ? CLOCK_WEATHER_POS_X_RATIO : CLOCK_WEATHER_POS_Y_RATIO;
        }
        return orientationKey(base);
    }

    private float defaultPositionRatio(int target, boolean x) {
        if (target == SCALE_TARGET_TIME) return x ? DEFAULT_TIME_X_RATIO : DEFAULT_TIME_Y_RATIO;
        if (target == SCALE_TARGET_DATE) return x ? DEFAULT_DATE_X_RATIO : DEFAULT_DATE_Y_RATIO;
        return x ? DEFAULT_WEATHER_X_RATIO : DEFAULT_WEATHER_Y_RATIO;
    }

    public static void resetClockLayoutPreferences(SharedPreferences.Editor editor) {
        if (editor == null) return;
        editor.putInt(CLOCK_LAYOUT_VERSION_KEY, CLOCK_LAYOUT_VERSION)
                .putInt(CLOCK_LAYOUT_MODE, CLOCK_LAYOUT_MODE_ORIGINAL)
                .putInt(CLOCK_ORIGINAL_SCALE_MODE, CLOCK_ORIGINAL_SCALE_LINKED)
                .putInt(CLOCK_SIZE_MODE, CLOCK_SIZE_MODE_GROUP)
                .putBoolean(CLOCK_SIZES_LINKED, true)
                .remove(CLOCK_GROUP_PIVOT_X_RATIO + "_portrait")
                .remove(CLOCK_GROUP_PIVOT_Y_RATIO + "_portrait")
                .remove(CLOCK_GROUP_PIVOT_X_RATIO + "_landscape")
                .remove(CLOCK_GROUP_PIVOT_Y_RATIO + "_landscape");
        putDefaultClockOrientation(editor, "_portrait");
        putDefaultClockOrientation(editor, "_landscape");
    }

    private static void putDefaultClockOrientation(SharedPreferences.Editor editor,
                                                    String suffix) {
        editor.putFloat(CLOCK_SCALE_FACTOR + suffix, DEFAULT_CLOCK_SCALE)
                .putFloat(CLOCK_POS_X_RATIO + suffix, DEFAULT_GROUP_PANEL_X_RATIO)
                .putFloat(CLOCK_POS_Y_RATIO + suffix, DEFAULT_GROUP_PANEL_Y_RATIO)
                .putFloat(CLOCK_TIME_SCALE_FACTOR + suffix, DEFAULT_CLOCK_SCALE)
                .putFloat(CLOCK_DATE_SCALE_FACTOR + suffix, DEFAULT_CLOCK_SCALE)
                .putFloat(CLOCK_WEATHER_SCALE_FACTOR + suffix, DEFAULT_CLOCK_SCALE)
                .putFloat(CLOCK_TIME_POS_X_RATIO + suffix, DEFAULT_TIME_X_RATIO)
                .putFloat(CLOCK_TIME_POS_Y_RATIO + suffix, DEFAULT_TIME_Y_RATIO)
                .putFloat(CLOCK_DATE_POS_X_RATIO + suffix, DEFAULT_DATE_X_RATIO)
                .putFloat(CLOCK_DATE_POS_Y_RATIO + suffix, DEFAULT_DATE_Y_RATIO)
                .putFloat(CLOCK_WEATHER_POS_X_RATIO + suffix, DEFAULT_WEATHER_X_RATIO)
                .putFloat(CLOCK_WEATHER_POS_Y_RATIO + suffix, DEFAULT_WEATHER_Y_RATIO);
    }

    private String orientationKey(String baseKey) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        return baseKey + (landscape ? "_landscape" : "_portrait");
    }

    private TextView text(String content, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label, int color) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(PRIMARY);
        view.setTextSize(15);
        view.setTypeface(FontManager.getPomodoroChineseFont(this));
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(rounded(color));
        return view;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(14));
        if (color == PANEL || color == PANEL_RAISED) drawable.setStroke(dp(1), STROKE);
        return drawable;
    }

    private void applyUiFont(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(FontManager.getPomodoroChineseFont(this));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyUiFont(group.getChildAt(i));
        }
    }

    private void styleModernDialog(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawable(rounded(PANEL));
        dialog.getWindow().setDimAmount(0.72f);
        applyUiFont(dialog.getWindow().getDecorView());
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) positive.setTextColor(Color.rgb(104, 213, 216));
        if (negative != null) negative.setTextColor(SECONDARY);
    }

    private void showSettingsButton() {
        if (settingsButton == null) {
            return;
        }
        settingsButton.animate().cancel();
        settingsButton.setVisibility(View.VISIBLE);
        settingsButton.animate().alpha(pomodoroSettingsLocked ? 0.42f : 0.88f)
                .setDuration(180).start();
        if (pomodoroButton != null) {
            pomodoroButton.animate().cancel();
            pomodoroButton.setVisibility(View.VISIBLE);
            pomodoroButton.animate().alpha(pomodoroSettingsLocked ? 1.0f : 0.88f)
                    .setDuration(180).start();
        }
        updatePomodoroQuickActions(pomodoroSettingsLocked);
    }

    private void updatePomodoroQuickActions(boolean settingsLocked) {
        pomodoroSettingsLocked = settingsLocked;
        if (settingsButton != null) {
            settingsButton.setEnabled(!settingsLocked);
            settingsButton.setTextColor(settingsLocked ? SECONDARY : Color.WHITE);
            settingsButton.setBackground(settingsLocked
                    ? rounded(Color.rgb(58, 61, 66)) : quickActionBackground);
            if (settingsButton.getVisibility() == View.VISIBLE) {
                settingsButton.setAlpha(settingsLocked ? 0.42f : 0.88f);
            }
        }
        if (pomodoroButton != null) {
            pomodoroButton.setEnabled(true);
            pomodoroButton.setTextColor(Color.WHITE);
            pomodoroButton.setBackground(settingsLocked
                    ? rounded(Color.rgb(37, 124, 137)) : quickActionBackground);
            if (pomodoroButton.getVisibility() == View.VISIBLE) {
                pomodoroButton.setAlpha(settingsLocked ? 1.0f : 0.88f);
            }
        }
    }

    private void hideSettingsButton() {
        if (settingsButton == null || settingsButton.getVisibility() != View.VISIBLE) {
            return;
        }
        settingsButton.animate().cancel();
        settingsButton.animate().alpha(0.0f).setDuration(260).withEndAction(new Runnable() {
            @Override
            public void run() {
                settingsButton.setVisibility(View.GONE);
            }
        }).start();
        if (pomodoroButton != null && pomodoroButton.getVisibility() == View.VISIBLE) {
            pomodoroButton.animate().cancel();
            pomodoroButton.animate().alpha(0.0f).setDuration(260).withEndAction(new Runnable() {
                @Override
                public void run() {
                    pomodoroButton.setVisibility(View.GONE);
                }
            }).start();
        }
    }

    private void startPhotoSlideshow() {
        updatePhotoClock();
        Set<String> selectedFolders = getSelectedPhotoFolders();
        String folderSignature = buildPhotoFolderSignature(selectedFolders);
        if (photoScanInProgress && folderSignature.equals(photoFolderSignature)) {
            schedulePhotoTicker();
            return;
        }
        if (photoCatalogLoaded && folderSignature.equals(photoFolderSignature)) {
            if (photoBitmap == null && !photoLoading && !photoFiles.isEmpty()) {
                loadNextPhoto();
            }
            schedulePhotoTicker();
            return;
        }
        stopPhotoSlideshow();
        if (firstSlideshowStart) {
            firstSlideshowStart = false;
            queueStartupPhoto();
        }
        refreshPhotoFiles(selectedFolders, folderSignature);
        schedulePhotoTicker();
    }

    /** Schedules only the next meaningful update instead of waking every second in clock mode. */
    private void schedulePhotoTicker() {
        photoHandler.removeCallbacks(photoTicker);
        if (!activityResumed || isFinishing() || isDestroyed()) return;
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowWall = System.currentTimeMillis();
        long delay = Math.max(200L, 60_000L - (nowWall % 60_000L));
        PomodoroHelper.Snapshot pomodoro = PomodoroHelper.getSnapshot(this);
        if (pomodoro.running) {
            delay = Math.min(delay, Math.max(200L, 1_000L - (nowWall % 1_000L)));
        }
        if (!isNightSleepActive && !photoLoading && !photoFiles.isEmpty() && nextPhotoAt > nowElapsed) {
            delay = Math.min(delay, Math.max(200L, nextPhotoAt - nowElapsed));
        }
        photoHandler.postDelayed(photoTicker, delay);
    }

    private boolean hasTreePhotoSource() {
        for (String source : getSelectedPhotoFolders()) {
            if (SettingsActivity.treeUriFromSource(source) != null) return true;
        }
        return false;
    }

    private void scheduleSafCatalogRefresh() {
        photoHandler.removeCallbacks(safCatalogRefreshRunnable);
        if (!activityResumed || !hasTreePhotoSource()) return;
        photoHandler.postDelayed(safCatalogRefreshRunnable,
                isLowPowerModeActive() ? SAF_RESCAN_LOW_POWER_MS : SAF_RESCAN_NORMAL_MS);
    }

    private int resolvePhotoFileLimit() {
        boolean lowRam = Runtime.getRuntime().maxMemory() <= 96L * 1024L * 1024L;
        if (Build.VERSION.SDK_INT >= 19) {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            lowRam = lowRam || (manager != null && manager.isLowRamDevice());
        }
        return PhotoCatalogPolicy.maxPhotoFiles(lowRam, Runtime.getRuntime().maxMemory());
    }

    private Set<String> getSelectedPhotoFolders() {
        Set<String> saved = prefs.getStringSet(SettingsActivity.PHOTO_FOLDERS, null);
        if (saved == null) return new HashSet<String>();
        Set<String> normalized = new HashSet<String>();
        for (String source : saved) {
            String value = SettingsActivity.normalizeFolderSource(source);
            if (value.length() > 0) normalized.add(value);
        }
        return normalized;
    }

    private String buildPhotoFolderSignature(Set<String> folders) {
        List<String> ordered = new ArrayList<String>(folders);
        Collections.sort(ordered);
        StringBuilder signature = new StringBuilder();
        if (ordered.isEmpty()) signature.append("@default");
        for (String source : ordered) {
            if (signature.length() > 0) signature.append('\n');
            signature.append(source);
        }
        signature.append("\n@favoritesOnly=").append(favoritesOnly);
        signature.append("\n@playbackOrder=").append(playbackOrder);
        List<String> hidden = new ArrayList<String>(hiddenPhotos);
        Collections.sort(hidden);
        for (String key : hidden) signature.append("\n@hidden=").append(key);
        if (favoritesOnly) {
            List<String> favorites = new ArrayList<String>(favoritePhotos);
            Collections.sort(favorites);
            for (String key : favorites) signature.append("\n@favorite=").append(key);
        }
        return signature.toString();
    }

    private void invalidatePhotoCatalog() {
        photoCatalogLoaded = false;
        photoFolderSignature = "";
    }

    private void stopPhotoSlideshow() {
        photoHandler.removeCallbacks(photoTicker);
        stopPhotoPan();
        if (showcaseColorOverlay != null) {
            showcaseColorOverlay.animate().cancel();
            showcaseColorOverlay.setAlpha(0.0f);
            showcaseColorOverlay.setVisibility(View.GONE);
        }
        photoGeneration++;
        photoLoading = false;
        photoScanInProgress = false;
        if (photoImage != null) {
            photoImage.animate().cancel();
            photoImage.setImageDrawable(null);
            photoImage.setAlpha(1.0f);
        }
        if (backgroundImage != null) {
            backgroundImage.animate().cancel();
            backgroundImage.setImageDrawable(null);
            backgroundImage.setVisibility(View.GONE);
        }
        releaseSoftBackground();
        safeRecycle(photoBitmap);
        photoBitmap = null;
        safeRecycle(pendingPhotoBitmap);
        pendingPhotoBitmap = null;
        currentPhotoSource = null;
        startupPhotoDisplayed = false;
    }

    private void queueStartupPhoto() {
        String key = prefs.getString(LAST_PHOTO_KEY, null);
        if (key == null || key.length() == 0 || hiddenPhotos.contains(key)
                || (favoritesOnly && !favoritePhotos.contains(key))) {
            return;
        }

        final PhotoSource source;
        if (key.startsWith("content://")) {
            source = PhotoSource.fromUri(Uri.parse(key));
        } else {
            File file = new File(key);
            if (!file.isFile()) {
                prefs.edit().remove(LAST_PHOTO_KEY).apply();
                return;
            }
            source = PhotoSource.fromFile(file, canonicalPath(file));
        }

        final int generation = photoGeneration;
        photoLoading = true;
        photoDecodeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                final Bitmap bitmap = decodePhoto(source);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            safeRecycle(bitmap);
                            return;
                        }
                        if (generation != photoGeneration || !activityResumed) {
                            safeRecycle(bitmap);
                            return;
                        }
                        photoLoading = false;
                        if (bitmap == null) {
                            if (!photoFiles.isEmpty()) loadNextPhoto();
                            return;
                        }
                        currentPhotoSource = source;
                        startupPhotoDisplayed = true;
                        nextPhotoAt = SystemClock.elapsedRealtime() + photoIntervalMs;
                        schedulePhotoTicker();
                        displayPhoto(bitmap);
                    }
                });
            }
        });
    }

    private void startPhotoPan() {
        stopPhotoPan();
        if (isLowPowerModeActive() || lowBatteryPaused || effectiveDisplayMode() != 0) {
            return;
        }
        if (photoImage == null || photoBitmap == null
                || photoImage.getWidth() <= 0 || photoImage.getHeight() <= 0) {
            return;
        }
        photoPanReverse = !photoPanReverse;
        lastPhotoPanFrameAt = 0L;
        photoPanAnimator = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        photoPanAnimator.setDuration(photoPanDurationMs);
        photoPanAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        photoPanAnimator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                if (!activityResumed || photoBitmap == null || isNightSleepActive || isFinishing() || isDestroyed()) {
                    animation.cancel();
                    return;
                }
                float progress = (Float) animation.getAnimatedValue();
                long now = SystemClock.uptimeMillis();
                if (!PhotoPanPolicy.shouldRender(
                        progress, lastPhotoPanFrameAt, now,
                        PerformanceModePolicy.frameIntervalMs(effectivePerformanceMode))) {
                    return;
                }
                lastPhotoPanFrameAt = now;
                applyPhotoPan(progress);
            }
        });
        photoPanAnimator.start();
    }

    private void stopPhotoPan() {
        if (photoPanAnimator != null) {
            photoPanAnimator.cancel();
        }
        lastPhotoPanFrameAt = 0L;
    }

    private void safeRecycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled() && Build.VERSION.SDK_INT < 21) {
            bitmap.recycle();
        }
    }

    private void applyPhotoPan(float progress) {
        if (photoImage == null || photoBitmap == null || effectiveDisplayMode() != 0) {
            return;
        }
        int viewWidth = photoImage.getWidth();
        int viewHeight = photoImage.getHeight();
        int bitmapWidth = photoBitmap.getWidth();
        int bitmapHeight = photoBitmap.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) {
            return;
        }

        float smoothProgress = progress * progress * (3.0f - 2.0f * progress);
        float travelFraction = isShowcaseModeActive() ? 0.28f : PHOTO_PAN_TRAVEL_FRACTION;
        float zoomProgress = photoPanReverse ? smoothProgress : 1.0f - smoothProgress;
        float showcaseZoom = isShowcaseModeActive()
                ? 1.02f + 0.04f * zoomProgress : 1.0f;
        float scale = Math.max(
                (float) viewWidth / bitmapWidth,
                (float) viewHeight / bitmapHeight) * showcaseZoom;
        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;
        float overflowX = Math.max(0.0f, scaledWidth - viewWidth);
        float overflowY = Math.max(0.0f, scaledHeight - viewHeight);
        float startPosition = (1.0f - travelFraction) / 2.0f;
        float travelProgress = photoPanReverse ? 1.0f - smoothProgress : smoothProgress;

        float targetPosX = isSmartFocusActive() ? focalX : 0.5f;
        float targetPosY = isSmartFocusActive() ? focalY : 0.5f;

        float position = startPosition + travelFraction * travelProgress;

        float translateX;
        float translateY;
        if (overflowX >= overflowY) {
            translateX = -overflowX * (targetPosX * 0.4f + position * 0.6f);
            translateY = -overflowY * targetPosY;
        } else {
            translateX = -overflowX * targetPosX;
            translateY = -overflowY * (targetPosY * 0.4f + position * 0.6f);
        }

        photoMatrix.reset();
        photoMatrix.setScale(scale, scale);
        photoMatrix.postTranslate(translateX, translateY);
        photoImage.setImageMatrix(photoMatrix);
    }

    private boolean isAdaptiveColorActive() {
        return adaptiveColorEnabled && !isLowPowerModeActive();
    }

    private boolean isSmartFocusActive() {
        return smartFocusEnabled && !isLowPowerModeActive() && effectiveDisplayMode() == 0;
    }

    private int effectiveDisplayMode() {
        return isLowPowerModeActive() && photoDisplayMode == 2 ? 1 : photoDisplayMode;
    }

    private void applyPhotoPresentation(Bitmap bitmap) {
        if (photoImage == null || bitmap == null) {
            return;
        }
        int mode = effectiveDisplayMode();
        if (mode == 0) {
            photoImage.setScaleType(ImageView.ScaleType.MATRIX);
            if (backgroundImage != null) {
                backgroundImage.setImageDrawable(null);
                backgroundImage.setVisibility(View.GONE);
            }
            releaseSoftBackground();
            applyPhotoPan(0.0f);
            return;
        }

        stopPhotoPan();
        photoImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoImage.setImageMatrix(new Matrix());
        if (mode == 2 && backgroundImage != null) {
            releaseSoftBackground();
            try {
                softBackgroundBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
                backgroundImage.setImageBitmap(softBackgroundBitmap);
                backgroundImage.setAlpha(0.72f);
                backgroundImage.setVisibility(View.VISIBLE);
            } catch (OutOfMemoryError ignored) {
                backgroundImage.setVisibility(View.GONE);
            }
        } else if (backgroundImage != null) {
            backgroundImage.setImageDrawable(null);
            backgroundImage.setVisibility(View.GONE);
            releaseSoftBackground();
        }
    }

    private void releaseSoftBackground() {
        if (softBackgroundBitmap != null
                && softBackgroundBitmap != photoBitmap
                && !softBackgroundBitmap.isRecycled()) {
            safeRecycle(softBackgroundBitmap);
        }
        softBackgroundBitmap = null;
    }

    private void updatePhotoClock() {
        nowDate.setTime(System.currentTimeMillis());
        updateClockTimeText();
        if (photoDate != null) {
            if (FontManager.usesLatinDate(dateFontId)) {
                photoDate.setText(photoDateFormatEn.format(nowDate));
            } else {
                photoDate.setText(photoDateFormat.format(nowDate));
            }
        }
        if (daylightProgressView != null
                && daylightProgressView.getVisibility() == View.VISIBLE) {
            daylightProgressView.setNow(nowDate.getTime());
        }
        updateAlarmIndicator();
        updatePomodoroDisplay(false);
    }

    private void updateClockTimeText() {
        if (photoTime == null) return;
        nowDate.setTime(System.currentTimeMillis());
        photoTime.setText(shouldShowClockSeconds()
                ? photoTimeSecondsFormat.format(nowDate)
                : photoTimeFormat.format(nowDate));
    }

    private boolean shouldShowClockSeconds() {
        PowerStateMonitor.State state = powerState == null
                ? PowerStateMonitor.State.unknown() : powerState;
        return ClockSecondPolicy.shouldShowSeconds(clockSecondsMode,
                state.batteryPresent, state.plugged);
    }

    private void scheduleSecondClockTicker() {
        photoHandler.removeCallbacks(secondClockTicker);
        if (!activityResumed || !shouldShowClockSeconds() || isNightSleepActive) return;
        photoHandler.postDelayed(secondClockTicker,
                ClockSecondPolicy.delayToNextSecond(System.currentTimeMillis()));
    }

    private void checkPomodoroCompletion() {
        PomodoroHelper.Transition transition = PomodoroHelper.finishIfDue(this);
        if (transition != null) {
            vibratePhaseFinished();
            Toast.makeText(this, PomodoroHelper.phaseLabel(transition.finishedPhase)
                    + "結束，下一階段：" + PomodoroHelper.phaseLabel(transition.nextPhase),
                    Toast.LENGTH_LONG).show();
            updatePomodoroDisplay(false);
        }
    }

    private void updatePomodoroDisplay() {
        updatePomodoroDisplay(true);
    }

    /** Lets the shared photo ticker refresh the display without scheduling itself twice. */
    private void updatePomodoroDisplay(boolean rescheduleTicker) {
        if (pomodoroRow == null || pomodoroLabel == null || pomodoroText == null) return;
        PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(this);
        if (!snapshot.hasSession) {
            applyPomodoroModeLayout(false);
            updatePomodoroQuickActions(false);
            pomodoroFocusPanel.setClickable(false);
            pomodoroRow.setVisibility(View.GONE);
            applyPomodoroAdvancedDisplay(null);
            return;
        }
        pomodoroLabel.setText(pomodoroEnglishLabel(snapshot));
        pomodoroText.setText(PomodoroHelper.formatRemaining(snapshot.remainingMs));
        applyPomodoroAdvancedDisplay(snapshot);
        applyPomodoroModeLayout(true);
        updatePomodoroQuickActions(snapshot.running);
        // 暫停時整個倒數畫面就是「繼續」按鈕；運行中仍讓既有觸控行為處理。
        pomodoroFocusPanel.setClickable(!snapshot.running);
        pomodoroRow.setVisibility(View.VISIBLE);
        if (activityResumed && rescheduleTicker) schedulePhotoTicker();
    }

    private void applyPomodoroAdvancedDisplay(PomodoroHelper.Snapshot snapshot) {
        boolean visible = pomodoroAdvancedDisplay && snapshot != null;
        if (pomodoroAdvancedInfo != null) {
            pomodoroAdvancedInfo.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                int cycle = snapshot.completedFocusSessions
                        + (PomodoroHelper.PHASE_FOCUS.equals(snapshot.phase) ? 1 : 0);
                pomodoroAdvancedInfo.setText(
                        PomodoroHelper.phaseLabel(snapshot.phase)
                                + "　·　第 " + Math.max(1, cycle) + " 輪"
                                + "　·　已完成 " + snapshot.completedFocusSessions + " 次");
            }
        }
        if (pomodoroProgressView != null) {
            pomodoroProgressView.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                float progress = snapshot.phaseDurationMs <= 0L ? 0.0f
                        : snapshot.elapsedMs / (float) snapshot.phaseDurationMs;
                pomodoroProgressView.setProgress(progress);
                pomodoroProgressView.setProgressColor(
                        PomodoroHelper.PHASE_FOCUS.equals(snapshot.phase)
                                ? POMODORO_RED : Color.rgb(104, 213, 216));
            }
        }
    }

    /** A paused session is intentionally resumed from the focus screen itself. */
    private void resumePomodoroFromFocusScreen() {
        PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(this);
        if (!snapshot.hasSession || snapshot.running) {
            return;
        }
        boolean exact = PomodoroHelper.startOrResume(this);
        if (!exact) {
            Toast.makeText(this, "系統未提供精確提醒，倒數會在畫面開啟時持續更新",
                    Toast.LENGTH_LONG).show();
        }
        updatePomodoroDisplay();
        hideSettingsButton();
        resetImmersiveTimeout();
    }

    private String pomodoroEnglishLabel(PomodoroHelper.Snapshot snapshot) {
        String phase = PomodoroHelper.PHASE_SHORT_BREAK.equals(snapshot.phase) ? "Short Break"
                : PomodoroHelper.PHASE_LONG_BREAK.equals(snapshot.phase) ? "Long Break" : "Focus";
        return snapshot.running ? phase : phase + "\nPaused";
    }

    private void applyPomodoroModeLayout(boolean active) {
        if (clockPanel == null || pomodoroFocusPanel == null) return;
        if (pomodoroModeLayoutActive != active) {
            pomodoroModeLayoutActive = active;
            if (active) {
                moveClockBlocksToPomodoroInfoPanel();
                movePomodoroRowToFocusPanel();
                pomodoroFocusPanel.setVisibility(View.VISIBLE);
            } else {
                restoreClockBlocksAfterPomodoro();
                movePomodoroRowToClockPanel();
                pomodoroFocusPanel.setVisibility(View.GONE);
            }
            if (!active) applyClockTranslation();
            if (rootContainer != null) {
                rootContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        updateWeatherFromCache();
                        if (!pomodoroModeLayoutActive) restoreClockPosition();
                    }
                });
            }
        }
        if (photoTime != null) {
            photoTime.setVisibility(clockTimeEnabled ? View.VISIBLE : View.GONE);
            photoTime.setAlpha(1.0f);
        }
        updateTimeBlockVisibility();
        if (dateRow != null) dateRow.setVisibility(View.VISIBLE);
        if (alarmRow != null) alarmRow.setVisibility(active ? View.GONE : alarmRow.getVisibility());
        updateDateBlockVisibility();
        int dimColor = Color.argb(120, 100, 100, 100);
        if (photoTime != null) {
            photoTime.setTextColor(isNightSleepActive ? dimColor : Color.WHITE);
        }
        if (pomodoroLabel != null) {
            pomodoroLabel.setTextSize(24);
            pomodoroLabel.setTextColor(isNightSleepActive ? dimColor : Color.WHITE);
        }
        if (pomodoroText != null) {
            pomodoroText.setTextSize(112);
            pomodoroText.setTextColor(isNightSleepActive ? dimColor : POMODORO_RED);
        }
        if (active) {
            applyClockComponentSizes(1.0f, 1.0f, 1.0f);
        } else {
            applyClockScale();
        }
        if (active) applyPomodoroFocusLayout();
    }

    /** Uses the real clock views in a temporary default-size group without saving its position. */
    private void moveClockBlocksToPomodoroInfoPanel() {
        if (pomodoroInfoPanel == null) return;
        moveBlockToPomodoroInfoPanel(timeBlock);
        moveBlockToPomodoroInfoPanel(dateBlock);
        moveBlockToPomodoroInfoPanel(weatherBlock);
        resetBlockTranslation(timeBlock);
        resetBlockTranslation(dateBlock);
        resetBlockTranslation(weatherBlock);
        pomodoroInfoPanel.setVisibility(View.VISIBLE);
    }

    private void moveBlockToPomodoroInfoPanel(View block) {
        if (block == null || block.getParent() == pomodoroInfoPanel) return;
        if (block.getParent() instanceof ViewGroup) {
            ((ViewGroup) block.getParent()).removeView(block);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        pomodoroInfoPanel.addView(block, params);
    }

    private void restoreClockBlocksAfterPomodoro() {
        if (legacyClockPanelActive) {
            moveClockBlocksToLegacyPanel();
        } else {
            moveClockBlocksToIndependentPanel();
        }
        if (pomodoroInfoPanel != null) pomodoroInfoPanel.setVisibility(View.GONE);
        applyClockBlockBackgrounds();
    }

    private void movePomodoroRowToFocusPanel() {
        if (pomodoroRow.getParent() == pomodoroFocusPanel) return;
        if (pomodoroRow.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) pomodoroRow.getParent()).removeView(pomodoroRow);
        }
        FrameLayout.LayoutParams focusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        focusParams.leftMargin = -dp(46);
        focusParams.topMargin = -dp(58);
        pomodoroFocusPanel.addView(pomodoroRow, focusParams);
    }

    /** Uses a fixed focus layout while keeping the countdown inside portrait screens. */
    private void applyPomodoroFocusLayout() {
        if (!pomodoroModeLayoutActive || pomodoroFocusPanel == null || pomodoroRow == null
                || pomodoroText == null || pomodoroLabel == null
                || pomodoroRow.getParent() != pomodoroFocusPanel) return;
        int panelWidth = pomodoroFocusPanel.getWidth();
        if (panelWidth <= 0) {
            pomodoroFocusPanel.post(new Runnable() {
                @Override
                public void run() {
                    applyPomodoroFocusLayout();
                }
            });
            return;
        }
        boolean portrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        if (pomodoroProgressView != null
                && pomodoroProgressView.getVisibility() == View.VISIBLE) {
            ViewGroup.LayoutParams progressParams = pomodoroProgressView.getLayoutParams();
            int progressWidth = Math.max(dp(120), Math.min(dp(240), panelWidth - dp(32)));
            if (progressParams != null && progressParams.width != progressWidth) {
                progressParams.width = progressWidth;
                pomodoroProgressView.setLayoutParams(progressParams);
            }
        }
        float targetSp = portrait ? 100.0f : 112.0f;
        pomodoroText.setTextSize(targetSp);
        float availableWidth = panelWidth - dp(portrait ? 32 : 112);
        float measuredWidth = pomodoroText.getPaint().measureText("180:00");
        pomodoroText.setTextSize(PomodoroLayout.fitTextSize(
                targetSp, measuredWidth, availableWidth));
        pomodoroLabel.setTextSize(portrait ? 20.0f : 24.0f);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) pomodoroRow.getLayoutParams();
        params.gravity = Gravity.CENTER;
        params.leftMargin = portrait ? 0 : -dp(46);
        // The row includes the small phase label. Shift by half its height so the
        // countdown itself, rather than the combined row, is exactly centered.
        params.topMargin = portrait ? -Math.max(dp(10), pomodoroLabel.getHeight() / 2) : -dp(46);
        pomodoroRow.setLayoutParams(params);
    }

    private void movePomodoroRowToClockPanel() {
        if (pomodoroRow.getParent() == clockPanel) return;
        if (pomodoroRow.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) pomodoroRow.getParent()).removeView(pomodoroRow);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        params.setMargins(dp(28), dp(28), dp(16), dp(16));
        clockPanel.addView(pomodoroRow, Math.min(1, clockPanel.getChildCount()), params);
    }

    private void resizeView(View view, int widthDp, int heightDp) {
        android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            int width = widthDp < 0 ? widthDp : dp(widthDp);
            int height = heightDp < 0 ? heightDp : dp(heightDp);
            if (params.width != width || params.height != height) {
                params.width = width;
                params.height = height;
                view.setLayoutParams(params);
            }
        }
    }

    private void updateAlarmIndicator() {
        if (alarmRow == null || alarmTimeText == null) return;
        int previousVisibility = alarmRow.getVisibility();
        String alarmTime = AlarmHelper.getNextAlarmTimeString(this);
        if (alarmTime != null) {
            alarmTimeText.setText(alarmTime);
            alarmRow.setVisibility(View.VISIBLE);
        } else {
            alarmRow.setVisibility(View.GONE);
        }
        updateDateBlockVisibility();
        if (previousVisibility != alarmRow.getVisibility() && rootContainer != null) {
            rootContainer.post(new Runnable() {
                @Override
                public void run() {
                    restoreClockPosition();
                }
            });
        }
    }

    private void checkForegroundAlarm() {
        SharedPreferences p = AlarmHelper.getPrefs(this);
        if (!p.getBoolean(AlarmHelper.PREF_ALARM_ENABLED, false)) return;

        int alarmH = p.getInt(AlarmHelper.PREF_ALARM_HOUR, 7);
        int alarmM = p.getInt(AlarmHelper.PREF_ALARM_MINUTE, 0);

        java.util.Calendar now = java.util.Calendar.getInstance();
        int nowH = now.get(java.util.Calendar.HOUR_OF_DAY);
        int nowM = now.get(java.util.Calendar.MINUTE);

        long minuteKey = (long) nowH * 60 + nowM;
        if (nowH == alarmH && nowM == alarmM && lastAlarmFiredMinute != minuteKey) {
            lastAlarmFiredMinute = minuteKey;

            boolean repeat = p.getBoolean(AlarmHelper.PREF_ALARM_REPEAT, true);
            if (!repeat) {
                p.edit().putBoolean(AlarmHelper.PREF_ALARM_ENABLED, false).apply();
            }

            Intent ringIntent = new Intent(this, AlarmRingingActivity.class);
            ringIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(ringIntent);
        }
    }

    private void scheduleWeatherRefresh() {
        photoHandler.removeCallbacks(weatherRefreshRunnable);
        updateWeatherFromCache();
        if (!weatherEnabled || Double.isNaN(weatherLatitude) || Double.isNaN(weatherLongitude)) {
            hideWeatherRows();
            return;
        }
        long lastSuccess = prefs.getLong(SettingsActivity.WEATHER_UPDATED_AT, 0L);
        long basis = Math.max(lastSuccess, weatherLastAttemptAt);
        long age = basis <= 0L ? Long.MAX_VALUE
                : Math.max(0L, System.currentTimeMillis() - basis);
        long interval = isLowPowerModeActive()
                ? WEATHER_FRESH_LOW_POWER_MS : WEATHER_FRESH_NORMAL_MS;
        photoHandler.postDelayed(weatherRefreshRunnable, Math.max(0L, interval - age));
    }

    private void requestWeatherRefresh() {
        requestWeatherRefresh(false, null);
    }

    private void requestWeatherRefresh(boolean force, WeatherRefreshCallback callback) {
        if (force) {
            photoHandler.removeCallbacks(weatherRefreshRunnable);
        }
        if (!activityResumed || !weatherEnabled
                || Double.isNaN(weatherLatitude) || Double.isNaN(weatherLongitude)) {
            if (callback != null) callback.onComplete(false);
            return;
        }
        if (weatherFetchInFlight) {
            if (callback != null) pendingWeatherRefreshCallback = callback;
            return;
        }
        if (!WeatherClient.isWifiConnected(this)) {
            weatherLastAttemptAt = System.currentTimeMillis();
            photoHandler.postDelayed(weatherRefreshRunnable,
                    isLowPowerModeActive()
                            ? WEATHER_FRESH_LOW_POWER_MS : WEATHER_FRESH_NORMAL_MS);
            if (callback != null) callback.onComplete(false);
            return;
        }
        weatherFetchInFlight = true;
        weatherLastAttemptAt = System.currentTimeMillis();
        pendingWeatherRefreshCallback = callback;
        final double latitude = weatherLatitude;
        final double longitude = weatherLongitude;
        final boolean extendedEnabled = weatherExtendedEnabled
                && (weatherExtendedForecastEnabled || weatherExtendedDaylightEnabled);
        weatherExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    WeatherClient.CurrentWeather weather;
                    WeatherClient.ExtendedWeather extended = null;
                    if (extendedEnabled) {
                        extended = WeatherClient.fetchExtended(latitude, longitude);
                        weather = extended.current;
                    } else {
                        weather = WeatherClient.fetchCurrent(latitude, longitude);
                    }
                    SharedPreferences.Editor editor = prefs.edit()
                            .putInt(SettingsActivity.WEATHER_TEMPERATURE, weather.temperatureCelsius)
                            .putInt(SettingsActivity.WEATHER_CODE, weather.weatherCode)
                            .putBoolean(SettingsActivity.WEATHER_IS_DAY, weather.daytime)
                            .putLong(SettingsActivity.WEATHER_UPDATED_AT,
                                    System.currentTimeMillis());
                    if (extended != null) {
                        editor.putLong(SettingsActivity.WEATHER_EXTENDED_UPDATED_AT,
                                        System.currentTimeMillis())
                                .putString(SettingsActivity.WEATHER_EXTENDED_FORECAST,
                                        encodeExtendedForecast(extended.nextHours))
                                .putLong(SettingsActivity.WEATHER_EXTENDED_SUNRISE_AT,
                                        extended.sunriseAtMs)
                                .putLong(SettingsActivity.WEATHER_EXTENDED_SUNSET_AT,
                                        extended.sunsetAtMs);
                    }
                    editor.apply();
                    success = true;
                } catch (Exception ignored) {
                    // If the optional response is malformed or unavailable, retain the current
                    // weather experience by retrying the small legacy request once.
                    if (extendedEnabled) {
                        try {
                            WeatherClient.CurrentWeather weather =
                                    WeatherClient.fetchCurrent(latitude, longitude);
                            prefs.edit()
                                    .putInt(SettingsActivity.WEATHER_TEMPERATURE,
                                            weather.temperatureCelsius)
                                    .putInt(SettingsActivity.WEATHER_CODE, weather.weatherCode)
                                    .putBoolean(SettingsActivity.WEATHER_IS_DAY, weather.daytime)
                                    .putLong(SettingsActivity.WEATHER_UPDATED_AT,
                                            System.currentTimeMillis())
                                    .apply();
                            success = true;
                        } catch (Exception ignoredCurrent) {
                            // 保留快取；舊裝置 TLS 或暫時斷線時不打擾相簿播放。
                        }
                    }
                }
                final boolean requestSucceeded = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        weatherFetchInFlight = false;
                        WeatherRefreshCallback callbackToRun = pendingWeatherRefreshCallback;
                        pendingWeatherRefreshCallback = null;
                        if (activityResumed) {
                            scheduleWeatherRefresh();
                        }
                        if (callbackToRun != null) {
                            callbackToRun.onComplete(requestSucceeded);
                        }
                    }
                });
            }
        });
    }

    private void updateWeatherFromCache() {
        if (weatherRow == null || compactWeatherRow == null || !weatherEnabled) {
            hideWeatherRows();
            return;
        }
        long updatedAt = prefs.getLong(SettingsActivity.WEATHER_UPDATED_AT, 0L);
        long age = System.currentTimeMillis() - updatedAt;
        if (updatedAt <= 0L || age < 0L || age > WEATHER_MAX_AGE_MS) {
            hideWeatherRows();
            return;
        }
        int temperature = prefs.getInt(SettingsActivity.WEATHER_TEMPERATURE, 0);
        int code = prefs.getInt(SettingsActivity.WEATHER_CODE, 0);
        boolean isDay = prefs.getBoolean(SettingsActivity.WEATHER_IS_DAY, true);
        weatherTemperature.setText(String.format(Locale.US, "%d°", temperature));
        compactWeatherTemperature.setText(String.format(Locale.US, "%d°", temperature));
        String displayedLocation = weatherMinimalLocation
                ? WeatherClient.minimalLocationName(weatherLocationName)
                : weatherLocationName;
        weatherLocation.setText(WeatherClient.removeAccents(displayedLocation));
        weatherLocation.setVisibility(
                weatherShowLocation && displayedLocation.length() > 0
                        ? View.VISIBLE : View.GONE);
        weatherIcon.setWeather(code, isDay);
        compactWeatherIcon.setWeather(code, isDay);

        boolean compact = weatherCompactMode && !weatherShowLocation;
        compactWeatherRow.setVisibility(compact ? View.VISIBLE : View.GONE);
        weatherRow.setVisibility(compact ? View.GONE : View.VISIBLE);
        updateExtendedWeatherFromCache();
        updateWeatherBlockVisibility();
        if (clockPanel != null) {
            clockPanel.requestLayout();
            if (legacyClockPanel != null) legacyClockPanel.requestLayout();
            clockPanel.post(new Runnable() {
                @Override
                public void run() {
                    restoreClockPosition();
                }
            });
        }
    }

    private void hideWeatherRows() {
        if (compactWeatherRow != null) compactWeatherRow.setVisibility(View.GONE);
        if (weatherRow != null) weatherRow.setVisibility(View.GONE);
        if (weatherExtendedPanel != null) weatherExtendedPanel.setVisibility(View.GONE);
        updateWeatherBlockVisibility();
    }

    private String encodeExtendedForecast(List<WeatherClient.HourlyWeather> hours) {
        StringBuilder encoded = new StringBuilder();
        if (hours == null) return "";
        for (WeatherClient.HourlyWeather hour : hours) {
            if (encoded.length() > 0) encoded.append(';');
            encoded.append(hour.localTime).append(',')
                    .append(hour.temperatureCelsius).append(',')
                    .append(hour.precipitationProbability).append(',')
                    .append(hour.weatherCode);
        }
        return encoded.toString();
    }

    private List<WeatherClient.HourlyWeather> decodeExtendedForecast(String encoded) {
        List<WeatherClient.HourlyWeather> hours =
                new ArrayList<WeatherClient.HourlyWeather>();
        if (encoded == null || encoded.length() == 0) return hours;
        String[] entries = encoded.split(";", -1);
        for (String entry : entries) {
            String[] fields = entry.split(",", -1);
            if (fields.length != 4) continue;
            try {
                hours.add(new WeatherClient.HourlyWeather(
                        fields[0], Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]), Integer.parseInt(fields[3])));
            } catch (NumberFormatException ignored) {
                // Ignore one malformed cached item rather than hiding valid later items.
            }
        }
        return hours;
    }

    private void updateExtendedWeatherFromCache() {
        if (weatherExtendedPanel == null || !weatherExtendedEnabled) {
            if (weatherExtendedPanel != null) weatherExtendedPanel.setVisibility(View.GONE);
            return;
        }
        long updatedAt = prefs.getLong(SettingsActivity.WEATHER_EXTENDED_UPDATED_AT, 0L);
        long age = System.currentTimeMillis() - updatedAt;
        List<WeatherClient.HourlyWeather> hours = decodeExtendedForecast(
                prefs.getString(SettingsActivity.WEATHER_EXTENDED_FORECAST, ""));
        boolean cacheValid = updatedAt > 0L && age >= 0L && age <= WEATHER_MAX_AGE_MS;
        boolean hasForecast = cacheValid && !hours.isEmpty();
        long sunriseAtMs = prefs.getLong(SettingsActivity.WEATHER_EXTENDED_SUNRISE_AT, -1L);
        long sunsetAtMs = prefs.getLong(SettingsActivity.WEATHER_EXTENDED_SUNSET_AT, -1L);
        boolean hasDaylight = cacheValid && sunriseAtMs > 0L && sunsetAtMs > sunriseAtMs;
        if ((!weatherExtendedForecastEnabled || !hasForecast)
                && (!weatherExtendedDaylightEnabled || !hasDaylight)) {
            weatherForecastText.setVisibility(View.GONE);
            daylightProgressView.setVisibility(View.GONE);
            daylightLabel.setVisibility(View.GONE);
            weatherExtendedPanel.setVisibility(View.GONE);
            return;
        }

        if (weatherExtendedForecastEnabled && hasForecast) {
            StringBuilder forecast = new StringBuilder("未來三小時  ");
            for (int i = 0; i < hours.size(); i++) {
                if (i > 0) forecast.append("  ·  ");
                WeatherClient.HourlyWeather hour = hours.get(i);
                forecast.append(hour.localTime).append(' ')
                        .append(hour.temperatureCelsius).append("°/")
                        .append(hour.precipitationProbability).append('%');
            }
            weatherForecastText.setText(forecast.toString());
            weatherForecastText.setVisibility(View.VISIBLE);
        } else {
            weatherForecastText.setVisibility(View.GONE);
        }

        if (weatherExtendedDaylightEnabled && hasDaylight) {
            daylightProgressView.setTimes(sunriseAtMs, sunsetAtMs);
            daylightProgressView.setNow(System.currentTimeMillis());
            daylightProgressView.setVisibility(View.VISIBLE);
            daylightLabel.setText("日出 " + weatherSunTime(sunriseAtMs)
                    + "　日落 " + weatherSunTime(sunsetAtMs));
            daylightLabel.setVisibility(View.VISIBLE);
        } else {
            daylightProgressView.setVisibility(View.GONE);
            daylightLabel.setVisibility(View.GONE);
        }
        weatherExtendedPanel.setVisibility(View.VISIBLE);
    }

    private String weatherSunTime(long timeMs) {
        if (weatherTimezone != null && weatherTimezone.length() > 0
                && !"auto".equalsIgnoreCase(weatherTimezone)) {
            weatherSunTimeFormat.setTimeZone(TimeZone.getTimeZone(weatherTimezone));
        } else {
            weatherSunTimeFormat.setTimeZone(TimeZone.getDefault());
        }
        return weatherSunTimeFormat.format(new Date(timeMs));
    }

    private double parseDouble(String value) {
        if (value == null) return Double.NaN;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private void refreshPhotoFiles(
            final Set<String> selectedFolders, final String folderSignature) {
        photoFiles.clear();
        playbackNavigator.setShuffle(playbackOrder == PlaybackOrderPolicy.RANDOM);
        playbackNavigator.reset(0);
        photoFailures = 0;
        photoCatalogLoaded = false;
        photoScanInProgress = true;
        photoFolderSignature = folderSignature;
        if (!photoLoading) {
            showPhotoStatus("正在讀取相簿…", SECONDARY);
        }

        final int generation = photoGeneration;
        final Set<String> favoriteSnapshot = new HashSet<String>(favoritePhotos);
        final Set<String> hiddenSnapshot = new HashSet<String>(hiddenPhotos);
        final boolean favoritesOnlySnapshot = favoritesOnly;

        photoScanExecutor.execute(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                final List<PhotoSource> found = new ArrayList<PhotoSource>();
                final boolean[] firstPublished = new boolean[] { false };
                final boolean[] inaccessibleTree = new boolean[] { false };
                collectSelectedPhotoFiles(selectedFolders, found, new PhotoDiscovery() {
                    @Override
                    public void onPhotoDiscovered(final PhotoSource source) {
                        if (firstPublished[0]
                                || !isPhotoEligible(source, favoriteSnapshot,
                                        hiddenSnapshot, favoritesOnlySnapshot)) {
                            return;
                        }
                        firstPublished[0] = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (generation != photoGeneration || !activityResumed
                                        || !photoFiles.isEmpty()) {
                                    return;
                                }
                                photoFiles.add(source);
                                playbackNavigator.setShuffle(
                                        playbackOrder == PlaybackOrderPolicy.RANDOM);
                                playbackNavigator.reset(photoFiles.size());
                                if (!photoLoading) loadNextPhoto();
                            }
                        });
                    }
                }, inaccessibleTree, generation);
                final List<PhotoSource> discovered = filterPhotos(
                        found, favoriteSnapshot, hiddenSnapshot, favoritesOnlySnapshot);
                PlaybackOrderPolicy.sort(discovered, playbackOrder);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != photoGeneration || !activityResumed) {
                            return;
                        }
                        photoScanInProgress = false;
                        photoCatalogLoaded = true;
                        photoFiles.clear();
                        photoFiles.addAll(discovered);
                        playbackNavigator.setShuffle(playbackOrder == PlaybackOrderPolicy.RANDOM);
                        int startupIndex = findPhotoIndex(currentPhotoSource);
                        if (startupIndex >= 0) {
                            playbackNavigator.resetAt(photoFiles.size(), startupIndex);
                        } else {
                            playbackNavigator.reset(photoFiles.size());
                        }
                        photoFailures = 0;

                        if (photoFiles.isEmpty()) {
                            showPhotoStatus(
                                    inaccessibleTree[0]
                                            ? "SD 卡資料夾權限已失效\n請到設定重新選取"
                                            : "沒有可播放的相片\n請到設定選擇相簿資料夾",
                                    inaccessibleTree[0] ? WARNING : SECONDARY);
                        } else if ((!startupPhotoDisplayed || startupIndex < 0)
                                && photoBitmap == null && !photoLoading) {
                            loadNextPhoto();
                        } else {
                            photoStatus.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private int findPhotoIndex(PhotoSource target) {
        if (target == null) return -1;
        String key = target.key();
        for (int i = 0; i < photoFiles.size(); i++) {
            if (key.equals(photoFiles.get(i).key())) return i;
        }
        return -1;
    }

    private List<PhotoSource> filterPhotos(
            List<PhotoSource> photos,
            Set<String> favorites,
            Set<String> hidden,
            boolean onlyFavorites) {
        List<PhotoSource> filtered = new ArrayList<PhotoSource>();
        for (PhotoSource source : photos) {
            if (isPhotoEligible(source, favorites, hidden, onlyFavorites)) filtered.add(source);
        }
        return filtered;
    }

    private boolean isPhotoEligible(PhotoSource source, Set<String> favorites,
            Set<String> hidden, boolean onlyFavorites) {
        String key = source.key();
        return !hidden.contains(key) && (!onlyFavorites || favorites.contains(key));
    }

    private void collectSelectedPhotoFiles(Set<String> selectedFolders,
            List<PhotoSource> output, PhotoDiscovery discovery, boolean[] inaccessibleTree,
            int scanGeneration) {
        Set<String> folders = new HashSet<String>(selectedFolders);
        if (folders.isEmpty()) {
            if (Build.VERSION.SDK_INT >= 21) {
                File bundledDirectory = new File(getFilesDir(), "數位風景");
                if (bundledDirectory.isDirectory()) {
                    folders.add(SettingsActivity.FILE_SOURCE_PREFIX
                            + bundledDirectory.getAbsolutePath());
                }
            } else {
                File defaultDirectory = new File(
                        Environment.getExternalStorageDirectory(), PHOTO_DIRECTORY);
                if (!defaultDirectory.exists()) defaultDirectory.mkdirs();
                folders.add(SettingsActivity.FILE_SOURCE_PREFIX
                        + defaultDirectory.getAbsolutePath());
            }
        }

        Set<String> visitedDirectories = new HashSet<String>();
        Set<String> discoveredPhotos = new LinkedHashSet<String>();
        for (String source : folders) {
            if (scanGeneration != photoGeneration
                    || discoveredPhotos.size() >= photoFileLimit) break;
            Uri treeUri = SettingsActivity.treeUriFromSource(source);
            if (treeUri != null && Build.VERSION.SDK_INT >= 21) {
                collectDocumentTreePhotos(treeUri, visitedDirectories, discoveredPhotos,
                        output, discovery, inaccessibleTree, 0, scanGeneration);
            } else {
                String path = SettingsActivity.filePathFromSource(source);
                if (path != null) {
                    collectPhotoFiles(new File(path), visitedDirectories, discoveredPhotos,
                            output, discovery, 0, scanGeneration);
                }
            }
        }
    }

    private void collectPhotoFiles(File directory, Set<String> visitedDirectories,
            Set<String> discoveredPhotos, List<PhotoSource> output,
            PhotoDiscovery discovery, int depth, int scanGeneration) {
        if (directory == null || !directory.isDirectory() || depth > MAX_PHOTO_DEPTH
                || discoveredPhotos.size() >= photoFileLimit
                || scanGeneration != photoGeneration) {
            return;
        }
        String directoryPath = canonicalPath(directory);
        if (!visitedDirectories.add("file|" + directoryPath)
                || new File(directory, ".nomedia").exists()) {
            return;
        }
        File[] entries = directory.listFiles();
        if (entries == null) return;
        List<File> childDirectories = new ArrayList<File>();
        for (File entry : entries) {
            if (scanGeneration != photoGeneration
                    || discoveredPhotos.size() >= photoFileLimit) return;
            if (entry.isDirectory() && !entry.getName().startsWith(".")) {
                childDirectories.add(entry);
            } else if (entry.isFile() && isSupportedPhoto(entry.getName())) {
                String identity = canonicalPath(entry);
                if (discoveredPhotos.add(identity)) {
                    PhotoSource source = PhotoSource.fromFile(
                            entry, identity, playbackOrder == PlaybackOrderPolicy.NEWEST
                                    || playbackOrder == PlaybackOrderPolicy.OLDEST);
                    output.add(source);
                    discovery.onPhotoDiscovered(source);
                }
            }
        }
        for (File child : childDirectories) {
            if (scanGeneration != photoGeneration
                    || discoveredPhotos.size() >= photoFileLimit) return;
            collectPhotoFiles(child, visitedDirectories, discoveredPhotos,
                    output, discovery, depth + 1, scanGeneration);
        }
    }

    @android.annotation.TargetApi(21)
    private void collectDocumentTreePhotos(Uri treeUri, Set<String> visitedDirectories,
            Set<String> discoveredPhotos, List<PhotoSource> output,
            PhotoDiscovery discovery, boolean[] inaccessibleTree, int depth,
            int scanGeneration) {
        if (depth > MAX_PHOTO_DEPTH || discoveredPhotos.size() >= photoFileLimit
                || scanGeneration != photoGeneration) return;
        try {
            String rootId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri root = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId);
            collectDocumentDirectoryPhotos(treeUri, root, visitedDirectories, discoveredPhotos,
                    output, discovery, inaccessibleTree, depth, scanGeneration);
        } catch (SecurityException error) {
            inaccessibleTree[0] = true;
        } catch (Exception ignored) {
            // A removed card or malformed provider must not stop other folders.
        }
    }

    @android.annotation.TargetApi(21)
    private void collectDocumentDirectoryPhotos(Uri treeUri, Uri directoryUri,
            Set<String> visitedDirectories, Set<String> discoveredPhotos,
            List<PhotoSource> output, PhotoDiscovery discovery,
            boolean[] inaccessibleTree, int depth, int scanGeneration) {
        if (depth > MAX_PHOTO_DEPTH || discoveredPhotos.size() >= photoFileLimit
                || scanGeneration != photoGeneration) return;
        String directoryId;
        try {
            directoryId = DocumentsContract.getDocumentId(directoryUri);
        } catch (Exception ignored) {
            return;
        }
        if (!visitedDirectories.add("tree|" + treeUri + "|" + directoryId)) return;

        Cursor cursor = null;
        try {
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId);
            String[] projection = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
            };
            cursor = getContentResolver().query(children, projection, null, null, null);
            if (cursor == null) return;
            int idColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int typeColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE);
            int modifiedColumn = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            List<Uri> childDirectories = new ArrayList<Uri>();
            while (cursor.moveToNext() && discoveredPhotos.size() < photoFileLimit
                    && scanGeneration == photoGeneration) {
                String childId = cursor.getString(idColumn);
                String name = cursor.getString(nameColumn);
                String mimeType = cursor.getString(typeColumn);
                Uri child = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    childDirectories.add(child);
                } else if (name != null && isSupportedPhoto(name)
                        && discoveredPhotos.add(child.toString())) {
                    long lastModified = modifiedColumn >= 0 && !cursor.isNull(modifiedColumn)
                            ? cursor.getLong(modifiedColumn) : 0L;
                    PhotoSource source = PhotoSource.fromUri(child, name, lastModified);
                    output.add(source);
                    discovery.onPhotoDiscovered(source);
                }
            }
            cursor.close();
            cursor = null;
            for (Uri childDirectory : childDirectories) {
                if (scanGeneration != photoGeneration
                        || discoveredPhotos.size() >= photoFileLimit) return;
                collectDocumentDirectoryPhotos(treeUri, childDirectory, visitedDirectories,
                        discoveredPhotos, output, discovery, inaccessibleTree,
                        depth + 1, scanGeneration);
            }
        } catch (SecurityException error) {
            inaccessibleTree[0] = true;
        } catch (Exception ignored) {
            // Continue with other selected sources.
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception ignored) {
            return file.getAbsolutePath();
        }
    }

    private boolean isSupportedPhoto(String name) {
        String lower = name.toLowerCase(Locale.US);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png");
    }

    private void loadNextPhoto() {
        if (photoLoading || photoFiles.isEmpty()) {
            return;
        }
        loadPhoto(playbackNavigator.next());
    }

    private void loadPreviousPhoto() {
        if (photoLoading || photoFiles.isEmpty()) {
            return;
        }
        int previous = playbackNavigator.previous();
        if (previous == PlaybackNavigator.NO_ITEM) {
            Toast.makeText(this, "目前沒有上一張", Toast.LENGTH_SHORT).show();
            return;
        }
        loadPhoto(previous);
    }

    private void loadPhoto(int index) {
        if (index < 0 || index >= photoFiles.size()) {
            return;
        }
        final PhotoSource source = photoFiles.get(index);
        final int generation = photoGeneration;
        photoLoading = true;
        nextPhotoAt = SystemClock.elapsedRealtime() + photoIntervalMs;
        schedulePhotoTicker();
        photoDecodeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decodePhoto(source);
                int domColor = BACKGROUND;
                float fx = 0.5f;
                float fy = 0.5f;
                if (bitmap != null && (isAdaptiveColorActive() || isSmartFocusActive())) {
                    Bitmap sample = null;
                    int[] pixels = null;
                    try {
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        if (w > 10 && h > 10) {
                            sample = Bitmap.createScaledBitmap(bitmap, 20, 20, false);
                            if (sample != null) {
                                pixels = new int[400];
                                sample.getPixels(pixels, 0, 20, 0, 0, 20, 20);
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        if (sample != null && sample != bitmap && !sample.isRecycled()) {
                            safeRecycle(sample);
                        }
                    }
                    if (pixels != null) {
                        if (isAdaptiveColorActive()) {
                            long rSum = 0, gSum = 0, bSum = 0;
                            for (int px : pixels) {
                                rSum += (px >> 16) & 0xff;
                                gSum += (px >> 8) & 0xff;
                                bSum += px & 0xff;
                            }
                            domColor = Color.rgb((int) (rSum / 400), (int) (gSum / 400), (int) (bSum / 400));
                        }
                        if (isSmartFocusActive()) {
                            long sumX = 0, sumY = 0, totalW = 0;
                            for (int y = 0; y < 20; y++) {
                                for (int x = 0; x < 20; x++) {
                                    int px = pixels[y * 20 + x];
                                    int brightness = (((px >> 16) & 0xff) * 299
                                            + ((px >> 8) & 0xff) * 587
                                            + (px & 0xff) * 114) / 1000;
                                    int weight = Math.abs(brightness - 128);
                                    sumX += (long) x * weight;
                                    sumY += (long) y * weight;
                                    totalW += weight;
                                }
                            }
                            if (totalW > 0) {
                                fx = Math.max(0.2f, Math.min(0.8f, (float) (sumX / totalW) / 20.0f));
                                fy = Math.max(0.2f, Math.min(0.8f, (float) (sumY / totalW) / 20.0f));
                            }
                        }
                    }
                }
                final int finalDomColor = domColor;
                final float finalFx = fx;
                final float finalFy = fy;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != photoGeneration || !activityResumed) {
                            if (bitmap != null && !bitmap.isRecycled()) {
                                safeRecycle(bitmap);
                            }
                            return;
                        }
                        photoLoading = false;
                        if (bitmap == null) {
                            photoFailures++;
                            if (photoFailures >= photoFiles.size()) {
                                photoFiles.clear();
                                showPhotoStatus("相簿中的圖片都無法讀取", WARNING);
                            } else {
                                loadNextPhoto();
                            }
                            return;
                        }
                        photoFailures = 0;
                        currentPhotoSource = source;
                        currentDominantColor = finalDomColor;
                        focalX = finalFx;
                        focalY = finalFy;
                        updateClockStyle();
                        displayPhoto(bitmap);
                    }
                });
            }
        });
    }

    private Bitmap decodePhoto(PhotoSource source) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        decodeBitmap(source, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int targetWidth = getResources().getDisplayMetrics().widthPixels;
        int targetHeight = getResources().getDisplayMetrics().heightPixels;

        // 限制解碼像素，避免高解析度照片耗盡記憶體。
        long maxPixels = Math.max(2500000L, (long) targetWidth * targetHeight * 3L / 2L);

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetWidth
                && bounds.outHeight / (sample * 2) >= targetHeight) {
            sample *= 2;
        }

        while ((long) (bounds.outWidth / sample) * (bounds.outHeight / sample) > maxPixels) {
            sample *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = isLowPowerModeActive()
                ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
        options.inDither = isLowPowerModeActive();
        Bitmap bmp = null;
        try {
            bmp = decodeBitmap(source, options);
        } catch (OutOfMemoryError ignored) {
            // 降低解析度後重試。
            options.inSampleSize = sample * 2;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            try {
                bmp = decodeBitmap(source, options);
            } catch (OutOfMemoryError ignoredAgain) {
                return null;
            }
        }
        if (bmp == null) return null;

        int rotationAngle = readRotationAngle(source);
        if (rotationAngle == 0) {
            return bmp;
        }
        try {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(rotationAngle);
            Bitmap rotated = Bitmap.createBitmap(
                    bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            if (rotated != bmp) {
                safeRecycle(bmp);
                bmp = rotated;
            }
        } catch (Throwable ignored) {
        }
        return bmp;
    }

    private Bitmap decodeBitmap(PhotoSource source, BitmapFactory.Options options) {
        InputStream input = null;
        try {
            input = openPhotoInputStream(source);
            return input == null ? null : BitmapFactory.decodeStream(input, null, options);
        } catch (Exception ignored) {
            return null;
        } finally {
            closeQuietly(input);
        }
    }

    private InputStream openPhotoInputStream(PhotoSource source) throws Exception {
        if (source.file != null) return new FileInputStream(source.file);
        return source.uri == null ? null : getContentResolver().openInputStream(source.uri);
    }

    private int readRotationAngle(PhotoSource source) {
        InputStream input = null;
        try {
            androidx.exifinterface.media.ExifInterface exif;
            if (source.uri != null) {
                input = getContentResolver().openInputStream(source.uri);
                if (input == null) {
                    return 0;
                }
                exif = new androidx.exifinterface.media.ExifInterface(input);
            } else if (source.file != null) {
                exif = new androidx.exifinterface.media.ExifInterface(
                        source.file.getAbsolutePath());
            } else {
                return 0;
            }
            int orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
            if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {
        } finally {
            closeQuietly(input);
        }
        return 0;
    }

    private void closeQuietly(InputStream input) {
        if (input == null) {
            return;
        }
        try {
            input.close();
        } catch (Exception ignored) {
        }
    }

    private void displayPhoto(final Bitmap bitmap) {
        photoStatus.setVisibility(View.GONE);
        updateShowcaseColorOverlay();
        if (isNightSleepActive) {
            if (photoBitmap != null && photoBitmap != bitmap && !photoBitmap.isRecycled()) {
                safeRecycle(photoBitmap);
            }
            photoBitmap = bitmap;
            photoImage.setImageBitmap(bitmap);
            applyPhotoPresentation(bitmap);
            photoImage.setAlpha(0.0f);
            if (backgroundImage != null) backgroundImage.setAlpha(0.0f);
            return;
        }

        if (photoBitmap == null) {
            photoBitmap = bitmap;
            photoImage.setImageBitmap(bitmap);
            applyPhotoPresentation(bitmap);
            startPhotoPan();
            photoImage.setAlpha(0.0f);
            photoImage.animate().alpha(1.0f).setDuration(1800).setInterpolator(smoothInterpolator).start();
            return;
        }

        if (pendingPhotoBitmap != null
                && pendingPhotoBitmap != bitmap
                && !pendingPhotoBitmap.isRecycled()) {
            safeRecycle(pendingPhotoBitmap);
        }
        pendingPhotoBitmap = bitmap;
        final int generation = photoGeneration;
        stopPhotoPan();
        photoImage.animate().cancel();

        Runnable swapRunnable = new Runnable() {
            @Override
            public void run() {
                if (generation != photoGeneration || !activityResumed) {
                    if (pendingPhotoBitmap == bitmap) {
                        pendingPhotoBitmap = null;
                    }
                    if (!bitmap.isRecycled()) {
                        safeRecycle(bitmap);
                    }
                    return;
                }

                Bitmap previous = photoBitmap;
                photoBitmap = bitmap;
                pendingPhotoBitmap = null;

                photoImage.setImageBitmap(bitmap);
                applyPhotoPresentation(bitmap);
                startPhotoPan();

                if (previous != null && previous != bitmap && !previous.isRecycled()) {
                    safeRecycle(previous);
                }

                resetTransformations();
                animateIn(transitionType);
            }
        };

        animateOut(transitionType, swapRunnable);
    }

    private void resetTransformations() {
        photoImage.setTranslationX(0.0f);
        photoImage.setTranslationY(0.0f);
        photoImage.setScaleX(1.0f);
        photoImage.setScaleY(1.0f);
        photoImage.setRotation(0.0f);
        photoImage.setRotationY(0.0f);
    }

    private android.view.ViewPropertyAnimator photoTransitionAnimator() {
        android.view.ViewPropertyAnimator animator = photoImage.animate();
        if (isShowcaseModeActive()) animator.withLayer();
        return animator;
    }

    private void animateOut(int type, Runnable endAction) {
        switch (type) {
            case 1:
                photoTransitionAnimator().translationX(-photoImage.getWidth() * 0.35f)
                        .alpha(0.0f).setDuration(1400)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 2:
                photoTransitionAnimator().translationY(-photoImage.getHeight() * 0.35f)
                        .alpha(0.0f).setDuration(1400)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 3:
                photoTransitionAnimator().scaleX(0.92f).scaleY(0.92f)
                        .alpha(0.0f).setDuration(1300)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 4:
                photoTransitionAnimator().rotationY(90f)
                        .alpha(0.0f).setDuration(1100)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 5:
                photoTransitionAnimator().rotation(-5f)
                        .alpha(0.0f).setDuration(1400)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 0:
            default:
                photoTransitionAnimator().alpha(0.0f).setDuration(1500)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
        }
    }

    private void animateIn(int type) {
        switch (type) {
            case 1:
                photoImage.setTranslationX(photoImage.getWidth() * 0.35f);
                photoImage.setAlpha(0.0f);
                photoTransitionAnimator().translationX(0.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 2:
                photoImage.setTranslationY(photoImage.getHeight() * 0.35f);
                photoImage.setAlpha(0.0f);
                photoTransitionAnimator().translationY(0.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 3:
                photoImage.setScaleX(1.06f);
                photoImage.setScaleY(1.06f);
                photoImage.setAlpha(0.0f);
                photoTransitionAnimator().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 4:
                photoImage.setRotationY(-90f);
                photoImage.setAlpha(0.0f);
                photoTransitionAnimator().rotationY(0.0f).alpha(1.0f).setDuration(1600)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 5:
                photoImage.setRotation(5f);
                photoImage.setAlpha(0.0f);
                photoTransitionAnimator().rotation(0.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 0:
            default:
                photoImage.setAlpha(0.0f);
                photoTransitionAnimator().alpha(1.0f).setDuration(2200)
                        .setInterpolator(smoothInterpolator).start();
                break;
        }
    }

    private void showPhotoStatus(String message, int color) {
        photoStatus.setText(message);
        photoStatus.setTextColor(color);
        photoStatus.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
