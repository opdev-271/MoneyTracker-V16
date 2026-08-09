package com.moneytracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small app-wide flags that live outside Room (language is handled by LocaleHelper). */
public class SettingsPrefs {

    private static final String PREFS_NAME = "settings";
    private static final String KEY_HIDE_BALANCE = "hide_balance";
    private static final String KEY_ACTIVE_ACCOUNT_ID = "active_account_id";
    private static final String KEY_ACTIVE_PROFILE_ID = "active_profile_id";
    private static final String KEY_COLOR_CODE_TRANSACTIONS = "color_code_transactions";
    private static final String KEY_ANIMATIONS_ENABLED = "animations_enabled";
    private static final String KEY_BALANCE_ACCOUNTS_PREFIX = "balance_accounts_";
    private static final String BALANCE_ACCOUNTS_ALL = "ALL";
    private static final String KEY_HOME_SHOW_BALANCE = "home_show_balance";
    private static final String KEY_HOME_SHOW_ACTIONS = "home_show_actions";
    private static final String KEY_HOME_SHOW_ACCOUNTS = "home_show_accounts";
    private static final String KEY_HOME_SHOW_TRANSACTIONS = "home_show_transactions";

    /** The id of the very first profile ever created (see OnboardingActivity). */
    public static final long DEFAULT_PROFILE_ID = 1L;

    private SettingsPrefs() {
    }

    /**
     * Single shared source of truth for the hide-balance flag. Home's eye button and the
     * Settings switch each used to keep their own copy in their own ViewModel, seeded once at
     * construction, so whichever screen was already alive kept showing the stale state after
     * the other one flipped it. Both now observe this instead.
     */
    private static final MutableLiveData<Boolean> balanceHidden = new MutableLiveData<>();

    public static boolean isBalanceHidden(Context context) {
        return prefs(context).getBoolean(KEY_HIDE_BALANCE, false);
    }

    /** Main thread only - the value is read back synchronously right after every write. */
    public static LiveData<Boolean> observeBalanceHidden(Context context) {
        if (balanceHidden.getValue() == null) {
            balanceHidden.setValue(isBalanceHidden(context));
        }
        return balanceHidden;
    }

    public static void setBalanceHidden(Context context, boolean hidden) {
        prefs(context).edit().putBoolean(KEY_HIDE_BALANCE, hidden).apply();
        balanceHidden.setValue(hidden);
    }

    public static long getActiveAccountId(Context context) {
        return prefs(context).getLong(KEY_ACTIVE_ACCOUNT_ID, -1L);
    }

    public static void setActiveAccountId(Context context, long accountId) {
        prefs(context).edit().putLong(KEY_ACTIVE_ACCOUNT_ID, accountId).apply();
    }

    public static long getActiveProfileId(Context context) {
        return prefs(context).getLong(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE_ID);
    }

    public static void setActiveProfileId(Context context, long profileId) {
        prefs(context).edit().putLong(KEY_ACTIVE_PROFILE_ID, profileId).apply();
    }

    public static boolean isColorCodeTransactions(Context context) {
        return prefs(context).getBoolean(KEY_COLOR_CODE_TRANSACTIONS, true);
    }

    public static void setColorCodeTransactions(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_COLOR_CODE_TRANSACTIONS, enabled).apply();
    }

    public static boolean isAnimationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ANIMATIONS_ENABLED, true);
    }

    public static void setAnimationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ANIMATIONS_ENABLED, enabled).apply();
    }

    /**
     * Which of a profile's accounts should be summed into the Home balance card.
     * {@code null} means "all accounts" (also the default before the user picks anything).
     */
    public static Set<Long> getBalanceAccountSelection(Context context, long profileId) {
        String stored = prefs(context).getString(KEY_BALANCE_ACCOUNTS_PREFIX + profileId, BALANCE_ACCOUNTS_ALL);
        if (BALANCE_ACCOUNTS_ALL.equals(stored) || stored.isEmpty()) {
            return null;
        }
        Set<Long> ids = new HashSet<>();
        for (String part : stored.split(",")) {
            try {
                ids.add(Long.parseLong(part));
            } catch (NumberFormatException ignored) {
                // skip malformed entry
            }
        }
        return ids;
    }

    /** Pass {@code null} (or a set covering every account) to mean "all accounts". */
    public static void setBalanceAccountSelection(Context context, long profileId, Set<Long> accountIds) {
        String value;
        if (accountIds == null) {
            value = BALANCE_ACCOUNTS_ALL;
        } else {
            List<String> parts = new ArrayList<>();
            for (Long id : accountIds) {
                parts.add(String.valueOf(id));
            }
            value = TextUtils.join(",", parts);
        }
        prefs(context).edit().putString(KEY_BALANCE_ACCOUNTS_PREFIX + profileId, value).apply();
    }

    public static boolean isHomeSectionVisible(Context context, HomeSection section) {
        return prefs(context).getBoolean(section.prefKey, true);
    }

    public static void setHomeSectionVisible(Context context, HomeSection section, boolean visible) {
        prefs(context).edit().putBoolean(section.prefKey, visible).apply();
    }

    public enum HomeSection {
        BALANCE(KEY_HOME_SHOW_BALANCE),
        ACTIONS(KEY_HOME_SHOW_ACTIONS),
        ACCOUNTS(KEY_HOME_SHOW_ACCOUNTS),
        TRANSACTIONS(KEY_HOME_SHOW_TRANSACTIONS);

        private final String prefKey;

        HomeSection(String prefKey) {
            this.prefKey = prefKey;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
