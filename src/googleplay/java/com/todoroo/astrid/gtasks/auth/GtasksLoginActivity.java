/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;

import org.tasks.AccountManager;
import org.tasks.R;
import org.tasks.dialogs.AccountSelectionDialog;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;

import javax.inject.Inject;

/**
 * This activity allows users to sign in or log in to Google Tasks
 * through the Android account manager
 *
 * @author Sam Bosley
 *
 */
public class GtasksLoginActivity extends InjectingAppCompatActivity implements AccountSelectionDialog.AccountSelectionHandler {

    private static final String FRAG_TAG_ACCOUNT_SELECTION_DIALOG = "frag_tag_account_selection_dialog";

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
        } else if (accountManager.isEmpty()) {
            Toast.makeText(this, R.string.gtasks_GLA_noaccounts, Toast.LENGTH_LONG).show();
            finish();
        } else {
            FragmentManager fragmentManager = getFragmentManager();
            AccountSelectionDialog fragmentByTag = (AccountSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_ACCOUNT_SELECTION_DIALOG);
            if (fragmentByTag == null) {
                fragmentByTag = new AccountSelectionDialog();
                fragmentByTag.show(fragmentManager, FRAG_TAG_ACCOUNT_SELECTION_DIALOG);
            }
            fragmentByTag.setAccountSelectionHandler(this);
        }
    }

    private void getAuthToken(String account) {
        final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
        pd.show();
        accountName = account;
        getAuthToken(account, pd);
    }

    private void getAuthToken(String a, final ProgressDialog pd) {
        accountManager.getAuthToken(this, a, new AccountManager.AuthResultHandler() {
            @Override
            public void authenticationSuccessful(String accountName, String authToken) {
                gtasksPreferenceService.setUserName(accountName);
                gtasksInvoker.setUserName(accountName);
                setResult(RESULT_OK);
                finish();
                DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
            }

            @Override
            public void authenticationFailed(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(GtasksLoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
                DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AccountManager.REQUEST_AUTHORIZATION && resultCode == RESULT_OK){
            final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
            pd.show();
            getAuthToken(accountName, pd);
        } else {
            //User didn't give permission--cancel
            onCancel();
        }
    }

    @Override
    public void accountSelected(String account) {
        getAuthToken(account);
    }

    @Override
    public void onCancel() {
        finish();
    }
}
