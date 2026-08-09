package com.moneytracker.app.data.repository;

import androidx.lifecycle.LiveData;

import com.moneytracker.app.data.AppDatabase;
import com.moneytracker.app.data.dao.CategoryDao;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.util.AppExecutors;

import java.util.List;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final AppExecutors executors;

    public CategoryRepository(AppDatabase database, AppExecutors executors) {
        this.categoryDao = database.categoryDao();
        this.executors = executors;
    }

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public LiveData<List<Category>> getCategoriesByType(String type) {
        return categoryDao.getCategoriesByType(type);
    }

    public void insert(Category category) {
        executors.diskIO().execute(() -> categoryDao.insert(category));
    }

    public void insert(Category category, OnCategorySaved callback) {
        executors.diskIO().execute(() -> {
            long id = categoryDao.insert(category);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onSaved(id));
            }
        });
    }

    public interface OnCategorySaved {
        void onSaved(long categoryId);
    }

    public void update(Category category) {
        executors.diskIO().execute(() -> categoryDao.update(category));
    }

    public void delete(Category category) {
        executors.diskIO().execute(() -> categoryDao.delete(category));
    }

    public interface OnUsageCounted {
        void onCounted(int transactionCount);
    }

    /** Counts the transactions filed under a category, for the delete confirmation. */
    public void countTransactionsUsing(long categoryId, OnUsageCounted callback) {
        executors.diskIO().execute(() -> {
            int count = categoryDao.countTransactionsUsingSync(categoryId);
            executors.mainThread().execute(() -> callback.onCounted(count));
        });
    }
}
