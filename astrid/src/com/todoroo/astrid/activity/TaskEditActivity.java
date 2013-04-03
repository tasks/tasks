/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.service.ThemeService;

public class TaskEditActivity extends AstridActivity {
    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        ActionBar actionBar = getSupportActionBar();
        if (Preferences.getBoolean(R.string.p_save_and_cancel, false)) {
            if (ThemeService.getTheme() == R.style.Theme_White_Alt)
                actionBar.setLogo(R.drawable.ic_menu_save_blue_alt);
            else
                actionBar.setLogo(R.drawable.ic_menu_save);
        } else {
            actionBar.setLogo(null);
        }

        super.onCreate(savedInstanceState);
		setContentView(R.layout.task_edit_wrapper_activity);

		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);

		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(R.layout.header_title_view);
		((TextView) actionBar.getCustomView().findViewById(R.id.title)).setText(R.string.TAd_contextEditTask);

	}

	public void updateTitle(boolean isNewTask) {
	    ActionBar actionBar = getSupportActionBar();
	    if (actionBar != null) {
	        TextView title = ((TextView) actionBar.getCustomView().findViewById(R.id.title));
	        if (ActFmPreferenceService.isPremiumUser())
	            title.setText(""); //$NON-NLS-1$
	        else
	            title.setText(isNewTask ? R.string.TEA_new_task : R.string.TAd_contextEditTask);
	    }
	}

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        Fragment frag = (Fragment) getSupportFragmentManager()
                .findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);
        if (frag != null) {
            fragmentLayout = LAYOUT_DOUBLE;
        } else {
            fragmentLayout = LAYOUT_SINGLE;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        TaskEditFragment frag = (TaskEditFragment) getSupportFragmentManager()
                .findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
        if (frag != null && frag.isInLayout())
            return frag.onKeyDown(keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
