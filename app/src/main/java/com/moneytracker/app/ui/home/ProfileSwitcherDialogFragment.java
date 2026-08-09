package com.moneytracker.app.ui.home;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Profile;
import com.moneytracker.app.data.repository.ProfileRepository;
import com.moneytracker.app.ui.widget.DialogButtonStyler;
import com.moneytracker.app.util.ProfileIcons;
import com.moneytracker.app.util.SettingsPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every profile (pictogram + name) so the user can switch the active one, with an
 * "add profile" row at the bottom — the same two-tier list/add pattern
 * {@link com.moneytracker.app.ui.addtransaction.CategoryPickerDialogFragment} uses for categories.
 */
public class ProfileSwitcherDialogFragment extends DialogFragment {

    public interface OnProfileChosenListener {
        void onProfileChosen(long profileId);
    }

    public static ProfileSwitcherDialogFragment newInstance() {
        return new ProfileSwitcherDialogFragment();
    }

    private ProfileRepository profileRepository;
    private LinearLayout listContainer;
    private OnProfileChosenListener listener;

    public void setOnProfileChosenListener(OnProfileChosenListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        profileRepository = MoneyTrackerApp.from(requireContext()).getProfileRepository();

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category_picker, null, false);
        listContainer = view.findViewById(R.id.category_list_container);

        profileRepository.getAllProfiles().observe(this, profiles ->
                renderRows(profiles != null ? profiles : new ArrayList<>()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(R.string.profile_switcher_title)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();
        dialog.setOnShowListener(d -> DialogButtonStyler.style(dialog));
        return dialog;
    }

    private void renderRows(List<Profile> profiles) {
        if (listContainer == null) {
            return;
        }
        listContainer.removeAllViews();
        long activeId = SettingsPrefs.getActiveProfileId(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Profile profile : profiles) {
            View row = inflater.inflate(R.layout.item_category_picker_row, listContainer, false);
            FrameLayout badge = row.findViewById(R.id.badge_icon);
            ImageView icon = row.findViewById(R.id.image_icon);
            TextView name = row.findViewById(R.id.text_name);
            View check = row.findViewById(R.id.image_check);

            icon.setImageResource(ProfileIcons.resolve(profile.getPictogram()));
            GradientDrawable badgeBg = (GradientDrawable) badge.getBackground().mutate();
            badgeBg.setColor(requireContext().getColor(R.color.surface_variant));
            name.setText(TextUtils.isEmpty(profile.getName())
                    ? getString(R.string.profile_switcher_title) : profile.getName());
            check.setVisibility(profile.getId() == activeId ? View.VISIBLE : View.GONE);

            row.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileChosen(profile.getId());
                }
                dismiss();
            });
            listContainer.addView(row);
        }

        View addRow = inflater.inflate(R.layout.item_category_picker_add_row, listContainer, false);
        TextView addLabel = addRow.findViewById(R.id.text_add_label);
        addLabel.setText(R.string.add_profile_action);
        addRow.setOnClickListener(v -> {
            AddEditProfileDialogFragment newProfileDialog = AddEditProfileDialogFragment.newInstance();
            newProfileDialog.setOnProfileChosenListener(listener);
            newProfileDialog.show(getParentFragmentManager(), "add_profile");
            dismiss();
        });
        listContainer.addView(addRow);
    }
}
