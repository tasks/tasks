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

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googlelogin.GoogleLoginServiceHelper;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncBackgroundService;
import com.todoroo.gtasks.GoogleConnectionManager;
import com.todoroo.gtasks.GoogleLoginException;
import com.todoroo.gtasks.GoogleTasksException;

/**
 * This activity allows users to sign in or log in to Producteev
 *
 * @author arne.jans
 *
 */
public class GtasksLoginActivity extends Activity {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    // --- ui initialization

    public GtasksLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setContentView(R.layout.gtasks_login_activity);
        setTitle(R.string.gtasks_GLA_title);

        final TextView errors = (TextView) findViewById(R.id.error);
        final EditText emailEditText = (EditText) findViewById(R.id.email);
        final EditText passwordEditText = (EditText) findViewById(R.id.password);
        final CheckBox isDomain = (CheckBox) findViewById(R.id.isDomain);

        Button signIn = (Button) findViewById(R.id.signIn);
        signIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                errors.setVisibility(View.GONE);
                Editable email = emailEditText.getText();
                Editable password = passwordEditText.getText();
                if(email.length() == 0 || password.length() == 0) {
                    errors.setVisibility(View.VISIBLE);
                    errors.setText(R.string.producteev_PLA_errorEmpty);
                    return;
                }

                performLogin(email.toString(), password.toString(), isDomain.isChecked());
            }

        });

        getCredentials(new OnGetCredentials() {
            @Override
            public void getCredentials(String[] accounts) {
                if(accounts != null && accounts.length > 0)
                    emailEditText.setText(accounts[0]);
            }
        });

    }


    private void performLogin(final String email, final String password, final boolean isDomain) {
        final ProgressDialog dialog = DialogUtilities.progressDialog(this,
                getString(R.string.DLG_wait));
        final TextView errors = (TextView) findViewById(R.id.error);
        dialog.show();
        new Thread() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                final StringBuilder errorMessage = new StringBuilder();
                GoogleConnectionManager gcm = new GoogleConnectionManager(email.toString(),
                        password.toString(), !isDomain);
                try {
                    gcm.authenticate(false);
                    gcm.get();
                    String token = gcm.getToken();
                    gtasksPreferenceService.setToken(token);
                    StatisticsService.reportEvent("gtasks-login");
                    Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, email);
                    Preferences.setString(GtasksPreferenceService.PREF_PASSWORD, password);
                    Preferences.setBoolean(GtasksPreferenceService.PREF_IS_DOMAIN, isDomain);

                    synchronize();
                } catch (GoogleLoginException e) {
                    errorMessage.append(getString(R.string.gtasks_GLA_errorAuth));
                    Log.e("gtasks", "login-auth", e);
                    return;
                } catch (GoogleTasksException e) {
                    errorMessage.append(getString(R.string.gtasks_GLA_errorAuth));
                    Log.e("gtasks", "login-gtasks", e);
                } catch (JSONException e) {
                    errorMessage.append(getString(R.string.gtasks_GLA_errorAuth));
                    Log.e("gtasks", "login-json", e);
                } catch (IOException e) {
                    errorMessage.append(getString(R.string.SyP_ioerror));
                    Log.e("gtasks", "login-io", e);
                    return;
                } finally {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.dismiss();
                            if(errorMessage.length() > 0) {
                                errors.setVisibility(View.VISIBLE);
                                errors.setText(errorMessage);
                            }
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Perform synchronization
     */
    protected void synchronize() {
        startService(new Intent(SyncBackgroundService.SYNC_ACTION, null,
                this, GtasksBackgroundService.class));
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

    // --- account management


    private static final int REQUEST_CODE_GOOGLE = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_GOOGLE){
            String accounts[] = data.getExtras().getStringArray(GoogleLoginServiceConstants.ACCOUNTS_KEY);
            credentialsListener.getCredentials(accounts);
        }
    }
    public interface OnGetCredentials {
        public void getCredentials(String[] accounts);
    }

    private OnGetCredentials credentialsListener;

    public void getCredentials(OnGetCredentials onGetCredentials) {
        credentialsListener = onGetCredentials;
        if(Integer.parseInt(Build.VERSION.SDK) >= 7)
            credentialsListener.getCredentials(ModernAuthManager.getAccounts(this));
        else
            GoogleLoginServiceHelper.getAccount(this, REQUEST_CODE_GOOGLE, false);
    }

}