package com.moneytracker.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.moneytracker.app.data.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    LiveData<List<Category>> getCategoriesByType(String type);

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    List<Category> getCategoriesByTypeSync(String type);

    @Query("SELECT COUNT(*) FROM categories")
    int countSync();

    /**
     * How many transactions currently point at this category. Deleting one is not
     * destructive - the transactions' FK is ON DELETE SET NULL, so they survive
     * uncategorized - but the management screen still warns with this number first,
     * since "uncategorized" silently drops those amounts out of Analytics' per-category
     * breakdown and that is not obvious from the delete button alone.
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    int countTransactionsUsingSync(long categoryId);
}
