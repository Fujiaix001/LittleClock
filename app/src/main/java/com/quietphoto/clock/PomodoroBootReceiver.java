package com.quietphoto.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores a user-started timer after reboot without introducing a persistent service. */
public final class PomodoroBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            PomodoroHelper.rescheduleAfterBoot(context);
        }
    }
}
