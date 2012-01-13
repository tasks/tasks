package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.service.ThemeService;

public class TaskListWrapperActivity extends AstridWrapperActivity {
    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    ThemeService.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_list_wrapper_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Filter savedFilter = getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER);
		setupTasklistFragmentWithFilter(savedFilter);
	}

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        Fragment frag = getFilterListFragment();
        if (frag != null) {
            mMultipleFragments = true;
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            mMultipleFragments = false;
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        Fragment frag = getTaskListFragment();
        if (frag != null)
            ((TextView)frag.getView().findViewById(R.id.listLabel)).setText(title);
        // update the actionbar-title
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
