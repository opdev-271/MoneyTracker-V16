package com.moneytracker.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * A single income or expense entry, always tied to exactly one account and one
 * category. {@code amount} is always stored positive; {@code type} decides sign.
 */
@Entity(
        tableName = "transactions",
        foreignKeys = {
                @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "account_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "category_id", onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index("account_id"),
                @Index("category_id")
        }
)
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "account_id")
    private long accountId;

    @ColumnInfo(name = "category_id")
    private Long categoryId;

    @NonNull
    @ColumnInfo(name = "type")
    private String type = Category.TYPE_EXPENSE;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "counterparty")
    private String counterparty;

    @ColumnInfo(name = "note")
    private String note;

    @NonNull
    @ColumnInfo(name = "date")
    private Date date = new Date();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    public void setDate(@NonNull Date date) {
        this.date = date;
    }

    /** Signed amount: negative for expenses, positive for income. */
    public double getSignedAmount() {
        return Category.TYPE_EXPENSE.equals(type) ? -amount : amount;
    }
}
