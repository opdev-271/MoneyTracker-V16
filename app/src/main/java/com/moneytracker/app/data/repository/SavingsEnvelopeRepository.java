package com.moneytracker.app.data.repository;

import androidx.lifecycle.LiveData;

import com.moneytracker.app.data.AppDatabase;
import com.moneytracker.app.data.dao.SavingsEnvelopeDao;
import com.moneytracker.app.data.dao.TransactionDao;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.entity.SavingsEnvelope;
import com.moneytracker.app.data.entity.Transaction;
import com.moneytracker.app.util.AppExecutors;

import java.util.List;

public class SavingsEnvelopeRepository {

    private final AppDatabase database;
    private final SavingsEnvelopeDao envelopeDao;
    private final TransactionDao transactionDao;
    private final AppExecutors executors;

    public SavingsEnvelopeRepository(AppDatabase database, AppExecutors executors) {
        this.database = database;
        this.envelopeDao = database.savingsEnvelopeDao();
        this.transactionDao = database.transactionDao();
        this.executors = executors;
    }

    public LiveData<List<SavingsEnvelope>> getEnvelopesForAccount(long accountId) {
        return envelopeDao.getEnvelopesForAccount(accountId);
    }

    public LiveData<Double> getTotalEnvelopedForAccount(long accountId) {
        return envelopeDao.getTotalEnvelopedForAccount(accountId);
    }

    public void insert(SavingsEnvelope envelope) {
        executors.diskIO().execute(() -> envelopeDao.insert(envelope));
    }

    public void update(SavingsEnvelope envelope) {
        executors.diskIO().execute(() -> envelopeDao.update(envelope));
    }

    public void delete(SavingsEnvelope envelope) {
        executors.diskIO().execute(() -> envelopeDao.delete(envelope));
    }

    /**
     * Moves money into an envelope. If {@code sourceAccountId} is a different account
     * than the envelope's own savings account, this also records a real expense/income
     * transaction pair (so both accounts' balances and histories reflect the transfer);
     * if the source IS the envelope's own account, it's just re-earmarking money that's
     * already there, so no transactions are created - only the envelope's own tally moves.
     */
    public void depositToEnvelope(long envelopeId, long sourceAccountId, double amount,
                                   OnDepositListener callback) {
        executors.diskIO().execute(() -> {
            database.runInTransaction(() -> {
                SavingsEnvelope envelope = envelopeDao.getEnvelopeByIdSync(envelopeId);
                if (envelope == null) {
                    return;
                }
                if (sourceAccountId != envelope.getAccountId()) {
                    Transaction expense = new Transaction();
                    expense.setAccountId(sourceAccountId);
                    expense.setType(Category.TYPE_EXPENSE);
                    expense.setAmount(amount);
                    expense.setNote(envelope.getName());
                    transactionDao.insert(expense);

                    Transaction income = new Transaction();
                    income.setAccountId(envelope.getAccountId());
                    income.setType(Category.TYPE_INCOME);
                    income.setAmount(amount);
                    income.setNote(envelope.getName());
                    transactionDao.insert(income);
                }
                envelope.setCurrentAmount(envelope.getCurrentAmount() + amount);
                envelopeDao.update(envelope);
            });
            if (callback != null) {
                executors.mainThread().execute(callback::onDeposited);
            }
        });
    }

    public interface OnDepositListener {
        void onDeposited();
    }
}
