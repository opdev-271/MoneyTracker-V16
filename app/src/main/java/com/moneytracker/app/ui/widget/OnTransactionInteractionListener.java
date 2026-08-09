package com.moneytracker.app.ui.widget;

/** Long-click starts/extends a selection, click toggles membership while one is active. */
public interface OnTransactionInteractionListener {
    void onTransactionLongClick(long transactionId);

    void onTransactionClick(long transactionId);
}
