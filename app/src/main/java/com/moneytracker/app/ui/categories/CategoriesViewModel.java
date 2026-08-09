package com.moneytracker.app.ui.categories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.data.entity.Category;

import java.util.List;

/**
 * Backs the category management screen. Categories are profile-independent (see the
 * seeding note in {@code AppDatabase}), so there is nothing to scope by account or
 * profile here — the two lists are simply the whole table split by type.
 */
public class CategoriesViewModel extends AndroidViewModel {

    private final LiveData<List<Category>> expenseCategories;
    private final LiveData<List<Category>> incomeCategories;

    public CategoriesViewModel(@NonNull Application application) {
        super(application);
        MoneyTrackerApp app = MoneyTrackerApp.from(application);
        expenseCategories = app.getCategoryRepository().getCategoriesByType(Category.TYPE_EXPENSE);
        incomeCategories = app.getCategoryRepository().getCategoriesByType(Category.TYPE_INCOME);
    }

    public LiveData<List<Category>> getExpenseCategories() {
        return expenseCategories;
    }

    public LiveData<List<Category>> getIncomeCategories() {
        return incomeCategories;
    }
}
