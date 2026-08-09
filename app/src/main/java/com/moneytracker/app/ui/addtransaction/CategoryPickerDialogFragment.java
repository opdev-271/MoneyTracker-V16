package com.moneytracker.app.ui.addtransaction;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.ui.categories.CategoryEditorDialogFragment;
import com.moneytracker.app.ui.widget.DialogButtonStyler;
import com.moneytracker.app.util.CategoryIcons;
import com.moneytracker.app.util.CategorySorter;
import com.moneytracker.app.util.ContrastColor;
import com.moneytracker.app.util.DisplayNameResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces an anchored PopupMenu (unreliable inside a scrolling screen) with a proper
 * list dialog. Also lets the user create a brand new category, right where they
 * noticed one was missing, instead of needing a separate management screen.
 */
public class CategoryPickerDialogFragment extends DialogFragment {

    private static final String ARG_TYPE = "arg_type";

    public static CategoryPickerDialogFragment newInstance(String type) {
        CategoryPickerDialogFragment fragment = new CategoryPickerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    private AddTransactionViewModel viewModel;
    private LinearLayout listContainer;
    private String type;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(AddTransactionViewModel.class);
        type = requireArguments().getString(ARG_TYPE, Category.TYPE_EXPENSE);

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category_picker, null, false);
        listContainer = view.findViewById(R.id.category_list_container);

        viewModel.getCategories().observe(this, categories ->
                renderRows(categories != null ? categories : new ArrayList<>()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(R.string.label_category)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();
        dialog.setOnShowListener(d -> DialogButtonStyler.style(dialog));
        return dialog;
    }

    private void renderRows(List<Category> categories) {
        if (listContainer == null) {
            return;
        }
        listContainer.removeAllViews();
        Long selectedId = viewModel.getSelectedCategoryId().getValue();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Category category : CategorySorter.byDisplayName(requireContext(), categories)) {
            View row = inflater.inflate(R.layout.item_category_picker_row, listContainer, false);
            FrameLayout badge = row.findViewById(R.id.badge_icon);
            ImageView icon = row.findViewById(R.id.image_icon);
            TextView name = row.findViewById(R.id.text_name);
            View check = row.findViewById(R.id.image_check);

            icon.setImageResource(CategoryIcons.resolve(category.getIcon()));
            int badgeColor = category.getColor() != 0
                    ? category.getColor()
                    : requireContext().getColor(R.color.text_tertiary);
            GradientDrawable badgeBg = (GradientDrawable) badge.getBackground().mutate();
            badgeBg.setColor(badgeColor);
            icon.setColorFilter(ContrastColor.forBackground(badgeColor));
            name.setText(DisplayNameResolver.resolve(requireContext(), category.getName()));
            check.setVisibility(selectedId != null && category.getId() == selectedId ? View.VISIBLE : View.GONE);

            row.setOnClickListener(v -> {
                viewModel.setSelectedCategoryId(category.getId());
                dismiss();
            });
            listContainer.addView(row);
        }

        View addRow = inflater.inflate(R.layout.item_category_picker_add_row, listContainer, false);
        addRow.setOnClickListener(v -> {
            // Type is locked to the transaction being entered: a category of the other
            // type would be created successfully and then be invisible in this picker.
            CategoryEditorDialogFragment addDialog =
                    CategoryEditorDialogFragment.newInstanceForAdd(type, false);
            addDialog.setOnCategorySavedListener(categoryId -> {
                viewModel.setSelectedCategoryId(categoryId);
                dismiss();
            });
            addDialog.show(getParentFragmentManager(), "add_category");
        });
        listContainer.addView(addRow);
    }
}
