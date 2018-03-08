package org.tasks.caldav;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class Account {

    private static final String AUTHORITY = "org.tasks";
    public static final String EXTRA_UUID = "uuid";

    private AccountManager accountManager;
    private android.accounts.Account account;

    public Account(AccountManager accountManager, android.accounts.Account account) {
        this.accountManager = accountManager;
        this.account = account;
    }

    public String getName() {
        return account.name;
    }

    public String getUuid() {
        return accountManager.getUserData(account, EXTRA_UUID);
    }

    String getPassword() {
        return accountManager.getPassword(account);
    }

    public android.accounts.Account getAccount() {
        return account;
    }

    void setPassword(String password) {
        accountManager.setPassword(account, password);
    }

    public void setUuid(String uuid) {
        accountManager.setUserData(account, EXTRA_UUID, uuid);
    }

    boolean isBackgroundSyncEnabled() {
        return ContentResolver.getSyncAutomatically(account, AUTHORITY);
    }

    void setSynchronizationEnabled(boolean enabled) {
        ContentResolver.setSyncAutomatically(account, AUTHORITY, enabled);
        if (enabled) {
            ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, TimeUnit.HOURS.toSeconds(1));
        } else {
            ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "account=" + account +
                '}';
    }
}
