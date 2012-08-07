/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth;

import java.io.IOException;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.SyncV2Service;

/**
 * This activity allows users to sign in or log in to Google Tasks
 * through the Android account manager
 *
 * @author Sam Bosley
 *
 */
public class GtasksLoginActivity extends ListActivity {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Autowired private SyncV2Service syncService;

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


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gtasks_login_activity);
        TextView header = new TextView(this);
        header.setText(R.string.actfm_GAA_title);
        header.setTextAppearance(this, R.style.TextAppearance_Medium);
        header.setPadding(10, 0, 10, 50);
        getListView().addHeaderView(header);

        accountManager = new GoogleAccountManager(this);
        Account[] accounts = accountManager.getAccounts();
        ArrayList<String> accountNames = new ArrayList<String>();
        for (Account a : accounts) {
            accountNames.add(a.name);
        }

        nameArray = accountNames.toArray(new String[accountNames.size()]);

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nameArray));
        findViewById(R.id.empty_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAuthCancel();
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        int offsetPosition = position - 1; // Subtract 1 because apparently android counts the header view as part of the adapter.
        if (offsetPosition >= 0 && offsetPosition < nameArray.length) {
            final ProgressDialog pd = DialogUtilities.progressDialog(this, this.getString(R.string.gtasks_GLA_authenticating));
            pd.show();
            final Account a = accountManager.getAccountByName(nameArray[offsetPosition]);
            accountName = a.name;
            getAuthToken(a, pd);
        }
    }

    private void getAuthToken(Account a, final ProgressDialog pd) {
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            public void run(final AccountManagerFuture<Bundle> future) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Bundle bundle = future.getResult(30, TimeUnit.SECONDS);
                            if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                                authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                                onAuthTokenSuccess();
                            }
                        } catch (final Exception e) {
                            Log.e("gtasks-login", "Login Error", e); //$NON-NLS-1$ //$NON-NLS-2$
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int error = e instanceof IOException ? R.string.gtasks_GLA_errorIOAuth :
                                        R.string.gtasks_GLA_errorAuth;
                                    Toast.makeText(GtasksLoginActivity.this,
                                            error,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        } finally {
                            DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
                        }
                    }
                }.start();
            }
        };
        accountManager.manager.getAuthToken(a, GtasksInvoker.AUTH_TOKEN_TYPE, null, this, callback, null);
    }

    private void onAuthCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void onAuthTokenSuccess() {
        gtasksPreferenceService.setToken(authToken);
        Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, accountName);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
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
