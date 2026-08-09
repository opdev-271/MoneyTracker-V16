package com.moneytracker.app.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Profile;
import com.moneytracker.app.databinding.ActivityOnboardingBinding;
import com.moneytracker.app.ui.MainActivity;
import com.moneytracker.app.ui.widget.ColorSwatchRow;
import com.moneytracker.app.ui.widget.IconGridAdapter;
import com.moneytracker.app.ui.widget.SegmentedToggle;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.LocaleHelper;
import com.moneytracker.app.util.OnboardingPrefs;
import com.moneytracker.app.util.ProfileIcons;
import com.moneytracker.app.util.SettingsPrefs;
import com.moneytracker.app.util.ThemePrefs;

/**
 * Shown once, on first launch. Language and theme apply immediately (each recreates
 * this Activity — theme via AppCompatDelegate, language via an explicit recreate()),
 * so the user sees the effect live instead of only after "Get started". Everything
 * else the user has entered survives that recreation via onSaveInstanceState.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String[] CURRENCIES = {"CZK", "EUR", "USD", "GBP"};
    private static final String STATE_NAME = "state_name";
    private static final String STATE_PICTOGRAM = "state_pictogram";
    private static final String STATE_CURRENCY = "state_currency";

    private ActivityOnboardingBinding binding;
    private String selectedLanguage;
    private String selectedTheme;
    private String selectedCurrency = "CZK";
    private String selectedPictogram = "pfp_cat";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyStoredLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ElevationShadows.apply(binding.getRoot());

        selectedLanguage = LocaleHelper.getPersistedLanguage(this);
        selectedTheme = ThemePrefs.getThemeMode(this);
        String restoredName = null;
        if (savedInstanceState != null) {
            restoredName = savedInstanceState.getString(STATE_NAME);
            selectedPictogram = savedInstanceState.getString(STATE_PICTOGRAM, selectedPictogram);
            selectedCurrency = savedInstanceState.getString(STATE_CURRENCY, selectedCurrency);
        }
        if (restoredName != null) {
            binding.editName.setText(restoredName);
        }

        int selectedColor = getColor(R.color.on_accent_text);
        int unselectedColor = getColor(R.color.text_secondary);

        IconGridAdapter avatarAdapter = new IconGridAdapter(ProfileIcons.allOptions(), selectedPictogram,
                getColor(R.color.icon_primary), R.drawable.bg_circle_light, R.drawable.bg_circle_light_selected);
        avatarAdapter.setOnIconSelectedListener(key -> selectedPictogram = key);
        binding.recyclerAvatar.setLayoutManager(new GridLayoutManager(this, 4));
        binding.recyclerAvatar.setAdapter(avatarAdapter);

        SegmentedToggle languageToggle = new SegmentedToggle(binding.languageContainer, binding.languageIndicator,
                new TextView[]{binding.optionEnglish, binding.optionCzech, binding.optionGerman},
                selectedColor, unselectedColor);
        languageToggle.setOnSegmentSelectedListener(index -> {
            String newLanguage = languageForIndex(index);
            if (!newLanguage.equals(selectedLanguage)) {
                LocaleHelper.persistLanguage(this, newLanguage);
                recreate();
            }
        });
        languageToggle.setSelectedIndex(indexForLanguage(selectedLanguage));

        SegmentedToggle themeToggle = new SegmentedToggle(binding.themeContainer, binding.themeIndicator,
                new TextView[]{binding.optionThemeLight, binding.optionThemeDark, binding.optionThemeSystem},
                selectedColor, unselectedColor);
        themeToggle.setOnSegmentSelectedListener(index -> {
            String newTheme = themeForIndex(index);
            if (!newTheme.equals(selectedTheme)) {
                // Applies immediately and recreates this Activity on its own.
                ThemePrefs.setThemeMode(this, newTheme);
            }
        });
        themeToggle.setSelectedIndex(indexForTheme(selectedTheme));

        SegmentedToggle currencyToggle = new SegmentedToggle(binding.currencyContainer, binding.currencyIndicator,
                new TextView[]{binding.optionCurrencyCzk, binding.optionCurrencyEur,
                        binding.optionCurrencyUsd, binding.optionCurrencyGbp},
                selectedColor, unselectedColor);
        currencyToggle.setOnSegmentSelectedListener(index -> {
            selectedCurrency = CURRENCIES[index];
            currencyToggle.setSelectedIndex(index);
        });
        currencyToggle.setSelectedIndex(indexForCurrency(selectedCurrency));

        binding.btnGetStarted.setOnClickListener(v -> finishOnboarding());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_NAME, binding.editName.getText().toString());
        outState.putString(STATE_PICTOGRAM, selectedPictogram);
        outState.putString(STATE_CURRENCY, selectedCurrency);
    }

    private void finishOnboarding() {
        String name = binding.editName.getText().toString().trim();
        OnboardingPrefs.setPreferredCurrency(this, selectedCurrency);
        LocaleHelper.persistLanguage(this, selectedLanguage);
        OnboardingPrefs.setDone(this, true);
        ThemePrefs.setThemeMode(this, selectedTheme);

        MoneyTrackerApp app = MoneyTrackerApp.from(this);
        Profile profile = new Profile();
        profile.setName(name);
        profile.setPictogram(selectedPictogram);
        profile.setSortOrder(0);
        app.getProfileRepository().insert(profile, profileId -> {
            SettingsPrefs.setActiveProfileId(this, profileId);

            Account cash = new Account();
            cash.setProfileId(profileId);
            cash.setName("account_cash");
            cash.setAccountType(Account.TYPE_CASH);
            cash.setCurrencyCode(selectedCurrency);
            cash.setInitialBalance(0);
            cash.setColor(ColorSwatchRow.DEFAULT_COLORS[0]);
            cash.setSortOrder(0);
            app.getAccountRepository().insert(cash, accountId -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }

    private static String languageForIndex(int index) {
        switch (index) {
            case 1:
                return LocaleHelper.LANGUAGE_CZECH;
            case 2:
                return LocaleHelper.LANGUAGE_GERMAN;
            default:
                return LocaleHelper.LANGUAGE_ENGLISH;
        }
    }

    private static int indexForLanguage(String language) {
        if (LocaleHelper.LANGUAGE_CZECH.equals(language)) {
            return 1;
        }
        if (LocaleHelper.LANGUAGE_GERMAN.equals(language)) {
            return 2;
        }
        return 0;
    }

    private static String themeForIndex(int index) {
        switch (index) {
            case 1:
                return ThemePrefs.THEME_DARK;
            case 2:
                return ThemePrefs.THEME_SYSTEM;
            default:
                return ThemePrefs.THEME_LIGHT;
        }
    }

    private static int indexForTheme(String theme) {
        if (ThemePrefs.THEME_DARK.equals(theme)) {
            return 1;
        }
        if (ThemePrefs.THEME_SYSTEM.equals(theme)) {
            return 2;
        }
        return 0;
    }

    private static int indexForCurrency(String currency) {
        for (int i = 0; i < CURRENCIES.length; i++) {
            if (CURRENCIES[i].equals(currency)) {
                return i;
            }
        }
        return 0;
    }
}
