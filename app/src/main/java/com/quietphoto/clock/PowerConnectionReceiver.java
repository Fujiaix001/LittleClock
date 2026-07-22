package com.quietphoto.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
                SettingsActivity.PREFERENCES, Context.MODE_PRIVATE);
        boolean autoStart = prefs.getBoolean(SettingsActivity.AUTO_START_CHARGING, true);
        if (!autoStart) {
            return;
        }

        Intent clockIntent = new Intent(context, PhotoClockActivity.class);
        clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(clockIntent);
    }
}
