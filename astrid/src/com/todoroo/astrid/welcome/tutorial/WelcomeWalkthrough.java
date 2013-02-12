/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.welcome.tutorial;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.ActFmGoogleAuthActivity;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.viewpagerindicator.CirclePageIndicator;

public class WelcomeWalkthrough extends ActFmLoginActivity {
    private ViewPager mPager;
    private WelcomePagerAdapter mAdapter;
    private Account[] accounts;
    private CirclePageIndicator mIndicator;
    private View currentView;
    private int currentPage;

    private String authToken;
    private boolean onSuccess = false;
    private boolean dismissDialog = false;

    public static final String KEY_SHOWED_WELCOME_LOGIN = "key_showed_welcome_login"; //$NON-NLS-1$

    public static final String TOKEN_MANUAL_SHOW = "manual"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mAdapter = new WelcomePagerAdapter(this, getIntent().hasExtra(TOKEN_MANUAL_SHOW));
        mAdapter.parent = this;
        accounts = mAdapter.accounts;

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        if (mAdapter.getCount() <= 1) {
            mIndicator.setVisibility(View.GONE);
        }

    }
    @Override
    protected int getContentViewResource() {
        return R.layout.welcome_walkthrough;
    }

    @Override
    protected int getTitleResource() {
        return 0;
    }

    public void instantiatePage(int position){
        if (position == mAdapter.getCount()-1) {
            initializeUI();
        }
    }

    private int getLoginPageLayout() {
        return mAdapter.fallbackLoginPage;
    }

    @Override
    protected void initializeUI() {
        String email = null;
        if (accounts != null && accounts.length > 0 && !TextUtils.isEmpty(accounts[0].name)) {
            email = accounts[0].name;
        }
        Button simpleLogin = (Button) findViewById(R.id.quick_login_google);
        if (simpleLogin != null && !TextUtils.isEmpty(email)) {
            initializeSimpleUI(email);
        } else {
            if (mAdapter != null && mAdapter.layouts[mAdapter.layouts.length - 1] != getLoginPageLayout())
                mAdapter.changeLoginPage(getLoginPageLayout());
            super.initializeUI();
        }
    }

    private void initializeSimpleUI(final String email) {
        Button simpleLogin = (Button) findViewById(R.id.quick_login_google);
        simpleLogin.setText(getString(R.string.actfm_quick_login, email));
        simpleLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsService.reportEvent(StatisticsConstants.ACTFM_LOGIN_SIMPLE);
                final ProgressDialog pd = DialogUtilities.progressDialog(WelcomeWalkthrough.this, getString(R.string.gtasks_GLA_authenticating));
                pd.show();
                getAuthToken(email, pd);
            }

            private void getAuthToken(final String e,
                    final ProgressDialog pd) {
                final GoogleAccountManager accountManager = new GoogleAccountManager(WelcomeWalkthrough.this);
                Account a = accountManager.getAccountByName(e);
                AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                    public void run(final AccountManagerFuture<Bundle> future) {
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Bundle bundle = future.getResult(30, TimeUnit.SECONDS);
                                    if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                                        authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                                        if (!onSuccess) {
                                            accountManager.manager.invalidateAuthToken(ActFmGoogleAuthActivity.AUTH_TOKEN_TYPE, authToken);
                                            getAuthToken(e, pd);
                                            onSuccess = true;
                                        } else {
                                            onAuthTokenSuccess(e, authToken);
                                            dismissDialog = true;
                                        }
                                    } else {
                                        dismissDialog = true;
                                    }
                                } catch (final Exception e) {
                                    Log.e("actfm-google-auth", "Login Error", e); //$NON-NLS-1$ //$NON-NLS-2$
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int error = e instanceof IOException ? R.string.gtasks_GLA_errorIOAuth :
                                                R.string.gtasks_GLA_errorAuth;
                                            Toast.makeText(WelcomeWalkthrough.this,
                                                    error,
                                                    Toast.LENGTH_LONG).show();
                                            onAuthError();
                                        }
                                    });
                                } finally {
                                    if (dismissDialog)
                                        DialogUtilities.dismissDialog(WelcomeWalkthrough.this, pd);
                                }
                            }
                        }.start();
                    }
                };
                accountManager.manager.getAuthToken(a, ActFmGoogleAuthActivity.AUTH_TOKEN_TYPE, null, WelcomeWalkthrough.this, callback, null);
            }
        });

        TextView rejectQuickLogin = (TextView) findViewById(R.id.quick_login_reject);
        rejectQuickLogin.setText(getString(R.string.actfm_quick_login_reject, email));
        rejectQuickLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsService.reportEvent(StatisticsConstants.ACTFM_LOGIN_SIMPLE_REJECTED);
                switchToLoginPage();
            }
        });

        errors = (TextView) findViewById(R.id.error);
    }

    private void onAuthTokenSuccess(final String email, final String authToken) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authenticate(email, email, "", "google", authToken); //$NON-NLS-1$ //$NON-NLS-2$
            }
        });
    }

    private void onAuthError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchToLoginPage();
            }
        });
    }

    private void switchToLoginPage() {
        mAdapter.changeLoginPage(getLoginPageLayout());
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(mAdapter.layouts.length - 1, false);
        initializeUI();
    }

    public void onPageChanged(View view, int position) {
        currentPage = position;
        currentView = view;
        findViewById(R.id.next).setVisibility(
                position == mAdapter.getCount() - 1 ? View.GONE : View.VISIBLE);

        if(currentPage == mAdapter.getCount() - 1) {
            if(findViewById(R.id.fb_login) != null) {
                setupLoginLater();
            } else {
                OnClickListener done = new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        finish();
                    }
                };
                View title = currentView.findViewById(R.id.welcome_walkthrough_title);
                if (title != null)
                    title.setOnClickListener(done);
                View image = currentView.findViewById(R.id.welcome_walkthrough_image);
                if (image != null)
                    image.setOnClickListener(done);
            }
        }
        ((CirclePageIndicator) mIndicator).setVisibility(currentPage == mAdapter.getCount()-1 ? View.GONE : View.VISIBLE);
    }

    protected void setupPWLogin() {
        Button pwLogin = (Button) findViewById(R.id.pw_signup);
        pwLogin.setOnClickListener(signUpListener);
    }

    protected void setupLoginLater() {
        TextView loginLater = (TextView)currentView.findViewById(R.id.login_later);
        loginLater.setOnClickListener(loginLaterListener);
        loginLater.setVisibility(View.VISIBLE);
    }


    protected final OnClickListener loginLaterListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            String title = getString(R.string.welcome_login_confirm_later_title);
            String confirmLater = getString(R.string.welcome_login_confirm_later_dialog);
            DialogUtilities.okCancelCustomDialog(WelcomeWalkthrough.this, title, confirmLater,
                    R.string.welcome_login_confirm_later_ok,
                    R.string.welcome_login_confirm_later_cancel,
                    android.R.drawable.ic_dialog_alert,
                    null, confirmLaterListener);
        }

        private final DialogInterface.OnClickListener confirmLaterListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
    };

}
