package org.tasks;

import android.accounts.Account;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class AccountManager {

    @Inject
    public AccountManager() {
    }

    public List<String> getAccounts() {
        return Collections.emptyList();
    }

    public Account getAccount(String userName) {
        return null;
    }
}
