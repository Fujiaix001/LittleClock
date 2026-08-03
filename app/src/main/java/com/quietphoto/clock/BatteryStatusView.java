package com.quietphoto.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Small dependency-free battery indicator that renders consistently on API 17 devices. */
final class BatteryStatusView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface typeface;
    private final RectF batteryRect = new RectF();
    private boolean present;
    private boolean plugged;
    private int levelPercent = -1;

    BatteryStatusView(Context context) {
        super(context);
        lightningPaint.setStyle(Paint.Style.FILL);
        lightningPaint.setColor(Color.rgb(104, 213, 216));
        typeface = FontManager.getPomodoroChineseFont(context);
        setContentDescription("電量未知");
        setWillNotDraw(false);
    }

    void setBatteryState(PowerStateMonitor.State state) {
        if (state == null) return;
        present = state.batteryPresent;
        plugged = state.plugged;
        levelPercent = state.levelPercent;
        setContentDescription(contentDescriptionText());
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = Math.round(68.0f * getResources().getDisplayMetrics().density);
        int height = Math.round(24.0f * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!present) return;
        float density = getResources().getDisplayMetrics().density;
        float iconLeft = 2.0f * density;
        float iconTop = 5.0f * density;
        float iconWidth = 20.0f * density;
        float iconHeight = 13.0f * density;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.0f, density));
        paint.setColor(textColor());
        batteryRect.set(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);
        canvas.drawRoundRect(batteryRect, 2.0f * density, 2.0f * density, paint);
        canvas.drawRect(iconLeft + iconWidth, iconTop + 4.0f * density,
                iconLeft + iconWidth + 2.0f * density, iconTop + 9.0f * density, paint);
        if (levelPercent >= 0) {
            paint.setStyle(Paint.Style.FILL);
            float fill = (iconWidth - 4.0f * density) * levelPercent / 100.0f;
            canvas.drawRoundRect(new RectF(iconLeft + 2.0f * density, iconTop + 2.0f * density,
                    iconLeft + 2.0f * density + Math.max(0.0f, fill),
                    iconTop + iconHeight - 2.0f * density),
                    density, density, paint);
        }
        paint.setTextSize(13.0f * density);
        paint.setTypeface(typeface);
        String text = levelPercent >= 0 ? levelPercent + "%" : "--";
        canvas.drawText(text, iconLeft + iconWidth + 8.0f * density,
                iconTop + 11.5f * density, paint);
        if (plugged) {
            paint.setColor(Color.rgb(104, 213, 216));
            paint.setStyle(Paint.Style.FILL);
            PathHelper.drawLightning(canvas, lightningPaint,
                    iconLeft + iconWidth + 3.5f * density,
                    iconTop + 2.0f * density, density);
        }
    }

    private int textColor() {
        if (levelPercent >= 0 && levelPercent <= 10) return Color.rgb(255, 125, 125);
        if (levelPercent >= 0 && levelPercent <= 20) return Color.rgb(255, 190, 96);
        return Color.WHITE;
    }

    private String contentDescriptionText() {
        if (!present) return "沒有電池";
        if (levelPercent < 0) return plugged ? "電量未知，充電中" : "電量未知";
        return "電量 " + levelPercent + "%" + (plugged ? "，充電中" : "");
    }

    /** Tiny geometry helper avoids depending on a drawable or icon font. */
    private static final class PathHelper {
        private PathHelper() {
        }

        static void drawLightning(Canvas canvas, Paint paint, float x, float y, float density) {
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(x + 5.0f * density, y);
            path.lineTo(x, y + 7.0f * density);
            path.lineTo(x + 4.0f * density, y + 7.0f * density);
            path.lineTo(x + 1.5f * density, y + 13.0f * density);
            path.lineTo(x + 8.0f * density, y + 5.0f * density);
            path.lineTo(x + 4.5f * density, y + 5.0f * density);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}
