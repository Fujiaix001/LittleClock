package com.quietphoto.clock;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
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
    private static final long PHOTO_PAN_FRAME_MS = 67;
    private static final float PHOTO_PAN_TRAVEL_FRACTION = 0.20f;
    private static final String CLOCK_POS_X_RATIO = "clock_pos_x_ratio";
    private static final String CLOCK_POS_Y_RATIO = "clock_pos_y_ratio";
    public static final String CLOCK_SCALE_FACTOR = "clock_scale_factor";
    private static final String CLOCK_TIME_SCALE_FACTOR = "clock_time_scale_factor";
    private static final String CLOCK_DATE_SCALE_FACTOR = "clock_date_scale_factor";
    private static final String CLOCK_WEATHER_SCALE_FACTOR = "clock_weather_scale_factor";
    private static final String CLOCK_FREE_TIME_X_RATIO = "clock_free_time_x_ratio";
    private static final String CLOCK_FREE_TIME_Y_RATIO = "clock_free_time_y_ratio";
    private static final String CLOCK_FREE_DATE_X_RATIO = "clock_free_date_x_ratio";
    private static final String CLOCK_FREE_DATE_Y_RATIO = "clock_free_date_y_ratio";
    private static final String CLOCK_FREE_WEATHER_X_RATIO = "clock_free_weather_x_ratio";
    private static final String CLOCK_FREE_WEATHER_Y_RATIO = "clock_free_weather_y_ratio";
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
    private static final long BURN_IN_INTERVAL_MS = 180000L;
    private static final long MEDIA_REFRESH_DELAY_MS = 2000L;
    private static final long WEATHER_FRESH_NORMAL_MS = 60L * 60L * 1000L;
    private static final long WEATHER_FRESH_LOW_POWER_MS = 120L * 60L * 1000L;
    private static final long WEATHER_MAX_AGE_MS = 6L * 60L * 60L * 1000L;
    private static final long FOCUS_REMINDER_DURATION_MS = 3000L;
    private static final long SAF_RESCAN_NORMAL_MS = 60L * 60L * 1000L;
    private static final long SAF_RESCAN_LOW_POWER_MS = 2L * 60L * 60L * 1000L;
    private static final int WALLPAPER_PACK_VERSION = 4;
    private static final String WALLPAPER_PACK_VERSION_KEY = "wallpaper_pack_version";
    private static final String GESTURE_HINT_SEEN_KEY = "gesture_hint_seen_v1";

    private FrameLayout rootContainer;
    private FrameLayout polaroidContainer;
    private ImageView backgroundImage;
    private ImageView photoImage;
    private TextView photoStatus;
    private AccessibleLinearLayout clockPanel;
    private TextView photoTime;
    private TextView photoDate;
    private LinearLayout dateRow;
    private LinearLayout compactWeatherRow;
    private WeatherIconView compactWeatherIcon;
    private TextView compactWeatherTemperature;
    private LinearLayout weatherRow;
    private WeatherIconView weatherIcon;
    private TextView weatherTemperature;
    private TextView weatherLocation;
    private LinearLayout alarmRow;
    private AlarmIconView alarmIcon;
    private TextView alarmTimeText;
    private LinearLayout pomodoroRow;
    private TextView pomodoroLabel;
    private TextView pomodoroText;
    private FrameLayout pomodoroFocusPanel;
    private Button settingsButton;
    private Button pomodoroButton;
    private StateListDrawable quickActionBackground;
    private FrameLayout focusReminderOverlay;
    private ImageView focusReminderImage;
    private TextView focusReminderMessage;
    private TextView gestureHint;
    private AlertDialog pomodoroDialog;
    private TextView pomodoroDialogStatus;
    private Button pomodoroStartPauseButton;

    private Bitmap photoBitmap;
    private Bitmap pendingPhotoBitmap;
    private Bitmap softBackgroundBitmap;
    private final List<PhotoSource> photoFiles = new ArrayList<PhotoSource>();
    private final PlaybackNavigator playbackNavigator = new PlaybackNavigator();
    private final Handler photoHandler = new Handler();
    private final ExecutorService photoScanExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService photoDecodeExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
    private final Matrix photoMatrix = new Matrix();
    private final Date nowDate = new Date();
    private final AccelerateDecelerateInterpolator smoothInterpolator = new AccelerateDecelerateInterpolator();
    private SharedPreferences prefs;
    private boolean clockTimeEnabled = true;
    private boolean clockDateEnabled = true;
    private boolean pomodoroModeLayoutActive;
    private boolean pomodoroSettingsLocked;
    private long lastAlarmFiredMinute = -1;
    private PhotoSource currentPhotoSource;

    private final SimpleDateFormat photoTimeFormat =
            new SimpleDateFormat("HH:mm", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormat =
            new SimpleDateFormat("M月d日 EEEE", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormatEn =
            new SimpleDateFormat("EEE, MMM d", Locale.US);

    private int photoFailures;
    private volatile int photoGeneration;
    private boolean photoLoading;
    private boolean photoScanInProgress;
    private boolean photoCatalogLoaded;
    private String photoFolderSignature = "";
    private boolean photoPanReverse = true;
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
    private boolean lowPowerMode = true;
    private boolean burnInEnabled = true;
    private boolean autoBrightnessEnabled;
    private boolean favoritesOnly;
    private int photoDisplayMode;
    private boolean weatherEnabled;
    private boolean weatherShowLocation;
    private boolean weatherCompactMode = true;
    private String weatherLocationName = "";
    private double weatherLatitude = Double.NaN;
    private double weatherLongitude = Double.NaN;
    private boolean weatherFetchInFlight;
    private long weatherLastAttemptAt;
    private final Set<String> favoritePhotos = new HashSet<String>();
    private final Set<String> hiddenPhotos = new HashSet<String>();

    private int currentDominantColor = BACKGROUND;
    private float focalX = 0.5f;
    private float focalY = 0.5f;

    private long photoPanStartedAt;
    private long nextPhotoAt;

    private boolean isDraggingClock;
    private boolean isScalingClock;
    private long lastClockDragEndTime;
    private long lastPhotoSwipeEndTime;
    private float touchDownRawX;
    private float touchDownRawY;
    private float clockStartTransX;
    private float clockStartTransY;
    private float clockScaleFactor = 1.0f;
    // These are always effective on-screen scales.  Linked mode changes all three together.
    private float timeScaleFactor = 1.0f;
    private float dateScaleFactor = 1.0f;
    private float weatherScaleFactor = 1.0f;
    private boolean clockSizesLinked = true;
    private boolean clockFreeLayoutEnabled;
    private int activeScaleTarget = SCALE_TARGET_NONE;
    private int freeClockTouchTarget = SCALE_TARGET_NONE;
    private boolean freeClockLongPressPending;
    private boolean freeClockDragging;
    private float freeClockDownRawX;
    private float freeClockDownRawY;
    private float freeClockDragStartX;
    private float freeClockDragStartY;
    private float timeFreeTranslationX;
    private float timeFreeTranslationY;
    private float dateFreeTranslationX;
    private float dateFreeTranslationY;
    private float weatherFreeTranslationX;
    private float weatherFreeTranslationY;
    private float clockBaseTranslationX;
    private float clockBaseTranslationY;
    private float burnInOffsetX;
    private float burnInOffsetY;
    // Reused on the UI thread so drag hit-testing and boundary clamping stay allocation-free.
    private final float[] clockBoundsScratch = new float[4];
    private final float[] previousClockBoundsScratch = new float[4];
    private final float[] clockChildBoundsScratch = new float[4];
    private final float[] dateRowBoundsScratch = new float[4];
    private final float[] scaleFocusScratch = new float[2];
    private final int[] rootScreenLocationScratch = new int[2];
    private float motionEventScreenOffsetX;
    private float motionEventScreenOffsetY;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector photoGestureDetector;
    private boolean photoActionLongPressPending;
    private float photoActionDownX;
    private float photoActionDownY;
    private final Runnable photoActionLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!photoActionLongPressPending || pomodoroModeLayoutActive || photoLoading) return;
            photoActionLongPressPending = false;
            showPhotoActions();
        }
    };
    private final Runnable freeClockLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!freeClockLongPressPending || !clockFreeLayoutEnabled
                    || freeClockTouchTarget == SCALE_TARGET_NONE || isScalingClock) {
                return;
            }
            freeClockLongPressPending = false;
            freeClockDragging = true;
            setFreeClockTargetAlpha(freeClockTouchTarget, 0.75f);
        }
    };
    private final Random random = new Random();
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private float lastLightLevel = -1.0f;
    private ContentObserver mediaObserver;
    private boolean mediaObserverRegistered;

    private static final class PhotoSource {
        final File file;
        final Uri uri;
        final String identity;

        private PhotoSource(File file, Uri uri, String identity) {
            this.file = file;
            this.uri = uri;
            this.identity = identity;
        }

        static PhotoSource fromFile(File file, String identity) {
            return new PhotoSource(file, null, identity);
        }

        static PhotoSource fromUri(Uri uri) {
            return new PhotoSource(null, uri, uri.toString());
        }

        String key() {
            return identity;
        }
    }

    private interface PhotoDiscovery {
        void onPhotoDiscovered(PhotoSource source);
    }

    private static final class AccessibleLinearLayout extends LinearLayout {
        AccessibleLinearLayout(android.content.Context context) {
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
            if (!isNightSleepActive && !photoLoading && !photoFiles.isEmpty()
                    && SystemClock.elapsedRealtime() >= nextPhotoAt) {
                loadNextPhoto();
            }
            schedulePhotoTicker();
        }
    };

    private final Runnable hideGestureHintRunnable = new Runnable() {
        @Override
        public void run() {
            if (gestureHint != null) gestureHint.setVisibility(View.GONE);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(buildInterface());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        setupPhotoGestures();
        createMediaObserver();
        extractDefaultWallpapers();
        AlarmHelper.updateAlarmSchedule(this);
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
            float orientationScale = prefs.getFloat(scaleKey, legacyScale);
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
            if (!prefs.contains(CLOCK_TIME_SCALE_FACTOR + orientation)) {
                editor.putFloat(CLOCK_TIME_SCALE_FACTOR + orientation, orientationScale);
                changed = true;
            }
            if (!prefs.contains(CLOCK_DATE_SCALE_FACTOR + orientation)) {
                editor.putFloat(CLOCK_DATE_SCALE_FACTOR + orientation, orientationScale);
                changed = true;
            }
            if (!prefs.contains(CLOCK_WEATHER_SCALE_FACTOR + orientation)) {
                editor.putFloat(CLOCK_WEATHER_SCALE_FACTOR + orientation, orientationScale);
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
        startPhotoSlideshow();
        if (hasPhotoReadAccess()) {
            registerMediaObserver();
        }
        registerLightSensor();
        scheduleBurnIn();
        scheduleWeatherRefresh();
        scheduleSafCatalogRefresh();
        showGestureHintIfNeeded();
        updateAlarmIndicator();
        updatePomodoroDisplay();
        rootContainer.post(new Runnable() {
            @Override
            public void run() {
                restoreClockPosition();
                restoreFreeClockTranslations();
            }
        });
    }

    @Override
    protected void onPause() {
        cancelPhotoActionLongPress();
        cancelFreeClockInteraction(false);
        if (currentPhotoSource != null) {
            prefs.edit().putString(LAST_PHOTO_KEY, currentPhotoSource.key()).apply();
        }
        activityResumed = false;
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.removeCallbacks(hideGestureHintRunnable);
        photoHandler.removeCallbacks(burnInRunnable);
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
        stopPhotoSlideshow();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cancelPhotoActionLongPress();
        cancelFreeClockInteraction(false);
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.removeCallbacks(hideGestureHintRunnable);
        photoHandler.removeCallbacks(burnInRunnable);
        photoHandler.removeCallbacks(mediaRefreshRunnable);
        photoHandler.removeCallbacks(weatherRefreshRunnable);
        photoHandler.removeCallbacks(safCatalogRefreshRunnable);
        photoHandler.removeCallbacks(pomodoroDialogTicker);
        photoHandler.removeCallbacks(hideFocusReminderRunnable);
        unregisterMediaObserver();
        unregisterLightSensor();
        stopPhotoSlideshow();
        photoScanExecutor.shutdownNow();
        photoDecodeExecutor.shutdownNow();
        weatherExecutor.shutdownNow();
        super.onDestroy();
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
        loadClockScalePreferences();
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
                        restoreFreeClockTranslations();
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

    private void handlePhotoActionLongPress(MotionEvent event) {
        if (event == null) return;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!isTouchOnClock(event) && !pomodoroModeLayoutActive && !photoLoading) {
                photoActionDownX = event.getRawX();
                photoActionDownY = event.getRawY();
                photoActionLongPressPending = true;
                photoHandler.removeCallbacks(photoActionLongPressRunnable);
                photoHandler.postDelayed(photoActionLongPressRunnable, PHOTO_ACTION_LONG_PRESS_MS);
            }
            return;
        }

        if (!photoActionLongPressPending) return;
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
    }

    private void cancelPhotoActionLongPress() {
        photoActionLongPressPending = false;
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
        handlePhotoActionLongPress(event);
        if (event != null) {
            // ScaleGestureDetector reports window coordinates; keep the exact window-to-screen
            // offset from this event instead of inferring it from a transformed root view.
            motionEventScreenOffsetX = event.getRawX() - event.getX();
            motionEventScreenOffsetY = event.getRawY() - event.getY();
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                cancelClockDragForMultiTouch();
            }
        }
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }
        if (clockFreeLayoutEnabled && handleFreeClockTouch(event)) {
            return true;
        }
        if (photoGestureDetector != null && event.getPointerCount() == 1
                && !isDraggingClock && !isScalingClock) {
            photoGestureDetector.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
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
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return x >= location[0] && x <= location[0] + view.getWidth()
                && y >= location[1] && y <= location[1] + view.getHeight();
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
        photoHandler.removeCallbacks(hideGestureHintRunnable);
        if (gestureHint != null) gestureHint.setVisibility(View.GONE);
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
        if (clockPanel == null || event == null) {
            return false;
        }
        if (clockFreeLayoutEnabled) {
            return findScaleTarget(event.getRawX(), event.getRawY()) != SCALE_TARGET_NONE;
        }
        return isPointOnClock(event.getRawX(), event.getRawY());
    }

    private boolean isPointOnClock(float rawX, float rawY) {
        if (!getClockVisualBounds(clockBoundsScratch) || rootContainer == null) return false;
        rootContainer.getLocationOnScreen(rootScreenLocationScratch);
        return rawX >= rootScreenLocationScratch[0] + clockBoundsScratch[0]
                && rawX <= rootScreenLocationScratch[0] + clockBoundsScratch[2]
                && rawY >= rootScreenLocationScratch[1] + clockBoundsScratch[1]
                && rawY <= rootScreenLocationScratch[1] + clockBoundsScratch[3];
    }

    private float[] getScaleFocusOnScreen(ScaleGestureDetector detector) {
        scaleFocusScratch[0] = motionEventScreenOffsetX + detector.getFocusX();
        scaleFocusScratch[1] = motionEventScreenOffsetY + detector.getFocusY();
        return scaleFocusScratch;
    }

    private int findScaleTarget(float rawX, float rawY) {
        if (clockFreeLayoutEnabled) {
            // Match the LinearLayout drawing order. When units overlap, the visible
            // top unit is selected first and can be dragged away to reveal the next.
            if (isPointOnClockChild(weatherRow, rawX, rawY)) return SCALE_TARGET_WEATHER;
            if (isPointOnClockChild(dateRow, rawX, rawY)) return SCALE_TARGET_DATE;
            if (isPointOnClockChild(photoTime, rawX, rawY)) return SCALE_TARGET_TIME;
            return SCALE_TARGET_NONE;
        }
        if (isPointOnClockChild(photoTime, rawX, rawY)) return SCALE_TARGET_TIME;
        if (isPointOnClockNestedChild(compactWeatherRow, rawX, rawY)) return SCALE_TARGET_WEATHER;
        if (isPointOnClockNestedChild(photoDate, rawX, rawY)) return SCALE_TARGET_DATE;
        if (isPointOnClockChild(weatherRow, rawX, rawY)) return SCALE_TARGET_WEATHER;
        return SCALE_TARGET_NONE;
    }

    /**
     * Android 4.2 can report a scale focus a few pixels away from a transformed child.
     * Prefer the closest visible clock part instead of falling through to whole-clock drag.
     */
    private int findNearestScaleTarget(float rawX, float rawY) {
        if (rootContainer == null) return SCALE_TARGET_NONE;
        rootContainer.getLocationOnScreen(rootScreenLocationScratch);
        float bestDistance = Float.MAX_VALUE;
        int target = SCALE_TARGET_NONE;

        if (getClockChildVisualBounds(photoTime, clockBoundsScratch)) {
            bestDistance = distanceSquaredToScreenBounds(rawX, rawY);
            target = SCALE_TARGET_TIME;
        }
        boolean hasDateBounds = clockFreeLayoutEnabled
                ? getClockChildVisualBounds(dateRow, clockBoundsScratch)
                : getClockNestedChildVisualBounds(photoDate, clockBoundsScratch);
        if (hasDateBounds) {
            float distance = distanceSquaredToScreenBounds(rawX, rawY);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = SCALE_TARGET_DATE;
            }
        }
        if (!clockFreeLayoutEnabled
                && getClockNestedChildVisualBounds(compactWeatherRow, clockBoundsScratch)) {
            float distance = distanceSquaredToScreenBounds(rawX, rawY);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = SCALE_TARGET_WEATHER;
            }
        }
        if (getClockChildVisualBounds(weatherRow, clockBoundsScratch)) {
            float distance = distanceSquaredToScreenBounds(rawX, rawY);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = SCALE_TARGET_WEATHER;
            }
        }
        float maximumDistance = dp(48);
        return bestDistance <= maximumDistance * maximumDistance ? target : SCALE_TARGET_NONE;
    }

    private float distanceSquaredToScreenBounds(float rawX, float rawY) {
        float left = rootScreenLocationScratch[0] + clockBoundsScratch[0];
        float top = rootScreenLocationScratch[1] + clockBoundsScratch[1];
        float right = rootScreenLocationScratch[0] + clockBoundsScratch[2];
        float bottom = rootScreenLocationScratch[1] + clockBoundsScratch[3];
        float deltaX = rawX < left ? left - rawX : rawX > right ? rawX - right : 0.0f;
        float deltaY = rawY < top ? top - rawY : rawY > bottom ? rawY - bottom : 0.0f;
        return deltaX * deltaX + deltaY * deltaY;
    }

    private boolean isPointOnClockChild(View child, float rawX, float rawY) {
        if (!getClockChildVisualBounds(child, clockBoundsScratch) || rootContainer == null) return false;
        rootContainer.getLocationOnScreen(rootScreenLocationScratch);
        return rawX >= rootScreenLocationScratch[0] + clockBoundsScratch[0]
                && rawX <= rootScreenLocationScratch[0] + clockBoundsScratch[2]
                && rawY >= rootScreenLocationScratch[1] + clockBoundsScratch[1]
                && rawY <= rootScreenLocationScratch[1] + clockBoundsScratch[3];
    }

    private boolean isPointOnClockNestedChild(View child, float rawX, float rawY) {
        if (!getClockNestedChildVisualBounds(child, clockBoundsScratch) || rootContainer == null) return false;
        rootContainer.getLocationOnScreen(rootScreenLocationScratch);
        return rawX >= rootScreenLocationScratch[0] + clockBoundsScratch[0]
                && rawX <= rootScreenLocationScratch[0] + clockBoundsScratch[2]
                && rawY >= rootScreenLocationScratch[1] + clockBoundsScratch[1]
                && rawY <= rootScreenLocationScratch[1] + clockBoundsScratch[3];
    }

    /** Writes the visual bounds in rootContainer coordinates, including property transforms. */
    private boolean getClockVisualBounds(float[] outBounds) {
        if (clockPanel == null || outBounds == null || outBounds.length < 4) return false;
        boolean hasVisibleChild = false;
        float left = Float.MAX_VALUE;
        float top = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        for (int i = 0; i < clockPanel.getChildCount(); i++) {
            if (!getClockChildVisualBounds(clockPanel.getChildAt(i), clockChildBoundsScratch)) continue;
            hasVisibleChild = true;
            left = Math.min(left, clockChildBoundsScratch[0]);
            top = Math.min(top, clockChildBoundsScratch[1]);
            right = Math.max(right, clockChildBoundsScratch[2]);
            bottom = Math.max(bottom, clockChildBoundsScratch[3]);
        }
        if (!hasVisibleChild) return false;
        outBounds[0] = left;
        outBounds[1] = top;
        outBounds[2] = right;
        outBounds[3] = bottom;
        return true;
    }

    private boolean getClockChildVisualBounds(View child, float[] outBounds) {
        if (clockPanel == null || child == null || child.getVisibility() == View.GONE
                || outBounds == null || outBounds.length < 4) {
            return false;
        }
        if (child == dateRow) {
            if (!getDateRowLocalVisualBounds(dateRowBoundsScratch)) return false;
            float rowScaleX = dateRow.getScaleX();
            float rowScaleY = dateRow.getScaleY();
            float rowLeft = dateRow.getLeft() + dateRow.getTranslationX()
                    + dateRow.getPivotX() * (1.0f - rowScaleX) + dateRowBoundsScratch[0] * rowScaleX;
            float rowTop = dateRow.getTop() + dateRow.getTranslationY()
                    + dateRow.getPivotY() * (1.0f - rowScaleY) + dateRowBoundsScratch[1] * rowScaleY;
            float rowRight = dateRow.getLeft() + dateRow.getTranslationX()
                    + dateRow.getPivotX() * (1.0f - rowScaleX) + dateRowBoundsScratch[2] * rowScaleX;
            float rowBottom = dateRow.getTop() + dateRow.getTranslationY()
                    + dateRow.getPivotY() * (1.0f - rowScaleY) + dateRowBoundsScratch[3] * rowScaleY;
            mapClockPanelBounds(rowLeft, rowTop, rowRight, rowBottom, outBounds);
            return true;
        }
        float childScaleX = child.getScaleX();
        float childScaleY = child.getScaleY();
        float childLeft = child.getLeft() + child.getTranslationX()
                + child.getPivotX() * (1.0f - childScaleX);
        float childTop = child.getTop() + child.getTranslationY()
                + child.getPivotY() * (1.0f - childScaleY);
        float childRight = childLeft + child.getWidth() * childScaleX;
        float childBottom = childTop + child.getHeight() * childScaleY;
        mapClockPanelBounds(childLeft, childTop, childRight, childBottom, outBounds);
        return true;
    }

    private boolean getClockNestedChildVisualBounds(View child, float[] outBounds) {
        if (dateRow == null || child == null || child.getParent() != dateRow
                || child.getVisibility() == View.GONE || outBounds == null || outBounds.length < 4) {
            return false;
        }
        float scaleX = child.getScaleX();
        float scaleY = child.getScaleY();
        float rowScaleX = dateRow.getScaleX();
        float rowScaleY = dateRow.getScaleY();
        float childLeft = child.getLeft() + child.getTranslationX()
                + child.getPivotX() * (1.0f - scaleX);
        float childTop = child.getTop() + child.getTranslationY()
                + child.getPivotY() * (1.0f - scaleY);
        float rowLeft = dateRow.getLeft() + dateRow.getTranslationX()
                + dateRow.getPivotX() * (1.0f - rowScaleX) + childLeft * rowScaleX;
        float rowTop = dateRow.getTop() + dateRow.getTranslationY()
                + dateRow.getPivotY() * (1.0f - rowScaleY) + childTop * rowScaleY;
        mapClockPanelBounds(rowLeft, rowTop,
                rowLeft + child.getWidth() * scaleX * rowScaleX,
                rowTop + child.getHeight() * scaleY * rowScaleY, outBounds);
        return true;
    }

    private boolean getDateRowLocalVisualBounds(float[] outBounds) {
        if (dateRow == null || outBounds == null || outBounds.length < 4) return false;
        boolean hasVisibleChild = false;
        float left = Float.MAX_VALUE;
        float top = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        for (int i = 0; i < dateRow.getChildCount(); i++) {
            View child = dateRow.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            float scaleX = child.getScaleX();
            float scaleY = child.getScaleY();
            float childLeft = child.getLeft() + child.getTranslationX()
                    + child.getPivotX() * (1.0f - scaleX);
            float childTop = child.getTop() + child.getTranslationY()
                    + child.getPivotY() * (1.0f - scaleY);
            hasVisibleChild = true;
            left = Math.min(left, childLeft);
            top = Math.min(top, childTop);
            right = Math.max(right, childLeft + child.getWidth() * scaleX);
            bottom = Math.max(bottom, childTop + child.getHeight() * scaleY);
        }
        if (!hasVisibleChild) return false;
        outBounds[0] = left;
        outBounds[1] = top;
        outBounds[2] = right;
        outBounds[3] = bottom;
        return true;
    }

    private void mapClockPanelBounds(float childLeft, float childTop, float childRight,
            float childBottom, float[] outBounds) {
        float panelOriginX = clockPanel.getLeft() + clockPanel.getTranslationX();
        float panelOriginY = clockPanel.getTop() + clockPanel.getTranslationY();
        float panelPivotX = clockPanel.getPivotX();
        float panelPivotY = clockPanel.getPivotY();
        float panelScaleX = clockPanel.getScaleX();
        float panelScaleY = clockPanel.getScaleY();
        outBounds[0] = panelOriginX + panelPivotX
                + (childLeft - panelPivotX) * panelScaleX;
        outBounds[1] = panelOriginY + panelPivotY
                + (childTop - panelPivotY) * panelScaleY;
        outBounds[2] = panelOriginX + panelPivotX
                + (childRight - panelPivotX) * panelScaleX;
        outBounds[3] = panelOriginY + panelPivotY
                + (childBottom - panelPivotY) * panelScaleY;
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
        lowPowerMode = prefs.getBoolean(SettingsActivity.LOW_POWER_MODE, true);
        burnInEnabled = prefs.getBoolean(SettingsActivity.BURN_IN_ENABLED, true);
        autoBrightnessEnabled = prefs.getBoolean(SettingsActivity.AUTO_BRIGHTNESS_ENABLED, false);
        favoritesOnly = prefs.getBoolean(SettingsActivity.FAVORITES_ONLY, false);
        photoDisplayMode = Math.max(0, Math.min(2,
                prefs.getInt(SettingsActivity.PHOTO_DISPLAY_MODE, 0)));
        weatherEnabled = prefs.getBoolean(SettingsActivity.WEATHER_ENABLED, false);
        weatherShowLocation = prefs.getBoolean(SettingsActivity.WEATHER_SHOW_LOCATION, false);
        weatherCompactMode = prefs.getBoolean(SettingsActivity.WEATHER_COMPACT_MODE, true);
        weatherLocationName = prefs.getString(SettingsActivity.WEATHER_LOCATION_NAME, "");
        weatherLatitude = parseDouble(prefs.getString(SettingsActivity.WEATHER_LATITUDE, null));
        weatherLongitude = parseDouble(prefs.getString(SettingsActivity.WEATHER_LONGITUDE, null));
        loadClockScalePreferences();

        favoritePhotos.clear();
        hiddenPhotos.clear();
        Set<String> savedFavorites = prefs.getStringSet(SettingsActivity.FAVORITE_PHOTOS, null);
        Set<String> savedHidden = prefs.getStringSet(SettingsActivity.HIDDEN_PHOTOS, null);
        if (savedFavorites != null) favoritePhotos.addAll(savedFavorites);
        if (savedHidden != null) hiddenPhotos.addAll(savedHidden);

        applyPolaroidStyle();
        updateClockStyle();
        applyClockScale();
    }

    private void loadClockScalePreferences() {
        clockSizesLinked = prefs.getBoolean(SettingsActivity.CLOCK_SIZES_LINKED, true);
        clockFreeLayoutEnabled = prefs.getBoolean(
                SettingsActivity.CLOCK_FREE_LAYOUT_ENABLED, false);
        clockScaleFactor = prefs.getFloat(orientationKey(CLOCK_SCALE_FACTOR), 0.75f);
        timeScaleFactor = prefs.getFloat(
                orientationKey(CLOCK_TIME_SCALE_FACTOR), clockScaleFactor);
        dateScaleFactor = prefs.getFloat(
                orientationKey(CLOCK_DATE_SCALE_FACTOR), clockScaleFactor);
        weatherScaleFactor = prefs.getFloat(
                orientationKey(CLOCK_WEATHER_SCALE_FACTOR), clockScaleFactor);
    }

    private void applyPolaroidStyle() {
        if (polaroidContainer == null) {
            return;
        }
        if (polaroidFrameEnabled && !lowPowerMode) {
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
        if (alarmTimeText != null) alarmTimeText.setTypeface(clockTypeface);
        if (pomodoroLabel != null) pomodoroLabel.setTypeface(clockTypeface);
        if (pomodoroText != null) pomodoroText.setTypeface(clockTypeface);
        updatePhotoClock();

        int bgColor = isAdaptiveColorActive()
                ? Color.argb(85, Color.red(currentDominantColor),
                        Color.green(currentDominantColor), Color.blue(currentDominantColor))
                : Color.argb(65, 0, 0, 0);
        if (clockFreeLayoutEnabled) {
            clockPanel.setBackground(null);
            clockPanel.setPadding(0, 0, 0, 0);
            setFreeUnitBackground(photoTime, clockBgEnabled, bgColor);
            setFreeUnitBackground(dateRow, clockBgEnabled, bgColor);
            setFreeUnitBackground(weatherRow, clockBgEnabled, bgColor);
        } else {
            setFreeUnitBackground(photoTime, false, bgColor);
            setFreeUnitBackground(dateRow, false, bgColor);
            setFreeUnitBackground(weatherRow, false, bgColor);
            if (clockBgEnabled) {
                clockPanel.setBackground(newClockBackground(bgColor));
                clockPanel.setPadding(dp(12), dp(4), dp(12), dp(6));
            } else {
                clockPanel.setBackground(null);
                clockPanel.setPadding(0, 0, 0, 0);
            }
        }
    }

    private GradientDrawable newClockBackground(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(10));
        return background;
    }

    private void setFreeUnitBackground(View unit, boolean enabled, int color) {
        if (unit == null) return;
        unit.setBackground(enabled ? newClockBackground(color) : null);
        int horizontal = enabled ? dp(8) : 0;
        int vertical = enabled ? dp(4) : 0;
        unit.setPadding(horizontal, vertical, horizontal, vertical);
    }

    private View buildInterface() {
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(BACKGROUND);
        rootContainer.setClipChildren(false);
        rootContainer.setClipToPadding(false);
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

        photoStatus = new TextView(this);
        photoStatus.setTextSize(17);
        photoStatus.setTextColor(SECONDARY);
        photoStatus.setGravity(Gravity.CENTER);
        photoStatus.setLineSpacing(0, 1.25f);
        rootContainer.addView(photoStatus, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        clockPanel = new AccessibleLinearLayout(this);
        clockPanel.setOrientation(LinearLayout.VERTICAL);
        clockPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        clockPanel.setClipChildren(false);
        clockPanel.setClipToPadding(false);
        clockPanel.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                applyClockScale();
            }
        });

        photoTime = new TextView(this);
        photoTime.setTextSize(64);
        photoTime.setTextColor(Color.WHITE);
        photoTime.setGravity(Gravity.CENTER_HORIZONTAL);
        photoTime.setTypeface(Typeface.DEFAULT_BOLD);
        photoTime.setIncludeFontPadding(false);
        photoTime.setShadowLayer(dp(3), dp(1), dp(1), Color.BLACK);
        clockPanel.addView(photoTime, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dateRow.setClipChildren(false);
        dateRow.setClipToPadding(false);

        photoDate = new TextView(this);
        photoDate.setTextSize(24);
        photoDate.setTextColor(Color.WHITE);
        photoDate.setGravity(Gravity.CENTER_HORIZONTAL);
        photoDate.setIncludeFontPadding(false);
        photoDate.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
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

        LinearLayout.LayoutParams compactWeatherParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        compactWeatherParams.gravity = Gravity.BOTTOM;
        compactWeatherParams.setMargins(0, 0, dp(9), dp(3));
        dateRow.addView(compactWeatherRow, compactWeatherParams);
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

        pomodoroRow = new LinearLayout(this);
        pomodoroRow.setOrientation(LinearLayout.VERTICAL);
        pomodoroRow.setGravity(Gravity.CENTER);
        pomodoroRow.setVisibility(View.GONE);
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
        LinearLayout.LayoutParams pomodoroParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pomodoroParams.setMargins(0, dp(3), 0, 0);
        clockPanel.addView(pomodoroRow, pomodoroParams);

        clockPanel.addView(dateRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

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
        weatherRowParams.setMargins(0, dp(3), 0, 0);
        clockPanel.addView(weatherRow, weatherRowParams);

        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        clockParams.setMargins(dp(28), dp(28), dp(16), dp(16));
        rootContainer.addView(clockPanel, clockParams);

        pomodoroFocusPanel = new FrameLayout(this);
        pomodoroFocusPanel.setVisibility(View.GONE);
        pomodoroFocusPanel.setClickable(true);
        pomodoroFocusPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumePomodoroFromFocusScreen();
            }
        });
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

        gestureHint = new TextView(this);
        gestureHint.setText("點一下開啟功能　左右滑換照片　長按管理");
        gestureHint.setTextSize(13);
        gestureHint.setTextColor(PRIMARY);
        gestureHint.setGravity(Gravity.CENTER);
        gestureHint.setPadding(dp(14), dp(8), dp(14), dp(8));
        gestureHint.setBackground(rounded(Color.argb(178, 10, 16, 22)));
        gestureHint.setVisibility(View.GONE);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hintParams.setMargins(dp(14), 0, dp(14), dp(24));
        rootContainer.addView(gestureHint, hintParams);

        updatePhotoClock();
        return rootContainer;
    }

    private void showGestureHintIfNeeded() {
        if (gestureHint == null || prefs.getBoolean(GESTURE_HINT_SEEN_KEY, false)) return;
        PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(this);
        if (snapshot.hasSession) return;
        prefs.edit().putBoolean(GESTURE_HINT_SEEN_KEY, true).apply();
        gestureHint.setVisibility(View.VISIBLE);
        photoHandler.removeCallbacks(hideGestureHintRunnable);
        photoHandler.postDelayed(hideGestureHintRunnable, 6500L);
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
        Button end = button("結束番茄", Color.rgb(104, 50, 56));
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

    private void applyClockScale() {
        if (clockPanel == null) return;
        boolean freeTargetChange = clockFreeLayoutEnabled
                && activeScaleTarget != SCALE_TARGET_NONE;
        boolean hadPreviousBounds = freeTargetChange
                ? getFreeClockTargetVisualBounds(
                        activeScaleTarget, previousClockBoundsScratch)
                : captureClockVisualBounds(previousClockBoundsScratch);

        clockPanel.setPivotX(clockPanel.getWidth() / 2f);
        clockPanel.setPivotY(clockPanel.getHeight() / 2f);
        if (pomodoroModeLayoutActive) {
            clockPanel.setScaleX(1.0f);
            clockPanel.setScaleY(1.0f);
            resetClockComponentTransforms();
            invalidateClockVisualChange(hadPreviousBounds);
            return;
        }

        if (clockFreeLayoutEnabled) {
            clockPanel.setScaleX(1.0f);
            clockPanel.setScaleY(1.0f);
            applyComponentScale(photoTime, timeScaleFactor);
            applyComponentScale(dateRow, dateScaleFactor);
            applyComponentScale(weatherRow, weatherScaleFactor);
            applyComponentScale(photoDate, 1.0f);
            applyComponentScale(compactWeatherRow, 1.0f);
            resetDateRowChildTranslations();
            applyFreeClockTranslations();
            if (freeTargetChange) {
                invalidateFreeClockTargetVisualChange(
                        activeScaleTarget, hadPreviousBounds);
            } else {
                invalidateClockVisualChange(hadPreviousBounds);
            }
            return;
        }

        // Preserve the legacy group behavior whenever every row has the same size.
        boolean useWholePanelScale = clockSizesLinked && hasUniformClockScale();
        float panelScale = useWholePanelScale ? timeScaleFactor : 1.0f;
        float timeLocalScale = useWholePanelScale ? 1.0f : timeScaleFactor;
        float dateLocalScale = useWholePanelScale ? 1.0f : dateScaleFactor;
        float weatherLocalScale = useWholePanelScale ? 1.0f : weatherScaleFactor;
        clockPanel.setScaleX(panelScale);
        clockPanel.setScaleY(panelScale);
        applyComponentScale(dateRow, 1.0f);
        applyComponentScale(photoTime, timeLocalScale);
        applyComponentScale(photoDate, dateLocalScale);
        applyComponentScale(compactWeatherRow, weatherLocalScale);
        applyComponentScale(weatherRow, weatherLocalScale);
        resetFreeClockViewTranslations();
        applyDateRowSpacingCompensation();
        applyClockSpacingCompensation();
        invalidateClockVisualChange(hadPreviousBounds);
    }

    private void resetDateRowChildTranslations() {
        if (dateRow == null) return;
        for (int i = 0; i < dateRow.getChildCount(); i++) {
            View child = dateRow.getChildAt(i);
            child.setTranslationX(0.0f);
            child.setTranslationY(0.0f);
        }
    }

    private void resetFreeClockViewTranslations() {
        if (photoTime != null) photoTime.setTranslationX(0.0f);
        if (dateRow != null) dateRow.setTranslationX(0.0f);
        if (weatherRow != null) weatherRow.setTranslationX(0.0f);
    }

    private void applyFreeClockTranslations() {
        if (photoTime != null) {
            photoTime.setTranslationX(timeFreeTranslationX);
            photoTime.setTranslationY(timeFreeTranslationY);
        }
        if (dateRow != null) {
            dateRow.setTranslationX(dateFreeTranslationX);
            dateRow.setTranslationY(dateFreeTranslationY);
        }
        if (weatherRow != null) {
            weatherRow.setTranslationX(weatherFreeTranslationX);
            weatherRow.setTranslationY(weatherFreeTranslationY);
        }
        if (pomodoroRow != null && pomodoroRow.getParent() == clockPanel) {
            pomodoroRow.setTranslationX(0.0f);
            pomodoroRow.setTranslationY(0.0f);
        }
    }

    private boolean captureClockVisualBounds(float[] outBounds) {
        return rootContainer != null && getClockVisualBounds(outBounds);
    }

    /**
     * On Android 4.2, a transformed child that draws outside its layout bounds can leave
     * old pixels behind when only the child is invalidated. Redraw the small union of its
     * old and new visual rectangles instead of invalidating the full photo surface.
     */
    private void invalidateClockVisualChange(boolean hadPreviousBounds) {
        if (rootContainer == null) return;
        boolean hasCurrentBounds = getClockVisualBounds(clockBoundsScratch);
        if (!hadPreviousBounds && !hasCurrentBounds) return;

        float left = hasCurrentBounds ? clockBoundsScratch[0] : previousClockBoundsScratch[0];
        float top = hasCurrentBounds ? clockBoundsScratch[1] : previousClockBoundsScratch[1];
        float right = hasCurrentBounds ? clockBoundsScratch[2] : previousClockBoundsScratch[2];
        float bottom = hasCurrentBounds ? clockBoundsScratch[3] : previousClockBoundsScratch[3];
        if (hadPreviousBounds && hasCurrentBounds) {
            left = Math.min(left, previousClockBoundsScratch[0]);
            top = Math.min(top, previousClockBoundsScratch[1]);
            right = Math.max(right, previousClockBoundsScratch[2]);
            bottom = Math.max(bottom, previousClockBoundsScratch[3]);
        }

        int shadowPadding = dp(24);
        int invalidLeft = Math.max(0, (int) Math.floor(left) - shadowPadding);
        int invalidTop = Math.max(0, (int) Math.floor(top) - shadowPadding);
        int invalidRight = Math.min(rootContainer.getWidth(), (int) Math.ceil(right) + shadowPadding);
        int invalidBottom = Math.min(rootContainer.getHeight(), (int) Math.ceil(bottom) + shadowPadding);
        if (invalidRight > invalidLeft && invalidBottom > invalidTop) {
            rootContainer.invalidate(invalidLeft, invalidTop, invalidRight, invalidBottom);
        }
    }

    private void saveClockScaleFactor() {
        if (prefs != null) {
            // Keep the legacy value in sync for upgrades and for older installations.
            clockScaleFactor = timeScaleFactor;
            prefs.edit()
                    .putFloat(orientationKey(CLOCK_SCALE_FACTOR), clockScaleFactor)
                    .putFloat(orientationKey(CLOCK_TIME_SCALE_FACTOR), timeScaleFactor)
                    .putFloat(orientationKey(CLOCK_DATE_SCALE_FACTOR), dateScaleFactor)
                    .putFloat(orientationKey(CLOCK_WEATHER_SCALE_FACTOR), weatherScaleFactor)
                    .apply();
        }
    }

    private void applyComponentScale(View component, float scale) {
        if (component == null) return;
        component.setPivotX(component.getWidth() / 2f);
        component.setPivotY(component.getHeight() / 2f);
        component.setScaleX(scale);
        component.setScaleY(scale);
    }

    private void resetClockComponentTransforms() {
        if (clockPanel == null) return;
        for (int i = 0; i < clockPanel.getChildCount(); i++) {
            View child = clockPanel.getChildAt(i);
            child.setScaleX(1.0f);
            child.setScaleY(1.0f);
            child.setTranslationY(0.0f);
        }
        if (dateRow != null) {
            for (int i = 0; i < dateRow.getChildCount(); i++) {
                View child = dateRow.getChildAt(i);
                child.setScaleX(1.0f);
                child.setScaleY(1.0f);
                child.setTranslationX(0.0f);
                child.setTranslationY(0.0f);
            }
        }
    }

    /**
     * Property scaling does not change LinearLayout bounds.  Compensate only for the
     * extra visual height, so no layout pass is needed while pinching.
     */
    private void applyClockSpacingCompensation() {
        if (clockPanel == null) return;
        float extraHeight = 0.0f;
        for (int i = 0; i < clockPanel.getChildCount(); i++) {
            View child = clockPanel.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                extraHeight += getClockChildBottomOverflow(child)
                        - getClockChildTopOverflow(child);
            }
        }

        float translationY = 0.0f;
        boolean firstVisibleChild = true;
        for (int i = 0; i < clockPanel.getChildCount(); i++) {
            View child = clockPanel.getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                child.setTranslationY(0.0f);
                continue;
            }
            if (firstVisibleChild) {
                translationY = -extraHeight / 2.0f - getClockChildTopOverflow(child);
                firstVisibleChild = false;
            }
            child.setTranslationY(translationY);
            translationY += getClockChildBottomOverflow(child);
            for (int next = i + 1; next < clockPanel.getChildCount(); next++) {
                View nextChild = clockPanel.getChildAt(next);
                if (nextChild.getVisibility() != View.GONE) {
                    translationY -= getClockChildTopOverflow(nextChild);
                    break;
                }
            }
        }
    }

    private void applyDateRowSpacingCompensation() {
        if (dateRow == null) return;
        float extraWidth = 0.0f;
        for (int i = 0; i < dateRow.getChildCount(); i++) {
            View child = dateRow.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                extraWidth += child.getWidth() * (child.getScaleX() - 1.0f);
            }
        }
        float previousExtra = 0.0f;
        for (int i = 0; i < dateRow.getChildCount(); i++) {
            View child = dateRow.getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                child.setTranslationX(0.0f);
                continue;
            }
            float childExtra = child.getWidth() * (child.getScaleX() - 1.0f);
            child.setTranslationX(-extraWidth / 2.0f + previousExtra + childExtra / 2.0f);
            previousExtra += childExtra;
        }
    }

    private float getClockChildTopOverflow(View child) {
        if (child == dateRow) return getDateRowTopOverflow();
        return child.getPivotY() * (1.0f - child.getScaleY());
    }

    private float getClockChildBottomOverflow(View child) {
        if (child == dateRow) return getDateRowBottomOverflow();
        return getClockChildTopOverflow(child) + child.getHeight() * child.getScaleY()
                - child.getHeight();
    }

    private float getDateRowTopOverflow() {
        if (dateRow == null) return 0.0f;
        float top = 0.0f;
        for (int i = 0; i < dateRow.getChildCount(); i++) {
            View child = dateRow.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            top = Math.min(top, child.getTop() + child.getTranslationY()
                    + child.getPivotY() * (1.0f - child.getScaleY()));
        }
        return top;
    }

    private float getDateRowBottomOverflow() {
        if (dateRow == null) return 0.0f;
        float bottom = dateRow.getHeight();
        for (int i = 0; i < dateRow.getChildCount(); i++) {
            View child = dateRow.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            float childTop = child.getTop() + child.getTranslationY()
                    + child.getPivotY() * (1.0f - child.getScaleY());
            bottom = Math.max(bottom, childTop + child.getHeight() * child.getScaleY());
        }
        return bottom - dateRow.getHeight();
    }

    private float clampClockScale(float scale) {
        return Math.max(0.4f, Math.min(6.0f, scale));
    }

    private boolean hasUniformClockScale() {
        return Math.abs(timeScaleFactor - dateScaleFactor) < 0.0001f
                && Math.abs(timeScaleFactor - weatherScaleFactor) < 0.0001f;
    }

    private void setupClockDragAndDrop() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (clockSizesLinked && !clockFreeLayoutEnabled) {
                    timeScaleFactor = clampClockScale(timeScaleFactor * factor);
                    dateScaleFactor = clampClockScale(dateScaleFactor * factor);
                    weatherScaleFactor = clampClockScale(weatherScaleFactor * factor);
                } else if (activeScaleTarget == SCALE_TARGET_TIME) {
                    timeScaleFactor = clampClockScale(timeScaleFactor * factor);
                } else if (activeScaleTarget == SCALE_TARGET_DATE) {
                    dateScaleFactor = clampClockScale(dateScaleFactor * factor);
                } else if (activeScaleTarget == SCALE_TARGET_WEATHER) {
                    weatherScaleFactor = clampClockScale(weatherScaleFactor * factor);
                }
                applyClockScale();
                if (clockFreeLayoutEnabled && activeScaleTarget != SCALE_TARGET_NONE) {
                    setAndClampFreeClockTranslation(
                            activeScaleTarget,
                            getFreeClockTranslationX(activeScaleTarget),
                            getFreeClockTranslationY(activeScaleTarget));
                }
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                if (pomodoroModeLayoutActive) return false;
                float[] focus = getScaleFocusOnScreen(detector);
                activeScaleTarget = findScaleTarget(focus[0], focus[1]);
                if (activeScaleTarget == SCALE_TARGET_NONE) {
                    activeScaleTarget = findNearestScaleTarget(focus[0], focus[1]);
                }
                if (clockSizesLinked && !clockFreeLayoutEnabled) {
                    if (activeScaleTarget == SCALE_TARGET_NONE && !isPointOnClock(focus[0], focus[1])) {
                        return false;
                    }
                } else if (activeScaleTarget == SCALE_TARGET_NONE) {
                    return false;
                }
                cancelClockDragForMultiTouch();
                isScalingClock = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScalingClock = false;
                if (activeScaleTarget != SCALE_TARGET_NONE || clockSizesLinked) {
                    saveClockScaleFactor();
                }
                if (clockFreeLayoutEnabled) saveFreeClockTranslations();
                activeScaleTarget = SCALE_TARGET_NONE;
            }
        });

        clockPanel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!clockFreeLayoutEnabled && !isScalingClock && !pomodoroModeLayoutActive) {
                    isDraggingClock = true;
                    setClockPanelAlpha(0.75f);
                    return true;
                }
                return false;
            }
        });

        clockPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rootContainer.performClick();
            }
        });

        clockPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                resetImmersiveTimeout();

                if (pomodoroModeLayoutActive) {
                    return true;
                }

                if (event.getPointerCount() > 1 || isScalingClock) {
                    if (isDraggingClock) {
                        isDraggingClock = false;
                        setClockPanelAlpha(1.0f);
                    }
                    return true;
                }

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownRawX = event.getRawX();
                        touchDownRawY = event.getRawY();
                        clockStartTransX = clockBaseTranslationX;
                        clockStartTransY = clockBaseTranslationY;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (isDraggingClock) {
                            float deltaX = event.getRawX() - touchDownRawX;
                            float deltaY = event.getRawY() - touchDownRawY;
                            clampAndApplyTranslation(
                                    clockStartTransX + deltaX,
                                    clockStartTransY + deltaY);
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (!isDraggingClock) {
                            view.performClick();
                            return true;
                        }
                    case MotionEvent.ACTION_CANCEL:
                        if (isDraggingClock) {
                            isDraggingClock = false;
                            lastClockDragEndTime = SystemClock.elapsedRealtime();
                            setClockPanelAlpha(1.0f);
                            saveClockPositionRatio();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void cancelClockDragForMultiTouch() {
        if (freeClockLongPressPending || freeClockDragging) {
            cancelFreeClockInteraction(true);
        }
        if (isDraggingClock) {
            isDraggingClock = false;
            setClockPanelAlpha(1.0f);
        }
    }

    private void setClockPanelAlpha(float alpha) {
        if (clockPanel == null) return;
        boolean hadPreviousBounds = captureClockVisualBounds(previousClockBoundsScratch);
        clockPanel.setAlpha(alpha);
        invalidateClockVisualChange(hadPreviousBounds);
    }

    private boolean handleFreeClockTouch(MotionEvent event) {
        if (event == null || !clockFreeLayoutEnabled || pomodoroModeLayoutActive) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            int target = findScaleTarget(event.getRawX(), event.getRawY());
            if (target == SCALE_TARGET_NONE) {
                target = findNearestScaleTarget(event.getRawX(), event.getRawY());
            }
            if (target == SCALE_TARGET_NONE) return false;
            freeClockTouchTarget = target;
            freeClockDownRawX = event.getRawX();
            freeClockDownRawY = event.getRawY();
            freeClockDragStartX = getFreeClockTranslationX(target);
            freeClockDragStartY = getFreeClockTranslationY(target);
            freeClockLongPressPending = true;
            photoHandler.removeCallbacks(freeClockLongPressRunnable);
            photoHandler.postDelayed(
                    freeClockLongPressRunnable, ViewConfiguration.getLongPressTimeout());
            return true;
        }

        if (event.getPointerCount() > 1 || isScalingClock) {
            cancelFreeClockInteraction(true);
            return true;
        }
        if (freeClockTouchTarget == SCALE_TARGET_NONE) return false;

        if (action == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getRawX() - freeClockDownRawX;
            float deltaY = event.getRawY() - freeClockDownRawY;
            if (freeClockLongPressPending) {
                int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
                if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                    freeClockLongPressPending = false;
                    photoHandler.removeCallbacks(freeClockLongPressRunnable);
                }
            }
            if (freeClockDragging) {
                setAndClampFreeClockTranslation(
                        freeClockTouchTarget,
                        freeClockDragStartX + deltaX,
                        freeClockDragStartY + deltaY);
            }
            return true;
        }

        if (action == MotionEvent.ACTION_UP) {
            boolean wasDragging = freeClockDragging;
            cancelFreeClockInteraction(wasDragging);
            if (wasDragging) {
                lastClockDragEndTime = SystemClock.elapsedRealtime();
            } else if (rootContainer != null) {
                rootContainer.performClick();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelFreeClockInteraction(freeClockDragging);
            return true;
        }
        return true;
    }

    private void cancelFreeClockInteraction(boolean savePosition) {
        photoHandler.removeCallbacks(freeClockLongPressRunnable);
        freeClockLongPressPending = false;
        if (freeClockDragging) {
            setFreeClockTargetAlpha(freeClockTouchTarget, 1.0f);
            if (savePosition) saveFreeClockTranslations();
        }
        freeClockDragging = false;
        freeClockTouchTarget = SCALE_TARGET_NONE;
    }

    private View getFreeClockTargetView(int target) {
        if (target == SCALE_TARGET_TIME) return photoTime;
        if (target == SCALE_TARGET_DATE) return dateRow;
        if (target == SCALE_TARGET_WEATHER) return weatherRow;
        return null;
    }

    private void setFreeClockTargetAlpha(int target, float alpha) {
        View targetView = getFreeClockTargetView(target);
        if (targetView == null) return;
        boolean hadPreviousBounds = getFreeClockTargetVisualBounds(
                target, previousClockBoundsScratch);
        targetView.setAlpha(alpha);
        invalidateFreeClockTargetVisualChange(target, hadPreviousBounds);
    }

    private float getFreeClockTranslationX(int target) {
        if (target == SCALE_TARGET_TIME) return timeFreeTranslationX;
        if (target == SCALE_TARGET_DATE) return dateFreeTranslationX;
        if (target == SCALE_TARGET_WEATHER) return weatherFreeTranslationX;
        return 0.0f;
    }

    private float getFreeClockTranslationY(int target) {
        if (target == SCALE_TARGET_TIME) return timeFreeTranslationY;
        if (target == SCALE_TARGET_DATE) return dateFreeTranslationY;
        if (target == SCALE_TARGET_WEATHER) return weatherFreeTranslationY;
        return 0.0f;
    }

    private void setFreeClockTranslation(int target, float translationX, float translationY) {
        View targetView = getFreeClockTargetView(target);
        if (targetView == null) return;
        if (target == SCALE_TARGET_TIME) {
            timeFreeTranslationX = translationX;
            timeFreeTranslationY = translationY;
        } else if (target == SCALE_TARGET_DATE) {
            dateFreeTranslationX = translationX;
            dateFreeTranslationY = translationY;
        } else if (target == SCALE_TARGET_WEATHER) {
            weatherFreeTranslationX = translationX;
            weatherFreeTranslationY = translationY;
        }
        targetView.setTranslationX(translationX);
        targetView.setTranslationY(translationY);
    }

    private void setAndClampFreeClockTranslation(int target, float translationX, float translationY) {
        if (rootContainer == null) return;
        boolean hadPreviousBounds = getFreeClockTargetVisualBounds(
                target, previousClockBoundsScratch);
        setFreeClockTranslation(target, translationX, translationY);
        if (getFreeClockTargetVisualBounds(target, clockBoundsScratch)) {
            float marginLeft = dp(12);
            float marginTop = dp(12);
            float marginRight = rootContainer.getWidth() - dp(12);
            float marginBottom = rootContainer.getHeight() - dp(12);
            float visualWidth = clockBoundsScratch[2] - clockBoundsScratch[0];
            float visualHeight = clockBoundsScratch[3] - clockBoundsScratch[1];
            float correctionX = visualWidth > marginRight - marginLeft
                    ? (rootContainer.getWidth() / 2.0f)
                            - (clockBoundsScratch[0] + clockBoundsScratch[2]) / 2.0f
                    : clockBoundsScratch[0] < marginLeft
                            ? marginLeft - clockBoundsScratch[0]
                            : clockBoundsScratch[2] > marginRight
                                    ? marginRight - clockBoundsScratch[2] : 0.0f;
            float correctionY = visualHeight > marginBottom - marginTop
                    ? (rootContainer.getHeight() / 2.0f)
                            - (clockBoundsScratch[1] + clockBoundsScratch[3]) / 2.0f
                    : clockBoundsScratch[1] < marginTop
                            ? marginTop - clockBoundsScratch[1]
                            : clockBoundsScratch[3] > marginBottom
                                    ? marginBottom - clockBoundsScratch[3] : 0.0f;
            if (correctionX != 0.0f || correctionY != 0.0f) {
                setFreeClockTranslation(
                        target, translationX + correctionX, translationY + correctionY);
            }
        }
        invalidateFreeClockTargetVisualChange(target, hadPreviousBounds);
    }

    private boolean getFreeClockTargetVisualBounds(int target, float[] outBounds) {
        View targetView = getFreeClockTargetView(target);
        return targetView != null && getClockChildVisualBounds(targetView, outBounds);
    }

    /** Redraw only the moving unit, important when the three units are far apart. */
    private void invalidateFreeClockTargetVisualChange(int target, boolean hadPreviousBounds) {
        if (rootContainer == null) return;
        boolean hasCurrentBounds = getFreeClockTargetVisualBounds(target, clockBoundsScratch);
        if (!hadPreviousBounds && !hasCurrentBounds) return;

        float left = hasCurrentBounds ? clockBoundsScratch[0] : previousClockBoundsScratch[0];
        float top = hasCurrentBounds ? clockBoundsScratch[1] : previousClockBoundsScratch[1];
        float right = hasCurrentBounds ? clockBoundsScratch[2] : previousClockBoundsScratch[2];
        float bottom = hasCurrentBounds ? clockBoundsScratch[3] : previousClockBoundsScratch[3];
        if (hadPreviousBounds && hasCurrentBounds) {
            left = Math.min(left, previousClockBoundsScratch[0]);
            top = Math.min(top, previousClockBoundsScratch[1]);
            right = Math.max(right, previousClockBoundsScratch[2]);
            bottom = Math.max(bottom, previousClockBoundsScratch[3]);
        }

        int shadowPadding = dp(24);
        int invalidLeft = Math.max(0, (int) Math.floor(left) - shadowPadding);
        int invalidTop = Math.max(0, (int) Math.floor(top) - shadowPadding);
        int invalidRight = Math.min(
                rootContainer.getWidth(), (int) Math.ceil(right) + shadowPadding);
        int invalidBottom = Math.min(
                rootContainer.getHeight(), (int) Math.ceil(bottom) + shadowPadding);
        if (invalidRight > invalidLeft && invalidBottom > invalidTop) {
            rootContainer.invalidate(invalidLeft, invalidTop, invalidRight, invalidBottom);
        }
    }

    private void saveFreeClockTranslations() {
        if (prefs == null || rootContainer == null
                || rootContainer.getWidth() <= 0 || rootContainer.getHeight() <= 0) {
            return;
        }
        float rootWidth = rootContainer.getWidth();
        float rootHeight = rootContainer.getHeight();
        android.content.SharedPreferences.Editor editor = prefs.edit();
        saveFreeClockTargetPosition(
                editor, SCALE_TARGET_TIME,
                CLOCK_FREE_TIME_X_RATIO, CLOCK_FREE_TIME_Y_RATIO,
                rootWidth, rootHeight);
        saveFreeClockTargetPosition(
                editor, SCALE_TARGET_DATE,
                CLOCK_FREE_DATE_X_RATIO, CLOCK_FREE_DATE_Y_RATIO,
                rootWidth, rootHeight);
        saveFreeClockTargetPosition(
                editor, SCALE_TARGET_WEATHER,
                CLOCK_FREE_WEATHER_X_RATIO, CLOCK_FREE_WEATHER_Y_RATIO,
                rootWidth, rootHeight);
        editor.apply();
    }

    private void saveFreeClockTargetPosition(
            android.content.SharedPreferences.Editor editor, int target,
            String xKey, String yKey, float rootWidth, float rootHeight) {
        if (!getFreeClockTargetVisualBounds(target, clockBoundsScratch)) return;
        editor.putFloat(orientationKey(xKey),
                (clockBoundsScratch[0] + clockBoundsScratch[2]) / (2.0f * rootWidth));
        editor.putFloat(orientationKey(yKey),
                (clockBoundsScratch[1] + clockBoundsScratch[3]) / (2.0f * rootHeight));
    }

    private void restoreFreeClockTranslations() {
        if (!clockFreeLayoutEnabled || prefs == null || rootContainer == null
                || rootContainer.getWidth() <= 0 || rootContainer.getHeight() <= 0) {
            return;
        }
        float rootWidth = rootContainer.getWidth();
        float rootHeight = rootContainer.getHeight();
        restoreFreeClockTargetPosition(
                SCALE_TARGET_TIME,
                CLOCK_FREE_TIME_X_RATIO, CLOCK_FREE_TIME_Y_RATIO,
                rootWidth, rootHeight);
        restoreFreeClockTargetPosition(
                SCALE_TARGET_DATE,
                CLOCK_FREE_DATE_X_RATIO, CLOCK_FREE_DATE_Y_RATIO,
                rootWidth, rootHeight);
        restoreFreeClockTargetPosition(
                SCALE_TARGET_WEATHER,
                CLOCK_FREE_WEATHER_X_RATIO, CLOCK_FREE_WEATHER_Y_RATIO,
                rootWidth, rootHeight);
    }

    private void restoreFreeClockTargetPosition(
            int target, String xKey, String yKey, float rootWidth, float rootHeight) {
        String orientedXKey = orientationKey(xKey);
        String orientedYKey = orientationKey(yKey);
        if (!prefs.contains(orientedXKey) || !prefs.contains(orientedYKey)
                || !getFreeClockTargetVisualBounds(target, clockBoundsScratch)) {
            return;
        }
        float desiredCenterX = prefs.getFloat(orientedXKey, 0.5f) * rootWidth;
        float desiredCenterY = prefs.getFloat(orientedYKey, 0.5f) * rootHeight;
        float currentCenterX = (clockBoundsScratch[0] + clockBoundsScratch[2]) / 2.0f;
        float currentCenterY = (clockBoundsScratch[1] + clockBoundsScratch[3]) / 2.0f;
        setAndClampFreeClockTranslation(
                target,
                getFreeClockTranslationX(target) + desiredCenterX - currentCenterX,
                getFreeClockTranslationY(target) + desiredCenterY - currentCenterY);
    }

    private float getClockDefaultLeft() {
        return rootContainer.getWidth() - clockPanel.getWidth() - dp(16);
    }

    private float getClockDefaultTop() {
        return rootContainer.getHeight() - clockPanel.getHeight() - dp(16);
    }

    private void clampAndApplyTranslation(float targetTransX, float targetTransY) {
        if (rootContainer == null || clockPanel == null) {
            return;
        }
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        if (rootW <= 0 || rootH <= 0 || !getClockVisualBounds(clockBoundsScratch)) {
            clockBaseTranslationX = targetTransX;
            clockBaseTranslationY = targetTransY;
            applyClockTranslation();
            return;
        }

        // Remove the current panel translation to get bounds at a zero translation,
        // then constrain the transformed visual group instead of its stale layout box.
        float fixedLeft = clockBoundsScratch[0] - clockPanel.getTranslationX();
        float fixedTop = clockBoundsScratch[1] - clockPanel.getTranslationY();
        float fixedRight = clockBoundsScratch[2] - clockPanel.getTranslationX();
        float fixedBottom = clockBoundsScratch[3] - clockPanel.getTranslationY();
        float minTransX = dp(12) - fixedLeft - burnInOffsetX;
        float maxTransX = rootW - dp(16) - fixedRight - burnInOffsetX;
        float minTransY = dp(12) - fixedTop - burnInOffsetY;
        float maxTransY = rootH - dp(16) - fixedBottom - burnInOffsetY;
        if (minTransX > maxTransX) minTransX = maxTransX = (minTransX + maxTransX) / 2.0f;
        if (minTransY > maxTransY) minTransY = maxTransY = (minTransY + maxTransY) / 2.0f;

        clockBaseTranslationX = Math.max(minTransX, Math.min(maxTransX, targetTransX));
        clockBaseTranslationY = Math.max(minTransY, Math.min(maxTransY, targetTransY));
        applyClockTranslation();
    }

    private void saveClockPositionRatio() {
        if (rootContainer == null || clockPanel == null) {
            return;
        }
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        if (!getClockVisualBounds(clockBoundsScratch)) return;
        float clockW = clockBoundsScratch[2] - clockBoundsScratch[0];
        float clockH = clockBoundsScratch[3] - clockBoundsScratch[1];
        if (rootW <= clockW || rootH <= clockH) {
            return;
        }

        float currentLeft = clockBoundsScratch[0];
        float currentTop = clockBoundsScratch[1];

        prefs.edit()
                .putFloat(orientationKey(CLOCK_POS_X_RATIO), currentLeft / (float) (rootW - clockW))
                .putFloat(orientationKey(CLOCK_POS_Y_RATIO), currentTop / (float) (rootH - clockH))
                .apply();
    }

    private void restoreClockPosition() {
        float ratioX = prefs.getFloat(
                orientationKey(CLOCK_POS_X_RATIO),
                0.95f);
        float ratioY = prefs.getFloat(
                orientationKey(CLOCK_POS_Y_RATIO),
                0.90f);
        if (ratioX < 0.0f || ratioY < 0.0f || rootContainer == null || clockPanel == null) {
            return;
        }
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        if (!getClockVisualBounds(clockBoundsScratch)) return;
        float clockW = clockBoundsScratch[2] - clockBoundsScratch[0];
        float clockH = clockBoundsScratch[3] - clockBoundsScratch[1];
        if (rootW <= clockW || rootH <= clockH) {
            return;
        }

        float fixedLeft = clockBoundsScratch[0] - clockPanel.getTranslationX();
        float fixedTop = clockBoundsScratch[1] - clockPanel.getTranslationY();
        clampAndApplyTranslation(
                ratioX * (rootW - clockW) - fixedLeft - burnInOffsetX,
                ratioY * (rootH - clockH) - fixedTop - burnInOffsetY);
        applyClockScale();
    }

    private void applyClockTranslation() {
        if (clockPanel != null) {
            boolean hadPreviousBounds = captureClockVisualBounds(previousClockBoundsScratch);
            if (pomodoroModeLayoutActive) {
                clockPanel.setTranslationX(0.0f);
                clockPanel.setTranslationY(0.0f);
            } else {
                clockPanel.setTranslationX(clockBaseTranslationX + burnInOffsetX);
                clockPanel.setTranslationY(clockBaseTranslationY + burnInOffsetY);
            }
            invalidateClockVisualChange(hadPreviousBounds);
        }
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
                lowPowerMode ? SAF_RESCAN_LOW_POWER_MS : SAF_RESCAN_NORMAL_MS);
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
        if (lowPowerMode || effectiveDisplayMode() != 0) {
            return;
        }
        if (photoImage == null || photoBitmap == null
                || photoImage.getWidth() <= 0 || photoImage.getHeight() <= 0) {
            return;
        }
        photoPanReverse = !photoPanReverse;
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
                applyPhotoPan((Float) animation.getAnimatedValue());
            }
        });
        photoPanAnimator.start();
    }

    private void stopPhotoPan() {
        if (photoPanAnimator != null) {
            photoPanAnimator.cancel();
        }
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

        float scale = Math.max(
                (float) viewWidth / bitmapWidth,
                (float) viewHeight / bitmapHeight);
        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;
        float overflowX = Math.max(0.0f, scaledWidth - viewWidth);
        float overflowY = Math.max(0.0f, scaledHeight - viewHeight);
        float smoothProgress = progress * progress * (3.0f - 2.0f * progress);
        float startPosition = (1.0f - PHOTO_PAN_TRAVEL_FRACTION) / 2.0f;
        float travelProgress = photoPanReverse ? 1.0f - smoothProgress : smoothProgress;

        float targetPosX = isSmartFocusActive() ? focalX : 0.5f;
        float targetPosY = isSmartFocusActive() ? focalY : 0.5f;

        float position = startPosition + PHOTO_PAN_TRAVEL_FRACTION * travelProgress;

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
        return adaptiveColorEnabled && !lowPowerMode;
    }

    private boolean isSmartFocusActive() {
        return smartFocusEnabled && !lowPowerMode && effectiveDisplayMode() == 0;
    }

    private int effectiveDisplayMode() {
        return lowPowerMode && photoDisplayMode == 2 ? 1 : photoDisplayMode;
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
        if (photoTime != null) {
            photoTime.setText(photoTimeFormat.format(nowDate));
        }
        if (photoDate != null) {
            if (FontManager.usesLatinDate(dateFontId)) {
                photoDate.setText(photoDateFormatEn.format(nowDate));
            } else {
                photoDate.setText(photoDateFormat.format(nowDate));
            }
        }
        updateAlarmIndicator();
        updatePomodoroDisplay();
    }

    private void checkPomodoroCompletion() {
        PomodoroHelper.Transition transition = PomodoroHelper.finishIfDue(this);
        if (transition != null) {
            vibratePhaseFinished();
            Toast.makeText(this, PomodoroHelper.phaseLabel(transition.finishedPhase)
                    + "結束，下一階段：" + PomodoroHelper.phaseLabel(transition.nextPhase),
                    Toast.LENGTH_LONG).show();
            updatePomodoroDisplay();
        }
    }

    private void updatePomodoroDisplay() {
        if (pomodoroRow == null || pomodoroLabel == null || pomodoroText == null) return;
        PomodoroHelper.Snapshot snapshot = PomodoroHelper.getSnapshot(this);
        if (!snapshot.hasSession) {
            applyPomodoroModeLayout(false);
            updatePomodoroQuickActions(false);
            pomodoroFocusPanel.setClickable(false);
            pomodoroRow.setVisibility(View.GONE);
            return;
        }
        pomodoroLabel.setText(pomodoroEnglishLabel(snapshot));
        pomodoroText.setText(PomodoroHelper.formatRemaining(snapshot.remainingMs));
        applyPomodoroModeLayout(true);
        updatePomodoroQuickActions(snapshot.running);
        // 暫停時整個倒數畫面就是「繼續」按鈕；運行中仍讓既有觸控行為處理。
        pomodoroFocusPanel.setClickable(!snapshot.running);
        pomodoroRow.setVisibility(View.VISIBLE);
        if (activityResumed) schedulePhotoTicker();
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
                movePomodoroRowToFocusPanel();
                pomodoroFocusPanel.setVisibility(View.VISIBLE);
            } else {
                movePomodoroRowToClockPanel();
                pomodoroFocusPanel.setVisibility(View.GONE);
            }
            applyClockTranslation();
            applyClockScale();
            if (rootContainer != null) {
                rootContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        updateWeatherFromCache();
                        if (!pomodoroModeLayoutActive) {
                            restoreClockPosition();
                            restoreFreeClockTranslations();
                        }
                    }
                });
            }
        }
        if (photoTime != null) {
            photoTime.setVisibility(clockTimeEnabled ? View.VISIBLE : View.GONE);
            photoTime.setTextSize(active ? 38 : 64);
            photoTime.setAlpha(1.0f);
        }
        if (photoDate != null) photoDate.setTextSize(active ? 17 : 24);
        if (dateRow != null) dateRow.setVisibility(View.VISIBLE);
        if (compactWeatherTemperature != null) compactWeatherTemperature.setTextSize(active ? 17 : 18);
        if (weatherTemperature != null) weatherTemperature.setTextSize(active ? 17 : 20);
        if (weatherLocation != null) weatherLocation.setTextSize(active ? 13 : 14);
        if (compactWeatherIcon != null) resizeView(compactWeatherIcon, active ? 19 : 22, active ? 19 : 22);
        if (weatherIcon != null) resizeView(weatherIcon, active ? 22 : 30, active ? 22 : 30);
        if (alarmRow != null) alarmRow.setVisibility(active ? View.GONE : alarmRow.getVisibility());
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
        if (active) applyPomodoroFocusLayout();
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(3), 0, 0);
        clockPanel.addView(pomodoroRow, 1, params);
    }

    private void resizeView(View view, int widthDp, int heightDp) {
        android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = dp(widthDp);
            params.height = dp(heightDp);
            view.setLayoutParams(params);
        }
    }

    private void updateAlarmIndicator() {
        if (alarmRow == null || alarmTimeText == null) return;
        String alarmTime = AlarmHelper.getNextAlarmTimeString(this);
        if (alarmTime != null) {
            alarmTimeText.setText(alarmTime);
            alarmRow.setVisibility(View.VISIBLE);
        } else {
            alarmRow.setVisibility(View.GONE);
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
        long interval = lowPowerMode ? WEATHER_FRESH_LOW_POWER_MS : WEATHER_FRESH_NORMAL_MS;
        photoHandler.postDelayed(weatherRefreshRunnable, Math.max(0L, interval - age));
    }

    private void requestWeatherRefresh() {
        if (!activityResumed || !weatherEnabled || weatherFetchInFlight) return;
        if (Double.isNaN(weatherLatitude) || Double.isNaN(weatherLongitude)) return;
        if (!WeatherClient.isWifiConnected(this)) {
            weatherLastAttemptAt = System.currentTimeMillis();
            photoHandler.postDelayed(weatherRefreshRunnable,
                    lowPowerMode ? WEATHER_FRESH_LOW_POWER_MS : WEATHER_FRESH_NORMAL_MS);
            return;
        }
        weatherFetchInFlight = true;
        weatherLastAttemptAt = System.currentTimeMillis();
        final double latitude = weatherLatitude;
        final double longitude = weatherLongitude;
        weatherExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final WeatherClient.CurrentWeather weather =
                            WeatherClient.fetchCurrent(latitude, longitude);
                    prefs.edit()
                            .putInt(SettingsActivity.WEATHER_TEMPERATURE, weather.temperatureCelsius)
                            .putInt(SettingsActivity.WEATHER_CODE, weather.weatherCode)
                            .putBoolean(SettingsActivity.WEATHER_IS_DAY, weather.daytime)
                            .putLong(SettingsActivity.WEATHER_UPDATED_AT, System.currentTimeMillis())
                            .apply();
                } catch (Exception ignored) {
                    // 保留快取；舊裝置 TLS 或暫時斷線時不打擾相簿播放。
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        weatherFetchInFlight = false;
                        if (activityResumed) scheduleWeatherRefresh();
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
        weatherLocation.setText(WeatherClient.removeAccents(weatherLocationName));
        weatherLocation.setVisibility(
                weatherShowLocation && weatherLocationName.length() > 0 ? View.VISIBLE : View.GONE);
        weatherIcon.setWeather(code, isDay);
        compactWeatherIcon.setWeather(code, isDay);

        // Free layout always uses a standalone weather unit; compact weather is nested
        // inside the date row and therefore cannot be positioned independently.
        boolean compact = weatherCompactMode && !weatherShowLocation && !clockFreeLayoutEnabled;
        compactWeatherRow.setVisibility(compact ? View.VISIBLE : View.GONE);
        weatherRow.setVisibility(compact ? View.GONE : View.VISIBLE);
        if (clockPanel != null) clockPanel.requestLayout();
    }

    private void hideWeatherRows() {
        if (compactWeatherRow != null) compactWeatherRow.setVisibility(View.GONE);
        if (weatherRow != null) weatherRow.setVisibility(View.GONE);
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
                                playbackNavigator.reset(photoFiles.size());
                                if (!photoLoading) loadNextPhoto();
                            }
                        });
                    }
                }, inaccessibleTree, generation);
                final List<PhotoSource> discovered = filterPhotos(
                        found, favoriteSnapshot, hiddenSnapshot, favoritesOnlySnapshot);

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
                    PhotoSource source = PhotoSource.fromFile(entry, identity);
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
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            };
            cursor = getContentResolver().query(children, projection, null, null, null);
            if (cursor == null) return;
            int idColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int typeColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE);
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
                    PhotoSource source = PhotoSource.fromUri(child);
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
        options.inPreferredConfig = lowPowerMode
                ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
        options.inDither = lowPowerMode;
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

    private void animateOut(int type, Runnable endAction) {
        switch (type) {
            case 1:
                photoImage.animate().translationX(-photoImage.getWidth() * 0.35f)
                        .alpha(0.0f).setDuration(1400)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 2:
                photoImage.animate().translationY(-photoImage.getHeight() * 0.35f)
                        .alpha(0.0f).setDuration(1400)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 3:
                photoImage.animate().scaleX(0.92f).scaleY(0.92f)
                        .alpha(0.0f).setDuration(1300)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 4:
                photoImage.animate().rotationY(90f)
                        .alpha(0.0f).setDuration(1100)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 5:
                photoImage.animate().rotation(-5f)
                        .alpha(0.0f).setDuration(1400)
                        .setInterpolator(smoothInterpolator)
                        .withEndAction(endAction).start();
                break;
            case 0:
            default:
                photoImage.animate().alpha(0.0f).setDuration(1500)
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
                photoImage.animate().translationX(0.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 2:
                photoImage.setTranslationY(photoImage.getHeight() * 0.35f);
                photoImage.setAlpha(0.0f);
                photoImage.animate().translationY(0.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 3:
                photoImage.setScaleX(1.06f);
                photoImage.setScaleY(1.06f);
                photoImage.setAlpha(0.0f);
                photoImage.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 4:
                photoImage.setRotationY(-90f);
                photoImage.setAlpha(0.0f);
                photoImage.animate().rotationY(0.0f).alpha(1.0f).setDuration(1600)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 5:
                photoImage.setRotation(5f);
                photoImage.setAlpha(0.0f);
                photoImage.animate().rotation(0.0f).alpha(1.0f).setDuration(2000)
                        .setInterpolator(smoothInterpolator).start();
                break;
            case 0:
            default:
                photoImage.setAlpha(0.0f);
                photoImage.animate().alpha(1.0f).setDuration(2200)
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
