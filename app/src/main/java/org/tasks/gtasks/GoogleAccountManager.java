package org.tasks.gtasks;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

import android.accounts.Account;
import android.content.Context;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

public class GoogleAccountManager {

  private final PermissionChecker permissionChecker;
  private final android.accounts.AccountManager accountManager;

  @Inject
  public GoogleAccountManager(
      @ForApplication Context context, PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;

    accountManager = android.accounts.AccountManager.get(context);
  }

  public List<String> getAccounts() {
    return transform(getAccountList(), account -> account.name);
  }

  public boolean hasAccount(final String name) {
    return getAccount(name) != null;
  }

  private List<Account> getAccountList() {
    return permissionChecker.canAccessAccounts()
        ? asList(accountManager.getAccountsByType("com.google"))
        : Collections.emptyList();
  }

  public Account getAccount(final String name) {
    if (Strings.isNullOrEmpty(name)) {
      return null;
    }

    return tryFind(getAccountList(), account -> name.equalsIgnoreCase(account.name)).orNull();
  }
}
