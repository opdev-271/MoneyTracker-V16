package com.moneytracker.app.ui.alltransactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.data.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class AllTransactionsViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final MutableLiveData<List<Long>> accountIds = new MutableLiveData<>();
    private final LiveData<List<TransactionWithDetails>> transactions;

    public AllTransactionsViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = MoneyTrackerApp.from(application).getTransactionRepository();
        transactions = Transformations.switchMap(accountIds, ids ->
                (ids == null || ids.isEmpty()) ? new MutableLiveData<>(new ArrayList<>())
                        : transactionRepository.getTransactionsForAccounts(ids));
    }

    public void setAccountIds(List<Long> ids) {
        if (!ids.equals(accountIds.getValue())) {
            accountIds.setValue(ids);
        }
    }

    public LiveData<List<TransactionWithDetails>> getTransactions() {
        return transactions;
    }
}
