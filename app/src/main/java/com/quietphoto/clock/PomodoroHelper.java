package com.quietphoto.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

/** Lightweight, persistent state and one-shot scheduling for the Pomodoro timer. */
public final class PomodoroHelper {
    public static final String PHASE_FOCUS = "focus";
    public static final String PHASE_SHORT_BREAK = "short_break";
    public static final String PHASE_LONG_BREAK = "long_break";

    public static final int DEFAULT_FOCUS_MINUTES = 25;
    public static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
    public static final int DEFAULT_LONG_BREAK_MINUTES = 15;

    public static final String PREF_FOCUS_MINUTES = "pomodoro_focus_minutes";
    public static final String PREF_SHORT_BREAK_MINUTES = "pomodoro_short_break_minutes";
    public static final String PREF_LONG_BREAK_MINUTES = "pomodoro_long_break_minutes";

    private static final String PREF_HAS_SESSION = "pomodoro_has_session";
    private static final String PREF_RUNNING = "pomodoro_running";
    private static final String PREF_PHASE = "pomodoro_phase";
    private static final String PREF_REMAINING_MS = "pomodoro_remaining_ms";
    private static final String PREF_END_ELAPSED = "pomodoro_end_elapsed";
    private static final String PREF_STARTED_ELAPSED = "pomodoro_started_elapsed";
    private static final String PREF_END_WALL = "pomodoro_end_wall";
    private static final String PREF_COMPLETED_FOCUS = "pomodoro_completed_focus";
    private static final String ACTION_FINISHED = "com.quietphoto.clock.POMODORO_FINISHED";
    private static final int REQUEST_CODE = 3001;
    private static final long MINUTE_MS = 60L * 1000L;
    private static final Object LOCK = new Object();

    private PomodoroHelper() { }

    public static final class Snapshot {
        public final boolean hasSession;
        public final boolean running;
        public final String phase;
        public final long remainingMs;
        public final int completedFocusSessions;

        Snapshot(boolean hasSession, boolean running, String phase, long remainingMs,
                int completedFocusSessions) {
            this.hasSession = hasSession;
            this.running = running;
            this.phase = phase;
            this.remainingMs = remainingMs;
            this.completedFocusSessions = completedFocusSessions;
        }
    }

    public static final class Transition {
        public final String finishedPhase;
        public final String nextPhase;
        public final int completedFocusSessions;

        Transition(String finishedPhase, String nextPhase, int completedFocusSessions) {
            this.finishedPhase = finishedPhase;
            this.nextPhase = nextPhase;
            this.completedFocusSessions = completedFocusSessions;
        }
    }

    public static Snapshot getSnapshot(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            boolean hasSession = prefs.getBoolean(PREF_HAS_SESSION, false);
            boolean running = prefs.getBoolean(PREF_RUNNING, false);
            String phase = prefs.getString(PREF_PHASE, PHASE_FOCUS);
            long remaining = running ? remainingRunningMs(prefs) : prefs.getLong(
                    PREF_REMAINING_MS, durationForPhase(prefs, phase));
            return new Snapshot(hasSession, running, phase, Math.max(0L, remaining),
                    prefs.getInt(PREF_COMPLETED_FOCUS, 0));
        }
    }

    public static void prepare(Context context, String phase) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            cancelEndAlarm(context);
            prefs.edit()
                    .putBoolean(PREF_HAS_SESSION, true)
                    .putBoolean(PREF_RUNNING, false)
                    .putString(PREF_PHASE, normalizePhase(phase))
                    .putLong(PREF_REMAINING_MS, durationForPhase(prefs, normalizePhase(phase)))
                    .remove(PREF_END_ELAPSED)
                    .remove(PREF_STARTED_ELAPSED)
                    .remove(PREF_END_WALL)
                    .apply();
        }
    }

    /** Starts the selected phase or resumes its saved remaining time. Returns whether an exact alarm was used. */
    public static boolean startOrResume(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            String phase = normalizePhase(prefs.getString(PREF_PHASE, PHASE_FOCUS));
            long remaining = Math.max(1000L, prefs.getLong(PREF_REMAINING_MS,
                    durationForPhase(prefs, phase)));
            long nowElapsed = SystemClock.elapsedRealtime();
            long nowWall = System.currentTimeMillis();
            prefs.edit()
                    .putBoolean(PREF_HAS_SESSION, true)
                    .putBoolean(PREF_RUNNING, true)
                    .putString(PREF_PHASE, phase)
                    .putLong(PREF_END_ELAPSED, nowElapsed + remaining)
                    .putLong(PREF_STARTED_ELAPSED, nowElapsed)
                    .putLong(PREF_END_WALL, nowWall + remaining)
                    .putLong(PREF_REMAINING_MS, remaining)
                    .apply();
            return scheduleEndAlarm(context, remaining);
        }
    }

    public static void pause(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            if (!prefs.getBoolean(PREF_RUNNING, false)) return;
            long remaining = Math.max(0L, remainingRunningMs(prefs));
            cancelEndAlarm(context);
            prefs.edit()
                    .putBoolean(PREF_RUNNING, false)
                    .putLong(PREF_REMAINING_MS, remaining)
                    .remove(PREF_END_ELAPSED)
                    .remove(PREF_STARTED_ELAPSED)
                    .remove(PREF_END_WALL)
                    .apply();
        }
    }

    public static void reset(Context context) {
        synchronized (LOCK) {
            cancelEndAlarm(context);
            prefs(context).edit()
                    .putBoolean(PREF_HAS_SESSION, false)
                    .putBoolean(PREF_RUNNING, false)
                    .putString(PREF_PHASE, PHASE_FOCUS)
                    .remove(PREF_REMAINING_MS)
                    .remove(PREF_END_ELAPSED)
                    .remove(PREF_STARTED_ELAPSED)
                    .remove(PREF_END_WALL)
                    .apply();
        }
    }

    /** Moves to the next phase without recording an unfinished focus session. */
    public static void skip(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            String phase = normalizePhase(prefs.getString(PREF_PHASE, PHASE_FOCUS));
            moveToNextPhase(context, prefs, phase, false);
        }
    }

    /** Returns a transition only once, even when the receiver and foreground UI arrive together. */
    public static Transition finishIfDue(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            if (!prefs.getBoolean(PREF_RUNNING, false) || remainingRunningMs(prefs) > 0L) {
                return null;
            }
            String finished = normalizePhase(prefs.getString(PREF_PHASE, PHASE_FOCUS));
            int completed = moveToNextPhase(context, prefs, finished, PHASE_FOCUS.equals(finished));
            String next = prefs(context).getString(PREF_PHASE, PHASE_FOCUS);
            return new Transition(finished, next, completed);
        }
    }

    /** Recreates the one outstanding alarm after device boot. */
    public static void rescheduleAfterBoot(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            if (!prefs.getBoolean(PREF_RUNNING, false)) return;
            long remaining = Math.max(0L, prefs.getLong(PREF_END_WALL, 0L) - System.currentTimeMillis());
            if (remaining <= 0L) {
                finishIfDueAfterBoot(context, prefs);
            } else {
                long nowElapsed = SystemClock.elapsedRealtime();
                prefs.edit().putLong(PREF_END_ELAPSED, nowElapsed + remaining)
                        .putLong(PREF_STARTED_ELAPSED, nowElapsed).apply();
                scheduleEndAlarm(context, remaining);
            }
        }
    }

    public static String phaseLabel(String phase) {
        if (PHASE_SHORT_BREAK.equals(phase)) return "短休息";
        if (PHASE_LONG_BREAK.equals(phase)) return "長休息";
        return "專注";
    }

    public static String formatRemaining(long remainingMs) {
        long totalSeconds = Math.max(0L, (remainingMs + 999L) / 1000L);
        return String.format(java.util.Locale.TAIWAN, "%02d:%02d",
                totalSeconds / 60L, totalSeconds % 60L);
    }

    private static int moveToNextPhase(Context context, SharedPreferences prefs, String finished,
            boolean countFocus) {
        cancelEndAlarm(context);
        int completed = prefs.getInt(PREF_COMPLETED_FOCUS, 0);
        if (countFocus) completed++;
        String next;
        if (PHASE_FOCUS.equals(finished)) {
            next = completed % 4 == 0 ? PHASE_LONG_BREAK : PHASE_SHORT_BREAK;
        } else {
            next = PHASE_FOCUS;
        }
        prefs.edit()
                .putBoolean(PREF_HAS_SESSION, true)
                .putBoolean(PREF_RUNNING, false)
                .putString(PREF_PHASE, next)
                .putLong(PREF_REMAINING_MS, durationForPhase(prefs, next))
                .putInt(PREF_COMPLETED_FOCUS, completed)
                .remove(PREF_END_ELAPSED)
                .remove(PREF_STARTED_ELAPSED)
                .remove(PREF_END_WALL)
                .apply();
        return completed;
    }

    private static void finishIfDueAfterBoot(Context context, SharedPreferences prefs) {
        String finished = normalizePhase(prefs.getString(PREF_PHASE, PHASE_FOCUS));
        moveToNextPhase(context, prefs, finished, PHASE_FOCUS.equals(finished));
    }

    private static long remainingRunningMs(SharedPreferences prefs) {
        long nowElapsed = SystemClock.elapsedRealtime();
        long startedElapsed = prefs.getLong(PREF_STARTED_ELAPSED, -1L);
        if (startedElapsed >= 0L && nowElapsed >= startedElapsed) {
            return prefs.getLong(PREF_END_ELAPSED, nowElapsed) - nowElapsed;
        }
        return prefs.getLong(PREF_END_WALL, 0L) - System.currentTimeMillis();
    }

    private static boolean scheduleEndAlarm(Context context, long remaining) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return false;
        PendingIntent pendingIntent = pendingIntent(context);
        long trigger = SystemClock.elapsedRealtime() + Math.max(1000L, remaining);
        boolean exact = AlarmHelper.canScheduleExactAlarms(context);
        try {
            if (exact && Build.VERSION.SDK_INT >= 23) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pendingIntent);
            } else if (exact && Build.VERSION.SDK_INT >= 19) {
                manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= 23) {
                manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pendingIntent);
            } else {
                manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pendingIntent);
            }
        } catch (SecurityException ignored) {
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pendingIntent);
            exact = false;
        }
        return exact;
    }

    private static void cancelEndAlarm(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pendingIntent(context));
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, PomodoroReceiver.class).setAction(ACTION_FINISHED);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(SettingsActivity.PREFERENCES, Context.MODE_PRIVATE);
    }

    private static String normalizePhase(String phase) {
        return PHASE_SHORT_BREAK.equals(phase) || PHASE_LONG_BREAK.equals(phase) ? phase : PHASE_FOCUS;
    }

    private static long durationForPhase(SharedPreferences prefs, String phase) {
        int minutes = PHASE_SHORT_BREAK.equals(phase)
                ? prefs.getInt(PREF_SHORT_BREAK_MINUTES, DEFAULT_SHORT_BREAK_MINUTES)
                : PHASE_LONG_BREAK.equals(phase)
                ? prefs.getInt(PREF_LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES)
                : prefs.getInt(PREF_FOCUS_MINUTES, DEFAULT_FOCUS_MINUTES);
        return Math.max(1, Math.min(180, minutes)) * MINUTE_MS;
    }
}
