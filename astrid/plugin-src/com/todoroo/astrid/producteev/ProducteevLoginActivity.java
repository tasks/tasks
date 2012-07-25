/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import java.util.TimeZone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.producteev.api.ApiAuthenticationException;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;

/**
 * This activity allows users to sign in or log in to Producteev
 *
 * @author arne.jans
 *
 */
public class ProducteevLoginActivity extends Activity {

    // --- ui initialization

    public ProducteevLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setContentView(R.layout.producteev_login_activity);
        setTitle(R.string.producteev_PLA_title);

        // terms clicking
        findViewById(R.id.terms).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.producteev.com/#terms"))); //$NON-NLS-1$
            }
        });

        final TextView errors = (TextView) findViewById(R.id.error);
        final EditText emailEditText = (EditText) findViewById(R.id.email);
        final EditText passwordEditText = (EditText) findViewById(R.id.password);
        final View newUserLayout = findViewById(R.id.newUserLayout);
        final Spinner timezoneList = (Spinner) findViewById(R.id.timezoneList);

        String[] timezoneEntries = getResources().getStringArray(R.array.PLA_timezones_list);
        String defaultTimeZone = TimeZone.getDefault().getID();
        int selected = 0;
        for(int i = 0; i < timezoneEntries.length; i++) {
            if(timezoneEntries[i].equals(defaultTimeZone))
                selected = i;
        }
        timezoneList.setSelection(selected);

        Button signIn = (Button) findViewById(R.id.signIn);
        signIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                errors.setVisibility(View.GONE);
                if(newUserLayout.getVisibility() == View.VISIBLE)
                    newUserLayout.setVisibility(View.GONE);
                else {
                    Editable email = emailEditText.getText();
                    Editable password = passwordEditText.getText();
                    if(email.length() == 0 || password.length() == 0) {
                        errors.setVisibility(View.VISIBLE);
                        errors.setText(R.string.producteev_PLA_errorEmpty);
                        return;
                    }

                    performLogin(email.toString(), password.toString());
                }
            }

        });

        Button createNew = (Button) findViewById(R.id.createNew);
        createNew.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                errors.setVisibility(View.GONE);
                if(newUserLayout.getVisibility() != View.VISIBLE)
                    newUserLayout.setVisibility(View.VISIBLE);
                else {
                    Editable email = emailEditText.getText();
                    Editable password = passwordEditText.getText();
                    Editable firstName = ((EditText)findViewById(R.id.firstName)).getText();
                    Editable lastName = ((EditText)findViewById(R.id.lastName)).getText();
                    String timezone = timezoneList.getSelectedItem().toString();
                    if(email.length() == 0 || password.length() == 0 ||
                            firstName.length() == 0 ||
                            lastName.length() == 0) {
                        errors.setVisibility(View.VISIBLE);
                        errors.setText(R.string.producteev_PLA_errorEmpty);
                        return;
                    }
                    performSignup(email.toString(), password.toString(),
                            firstName.toString(), lastName.toString(), timezone);
                }
            }
        });

    }


    private void performLogin(final String email, final String password) {
        final ProgressDialog dialog = DialogUtilities.progressDialog(this,
                getString(R.string.DLG_wait));
        final TextView errors = (TextView) findViewById(R.id.error);
        dialog.show();
        new Thread() {
            @Override
            public void run() {
                ProducteevInvoker invoker = ProducteevSyncProvider.getInvoker();
                final StringBuilder errorMessage = new StringBuilder();
                try {
                    invoker.authenticate(email, password);

                    Preferences.setString(R.string.producteev_PPr_email, email);
                    Preferences.setString(R.string.producteev_PPr_password, password);
                    ProducteevUtilities.INSTANCE.setToken(invoker.getToken());

                    StatisticsService.reportEvent(StatisticsConstants.PRODUCTEEV_LOGIN);

                    synchronize();
                } catch (ApiAuthenticationException e) {
                    errorMessage.append(getString(R.string.producteev_PLA_errorAuth));
                } catch (Exception e) {
                    errorMessage.append(e.getMessage());
                } finally {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DialogUtilities.dismissDialog(ProducteevLoginActivity.this, dialog);
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

    private void performSignup(final String email, final String password,
            final String firstName, final String lastName, final String timezone) {
        final ProgressDialog dialog = DialogUtilities.progressDialog(this,
                getString(R.string.DLG_wait));
        final TextView errors = (TextView) findViewById(R.id.error);
        dialog.show();
        new Thread() {
            @Override
            public void run() {
                ProducteevInvoker invoker = ProducteevSyncProvider.getInvoker();
                final StringBuilder errorMessage = new StringBuilder();
                try {
                    invoker.usersSignUp(email, firstName, lastName, password, timezone, null);
                    invoker.authenticate(email, password);

                    Preferences.setString(R.string.producteev_PPr_email, email);
                    Preferences.setString(R.string.producteev_PPr_password, password);
                    ProducteevUtilities.INSTANCE.setToken(invoker.getToken());

                    StatisticsService.reportEvent(StatisticsConstants.PRODUCTEEV_SIGNUP);

                    synchronize();
                } catch (Exception e) {
                    errorMessage.append(e.getMessage());
                } finally {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DialogUtilities.dismissDialog(ProducteevLoginActivity.this, dialog);
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
        startService(new Intent(null, null,
                this, ProducteevBackgroundService.class));
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

}
