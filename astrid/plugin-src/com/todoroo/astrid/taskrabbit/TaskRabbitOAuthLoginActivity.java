/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.taskrabbit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.DialogUtilities;

/**
 * This activity displays a <code>WebView</code> that allows users to log in to
 * the synchronization provider requested. A callback method determines whether
 * their login was successful and therefore whether to dismiss the dialog.
 *
 * @author timsu
 *
 */
public class TaskRabbitOAuthLoginActivity extends FragmentActivity {

    /**
     * URL to display
     */
    public static final String URL_TOKEN = "u"; //$NON-NLS-1$

    /**
     * Resultant URL data
     */
    public static final String DATA_RESPONSE = "response"; //$NON-NLS-1$

    @Autowired
    RestClient restClient;

    ProgressDialog pd;

    // --- ui initialization

    @SuppressWarnings("nls")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DependencyInjectionService.getInstance().inject(this);

        setContentView(R.layout.oauth_login_activity);
        getSupportActionBar().setTitle(R.string.actfm_OLA_prompt);

        final String urlParam = getIntent().getStringExtra(URL_TOKEN);

        final WebView webView = (WebView) findViewById(R.id.browser);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                System.err.println("hey error. " + errorCode + ": " + description);
            }

            @Override
            public void onReceivedSslError(WebView view,
                    SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("?client_id=")
                        && (url.lastIndexOf("?client_id=") != url.indexOf("?client_id="))) {
                    String redirectUrl = url.substring(0,
                            url.lastIndexOf("?client_id="));
                    webView.loadUrl(redirectUrl);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                super.onPageFinished(view, url);
                pd.dismiss();
                if (url.contains("access_token=")) {
                    String token = url.substring(url.indexOf("access_token="),
                            url.length());
                    Intent intent = new Intent();
                    intent.putExtra(DATA_RESPONSE, token);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        pd = DialogUtilities.progressDialog(this, getString(R.string.DLG_wait));
        webView.loadUrl(urlParam);
    }

}
