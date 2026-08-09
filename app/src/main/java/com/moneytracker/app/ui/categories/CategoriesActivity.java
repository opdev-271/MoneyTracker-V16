package com.moneytracker.app.ui.categories;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.databinding.ActivityCategoriesBinding;
import com.moneytracker.app.util.CategorySorter;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.LocaleHelper;
import com.moneytracker.app.util.ViewAnimations;

import java.util.List;

/**
 * Category management, opened from Settings: the full list split into expense and
 * income, with add/edit/delete through {@link CategoryEditorDialogFragment}. The
 * add-transaction screen's picker can still create a category inline; this is where
 * existing ones get renamed, recolored or removed.
 */
public class CategoriesActivity extends AppCompatActivity {

    public static Intent newIntent(Context context) {
        return new Intent(context, CategoriesActivity.class);
    }

    private ActivityCategoriesBinding binding;
    private CategoriesViewModel viewModel;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyStoredLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
        binding = ActivityCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ElevationShadows.apply(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CategoriesViewModel.class);

        CategoryManageAdapter expenseAdapter = new CategoryManageAdapter(this::openEditor);
        binding.recyclerExpense.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerExpense.setAdapter(expenseAdapter);

        CategoryManageAdapter incomeAdapter = new CategoryManageAdapter(this::openEditor);
        binding.recyclerIncome.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerIncome.setAdapter(incomeAdapter);

        binding.btnClose.setOnClickListener(v -> closeWithSlideDown());
        binding.btnAddExpenseCategory.setOnClickListener(v -> openCreator(Category.TYPE_EXPENSE));
        binding.btnAddIncomeCategory.setOnClickListener(v -> openCreator(Category.TYPE_INCOME));
        ViewAnimations.addPressFeedback(binding.btnAddExpenseCategory, binding.btnAddIncomeCategory);

        viewModel.getExpenseCategories().observe(this, categories -> {
            expenseAdapter.submitList(CategorySorter.byDisplayName(this, categories));
            binding.textEmptyExpense.setVisibility(isEmpty(categories) ? View.VISIBLE : View.GONE);
        });
        viewModel.getIncomeCategories().observe(this, categories -> {
            incomeAdapter.submitList(CategorySorter.byDisplayName(this, categories));
            binding.textEmptyIncome.setVisibility(isEmpty(categories) ? View.VISIBLE : View.GONE);
        });
    }

    private static boolean isEmpty(List<Category> categories) {
        return categories == null || categories.isEmpty();
    }

    private void openEditor(Category category) {
        CategoryEditorDialogFragment.newInstanceForEdit(category)
                .show(getSupportFragmentManager(), "edit_category");
    }

    /** The type button that was tapped preselects the type, but it stays changeable. */
    private void openCreator(String type) {
        CategoryEditorDialogFragment.newInstanceForAdd(type, true)
                .show(getSupportFragmentManager(), "add_category");
    }

    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }
}
