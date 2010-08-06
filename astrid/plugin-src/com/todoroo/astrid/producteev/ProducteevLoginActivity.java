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
package com.todoroo.astrid.producteev;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;

/**
 * This activity displays a <code>WebView</code> that allows users to log in to
 * the synchronization provider requested. A callback method determines whether
 * their login was successful and therefore whether to dismiss the dialog.
 *
 * @author arne.jans
 *
 */
public class ProducteevLoginActivity extends Activity {

    @Autowired
    DialogUtilities dialogUtilities;

    // --- callback

    /** Callback interface */
    public interface SyncLoginCallback {
        /**
         * Verifies whether the user's login attempt was successful. Will be
         * called off of the UI thread, use the handler to post messages.
         *
         * @return error string, or null if sync was successful
         */
        public String verifyLogin(Handler handler, String email, String password);
    }

    protected static SyncLoginCallback callback = null;

    /** Sets callback method */
    public static void setCallback(SyncLoginCallback newCallback) {
        callback = newCallback;
    }

    // --- ui initialization

    public ProducteevLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.producteev_login_activity);

        final EditText emailEditText = (EditText) findViewById(R.id.Poducteev_EMail_EditText);
        final EditText passwordEditText = (EditText) findViewById(R.id.Producteev_Password_EditText);
        Button cancel = (Button) findViewById(R.id.cancel);
        Button login = (Button) findViewById(R.id.done);

        login.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final Handler handler = new Handler();

                if (callback == null) {
                    finish();
                    return;
                }

                final String email = emailEditText.getText().toString();
                final String password = passwordEditText.getText().toString();
                if (email == null || email.length() == 0) {
                    // no email given
                    Toast.makeText(ProducteevLoginActivity.this,
                            R.string.producteev_MLA_email_empty,
                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                    return;

                }
                if (password == null || password.length() == 0) {
                    // no password given
                    Toast.makeText(ProducteevLoginActivity.this,
                            R.string.producteev_MLA_password_empty,
                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                    return;

                }
                new Thread(new Runnable() {
                    public void run() {
                        final String result = callback.verifyLogin(handler,
                                email, password);
                        if (result == null) {
                            finish();
                        } else {
                            // display the error
                            handler.post(new Runnable() {
                                public void run() {
                                    dialogUtilities.okDialog(
                                            ProducteevLoginActivity.this,
                                            result, null);
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}