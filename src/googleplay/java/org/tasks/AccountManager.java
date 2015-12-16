package org.tasks;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.tasks.injection.ForApplication;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

public class AccountManager {

    public void clearToken(GoogleAccountCredential credential) throws IOException {
        try {
            String token = credential.getToken();
            Timber.d("Invalidating %s", token);
            GoogleAuthUtil.clearToken(context, token);
            GoogleAuthUtil.getTokenWithNotification(context, credential.getSelectedAccount(), "oauth2:" + TasksScopes.TASKS, null);
        } catch (GoogleAuthException e) {
            Timber.e(e, e.getMessage());
            throw new IOException(e);
        }
    }

    public interface AuthResultHandler {
        void authenticationSuccessful(String accountName, String authToken);
        void authenticationFailed(String message);
    }

    public static final int REQUEST_AUTHORIZATION = 10987;

    private final Context context;
    private final GoogleAccountManager googleAccountManager;

    @Inject
    public AccountManager(@ForApplication Context context) {
        this.context = context;

        googleAccountManager = new GoogleAccountManager(context);
    }

    public List<String> getAccounts() {
        return transform(getAccountList(), new Function<Account, String>() {
            @Nullable
            @Override
            public String apply(Account account) {
                return account.name;
            }
        });
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            handler.authenticationSuccessful(accountName, GoogleAuthUtil.getToken(activity, account, "oauth2:" + TasksScopes.TASKS, null));
                        } catch(UserRecoverableAuthException e) {
                            Timber.e(e, e.getMessage());
                            activity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                        } catch(GoogleAuthException | IOException e) {
                            Timber.e(e, e.getMessage());
                            handler.authenticationFailed(context.getString(R.string.gtasks_GLA_errorIOAuth));
                        }
                    }
                }).start();
        }
    }

    private List<Account> getAccountList() {
        return asList(googleAccountManager.getAccounts());
    }

    public Account getAccount(final String name) {
        return tryFind(getAccountList(), new Predicate<Account>() {
            @Override
            public boolean apply(Account account) {
                return name.equalsIgnoreCase(account.name);
            }
        }).orNull();
    }
}
