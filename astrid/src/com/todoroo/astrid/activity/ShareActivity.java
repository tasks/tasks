package com.todoroo.astrid.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.timsu.astrid.R;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;

public class ShareActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.header_title_view);
        ((TextView) actionBar.getCustomView().findViewById(R.id.title)).setText(R.string.EPr_share_astrid);

        setContentView(R.layout.share_activity);
        TextView fb = (TextView) findViewById(R.id.share_facebook);
        setUpTextView(fb, getString(R.string.share_with_facebook), "http://facebook.com/weloveastrid", "facebook"); //$NON-NLS-1$ //$NON-NLS-2$

        TextView twitter = (TextView) findViewById(R.id.share_twitter);
        setUpTextView(twitter, getString(R.string.share_with_twitter), "http://twitter.com/astrid", "twitter"); //$NON-NLS-1$ //$NON-NLS-2$

        TextView google = (TextView) findViewById(R.id.share_google);
        setUpTextView(google, getString(R.string.share_with_google), "https://plus.google.com/116404018347675245869", "google"); //$NON-NLS-1$ //$NON-NLS-2$

        setupText();
        StatisticsService.reportEvent(StatisticsConstants.SHARE_PAGE_VIEWED);
    }

    private void setUpTextView(TextView tv, String text, final String url, final String buttonId) {
        tv.setText(text);
        ((View) tv.getParent()).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsService.reportEvent(StatisticsConstants.SHARE_BUTTON_CLICKED, "button", buttonId); //$NON-NLS-1$
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    private void setupText() {
        View speechBubbleBackground = findViewById(R.id.speech_bubble_container);
        speechBubbleBackground.setBackgroundColor(0);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        ImageView icon = (ImageView) findViewById(R.id.astridIcon);

        int dim = (int) (80 * metrics.density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dim, dim));
        icon.setScaleType(ScaleType.FIT_CENTER);

        TextView speechBubble = (TextView) findViewById(R.id.reminder_message);

        speechBubble.setText(R.string.share_speech_bubble);
        speechBubble.setTextSize(17);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
