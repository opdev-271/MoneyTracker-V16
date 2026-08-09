package com.moneytracker.app.util;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

/**
 * Picks a legible white/black foreground for an arbitrary background color, so a
 * user-chosen accent color (account badges, chip fills, ...) never has to be paired
 * with a fixed icon/text color that might coincidentally match it and disappear.
 */
public class ContrastColor {

    private ContrastColor() {
    }

    public static int forBackground(int backgroundColor) {
        double luminance = ColorUtils.calculateLuminance(backgroundColor);
        return luminance < 0.5 ? Color.WHITE : Color.BLACK;
    }
}
