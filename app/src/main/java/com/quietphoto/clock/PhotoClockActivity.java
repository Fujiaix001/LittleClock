package com.quietphoto.clock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private static final String PHOTO_DIRECTORY = "QuietPanel/Photos";
    private static final int MAX_PHOTO_FILES = 10000;
    private static final long IMMERSIVE_TIMEOUT_MS = 5000L;

    private FrameLayout rootContainer;
    private FrameLayout polaroidContainer;
    private ImageView photoImage;
    private TextView photoStatus;
    private LinearLayout clockPanel;
    private TextView photoTime;
    private TextView photoDate;
    private Button settingsButton;

    private Bitmap photoBitmap;
    private Bitmap pendingPhotoBitmap;
    private final List<File> photoFiles = new ArrayList<File>();
    private final Handler photoHandler = new Handler();
    private final Matrix photoMatrix = new Matrix();
    private final Date nowDate = new Date();
    private final AccelerateDecelerateInterpolator smoothInterpolator = new AccelerateDecelerateInterpolator();
    private SharedPreferences prefs;

    private final SimpleDateFormat photoTimeFormat =
            new SimpleDateFormat("HH:mm", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormat =
            new SimpleDateFormat("M月d日 EEEE", Locale.TAIWAN);
    private final SimpleDateFormat photoDateFormatEn =
            new SimpleDateFormat("EEE, MMM d", Locale.US);

    private int photoIndex;
    private int photoFailures;
    private int photoGeneration;
    private boolean photoLoading;
    private boolean photoPanReverse = true;
    private boolean activityResumed;

    private long photoIntervalMs = 45000L;
    private long photoPanDurationMs = 43000L;
    private boolean clockBgEnabled;
    private int clockFontStyle;
    private boolean nightModeEnabled;
    private int nightStartHour = 23;
    private int nightEndHour = 7;
    private int transitionType;
    private boolean isNightSleepActive;

    // 3 大視覺特效標誌
    private boolean adaptiveColorEnabled = true;
    private boolean polaroidFrameEnabled = false;
    private boolean smartFocusEnabled = true;

    private int currentDominantColor = BACKGROUND;
    private float focalX = 0.5f;
    private float focalY = 0.5f;

    private long photoPanStartedAt;
    private long nextPhotoAt;

    private boolean isDraggingClock;
    private boolean isScalingClock;
    private long lastClockDragEndTime;
    private float touchDownRawX;
    private float touchDownRawY;
    private float clockStartTransX;
    private float clockStartTransY;
    private float clockScaleFactor = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;

    private final Runnable hideImmersiveRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };

    private final Runnable photoTicker = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed) {
                return;
            }
            updatePhotoClock();
            checkNightSleepMode();
            if (!isNightSleepActive && !photoLoading && !photoFiles.isEmpty()
                    && SystemClock.elapsedRealtime() >= nextPhotoAt) {
                loadNextPhoto();
            }
            photoHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable photoPanTicker = new Runnable() {
        @Override
        public void run() {
            if (!activityResumed || photoBitmap == null || isNightSleepActive) {
                return;
            }
            long elapsed = SystemClock.elapsedRealtime() - photoPanStartedAt;
            float progress = Math.min(1.0f, (float) elapsed / photoPanDurationMs);
            applyPhotoPan(progress);
            if (progress < 1.0f) {
                photoHandler.postDelayed(this, PHOTO_PAN_FRAME_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(SettingsActivity.PREFERENCES, MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(buildInterface());
        checkAndRequestStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        hideSystemUI();
        loadSettingsConfig();
        startPhotoSlideshow();
        rootContainer.post(new Runnable() {
            @Override
            public void run() {
                restoreClockPosition();
            }
        });
    }

    @Override
    protected void onPause() {
        activityResumed = false;
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        stopPhotoSlideshow();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        photoHandler.removeCallbacks(hideImmersiveRunnable);
        stopPhotoSlideshow();
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
        if (photoImage != null && photoBitmap != null) {
            applyPhotoPan(0.0f);
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

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            String perm = (Build.VERSION.SDK_INT >= 33) ?
                    "android.permission.READ_MEDIA_IMAGES" :
                    Manifest.permission.READ_EXTERNAL_STORAGE;
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { perm }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults != null && grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        clockFontStyle = prefs.getInt(SettingsActivity.CLOCK_FONT_STYLE, 0);
        nightModeEnabled = prefs.getBoolean(SettingsActivity.NIGHT_MODE_ENABLED, false);
        nightStartHour = prefs.getInt(SettingsActivity.NIGHT_START_HOUR, 23);
        nightEndHour = prefs.getInt(SettingsActivity.NIGHT_END_HOUR, 7);
        transitionType = prefs.getInt(SettingsActivity.TRANSITION_TYPE, 0);

        adaptiveColorEnabled = prefs.getBoolean(SettingsActivity.ADAPTIVE_COLOR_ENABLED, true);
        polaroidFrameEnabled = prefs.getBoolean(SettingsActivity.POLAROID_FRAME_ENABLED, false);
        smartFocusEnabled = prefs.getBoolean(SettingsActivity.SMART_FOCUS_ENABLED, true);
        clockScaleFactor = prefs.getFloat(CLOCK_SCALE_FACTOR, 1.0f);

        applyPolaroidStyle();
        updateClockStyle();
        applyClockScale();
    }

    private void applyPolaroidStyle() {
        if (polaroidContainer == null) {
            return;
        }
        if (polaroidFrameEnabled) {
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
            enterNightSleepMode();
        } else if (!shouldSleep && isNightSleepActive) {
            isNightSleepActive = false;
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
        if (photoTime != null) {
            photoTime.setTextColor(Color.argb(120, 100, 100, 100));
        }
        if (photoDate != null) {
            photoDate.setTextColor(Color.argb(120, 100, 100, 100));
        }
    }

    private void restoreNormalMode() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);

        if (photoTime != null) {
            photoTime.setTextColor(Color.WHITE);
        }
        if (photoDate != null) {
            photoDate.setTextColor(Color.WHITE);
        }
        if (photoImage != null && photoBitmap != null) {
            photoImage.animate().cancel();
            photoImage.animate().alpha(1.0f).setDuration(1600).start();
            startPhotoPan();
        }
    }

    private void updateClockStyle() {
        if (clockPanel == null) {
            return;
        }

        Typeface tf;
        switch (clockFontStyle) {
            case 1:
                tf = FontManager.getFont(this, "fonts/font_digital.ttf", Typeface.MONOSPACE);
                break;
            case 2:
                tf = FontManager.getFont(this, "fonts/font_sans.ttf", Typeface.SANS_SERIF);
                break;
            case 3:
                tf = FontManager.getFont(this, "fonts/font_serif.ttf", Typeface.SERIF);
                break;
            case 4:
                tf = FontManager.getFont(this, "fonts/font_rounded.ttf", Typeface.SANS_SERIF);
                break;
            case 5:
                tf = FontManager.getFont(this, "fonts/font_kai.ttf", Typeface.SERIF);
                break;
            case 6:
                tf = FontManager.getFont(this, "fonts/font_heavy.ttf", Typeface.DEFAULT_BOLD);
                break;
            case 7:
                tf = FontManager.getFont(this, "fonts/font_orbitron.ttf", Typeface.SANS_SERIF);
                break;
            case 8:
                tf = FontManager.getFont(this, "fonts/font_audiowide.ttf", Typeface.SANS_SERIF);
                break;
            case 9:
                tf = FontManager.getFont(this, "fonts/font_oxanium.ttf", Typeface.SANS_SERIF);
                break;
            case 10:
                tf = FontManager.getFont(this, "fonts/font_sairastencil.ttf", Typeface.SANS_SERIF);
                break;
            case 11:
                tf = FontManager.getFont(this, "fonts/font_zendots.ttf", Typeface.SANS_SERIF);
                break;
            case 0:
            default:
                tf = Typeface.DEFAULT_BOLD;
                break;
        }

        if (photoTime != null) {
            photoTime.setTypeface(tf);
        }
        if (photoDate != null) {
            photoDate.setTypeface(tf);
        }
        updatePhotoClock();

        if (clockBgEnabled) {
            GradientDrawable bg = new GradientDrawable();
            int bgColor = adaptiveColorEnabled ?
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
                if (SystemClock.elapsedRealtime() - lastClockDragEndTime < 400) {
                    return;
                }
                showSettingsButton();
                resetImmersiveTimeout();
            }
        });

        // 相片繪圖層
        polaroidContainer = new FrameLayout(this);
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

        clockPanel = new LinearLayout(this);
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

        photoDate = new TextView(this);
        photoDate.setTextSize(24);
        photoDate.setTextColor(Color.WHITE);
        photoDate.setGravity(Gravity.CENTER_HORIZONTAL);
        photoDate.setIncludeFontPadding(false);
        photoDate.setShadowLayer(dp(2), dp(1), dp(1), Color.BLACK);
        clockPanel.addView(photoDate, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.RIGHT);
        clockParams.setMargins(dp(28), dp(28), dp(16), dp(16));
        rootContainer.addView(clockPanel, clockParams);

        updateClockStyle();
        setupClockDragAndDrop();

        settingsButton = new Button(this);
        settingsButton.setText("⚙️ 設定");
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
                dp(130), dp(48), Gravity.TOP | Gravity.LEFT);
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
            prefs.edit().putFloat(CLOCK_SCALE_FACTOR, clockScaleFactor).apply();
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

        clockPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                resetImmersiveTimeout();
                if (scaleGestureDetector != null) {
                    scaleGestureDetector.onTouchEvent(event);
                }

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
                        clockStartTransX = clockPanel.getTranslationX();
                        clockStartTransY = clockPanel.getTranslationY();
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
            clockPanel.setTranslationX(targetTransX);
            clockPanel.setTranslationY(targetTransY);
            return;
        }

        float defaultLeft = getClockDefaultLeft();
        float defaultTop = getClockDefaultTop();

        float minTransX = -defaultLeft + dp(12);
        float maxTransX = dp(16);
        float minTransY = -defaultTop + dp(12);
        float maxTransY = dp(16);

        clockPanel.setTranslationX(Math.max(minTransX, Math.min(maxTransX, targetTransX)));
        clockPanel.setTranslationY(Math.max(minTransY, Math.min(maxTransY, targetTransY)));
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

        float currentLeft = getClockDefaultLeft() + clockPanel.getTranslationX();
        float currentTop = getClockDefaultTop() + clockPanel.getTranslationY();

        prefs.edit()
                .putFloat(SettingsActivity.CLOCK_X_RATIO, currentLeft / (float) (rootW - clockW))
                .putFloat(SettingsActivity.CLOCK_Y_RATIO, currentTop / (float) (rootH - clockH))
                .commit();
    }

    private void restoreClockPosition() {
        float ratioX = prefs.getFloat(SettingsActivity.CLOCK_X_RATIO, -1.0f);
        float ratioY = prefs.getFloat(SettingsActivity.CLOCK_Y_RATIO, -1.0f);
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
        refreshPhotoFiles();
        if (!photoFiles.isEmpty()) {
            loadNextPhoto();
        }
        photoHandler.postDelayed(photoTicker, 1000);
    }

    private void stopPhotoSlideshow() {
        photoHandler.removeCallbacks(photoTicker);
        photoHandler.removeCallbacks(photoPanTicker);
        photoGeneration++;
        photoLoading = false;
        if (photoImage != null) {
            photoImage.animate().cancel();
            photoImage.setImageDrawable(null);
            photoImage.setAlpha(1.0f);
        }
        if (photoBitmap != null && !photoBitmap.isRecycled()) {
            photoBitmap.recycle();
            photoBitmap = null;
        }
        if (pendingPhotoBitmap != null && !pendingPhotoBitmap.isRecycled()) {
            pendingPhotoBitmap.recycle();
            pendingPhotoBitmap = null;
        }
    }

    private void startPhotoPan() {
        photoHandler.removeCallbacks(photoPanTicker);
        if (photoImage == null || photoBitmap == null
                || photoImage.getWidth() <= 0 || photoImage.getHeight() <= 0) {
            return;
        }
        photoPanReverse = !photoPanReverse;
        photoPanStartedAt = SystemClock.elapsedRealtime();
        applyPhotoPan(0.0f);
        photoHandler.postDelayed(photoPanTicker, PHOTO_PAN_FRAME_MS);
    }

    private void stopPhotoPan() {
        photoHandler.removeCallbacks(photoPanTicker);
    }

    private void applyPhotoPan(float progress) {
        if (photoImage == null || photoBitmap == null) {
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

        float targetPosX = smartFocusEnabled ? focalX : 0.5f;
        float targetPosY = smartFocusEnabled ? focalY : 0.5f;

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

    private void updatePhotoClock() {
        nowDate.setTime(System.currentTimeMillis());
        if (photoTime != null) {
            photoTime.setText(photoTimeFormat.format(nowDate));
        }
        if (photoDate != null) {
            if (clockFontStyle >= 7 && clockFontStyle <= 11) {
                photoDate.setText(photoDateFormatEn.format(nowDate));
            } else {
                photoDate.setText(photoDateFormat.format(nowDate));
            }
        }
    }

    private void refreshPhotoFiles() {
        photoFiles.clear();
        photoIndex = 0;
        photoFailures = 0;

        Set<String> selectedFolders = prefs.getStringSet(SettingsActivity.PHOTO_FOLDERS, null);
        if (selectedFolders == null || selectedFolders.isEmpty()) {
            File defaultDirectory = new File(
                    Environment.getExternalStorageDirectory(), PHOTO_DIRECTORY);
            if (!defaultDirectory.exists() && !defaultDirectory.mkdirs()) {
                showPhotoStatus(
                        "無法建立預設照片資料夾\n" + defaultDirectory.getAbsolutePath(),
                        WARNING);
                return;
            }
            selectedFolders = new HashSet<String>();
            selectedFolders.add(defaultDirectory.getAbsolutePath());
        }

        Set<String> discoveredPhotos = new LinkedHashSet<String>();
        for (String path : selectedFolders) {
            collectPhotoFiles(new File(path), discoveredPhotos, 0);
            if (discoveredPhotos.size() >= MAX_PHOTO_FILES) {
                break;
            }
        }
        for (String path : discoveredPhotos) {
            photoFiles.add(new File(path));
        }
        Collections.shuffle(photoFiles);
        if (photoFiles.isEmpty()) {
            showPhotoStatus(
                    "選擇的資料夾沒有可播放照片\n"
                            + "請點一下畫面，再點選「⚙️ 設定」選擇相簿",
                    SECONDARY);
        } else {
            showPhotoStatus(
                    "正在載入 " + photoFiles.size() + " 張照片…", SECONDARY);
        }
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
        if (photoIndex >= photoFiles.size()) {
            Collections.shuffle(photoFiles);
            photoIndex = 0;
        }

        final File file = photoFiles.get(photoIndex++);
        final int generation = photoGeneration;
        photoLoading = true;
        nextPhotoAt = SystemClock.elapsedRealtime() + photoIntervalMs;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decodePhoto(file);
                int domColor = BACKGROUND;
                float fx = 0.5f;
                float fy = 0.5f;
                if (bitmap != null) {
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
                            sample.recycle();
                        }
                    }
                    if (pixels != null) {
                        if (adaptiveColorEnabled) {
                            long rSum = 0, gSum = 0, bSum = 0;
                            for (int px : pixels) {
                                rSum += (px >> 16) & 0xff;
                                gSum += (px >> 8) & 0xff;
                                bSum += px & 0xff;
                            }
                            domColor = Color.rgb((int) (rSum / 400), (int) (gSum / 400), (int) (bSum / 400));
                        }
                        if (smartFocusEnabled) {
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
                                bitmap.recycle();
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
                        currentDominantColor = finalDomColor;
                        focalX = finalFx;
                        focalY = finalFy;
                        updateClockStyle();
                        displayPhoto(bitmap);
                    }
                });
            }
        }, "QuietPhotoClock-photo-decode").start();
    }

    private Bitmap decodePhoto(File file) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int targetWidth = getResources().getDisplayMetrics().widthPixels;
        int targetHeight = getResources().getDisplayMetrics().heightPixels;

        // 計算最大安全解碼像素上限 (預設限制在 250萬像素內，使記憶體極致鎖定在 <5MB)
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
            bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (OutOfMemoryError ignored) {
            // 自動降級重試：縮小 4 倍記憶體再次嘗試
            options.inSampleSize = sample * 2;
            try {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            } catch (OutOfMemoryError ignoredAgain) {
                return null;
            }
        }
        if (bmp == null) return null;

        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            else if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            else if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            if (rotationAngle != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotationAngle);
                Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                if (rotated != bmp) {
                    bmp.recycle();
                    bmp = rotated;
                }
            }
        } catch (Throwable ignored) {
        }
        return bmp;
    }

    private void displayPhoto(final Bitmap bitmap) {
        photoStatus.setVisibility(View.GONE);
        if (isNightSleepActive) {
            if (photoBitmap != null && photoBitmap != bitmap && !photoBitmap.isRecycled()) {
                photoBitmap.recycle();
            }
            photoBitmap = bitmap;
            return;
        }

        if (photoBitmap == null) {
            photoBitmap = bitmap;
            photoImage.setImageBitmap(bitmap);
            startPhotoPan();
            photoImage.setAlpha(0.0f);
            photoImage.animate().alpha(1.0f).setDuration(1800).setInterpolator(smoothInterpolator).start();
            return;
        }

        if (pendingPhotoBitmap != null
                && pendingPhotoBitmap != bitmap
                && !pendingPhotoBitmap.isRecycled()) {
            pendingPhotoBitmap.recycle();
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
                        bitmap.recycle();
                    }
                    return;
                }

                Bitmap previous = photoBitmap;
                photoBitmap = bitmap;
                pendingPhotoBitmap = null;

                photoImage.setImageBitmap(bitmap);
                startPhotoPan();

                if (previous != null && previous != bitmap && !previous.isRecycled()) {
                    previous.recycle();
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
