package org.tasks.caldav;

import static com.google.common.collect.Iterables.tryFind;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import com.google.common.base.Optional;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import timber.log.Timber;

@ApplicationScope
public class CaldavAccountManager {

  private static final String AUTHORITY = "org.tasks";
  private static final String ACCOUNT_TYPE = "org.tasks.caldav";

  private final PermissionChecker permissionChecker;
  private final android.accounts.AccountManager accountManager;
  private final LocalBroadcastManager localBroadcastManager;
  private final TaskDeleter taskDeleter;
  private final CaldavDao caldavDao;
  private final TaskDao taskDao;

  @Inject
  public CaldavAccountManager(@ForApplication Context context, PermissionChecker permissionChecker,
      CaldavDao caldavDao, TaskDeleter taskDeleter,
      LocalBroadcastManager localBroadcastManager, TaskDao taskDao) {
    this.permissionChecker = permissionChecker;
    this.caldavDao = caldavDao;
    this.taskDao = taskDao;
    this.taskDeleter = taskDeleter;
    this.localBroadcastManager = localBroadcastManager;
    accountManager = android.accounts.AccountManager.get(context);
    syncAccountList();
  }

  public Account getAccount(String uuid) {
    for (Account account : getAccounts()) {
      if (uuid.equals(account.getUuid())) {
        return account;
      }
    }
    return null;
  }

  private List<Account> getAccounts() {
    if (!permissionChecker.canAccessAccounts()) {
      return Collections.emptyList();
    }

    List<Account> accounts = new ArrayList<>();
    for (android.accounts.Account account : accountManager.getAccountsByType(ACCOUNT_TYPE)) {
      accounts.add(new Account(accountManager, account));
    }
    return accounts;
  }

  boolean removeAccount(Account account) {
    return removeAccount(account.getAccount());
  }

  boolean removeAccount(android.accounts.Account account) {
    try {
      return accountManager
          .removeAccount(account, null, null)
          .getResult();
    } catch (OperationCanceledException | IOException | AuthenticatorException e) {
      Timber.e(e.getMessage(), e);
    }
    return false;
  }

  boolean addAccount(CaldavAccount caldavAccount, String password) {
    Timber.d("Adding %s", caldavAccount);
    android.accounts.Account account = new android.accounts.Account(caldavAccount.getUuid(),
        ACCOUNT_TYPE);
    return accountManager.addAccountExplicitly(account, password, null);
  }

  private void syncAccountList() {
    List<CaldavAccount> oldAccountList = caldavDao.getAllOrderedByName();
    List<Account> newAccountList = getAccounts();

    for (CaldavAccount local : oldAccountList) {
      Optional<Account> match = tryFind(newAccountList,
          remote -> local.getUuid().equals(remote.getUuid()));
      if (!match.isPresent()) {
        addAccount(local, null);
      }
    }
  }

  void deleteAccount(CaldavAccount account) {
    String uuid = account.getUuid();
    for (Task task : taskDao.getCaldavTasks(uuid)) {
      taskDeleter.markDeleted(task);
    }
    caldavDao.deleteTasksForAccount(uuid);
    caldavDao.delete(account);
    localBroadcastManager.broadcastRefreshList();
  }

  public boolean initiateManualSync() {
    for (org.tasks.caldav.Account account : getAccounts()) {
      Bundle extras = new Bundle();
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
      ContentResolver.requestSync(account.getAccount(), AUTHORITY, extras);
    }
    return true;
  }

  public void requestSynchronization() {
    for (org.tasks.caldav.Account account : getAccounts()) {
      ContentResolver.requestSync(account.getAccount(), AUTHORITY, new Bundle());
    }
  }

  public void setBackgroundSynchronization(boolean enabled) {
    for (org.tasks.caldav.Account account : getAccounts()) {
      account.setSynchronizationEnabled(enabled);
    }
  }
}
