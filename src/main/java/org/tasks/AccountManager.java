package org.tasks;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

public class AccountManager {

    private static final Logger log = LoggerFactory.getLogger(AccountManager.class);

    public interface AuthResultHandler {
        void authenticationSuccessful(String accountName, String authToken);

        void authenticationFailed(String message);
    }

    private GoogleAccountManager googleAccountManager;
    private Activity activity;

    @Inject
    public AccountManager(Activity activity) {
        this.activity = activity;

        googleAccountManager = new GoogleAccountManager(activity);
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

    public void getAuthToken(final String accountName, final AuthResultHandler handler) {
        Account account = getAccount(accountName);
        if (account == null) {
            handler.authenticationFailed(activity.getString(R.string.gtasks_error_accountNotFound, accountName));
        } else {
            googleAccountManager.getAccountManager().getAuthToken(account, GtasksInvoker.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(final AccountManagerFuture<Bundle> future) {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Bundle bundle = future.getResult(30, TimeUnit.SECONDS);
                                if (bundle.containsKey(android.accounts.AccountManager.KEY_AUTHTOKEN)) {
                                    handler.authenticationSuccessful(accountName, bundle.getString(android.accounts.AccountManager.KEY_AUTHTOKEN));
                                } else {
                                    log.error("No auth token found in response bundle");
                                    handler.authenticationFailed(activity.getString(R.string.gtasks_error_accountNotFound, accountName));
                                }
                            } catch (final Exception e) {
                                log.error(e.getMessage(), e);
                                handler.authenticationFailed(activity.getString(e instanceof IOException
                                        ? R.string.gtasks_GLA_errorIOAuth
                                        : R.string.gtasks_GLA_errorAuth));
                            }
                        }
                    }.start();
                }
            }, null);
        }
    }

    private List<Account> getAccountList() {
        return asList(googleAccountManager.getAccounts());
    }

    private Account getAccount(final String name) {
        return tryFind(getAccountList(), new Predicate<Account>() {
            @Override
            public boolean apply(Account account) {
                return name.equalsIgnoreCase(account.name);
            }
        }).orNull();
    }
}
