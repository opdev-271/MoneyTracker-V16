package com.moneytracker.app.ui.alltransactions;

import android.content.Context;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.util.AppExecutors;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.util.XlsxWriter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the .xlsx export for the "all transactions" screen. Per-row "account balance"
 * is the account's running balance immediately after that transaction (like a bank
 * statement); the optional summary block at the end shows each account's *current*
 * balance (as of export time) plus a grand total.
 */
public class TransactionExporter {

    public static class ColumnSelection {
        public boolean date = true;
        public boolean amount = true;
        public boolean type = true;
        public boolean account = true;
        public boolean category = true;
        public boolean note = true;
        public boolean accountBalance = true;
        public boolean allAccountsSummary = true;
    }

    public interface OnExportListener {
        void onExported(File file);

        void onError(Exception e);
    }

    private TransactionExporter() {
    }

    public static void export(Context context, List<TransactionWithDetails> transactions,
                               AccountRepository accountRepository, ColumnSelection columns,
                               OnExportListener listener) {
        Context appContext = context.getApplicationContext();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                File file = doExport(appContext, transactions, accountRepository, columns);
                AppExecutors.getInstance().mainThread().execute(() -> listener.onExported(file));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> listener.onError(e));
            }
        });
    }

    private static File doExport(Context context, List<TransactionWithDetails> transactions,
                                  AccountRepository accountRepository, ColumnSelection columns) throws java.io.IOException {
        List<Long> accountIds = new ArrayList<>();
        for (TransactionWithDetails t : transactions) {
            if (!accountIds.contains(t.accountId)) {
                accountIds.add(t.accountId);
            }
        }
        List<Account> accounts = accountIds.isEmpty()
                ? new ArrayList<>() : accountRepository.getAccountsByIdsSync(accountIds);
        Map<Long, Account> accountById = new HashMap<>();
        for (Account account : accounts) {
            accountById.put(account.getId(), account);
        }

        Map<Long, List<TransactionWithDetails>> byAccount = new HashMap<>();
        for (TransactionWithDetails t : transactions) {
            List<TransactionWithDetails> list = byAccount.get(t.accountId);
            if (list == null) {
                list = new ArrayList<>();
                byAccount.put(t.accountId, list);
            }
            list.add(t);
        }

        Map<Long, Double> runningBalanceAfter = new HashMap<>();
        Map<Long, Double> currentBalance = new HashMap<>();
        for (Long accountId : byAccount.keySet()) {
            List<TransactionWithDetails> list = byAccount.get(accountId);
            Collections.sort(list, (a, b) -> {
                int byDate = a.date.compareTo(b.date);
                if (byDate != 0) {
                    return byDate;
                }
                return a.id < b.id ? -1 : (a.id > b.id ? 1 : 0);
            });
            Account account = accountById.get(accountId);
            double running = account != null ? account.getInitialBalance() : 0;
            for (TransactionWithDetails t : list) {
                running += t.getSignedAmount();
                runningBalanceAfter.put(t.id, running);
            }
            currentBalance.put(accountId, running);
        }

        List<Object> header = new ArrayList<>();
        if (columns.date) {
            header.add(context.getString(R.string.export_column_date));
        }
        if (columns.amount) {
            header.add(context.getString(R.string.export_column_amount));
        }
        if (columns.type) {
            header.add(context.getString(R.string.export_column_type));
        }
        if (columns.account) {
            header.add(context.getString(R.string.export_column_account));
        }
        if (columns.category) {
            header.add(context.getString(R.string.export_column_category));
        }
        if (columns.note) {
            header.add(context.getString(R.string.export_column_note));
        }
        if (columns.accountBalance) {
            header.add(context.getString(R.string.export_column_account_balance));
        }

        List<XlsxWriter.Row> rows = new ArrayList<>();
        rows.add(new XlsxWriter.Row(header, true));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        for (TransactionWithDetails t : transactions) {
            boolean isExpense = Category.TYPE_EXPENSE.equals(t.type);
            List<Object> row = new ArrayList<>();
            if (columns.date) {
                row.add(dateFormat.format(t.date));
            }
            if (columns.amount) {
                row.add(isExpense ? -t.amount : t.amount);
            }
            if (columns.type) {
                row.add(context.getString(isExpense ? R.string.label_expense : R.string.label_income));
            }
            if (columns.account) {
                row.add(DisplayNameResolver.resolve(context, t.accountName));
            }
            if (columns.category) {
                row.add(DisplayNameResolver.resolve(context, t.categoryName));
            }
            if (columns.note) {
                row.add(t.note != null ? t.note : "");
            }
            if (columns.accountBalance) {
                Double balance = runningBalanceAfter.get(t.id);
                row.add(balance != null ? balance : 0d);
            }
            rows.add(new XlsxWriter.Row(row, false));
        }

        if (columns.allAccountsSummary) {
            rows.add(new XlsxWriter.Row(new ArrayList<>(), false));
            List<Object> summaryTitle = new ArrayList<>();
            summaryTitle.add(context.getString(R.string.export_summary_title));
            rows.add(new XlsxWriter.Row(summaryTitle, true));

            List<Object> summaryHeader = new ArrayList<>();
            summaryHeader.add(context.getString(R.string.export_column_account));
            summaryHeader.add(context.getString(R.string.export_column_account_balance));
            rows.add(new XlsxWriter.Row(summaryHeader, true));

            double total = 0;
            for (Account account : accounts) {
                Double balance = currentBalance.get(account.getId());
                double value = balance != null ? balance : account.getInitialBalance();
                total += value;
                List<Object> summaryRow = new ArrayList<>();
                summaryRow.add(DisplayNameResolver.resolve(context, account.getName()));
                summaryRow.add(value);
                rows.add(new XlsxWriter.Row(summaryRow, false));
            }
            List<Object> totalRow = new ArrayList<>();
            totalRow.add(context.getString(R.string.export_summary_total));
            totalRow.add(total);
            rows.add(new XlsxWriter.Row(totalRow, true));
        }

        File dir = new File(context.getCacheDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new java.io.IOException("Could not create export directory");
        }
        File file = new File(dir, "moneytracker_export_" + System.currentTimeMillis() + ".xlsx");
        XlsxWriter.write(file, context.getString(R.string.export_sheet_name), rows);
        return file;
    }
}
