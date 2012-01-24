package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.service.ThemeService;

public class TaskEditWrapperActivity extends AstridWrapperActivity {
    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_edit_wrapper_activity);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);

		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(R.layout.header_title_view);
		((TextView) actionBar.getCustomView().findViewById(R.id.title)).setText(R.string.TAd_contextEditTask);
	}

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        Fragment frag = (Fragment) getSupportFragmentManager()
                .findFragmentByTag(TaskListActivity.TAG_TASKLIST_FRAGMENT);
        if (frag != null) {
            fragmentLayout = LAYOUT_DOUBLE;
        } else {
            fragmentLayout = LAYOUT_SINGLE;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        TaskEditActivity frag = (TaskEditActivity) getSupportFragmentManager()
                .findFragmentByTag(TaskEditActivity.TAG_TASKEDIT_FRAGMENT);
        if (frag != null && frag.isInLayout())
            return frag.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
