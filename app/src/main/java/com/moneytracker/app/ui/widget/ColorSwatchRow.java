package com.moneytracker.app.ui.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

import com.moneytracker.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-select set of color swatches (plain {@link View}s given an oval
 * {@link GradientDrawable} background), shared by the category, account and envelope
 * dialogs so a user-chosen accent color always comes from the same presets.
 *
 * <p>Two layouts: the default single horizontal row for the small palettes, and a
 * wrapping grid ({@code columns > 0}) for the larger {@link #CATEGORY_COLORS} palette,
 * which would otherwise run off the edge of a dialog.
 */
public class ColorSwatchRow {

    public static final int[] DEFAULT_COLORS = {
            0xFF181A12, 0xFFD9F55F, 0xFFB9B3F4, 0xFFF2907B, 0xFF8EC9F0, 0xFFF2C94C, 0xFF7ED9A7
    };

    /**
     * Wider palette for categories: the same hues the seeded categories use, plus
     * enough extra to give every category on a busy list its own recognizable color.
     * Kept to mid-saturation tones - a category color is used as a badge fill behind a
     * white icon and as an Analytics pie slice, so very light or very dark picks read
     * badly in one place or the other.
     */
    public static final int[] CATEGORY_COLORS = {
            0xFFF2907B, 0xFFE2674A, 0xFFD9534F, 0xFFE8739E,
            0xFFB9B3F4, 0xFF9B8CE8, 0xFF7C6FD6, 0xFF8EC9F0,
            0xFF5AA9E6, 0xFF4FC3C7, 0xFF7ED9A7, 0xFF5FBF7F,
            0xFFA9C93F, 0xFFD9F55F, 0xFFF2C94C, 0xFFE9A23B,
            0xFFC98B5E, 0xFF9AA091, 0xFF6F746A, 0xFF181A12
    };

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private final Context context;
    private final LinearLayout container;
    private final int[] colors;
    private final int columns;
    private final List<View> swatches = new ArrayList<>();
    private int selectedColor;
    private OnColorSelectedListener listener;

    public ColorSwatchRow(Context context, LinearLayout container, int[] colors, int initialSelectedColor) {
        this(context, container, colors, initialSelectedColor, 0);
    }

    /**
     * @param columns 0 lays every swatch out in {@code container} itself (which must be
     *                horizontal); anything larger fills {@code container} (which must be
     *                vertical) with rows of that many swatches.
     */
    public ColorSwatchRow(Context context, LinearLayout container, int[] colors,
                          int initialSelectedColor, int columns) {
        this.context = context;
        this.container = container;
        this.colors = colors;
        this.columns = columns;
        this.selectedColor = initialSelectedColor;
        build();
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    /** Fires on taps only, not on the initial selection passed to the constructor. */
    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    private void build() {
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (36 * density);
        int marginPx = (int) (8 * density);
        int strokePx = (int) (2.5f * density);

        container.removeAllViews();
        swatches.clear();

        LinearLayout row = null;
        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            if (columns > 0 && i % columns == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    rowParams.topMargin = marginPx;
                }
                row.setLayoutParams(rowParams);
                container.addView(row);
            }

            View swatch = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMarginEnd(marginPx);
            swatch.setLayoutParams(params);
            swatch.setTag(color);
            applySwatchDrawable(swatch, color, strokePx);
            swatch.setOnClickListener(v -> {
                selectedColor = color;
                refreshSelection(strokePx);
                if (listener != null) {
                    listener.onColorSelected(color);
                }
            });
            swatches.add(swatch);
            (columns > 0 ? row : container).addView(swatch);
        }
    }

    private void applySwatchDrawable(View swatch, int color, int strokePx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        if (color == selectedColor) {
            drawable.setStroke(strokePx, context.getColor(R.color.income_green));
        }
        swatch.setBackground(drawable);
    }

    private void refreshSelection(int strokePx) {
        for (View swatch : swatches) {
            applySwatchDrawable(swatch, (int) swatch.getTag(), strokePx);
        }
    }
}
