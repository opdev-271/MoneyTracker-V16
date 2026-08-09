package com.moneytracker.app.ui.alltransactions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.databinding.ActivityAllTransactionsBinding;
import com.moneytracker.app.ui.widget.OnTransactionInteractionListener;
import com.moneytracker.app.ui.widget.TransactionSelectionController;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.LocaleHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AllTransactionsActivity extends AppCompatActivity {

    private static final String EXTRA_ACCOUNT_IDS = "extra_account_ids";

    public static Intent newIntent(Context context, List<Long> accountIds) {
        long[] ids = new long[accountIds.size()];
        for (int i = 0; i < accountIds.size(); i++) {
            ids[i] = accountIds.get(i);
        }
        Intent intent = new Intent(context, AllTransactionsActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_IDS, ids);
        return intent;
    }

    private ActivityAllTransactionsBinding binding;
    private AllTransactionsAdapter adapter;
    private TransactionSelectionController selectionController;
    private List<TransactionWithDetails> latestTransactions = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyStoredLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
        binding = ActivityAllTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ElevationShadows.apply(binding.getRoot());

        selectionController = new TransactionSelectionController(this,
                MoneyTrackerApp.from(this).getTransactionRepository(),
                new TransactionSelectionController.Host() {
                    @Override
                    public TransactionWithDetails findTransactionById(long id) {
                        for (TransactionWithDetails t : latestTransactions) {
                            if (t.id == id) {
                                return t;
                            }
                        }
                        return null;
                    }

                    @Override
                    public void onSelectionChanged(Set<Long> selectedIds) {
                        if (adapter != null) {
                            adapter.setSelectedIds(selectedIds);
                        }
                    }
                });

        adapter = new AllTransactionsAdapter(new OnTransactionInteractionListener() {
            @Override
            public void onTransactionLongClick(long transactionId) {
                selectionController.onItemLongClicked(transactionId);
            }

            @Override
            public void onTransactionClick(long transactionId) {
                selectionController.onItemClicked(transactionId);
            }
        });
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTransactions.setAdapter(adapter);

        binding.btnClose.setOnClickListener(v -> closeWithSlideDown());
        binding.btnExport.setOnClickListener(v -> showExportDialog());

        long[] ids = getIntent().getLongArrayExtra(EXTRA_ACCOUNT_IDS);
        List<Long> accountIds = new ArrayList<>();
        if (ids != null) {
            for (long id : ids) {
                accountIds.add(id);
            }
        }

        AllTransactionsViewModel viewModel = new ViewModelProvider(this).get(AllTransactionsViewModel.class);
        viewModel.setAccountIds(accountIds);
        viewModel.getTransactions().observe(this, transactions -> {
            latestTransactions = transactions != null ? transactions : new ArrayList<>();
            adapter.submitList(AllTransactionsAdapter.buildItems(transactions));
            boolean empty = latestTransactions.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void showExportDialog() {
        if (latestTransactions.isEmpty()) {
            Toast.makeText(this, R.string.empty_transactions, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = {
                getString(R.string.export_column_date),
                getString(R.string.export_column_amount),
                getString(R.string.export_column_type),
                getString(R.string.export_column_account),
                getString(R.string.export_column_category),
                getString(R.string.export_column_note),
                getString(R.string.export_column_account_balance),
                getString(R.string.export_column_all_balance),
        };
        boolean[] checked = {true, true, true, true, true, true, true, true};

        new AlertDialog.Builder(this)
                .setTitle(R.string.export_columns_title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.export_positive_button, (dialog, which) -> runExport(checked))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void runExport(boolean[] checked) {
        TransactionExporter.ColumnSelection columns = new TransactionExporter.ColumnSelection();
        columns.date = checked[0];
        columns.amount = checked[1];
        columns.type = checked[2];
        columns.account = checked[3];
        columns.category = checked[4];
        columns.note = checked[5];
        columns.accountBalance = checked[6];
        columns.allAccountsSummary = checked[7];

        AccountRepository accountRepository = MoneyTrackerApp.from(this).getAccountRepository();
        TransactionExporter.export(this, latestTransactions, accountRepository, columns, new TransactionExporter.OnExportListener() {
            @Override
            public void onExported(File file) {
                shareFile(file);
            }

            @Override
            public void onError(Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Toast.makeText(AllTransactionsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_action)));
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
}
