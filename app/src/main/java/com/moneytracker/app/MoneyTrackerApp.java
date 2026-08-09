package com.moneytracker.app;

import android.app.Application;
import android.content.Context;

import com.moneytracker.app.data.AppDatabase;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.data.repository.CategoryRepository;
import com.moneytracker.app.data.repository.ProfileRepository;
import com.moneytracker.app.data.repository.SavingsEnvelopeRepository;
import com.moneytracker.app.data.repository.TransactionRepository;
import com.moneytracker.app.util.AppExecutors;
import com.moneytracker.app.util.ExchangeRateFetcher;
import com.moneytracker.app.util.ExchangeRatePrefs;
import com.moneytracker.app.util.LocaleHelper;
import com.moneytracker.app.util.ThemePrefs;

import java.io.IOException;
import java.util.Map;

/** Holds app-wide singletons (DB + repositories) so every ViewModel can reach them cheaply. */
public class MoneyTrackerApp extends Application {

    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private ProfileRepository profileRepository;
    private SavingsEnvelopeRepository savingsEnvelopeRepository;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyStoredLocale(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePrefs.applyStoredThemeMode(this);
        AppDatabase database = AppDatabase.getInstance(this);
        AppExecutors executors = AppExecutors.getInstance();
        accountRepository = new AccountRepository(database, executors);
        categoryRepository = new CategoryRepository(database, executors);
        transactionRepository = new TransactionRepository(database, executors);
        profileRepository = new ProfileRepository(database, executors);
        savingsEnvelopeRepository = new SavingsEnvelopeRepository(database, executors);

        executors.diskIO().execute(this::refreshExchangeRates);
    }

    /** Best-effort: on success, caches fresh rates; on any failure (no network, etc.) just
     * silently keeps whatever was cached (or the built-in fallback) - no error UI, not asked for. */
    private void refreshExchangeRates() {
        try {
            Map<String, Double> rates = ExchangeRateFetcher.fetchRatesRelativeToEur();
            ExchangeRatePrefs.setRates(this, rates);
        } catch (IOException ignored) {
            // No network or the feed didn't parse - keep last cached/fallback rates.
        }
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public ProfileRepository getProfileRepository() {
        return profileRepository;
    }

    public SavingsEnvelopeRepository getSavingsEnvelopeRepository() {
        return savingsEnvelopeRepository;
    }

    public static MoneyTrackerApp from(Context context) {
        return (MoneyTrackerApp) context.getApplicationContext();
    }
}
