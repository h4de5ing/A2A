package org.las2mile.scrcpy;

import android.util.Log;

public final class Ln {
    private static final String TAG = "scrcpy";
    private static final Level THRESHOLD = BuildConfig.DEBUG ? Level.DEBUG : Level.INFO;

    public static boolean isEnabled(Level level) {
        return level.ordinal() >= THRESHOLD.ordinal();
    }

    public static void d(String message) {
        if (isEnabled(Level.DEBUG)) {
            Log.d(TAG, message);
            System.out.println("DEBUG: " + message);
        }
    }

    enum Level {
        DEBUG,
        INFO,
    }
}
