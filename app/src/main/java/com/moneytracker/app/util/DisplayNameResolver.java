package com.moneytracker.app.util;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import com.moneytracker.app.R;

/**
 * Seed accounts/categories are stored with a stable key (e.g. "category_food")
 * instead of free text so their label can be re-localized after a language
 * switch. User-created accounts/categories have arbitrary free text names and
 * are simply returned as-is.
 */
public class DisplayNameResolver {

    private static final Map<String, Integer> KEY_TO_RES = new HashMap<>();

    static {
        KEY_TO_RES.put("account_cash", R.string.account_cash_default);

        KEY_TO_RES.put("category_food", R.string.category_food);
        KEY_TO_RES.put("category_transport", R.string.category_transport);
        KEY_TO_RES.put("category_shopping", R.string.category_shopping);
        KEY_TO_RES.put("category_bills", R.string.category_bills);
        KEY_TO_RES.put("category_entertainment", R.string.category_entertainment);
        KEY_TO_RES.put("category_health", R.string.category_health);
        KEY_TO_RES.put("category_other_expense", R.string.category_other_expense);
        KEY_TO_RES.put("category_salary", R.string.category_salary);
        KEY_TO_RES.put("category_gift", R.string.category_gift);
        KEY_TO_RES.put("category_other_income", R.string.category_other_income);
    }

    private DisplayNameResolver() {
    }

    public static String resolve(Context context, String rawNameOrKey) {
        if (rawNameOrKey == null) {
            return "";
        }
        Integer resId = KEY_TO_RES.get(rawNameOrKey);
        return resId != null ? context.getString(resId) : rawNameOrKey;
    }
}
