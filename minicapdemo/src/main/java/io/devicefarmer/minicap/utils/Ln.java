package io.devicefarmer.minicap.utils;

import android.util.Log;

import com.android.minicapdemo.BuildConfig;


/**
 * Log both to Android logger (so that logs are visible in "adb logcat") and standard output/error (so that they are visible in the terminal
 * directly).
 */
public final class Ln {

    private static final String TAG = "scrcpy";
    private static final String PREFIX = "[server] ";

    enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static final Level THRESHOLD = BuildConfig.DEBUG ? Level.DEBUG : Level.INFO;

    private Ln() {
    }

    public static boolean isEnabled(Level level) {
        return level.ordinal() >= THRESHOLD.ordinal();
    }

    public static void d(String message) {
        if (isEnabled(Level.DEBUG)) {
            Log.d(TAG, PREFIX + "DEBUG: " + message);
        }
    }

    public static void i(String message) {
        if (isEnabled(Level.INFO)) {
            Log.i(TAG, PREFIX + "INFO: " + message);
        }
    }

    public static void w(String message) {
        if (isEnabled(Level.WARN)) {
            Log.w(TAG, PREFIX + "WARN: " + message);
        }
    }

    public static void e(String message, Throwable throwable) {
        if (isEnabled(Level.ERROR)) {
            Log.e(TAG, (PREFIX + "ERROR: " + message + throwable));
            if (throwable != null) throwable.printStackTrace();
        }
    }

    public static void e(String message) {
        e(message, null);
    }
}
