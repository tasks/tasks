package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.service.ThemeService;

public class FilterListWrapperActivity extends AstridWrapperActivity {

    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
		super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_list_wrapper_activity);
        if (findViewById(R.id.tasklist_fragment_container) != null)
            setupTasklistFragmentWithFilter((Filter)getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER));
        else {
            Fragment tla = getTaskListFragment();
            if (tla != null) {
                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.remove(tla);
                transaction.commit();
            }
        }
	}

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        Fragment frag = getTaskListFragment();
        if (frag != null) {
            mMultipleFragments = true;
        } else {
            mMultipleFragments = false;
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
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
    }
}
