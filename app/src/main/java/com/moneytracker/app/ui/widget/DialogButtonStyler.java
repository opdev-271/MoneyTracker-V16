package com.moneytracker.app.ui.widget;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.moneytracker.app.R;

/**
 * Every DialogFragment builds with {@code MaterialAlertDialogBuilder} and
 * {@code R.style.ThemeOverlay_MoneyTracker_Dialog}, which already colors the positive
 * button via {@code colorPrimary}; this just differentiates the negative ("Cancel") and
 * neutral ("Delete") actions so a destructive action doesn't share the same accent as
 * the confirming one, and gives the dialog surface a visible hairline border.
 *
 * <p>Button views don't exist until the dialog is actually shown, so callers must invoke
 * this from inside their own {@code setOnShowListener} callback - typically the same one
 * already used to override the positive button's click behavior - rather than this class
 * installing a second, mutually-clobbering listener.
 */
public class DialogButtonStyler {

    private DialogButtonStyler() {
    }

    public static void style(AlertDialog dialog) {
        android.widget.Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negative != null) {
            negative.setTextColor(dialog.getContext().getColor(R.color.text_secondary));
        }
        android.widget.Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutral != null) {
            neutral.setTextColor(dialog.getContext().getColor(R.color.expense_red));
        }
        applySurfaceBorder(dialog);
    }

    /**
     * Since v10 {@code surface} equals {@code background}, so a dialog's edge - the
     * bottom especially - can visually disappear against whatever sits behind it.
     * Material builds the window background as a (possibly inset-wrapped)
     * {@link MaterialShapeDrawable}; rather than replacing it (and re-deriving its
     * insets), reach into it and add the same 1dp divider hairline the app's cards
     * use, plus re-assert the 28dp corner radius that newer platforms may override
     * via {@code android:dialogCornerRadius}.
     */
    private static void applySurfaceBorder(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        Drawable background = window.getDecorView().getBackground();
        if (background instanceof InsetDrawable) {
            background = ((InsetDrawable) background).getDrawable();
        }
        if (!(background instanceof MaterialShapeDrawable)) {
            return;
        }
        MaterialShapeDrawable shape = (MaterialShapeDrawable) background;
        float density = dialog.getContext().getResources().getDisplayMetrics().density;
        shape.setShapeAppearanceModel(
                ShapeAppearanceModel.builder().setAllCornerSizes(28f * density).build());
        shape.setStroke(1f * density, dialog.getContext().getColor(R.color.divider));
    }
}
