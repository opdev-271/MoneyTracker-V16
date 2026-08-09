package com.moneytracker.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.moneytracker.app.data.dao.AccountDao;
import com.moneytracker.app.data.dao.CategoryDao;
import com.moneytracker.app.data.dao.ProfileDao;
import com.moneytracker.app.data.dao.SavingsEnvelopeDao;
import com.moneytracker.app.data.dao.TransactionDao;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.entity.Profile;
import com.moneytracker.app.data.entity.SavingsEnvelope;
import com.moneytracker.app.data.entity.Transaction;
import com.moneytracker.app.util.AppExecutors;

import java.util.Arrays;

@Database(
        entities = {Account.class, Category.class, Transaction.class, Profile.class, SavingsEnvelope.class},
        version = 5,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract AccountDao accountDao();

    public abstract CategoryDao categoryDao();

    public abstract TransactionDao transactionDao();

    public abstract ProfileDao profileDao();

    public abstract SavingsEnvelopeDao savingsEnvelopeDao();

    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "moneytracker.db")
                            .addCallback(seedCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * Only the (profile-independent) categories are seeded here. The first profile and
     * its default account are created by {@code OnboardingActivity} once the user has
     * picked a name/pictogram — seeding them here too would race the onboarding insert,
     * since this callback and that insert can both trigger the database's first-ever
     * open on the same background thread.
     */
    private static final Callback seedCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            AppExecutors.getInstance().diskIO().execute(() -> {
                if (instance == null) {
                    return;
                }
                seedCategories(instance.categoryDao());
            });
        }
    };

    private static void seedCategories(CategoryDao dao) {
        Category food = newCategory("category_food", Category.TYPE_EXPENSE, "ic_cat_food", 0xFFF2907B);
        Category transport = newCategory("category_transport", Category.TYPE_EXPENSE, "ic_cat_transport", 0xFF8EC9F0);
        Category shopping = newCategory("category_shopping", Category.TYPE_EXPENSE, "ic_cat_shopping", 0xFFB9B3F4);
        Category bills = newCategory("category_bills", Category.TYPE_EXPENSE, "ic_cat_bills", 0xFFE2674A);
        Category entertainment = newCategory("category_entertainment", Category.TYPE_EXPENSE, "ic_cat_entertainment", 0xFF9B8CE8);
        Category health = newCategory("category_health", Category.TYPE_EXPENSE, "ic_cat_health", 0xFF7ED9A7);
        Category otherExpense = newCategory("category_other_expense", Category.TYPE_EXPENSE, "ic_cat_other", 0xFF9AA091);

        Category salary = newCategory("category_salary", Category.TYPE_INCOME, "ic_cat_salary", 0xFFA9C93F);
        Category gift = newCategory("category_gift", Category.TYPE_INCOME, "ic_cat_gift", 0xFFF2C94C);
        Category otherIncome = newCategory("category_other_income", Category.TYPE_INCOME, "ic_cat_other", 0xFF8EC9F0);

        dao.insertAll(Arrays.asList(
                food, transport, shopping, bills, entertainment, health, otherExpense,
                salary, gift, otherIncome
        ));
    }

    private static Category newCategory(String nameKey, String type, String icon, int color) {
        Category category = new Category();
        category.setName(nameKey);
        category.setType(type);
        category.setIcon(icon);
        category.setColor(color);
        return category;
    }
}
