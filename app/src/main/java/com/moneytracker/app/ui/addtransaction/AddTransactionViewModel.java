package com.moneytracker.app.ui.addtransaction;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.entity.Transaction;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.data.repository.CategoryRepository;
import com.moneytracker.app.data.repository.TransactionRepository;
import com.moneytracker.app.util.Event;
import com.moneytracker.app.util.SettingsPrefs;

import java.util.Calendar;
import java.util.List;

public class AddTransactionViewModel extends AndroidViewModel {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    private final MutableLiveData<String> type = new MutableLiveData<>(Category.TYPE_EXPENSE);
    private final MutableLiveData<Long> selectedAccountId = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedCategoryId = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedDateMillis = new MutableLiveData<>(Calendar.getInstance().getTimeInMillis());

    private final LiveData<List<Account>> accounts;
    private final LiveData<List<Category>> categories;

    private final MutableLiveData<Event<Boolean>> savedEvent = new MutableLiveData<>();
    private Long editingTransactionId;

    public AddTransactionViewModel(@NonNull Application application) {
        super(application);
        MoneyTrackerApp app = MoneyTrackerApp.from(application);
        accountRepository = app.getAccountRepository();
        categoryRepository = app.getCategoryRepository();
        transactionRepository = app.getTransactionRepository();

        accounts = accountRepository.getAccountsForProfile(SettingsPrefs.getActiveProfileId(application));
        categories = Transformations.switchMap(type, categoryRepository::getCategoriesByType);
    }

    public void setInitialType(String initialType) {
        if (initialType != null) {
            type.setValue(initialType);
        }
    }

    public void setInitialAccountId(long accountId) {
        if (accountId != -1L) {
            selectedAccountId.setValue(accountId);
        }
    }

    public void setEditingTransactionId(long transactionId) {
        this.editingTransactionId = transactionId;
    }

    public boolean isEditing() {
        return editingTransactionId != null;
    }

    public LiveData<Transaction> getTransactionById(long transactionId) {
        return transactionRepository.getTransactionById(transactionId);
    }

    public LiveData<String> getType() {
        return type;
    }

    public void setType(String newType) {
        if (!newType.equals(type.getValue())) {
            type.setValue(newType);
            selectedCategoryId.setValue(null);
        }
    }

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Long> getSelectedAccountId() {
        return selectedAccountId;
    }

    public void setSelectedAccountId(long accountId) {
        selectedAccountId.setValue(accountId);
    }

    public LiveData<Long> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(long categoryId) {
        selectedCategoryId.setValue(categoryId);
    }

    public LiveData<Long> getSelectedDateMillis() {
        return selectedDateMillis;
    }

    public void setSelectedDateMillis(long millis) {
        selectedDateMillis.setValue(millis);
    }

    public LiveData<Event<Boolean>> getSavedEvent() {
        return savedEvent;
    }

    public boolean hasAccount() {
        return selectedAccountId.getValue() != null;
    }

    public boolean hasCategory() {
        return selectedCategoryId.getValue() != null;
    }

    public void save(double amount, String counterparty, String note) {
        Long accountId = selectedAccountId.getValue();
        Long categoryId = selectedCategoryId.getValue();
        if (accountId == null || categoryId == null || amount <= 0) {
            return;
        }
        Transaction transaction = new Transaction();
        if (editingTransactionId != null) {
            transaction.setId(editingTransactionId);
        }
        transaction.setAccountId(accountId);
        transaction.setCategoryId(categoryId);
        transaction.setType(type.getValue());
        transaction.setAmount(amount);
        transaction.setCounterparty(counterparty);
        transaction.setNote(note);
        Long dateMillis = selectedDateMillis.getValue();
        transaction.setDate(new java.util.Date(dateMillis != null ? dateMillis : System.currentTimeMillis()));

        if (editingTransactionId != null) {
            transactionRepository.update(transaction);
            savedEvent.setValue(new Event<>(true));
        } else {
            transactionRepository.insert(transaction, id -> savedEvent.setValue(new Event<>(true)));
        }
    }
}
