package com.quietphoto.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Lightweight progress track used only by the optional Pomodoro display mode. */
public final class PomodoroProgressView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private float progress;

    public PomodoroProgressView(Context context) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        trackPaint.setColor(Color.argb(105, 255, 255, 255));
        progressPaint.setColor(Color.rgb(255, 104, 104));
        setContentDescription("番茄鐘進度");
        setPadding(Math.round(density * 4.0f), 0, Math.round(density * 4.0f), 0);
    }

    public void setProgress(float progress) {
        float bounded = Math.max(0.0f, Math.min(1.0f, progress));
        if (Math.abs(this.progress - bounded) < 0.001f) return;
        this.progress = bounded;
        invalidate();
    }

    public void setProgressColor(int color) {
        if (progressPaint.getColor() == color) return;
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getHeight() * 0.5f;
        float halfHeight = Math.max(1.0f, getHeight() * 0.18f);
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        if (right <= left) return;
        track.set(left, centerY - halfHeight, right, centerY + halfHeight);
        float radius = halfHeight;
        canvas.drawRoundRect(track, radius, radius, trackPaint);
        if (progress <= 0.0f) return;
        track.right = left + (right - left) * progress;
        canvas.drawRoundRect(track, radius, radius, progressPaint);
    }
}
