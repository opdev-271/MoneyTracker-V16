package com.moneytracker.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A single money account (cash, bank account, savings...). Each account has its
 * own currency; the running balance is initialBalance + sum of its transactions.
 * Every account belongs to exactly one profile.
 */
@Entity(
        tableName = "accounts",
        foreignKeys = @ForeignKey(entity = Profile.class, parentColumns = "id",
                childColumns = "profile_id", onDelete = ForeignKey.CASCADE),
        indices = @Index("profile_id")
)
public class Account {

    public static final String TYPE_CASH = "CASH";
    public static final String TYPE_BANK = "BANK";
    public static final String TYPE_SAVINGS = "SAVINGS";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "profile_id")
    private long profileId;

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @NonNull
    @ColumnInfo(name = "account_type")
    private String accountType = TYPE_CASH;

    @NonNull
    @ColumnInfo(name = "currency_code")
    private String currencyCode = "CZK";

    @ColumnInfo(name = "initial_balance")
    private double initialBalance;

    @ColumnInfo(name = "account_number")
    private String accountNumber;

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

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(@NonNull String accountType) {
        this.accountType = accountType;
    }

    @NonNull
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(@NonNull String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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
