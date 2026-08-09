package com.moneytracker.app.util;

import android.content.Context;

import com.moneytracker.app.data.entity.Category;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orders categories the way the user reads them.
 *
 * <p>{@code CategoryDao} sorts by {@code name ASC}, but a seeded category's stored name is
 * a lookup key ({@code "category_food"}), not a label — so SQL sorts the keys while the
 * list shows translations, and the two only happened to agree because every seeded key
 * shared the {@code category_} prefix. As soon as one category is renamed (or created) it
 * stores literal text and jumps somewhere arbitrary in that ordering. Sorting on the
 * resolved, locale-collated label instead keeps the list alphabetical in whatever
 * language the app is currently showing.
 *
 * <p>Pass a locale-aware {@link Context} (an Activity or Fragment context, which
 * {@code LocaleHelper} has already configured) — not the raw Application.
 */
public class CategorySorter {

    private CategorySorter() {
    }

    public static List<Category> byDisplayName(Context context, List<Category> categories) {
        List<Category> sorted = new ArrayList<>(categories != null ? categories : Collections.emptyList());
        Collator collator = Collator.getInstance(
                context.getResources().getConfiguration().getLocales().get(0));
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(sorted, (a, b) -> collator.compare(
                DisplayNameResolver.resolve(context, a.getName()),
                DisplayNameResolver.resolve(context, b.getName())));
        return sorted;
    }
}
