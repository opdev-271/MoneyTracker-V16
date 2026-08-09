package com.moneytracker.app.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.moneytracker.app.R;
import com.moneytracker.app.ui.widget.IconGridAdapter;

/** Maps a category's stored icon key to its drawable resource. */
public class CategoryIcons {

    private static final Map<String, Integer> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put("ic_cat_food", R.drawable.ic_cat_food);
        ICONS.put("ic_cat_transport", R.drawable.ic_cat_transport);
        ICONS.put("ic_cat_shopping", R.drawable.ic_cat_shopping);
        ICONS.put("ic_cat_bills", R.drawable.ic_cat_bills);
        ICONS.put("ic_cat_entertainment", R.drawable.ic_cat_entertainment);
        ICONS.put("ic_cat_health", R.drawable.ic_cat_health);
        ICONS.put("ic_cat_home", R.drawable.ic_cat_home);
        ICONS.put("ic_cat_education", R.drawable.ic_cat_education);
        ICONS.put("ic_cat_travel", R.drawable.ic_cat_travel);
        ICONS.put("ic_cat_pets", R.drawable.ic_cat_pets);
        ICONS.put("ic_cat_coffee", R.drawable.ic_cat_coffee);
        ICONS.put("ic_cat_sport", R.drawable.ic_cat_sport);
        ICONS.put("ic_cat_fuel", R.drawable.ic_cat_fuel);
        ICONS.put("ic_cat_phone", R.drawable.ic_cat_phone);
        ICONS.put("ic_cat_subscription", R.drawable.ic_cat_subscription);
        ICONS.put("ic_cat_insurance", R.drawable.ic_cat_insurance);
        ICONS.put("ic_cat_salary", R.drawable.ic_cat_salary);
        ICONS.put("ic_cat_gift", R.drawable.ic_cat_gift);
        ICONS.put("ic_cat_savings", R.drawable.ic_cat_savings);
        ICONS.put("ic_cat_investment", R.drawable.ic_cat_investment);
        ICONS.put("ic_cat_other", R.drawable.ic_cat_other);
    }

    private CategoryIcons() {
    }

    /** True if the key names an icon that still exists in the set above. */
    public static boolean hasKey(String iconKey) {
        return iconKey != null && ICONS.containsKey(iconKey);
    }

    public static int resolve(String iconKey) {
        Integer resId = ICONS.get(iconKey);
        return resId != null ? resId : R.drawable.ic_cat_other;
    }

    public static List<IconGridAdapter.IconOption> allOptions() {
        List<IconGridAdapter.IconOption> options = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ICONS.entrySet()) {
            options.add(new IconGridAdapter.IconOption(entry.getKey(), entry.getValue()));
        }
        return options;
    }
}
