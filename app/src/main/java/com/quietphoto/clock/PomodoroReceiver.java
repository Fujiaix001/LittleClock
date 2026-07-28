package com.quietphoto.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Delivers one concise notification when a background Pomodoro phase ends. */
public final class PomodoroReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "pomodoro_silent_channel_v2";
    private static final int NOTIFICATION_ID = 3001;

    @Override
    @android.annotation.SuppressLint("NotificationPermission")
    public void onReceive(Context context, Intent intent) {
        PomodoroHelper.Transition transition = PomodoroHelper.finishIfDue(context);
        if (transition == null) return;
        vibratePhaseFinished(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "番茄鐘提醒", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("專注與休息階段結束提醒");
            channel.setSound(null, null);
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(context, PhotoClockActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(context, 3002, openIntent, flags);
        String title = PomodoroHelper.phaseLabel(transition.finishedPhase) + "結束";
        String text = "下一階段：" + PomodoroHelper.phaseLabel(transition.nextPhase) + "，點擊開啟番茄鐘";
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setDefaults(0);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    @SuppressWarnings("deprecation")
    private void vibratePhaseFinished(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            long[] pattern = new long[] { 0L, 180L, 130L, 180L, 130L, 250L };
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern,
                        new int[] { 0, 255, 0, 255, 0, 255 }, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (RuntimeException ignored) {
            // Notifications remain available on devices without a vibration motor.
        }
    }
}
