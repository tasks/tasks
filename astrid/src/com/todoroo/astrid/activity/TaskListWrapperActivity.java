package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.ListDropdownPopover;

public class TaskListWrapperActivity extends AstridWrapperActivity {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter";
    private View listsNav;
    private TextView lists;

    private ListDropdownPopover popover;

    private final OnClickListener popupMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setListsDropdownSelected(true);
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

		listsNav = actionBar.getCustomView().findViewById(R.id.lists_nav);
		lists = (TextView) actionBar.getCustomView().findViewById(R.id.list_title);

		initializeFragments(actionBar);
	}

	/**
	 *
	 * @param actionBar
	 */
    protected void initializeFragments(ActionBar actionBar) {
        View filterFragment = findViewById(R.id.filterlist_fragment_container);
        View editFragment = findViewById(R.id.taskedit_fragment_container);

		if (filterFragment != null) {
		    actionBar.setDisplayHomeAsUpEnabled(false);
		    actionBar.getCustomView().findViewById(R.id.list_disclosure_arrow).setVisibility(View.GONE);
		    listsNav.setOnClickListener(null);

		    if(editFragment != null && editFragment.getVisibility() == View.INVISIBLE) {
		        fragmentLayout = LAYOUT_TRIPLE;
		    } else {
		        fragmentLayout = LAYOUT_DOUBLE;
		    }

		    setupFragment(FilterListActivity.TAG_FILTERLIST_FRAGMENT,
		            R.id.filterlist_fragment_container, FilterListActivity.class);
		} else {
		    fragmentLayout = LAYOUT_SINGLE;
		    actionBar.setDisplayHomeAsUpEnabled(true);
		    listsNav.setOnClickListener(popupMenuClickListener);
		    createPopover();
		    setupPopoverWithFilterList((FilterListActivity) setupFragment(FilterListActivity.TAG_FILTERLIST_FRAGMENT, 0, FilterListActivity.class));
		}
    }

    private void createPopover() {
	    popover = new ListDropdownPopover(TaskListWrapperActivity.this);
        popover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                setListsDropdownSelected(false);
            }
        });
	}

	public void setupPopoverWithFilterList(FilterListActivity fla) {
	    if (popover != null) {
	        View view = fla.getView();
	        if (view != null) {
	            FrameLayout parent = (FrameLayout) view.getParent();
	            if (parent != null)
	                parent.removeView(view);
	            popover.setContent(view);
	        }
	    }
	}

	@Override
	public boolean onFilterItemClicked(FilterListItem item) {
	    if (popover != null)
	        popover.dismiss();
	    return super.onFilterItemClicked(item);
	}

	private void setListsDropdownSelected(boolean selected) {
	    int oldTextColor = lists.getTextColors().getDefaultColor();
	    int textStyle = (selected ? R.style.TextAppearance_ActionBar_ListsHeader_Selected : R.style.TextAppearance_ActionBar_ListsHeader);
	    lists.setTextAppearance(this, textStyle);
	    listsNav.setBackgroundColor(selected ? oldTextColor : android.R.color.transparent);
	}

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Filter savedFilter = getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER);
        setupTasklistFragmentWithFilter(savedFilter);
        if (savedFilter != null)
            lists.setText(savedFilter.title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (popover != null)
            popover.dismiss();
    }

    public void setSelectedItem(Filter item) {
       lists.setText(item.title);
    }

    @Override
    public void onBackPressed() {
     // manage task edit visibility
        View taskeditFragmentContainer = findViewById(R.id.taskedit_fragment_container);
        if(taskeditFragmentContainer != null && taskeditFragmentContainer.getVisibility() == View.VISIBLE) {
            if(fragmentLayout == LAYOUT_DOUBLE) {
                findViewById(R.id.taskedit_fragment_container).setVisibility(View.GONE);
                findViewById(R.id.filterlist_fragment_container).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.taskedit_fragment_container).setVisibility(View.INVISIBLE);
            }
            onPostResume();
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == FilterListActivity.REQUEST_NEW_LIST || requestCode == FilterListActivity.REQUEST_NEW_FILTER) && resultCode == Activity.RESULT_OK) {
            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                getIntent().putExtra(TaskListActivity.TOKEN_FILTER, newList);
                FilterListActivity fla = getFilterListFragment();
                if (fla != null)
                    fla.refresh();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
