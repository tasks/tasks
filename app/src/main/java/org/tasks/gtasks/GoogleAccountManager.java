package org.tasks.gtasks;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.google.common.base.Strings;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

public class GoogleAccountManager {

    private static final String AUTHORITY = "org.tasks";

    private final PermissionChecker permissionChecker;
    private final android.accounts.AccountManager accountManager;
    private final GtasksPreferenceService gtasksPreferenceService;

    @Inject
    public GoogleAccountManager(@ForApplication Context context, PermissionChecker permissionChecker,
                                GtasksPreferenceService gtasksPreferenceService) {
        this.permissionChecker = permissionChecker;

        accountManager = android.accounts.AccountManager.get(context);
        this.gtasksPreferenceService = gtasksPreferenceService;
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

    public Account getSelectedAccount() {
        return getAccount(gtasksPreferenceService.getUserName());
    }

    public void setBackgroundSynchronization(boolean enabled) {
        Account account = getSelectedAccount();
        if (account != null) {
            Timber.d("enableBackgroundSynchronization=%s", enabled);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, enabled);
            if (enabled) {
                ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, TimeUnit.HOURS.toSeconds(1));
            } else {
                ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
            }
        }
    }

    public Account getAccount(final String name) {
        if (Strings.isNullOrEmpty(name)) {
            return null;
        }

        return tryFind(getAccountList(), account -> name.equalsIgnoreCase(account.name)).orNull();
    }
}
