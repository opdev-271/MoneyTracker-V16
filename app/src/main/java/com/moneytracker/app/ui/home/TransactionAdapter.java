package com.moneytracker.app.ui.home;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
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
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.util.CategoryIcons;
import com.moneytracker.app.util.ContrastColor;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.CurrencyFormatter;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.ui.widget.OnTransactionInteractionListener;
import com.moneytracker.app.util.TransactionColors;
import com.moneytracker.app.util.ViewAnimations;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class TransactionAdapter extends ListAdapter<TransactionWithDetails, TransactionAdapter.ViewHolder> {

    private final int[] highestAnimatedPosition = {-1};
    private final OnTransactionInteractionListener listener;
    private Set<Long> selectedIds = Collections.emptySet();

    public TransactionAdapter(OnTransactionInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSelectedIds(Set<Long> selectedIds) {
        this.selectedIds = selectedIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionWithDetails item = getItem(position);
        holder.bind(item, selectedIds.contains(item.id), listener);
        ViewAnimations.animateItemEntranceOnce(holder.itemView, position, highestAnimatedPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout badgeIcon;
        private final ImageView imageDirection;
        private final TextView textTitle;
        private final TextView textSubtitle;
        private final TextView textAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeIcon = itemView.findViewById(R.id.badge_icon);
            imageDirection = itemView.findViewById(R.id.image_direction);
            textTitle = itemView.findViewById(R.id.text_title);
            textSubtitle = itemView.findViewById(R.id.text_subtitle);
            textAmount = itemView.findViewById(R.id.text_amount);
            ElevationShadows.apply(itemView);
        }

        void bind(TransactionWithDetails item, boolean selected, OnTransactionInteractionListener listener) {
            Context context = itemView.getContext();
            boolean isExpense = Category.TYPE_EXPENSE.equals(item.type);

            // The badge icons are authored white, but a category's color is user-chosen out
            // of a palette that runs from near-black to pale lime, so the tint has to be
            // derived from whatever fill it lands on - same as the account cards do.
            int badgeColor;
            if (item.categoryIcon != null) {
                imageDirection.setImageResource(CategoryIcons.resolve(item.categoryIcon));
                badgeColor = item.categoryColor != 0 ? item.categoryColor
                        : context.getColor(R.color.text_tertiary);
            } else {
                imageDirection.setImageResource(isExpense ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                badgeColor = context.getColor(R.color.surface_variant);
            }
            GradientDrawable badge = (GradientDrawable) badgeIcon.getBackground().mutate();
            badge.setColor(badgeColor);
            imageDirection.setColorFilter(ContrastColor.forBackground(badgeColor));

            String title = !TextUtils.isEmpty(item.counterparty)
                    ? item.counterparty
                    : DisplayNameResolver.resolve(context, item.categoryName);
            textTitle.setText(title);

            String relation = context.getString(isExpense ? R.string.sent_by_you : R.string.received_by_you);
            String date = new SimpleDateFormat("MMM d", Locale.getDefault()).format(item.date);
            textSubtitle.setText(context.getString(R.string.subtitle_format, relation, date));

            double signed = isExpense ? -item.amount : item.amount;
            textAmount.setText(CurrencyFormatter.formatSigned(signed, item.accountCurrency));
            textAmount.setTextColor(TransactionColors.amountColor(context, item.type));

            itemView.setBackgroundResource(selected ? R.drawable.bg_row_selected : R.drawable.bg_card_white_small);
            itemView.setOnLongClickListener(v -> {
                listener.onTransactionLongClick(item.id);
                return true;
            });
            itemView.setOnClickListener(v -> listener.onTransactionClick(item.id));
        }
    }

    private static final DiffUtil.ItemCallback<TransactionWithDetails> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TransactionWithDetails>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransactionWithDetails oldItem, @NonNull TransactionWithDetails newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransactionWithDetails oldItem, @NonNull TransactionWithDetails newItem) {
                    return oldItem.amount == newItem.amount
                            && oldItem.type.equals(newItem.type)
                            && TextUtils.equals(oldItem.counterparty, newItem.counterparty)
                            && TextUtils.equals(oldItem.categoryName, newItem.categoryName)
                            && oldItem.date.equals(newItem.date);
                }
            };
}
