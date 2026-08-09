package com.moneytracker.app.ui.wallet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.R;
import com.moneytracker.app.ui.widget.DialogButtonStyler;

/**
 * Confirmation in front of {@link WalletViewModel#deleteAccount(long)}: deleting an account
 * cascades to every transaction and savings envelope on it, so it must not happen on a single
 * stray tap. Shown from {@link WalletFragment}'s child fragment manager (also when the edit
 * dialog triggers it), so {@code requireParentFragment()} resolves to the ViewModel owner.
 */
public class DeleteAccountDialogFragment extends DialogFragment {

    public static final String TAG = "delete_account";

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";

    public static DeleteAccountDialogFragment newInstance(long accountId, String resolvedName) {
        DeleteAccountDialogFragment fragment = new DeleteAccountDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, accountId);
        args.putString(ARG_NAME, resolvedName);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        WalletViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(WalletViewModel.class);
        long accountId = requireArguments().getLong(ARG_ID);
        String name = requireArguments().getString(ARG_NAME, "");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(R.string.dialog_delete_account_title)
                .setMessage(getString(R.string.dialog_delete_account_message, name))
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .setPositiveButton(R.string.action_delete, (d, which) -> viewModel.deleteAccount(accountId))
                .create();

        dialog.setOnShowListener((DialogInterface.OnShowListener) d -> {
            DialogButtonStyler.style(dialog);
            // Here the destructive action is the confirming one, so it - not the neutral slot -
            // gets the warning color.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(requireContext().getColor(R.color.expense_red));
        });
        return dialog;
    }
}
