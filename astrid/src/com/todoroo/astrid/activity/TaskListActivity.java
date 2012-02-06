package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagUpdatesFragment;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.reminders.NotificationFragment;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.ui.MainMenuPopover;
import com.todoroo.astrid.ui.MainMenuPopover.MainMenuListener;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.welcome.tutorial.WelcomeWalkthrough;

public class TaskListActivity extends AstridActivity implements MainMenuListener {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter"; //$NON-NLS-1$
    private View listsNav;
    private ImageView listsNavDisclosure;
    private TextView lists;
    private ImageView mainMenu;
    private ImageView commentsButton;

    private FragmentPopover listsPopover;
    private FragmentPopover editPopover;
    private FragmentPopover commentsPopover;
    private MainMenuPopover mainMenuPopover;

    private final TagDeletedReceiver tagDeletedReceiver = new TagDeletedReceiver();

    private final OnClickListener mainMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mainMenu.setSelected(true);
            mainMenuPopover.show(v);
        }
    };

    private final OnClickListener popupMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setListsDropdownSelected(true);
            listsPopover.show(v);
        }
    };

    private final OnClickListener commentsButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (fragmentLayout == LAYOUT_DOUBLE) {
                TagUpdatesFragment frag = getTagUpdatesFragment();
                if (frag != null) {
                    setupPopoverWithFragment(commentsPopover, frag, null);
                    commentsPopover.show(listsNav);
                }
            } else {
                // In this case we should be in LAYOUT_SINGLE--delegate to the task list fragment
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null)
                    tlf.handleCommentsButtonClicked();
            }
        }
    };

    private final OnDismissListener editPopoverDismissListener = new OnDismissListener() {
        @Override
        public void onDismiss() {
            TaskEditFragment tea = getTaskEditFragment();
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
		mainMenu = (ImageView) actionBar.getCustomView().findViewById(R.id.main_menu);
		commentsButton = (ImageView) actionBar.getCustomView().findViewById(R.id.comments);

		initializeFragments(actionBar);
		createMainMenuPopover();
		mainMenu.setOnClickListener(mainMenuClickListener);
		commentsButton.setOnClickListener(commentsButtonClickListener);
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
		        actionBar.getCustomView().findViewById(R.id.comments).setVisibility(View.GONE);
		    } else {
		        fragmentLayout = LAYOUT_DOUBLE;
		        createEditPopover();
		        createCommentsPopover();
		    }

		    setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT,
		            R.id.filterlist_fragment_container, FilterListFragment.class);
		} else {
		    fragmentLayout = LAYOUT_SINGLE;
		    actionBar.setDisplayHomeAsUpEnabled(true);
		    listsNav.setOnClickListener(popupMenuClickListener);
		    createListsPopover();
		    setupPopoverWithFilterList((FilterListFragment) setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT, 0, FilterListFragment.class));
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

    private void createCommentsPopover() {
        commentsPopover = new FragmentPopover(this, R.layout.taskedit_popover);
        commentsPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                TagUpdatesFragment frag = getTagUpdatesFragment();
                FrameLayout parent = (FrameLayout) frag.getView().getParent();
                parent.removeView(frag.getView());
                ((FrameLayout) findViewById(R.id.taskedit_fragment_container)).addView(frag.getView());
            }
        });
    }

    private void createMainMenuPopover() {
        mainMenuPopover = new MainMenuPopover(this, R.layout.main_menu_popover);
        mainMenuPopover.setMenuListener(this);
        mainMenuPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                mainMenu.setSelected(false);
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

	public void setupPopoverWithFilterList(FilterListFragment fla) {
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

	    TypedValue listDisclosure = new TypedValue();
	    getTheme().resolveAttribute(R.attr.asListsDisclosure, listDisclosure, false);
	    lists.setTextAppearance(this, textStyle);
	    listsNav.setBackgroundColor(selected ? oldTextColor : android.R.color.transparent);
	    listsNavDisclosure.setSelected(selected);
	}

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Filter savedFilter = getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER);
        if (getIntent().getIntExtra(TaskListFragment.TOKEN_SOURCE, Constants.SOURCE_DEFAULT) == Constants.SOURCE_NOTIFICATION)
            setupTasklistFragmentWithFilterAndCustomTaskList(savedFilter, NotificationFragment.class);
        else if (!Flags.checkAndClear(Flags.TLA_RESUMED_FROM_VOICE_ADD))
            setupTasklistFragmentWithFilter(savedFilter);

        if (savedFilter != null)
            setListsTitle(savedFilter.title);

        if (!Flags.checkAndClear(Flags.TLA_DISMISSED_FROM_TASK_EDIT)) {
            TaskEditFragment tea = getTaskEditFragment();
            if (tea != null)
                onBackPressed();
        }
    }

    public void setListsTitle(String title) {
        lists.setText(title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(tagDeletedReceiver, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listsPopover != null)
            listsPopover.dismiss();
        if (editPopover != null)
            editPopover.dismiss();
        if (mainMenuPopover != null)
            mainMenuPopover.dismiss();
        if (commentsPopover != null)
            commentsPopover.dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(tagDeletedReceiver);
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
        if ((requestCode == FilterListFragment.REQUEST_NEW_LIST || requestCode == FilterListFragment.REQUEST_NEW_FILTER) && resultCode == Activity.RESULT_OK) {
            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                onFilterItemClicked(newList); // Switch to the new list
                FilterListFragment fla = getFilterListFragment();
                if (fla != null)
                    fla.clear();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void mainMenuItemSelected(int item) {
        TaskListFragment tla = getTaskListFragment();
        switch (item) {
        case MainMenuPopover.MAIN_MENU_ITEM_TASKS:
            // Do nothing
            break;
        case MainMenuPopover.MAIN_MENU_ITEM_FRIENDS:
            // Doesn't exist yet
            break;
        case MainMenuPopover.MAIN_MENU_ITEM_SUGGESTIONS:
            // Doesn't exist yet
            break;
        case MainMenuPopover.MAIN_MENU_ITEM_TUTORIAL:
            Intent showWelcomeLogin = new Intent(this, WelcomeWalkthrough.class);
            showWelcomeLogin.putExtra(ActFmLoginActivity.SHOW_TOAST, false);
            showWelcomeLogin.putExtra(WelcomeWalkthrough.TOKEN_MANUAL_SHOW, true);
            startActivity(showWelcomeLogin);
            break;
        case MainMenuPopover.MAIN_MENU_ITEM_SETTINGS:
            if (tla != null)
                tla.showSettings();
            break;
        case MainMenuPopover.MAIN_MENU_ITEM_SUPPORT:
            if (tla != null)
                tla.showSupport();
            break;
        }
    }

    private class TagDeletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deletedTag = intent.getStringExtra(TagViewFragment.EXTRA_TAG_NAME);
            String deletedTagSql = intent.getStringExtra(TagFilterExposer.TAG_SQL);
            FilterListFragment fl = getFilterListFragment();
            if (fl != null) {
                Filter currentlyShowing = getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER);
                if (currentlyShowing != null) {
                    boolean titlesMatch = currentlyShowing.title != null && currentlyShowing.title.equals(deletedTag);
                    boolean sqlMatches = currentlyShowing.sqlQuery != null && currentlyShowing.sqlQuery.equals(deletedTagSql);
                    if (titlesMatch && sqlMatches)
                        fl.switchToActiveTasks();
                }
                fl.clear(); // Should auto refresh
            }
        }

    }
}
