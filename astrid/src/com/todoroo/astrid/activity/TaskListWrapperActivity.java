package com.todoroo.astrid.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.ListDropdownPopover;

public class TaskListWrapperActivity extends AstridWrapperActivity {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter";
    private int currSelection;
    private View listsNav;
    private TextView lists;

    private ArrayAdapter<Filter> listDropdownAdapter;
    private ListDropdownPopover popover;

    private final OnItemClickListener listClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Filter item = listDropdownAdapter.getItem(position);
            currSelection = position;
            onFilterItemClicked(item);
            popover.dismiss();
            lists.setText(item.title);
        }
    };

    private final OnClickListener popupMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            listsNav.setBackgroundColor(Color.RED);
            popover.show(v);
        }
    };
    /**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    ThemeService.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_list_wrapper_activity);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(R.layout.header_nav_views);

        popover = new ListDropdownPopover(TaskListWrapperActivity.this, R.layout.list_dropdown_popover);
        popover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                listsNav.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        listsNav = actionBar.getCustomView().findViewById(R.id.lists_nav);
		lists = (TextView) actionBar.getCustomView().findViewById(R.id.list_title);
		currSelection = 0;

		View container = findViewById(R.id.filterlist_fragment_container);
		if (container != null) {
		    mMultipleFragments = true;
		    actionBar.setDisplayHomeAsUpEnabled(false);
		    listsNav.setOnClickListener(null);
		} else {
		    mMultipleFragments = false;
		    actionBar.setDisplayHomeAsUpEnabled(true);
		    listsNav.setOnClickListener(popupMenuClickListener);
		}

		Filter savedFilter = getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER);
		setupTasklistFragmentWithFilter(savedFilter);
		setupFilterlistFragment();

		if (savedInstanceState != null) {
		    currSelection = savedInstanceState.getInt(TOKEN_SELECTED_FILTER);
		}

	}

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    public void updateDropdownNav(ArrayAdapter<Filter> arrayAdapter) {
        listDropdownAdapter = arrayAdapter;
        popover.setAdapter(listDropdownAdapter, listClickListener);
        if (currSelection < listDropdownAdapter.getCount()) {
            lists.setText(listDropdownAdapter.getItem(currSelection).title);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        popover.dismiss();
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        icicle.putInt(TOKEN_SELECTED_FILTER, currSelection);
        super.onSaveInstanceState(icicle);
    }

    public void setSelectedItem(Filter item) {
       currSelection = listDropdownAdapter.getPosition(item);
       lists.setText(item.title);
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
