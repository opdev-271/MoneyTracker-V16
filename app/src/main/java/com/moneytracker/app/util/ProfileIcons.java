package com.moneytracker.app.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.moneytracker.app.R;
import com.moneytracker.app.ui.widget.IconGridAdapter;

/** Maps a profile's stored pictogram key to its drawable resource. */
public class ProfileIcons {

    private static final Map<String, Integer> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put("pfp_cat", R.drawable.ic_pfp_cat);
        ICONS.put("pfp_dog", R.drawable.ic_pfp_dog);
        ICONS.put("pfp_fox", R.drawable.ic_pfp_fox);
        ICONS.put("pfp_owl", R.drawable.ic_pfp_owl);
        ICONS.put("pfp_bear", R.drawable.ic_pfp_bear);
        ICONS.put("pfp_rabbit", R.drawable.ic_pfp_rabbit);
        ICONS.put("pfp_money", R.drawable.ic_pfp_money);
        ICONS.put("pfp_star", R.drawable.ic_pfp_star);
    }

    private ProfileIcons() {
    }

    public static int resolve(String pictogramKey) {
        Integer resId = ICONS.get(pictogramKey);
        return resId != null ? resId : R.drawable.ic_pfp_cat;
    }

    public static List<IconGridAdapter.IconOption> allOptions() {
        List<IconGridAdapter.IconOption> options = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ICONS.entrySet()) {
            options.add(new IconGridAdapter.IconOption(entry.getKey(), entry.getValue()));
        }
        return options;
    }
}
