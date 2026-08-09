package com.moneytracker.app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Profile;
import com.moneytracker.app.data.relation.AccountWithBalance;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.data.repository.ProfileRepository;
import com.moneytracker.app.data.repository.TransactionRepository;
import com.moneytracker.app.util.CurrencyConverter;
import com.moneytracker.app.util.ExchangeRatePrefs;
import com.moneytracker.app.util.OnboardingPrefs;
import com.moneytracker.app.util.SettingsPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeViewModel extends AndroidViewModel {

    private static final int RECENT_TRANSACTIONS_LIMIT = 3;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ProfileRepository profileRepository;

    private final MutableLiveData<Long> activeProfileId;
    private final LiveData<Profile> activeProfile;
    private final LiveData<List<Account>> accounts;

    /** {@code null} means "all accounts" — also the default until the user picks a subset. */
    private final MutableLiveData<Set<Long>> selectedAccountIds;
    private final MediatorLiveData<List<Long>> resolvedAccountIds = new MediatorLiveData<>();

    private final LiveData<List<AccountWithBalance>> accountsWithBalance;
    private final MediatorLiveData<Double> balance = new MediatorLiveData<>();
    private final LiveData<List<TransactionWithDetails>> recentTransactions;
    private final LiveData<Boolean> balanceHidden;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        MoneyTrackerApp app = MoneyTrackerApp.from(application);
        accountRepository = app.getAccountRepository();
        transactionRepository = app.getTransactionRepository();
        profileRepository = app.getProfileRepository();

        long initialProfileId = SettingsPrefs.getActiveProfileId(application);
        activeProfileId = new MutableLiveData<>(initialProfileId);
        activeProfile = Transformations.switchMap(activeProfileId, profileRepository::getProfileById);
        accounts = Transformations.switchMap(activeProfileId, accountRepository::getAccountsForProfile);

        selectedAccountIds = new MutableLiveData<>(
                SettingsPrefs.getBalanceAccountSelection(application, initialProfileId));

        resolvedAccountIds.addSource(accounts, list -> recomputeResolvedIds());
        resolvedAccountIds.addSource(selectedAccountIds, ids -> recomputeResolvedIds());

        accountsWithBalance = Transformations.switchMap(activeProfileId, accountRepository::getAccountsWithBalanceForProfile);
        balance.addSource(accountsWithBalance, list -> recomputeBalance());
        balance.addSource(resolvedAccountIds, ids -> recomputeBalance());
        balance.addSource(ExchangeRatePrefs.ratesUpdatedSignal, tick -> recomputeBalance());

        recentTransactions = Transformations.switchMap(resolvedAccountIds, ids ->
                (ids == null || ids.isEmpty()) ? new MutableLiveData<>(new ArrayList<>())
                        : transactionRepository.getRecentTransactionsForAccounts(ids, RECENT_TRANSACTIONS_LIMIT));

        balanceHidden = SettingsPrefs.observeBalanceHidden(application);
    }

    private void recomputeResolvedIds() {
        List<Account> accountList = accounts.getValue();
        if (accountList == null) {
            return;
        }
        Set<Long> selection = selectedAccountIds.getValue();
        List<Long> result = new ArrayList<>();
        for (Account account : accountList) {
            if (selection == null || selection.contains(account.getId())) {
                result.add(account.getId());
            }
        }
        resolvedAccountIds.setValue(result);
    }

    /** Converts each selected account's own balance to the preferred currency before
     * summing, instead of adding raw numbers across potentially different currencies. */
    private void recomputeBalance() {
        List<AccountWithBalance> list = accountsWithBalance.getValue();
        List<Long> ids = resolvedAccountIds.getValue();
        if (list == null || ids == null) {
            return;
        }
        String baseCurrency = OnboardingPrefs.getPreferredCurrency(getApplication());
        double sum = 0;
        for (AccountWithBalance item : list) {
            if (ids.contains(item.account.getId())) {
                sum += CurrencyConverter.convert(getApplication(), item.balance,
                        item.account.getCurrencyCode(), baseCurrency);
            }
        }
        balance.setValue(sum);
    }

    public LiveData<Profile> getActiveProfile() {
        return activeProfile;
    }

    public void switchProfile(long profileId) {
        Long current = activeProfileId.getValue();
        if (current != null && current == profileId) {
            return;
        }
        activeProfileId.setValue(profileId);
        SettingsPrefs.setActiveProfileId(getApplication(), profileId);
        selectedAccountIds.setValue(SettingsPrefs.getBalanceAccountSelection(getApplication(), profileId));
    }

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<Set<Long>> getSelectedAccountIds() {
        return selectedAccountIds;
    }

    public LiveData<List<Long>> getResolvedAccountIds() {
        return resolvedAccountIds;
    }

    /** Pass a set covering every current account (or use the dialog's "select all") to mean "all". */
    public void setSelectedAccountIds(Set<Long> ids) {
        List<Account> accountList = accounts.getValue();
        Set<Long> normalized = ids;
        if (accountList != null && ids != null && ids.size() >= accountList.size()) {
            boolean coversAll = true;
            for (Account account : accountList) {
                if (!ids.contains(account.getId())) {
                    coversAll = false;
                    break;
                }
            }
            if (coversAll) {
                normalized = null;
            }
        }
        selectedAccountIds.setValue(normalized);
        Long profileId = activeProfileId.getValue();
        if (profileId != null) {
            SettingsPrefs.setBalanceAccountSelection(getApplication(), profileId, normalized);
        }
    }

    public LiveData<Double> getBalance() {
        return balance;
    }

    public LiveData<List<AccountWithBalance>> getAccountsWithBalance() {
        return accountsWithBalance;
    }

    public LiveData<List<TransactionWithDetails>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<Boolean> getBalanceHidden() {
        return balanceHidden;
    }

    public void toggleBalanceHidden() {
        boolean hidden = Boolean.TRUE.equals(balanceHidden.getValue());
        // Writing the pref is what updates balanceHidden - and with it the Settings switch.
        SettingsPrefs.setBalanceHidden(getApplication(), !hidden);
    }

    /** Account to preselect when quick-adding a transaction; -1 lets AddTransaction pick its own default. */
    public long getQuickAddAccountId() {
        List<Long> ids = resolvedAccountIds.getValue();
        if (ids != null && !ids.isEmpty()) {
            return ids.get(0);
        }
        return -1L;
    }
}
