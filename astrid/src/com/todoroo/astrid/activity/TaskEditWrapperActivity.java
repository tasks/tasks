package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

import com.timsu.astrid.R;
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
	}

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        Fragment frag = (Fragment) getSupportFragmentManager()
                .findFragmentById(R.id.tasklist_fragment);
        if (frag != null) {
            mMultipleFragments = true;
        } else {
            mMultipleFragments = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        TaskEditActivity frag = (TaskEditActivity) getSupportFragmentManager()
                .findFragmentById(R.id.taskedit_fragment);
        if (frag != null && frag.isInLayout())
            return frag.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }
}
