package com.moneytracker.app.ui.home;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Profile;
import com.moneytracker.app.data.repository.ProfileRepository;
import com.moneytracker.app.ui.widget.DialogButtonStyler;
import com.moneytracker.app.ui.widget.IconGridAdapter;
import com.moneytracker.app.util.ProfileIcons;

import java.util.List;

/** Creates a new profile: a name plus one pictogram from {@link ProfileIcons}. */
public class AddEditProfileDialogFragment extends DialogFragment {

    public static AddEditProfileDialogFragment newInstance() {
        return new AddEditProfileDialogFragment();
    }

    private EditText editName;
    private IconGridAdapter avatarAdapter;
    private ProfileSwitcherDialogFragment.OnProfileChosenListener listener;

    public void setOnProfileChosenListener(ProfileSwitcherDialogFragment.OnProfileChosenListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_profile, null, false);
        editName = view.findViewById(R.id.edit_name);

        List<IconGridAdapter.IconOption> avatarOptions = ProfileIcons.allOptions();
        avatarAdapter = new IconGridAdapter(avatarOptions, avatarOptions.get(0).key,
                requireContext().getColor(R.color.icon_primary),
                R.drawable.bg_circle_light, R.drawable.bg_circle_light_selected);
        RecyclerView recyclerAvatar = view.findViewById(R.id.recycler_avatar);
        recyclerAvatar.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        recyclerAvatar.setAdapter(avatarAdapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(R.string.add_profile_title)
                .setView(view)
                .setPositiveButton(R.string.button_save, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();
        dialog.setOnShowListener((DialogInterface.OnShowListener) d -> {
            DialogButtonStyler.style(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (trySave()) {
                    dismiss();
                }
            });
        });
        return dialog;
    }

    private boolean trySave() {
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), R.string.profile_name_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        Profile profile = new Profile();
        profile.setName(name);
        profile.setPictogram(avatarAdapter.getSelectedKey());
        profile.setSortOrder(0);

        ProfileRepository profileRepository = MoneyTrackerApp.from(requireContext()).getProfileRepository();
        profileRepository.insert(profile, profileId -> {
            if (listener != null) {
                listener.onProfileChosen(profileId);
            }
        });
        return true;
    }
}
