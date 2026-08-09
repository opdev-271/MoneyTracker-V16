package com.moneytracker.app.util;

import android.content.Context;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Category;

/**
 * Resolves the amount-text color for a transaction row, respecting the user's
 * "color-code transactions" setting (see SettingsPrefs). Income is always green;
 * expense is red only when the setting is on, otherwise it stays the neutral
 * text color — matching the app's pre-toggle behavior.
 */
public class TransactionColors {

    private TransactionColors() {
    }

    public static int amountColor(Context context, String type) {
        boolean isExpense = Category.TYPE_EXPENSE.equals(type);
        if (!isExpense) {
            return context.getColor(R.color.income_green);
        }
        boolean colorCode = SettingsPrefs.isColorCodeTransactions(context);
        return context.getColor(colorCode ? R.color.expense_red : R.color.text_primary);
    }
}
