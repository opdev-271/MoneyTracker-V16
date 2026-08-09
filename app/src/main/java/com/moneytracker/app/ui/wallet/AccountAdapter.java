package com.moneytracker.app.ui.wallet;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.relation.AccountWithBalance;
import com.moneytracker.app.ui.widget.ColorSwatchRow;
import com.moneytracker.app.util.AccountIcons;
import com.moneytracker.app.util.ContrastColor;
import com.moneytracker.app.util.CurrencyFormatter;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.util.ViewAnimations;

public class AccountAdapter extends ListAdapter<AccountWithBalance, AccountAdapter.ViewHolder> {

    public interface OnAccountClickListener {
        /** Row tap: drills into that account's own transactions (same concept as "All transactions"). */
        void onAccountClick(AccountWithBalance item);

        /** Pencil icon tap: opens the add/edit account dialog. */
        void onAccountEditClick(AccountWithBalance item);

        /** Trash icon tap: asks for confirmation before removing the account. */
        void onAccountDeleteClick(AccountWithBalance item);
    }

    private final OnAccountClickListener listener;
    private final int[] highestAnimatedPosition = {-1};

    public AccountAdapter(OnAccountClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
        ViewAnimations.animateItemEntranceOnce(holder.itemView, position, highestAnimatedPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageAccountType;
        private final TextView textName;
        private final TextView textCurrency;
        private final TextView textBalance;
        private final TextView textEnveloped;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAccountType = itemView.findViewById(R.id.image_account_type);
            textName = itemView.findViewById(R.id.text_name);
            textCurrency = itemView.findViewById(R.id.text_currency);
            textBalance = itemView.findViewById(R.id.text_balance);
            textEnveloped = itemView.findViewById(R.id.text_enveloped);
            btnEdit = itemView.findViewById(R.id.btn_edit_account);
            btnDelete = itemView.findViewById(R.id.btn_delete_account);
            ViewAnimations.addPressFeedback(itemView, btnEdit, btnDelete);
        }

        void bind(AccountWithBalance item, OnAccountClickListener listener) {
            int color = item.account.getColor() != 0 ? item.account.getColor() : ColorSwatchRow.DEFAULT_COLORS[1];
            GradientDrawable card = (GradientDrawable) itemView.getBackground().mutate();
            card.setColor(color);
            int onCard = ContrastColor.forBackground(color);
            int onCardMuted = ColorUtils.setAlphaComponent(onCard, 170);

            textName.setText(DisplayNameResolver.resolve(itemView.getContext(), item.account.getName()));
            textName.setTextColor(onCardMuted);
            textCurrency.setText(item.account.getCurrencyCode());
            textCurrency.setTextColor(onCardMuted);
            textBalance.setText(CurrencyFormatter.format(item.balance, item.account.getCurrencyCode()));
            textBalance.setTextColor(onCard);
            imageAccountType.setImageResource(AccountIcons.resolve(item.account.getAccountType()));
            imageAccountType.setColorFilter(onCard);
            btnEdit.setColorFilter(onCardMuted);
            btnDelete.setColorFilter(onCardMuted);

            boolean showEnveloped = Account.TYPE_SAVINGS.equals(item.account.getAccountType()) && item.envelopedTotal > 0;
            if (showEnveloped) {
                textEnveloped.setText(itemView.getContext().getString(R.string.envelope_summary_format,
                        CurrencyFormatter.format(item.envelopedTotal, item.account.getCurrencyCode())));
                textEnveloped.setTextColor(onCardMuted);
            }
            textEnveloped.setVisibility(showEnveloped ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onAccountClick(item));
            btnEdit.setOnClickListener(v -> listener.onAccountEditClick(item));
            btnDelete.setOnClickListener(v -> listener.onAccountDeleteClick(item));
        }
    }

    private static final DiffUtil.ItemCallback<AccountWithBalance> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AccountWithBalance>() {
                @Override
                public boolean areItemsTheSame(@NonNull AccountWithBalance oldItem, @NonNull AccountWithBalance newItem) {
                    return oldItem.account.getId() == newItem.account.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull AccountWithBalance oldItem, @NonNull AccountWithBalance newItem) {
                    return oldItem.account.getName().equals(newItem.account.getName())
                            && oldItem.account.getCurrencyCode().equals(newItem.account.getCurrencyCode())
                            && oldItem.account.getAccountType().equals(newItem.account.getAccountType())
                            && oldItem.account.getColor() == newItem.account.getColor()
                            && oldItem.balance == newItem.balance
                            && oldItem.envelopedTotal == newItem.envelopedTotal;
                }
            };
}
