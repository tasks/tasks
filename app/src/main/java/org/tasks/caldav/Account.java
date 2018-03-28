package org.tasks.caldav;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import java.util.concurrent.TimeUnit;

public class Account {

  private static final String AUTHORITY = "org.tasks";

  private final AccountManager accountManager;
  private final android.accounts.Account account;

  public Account(AccountManager accountManager, android.accounts.Account account) {
    this.accountManager = accountManager;
    this.account = account;
  }

  public String getUuid() {
    return account.name;
  }

  String getPassword() {
    return accountManager.getPassword(account);
  }

  void setPassword(String password) {
    accountManager.setPassword(account, password);
  }

  public android.accounts.Account getAccount() {
    return account;
  }

  void setSynchronizationEnabled(boolean enabled) {
    ContentResolver.setSyncAutomatically(account, AUTHORITY, enabled);
    if (enabled) {
      ContentResolver
          .addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, TimeUnit.HOURS.toSeconds(1));
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
