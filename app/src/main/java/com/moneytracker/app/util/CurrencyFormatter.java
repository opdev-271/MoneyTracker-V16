package com.moneytracker.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Formats amounts as "1,234.56" plus a currency symbol, independent of device locale. */
public class CurrencyFormatter {

    private static final Map<String, String> SYMBOLS = new HashMap<>();

    static {
        SYMBOLS.put("CZK", "Kč");
        SYMBOLS.put("EUR", "€");
        SYMBOLS.put("USD", "$");
        SYMBOLS.put("GBP", "£");
    }

    private CurrencyFormatter() {
    }

    public static String symbolFor(String currencyCode) {
        String symbol = SYMBOLS.get(currencyCode);
        return symbol != null ? symbol : (currencyCode == null ? "" : currencyCode);
    }

    /** e.g. "$34,567.90" or "34 567,90 Kč" */
    public static String format(double amount, String currencyCode) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        String number = format.format(Math.abs(amount));
        String symbol = symbolFor(currencyCode);
        String sign = amount < 0 ? "-" : "";
        if ("Kč".equals(symbol)) {
            return sign + number + " " + symbol;
        }
        return sign + symbol + number;
    }

    /** e.g. "+$1,923" or "-1 532 Kč" for transaction list rows (no decimals). */
    public static String formatSigned(double signedAmount, String currencyCode) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        String number = format.format(Math.abs(signedAmount));
        String symbol = symbolFor(currencyCode);
        String sign = signedAmount < 0 ? "-" : "+";
        if ("Kč".equals(symbol)) {
            return sign + number + " " + symbol;
        }
        return sign + symbol + number;
    }
}
