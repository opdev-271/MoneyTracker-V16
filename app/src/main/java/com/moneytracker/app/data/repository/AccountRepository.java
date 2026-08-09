package com.moneytracker.app.data.repository;

import androidx.lifecycle.LiveData;

import com.moneytracker.app.data.AppDatabase;
import com.moneytracker.app.data.dao.AccountDao;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.relation.AccountWithBalance;
import com.moneytracker.app.util.AppExecutors;

import java.util.List;

public class AccountRepository {

    private final AccountDao accountDao;
    private final AppExecutors executors;

    public AccountRepository(AppDatabase database, AppExecutors executors) {
        this.accountDao = database.accountDao();
        this.executors = executors;
    }

    public LiveData<List<Account>> getAccountsForProfile(long profileId) {
        return accountDao.getAccountsForProfile(profileId);
    }

    public LiveData<Account> getAccountById(long accountId) {
        return accountDao.getAccountById(accountId);
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return accountDao.getAccountBalance(accountId);
    }

    /** Synchronous — call off the main thread (e.g. via AppExecutors.diskIO()). */
    public List<Account> getAccountsByIdsSync(List<Long> accountIds) {
        return accountDao.getAccountsByIdsSync(accountIds);
    }

    public LiveData<List<AccountWithBalance>> getAccountsWithBalanceForProfile(long profileId) {
        return accountDao.getAccountsWithBalanceForProfile(profileId);
    }

    public void insert(Account account, OnAccountSaved callback) {
        executors.diskIO().execute(() -> {
            long id = accountDao.insert(account);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onSaved(id));
            }
        });
    }

    public void update(Account account) {
        executors.diskIO().execute(() -> accountDao.update(account));
    }

    public void delete(Account account) {
        executors.diskIO().execute(() -> accountDao.delete(account));
    }

    public interface OnAccountSaved {
        void onSaved(long accountId);
    }
}
