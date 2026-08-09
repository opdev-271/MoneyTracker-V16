package com.moneytracker.app.data.relation;

import androidx.room.Embedded;

import com.moneytracker.app.data.entity.Account;

/** An account plus its computed running balance (initial balance + all its transactions). */
public class AccountWithBalance {

    @Embedded
    public Account account;

    public double balance;

    /** Sum of all this account's savings envelopes' currentAmount; 0 for non-savings accounts. */
    public double envelopedTotal;
}
