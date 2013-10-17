/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

import com.actionbarsherlock.app.ActionBar;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.service.ThemeService;

import org.tasks.R;

public class TaskEditActivity extends AstridActivity {
    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        ActionBar actionBar = getSupportActionBar();
        if(ThemeService.getTheme() == R.style.Theme) {
            actionBar.setLogo(R.drawable.ic_action_save_light);
        } else {
            actionBar.setLogo(R.drawable.ic_action_save);
        }

        super.onCreate(savedInstanceState);
		setContentView(R.layout.task_edit_wrapper_activity);

		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
	}

	public void updateTitle(boolean isNewTask) {
	    ActionBar actionBar = getSupportActionBar();
	    if (actionBar != null) {
            actionBar.setTitle(isNewTask ? R.string.TEA_new_task : R.string.TAd_contextEditTask);
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
