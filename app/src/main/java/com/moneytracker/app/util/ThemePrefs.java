package com.moneytracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Lets the user pick Light, Dark, or System from Settings. Applying a mode calls
 * {@link AppCompatDelegate#setDefaultNightMode}, which automatically recreates every
 * running AppCompatActivity — no manual recreate() needed (unlike language switching).
 */
public class ThemePrefs {

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    private static final String PREFS_NAME = "settings";
    private static final String KEY_THEME = "theme_mode";

    private ThemePrefs() {
    }

    public static String getThemeMode(Context context) {
        return prefs(context).getString(KEY_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, String mode) {
        prefs(context).edit().putString(KEY_THEME, mode).apply();
        applyThemeMode(mode);
    }

    public static void applyStoredThemeMode(Context context) {
        applyThemeMode(getThemeMode(context));
    }

    private static void applyThemeMode(String mode) {
        int nightMode;
        if (THEME_DARK.equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (THEME_LIGHT.equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
