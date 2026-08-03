package com.quietphoto.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/** Foreground-only battery state monitor. It owns no thread and no persistent component. */
final class PowerStateMonitor {
    interface Listener {
        void onPowerStateChanged(State state);
    }

    static final class State {
        final boolean batteryPresent;
        final boolean plugged;
        final boolean stateKnown;
        final int levelPercent;
        final float temperatureCelsius;

        State(boolean batteryPresent, boolean plugged, boolean stateKnown,
                int levelPercent, float temperatureCelsius) {
            this.batteryPresent = batteryPresent;
            this.plugged = plugged;
            this.stateKnown = stateKnown;
            this.levelPercent = levelPercent;
            this.temperatureCelsius = temperatureCelsius;
        }

        static State unknown() {
            return new State(true, true, false, -1, Float.NaN);
        }
    }

    private final Context appContext;
    private BroadcastReceiver receiver;
    private Listener listener;
    private State state = State.unknown();

    PowerStateMonitor(Context context) {
        appContext = context.getApplicationContext();
    }

    void start(Listener stateListener) {
        stop();
        listener = stateListener;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFromIntent(intent);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        try {
            Intent sticky = appContext.registerReceiver(receiver, filter);
            if (sticky != null) updateFromIntent(sticky);
        } catch (RuntimeException ignored) {
            receiver = null;
            state = State.unknown();
        }
    }

    void stop() {
        if (receiver != null) {
            try {
                appContext.unregisterReceiver(receiver);
            } catch (RuntimeException ignored) {
                // Some old vendor builds report an already-unregistered receiver.
            }
            receiver = null;
        }
        listener = null;
    }

    State getState() {
        return state;
    }

    private void updateFromIntent(Intent intent) {
        if (intent == null) return;
        boolean stateKnown = intent.hasExtra(BatteryManager.EXTRA_PRESENT)
                || intent.hasExtra(BatteryManager.EXTRA_LEVEL)
                || intent.hasExtra(BatteryManager.EXTRA_PLUGGED);
        boolean present = !intent.hasExtra(BatteryManager.EXTRA_PRESENT)
                || intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
        int pluggedType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean plugged = pluggedType == BatteryManager.BATTERY_PLUGGED_AC
                || pluggedType == BatteryManager.BATTERY_PLUGGED_USB
                || pluggedType == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = scale > 0 && level >= 0
                ? Math.max(0, Math.min(100, Math.round(level * 100.0f / scale))) : -1;
        float temperature = Float.NaN;
        if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f;
        }
        state = new State(present, plugged || !present, stateKnown, percent, temperature);
        if (listener != null) listener.onPowerStateChanged(state);
    }
}
