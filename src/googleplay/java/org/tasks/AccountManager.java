package org.tasks;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Strings;

import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

public class AccountManager {

    public interface AuthResultHandler {
        void authenticationSuccessful(String accountName);
        void authenticationFailed(String message);
    }

    public static final int REQUEST_AUTHORIZATION = 10987;

    private final Context context;
    private final PermissionChecker permissionChecker;
    private final GoogleAccountManager googleAccountManager;

    @Inject
    public AccountManager(@ForApplication Context context, PermissionChecker permissionChecker) {
        this.context = context;
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

    public void getAuthToken(final Activity activity, final String accountName, final AuthResultHandler handler) {
        final Account account = getAccount(accountName);
        if (account == null) {
            handler.authenticationFailed(activity.getString(R.string.gtasks_error_accountNotFound, accountName));
        } else {
                new Thread(() -> {
                    try {
                        GoogleAuthUtil.getToken(activity, account, "oauth2:" + TasksScopes.TASKS, null);
                        handler.authenticationSuccessful(accountName);
                    } catch(UserRecoverableAuthException e) {
                        Timber.e(e, e.getMessage());
                        activity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    } catch(GoogleAuthException | IOException e) {
                        Timber.e(e, e.getMessage());
                        handler.authenticationFailed(context.getString(R.string.gtasks_GLA_errorIOAuth));
                    }
                }).start();
        }
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
