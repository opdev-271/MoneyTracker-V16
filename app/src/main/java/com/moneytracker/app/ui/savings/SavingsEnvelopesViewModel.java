package com.moneytracker.app.ui.savings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.SavingsEnvelope;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.data.repository.SavingsEnvelopeRepository;
import com.moneytracker.app.util.SettingsPrefs;

import java.util.List;

public class SavingsEnvelopesViewModel extends AndroidViewModel {

    private final AccountRepository accountRepository;
    private final SavingsEnvelopeRepository envelopeRepository;

    private final long accountId;
    private final LiveData<Account> account;
    private final LiveData<Double> balance;
    private final LiveData<List<SavingsEnvelope>> envelopes;
    private final LiveData<Double> totalEnveloped;
    private final LiveData<List<Account>> profileAccounts;

    public SavingsEnvelopesViewModel(@NonNull Application application, long accountId) {
        super(application);
        this.accountId = accountId;
        MoneyTrackerApp app = MoneyTrackerApp.from(application);
        accountRepository = app.getAccountRepository();
        envelopeRepository = app.getSavingsEnvelopeRepository();

        account = accountRepository.getAccountById(accountId);
        balance = accountRepository.getAccountBalance(accountId);
        envelopes = envelopeRepository.getEnvelopesForAccount(accountId);
        totalEnveloped = envelopeRepository.getTotalEnvelopedForAccount(accountId);
        profileAccounts = accountRepository.getAccountsForProfile(SettingsPrefs.getActiveProfileId(application));
    }

    public LiveData<Account> getAccount() {
        return account;
    }

    public LiveData<Double> getBalance() {
        return balance;
    }

    public LiveData<List<SavingsEnvelope>> getEnvelopes() {
        return envelopes;
    }

    public LiveData<Double> getTotalEnveloped() {
        return totalEnveloped;
    }

    public LiveData<List<Account>> getProfileAccounts() {
        return profileAccounts;
    }

    public void saveEnvelope(SavingsEnvelope envelope) {
        envelope.setAccountId(accountId);
        if (envelope.getId() == 0) {
            envelopeRepository.insert(envelope);
        } else {
            envelopeRepository.update(envelope);
        }
    }

    public void deleteEnvelope(SavingsEnvelope envelope) {
        envelopeRepository.delete(envelope);
    }

    public void depositToEnvelope(long envelopeId, long sourceAccountId, double amount, Runnable onDone) {
        envelopeRepository.depositToEnvelope(envelopeId, sourceAccountId, amount, onDone::run);
    }

    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final Application application;
        private final long accountId;

        public Factory(Application application, long accountId) {
            this.application = application;
            this.accountId = accountId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SavingsEnvelopesViewModel(application, accountId);
        }
    }
}
