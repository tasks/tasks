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
package com.todoroo.astrid.actfm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.AuthListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.LoginButton;
import com.facebook.android.Util;
import com.timsu.astrid.C2DMReceiver;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TaskService;

/**
 * This activity allows users to sign in or log in to Producteev
 *
 * @author arne.jans
 *
 */
public class ActFmLoginActivity extends Activity implements AuthListener {

    public static final String APP_ID = "183862944961271"; //$NON-NLS-1$

    @Autowired TaskService taskService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    private final ActFmInvoker actFmInvoker = new ActFmInvoker();

    private Facebook facebook;
    private AsyncFacebookRunner facebookRunner;
    private TextView errors;
    private boolean noSync = false;

    // --- ui initialization

    static {
        AstridDependencyInjector.initialize();
    }

    public static final String EXTRA_DO_NOT_SYNC = "nosync"; //$NON-NLS-1$

    public ActFmLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @SuppressWarnings("nls")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setContentView(R.layout.sharing_login_activity);
        setTitle(R.string.sharing_SLA_title);

        noSync = getIntent().getBooleanExtra(EXTRA_DO_NOT_SYNC, false);

        facebook = new Facebook(APP_ID);
        facebookRunner = new AsyncFacebookRunner(facebook);

        errors = (TextView) findViewById(R.id.error);
        LoginButton loginButton = (LoginButton) findViewById(R.id.fb_login);
        loginButton.init(this, facebook, this, new String[] {
                "email",
                "offline_access",
                "publish_stream"
        });

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

        setResult(RESULT_CANCELED);
    }

    // --- facebook handler

    @SuppressWarnings("nls")
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        String error = data.getStringExtra("error");
        if (error == null) {
            error = data.getStringExtra("error_type");
        }
        String token = data.getStringExtra("access_token");
        if(error != null) {
            onFBAuthFail(error);
        } else if(token == null) {
            onFBAuthFail("Something went wrong! Please try again.");
        } else {
            facebook.setAccessToken(token);
            onFBAuthSucceed();
        }
        errors.setVisibility(View.GONE);
    }

    public void onFBAuthSucceed() {
        createUserAccountFB();
    }

    public void onFBAuthFail(String error) {
        DialogUtilities.okDialog(this, getString(R.string.sharing_SLA_title),
                android.R.drawable.ic_dialog_alert, error, null);
    }

    @Override
    public void onFBAuthCancel() {
        // do nothing
    }

    // --- astrid social handler

    ProgressDialog progressDialog;

    /**
     * Create user account via FB
     */
    public void createUserAccountFB() {
        progressDialog = DialogUtilities.progressDialog(this,
                getString(R.string.DLG_please_wait));
        facebookRunner.request("me", new SLARequestListener()); //$NON-NLS-1$
    }

    private class SLARequestListener implements RequestListener {

        @SuppressWarnings("nls")
        @Override
        public void onComplete(String response, Object state) {
            JSONObject json;
            try {
                json = Util.parseJson(response);
                String name = json.getString("name"); //$NON-NLS-1$
                String email = json.getString("email"); //$NON-NLS-1$

                JSONObject result = actFmInvoker.authenticate(email, name, ActFmInvoker.PROVIDER_FACEBOOK,
                        facebook.getAccessToken());

                String token = actFmInvoker.getToken();
                actFmPreferenceService.setToken(token);

                if(Preferences.getStringValue(R.string.actfm_APr_interval_key) == null)
                    Preferences.setStringFromInteger(R.string.actfm_APr_interval_key, 3600);

                Preferences.setLong(ActFmPreferenceService.PREF_USER_ID,
                        result.optLong("id"));
                Preferences.setString(ActFmPreferenceService.PREF_NAME, result.optString("name"));
                Preferences.setString(ActFmPreferenceService.PREF_EMAIL, result.optString("email"));
                Preferences.setString(ActFmPreferenceService.PREF_PICTURE, result.optString("picture"));

                C2DMReceiver.register();

                progressDialog.dismiss();
                setResult(RESULT_OK);
                finish();

                if(!noSync) {
                    synchronize();
                }

            } catch (Throwable e) {
                handleError(e);
            }
        }

        private void handleError(final Throwable e) {
            progressDialog.dismiss();
            Log.e("astrid-sharing", "error-doing-sla", e); //$NON-NLS-1$ //$NON-NLS-2$

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    errors.setText(e.toString());
                    errors.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onFacebookError(FacebookError e, Object state) {
            handleError(e);
        }

        @Override
        public void onFileNotFoundException(FileNotFoundException e,
                Object state) {
            handleError(e);
        }

        @Override
        public void onIOException(IOException e, Object state) {
            handleError(e);
        }

        @Override
        public void onMalformedURLException(MalformedURLException e,
                Object state) {
            handleError(e);
        }

    }

    public void synchronize() {
        startService(new Intent(null, null,
                this, ActFmBackgroundService.class));
    }

}