package com.moneytracker.app.data.relation;

import androidx.room.ColumnInfo;

import java.util.Date;

/**
 * Flattened projection of a transaction joined with its account and category,
 * exactly what the Home and Wallet lists need to render a row without extra
 * lookups.
 */
public class TransactionWithDetails {

    public long id;

    @ColumnInfo(name = "account_id")
    public long accountId;

    @ColumnInfo(name = "category_id")
    public Long categoryId;

    public String type;

    public double amount;

    public String counterparty;

    public String note;

    public Date date;

    @ColumnInfo(name = "account_name")
    public String accountName;

    @ColumnInfo(name = "account_currency")
    public String accountCurrency;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "category_icon")
    public String categoryIcon;

    @ColumnInfo(name = "category_color")
    public int categoryColor;

    /** Signed amount: negative for expenses, positive for income. */
    public double getSignedAmount() {
        return "EXPENSE".equals(type) ? -amount : amount;
    }
}
