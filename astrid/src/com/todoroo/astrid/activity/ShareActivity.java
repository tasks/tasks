package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;

public class ShareActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.share_activity);
        TextView fb = (TextView) findViewById(R.id.share_facebook);
        setUpTextView(fb, getString(R.string.share_with_facebook), "http://facebook.com/weloveastrid"); //$NON-NLS-1$

        TextView twitter = (TextView) findViewById(R.id.share_twitter);
        setUpTextView(twitter, getString(R.string.share_with_twitter), "http://twitter.com/#!/weloveastrid"); //$NON-NLS-1$

        TextView google = (TextView) findViewById(R.id.share_google);
        setUpTextView(google, getString(R.string.share_with_google), "https://plus.google.com/116404018347675245869"); //$NON-NLS-1$

    }

    private void setUpTextView(TextView tv, String text, final String url) {
        SpannableString span = new SpannableString(text);
        span.setSpan(new UnderlineSpan(), 0, text.length(), 0);
        tv.setText(span);
        tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    }

}
