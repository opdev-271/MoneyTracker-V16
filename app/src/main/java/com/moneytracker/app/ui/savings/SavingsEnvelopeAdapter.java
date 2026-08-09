package com.moneytracker.app.ui.savings;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.SavingsEnvelope;
import com.moneytracker.app.ui.widget.ColorSwatchRow;
import com.moneytracker.app.util.CurrencyFormatter;
import com.moneytracker.app.util.ViewAnimations;

public class SavingsEnvelopeAdapter extends ListAdapter<SavingsEnvelope, SavingsEnvelopeAdapter.ViewHolder> {

    public interface OnEnvelopeInteractionListener {
        /** Row tap: opens the "send money" dialog for this envelope. */
        void onEnvelopeClick(SavingsEnvelope envelope);

        /** Pencil icon tap: opens the edit/delete dialog. */
        void onEnvelopeEditClick(SavingsEnvelope envelope);
    }

    private final OnEnvelopeInteractionListener listener;
    private String currencyCode;
    private final int[] highestAnimatedPosition = {-1};

    public SavingsEnvelopeAdapter(OnEnvelopeInteractionListener listener, String currencyCode) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.currencyCode = currencyCode;
    }

    /** Called once the envelopes' own account (and thus its currency) has loaded. */
    public void setCurrencyCode(String currencyCode) {
        if (!this.currencyCode.equals(currencyCode)) {
            this.currencyCode = currencyCode;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings_envelope, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener, currencyCode);
        ViewAnimations.animateItemEntranceOnce(holder.itemView, position, highestAnimatedPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View dotColor;
        private final TextView textName;
        private final ImageButton btnEdit;
        private final ProgressBar progressBar;
        private final TextView textProgressLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            dotColor = itemView.findViewById(R.id.dot_color);
            textName = itemView.findViewById(R.id.text_name);
            btnEdit = itemView.findViewById(R.id.btn_edit_envelope);
            progressBar = itemView.findViewById(R.id.progress_envelope);
            textProgressLabel = itemView.findViewById(R.id.text_progress_label);
            ViewAnimations.addPressFeedback(itemView, btnEdit);
        }

        void bind(SavingsEnvelope envelope, OnEnvelopeInteractionListener listener, String currencyCode) {
            textName.setText(envelope.getName());

            int color = envelope.getColor() != 0 ? envelope.getColor() : ColorSwatchRow.DEFAULT_COLORS[1];
            GradientDrawable dot = (GradientDrawable) dotColor.getBackground().mutate();
            dot.setColor(color);

            double goal = envelope.getGoalAmount();
            double current = envelope.getCurrentAmount();
            int progress = goal > 0 ? (int) Math.min(1000, Math.round(current / goal * 1000)) : 0;
            progressBar.setProgress(progress);

            String currentText = CurrencyFormatter.format(current, currencyCode);
            String goalText = CurrencyFormatter.format(goal, currencyCode);
            textProgressLabel.setText(currentText + " / " + goalText);

            itemView.setOnClickListener(v -> listener.onEnvelopeClick(envelope));
            btnEdit.setOnClickListener(v -> listener.onEnvelopeEditClick(envelope));
        }
    }

    private static final DiffUtil.ItemCallback<SavingsEnvelope> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SavingsEnvelope>() {
                @Override
                public boolean areItemsTheSame(@NonNull SavingsEnvelope oldItem, @NonNull SavingsEnvelope newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull SavingsEnvelope oldItem, @NonNull SavingsEnvelope newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getGoalAmount() == newItem.getGoalAmount()
                            && oldItem.getCurrentAmount() == newItem.getCurrentAmount()
                            && oldItem.getColor() == newItem.getColor();
                }
            };
}
