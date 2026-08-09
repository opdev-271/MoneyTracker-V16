package com.moneytracker.app.util;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import com.moneytracker.app.R;

/**
 * Makes {@code android:elevation} actually produce a shadow, and tints that shadow warm.
 *
 * <p>Every raised surface in the app is a plain {@code LinearLayout} whose background is
 * a rounded {@code <shape>}. That combination silently casts <em>no shadow at all</em>:
 * {@link android.graphics.drawable.GradientDrawable#getOutline} sets the outline's alpha
 * to zero unless the drawable is opaque over its whole bounds, and a rounded rectangle
 * never is - it leaves the corners uncovered. The shadow the design called for was
 * therefore never drawn, and the depth people saw came entirely from the 1dp hairline
 * that used to sit on every card.
 *
 * <p>{@link #apply} fixes that at the root: it reuses the platform's own background
 * outline provider - which already derives the correct rounded-rect silhouette - and
 * only overrides the alpha that {@code GradientDrawable} refuses to set. On API 28+ it
 * also swaps the shadow's black for the theme's warm near-black.
 *
 * <p>Call it once per inflated tree: from an Activity after {@code setContentView}, from
 * a Fragment in {@code onViewCreated}, and from a RecyclerView ViewHolder's constructor
 * (list items are inflated long after their screen's tree was walked).
 */
public class ElevationShadows {

    private ElevationShadows() {
    }

    /**
     * The platform's BACKGROUND provider with the outline forced opaque. Everything else
     * about it - shape, corner radius, bounds - is exactly what the background drawable
     * reports, so a card's shadow always matches the card's own silhouette.
     */
    private static final ViewOutlineProvider SHADOW_CASTING_BACKGROUND = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            ViewOutlineProvider.BACKGROUND.getOutline(view, outline);
            outline.setAlpha(1f);
        }
    };

    /** Walks {@code root} and its descendants, fixing every view that asks for elevation. */
    public static void apply(View root) {
        if (root == null) {
            return;
        }
        applyToSelf(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i));
            }
        }
    }

    private static void applyToSelf(View view) {
        Drawable background = view.getBackground();
        if (background == null || view.getElevation() <= 0f) {
            return;
        }
        view.setOutlineProvider(SHADOW_CASTING_BACKGROUND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setOutlineAmbientShadowColor(view.getContext().getColor(R.color.shadow_ambient));
            view.setOutlineSpotShadowColor(view.getContext().getColor(R.color.shadow_spot));
        }
    }
}
