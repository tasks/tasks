/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GoogleTasksException;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;

@SuppressWarnings("nls")
public class GtasksTokenValidator {

    private static final String TOKEN_INTENT_RECEIVED = "intent!"; //$NON-NLS-1$

    private static final int REVALIDATION_TRIES = 4;
    /**
     * Invalidates and then revalidates the auth token for the currently logged in user
     * Shouldn't be called from the main thread--will block on network calls
     * @param token
     * @return valid token on success, null on failure
     */
    public static synchronized String validateAuthToken(Context c, String token) throws GoogleTasksException {
        GoogleAccountManager accountManager = new GoogleAccountManager(ContextManager.getContext());

        if(testToken(token))
            return token;

        // If fail, token may have expired -- get a new one and return that
        String accountName = Preferences.getStringValue(GtasksPreferenceService.PREF_USER_NAME);
        Account a = accountManager.getAccountByName(accountName);
        if (a == null) {
            throw new GoogleTasksException(c.getString(R.string.gtasks_error_accountNotFound, accountName), "account-not-found");
        }

        for (int i = 0; i < REVALIDATION_TRIES; i++) {
            accountManager.invalidateAuthToken(token);

            // try with notify-auth-failure = false
            AccountManagerFuture<Bundle> future = accountManager.manager.getAuthToken(a, GtasksInvoker.AUTH_TOKEN_TYPE, false, null, null);
            token = getTokenFromFuture(c, future);

            if(TOKEN_INTENT_RECEIVED.equals(token))
                return null;
            else if(token != null && testToken(token))
                return token;
        }


        throw new GoogleTasksException(c.getString(R.string.gtasks_error_authRefresh), "auth-token-refresh");
    }

    private static boolean testToken(String token) {
        GtasksInvoker testService = new GtasksInvoker(token);
        try {
            testService.ping();
            return true;
        } catch (IOException i) {
            return false;
        }
    }

    private static String getTokenFromFuture(Context c, AccountManagerFuture<Bundle> future)
            throws GoogleTasksException {
        Bundle result;
        try {
            result = future.getResult();
            if(result == null)
                throw new NullPointerException("Future result was null."); //$NON-NLS-1$
        } catch (Exception e) {
            throw new GoogleTasksException(e.getLocalizedMessage(), "future-error");
        }

        // check what kind of result was returned
        String token;
        if (result.containsKey(AccountManager.KEY_AUTHTOKEN)) {
            token = result.getString(AccountManager.KEY_AUTHTOKEN);
        } else if (result.containsKey(AccountManager.KEY_INTENT)) {
            Intent intent = (Intent) result.get(AccountManager.KEY_INTENT);
            if (c instanceof Activity)
                c.startActivity(intent);
            else
                throw new GoogleTasksException(c.getString(R.string.gtasks_error_background_sync_auth), "background-auth-error");
            return TOKEN_INTENT_RECEIVED;
        } else {
            throw new GoogleTasksException(c.getString(R.string.gtasks_error_accountManager), "account-manager-error");
        }
        return token;
    }
}
