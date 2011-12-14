package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.widget.ListView;

import com.timsu.astrid.R;
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

        Fragment frag = (Fragment) getSupportFragmentManager()
                .findFragmentById(R.id.tasklist_fragment);
        if (frag != null)
        {
            mMultipleFragments = true;
            ((ListFragment) frag).getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
	}
}
