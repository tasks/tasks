package org.tasks.gtasks;

import android.accounts.Account;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class GoogleAccountManager {

  @Inject
  public GoogleAccountManager() {}

  public List<String> getAccounts() {
    return Collections.emptyList();
  }

  private List<Account> getAccountList() {
    return Collections.emptyList();
  }

  public Account getAccount(final String name) {
    return null;
  }
}
