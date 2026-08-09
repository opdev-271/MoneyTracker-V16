package com.moneytracker.app.ui.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.data.dao.TransactionDao;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.data.repository.AccountRepository;
import com.moneytracker.app.data.repository.TransactionRepository;
import com.moneytracker.app.util.SettingsPrefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AnalyticsViewModel extends AndroidViewModel {

    public enum Period {
        DAY, WEEK, MONTH, YEAR
    }

    private static class QueryParams {
        final long[] range;
        final List<Long> accountIds;

        QueryParams(long[] range, List<Long> accountIds) {
            this.range = range;
            this.accountIds = accountIds;
        }
    }

    private final long activeProfileId;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final MutableLiveData<Period> period = new MutableLiveData<>(Period.MONTH);
    private final LiveData<long[]> dateRange;
    private final LiveData<List<Account>> accounts;

    /** {@code null} means "all accounts of this profile" (the "All" chip). */
    private final MutableLiveData<Long> selectedAccountId = new MutableLiveData<>(null);
    private final MediatorLiveData<List<Long>> resolvedAccountIds = new MediatorLiveData<>();
    private final MediatorLiveData<QueryParams> queryParams = new MediatorLiveData<>();

    private final LiveData<Double> totalIncome;
    private final LiveData<Double> totalExpense;
    private final LiveData<Double> previousTotalExpense;
    private final LiveData<List<TransactionDao.CategoryTotal>> categoryTotals;
    private final LiveData<List<TransactionWithDetails>> periodTransactions;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        MoneyTrackerApp app = MoneyTrackerApp.from(application);
        accountRepository = app.getAccountRepository();
        transactionRepository = app.getTransactionRepository();
        activeProfileId = SettingsPrefs.getActiveProfileId(application);

        accounts = accountRepository.getAccountsForProfile(activeProfileId);
        dateRange = Transformations.map(period, AnalyticsViewModel::computeRange);

        resolvedAccountIds.addSource(accounts, list -> recomputeResolvedIds());
        resolvedAccountIds.addSource(selectedAccountId, id -> recomputeResolvedIds());

        queryParams.addSource(dateRange, range -> updateQueryParams(range, resolvedAccountIds.getValue()));
        queryParams.addSource(resolvedAccountIds, ids -> updateQueryParams(dateRange.getValue(), ids));

        totalIncome = Transformations.switchMap(queryParams, params ->
                params.accountIds.isEmpty() ? new MutableLiveData<>(0d)
                        : transactionRepository.getTotalIncomeBetweenForAccounts(params.accountIds, params.range[0], params.range[1]));
        totalExpense = Transformations.switchMap(queryParams, params ->
                params.accountIds.isEmpty() ? new MutableLiveData<>(0d)
                        : transactionRepository.getTotalExpenseBetweenForAccounts(params.accountIds, params.range[0], params.range[1]));
        categoryTotals = Transformations.switchMap(queryParams, params ->
                params.accountIds.isEmpty() ? new MutableLiveData<>(new ArrayList<>())
                        : transactionRepository.getExpenseTotalsByCategoryForAccounts(params.accountIds, params.range[0], params.range[1]));
        periodTransactions = Transformations.switchMap(queryParams, params ->
                params.accountIds.isEmpty() ? new MutableLiveData<>(new ArrayList<>())
                        : transactionRepository.getTransactionsBetweenForAccounts(params.accountIds, params.range[0], params.range[1]));
        previousTotalExpense = Transformations.switchMap(queryParams, params -> {
            if (params.accountIds.isEmpty()) {
                return new MutableLiveData<>(0d);
            }
            long[] prev = previousRangeOf(params.range, period.getValue());
            return transactionRepository.getTotalExpenseBetweenForAccounts(params.accountIds, prev[0], prev[1]);
        });
    }

    /** The same-length window immediately before the current one (last month for MONTH, etc.). */
    private static long[] previousRangeOf(long[] range, Period period) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(range[0]);
        switch (period != null ? period : Period.MONTH) {
            case DAY:
                start.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case WEEK:
                start.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case YEAR:
                start.add(Calendar.YEAR, -1);
                break;
            case MONTH:
            default:
                start.add(Calendar.MONTH, -1);
                break;
        }
        return new long[]{start.getTimeInMillis(), range[0] - 1};
    }

    private void recomputeResolvedIds() {
        List<Account> accountList = accounts.getValue();
        if (accountList == null) {
            return;
        }
        Long selected = selectedAccountId.getValue();
        List<Long> ids = new ArrayList<>();
        for (Account account : accountList) {
            if (selected == null || selected == account.getId()) {
                ids.add(account.getId());
            }
        }
        resolvedAccountIds.setValue(ids);
    }

    private void updateQueryParams(long[] range, List<Long> accountIds) {
        if (range == null || accountIds == null) {
            return;
        }
        queryParams.setValue(new QueryParams(range, accountIds));
    }

    private static long[] computeRange(Period period) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        switch (period) {
            case WEEK:
                start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                break;
            case YEAR:
                start.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case DAY:
                break;
            case MONTH:
            default:
                start.set(Calendar.DAY_OF_MONTH, 1);
                break;
        }

        Calendar end = (Calendar) start.clone();
        switch (period) {
            case WEEK:
                end.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case YEAR:
                end.add(Calendar.YEAR, 1);
                break;
            case DAY:
                end.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case MONTH:
            default:
                end.add(Calendar.MONTH, 1);
                break;
        }
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis() - 1};
    }

    public void setPeriod(Period newPeriod) {
        if (!newPeriod.equals(period.getValue())) {
            period.setValue(newPeriod);
        }
    }

    public LiveData<Period> getPeriod() {
        return period;
    }

    public LiveData<long[]> getDateRange() {
        return dateRange;
    }

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<Long> getSelectedAccountId() {
        return selectedAccountId;
    }

    public void setSelectedAccountId(Long accountId) {
        if (accountId == null ? selectedAccountId.getValue() != null : !accountId.equals(selectedAccountId.getValue())) {
            selectedAccountId.setValue(accountId);
        }
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<List<TransactionDao.CategoryTotal>> getCategoryTotals() {
        return categoryTotals;
    }

    public LiveData<Double> getPreviousTotalExpense() {
        return previousTotalExpense;
    }

    public LiveData<List<TransactionWithDetails>> getPeriodTransactions() {
        return periodTransactions;
    }
}
