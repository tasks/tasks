/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.astrid.gtasks.auth;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsService;

/**
 * This activity allows users to sign in or log in to Google Tasks
 * through the Android account manager
 *
 * @author Sam Bosley
 *
 */
public class GtasksLoginActivity extends ListActivity {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    // --- ui initialization

    private GoogleAccountManager accountManager;
    private String[] nameArray;

    private String authToken;
    private String accountName;

    static {
        AstridDependencyInjector.initialize();
    }

    public GtasksLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setTitle(R.string.gtasks_GLA_title);
        accountManager = new GoogleAccountManager(this);
        Account[] accounts = accountManager.getAccounts();
        ArrayList<String> accountNames = new ArrayList<String>();
        for (Account a : accounts) {
            accountNames.add(a.name);
        }

        nameArray = accountNames.toArray(new String[accountNames.size()]);

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nameArray));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final ProgressDialog pd = DialogUtilities.progressDialog(this, this.getString(R.string.gtasks_GLA_authenticating));
        pd.show();
        final Account a = accountManager.getAccountByName(nameArray[position]);
        accountName = a.name;
        getAuthToken(a, pd);
    }

    private void getAuthToken(Account a, final ProgressDialog pd) {
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            public void run(final AccountManagerFuture<Bundle> future) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Bundle bundle = future.getResult(60, TimeUnit.SECONDS);
                            if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                                Intent i = (Intent) bundle.get(AccountManager.KEY_INTENT);
                                startActivityForResult(i, REQUEST_AUTHENTICATE);
                            } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                                authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                                onAuthTokenSuccess();
                            }
                        } catch (Exception e) {
                            GtasksLoginActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(GtasksLoginActivity.this, R.string.gtasks_GLA_errorAuth, Toast.LENGTH_LONG).show();
                                }
                            });
                        } finally {
                            pd.dismiss();
                        }
                    }
                }.start();
            }
        };
        accountManager.manager.getAuthToken(a, GtasksService.AUTH_TOKEN_TYPE, true, callback, null);
    }

    private void onAuthCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void onAuthTokenSuccess() {
        gtasksPreferenceService.setToken(authToken);
        Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, accountName);
        synchronize();
    }

    /**
     * Perform synchronization
     */
    protected void synchronize() {
        startService(new Intent(null, null,
                this, GtasksBackgroundService.class));
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

    private static final int REQUEST_AUTHENTICATE = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_AUTHENTICATE && resultCode == RESULT_OK){
            final ProgressDialog pd = DialogUtilities.progressDialog(this, this.getString(R.string.gtasks_GLA_authenticating));
            pd.show();
            final Account a = accountManager.getAccountByName(accountName);
            getAuthToken(a, pd);
        } else {
            //User didn't give permission--cancel
            onAuthCancel();
        }
    }

}