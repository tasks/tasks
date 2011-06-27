package com.todoroo.astrid.gtasks.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksService;

public class GtasksTokenValidator {

    private static GoogleAccountManager accountManager = new GoogleAccountManager(ContextManager.getContext());

    /**
     * Invalidates and then revalidates the auth token for the currently logged in user
     * @param token
     * @return
     */
    public static String validateAuthToken(String token) {
        Account a = accountManager.getAccountByName(Preferences.getStringValue(GtasksPreferenceService.PREF_USER_NAME));
        if (a == null) return null;

        accountManager.invalidateAuthToken(token);
        AccountManagerFuture<Bundle> future = accountManager.manager.getAuthToken(a, GtasksService.AUTH_TOKEN_TYPE, true, null, null);

        try {
            if (future.getResult().containsKey(AccountManager.KEY_AUTHTOKEN)) {
                Bundle result = future.getResult();
                return result.getString(AccountManager.KEY_AUTHTOKEN);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
