package com.moneytracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached currency exchange rates (units per 1 EUR), refreshed in the background on
 * app start by {@link ExchangeRateFetcher} via MoneyTrackerApp. Seeded with rough
 * approximate fallback rates so conversion is never wildly wrong even before the
 * first successful fetch (e.g. first-ever launch offline).
 */
public class ExchangeRatePrefs {

    private static final String PREFS_NAME = "exchange_rates";
    private static final String KEY_RATE_PREFIX = "rate_";
    private static final String KEY_LAST_UPDATED = "last_updated_millis";

    private static final Map<String, Float> FALLBACK_RATES = new HashMap<>();

    static {
        FALLBACK_RATES.put("EUR", 1f);
        FALLBACK_RATES.put("USD", 1.08f);
        FALLBACK_RATES.put("GBP", 0.85f);
        FALLBACK_RATES.put("CZK", 25f);
    }

    /** Ticks (postValue) after every successful background refresh, so a screen already
     * on-screen can recompute without waiting for the next cold start. The value itself
     * carries no meaning - only that a refresh just happened. */
    public static final MutableLiveData<Long> ratesUpdatedSignal = new MutableLiveData<>();

    private ExchangeRatePrefs() {
    }

    public static float getRateRelativeToEur(Context context, String currencyCode) {
        if ("EUR".equals(currencyCode)) {
            return 1f;
        }
        Float fallback = FALLBACK_RATES.get(currencyCode);
        float fallbackValue = fallback != null ? fallback : 1f;
        return prefs(context).getFloat(KEY_RATE_PREFIX + currencyCode, fallbackValue);
    }

    /** Call off the main thread; safe to call from a background fetch. */
    public static void setRates(Context context, Map<String, Double> ratesRelativeToEur) {
        SharedPreferences.Editor editor = prefs(context).edit();
        for (Map.Entry<String, Double> entry : ratesRelativeToEur.entrySet()) {
            editor.putFloat(KEY_RATE_PREFIX + entry.getKey(), entry.getValue().floatValue());
        }
        editor.putLong(KEY_LAST_UPDATED, System.currentTimeMillis());
        editor.apply();
        ratesUpdatedSignal.postValue(System.currentTimeMillis());
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
