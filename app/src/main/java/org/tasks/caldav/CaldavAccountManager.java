package org.tasks.caldav;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;

import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Iterables.tryFind;
import static org.tasks.caldav.Account.EXTRA_UUID;

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

    public String getUuid(android.accounts.Account account) {
        return accountManager.getUserData(account, EXTRA_UUID);
    }

    public Account getAccount(String uuid) {
        for (Account account : getAccounts()) {
            if (uuid.equals(account.getUuid())) {
                return account;
            }
        }
        return null;
    }

    public List<Account> getAccounts() {
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
        android.accounts.Account account = new android.accounts.Account(caldavAccount.getName(), ACCOUNT_TYPE);
        Bundle userdata = new Bundle();
        userdata.putString(EXTRA_UUID, caldavAccount.getUuid());
        return accountManager.addAccountExplicitly(account, password, userdata);
    }

    private void createAccount(Account account) {
        Timber.d("Adding %s", account);
        String uuid = account.getUuid();
        if (!Strings.isNullOrEmpty(uuid)) {
            caldavDao.insert(new CaldavAccount(account.getName(), uuid));
        }
    }

    private void syncAccountList() {
        List<CaldavAccount> oldAccountList = caldavDao.getAllOrderedByName();
        List<Account> newAccountList = getAccounts();

        for (CaldavAccount local : oldAccountList) {
            Optional<Account> match = tryFind(newAccountList, remote -> local.getUuid().equals(remote.getUuid()));
            if (match.isPresent()) {
                Timber.d("found %s", match.get());
            } else {
                addAccount(local, null);
            }
        }

        for (Account remote : newAccountList) {
            Optional<CaldavAccount> match = tryFind(oldAccountList, local -> remote.getUuid().equals(local.getUuid()));
            if (match.isPresent()) {
                Timber.d("found %s", match.get());
            } else {
                createAccount(remote);
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
}
