package com.moneytracker.app.ui.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytracker.app.R;

import java.util.List;
import java.util.Objects;

/**
 * Single-select grid of icon choices, shared by the onboarding avatar step, the
 * profile add/edit dialog, and the category add/edit dialog — anywhere the user
 * picks one pictogram out of a fixed set. The icon tint is fixed (some icon sets
 * are drawn for a light cell, others for a dark one); only the background swaps,
 * gaining a colored ring, to show which cell is selected.
 */
public class IconGridAdapter extends RecyclerView.Adapter<IconGridAdapter.ViewHolder> {

    public static class IconOption {
        public final String key;
        public final int drawableRes;

        public IconOption(String key, int drawableRes) {
            this.key = key;
            this.drawableRes = drawableRes;
        }
    }

    public interface OnIconSelectedListener {
        void onIconSelected(String key);
    }

    private final List<IconOption> options;
    private final int iconTint;
    private final int unselectedBackgroundRes;
    private final int selectedBackgroundRes;
    private String selectedKey;
    private OnIconSelectedListener listener;

    public IconGridAdapter(List<IconOption> options, String initialSelectedKey, int iconTint,
                            int unselectedBackgroundRes, int selectedBackgroundRes) {
        this.options = options;
        this.selectedKey = initialSelectedKey;
        this.iconTint = iconTint;
        this.unselectedBackgroundRes = unselectedBackgroundRes;
        this.selectedBackgroundRes = selectedBackgroundRes;
    }

    public void setOnIconSelectedListener(OnIconSelectedListener listener) {
        this.listener = listener;
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_grid_cell, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconOption option = options.get(position);
        boolean selected = Objects.equals(option.key, selectedKey);

        holder.image.setImageResource(option.drawableRes);
        holder.image.setColorFilter(iconTint);
        holder.root.setBackgroundResource(selected ? selectedBackgroundRes : unselectedBackgroundRes);

        holder.itemView.setOnClickListener(v -> {
            int previousIndex = indexOf(selectedKey);
            selectedKey = option.key;
            if (previousIndex != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousIndex);
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
            if (listener != null) {
                listener.onIconSelected(selectedKey);
            }
        });
    }

    private int indexOf(String key) {
        for (int i = 0; i < options.size(); i++) {
            if (Objects.equals(options.get(i).key, key)) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout root;
        final ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.cell_root);
            image = itemView.findViewById(R.id.image_icon);
        }
    }
}
