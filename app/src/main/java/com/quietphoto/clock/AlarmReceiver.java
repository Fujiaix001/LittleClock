package com.quietphoto.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "LittleClock:AlarmWakeLock");
            wakeLock.acquire(10000L); // 10 seconds max lock
        }

        SharedPreferences prefs = AlarmHelper.getPrefs(context);
        boolean repeat = prefs.getBoolean(AlarmHelper.PREF_ALARM_REPEAT, true);
        if (!repeat) {
            // Disable single alarm after firing
            prefs.edit().putBoolean(AlarmHelper.PREF_ALARM_ENABLED, false).apply();
        } else {
            // Reschedule for next day
            int hour = prefs.getInt(AlarmHelper.PREF_ALARM_HOUR, 7);
            int minute = prefs.getInt(AlarmHelper.PREF_ALARM_MINUTE, 0);
            AlarmHelper.scheduleAlarmAt(context, hour, minute, true);
        }

        Intent ringIntent = new Intent(context, AlarmRingingActivity.class);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(ringIntent);

        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {
            }
        }
    }
}
