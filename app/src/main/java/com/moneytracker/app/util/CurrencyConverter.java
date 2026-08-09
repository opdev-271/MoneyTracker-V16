package com.moneytracker.app.util;

import android.content.Context;

/** Converts between currencies via EUR as a pivot, using {@link ExchangeRatePrefs}' cached rates. */
public class CurrencyConverter {

    private CurrencyConverter() {
    }

    public static double convert(Context context, double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }
        float fromRate = ExchangeRatePrefs.getRateRelativeToEur(context, fromCurrency);
        float toRate = ExchangeRatePrefs.getRateRelativeToEur(context, toCurrency);
        if (fromRate <= 0) {
            return amount;
        }
        double amountInEur = amount / fromRate;
        return amountInEur * toRate;
    }
}
