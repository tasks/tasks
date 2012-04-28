package com.todoroo.astrid.calls;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.service.ThemeService;

public class MissedCallActivity extends Activity {

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$

    private final OnClickListener dismissListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            AndroidUtilities.callOverridePendingTransition(MissedCallActivity.this, 0, android.R.anim.fade_out);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.missed_call_activity);
        int color = ThemeService.getThemeColor();

        TextView returnCallButton = (TextView) findViewById(R.id.call_return);
        TextView addTaskButton = (TextView) findViewById(R.id.call_add);
        TextView ignoreButton = (TextView) findViewById(R.id.call_ignore);
        View dismissView = findViewById(R.id.dismiss);

        Resources r = getResources();
        returnCallButton.setBackgroundColor(r.getColor(color));
        addTaskButton.setBackgroundColor(r.getColor(color));

        ignoreButton.setOnClickListener(dismissListener);
        dismissView.setOnClickListener(dismissListener);

        System.err.println("Should display notification for number: " + getIntent().getStringExtra(EXTRA_NUMBER));
    }
}
