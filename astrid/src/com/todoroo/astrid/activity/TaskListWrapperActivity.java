package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.utility.Flags;

public class TaskListWrapperActivity extends AstridWrapperActivity {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter";
    private View listsNav;
    private ImageView listsNavDisclosure;
    private TextView lists;

    private FragmentPopover listsPopover;
    private FragmentPopover editPopover;

    private final OnClickListener popupMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setListsDropdownSelected(true);
            listsPopover.show(v);
        }
    };

    private final OnDismissListener editPopoverDismissListener = new OnDismissListener() {
        @Override
        public void onDismiss() {
            TaskEditActivity tea = getTaskEditFragment();
            if (tea != null) {
                try {
                    if (!Flags.checkAndClear(Flags.TLA_DISMISSED_FROM_TASK_EDIT))
                        tea.save(false);
                } catch (IllegalStateException e) {
                    // Save during pause, ignore it
                }
            }
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
		listsNavDisclosure = (ImageView) actionBar.getCustomView().findViewById(R.id.list_disclosure_arrow);
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
		        createEditPopover();
		    }

		    setupFragment(FilterListActivity.TAG_FILTERLIST_FRAGMENT,
		            R.id.filterlist_fragment_container, FilterListActivity.class);
		} else {
		    fragmentLayout = LAYOUT_SINGLE;
		    actionBar.setDisplayHomeAsUpEnabled(true);
		    listsNav.setOnClickListener(popupMenuClickListener);
		    createListsPopover();
		    setupPopoverWithFilterList((FilterListActivity) setupFragment(FilterListActivity.TAG_FILTERLIST_FRAGMENT, 0, FilterListActivity.class));
		}
    }

    private void createListsPopover() {
	    listsPopover = new FragmentPopover(this, R.layout.list_dropdown_popover);
        listsPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                setListsDropdownSelected(false);
            }
        });
	}

    private void createEditPopover() {
        editPopover = new FragmentPopover(this, R.layout.taskedit_popover);
        editPopover.setOnDismissListener(editPopoverDismissListener);
        editPopover.setTouchInterceptor(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if ((event.getAction() == MotionEvent.ACTION_DOWN)
                        && ((x < 0) || (x >= editPopover.getContentView().getWidth()) || (y < 0) || (y >= editPopover.getContentView().getHeight()))) return true;
                return false;
            }
        });
    }

    private void setupPopoverWithFragment(FragmentPopover popover, Fragment frag, LayoutParams params) {
        if (popover != null) {
            View view = frag.getView();
            if (view != null) {
                FrameLayout parent = (FrameLayout) view.getParent();
                if (parent != null)
                    parent.removeView(view);
                if (params == null)
                    popover.setContent(view);
                else
                    popover.setContent(view, params);
            }
        }
    }

	public void setupPopoverWithFilterList(FilterListActivity fla) {
	    setupPopoverWithFragment(listsPopover, fla, null);
	}

	@Override
    public void onTaskListItemClicked(long taskId) {
	    super.onTaskListItemClicked(taskId);
	    if (fragmentLayout == LAYOUT_DOUBLE && getTaskEditFragment() != null) {
	        DisplayMetrics metrics = getResources().getDisplayMetrics();
	        setupPopoverWithFragment(editPopover, getTaskEditFragment(), new LayoutParams((int) (400 * metrics.density), (int) (600 * metrics.density)));
	        editPopover.show(listsNav);
	    }
	}


	@Override
	public boolean onFilterItemClicked(FilterListItem item) {
	    if (listsPopover != null)
	        listsPopover.dismiss();
	    return super.onFilterItemClicked(item);
	}

	private void setListsDropdownSelected(boolean selected) {
	    int oldTextColor = lists.getTextColors().getDefaultColor();
	    int textStyle = (selected ? R.style.TextAppearance_ActionBar_ListsHeader_Selected : R.style.TextAppearance_ActionBar_ListsHeader);
	    lists.setTextAppearance(this, textStyle);
	    listsNav.setBackgroundColor(selected ? oldTextColor : android.R.color.transparent);
	    TypedValue tv = new TypedValue();
	    getTheme().resolveAttribute(R.attr.asListsDisclosure, tv, false);
	    listsNavDisclosure.setImageResource(selected ? R.drawable.lists_disclosure_white : tv.data);
	}

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Filter savedFilter = getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER);
        if (!Flags.checkAndClear(Flags.TLA_RESUMED_FROM_VOICE_ADD))
            setupTasklistFragmentWithFilter(savedFilter);
        if (savedFilter != null)
            lists.setText(savedFilter.title);

        if (!Flags.checkAndClear(Flags.TLA_DISMISSED_FROM_TASK_EDIT)) {
            TaskEditActivity tea = getTaskEditFragment();
            if (tea != null)
                onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listsPopover != null)
            listsPopover.dismiss();
        if (editPopover != null)
            editPopover.dismiss();
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
            Flags.set(Flags.TLA_DISMISSED_FROM_TASK_EDIT);
            onPostResume();
        } else {
            if (editPopover != null && editPopover.isShowing()) {
                Flags.set(Flags.TLA_DISMISSED_FROM_TASK_EDIT);
                editPopover.dismiss();
            }
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
