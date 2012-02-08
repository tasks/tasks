package com.todoroo.astrid.actfm;

import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.view.MenuItem;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.service.ThemeService;

public class TagUpdatesActivity extends AstridActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag_updates_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.header_title_view);
        ((TextView) actionBar.getCustomView().findViewById(R.id.title)).setText(R.string.TAd_contextEditTask);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
