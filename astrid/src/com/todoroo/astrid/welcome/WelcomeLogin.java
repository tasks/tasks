package com.todoroo.astrid.welcome;

import org.json.JSONObject;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.android.AuthListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.data.Task;

public class WelcomeLogin extends ActFmLoginActivity implements AuthListener {

    // --- ui initialization

    public static final String KEY_SHOWED_WELCOME_LOGIN = "key_showed_welcome_login"; //$NON-NLS-1$

    @Override
    protected int getContentViewResource() {
        return R.layout.welcome_login_activity;
    }

    @Override
    protected int getTitleResource() {
        return R.string.welcome_login_title;
    }

    @Override
    protected void recordPageView() {
        // don't record, every new user hits this page
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);
        if (Preferences.getBoolean(KEY_SHOWED_WELCOME_LOGIN, false)) {
            finishAndShowNext();
        }

        initializeUI();
    }

    @Override
    protected void finishAndShowNext() {
        Intent welcomeScreen = new Intent(this, WelcomeGraphic.class);
        welcomeScreen.putExtra(WelcomeGraphic.START_SYNC, true);
        noSync = true; // For superclass
        startActivity(welcomeScreen);
        finish();
        Preferences.setBoolean(KEY_SHOWED_WELCOME_LOGIN, true);
    }

    @Override
    protected void initializeUI() {
        findViewById(R.id.gg_login).setOnClickListener(googleListener);
        setupTermsOfService();
        setupPWLogin();
        setupLoginLater();
    }

    private SpannableString getLinkStringWithCustomInterval(String base, String linkComponent,
                                                            int start, int endOffset, final OnClickListener listener) {
        SpannableString link = new SpannableString (String.format("%s %s", //$NON-NLS-1$
                base, linkComponent));
        ClickableSpan linkSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                listener.onClick(widget);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(true);
                ds.setColor(Color.rgb(255, 255, 255));
            }
        };
        link.setSpan(linkSpan, start, link.length() + endOffset, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return link;
    }

    private void setupTermsOfService() {
        TextView tos = (TextView)findViewById(R.id.tos);
        tos.setOnClickListener(showTosListener);

        String tosBase = getString(R.string.welcome_login_tos_base);
        String tosLink = getString(R.string.welcome_login_tos_link);
        SpannableString link = getLinkStringWithCustomInterval(tosBase, tosLink, tosBase.length() + 2, -1, showTosListener);
        tos.setText(link);
    }

    private void setupPWLogin() {
        Button pwLogin = (Button) findViewById(R.id.pw_login);
        pwLogin.setOnClickListener(signUpListener);
    }

    private void setupLoginLater() {
        TextView loginLater = (TextView)findViewById(R.id.login_later);
        loginLater.setOnClickListener(loginLaterListener);
        String loginLaterBase = getString(R.string.welcome_login_later);
        SpannableString loginLaterLink = new SpannableString(String.format("%s", loginLaterBase)); //$NON-NLS-1$
        ClickableSpan laterSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                loginLaterListener.onClick(widget);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(true);
                ds.setColor(Color.rgb(255, 255, 255));
            }
        };
        loginLaterLink.setSpan(laterSpan, 0, loginLaterBase.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginLater.setText(loginLaterLink);
    }

    // --- event handler

    private final OnClickListener showTosListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Eula.showEulaBasic(WelcomeLogin.this);
        }
    };

    private final OnClickListener loginLaterListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            String title = getString(R.string.welcome_login_confirm_later_title);
            String confirmLater = getString(R.string.welcome_login_confirm_later_dialog);
            DialogUtilities.okCancelCustomDialog(WelcomeLogin.this, title, confirmLater,
                    R.string.welcome_login_confirm_later_ok,
                    R.string.welcome_login_confirm_later_cancel,
                    null, confirmLaterListener);
        }

        private final DialogInterface.OnClickListener confirmLaterListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAndShowNext();
            }
        };
    };

    @Override
    protected void postAuthenticate(JSONObject result, String token) {
        // Delete the "Setup sync" task on successful login
        taskService.deleteWhere(Task.TITLE.eq(getString(R.string.intro_task_3_summary)));
        super.postAuthenticate(result, token);
    }
}
