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
package com.timsu.astrid.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.timsu.astrid.utilities.DialogUtilities;

/**
 * This activity displays a <code>WebView</code> that allows users to log in to the
 * synchronization provider requested. A callback method determines whether
 * their login was successful and therefore whether to dismiss the dialog.
 *
 * @author timsu
 *
 */
public class SyncLoginActivity extends Activity {

    // --- bundle arguments

    /**
     * URL to display
     */
    public static final String URL_TOKEN   = "u";

    /**
     * Resource for the label to display at the top of the screen
     */
    public static final String LABEL_TOKEN = "l";

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

    private static SyncLoginCallback callback = null;

    /** Sets callback method */
    public static void setCallback(SyncLoginCallback newCallback) {
        callback = newCallback;
    }

    // --- ui initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_login);

        String urlParam = getIntent().getStringExtra(URL_TOKEN);
        int labelParam = getIntent().getIntExtra(LABEL_TOKEN, 0);

        TextView label = (TextView)findViewById(R.id.login_label);
        final WebView webView = (WebView)findViewById(R.id.browser);
        Button done = (Button)findViewById(R.id.done);
        Button cancel = (Button)findViewById(R.id.cancel);

        if(labelParam != 0)
            label.setText(labelParam);

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
            @Override
            public void onClick(View v) {
            	final Handler handler = new Handler();

            	if(callback == null) {
                    finish();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
            			final String result = callback.verifyLogin(handler);
//                        TaskListSubActivity.syncPreferencesOpened = true;
            			if(result == null) {
//            				TaskList.synchronizeNow = true;
            				finish();
        			    } else {
        			    	// display the error
        			    	handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    DialogUtilities.okDialog(SyncLoginActivity.this, result,
                                    		new DialogInterface.OnClickListener() {
                                    	public void onClick(DialogInterface arg0, int arg1) {
//                                    		TaskListSubActivity.shouldRefreshTaskList = true;
                                    		finish();
                                    	}
                                    });
                                }
        			    	});
        			    }
                    }
                }).start();
            }
        });

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}