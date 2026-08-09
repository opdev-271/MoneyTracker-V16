package com.moneytracker.app.ui.widget;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Transaction;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.data.repository.TransactionRepository;
import com.moneytracker.app.ui.addtransaction.AddTransactionActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * Long-press-to-select for a list of transaction rows, shared by Home's recent list
 * and the "all transactions" screen: first long-press starts a contextual
 * {@link ActionMode} and selects that row, further taps toggle membership, and the
 * mode ends itself once the selection empties out.
 */
public class TransactionSelectionController {

    public interface Host {
        /** Looks up a currently-displayed transaction by id (for duplicating). */
        TransactionWithDetails findTransactionById(long id);

        /** Called whenever the selection changes, so the adapter can re-highlight rows. */
        void onSelectionChanged(Set<Long> selectedIds);
    }

    private final AppCompatActivity activity;
    private final TransactionRepository transactionRepository;
    private final Host host;
    private final Set<Long> selectedIds = new HashSet<>();
    private ActionMode actionMode;

    public TransactionSelectionController(AppCompatActivity activity,
                                           TransactionRepository transactionRepository, Host host) {
        this.activity = activity;
        this.transactionRepository = transactionRepository;
        this.host = host;
    }

    public void onItemLongClicked(long transactionId) {
        if (actionMode != null) {
            onItemClicked(transactionId);
            return;
        }
        selectedIds.clear();
        selectedIds.add(transactionId);
        actionMode = activity.startSupportActionMode(callback);
        notifySelectionChanged();
    }

    public void onItemClicked(long transactionId) {
        if (actionMode == null) {
            return;
        }
        if (!selectedIds.remove(transactionId)) {
            selectedIds.add(transactionId);
        }
        if (selectedIds.isEmpty()) {
            actionMode.finish();
            return;
        }
        notifySelectionChanged();
        actionMode.invalidate();
    }

    private void notifySelectionChanged() {
        host.onSelectionChanged(new HashSet<>(selectedIds));
    }

    private final ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_transaction_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean singleSelected = selectedIds.size() == 1;
            menu.findItem(R.id.action_duplicate).setVisible(singleSelected);
            menu.findItem(R.id.action_edit).setVisible(singleSelected);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                deleteSelected();
                mode.finish();
                return true;
            }
            if (id == R.id.action_duplicate) {
                duplicateSelected();
                mode.finish();
                return true;
            }
            if (id == R.id.action_edit) {
                editSelected();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedIds.clear();
            host.onSelectionChanged(new HashSet<>());
        }
    };

    private void deleteSelected() {
        for (Long id : selectedIds) {
            Transaction stub = new Transaction();
            stub.setId(id);
            transactionRepository.delete(stub);
        }
    }

    private void duplicateSelected() {
        if (selectedIds.size() != 1) {
            return;
        }
        long id = selectedIds.iterator().next();
        TransactionWithDetails source = host.findTransactionById(id);
        if (source == null) {
            return;
        }
        Transaction copy = new Transaction();
        copy.setAccountId(source.accountId);
        copy.setCategoryId(source.categoryId);
        copy.setType(source.type);
        copy.setAmount(source.amount);
        copy.setCounterparty(source.counterparty);
        copy.setNote(source.note);
        copy.setDate(source.date);
        transactionRepository.insert(copy, null);
    }

    private void editSelected() {
        if (selectedIds.size() != 1) {
            return;
        }
        long id = selectedIds.iterator().next();
        activity.startActivity(AddTransactionActivity.newIntentForEdit(activity, id));
    }
}
