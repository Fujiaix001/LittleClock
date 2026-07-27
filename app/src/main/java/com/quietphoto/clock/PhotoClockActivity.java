package com.quietphoto.clock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentUris;
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
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static final int BACKGROUND = Color.rgb(11, 15, 20);
    private static final int PRIMARY = Color.rgb(242, 238, 230);
    private static final int SECONDARY = Color.rgb(143, 152, 163);
    private static final int WARNING = Color.rgb(239, 108, 108);

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final long PHOTO_PAN_FRAME_MS = 67;
    private static final float PHOTO_PAN_TRAVEL_FRACTION = 0.20f;
    private static final String CLOCK_POS_X_RATIO = "clock_pos_x_ratio";
    private static final String CLOCK_POS_Y_RATIO = "clock_pos_y_ratio";
    public static final String CLOCK_SCALE_FACTOR = "clock_scale_factor";
    private static final String LAST_PHOTO_KEY = "last_photo_key";
    private static final String PHOTO_DIRECTORY = "QuietPanel/Photos";
    private static final int MAX_PHOTO_FILES = 10000;
    private static final long IMMERSIVE_TIMEOUT_MS = 5000L;
    private static final long BURN_IN_INTERVAL_MS = 180000L;
    private static final long MEDIA_REFRESH_DELAY_MS = 2000L;
    private static final long WEATHER_FRESH_NORMAL_MS = 60L * 60L * 1000L;
    private static final long WEATHER_FRESH_LOW_POWER_MS = 120L * 60L * 1000L;
    private static final long WEATHER_MAX_AGE_MS = 6L * 60L * 60L * 1000L;

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
    private Button settingsButton;

    private Bitmap photoBitmap;
    private Bitmap pendingPhotoBitmap;
    private Bitmap softBackgroundBitmap;
    private final List<PhotoSource> photoFiles = new ArrayList<PhotoSource>();
    private final PlaybackNavigator playbackNavigator = new PlaybackNavigator();
    private final Handler photoHandler = new Handler();
    private final ExecutorService photoExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
    private final Matrix photoMatrix = new Matrix();
    private final Date nowDate = new Date();
    private final AccelerateDecelerateInterpolator smoothInterpolator = new AccelerateDecelerateInterpolator();
    private SharedPreferences prefs;
    private boolean clockTimeEnabled = true;
    private boolean clockDateEnabled = true;
    private long lastAlarmFiredMinute = -1;
    private PhotoSource currentPhotoSource;

    private final SimpleDateFormat photoTimeFormat =
            new SimpleDateFormat("HH:mm", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormat =
            new SimpleDateFormat("M月d日 EEEE", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormatEn =
            new SimpleDateFormat("EEE, MMM d", Locale.US);

    private int photoFailures;
    private int photoGeneration;
    private boolean photoLoading;
    private boolean photoPanReverse = true;
    private boolean activityResumed;
    private boolean firstSlideshowStart = true;
    private boolean startupPhotoDisplayed;

    private long photoIntervalMs = 45000L;
    private long photoPanDurationMs = 43000L;
    private boolean clockBgEnabled;
    private String clockFontId = FontManager.DEFAULT_ID;
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
    private float clockBaseTranslationX;
    private float clockBaseTranslationY;
    private float burnInOffsetX;
    private float burnInOffsetY;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector photoGestureDetector;
    private final Random random = new Random();
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private float lastLightLevel = -1.0f;
    private ContentObserver mediaObserver;
    private boolean mediaObserverRegistered;

    private static final class PhotoSource {
        final String path;
        final Uri uri;

        PhotoSource(String path, Uri uri) {
            this.path = path;
            this.uri = uri;
        }

        String key() {
            return path != null ? path : uri.toString();
        }
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
            checkNightSleepMode();
            if (!isNightSleepActive && !photoLoading && !photoFiles.isEmpty()
                    && SystemClock.elapsedRealtime() >= nextPhotoAt) {
                loadNextPhoto();
            }
            photoHandler.postDelayed(this, 1000);
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
            if (activityResumed && hasPhotoReadAccess()) {
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(buildInterface());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        setupPhotoGestures();
        createMediaObserver();
        extractDefaultWallpapers();
        checkAndRequestStoragePermission();
    }

    private void extractDefaultWallpapers() {
        if (prefs.getBoolean("wallpaper_extracted_internal", false)) {
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
                    prefs.edit().putBoolean("wallpaper_extracted_internal", true).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        updateAlarmIndicator();
        rootContainer.post(new Runnable() {
            @Override
            public void run() {
                restoreClockPosition();
            }
        });
    }

    @Override
    protected void onPause() {
        if (currentPhotoSource != null) {
            prefs.edit().putString(LAST_PHOTO_KEY, currentPhotoSource.key()).apply();
        }
        activityResumed = false;
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.removeCallbacks(burnInRunnable);
        photoHandler.removeCallbacks(mediaRefreshRunnable);
        photoHandler.removeCallbacks(weatherRefreshRunnable);
        unregisterMediaObserver();
        unregisterLightSensor();
        stopPhotoSlideshow();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        photoHandler.removeCallbacks(burnInRunnable);
        photoHandler.removeCallbacks(mediaRefreshRunnable);
        photoHandler.removeCallbacks(weatherRefreshRunnable);
        unregisterMediaObserver();
        unregisterLightSensor();
        stopPhotoSlideshow();
        photoExecutor.shutdownNow();
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
        clockScaleFactor = prefs.getFloat(
                orientationKey(CLOCK_SCALE_FACTOR),
                prefs.getFloat(CLOCK_SCALE_FACTOR, 1.0f));
        applyClockScale();
        if (photoImage != null && photoBitmap != null) {
            applyPhotoPresentation(photoBitmap);
        }
        if (rootContainer != null) {
            rootContainer.post(new Runnable() {
                @Override
                public void run() {
                    restoreClockPosition();
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

                    @Override
                    public void onLongPress(MotionEvent event) {
                        if (!isTouchOnClock(event)) {
                            showPhotoActions();
                        }
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event != null && event.getAction() == MotionEvent.ACTION_DOWN && isNightSleepActive) {
            triggerNightSleepWakeup();
        }
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }
        if (photoGestureDetector != null && event.getPointerCount() == 1
                && !isDraggingClock && !isScalingClock) {
            photoGestureDetector.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
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
        int[] location = new int[2];
        clockPanel.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        float scaleX = clockPanel.getScaleX();
        float scaleY = clockPanel.getScaleY();
        float left = location[0] + clockPanel.getPivotX() * (1.0f - scaleX);
        float top = location[1] + clockPanel.getPivotY() * (1.0f - scaleY);
        float right = left + clockPanel.getWidth() * scaleX;
        float bottom = top + clockPanel.getHeight() * scaleY;
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private void showPhotoActions() {
        final PhotoSource source = currentPhotoSource;
        if (source == null) {
            return;
        }
        final String key = source.key();
        final boolean favorite = favoritePhotos.contains(key);
        String[] actions = {
                favorite ? "取消收藏" : "加入收藏",
                "隱藏此相片",
                "取消"
        };

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.select_dialog_item, actions) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setPadding(dp(16), dp(8), dp(16), dp(8));
                view.setMinHeight(0); // Override default minHeight on older Android
                return view;
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("相片操作")
                .setAdapter(adapter, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (favorite) favoritePhotos.remove(key);
                            else favoritePhotos.add(key);
                            prefs.edit().putStringSet(
                                    SettingsActivity.FAVORITE_PHOTOS,
                                    new HashSet<String>(favoritePhotos)).apply();
                            Toast.makeText(PhotoClockActivity.this,
                                    favorite ? "已取消收藏" : "已加入收藏",
                                    Toast.LENGTH_SHORT).show();
                            if (favoritesOnly && favorite) startPhotoSlideshow();
                        } else if (which == 1) {
                            hiddenPhotos.add(key);
                            prefs.edit().putStringSet(
                                    SettingsActivity.HIDDEN_PHOTOS,
                                    new HashSet<String>(hiddenPhotos)).apply();
                            startPhotoSlideshow();
                        }
                    }
                })
                .show();
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
        if (Build.VERSION.SDK_INT < 23 || hasPhotoReadAccess()) {
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
        if (Build.VERSION.SDK_INT >= 34) {
            return checkSelfPermission("android.permission.READ_MEDIA_IMAGES")
                            == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                            == PackageManager.PERMISSION_GRANTED;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission("android.permission.READ_MEDIA_IMAGES")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasPhotoReadAccess()) {
                startPhotoSlideshow();
            } else {
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
        clockScaleFactor = prefs.getFloat(
                orientationKey(CLOCK_SCALE_FACTOR),
                prefs.getFloat(CLOCK_SCALE_FACTOR, 0.75f));

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

        android.graphics.Typeface tf = FontManager.getFont(this, clockFontId);

        if (photoTime != null) {
            photoTime.setTypeface(tf);
            photoTime.setVisibility(clockTimeEnabled ? View.VISIBLE : View.GONE);
        }
        if (photoDate != null) {
            photoDate.setTypeface(tf);
            photoDate.setVisibility(clockDateEnabled ? View.VISIBLE : View.GONE);
        }
        if (weatherTemperature != null) weatherTemperature.setTypeface(tf);
        if (compactWeatherTemperature != null) compactWeatherTemperature.setTypeface(tf);
        if (alarmTimeText != null) alarmTimeText.setTypeface(tf);
        if (weatherLocation != null) weatherLocation.setTypeface(tf);
        updatePhotoClock();

        if (clockBgEnabled) {
            GradientDrawable bg = new GradientDrawable();
            int bgColor = isAdaptiveColorActive() ?
                    Color.argb(85, Color.red(currentDominantColor), Color.green(currentDominantColor), Color.blue(currentDominantColor)) :
                    Color.argb(65, 0, 0, 0);
            bg.setColor(bgColor);
            bg.setCornerRadius(dp(10));
            clockPanel.setBackground(bg);
            clockPanel.setPadding(dp(12), dp(4), dp(12), dp(6));
        } else {
            clockPanel.setBackground(null);
            clockPanel.setPadding(0, 0, 0, 0);
        }
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

        updatePhotoClock();
        return rootContainer;
    }

    private void applyClockScale() {
        if (clockPanel != null) {
            clockPanel.setPivotX(clockPanel.getWidth() / 2f);
            clockPanel.setPivotY(clockPanel.getHeight() / 2f);
            clockPanel.setScaleX(clockScaleFactor);
            clockPanel.setScaleY(clockScaleFactor);
        }
    }

    private void saveClockScaleFactor() {
        if (prefs != null) {
            prefs.edit().putFloat(orientationKey(CLOCK_SCALE_FACTOR), clockScaleFactor).apply();
        }
    }

    private void setupClockDragAndDrop() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                clockScaleFactor *= factor;
                clockScaleFactor = Math.max(0.4f, Math.min(6.0f, clockScaleFactor));
                applyClockScale();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScalingClock = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScalingClock = false;
                saveClockScaleFactor();
            }
        });

        clockPanel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!isScalingClock) {
                    isDraggingClock = true;
                    clockPanel.setAlpha(0.75f);
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

                if (event.getPointerCount() > 1 || isScalingClock) {
                    if (isDraggingClock) {
                        isDraggingClock = false;
                        clockPanel.setAlpha(1.0f);
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
                            clockPanel.setAlpha(1.0f);
                            saveClockPositionRatio();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
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
        int clockW = clockPanel.getWidth();
        int clockH = clockPanel.getHeight();
        if (rootW <= 0 || rootH <= 0 || clockW <= 0 || clockH <= 0) {
            clockBaseTranslationX = targetTransX;
            clockBaseTranslationY = targetTransY;
            applyClockTranslation();
            return;
        }

        float defaultLeft = getClockDefaultLeft();
        float defaultTop = getClockDefaultTop();

        float minTransX = -defaultLeft + dp(12);
        float maxTransX = dp(16);
        float minTransY = -defaultTop + dp(12);
        float maxTransY = dp(16);

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
        int clockW = clockPanel.getWidth();
        int clockH = clockPanel.getHeight();
        if (rootW <= clockW || rootH <= clockH) {
            return;
        }

        float currentLeft = getClockDefaultLeft() + clockBaseTranslationX;
        float currentTop = getClockDefaultTop() + clockBaseTranslationY;

        prefs.edit()
                .putFloat(orientationKey(CLOCK_POS_X_RATIO), currentLeft / (float) (rootW - clockW))
                .putFloat(orientationKey(CLOCK_POS_Y_RATIO), currentTop / (float) (rootH - clockH))
                .apply();
    }

    private void restoreClockPosition() {
        float ratioX = prefs.getFloat(
                orientationKey(CLOCK_POS_X_RATIO),
                prefs.getFloat(SettingsActivity.CLOCK_X_RATIO, 0.95f));
        float ratioY = prefs.getFloat(
                orientationKey(CLOCK_POS_Y_RATIO),
                prefs.getFloat(SettingsActivity.CLOCK_Y_RATIO, 0.90f));
        if (ratioX < 0.0f || ratioY < 0.0f || rootContainer == null || clockPanel == null) {
            return;
        }
        int rootW = rootContainer.getWidth();
        int rootH = rootContainer.getHeight();
        int clockW = clockPanel.getWidth();
        int clockH = clockPanel.getHeight();
        if (rootW <= clockW || rootH <= clockH) {
            return;
        }

        float defaultLeft = getClockDefaultLeft();
        float defaultTop = getClockDefaultTop();
        clampAndApplyTranslation(ratioX * (rootW - clockW) - defaultLeft, ratioY * (rootH - clockH) - defaultTop);
        applyClockScale();
    }

    private void applyClockTranslation() {
        if (clockPanel != null) {
            clockPanel.setTranslationX(clockBaseTranslationX + burnInOffsetX);
            clockPanel.setTranslationY(clockBaseTranslationY + burnInOffsetY);
        }
    }

    private String orientationKey(String baseKey) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        return baseKey + (landscape ? "_landscape" : "_portrait");
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private void showSettingsButton() {
        if (settingsButton == null) {
            return;
        }
        settingsButton.animate().cancel();
        settingsButton.setVisibility(View.VISIBLE);
        settingsButton.animate().alpha(0.88f).setDuration(180).start();
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
    }

    private void startPhotoSlideshow() {
        stopPhotoSlideshow();
        updatePhotoClock();
        if (firstSlideshowStart) {
            firstSlideshowStart = false;
            queueStartupPhoto();
        }
        refreshPhotoFiles();
        photoHandler.postDelayed(photoTicker, 1000);
    }

    private void stopPhotoSlideshow() {
        photoHandler.removeCallbacks(photoTicker);
        stopPhotoPan();
        photoGeneration++;
        photoLoading = false;
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
        playbackNavigator.reset(0);
    }

    private void queueStartupPhoto() {
        String key = prefs.getString(LAST_PHOTO_KEY, null);
        if (key == null || key.length() == 0 || hiddenPhotos.contains(key)
                || (favoritesOnly && !favoritePhotos.contains(key))) {
            return;
        }

        final PhotoSource source;
        if (key.startsWith("content://")) {
            source = new PhotoSource(null, Uri.parse(key));
        } else {
            File file = new File(key);
            if (!file.isFile()) {
                prefs.edit().remove(LAST_PHOTO_KEY).apply();
                return;
            }
            source = new PhotoSource(key, null);
        }

        final int generation = photoGeneration;
        photoExecutor.execute(new Runnable() {
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
                        if (generation != photoGeneration || !activityResumed || bitmap == null) {
                            safeRecycle(bitmap);
                            return;
                        }
                        currentPhotoSource = source;
                        startupPhotoDisplayed = true;
                        nextPhotoAt = SystemClock.elapsedRealtime() + photoIntervalMs;
                        displayPhoto(bitmap);
                    }
                });
            }
        });
    }

    private void startPhotoPan() {
        stopPhotoPan();
        if (effectiveDisplayMode() != 0) {
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
            if (FontManager.usesLatinDate(clockFontId)) {
                photoDate.setText(photoDateFormatEn.format(nowDate));
            } else {
                photoDate.setText(photoDateFormat.format(nowDate));
            }
        }
        updateAlarmIndicator();
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

        boolean compact = weatherCompactMode && !weatherShowLocation;
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

    private void refreshPhotoFiles() {
        photoFiles.clear();
        photoFailures = 0;
        photoLoading = true;
        showPhotoStatus("正在尋找相片…", SECONDARY);

        final int generation = photoGeneration;
        Set<String> configuredFolders = prefs.getStringSet(SettingsActivity.PHOTO_FOLDERS, null);
        final Set<String> selectedFolders = configuredFolders == null
                ? new HashSet<String>()
                : new HashSet<String>(configuredFolders);
        final Set<String> favoriteSnapshot = new HashSet<String>(favoritePhotos);
        final Set<String> hiddenSnapshot = new HashSet<String>(hiddenPhotos);
        final boolean favoritesOnlySnapshot = favoritesOnly;

        photoExecutor.execute(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                List<PhotoSource> found = new ArrayList<PhotoSource>();
                if (Build.VERSION.SDK_INT >= 29) {
                    Set<String> externalFolders = new HashSet<String>();
                    Set<String> internalFolders = new HashSet<String>();
                    String internalPath = getFilesDir().getAbsolutePath();
                    for (String folder : selectedFolders) {
                        if (folder.startsWith(internalPath)) {
                            internalFolders.add(folder);
                        } else {
                            externalFolders.add(folder);
                        }
                    }
                    if (!externalFolders.isEmpty()) {
                        found.addAll(queryMediaStore(externalFolders));
                    }
                    if (!internalFolders.isEmpty()) {
                        found.addAll(scanLegacyFolders(internalFolders));
                    }
                } else {
                    found.addAll(scanLegacyFolders(selectedFolders));
                }
                final List<PhotoSource> discovered = filterPhotos(
                        found, favoriteSnapshot, hiddenSnapshot, favoritesOnlySnapshot);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != photoGeneration || !activityResumed) {
                            return;
                        }
                        photoFiles.clear();
                        photoFiles.addAll(discovered);
                        int startupIndex = findPhotoIndex(currentPhotoSource);
                        if (startupPhotoDisplayed && startupIndex >= 0) {
                            playbackNavigator.resetAt(photoFiles.size(), startupIndex);
                        } else {
                            playbackNavigator.reset(photoFiles.size());
                        }
                        photoFailures = 0;
                        photoLoading = false;

                        if (photoFiles.isEmpty()) {
                            showPhotoStatus(
                                    "沒有可播放的相片\n請到設定選擇相簿或調整相片權限",
                                    SECONDARY);
                        } else if (!startupPhotoDisplayed || startupIndex < 0) {
                            showPhotoStatus(
                                    "正在載入 " + photoFiles.size() + " 張相片…", SECONDARY);
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
            String key = source.key();
            if (hidden.contains(key)) {
                continue;
            }
            if (onlyFavorites && !favorites.contains(key)) {
                continue;
            }
            filtered.add(source);
        }
        return filtered;
    }

    private List<PhotoSource> queryMediaStore(Set<String> selectedFolders) {
        List<PhotoSource> photos = new ArrayList<PhotoSource>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
        };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    collection,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");
            if (cursor == null) {
                return photos;
            }
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            while (cursor.moveToNext() && photos.size() < MAX_PHOTO_FILES) {
                long id = cursor.getLong(idColumn);
                String path = pathColumn >= 0 ? cursor.getString(pathColumn) : null;
                if (!selectedFolders.isEmpty() && !isInSelectedFolder(path, selectedFolders)) {
                    continue;
                }
                photos.add(new PhotoSource(path, ContentUris.withAppendedId(collection, id)));
            }
        } catch (SecurityException ignored) {
            // Permission can be revoked while the app is scanning.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return photos;
    }

    private boolean isInSelectedFolder(String photoPath, Set<String> selectedFolders) {
        if (photoPath == null) {
            return false;
        }
        String normalizedPhoto = new File(photoPath).getAbsolutePath();
        for (String folder : selectedFolders) {
            String normalizedFolder = new File(folder).getAbsolutePath();
            if (normalizedPhoto.equals(normalizedFolder)
                    || normalizedPhoto.startsWith(normalizedFolder + File.separator)) {
                return true;
            }
        }
        return false;
    }

    private List<PhotoSource> scanLegacyFolders(Set<String> selectedFolders) {
        Set<String> folders = new HashSet<String>(selectedFolders);
        if (folders.isEmpty()) {
            File defaultDirectory = new File(
                    Environment.getExternalStorageDirectory(), PHOTO_DIRECTORY);
            if (!defaultDirectory.exists()) {
                defaultDirectory.mkdirs();
            }
            folders.add(defaultDirectory.getAbsolutePath());
        }

        Set<String> discoveredPaths = new LinkedHashSet<String>();
        for (String path : folders) {
            collectPhotoFiles(new File(path), discoveredPaths, 0);
            if (discoveredPaths.size() >= MAX_PHOTO_FILES) {
                break;
            }
        }

        List<PhotoSource> photos = new ArrayList<PhotoSource>();
        for (String path : discoveredPaths) {
            photos.add(new PhotoSource(path, null));
        }
        return photos;
    }

    private void collectPhotoFiles(File directory, Set<String> discoveredPhotos, int depth) {
        if (directory == null || !directory.isDirectory() || depth > 12 || discoveredPhotos.size() >= MAX_PHOTO_FILES) {
            return;
        }
        if (new File(directory, ".nomedia").exists()) {
            return;
        }
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (discoveredPhotos.size() >= MAX_PHOTO_FILES) {
                return;
            }
            if (entry.isDirectory() && !entry.getName().startsWith(".")) {
                collectPhotoFiles(entry, discoveredPhotos, depth + 1);
            } else if (entry.isFile() && isSupportedPhoto(entry.getName())) {
                discoveredPhotos.add(entry.getAbsolutePath());
            }
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
        photoExecutor.execute(new Runnable() {
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
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        Bitmap bmp = null;
        try {
            bmp = decodeBitmap(source, options);
        } catch (OutOfMemoryError ignored) {
            // 降低解析度後重試。
            options.inSampleSize = sample * 2;
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
        if (source.uri == null) {
            return source.path == null ? null : BitmapFactory.decodeFile(source.path, options);
        }

        InputStream input = null;
        try {
            input = getContentResolver().openInputStream(source.uri);
            return input == null ? null : BitmapFactory.decodeStream(input, null, options);
        } catch (Exception ignored) {
            return null;
        } finally {
            closeQuietly(input);
        }
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
            } else if (source.path != null) {
                exif = new androidx.exifinterface.media.ExifInterface(source.path);
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
