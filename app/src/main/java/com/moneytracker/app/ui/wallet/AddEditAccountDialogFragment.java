package com.moneytracker.app.ui.wallet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.ui.widget.ColorSwatchRow;
import com.moneytracker.app.ui.widget.DialogButtonStyler;
import com.moneytracker.app.ui.widget.SegmentedToggle;
import com.moneytracker.app.util.OnboardingPrefs;
import com.moneytracker.app.util.SettingsPrefs;

/** Add/edit dialog shown from {@link WalletFragment}; shares its ViewModel via the parent fragment. */
public class AddEditAccountDialogFragment extends DialogFragment {

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_CURRENCY = "arg_currency";
    private static final String ARG_INITIAL_BALANCE = "arg_initial_balance";
    private static final String ARG_ACCOUNT_NUMBER = "arg_account_number";
    private static final String ARG_COLOR = "arg_color";

    public static AddEditAccountDialogFragment newInstanceForAdd() {
        AddEditAccountDialogFragment fragment = new AddEditAccountDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, 0);
        fragment.setArguments(args);
        return fragment;
    }

    public static AddEditAccountDialogFragment newInstanceForEdit(Account account, String resolvedName) {
        AddEditAccountDialogFragment fragment = new AddEditAccountDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, account.getId());
        args.putString(ARG_NAME, resolvedName);
        args.putString(ARG_TYPE, account.getAccountType());
        args.putString(ARG_CURRENCY, account.getCurrencyCode());
        args.putDouble(ARG_INITIAL_BALANCE, account.getInitialBalance());
        args.putString(ARG_ACCOUNT_NUMBER, account.getAccountNumber());
        args.putInt(ARG_COLOR, account.getColor());
        fragment.setArguments(args);
        return fragment;
    }

    private WalletViewModel viewModel;
    private EditText editName;
    private Spinner spinnerCurrency;
    private EditText editInitialBalance;
    private EditText editAccountNumber;
    private SegmentedToggle typeToggle;
    private ColorSwatchRow colorSwatchRow;
    private String selectedType = Account.TYPE_CASH;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(WalletViewModel.class);

        long accountId = requireArguments().getLong(ARG_ID, 0);
        boolean isEdit = accountId != 0;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_account, null, false);
        editName = view.findViewById(R.id.edit_name);
        spinnerCurrency = view.findViewById(R.id.spinner_currency);
        editInitialBalance = view.findViewById(R.id.edit_initial_balance);
        editAccountNumber = view.findViewById(R.id.edit_account_number);

        selectedType = isEdit ? requireArguments().getString(ARG_TYPE, Account.TYPE_CASH) : Account.TYPE_CASH;
        typeToggle = new SegmentedToggle(view.findViewById(R.id.type_container), view.findViewById(R.id.type_indicator),
                new TextView[]{view.findViewById(R.id.type_cash), view.findViewById(R.id.type_bank),
                        view.findViewById(R.id.type_savings)},
                requireContext().getColor(R.color.on_accent_text), requireContext().getColor(R.color.text_secondary));
        typeToggle.setOnSegmentSelectedListener(index -> {
            selectedType = indexToType(index);
            typeToggle.setSelectedIndex(index);
        });
        typeToggle.setSelectedIndex(typeToIndex(selectedType));

        int existingColor = isEdit ? requireArguments().getInt(ARG_COLOR, 0) : 0;
        int initialColor = existingColor != 0 ? existingColor : ColorSwatchRow.DEFAULT_COLORS[0];
        LinearLayout colorRow = view.findViewById(R.id.color_row);
        colorSwatchRow = new ColorSwatchRow(requireContext(), colorRow, ColorSwatchRow.DEFAULT_COLORS, initialColor);

        if (isEdit) {
            editName.setText(requireArguments().getString(ARG_NAME));
            editInitialBalance.setText(String.valueOf(requireArguments().getDouble(ARG_INITIAL_BALANCE)));
            editAccountNumber.setText(requireArguments().getString(ARG_ACCOUNT_NUMBER));
            String currency = requireArguments().getString(ARG_CURRENCY);
            ArrayAdapter adapter = (ArrayAdapter) spinnerCurrency.getAdapter();
            if (adapter == null) {
                adapter = ArrayAdapter.createFromResource(requireContext(), R.array.currency_codes,
                        android.R.layout.simple_spinner_dropdown_item);
                spinnerCurrency.setAdapter(adapter);
            }
            int position = adapter.getPosition(currency);
            if (position >= 0) {
                spinnerCurrency.setSelection(position);
            }
        } else {
            String preferredCurrency = OnboardingPrefs.getPreferredCurrency(requireContext());
            ArrayAdapter adapter = (ArrayAdapter) spinnerCurrency.getAdapter();
            if (adapter != null) {
                int position = adapter.getPosition(preferredCurrency);
                if (position >= 0) {
                    spinnerCurrency.setSelection(position);
                }
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(isEdit ? R.string.dialog_edit_account_title : R.string.dialog_add_account_title)
                .setView(view)
                .setPositiveButton(R.string.button_save, null)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dismiss());

        if (isEdit) {
            builder.setNeutralButton(R.string.action_delete, (dialog, which) -> {
                String resolvedName = requireArguments().getString(ARG_NAME, "");
                dismiss();
                // Parent manager = WalletFragment's child manager, so the confirmation dialog
                // reaches the same WalletViewModel this one uses.
                DeleteAccountDialogFragment.newInstance(accountId, resolvedName)
                        .show(getParentFragmentManager(), DeleteAccountDialogFragment.TAG);
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface.OnShowListener) d -> {
            DialogButtonStyler.style(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (trySave(accountId)) {
                    dismiss();
                }
            });
        });
        return dialog;
    }

    private boolean trySave(long accountId) {
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), R.string.account_name_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        double initialBalance;
        try {
            initialBalance = Double.parseDouble(editInitialBalance.getText().toString().trim());
        } catch (NumberFormatException e) {
            initialBalance = 0;
        }
        String currency = (String) spinnerCurrency.getSelectedItem();
        String accountNumber = editAccountNumber.getText().toString().trim();

        Account account = new Account();
        account.setId(accountId);
        account.setProfileId(SettingsPrefs.getActiveProfileId(requireContext()));
        account.setName(name);
        account.setAccountType(selectedType);
        account.setCurrencyCode(currency != null ? currency : "CZK");
        account.setInitialBalance(initialBalance);
        account.setAccountNumber(TextUtils.isEmpty(accountNumber) ? null : accountNumber);
        account.setColor(colorSwatchRow.getSelectedColor());
        account.setSortOrder(0);

        viewModel.saveAccount(account);
        return true;
    }

    private static int typeToIndex(String type) {
        if (Account.TYPE_BANK.equals(type)) {
            return 1;
        }
        if (Account.TYPE_SAVINGS.equals(type)) {
            return 2;
        }
        return 0;
    }

    private static String indexToType(int index) {
        switch (index) {
            case 1:
                return Account.TYPE_BANK;
            case 2:
                return Account.TYPE_SAVINGS;
            default:
                return Account.TYPE_CASH;
        }
    }
}
