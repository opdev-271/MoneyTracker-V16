package com.moneytracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * Lets the user pick English or Czech from Settings, independent of the
 * device's system language. The chosen code is persisted and re-applied to
 * every Activity's base context in {@code attachBaseContext}.
 */
public class LocaleHelper {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_CZECH = "cs";
    public static final String LANGUAGE_GERMAN = "de";

    private static final String PREFS_NAME = "settings";
    private static final String KEY_LANGUAGE = "language";

    private LocaleHelper() {
    }

    public static Context applyStoredLocale(Context context) {
        String language = getPersistedLanguage(context);
        return setLocale(context, language);
    }

    public static String getPersistedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String systemDefault = Locale.getDefault().getLanguage();
        String fallback;
        if (LANGUAGE_CZECH.equals(systemDefault)) {
            fallback = LANGUAGE_CZECH;
        } else if (LANGUAGE_GERMAN.equals(systemDefault)) {
            fallback = LANGUAGE_GERMAN;
        } else {
            fallback = LANGUAGE_ENGLISH;
        }
        return prefs.getString(KEY_LANGUAGE, fallback);
    }

    public static void persistLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public static Context setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration);
        } else {
            context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
            return context;
        }
    }
}
