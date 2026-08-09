package com.moneytracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/** First-launch questionnaire answers (preferred currency) and whether it's been completed. */
public class OnboardingPrefs {

    private static final String PREFS_NAME = "settings";
    private static final String KEY_DONE = "onboarding_done";
    private static final String KEY_PREFERRED_CURRENCY = "preferred_currency";

    private OnboardingPrefs() {
    }

    public static boolean isDone(Context context) {
        return prefs(context).getBoolean(KEY_DONE, false);
    }

    public static void setDone(Context context, boolean done) {
        prefs(context).edit().putBoolean(KEY_DONE, done).apply();
    }

    public static String getPreferredCurrency(Context context) {
        return prefs(context).getString(KEY_PREFERRED_CURRENCY, "CZK");
    }

    public static void setPreferredCurrency(Context context, String currency) {
        prefs(context).edit().putString(KEY_PREFERRED_CURRENCY, currency).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
