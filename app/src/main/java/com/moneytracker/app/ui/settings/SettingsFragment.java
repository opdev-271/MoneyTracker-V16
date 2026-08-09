package com.moneytracker.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.databinding.FragmentSettingsBinding;
import com.moneytracker.app.ui.categories.CategoriesActivity;
import com.moneytracker.app.ui.widget.SegmentedToggle;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.LocaleHelper;
import com.moneytracker.app.util.SettingsPrefs;
import com.moneytracker.app.util.ThemePrefs;
import com.moneytracker.app.util.ViewAnimations;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private SegmentedToggle languageToggle;
    private SegmentedToggle themeToggle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        ViewAnimations.fadeInContent(view);
        ElevationShadows.apply(view);

        long activeProfileId = SettingsPrefs.getActiveProfileId(requireContext());
        MoneyTrackerApp.from(requireContext()).getProfileRepository().getProfileById(activeProfileId)
                .observe(getViewLifecycleOwner(), profile -> {
                    if (profile != null && !TextUtils.isEmpty(profile.getName())) {
                        binding.cardProfile.setVisibility(View.VISIBLE);
                        binding.textProfileName.setText(profile.getName());
                    } else {
                        binding.cardProfile.setVisibility(View.GONE);
                    }
                });

        int selectedColor = requireContext().getColor(R.color.on_accent_text);
        int unselectedColor = requireContext().getColor(R.color.text_secondary);

        languageToggle = new SegmentedToggle(binding.languageContainer, binding.languageIndicator,
                new TextView[]{binding.optionEnglish, binding.optionCzech, binding.optionGerman},
                selectedColor, unselectedColor);
        languageToggle.setOnSegmentSelectedListener(index -> changeLanguage(languageForIndex(index)));

        themeToggle = new SegmentedToggle(binding.themeContainer, binding.themeIndicator,
                new TextView[]{binding.optionThemeLight, binding.optionThemeDark, binding.optionThemeSystem},
                selectedColor, unselectedColor);
        themeToggle.setOnSegmentSelectedListener(index -> viewModel.setTheme(themeForIndex(index)));

        binding.cardCategories.setOnClickListener(v ->
                startActivity(CategoriesActivity.newIntent(requireContext())));
        ViewAnimations.addPressFeedback(binding.cardCategories);

        binding.switchHideBalance.setOnClickListener(v ->
                viewModel.setHideBalance(binding.switchHideBalance.isChecked()));
        binding.switchColorCodeTransactions.setOnClickListener(v ->
                viewModel.setColorCodeTransactions(binding.switchColorCodeTransactions.isChecked()));
        binding.switchDisableAnimations.setOnClickListener(v ->
                viewModel.setAnimationsEnabled(!binding.switchDisableAnimations.isChecked()));
        binding.switchHomeShowBalance.setOnClickListener(v ->
                viewModel.setHomeShowBalance(binding.switchHomeShowBalance.isChecked()));
        binding.switchHomeShowActions.setOnClickListener(v ->
                viewModel.setHomeShowActions(binding.switchHomeShowActions.isChecked()));
        binding.switchHomeShowAccounts.setOnClickListener(v ->
                viewModel.setHomeShowAccounts(binding.switchHomeShowAccounts.isChecked()));
        binding.switchHomeShowTransactions.setOnClickListener(v ->
                viewModel.setHomeShowTransactions(binding.switchHomeShowTransactions.isChecked()));

        viewModel.getLanguage().observe(getViewLifecycleOwner(), language ->
                languageToggle.setSelectedIndex(indexForLanguage(language)));
        viewModel.getTheme().observe(getViewLifecycleOwner(), theme ->
                themeToggle.setSelectedIndex(indexForTheme(theme)));
        viewModel.getHideBalance().observe(getViewLifecycleOwner(), hidden ->
                binding.switchHideBalance.setChecked(Boolean.TRUE.equals(hidden)));
        viewModel.getColorCodeTransactions().observe(getViewLifecycleOwner(), enabled ->
                binding.switchColorCodeTransactions.setChecked(Boolean.TRUE.equals(enabled)));
        viewModel.getAnimationsEnabled().observe(getViewLifecycleOwner(), enabled ->
                binding.switchDisableAnimations.setChecked(!Boolean.TRUE.equals(enabled)));
        viewModel.getHomeShowBalance().observe(getViewLifecycleOwner(), visible ->
                binding.switchHomeShowBalance.setChecked(Boolean.TRUE.equals(visible)));
        viewModel.getHomeShowActions().observe(getViewLifecycleOwner(), visible ->
                binding.switchHomeShowActions.setChecked(Boolean.TRUE.equals(visible)));
        viewModel.getHomeShowAccounts().observe(getViewLifecycleOwner(), visible ->
                binding.switchHomeShowAccounts.setChecked(Boolean.TRUE.equals(visible)));
        viewModel.getHomeShowTransactions().observe(getViewLifecycleOwner(), visible ->
                binding.switchHomeShowTransactions.setChecked(Boolean.TRUE.equals(visible)));
    }

    private void changeLanguage(String language) {
        if (viewModel.setLanguage(language)) {
            requireActivity().recreate();
        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
