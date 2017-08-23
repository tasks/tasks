/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth;

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.services.tasks.TasksScopes;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;

import org.tasks.AccountManager;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * This activity allows users to sign in or log in to Google Tasks
 * through the Android account manager
 *
 * @author Sam Bosley
 *
 */
public class GtasksLoginActivity extends InjectingAppCompatActivity {

    public interface AuthResultHandler {
        void authenticationSuccessful(String accountName);
        void authenticationFailed(String message);
    }

    private static final int RC_REQUEST_OAUTH = 10987;
    private static final int RC_CHOOSE_ACCOUNT = 10988;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject DialogBuilder dialogBuilder;
    @Inject AccountManager accountManager;
    @Inject GtasksInvoker gtasksInvoker;

    private String accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String existingUsername = gtasksPreferenceService.getUserName();
        if (existingUsername != null && accountManager.hasAccount(existingUsername)) {
            getAuthToken(existingUsername);
        } else {
            Intent chooseAccountIntent = android.accounts.AccountManager.newChooseAccountIntent(
                    null, null, new String[]{"com.google"}, false, null, null, null, null);
            startActivityForResult(chooseAccountIntent, RC_CHOOSE_ACCOUNT);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private void getAuthToken(String account) {
        final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
        pd.show();
        accountName = account;
        getAuthToken(account, pd);
    }

    private void getAuthToken(String a, final ProgressDialog pd) {
        getAuthToken(this, a, new AuthResultHandler() {
            @Override
            public void authenticationSuccessful(String accountName) {
                gtasksPreferenceService.setUserName(accountName);
                gtasksInvoker.setUserName(accountName);
                setResult(RESULT_OK);
                finish();
                DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
            }

            @Override
            public void authenticationFailed(final String message) {
                runOnUiThread(() -> Toast.makeText(GtasksLoginActivity.this, message, Toast.LENGTH_LONG).show());
                DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CHOOSE_ACCOUNT && resultCode == RESULT_OK) {
            String account = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
            getAuthToken(account);
        } else if (requestCode == RC_REQUEST_OAUTH && resultCode == RESULT_OK) {
            final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
            pd.show();
            getAuthToken(accountName, pd);
        } else {
            //User didn't give permission--cancel
            finish();
        }
    }

    private void getAuthToken(final Activity activity, final String accountName, final AuthResultHandler handler) {
        final Account account = accountManager.getAccount(accountName);
        if (account == null) {
            handler.authenticationFailed(activity.getString(R.string.gtasks_error_accountNotFound, accountName));
        } else {
            new Thread(() -> {
                try {
                    GoogleAuthUtil.getToken(activity, account, "oauth2:" + TasksScopes.TASKS, null);
                    handler.authenticationSuccessful(accountName);
                } catch(UserRecoverableAuthException e) {
                    Timber.e(e, e.getMessage());
                    activity.startActivityForResult(e.getIntent(), RC_REQUEST_OAUTH);
                } catch(GoogleAuthException | IOException e) {
                    Timber.e(e, e.getMessage());
                    handler.authenticationFailed(getString(R.string.gtasks_GLA_errorIOAuth));
                }
            }).start();
        }
    }
}
