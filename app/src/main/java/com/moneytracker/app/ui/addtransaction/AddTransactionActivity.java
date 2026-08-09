package com.moneytracker.app.ui.addtransaction;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.entity.Transaction;
import com.moneytracker.app.databinding.ActivityAddTransactionBinding;
import com.moneytracker.app.ui.widget.SegmentedToggle;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.CurrencyFormatter;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.util.LocaleHelper;
import com.moneytracker.app.util.ViewAnimations;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private static final String EXTRA_TYPE = "extra_type";
    private static final String EXTRA_ACCOUNT_ID = "extra_account_id";
    private static final String EXTRA_TRANSACTION_ID = "extra_transaction_id";

    public static Intent newIntent(Context context, String type, long accountId) {
        Intent intent = new Intent(context, AddTransactionActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return intent;
    }

    public static Intent newIntentForEdit(Context context, long transactionId) {
        Intent intent = new Intent(context, AddTransactionActivity.class);
        intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        return intent;
    }

    private ActivityAddTransactionBinding binding;
    private AddTransactionViewModel viewModel;
    private SegmentedToggle typeToggle;
    private boolean editFormPopulated = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyStoredLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ElevationShadows.apply(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);
        viewModel.setInitialType(getIntent().getStringExtra(EXTRA_TYPE));
        viewModel.setInitialAccountId(getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -1L));

        long editTransactionId = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, -1L);
        if (editTransactionId != -1L) {
            binding.textTitle.setText(R.string.edit_transaction_title);
            viewModel.setEditingTransactionId(editTransactionId);
            viewModel.getTransactionById(editTransactionId).observe(this, this::populateForEdit);
        }

        typeToggle = new SegmentedToggle(binding.toggleContainer, binding.toggleIndicator,
                new TextView[]{binding.toggleExpense, binding.toggleIncome},
                getColor(R.color.on_accent_text), getColor(R.color.text_secondary));
        typeToggle.setOnSegmentSelectedListener(index ->
                viewModel.setType(index == 0 ? Category.TYPE_EXPENSE : Category.TYPE_INCOME));

        binding.btnClose.setOnClickListener(v -> closeWithSlideDown());
        binding.rowAccount.setOnClickListener(this::showAccountPicker);
        binding.rowCategory.setOnClickListener(this::showCategoryPicker);
        binding.rowDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> attemptSave());
        ViewAnimations.addPressFeedback(binding.btnSave, binding.btnClose,
                binding.rowAccount, binding.rowCategory, binding.rowDate);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getType().observe(this, type ->
                typeToggle.setSelectedIndex(Category.TYPE_EXPENSE.equals(type) ? 0 : 1));

        viewModel.getAccounts().observe(this, accounts -> updateAccountDisplay());
        viewModel.getSelectedAccountId().observe(this, id -> updateAccountDisplay());

        viewModel.getCategories().observe(this, categories -> updateCategoryDisplay());
        viewModel.getSelectedCategoryId().observe(this, id -> updateCategoryDisplay());

        viewModel.getSelectedDateMillis().observe(this, millis -> {
            if (millis != null) {
                binding.textSelectedDate.setText(
                        new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(millis));
            }
        });

        viewModel.getSavedEvent().observe(this, event -> {
            Boolean saved = event != null ? event.getContentIfNotHandled() : null;
            if (Boolean.TRUE.equals(saved)) {
                closeWithSlideDown();
            }
        });
    }

    private void populateForEdit(Transaction transaction) {
        if (transaction == null || editFormPopulated) {
            return;
        }
        editFormPopulated = true;
        viewModel.setType(transaction.getType());
        viewModel.setSelectedAccountId(transaction.getAccountId());
        if (transaction.getCategoryId() != null) {
            viewModel.setSelectedCategoryId(transaction.getCategoryId());
        }
        viewModel.setSelectedDateMillis(transaction.getDate().getTime());
        binding.editAmount.setText(String.valueOf(transaction.getAmount()));
        binding.editDescription.setText(transaction.getNote());
    }

    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }

    private void updateAccountDisplay() {
        List<Account> accounts = viewModel.getAccounts().getValue();
        Long selectedId = viewModel.getSelectedAccountId().getValue();
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        Account selected = null;
        for (Account account : accounts) {
            if (selectedId != null && account.getId() == selectedId) {
                selected = account;
                break;
            }
        }
        if (selected == null) {
            selected = accounts.get(0);
            viewModel.setSelectedAccountId(selected.getId());
            return;
        }
        binding.textSelectedAccount.setText(DisplayNameResolver.resolve(this, selected.getName()));
        binding.textCurrencySymbol.setText(CurrencyFormatter.symbolFor(selected.getCurrencyCode()));
    }

    private void updateCategoryDisplay() {
        List<Category> categories = viewModel.getCategories().getValue();
        Long selectedId = viewModel.getSelectedCategoryId().getValue();
        if (categories == null) {
            return;
        }
        Category selected = null;
        for (Category category : categories) {
            if (selectedId != null && category.getId() == selectedId) {
                selected = category;
                break;
            }
        }
        binding.textSelectedCategory.setText(
                selected != null ? DisplayNameResolver.resolve(this, selected.getName()) : "");
    }

    private void showAccountPicker(android.view.View anchor) {
        List<Account> accounts = viewModel.getAccounts().getValue();
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        for (Account account : accounts) {
            popupMenu.getMenu().add(0, (int) account.getId(), 0,
                    DisplayNameResolver.resolve(this, account.getName()));
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            viewModel.setSelectedAccountId(item.getItemId());
            return true;
        });
        popupMenu.show();
    }

    private void showCategoryPicker(android.view.View anchor) {
        String currentType = viewModel.getType().getValue();
        CategoryPickerDialogFragment.newInstance(currentType != null ? currentType : Category.TYPE_EXPENSE)
                .show(getSupportFragmentManager(), "category_picker");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        Long current = viewModel.getSelectedDateMillis().getValue();
        if (current != null) {
            calendar.setTimeInMillis(current);
        }
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth);
            viewModel.setSelectedDateMillis(picked.getTimeInMillis());
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void attemptSave() {
        String amountText = binding.editAmount.getText().toString().trim();
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0) {
            Toast.makeText(this, R.string.error_amount_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!viewModel.hasAccount()) {
            Toast.makeText(this, R.string.error_account_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!viewModel.hasCategory()) {
            Toast.makeText(this, R.string.error_category_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String description = binding.editDescription.getText().toString().trim();
        viewModel.save(amount, null, TextUtils.isEmpty(description) ? null : description);
    }
}
