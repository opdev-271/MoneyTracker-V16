package com.moneytracker.app.ui.home;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.moneytracker.app.util.DisplayNameResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checklist of the active profile's accounts to sum into the Home balance card, with a
 * "select all" shortcut. Shares {@link HomeViewModel} with the host {@link HomeFragment}.
 */
public class BalanceAccountsPickerDialogFragment extends DialogFragment {

    public static BalanceAccountsPickerDialogFragment newInstance() {
        return new BalanceAccountsPickerDialogFragment();
    }

    private HomeViewModel viewModel;
    private LinearLayout accountsContainer;
    private final List<CheckBox> checkBoxes = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(HomeViewModel.class);

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_balance_accounts_picker, null, false);
        accountsContainer = view.findViewById(R.id.accounts_container);
        TextView btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnSelectAll.setOnClickListener(v -> {
            for (CheckBox checkBox : checkBoxes) {
                checkBox.setChecked(true);
            }
        });

        List<Account> accounts = viewModel.getAccounts().getValue();
        Set<Long> selectedIds = viewModel.getSelectedAccountIds().getValue();
        renderRows(accounts != null ? accounts : new ArrayList<>(), selectedIds);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(R.string.balance_accounts_picker_title)
                .setView(view)
                .setPositiveButton(R.string.button_save, (d, which) -> saveSelection())
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();
        dialog.setOnShowListener(d -> DialogButtonStyler.style(dialog));
        return dialog;
    }

    private void renderRows(List<Account> accounts, Set<Long> selectedIds) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Account account : accounts) {
            View row = inflater.inflate(R.layout.item_balance_account_row, accountsContainer, false);
            CheckBox checkBox = row.findViewById(R.id.checkbox_account);
            TextView name = row.findViewById(R.id.text_name);
            View dotColor = row.findViewById(R.id.dot_color);

            name.setText(DisplayNameResolver.resolve(requireContext(), account.getName()));
            checkBox.setChecked(selectedIds == null || selectedIds.contains(account.getId()));
            checkBox.setTag(account.getId());

            int color = account.getColor() != 0 ? account.getColor() : ColorSwatchRow.DEFAULT_COLORS[0];
            ((GradientDrawable) dotColor.getBackground().mutate()).setColor(color);

            checkBoxes.add(checkBox);
            accountsContainer.addView(row);
        }
    }

    private void saveSelection() {
        Set<Long> selected = new HashSet<>();
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                selected.add((Long) checkBox.getTag());
            }
        }
        viewModel.setSelectedAccountIds(selected);
    }
}
