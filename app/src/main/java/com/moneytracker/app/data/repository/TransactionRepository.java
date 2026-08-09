package com.moneytracker.app.data.repository;

import androidx.lifecycle.LiveData;

import com.moneytracker.app.data.AppDatabase;
import com.moneytracker.app.data.dao.TransactionDao;
import com.moneytracker.app.data.entity.Transaction;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.util.AppExecutors;

import java.util.List;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final AppExecutors executors;

    public TransactionRepository(AppDatabase database, AppExecutors executors) {
        this.transactionDao = database.transactionDao();
        this.executors = executors;
    }

    public LiveData<Transaction> getTransactionById(long transactionId) {
        return transactionDao.getTransactionById(transactionId);
    }

    public LiveData<List<TransactionWithDetails>> getTransactionsForAccounts(List<Long> accountIds) {
        return transactionDao.getTransactionsForAccounts(accountIds);
    }

    public LiveData<List<TransactionWithDetails>> getRecentTransactionsForAccounts(List<Long> accountIds, int limit) {
        return transactionDao.getRecentTransactionsForAccounts(accountIds, limit);
    }

    public LiveData<List<TransactionWithDetails>> getTransactionsBetweenForAccounts(
            List<Long> accountIds, long startMillis, long endMillis) {
        return transactionDao.getTransactionsBetweenForAccounts(accountIds, startMillis, endMillis);
    }

    public LiveData<Double> getTotalIncomeBetweenForAccounts(List<Long> accountIds, long startMillis, long endMillis) {
        return transactionDao.getTotalIncomeBetweenForAccounts(accountIds, startMillis, endMillis);
    }

    public LiveData<Double> getTotalExpenseBetweenForAccounts(List<Long> accountIds, long startMillis, long endMillis) {
        return transactionDao.getTotalExpenseBetweenForAccounts(accountIds, startMillis, endMillis);
    }

    public LiveData<List<TransactionDao.CategoryTotal>> getExpenseTotalsByCategoryForAccounts(
            List<Long> accountIds, long startMillis, long endMillis) {
        return transactionDao.getExpenseTotalsByCategoryForAccounts(accountIds, startMillis, endMillis);
    }

    public void insert(Transaction transaction, OnTransactionSaved callback) {
        executors.diskIO().execute(() -> {
            long id = transactionDao.insert(transaction);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onSaved(id));
            }
        });
    }

    public void update(Transaction transaction) {
        executors.diskIO().execute(() -> transactionDao.update(transaction));
    }

    public void delete(Transaction transaction) {
        executors.diskIO().execute(() -> transactionDao.delete(transaction));
    }

    public interface OnTransactionSaved {
        void onSaved(long transactionId);
    }
}
