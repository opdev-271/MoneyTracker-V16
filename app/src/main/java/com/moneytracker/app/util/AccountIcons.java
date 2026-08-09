package com.moneytracker.app.util;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;

/** Maps an account's type to its badge icon (currency stays a plain text label, not an icon). */
public class AccountIcons {

    private AccountIcons() {
    }

    public static int resolve(String accountType) {
        if (Account.TYPE_BANK.equals(accountType)) {
            return R.drawable.ic_account_bank;
        }
        if (Account.TYPE_SAVINGS.equals(accountType)) {
            return R.drawable.ic_account_savings;
        }
        return R.drawable.ic_account_cash;
    }
}
