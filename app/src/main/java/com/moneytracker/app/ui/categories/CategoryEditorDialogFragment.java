package com.moneytracker.app.ui.categories;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.repository.CategoryRepository;
import com.moneytracker.app.ui.widget.ColorSwatchRow;
import com.moneytracker.app.ui.widget.DialogButtonStyler;
import com.moneytracker.app.ui.widget.IconGridAdapter;
import com.moneytracker.app.ui.widget.SegmentedToggle;
import com.moneytracker.app.util.CategoryIcons;
import com.moneytracker.app.util.ContrastColor;
import com.moneytracker.app.util.DisplayNameResolver;

/**
 * The single add/edit form for a category — name, expense/income type, icon and color —
 * used both by {@link CategoriesActivity} and by the picker inside the add-transaction
 * screen. It writes through {@link CategoryRepository} directly rather than through a
 * ViewModel so it does not care which of those two hosts opened it; the host learns the
 * saved id through {@link OnCategorySavedListener}.
 */
public class CategoryEditorDialogFragment extends DialogFragment {

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_ICON = "arg_icon";
    private static final String ARG_COLOR = "arg_color";
    private static final String ARG_ALLOW_TYPE_CHANGE = "arg_allow_type_change";

    /**
     * @param allowTypeChange false when the caller already knows which type the category
     *                        has to be — creating one mid-transaction, where an income
     *                        category would immediately vanish from an expense picker.
     */
    public static CategoryEditorDialogFragment newInstanceForAdd(String type, boolean allowTypeChange) {
        CategoryEditorDialogFragment fragment = new CategoryEditorDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, 0);
        args.putString(ARG_TYPE, type);
        args.putBoolean(ARG_ALLOW_TYPE_CHANGE, allowTypeChange);
        fragment.setArguments(args);
        return fragment;
    }

    public static CategoryEditorDialogFragment newInstanceForEdit(Category category) {
        CategoryEditorDialogFragment fragment = new CategoryEditorDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, category.getId());
        args.putString(ARG_NAME, category.getName());
        args.putString(ARG_TYPE, category.getType());
        args.putString(ARG_ICON, category.getIcon());
        args.putInt(ARG_COLOR, category.getColor());
        args.putBoolean(ARG_ALLOW_TYPE_CHANGE, true);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnCategorySavedListener {
        void onCategorySaved(long categoryId);
    }

    private EditText editName;
    private IconGridAdapter iconAdapter;
    private ColorSwatchRow colorSwatchRow;
    private SegmentedToggle typeToggle;
    private FrameLayout previewBadge;
    private ImageView previewIcon;
    private String type;
    private OnCategorySavedListener listener;

    public void setOnCategorySavedListener(OnCategorySavedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        long categoryId = args.getLong(ARG_ID, 0);
        boolean isEdit = categoryId != 0;
        type = args.getString(ARG_TYPE, Category.TYPE_EXPENSE);

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_category, null, false);
        editName = view.findViewById(R.id.edit_name);
        previewBadge = view.findViewById(R.id.preview_badge);
        previewIcon = view.findViewById(R.id.preview_icon);

        String initialIcon = args.getString(ARG_ICON);
        if (!CategoryIcons.hasKey(initialIcon)) {
            initialIcon = CategoryIcons.allOptions().get(0).key;
        }
        // Light cells with a dark icon rather than the avatar picker's grey-on-grey ones:
        // both colors are theme-aware, so this reads as a light chip in the light theme
        // and a dark one in the dark theme, instead of a wall of muddy grey circles
        // sitting on the dialog's white surface.
        iconAdapter = new IconGridAdapter(CategoryIcons.allOptions(), initialIcon,
                requireContext().getColor(R.color.icon_primary),
                R.drawable.bg_circle_light, R.drawable.bg_circle_light_selected);
        iconAdapter.setOnIconSelectedListener(key -> updatePreview());
        RecyclerView recyclerIcons = view.findViewById(R.id.recycler_icons);
        recyclerIcons.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        recyclerIcons.setAdapter(iconAdapter);

        int existingColor = args.getInt(ARG_COLOR, 0);
        LinearLayout colorRow = view.findViewById(R.id.color_row);
        colorSwatchRow = new ColorSwatchRow(requireContext(), colorRow, ColorSwatchRow.CATEGORY_COLORS,
                existingColor != 0 ? existingColor : ColorSwatchRow.CATEGORY_COLORS[0], 5);
        colorSwatchRow.setOnColorSelectedListener(color -> updatePreview());

        setUpTypeToggle(view, args.getBoolean(ARG_ALLOW_TYPE_CHANGE, true));

        if (isEdit) {
            // A seeded category stores a lookup key ("category_food"), not a label, so the
            // field has to show the resolved translation. Saving then writes that literal
            // text back and the category stops re-localizing on a language switch - which
            // is the right outcome once the user has renamed it themselves.
            editName.setText(DisplayNameResolver.resolve(requireContext(), args.getString(ARG_NAME, "")));
        }
        updatePreview();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(isEdit ? R.string.edit_category_title : R.string.add_category_title)
                .setView(view)
                .setPositiveButton(R.string.button_save, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss());
        if (isEdit) {
            builder.setNeutralButton(R.string.action_delete, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface.OnShowListener) d -> {
            DialogButtonStyler.style(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (trySave(categoryId)) {
                    dismiss();
                }
            });
            if (isEdit) {
                // Set after the dialog is shown so the confirmation can replace it instead
                // of the framework dismissing this one out from under the user first.
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener(v -> confirmDelete(categoryId));
            }
        });
        return dialog;
    }

    private void setUpTypeToggle(View view, boolean allowTypeChange) {
        View typeContainer = view.findViewById(R.id.type_container);
        if (!allowTypeChange) {
            typeContainer.setVisibility(View.GONE);
            return;
        }
        typeToggle = new SegmentedToggle((ViewGroup) typeContainer,
                view.findViewById(R.id.type_indicator),
                new TextView[]{view.findViewById(R.id.option_type_expense),
                        view.findViewById(R.id.option_type_income)},
                requireContext().getColor(R.color.on_accent_text),
                requireContext().getColor(R.color.text_secondary));
        typeToggle.setOnSegmentSelectedListener(index ->
                type = index == 1 ? Category.TYPE_INCOME : Category.TYPE_EXPENSE);
        typeToggle.setSelectedIndex(Category.TYPE_INCOME.equals(type) ? 1 : 0);
    }

    private void updatePreview() {
        int color = colorSwatchRow.getSelectedColor();
        previewIcon.setImageResource(CategoryIcons.resolve(iconAdapter.getSelectedKey()));
        previewIcon.setColorFilter(ContrastColor.forBackground(color));
        // setColor() replaces the drawable's authored gradient with a flat fill, which is
        // what the badge wants: this previews the category color exactly as the lists
        // will paint it.
        GradientDrawable badge = (GradientDrawable) previewBadge.getBackground().mutate();
        badge.setColor(color);
        previewBadge.setBackground(badge);
    }

    private boolean trySave(long categoryId) {
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), R.string.error_category_name_required, Toast.LENGTH_SHORT).show();
            return false;
        }

        Category category = new Category();
        category.setId(categoryId);
        category.setName(name);
        category.setType(type);
        category.setIcon(iconAdapter.getSelectedKey());
        category.setColor(colorSwatchRow.getSelectedColor());

        CategoryRepository repository = MoneyTrackerApp.from(requireContext()).getCategoryRepository();
        if (categoryId == 0) {
            repository.insert(category, newId -> notifySaved(newId));
        } else {
            repository.update(category);
            notifySaved(categoryId);
        }
        return true;
    }

    private void notifySaved(long categoryId) {
        if (listener != null) {
            listener.onCategorySaved(categoryId);
        }
    }

    private void confirmDelete(long categoryId) {
        CategoryRepository repository = MoneyTrackerApp.from(requireContext()).getCategoryRepository();
        repository.countTransactionsUsing(categoryId, count -> {
            if (!isAdded()) {
                return;
            }
            String message = count == 0
                    ? getString(R.string.dialog_delete_category_message)
                    : getResources().getQuantityString(R.plurals.dialog_delete_category_message_used, count, count);
            AlertDialog confirm = new MaterialAlertDialogBuilder(requireContext(),
                    R.style.ThemeOverlay_MoneyTracker_Dialog)
                    .setTitle(R.string.dialog_delete_category_title)
                    .setMessage(message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_delete, (d, which) -> {
                        Category category = new Category();
                        category.setId(categoryId);
                        repository.delete(category);
                        dismiss();
                    })
                    .create();
            confirm.setOnShowListener((DialogInterface.OnShowListener) d -> {
                DialogButtonStyler.style(confirm);
                // Here the destructive action is the confirming one, so it gets the
                // warning color - matching DeleteAccountDialogFragment.
                confirm.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(requireContext().getColor(R.color.expense_red));
            });
            confirm.show();
        });
    }
}
