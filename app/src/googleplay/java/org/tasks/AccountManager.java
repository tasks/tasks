package org.tasks;

import android.accounts.Account;
import android.content.Context;

import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.common.base.Strings;

import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

public class AccountManager {

    private final PermissionChecker permissionChecker;
    private final GoogleAccountManager googleAccountManager;

    @Inject
    public AccountManager(@ForApplication Context context, PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;

        googleAccountManager = new GoogleAccountManager(context);
    }

    public List<String> getAccounts() {
        return transform(getAccountList(), account -> account.name);
    }

    public boolean hasAccount(final String name) {
        return getAccount(name) != null;
    }

    public boolean isEmpty() {
        return getAccounts().isEmpty();
    }

    private List<Account> getAccountList() {
        return permissionChecker.canAccessAccounts()
                ? asList(googleAccountManager.getAccounts())
                : Collections.emptyList();
    }

    public Account getAccount(final String name) {
        if (Strings.isNullOrEmpty(name)) {
            return null;
        }

        return tryFind(getAccountList(), account -> name.equalsIgnoreCase(account.name)).orNull();
    }
}
