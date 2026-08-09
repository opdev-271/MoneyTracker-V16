package com.moneytracker.app.ui.savings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.ui.widget.DialogButtonStyler;
import com.moneytracker.app.util.DisplayNameResolver;

import java.util.ArrayList;
import java.util.List;

/** "Send money" dialog for one envelope: pick a source account, enter an amount. */
public class DepositEnvelopeDialogFragment extends DialogFragment {

    private static final String ARG_ENVELOPE_ID = "arg_envelope_id";
    private static final String ARG_ENVELOPE_NAME = "arg_envelope_name";

    public static DepositEnvelopeDialogFragment newInstance(long envelopeId, String envelopeName) {
        DepositEnvelopeDialogFragment fragment = new DepositEnvelopeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ENVELOPE_ID, envelopeId);
        args.putString(ARG_ENVELOPE_NAME, envelopeName);
        fragment.setArguments(args);
        return fragment;
    }

    private Spinner spinnerAccount;
    private EditText editAmount;
    private List<Account> accounts = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SavingsEnvelopesActivity host = (SavingsEnvelopesActivity) requireActivity();
        SavingsEnvelopesViewModel viewModel = host.getViewModel();

        long envelopeId = requireArguments().getLong(ARG_ENVELOPE_ID);
        String envelopeName = requireArguments().getString(ARG_ENVELOPE_NAME, "");

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_deposit_envelope, null, false);
        spinnerAccount = view.findViewById(R.id.spinner_source_account);
        editAmount = view.findViewById(R.id.edit_amount);

        viewModel.getProfileAccounts().observe(this, list -> {
            accounts = list != null ? list : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Account account : accounts) {
                names.add(DisplayNameResolver.resolve(requireContext(), account.getName()));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, names);
            spinnerAccount.setAdapter(adapter);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(getString(R.string.savings_deposit_title, envelopeName))
                .setView(view)
                .setPositiveButton(R.string.savings_deposit_action, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();
        dialog.setOnShowListener((DialogInterface.OnShowListener) d -> {
            DialogButtonStyler.style(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (trySend(viewModel, envelopeId)) {
                    dismiss();
                }
            });
        });
        return dialog;
    }

    private boolean trySend(SavingsEnvelopesViewModel viewModel, long envelopeId) {
        int position = spinnerAccount.getSelectedItemPosition();
        if (position < 0 || position >= accounts.size()) {
            return false;
        }
        double amount;
        try {
            amount = Double.parseDouble(editAmount.getText().toString().trim());
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0) {
            Toast.makeText(requireContext(), R.string.error_amount_positive, Toast.LENGTH_SHORT).show();
            return false;
        }
        long sourceAccountId = accounts.get(position).getId();
        viewModel.depositToEnvelope(envelopeId, sourceAccountId, amount, () -> {
        });
        return true;
    }
}
