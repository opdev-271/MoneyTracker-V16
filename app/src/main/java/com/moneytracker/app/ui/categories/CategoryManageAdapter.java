package com.moneytracker.app.ui.categories;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.util.CategoryIcons;
import com.moneytracker.app.util.ContrastColor;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.util.ViewAnimations;

import java.util.Objects;

/** One row per category on the management screen: its badge, its name, tap to edit. */
public class CategoryManageAdapter extends ListAdapter<Category, CategoryManageAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final OnCategoryClickListener listener;
    private final int[] highestAnimatedPosition = {-1};

    public CategoryManageAdapter(OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
        ViewAnimations.animateItemEntranceOnce(holder.itemView, position, highestAnimatedPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout badge;
        private final ImageView icon;
        private final TextView textName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            badge = itemView.findViewById(R.id.badge_icon);
            icon = itemView.findViewById(R.id.image_icon);
            textName = itemView.findViewById(R.id.text_name);
            ViewAnimations.addPressFeedback(itemView);
            ElevationShadows.apply(itemView);
        }

        void bind(Category category, OnCategoryClickListener listener) {
            textName.setText(DisplayNameResolver.resolve(itemView.getContext(), category.getName()));
            icon.setImageResource(CategoryIcons.resolve(category.getIcon()));

            int color = category.getColor() != 0
                    ? category.getColor()
                    : itemView.getContext().getColor(R.color.text_tertiary);
            GradientDrawable badgeBg = (GradientDrawable) badge.getBackground().mutate();
            badgeBg.setColor(color);
            badge.setBackground(badgeBg);
            // The palette runs from near-black to pale lime, so a fixed white icon would
            // disappear on the light end of it.
            icon.setColorFilter(ContrastColor.forBackground(color));

            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Category>() {
                @Override
                public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && Objects.equals(oldItem.getIcon(), newItem.getIcon())
                            && oldItem.getColor() == newItem.getColor();
                }
            };
}
