package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagUpdatesFragment;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.NotificationFragment;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.ui.MainMenuPopover;
import com.todoroo.astrid.ui.MainMenuPopover.MainMenuListener;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

public class TaskListActivity extends AstridActivity implements MainMenuListener {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter"; //$NON-NLS-1$

    private View listsNav;
    private ImageView listsNavDisclosure;
    private TextView lists;
    private ImageView mainMenu;
    private Button commentsButton;

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
                    frag.setLastViewed();
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

        if (AndroidUtilities.isTabletSized(this))
            setContentView(R.layout.task_list_wrapper_activity_3pane);
        else
            setContentView(R.layout.task_list_wrapper_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.header_nav_views);

        listsNav = actionBar.getCustomView().findViewById(R.id.lists_nav);
        listsNavDisclosure = (ImageView) actionBar.getCustomView().findViewById(R.id.list_disclosure_arrow);
        lists = (TextView) actionBar.getCustomView().findViewById(R.id.list_title);
        mainMenu = (ImageView) actionBar.getCustomView().findViewById(R.id.main_menu);
        commentsButton = (Button) actionBar.getCustomView().findViewById(R.id.comments);

        initializeFragments(actionBar);
        createMainMenuPopover();
        mainMenu.setOnClickListener(mainMenuClickListener);
        commentsButton.setOnClickListener(commentsButtonClickListener);

        Filter savedFilter = getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER);

        if (getIntent().getIntExtra(TaskListFragment.TOKEN_SOURCE, Constants.SOURCE_DEFAULT) ==
                Constants.SOURCE_NOTIFICATION)
            setupTasklistFragmentWithFilterAndCustomTaskList(savedFilter, NotificationFragment.class);
        else
            setupTasklistFragmentWithFilter(savedFilter);

        if (savedFilter != null)
            setListsTitle(savedFilter.title);
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
        int layout;
        if (AndroidUtilities.isTabletSized(this))
            layout = R.layout.main_menu_popover_tablet;
        else
            layout = R.layout.main_menu_popover;

        mainMenuPopover = new MainMenuPopover(this, layout, (fragmentLayout != LAYOUT_SINGLE));
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
        setCommentsCount(0);
        return super.onFilterItemClicked(item);
    }

    private void setListsDropdownSelected(boolean selected) {
        int oldTextColor = lists.getTextColors().getDefaultColor();
        int textStyle = (selected ? R.style.TextAppearance_ActionBar_ListsHeader_Selected :
            R.style.TextAppearance_ActionBar_ListsHeader);

        TypedValue listDisclosure = new TypedValue();
        getTheme().resolveAttribute(R.attr.asListsDisclosure, listDisclosure, false);
        lists.setTextAppearance(this, textStyle);
        listsNav.setBackgroundColor(selected ? oldTextColor : android.R.color.transparent);
        listsNavDisclosure.setSelected(selected);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

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
        try {
            unregisterReceiver(tagDeletedReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver might not be registered if for example activity is stopped before on resume (?)
        }
    }

    public void setSelectedItem(Filter item) {
        lists.setText(item.title);
    }

    public void setCommentsCount(int count) {
        TypedValue tv = new TypedValue();

        if (count > 0) {
            commentsButton.setText(Integer.toString(count));
            getTheme().resolveAttribute(R.attr.asFilledCommentButtonImg, tv, false);
        } else {
            commentsButton.setText(""); //$NON-NLS-1$
            getTheme().resolveAttribute(R.attr.asCommentButtonImg, tv, false);
        }
        commentsButton.setBackgroundResource(tv.data);
    }

    public void showComments() {
        commentsButton.performClick();
    }

    @Override
    public void onBackPressed() {
        // manage task edit visibility
        View taskeditFragmentContainer = findViewById(R.id.taskedit_fragment_container);
        if(taskeditFragmentContainer != null && taskeditFragmentContainer.getVisibility() == View.VISIBLE) {
            if(fragmentLayout == LAYOUT_DOUBLE) {
                findViewById(R.id.taskedit_fragment_container).setVisibility(View.GONE);
                findViewById(R.id.filterlist_fragment_container).setVisibility(View.VISIBLE);
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
        if ((requestCode == FilterListFragment.REQUEST_NEW_LIST ||
                requestCode == TaskListFragment.ACTIVITY_REQUEST_NEW_FILTER) &&
                resultCode == Activity.RESULT_OK) {
            if(data == null)
                return;

            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                onFilterItemClicked(newList); // Switch to the new list
                FilterListFragment fla = getFilterListFragment();
                if (fla != null)
                    fla.clear();
            }
        } else if (requestCode == TaskListFragment.ACTIVITY_EDIT_TASK && resultCode != Activity.RESULT_CANCELED) {
            // Handle switch to assigned filter when it comes from TaskEditActivity finishing
            // For cases when we're in a multi-frame layout, the TaskEditFragment will notify us here directly
            TaskListFragment tlf = getTaskListFragment();
            if (tlf != null) {
                if (data != null) {
                    if (data.getBooleanExtra(TaskEditFragment.TOKEN_TASK_WAS_ASSIGNED, false)) {
                        String assignedTo = data.getStringExtra(TaskEditFragment.TOKEN_ASSIGNED_TO);
                        switchToAssignedFilter(assignedTo);
                    } else if (data.getParcelableExtra(TaskEditFragment.TOKEN_NEW_REPEATING_TASK) != null) {
                        Task repeating = data.getParcelableExtra(TaskEditFragment.TOKEN_NEW_REPEATING_TASK);
                        DateChangedAlerts.showRepeatChangedDialog(this, repeating);
                    }
                    if (data.getBooleanExtra(TaskEditFragment.TOKEN_TAGS_CHANGED, false))
                        tagsChanged(true);
                }
                tlf.refresh();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void tagsChanged() {
        tagsChanged(false);
    }

    private void tagsChanged(boolean onActivityResult) {
        FilterListFragment flf = getFilterListFragment();
        if (flf != null) {
            if (onActivityResult)
                flf.clear();
            else
                flf.refresh();
        }
    }

    protected void refreshTaskList() {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf != null)
            tlf.refresh();
    }

    public void switchToAssignedFilter(final String assignedEmail) {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf != null && tlf.isInbox()) {
            DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Filter assignedFilter = CustomFilterExposer.getAssignedByMeFilter(getResources());
                    onFilterItemClicked(assignedFilter);
                }
            };
            DialogUtilities.okCancelCustomDialog(this,
                    getString(R.string.actfm_view_task_title),
                    getString(R.string.actfm_view_task_text, assignedEmail),
                    R.string.actfm_view_task_ok, R.string.actfm_view_task_cancel,
                    0, okListener, null);
        }
    }

    public void incrementFilterCount(Filter filter) {
        FilterListFragment flf = getFilterListFragment();
        if (flf != null) {
            flf.adapter.incrementFilterCount(filter);
        }
    }

    public void decrementFilterCount(Filter filter) {
        FilterListFragment flf = getFilterListFragment();
        if (flf != null) {
            flf.adapter.decrementFilterCount(filter);
        }
    }

    @Override
    public void mainMenuItemSelected(int item, Intent customIntent) {
        TaskListFragment tlf = getTaskListFragment();
        switch (item) {
        case MainMenuPopover.MAIN_MENU_ITEM_LISTS:
            listsNav.performClick();
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_SEARCH:
            onSearchRequested();
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_FRIENDS:
            // Doesn't exist yet
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_SUGGESTIONS:
            // Doesn't exist yet
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_SETTINGS:
            if (tlf != null)
                tlf.showSettings();
            return;
        }
        tlf.handleOptionsMenuItemSelected(item, customIntent);
    }

    public MainMenuPopover getMainMenuPopover() {
        return mainMenuPopover;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mainMenuPopover.suppressNextKeyEvent();
            mainMenu.performClick();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class TagDeletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deletedTag = intent.getStringExtra(TagViewFragment.EXTRA_TAG_NAME);
            String deletedTagSql = intent.getStringExtra(TagFilterExposer.TAG_SQL);
            FilterListFragment fl = getFilterListFragment();
            if (deletedTagSql.equals(TagFilterExposer.SHOW_ACTIVE_TASKS)) {
                fl.switchToActiveTasks();
                fl.clear(); // Should auto refresh
            }
            else if (fl != null) {
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
