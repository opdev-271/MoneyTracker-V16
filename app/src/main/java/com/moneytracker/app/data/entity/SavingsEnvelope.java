package com.moneytracker.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A named savings goal ("envelope") living inside one SAVINGS-type {@link Account}.
 * {@code currentAmount} is a subset of that account's own balance the user has
 * earmarked toward {@code goalAmount} - it never changes the account's balance by
 * itself; money only moves in via a real income/expense transaction pair recorded on
 * the source account and the savings account (see SavingsEnvelopeRepository).
 */
@Entity(
        tableName = "savings_envelopes",
        foreignKeys = @ForeignKey(entity = Account.class, parentColumns = "id",
                childColumns = "account_id", onDelete = ForeignKey.CASCADE),
        indices = @Index("account_id")
)
public class SavingsEnvelope {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "account_id")
    private long accountId;

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @ColumnInfo(name = "goal_amount")
    private double goalAmount;

    @ColumnInfo(name = "current_amount")
    private double currentAmount;

    @ColumnInfo(name = "color")
    private int color;

    @ColumnInfo(name = "sort_order")
    private int sortOrder;

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

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public double getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(double goalAmount) {
        this.goalAmount = goalAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
