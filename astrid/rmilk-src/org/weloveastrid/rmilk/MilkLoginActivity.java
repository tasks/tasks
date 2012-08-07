/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;

/**
 * This activity displays a <code>WebView</code> that allows users to log in to the
 * synchronization provider requested. A callback method determines whether
 * their login was successful and therefore whether to dismiss the dialog.
 *
 * @author timsu
 *
 */
public class MilkLoginActivity extends Activity {

    // --- bundle arguments

    /**
     * URL to display
     */
    public static final String URL_TOKEN   = "u"; //$NON-NLS-1$

    // --- callback

    /** Callback interface */
    public interface SyncLoginCallback {
        /**
         * Verifies whether the user's login attempt was successful. Will be
         * called off of the UI thread, use the handler to post messages.
         *
         * @return error string, or null if sync was successful
         */
        public String verifyLogin(Handler handler);
    }

    protected static SyncLoginCallback callback = null;

    /** Sets callback method */
    public static void setCallback(SyncLoginCallback newCallback) {
        callback = newCallback;
    }

    // --- ui initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rmilk_login_activity);
        setTitle(R.string.rmilk_MPr_header);

        final String urlParam = getIntent().getStringExtra(URL_TOKEN);

        final WebView webView = (WebView)findViewById(R.id.browser);
        Button done = (Button)findViewById(R.id.done);
        Button cancel = (Button)findViewById(R.id.cancel);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setSupportZoom(true);
        webView.loadUrl(urlParam);

        done.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	final Handler handler = new Handler();

            	if(callback == null) {
                    finish();
                    return;
                }

                new Thread(new Runnable() {
                    public void run() {
            			final String result = callback.verifyLogin(handler);
            			if(result == null) {
            				finish();
        			    } else {
        			    	// display the error, re-load url
        			    	handler.post(new Runnable() {
                                public void run() {
                                    DialogUtilities.okDialog(MilkLoginActivity.this,
                                            result, null);
                                    webView.loadUrl(urlParam);
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
