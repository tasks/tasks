/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TaskEditActivity extends AstridActivity {

    @Inject ActivityPreferences preferences;

    @Bind(R.id.toolbar) Toolbar toolbar;

    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyThemeAndStatusBarColor();

        setContentView(R.layout.task_edit_wrapper_activity);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(false);
            Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_arrow_back_24dp));
            DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
            supportActionBar.setHomeAsUpIndicator(drawable);
        }
	}

    @Override
    protected void onResume() {
        super.onResume();

        Fragment frag = getFragmentManager()
                .findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);
        if (frag == null) {
            fragmentLayout = LAYOUT_SINGLE;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        TaskEditFragment frag = (TaskEditFragment) getFragmentManager()
                .findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
        if (frag != null && frag.isInLayout()) {
            return frag.onKeyDown(keyCode);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
