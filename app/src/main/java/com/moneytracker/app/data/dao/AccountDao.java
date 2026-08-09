package com.moneytracker.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.relation.AccountWithBalance;

import java.util.List;

@Dao
public interface AccountDao {

    @Insert
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId ORDER BY sort_order ASC, id ASC")
    LiveData<List<Account>> getAccountsForProfile(long profileId);

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId ORDER BY sort_order ASC, id ASC")
    List<Account> getAccountsForProfileSync(long profileId);

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    LiveData<Account> getAccountById(long accountId);

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    Account getAccountByIdSync(long accountId);

    @Query("SELECT * FROM accounts WHERE id IN (:accountIds)")
    List<Account> getAccountsByIdsSync(List<Long> accountIds);

    @Query("SELECT COUNT(*) FROM accounts WHERE profile_id = :profileId")
    int countForProfileSync(long profileId);

    @Query("SELECT (a.initial_balance + IFNULL((SELECT SUM(CASE WHEN t.type = 'EXPENSE' THEN -t.amount ELSE t.amount END) " +
            "FROM transactions t WHERE t.account_id = a.id), 0)) " +
            "FROM accounts a WHERE a.id = :accountId")
    LiveData<Double> getAccountBalance(long accountId);

    @Query("SELECT a.*, (a.initial_balance + IFNULL((SELECT SUM(CASE WHEN t.type = 'EXPENSE' THEN -t.amount ELSE t.amount END) " +
            "FROM transactions t WHERE t.account_id = a.id), 0)) AS balance, " +
            "IFNULL((SELECT SUM(se.current_amount) FROM savings_envelopes se WHERE se.account_id = a.id), 0) AS envelopedTotal " +
            "FROM accounts a WHERE a.profile_id = :profileId ORDER BY a.sort_order ASC, a.id ASC")
    LiveData<List<AccountWithBalance>> getAccountsWithBalanceForProfile(long profileId);
}
