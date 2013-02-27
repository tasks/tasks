/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.service.ThemeService;

public class CommentsActivity extends AstridActivity {
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

        Fragment fragment;
        String tag;
        if (getIntent().getExtras().containsKey(TaskCommentsFragment.EXTRA_TASK)) {
            fragment = new TaskCommentsFragment();
            tag = "taskupdates_fragment"; //$NON-NLS-1$
        } else {
            fragment = new TagCommentsFragment();
            tag = "tagupdates_fragment"; //$NON-NLS-1$
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.comments_fragment_container, fragment, tag);
        transaction.commit();
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
