/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagUpdatesFragment;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.adapter.TaskListFragmentPagerAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.people.PeopleFilterMode;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.service.abtesting.ABTestEventReportingService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.reusable.FeaturedListFilterMode;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.ui.MainMenuPopover;
import com.todoroo.astrid.ui.MainMenuPopover.MainMenuListener;
import com.todoroo.astrid.ui.TaskListFragmentPager;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

public class TaskListActivity extends AstridActivity implements MainMenuListener, OnPageChangeListener {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter"; //$NON-NLS-1$

    /** token for indicating source of TLA launch */
    public static final String TOKEN_SOURCE = "source"; //$NON-NLS-1$

    public static final String NEW_LIST = "newList"; //$NON-NLS-1$

    public static final String OPEN_TASK = "openTask"; //$NON-NLS-1$

    private static final String FILTER_MODE = "filterMode"; //$NON-NLS-1$

    public static final int FILTER_MODE_NORMAL = 0;
    public static final int FILTER_MODE_PEOPLE = 1;
    public static final int FILTER_MODE_FEATURED = 2;

    @Autowired private ABTestEventReportingService abTestEventReportingService;

    private View listsNav;
    private ImageView listsNavDisclosure;
    private TextView lists;
    private ImageView mainMenu;
    private AsyncImageView personImage;
    private Button commentsButton;
    private int filterMode;
    private FilterModeSpec filterModeSpec;

    private TaskListFragmentPager tlfPager;
    private TaskListFragmentPagerAdapter tlfPagerAdapter;

    private FragmentPopover listsPopover;
    private MainMenuPopover mainMenuPopover;
    private boolean commentsVisible = false;

    private boolean swipeEnabled = false;

    private final TagDeletedReceiver tagDeletedReceiver = new TagDeletedReceiver();

    private final OnClickListener mainMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mainMenu.setSelected(true);
            mainMenuPopover.show(v);
            hideKeyboard();
        }
    };

    private final OnClickListener popupMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setListsDropdownSelected(true);
            listsPopover.show(v);
            hideKeyboard();
        }
    };

    private final OnClickListener commentsButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (fragmentLayout == LAYOUT_DOUBLE) {
                View container = findViewById(R.id.taskedit_fragment_container);
                if (getTaskEditFragment() != null)
                    return;
                container.setVisibility(container.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                commentsVisible = container.getVisibility() == View.VISIBLE;
            } else {
                // In this case we should be in LAYOUT_SINGLE--delegate to the task list fragment
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null)
                    tlf.handleCommentsButtonClicked();
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
        DependencyInjectionService.getInstance().inject(this);

        int contentView = getContentView();
        if (contentView == R.layout.task_list_wrapper_activity)
            swipeEnabled = true;
        setContentView(contentView);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(getHeaderView());

        listsNav = actionBar.getCustomView().findViewById(R.id.lists_nav);
        listsNavDisclosure = (ImageView) actionBar.getCustomView().findViewById(R.id.list_disclosure_arrow);
        lists = (TextView) actionBar.getCustomView().findViewById(R.id.list_title);
        mainMenu = (ImageView) actionBar.getCustomView().findViewById(R.id.main_menu);
        personImage = (AsyncImageView) actionBar.getCustomView().findViewById(R.id.person_image);
        personImage.setDefaultImageResource(R.drawable.icn_default_person_image);
        commentsButton = (Button) actionBar.getCustomView().findViewById(R.id.comments);

        initializeFragments(actionBar);
        createMainMenuPopover();
        mainMenu.setOnClickListener(mainMenuClickListener);
        commentsButton.setOnClickListener(commentsButtonClickListener);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            extras = (Bundle) extras.clone();

        if (extras == null)
            extras = new Bundle();

        Filter savedFilter = getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY).trim();
            String title = getString(R.string.FLA_search_filter, query);
            savedFilter = new Filter(title, title,
                    new QueryTemplate().where(Functions.upper(Task.TITLE).like(
                            "%" + //$NON-NLS-1$
                                    query.toUpperCase() + "%")), //$NON-NLS-1$
                    null);
        }

        if (savedFilter == null) {
            savedFilter = getDefaultFilter();
            extras.putAll(configureIntentAndExtrasWithFilter(getIntent(), savedFilter));
        }

        extras.putParcelable(TaskListFragment.TOKEN_FILTER, savedFilter);

        if (swipeIsEnabled()) {
            FilterListFragment flf = getFilterListFragment();
            if (flf == null)
                throw new RuntimeException("Filterlist fragment was null, needs to exist to construct the fragment pager"); //$NON-NLS-1$
            FilterAdapter adapter = flf.adapter;
            tlfPager = (TaskListFragmentPager) findViewById(R.id.pager);
            tlfPagerAdapter = new TaskListFragmentPagerAdapter(getSupportFragmentManager(), adapter);
            tlfPager.setAdapter(tlfPagerAdapter);
            tlfPager.setOnPageChangeListener(this);
        }

        setupTasklistFragmentWithFilter(savedFilter, extras);

        if (savedFilter != null)
            setListsTitle(savedFilter.title);

        if (getIntent().hasExtra(TOKEN_SOURCE)) {
            trackActivitySource();
        }

        // Have to call this here because sometimes StartupService
        // isn't called (i.e. if the app was silently alive in the background)
        abTestEventReportingService.trackUserRetention();
    }

    protected int getHeaderView() {
        return R.layout.header_nav_views;
    }

    protected int getContentView() {
        if (AstridPreferences.useTabletLayout(this))
            return R.layout.task_list_wrapper_activity_3pane;
        else if (!Preferences.getBoolean(R.string.p_swipe_lists_enabled, false))
            return R.layout.task_list_wrapper_activity_no_swipe;
        else
            return R.layout.task_list_wrapper_activity;
    }

    protected Filter getDefaultFilter() {
        return filterModeSpec.getDefaultFilter(this);
    }

    private boolean swipeIsEnabled() {
        return fragmentLayout == LAYOUT_SINGLE && swipeEnabled;
    }

    @Override
    public TaskListFragment getTaskListFragment() {
        if (swipeIsEnabled()) {
            return tlfPager.getCurrentFragment();
        } else {
            return super.getTaskListFragment();
        }
    }

    @Override
    public void setupTasklistFragmentWithFilterAndCustomTaskList(Filter filter, Bundle extras, Class<?> customTaskList) {
        if (swipeIsEnabled()) {
            tlfPager.showFilterWithCustomTaskList(filter, customTaskList);
            tlfPager.forceReload(); // Hack to force reload of current page
        } else {
            super.setupTasklistFragmentWithFilterAndCustomTaskList(filter, extras, customTaskList);
        }
    }

    @Override
    protected Bundle configureIntentAndExtrasWithFilter(Intent intent,
            Filter filter) {
        Bundle extras = super.configureIntentAndExtrasWithFilter(intent, filter);
        getIntent().putExtra(FILTER_MODE, filterMode);
        return extras;
    }

    /**
     *
     * @param actionBar
     */
    protected void initializeFragments(ActionBar actionBar) {
        View filterFragment = findViewById(R.id.filterlist_fragment_container);
        View editFragment = findViewById(R.id.taskedit_fragment_container);
        filterMode = getIntent().getIntExtra(FILTER_MODE, FILTER_MODE_NORMAL);
        updateFilterModeSpec(filterMode);

        if (filterFragment != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.getCustomView().findViewById(R.id.list_disclosure_arrow).setVisibility(View.GONE);
            listsNav.setOnClickListener(null);

            if(editFragment != null && editFragment.getVisibility() == View.INVISIBLE) {
                fragmentLayout = LAYOUT_TRIPLE;
                actionBar.getCustomView().findViewById(R.id.comments).setVisibility(View.GONE);
            } else {
                fragmentLayout = LAYOUT_DOUBLE;
                if (AndroidUtilities.getSdkVersion() >= 11) {
                    setupLayoutTransitions();
                }
            }

            setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT,
                    R.id.filterlist_fragment_container, filterModeSpec.getFilterListClass(), false, false);
        } else {
            fragmentLayout = LAYOUT_SINGLE;
            actionBar.setDisplayHomeAsUpEnabled(true);
            listsNav.setOnClickListener(popupMenuClickListener);
            createListsPopover();
            setupPopoverWithFilterList((FilterListFragment) setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT, 0,
                    filterModeSpec.getFilterListClass(), true, false));
        }
    }

    private void setupLayoutTransitions() {
        LayoutTransition transition = new LayoutTransition();
        ViewGroup container = (ViewGroup) findViewById(R.id.right_column);
        container.setLayoutTransition(transition);
    }

    protected Class<? extends FilterListFragment> getFilterListClass() {
        return FilterListFragment.class;
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

    private void createMainMenuPopover() {
        int layout;
        boolean isTablet = AstridPreferences.useTabletLayout(this);
        if (isTablet)
            layout = R.layout.main_menu_popover_tablet;
        else
            layout = R.layout.main_menu_popover;

        mainMenuPopover = new MainMenuPopover(this, layout, (fragmentLayout != LAYOUT_SINGLE), this);
        mainMenuPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                mainMenu.setSelected(false);
            }
        });

        if (isTablet)
            mainMenuPopover.refreshFixedItems();
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
    public boolean onFilterItemClicked(FilterListItem item) {
        if (listsPopover != null)
            listsPopover.dismiss();
        setCommentsCount(0);

        if (swipeIsEnabled()) {
            TaskListFragmentPager.showSwipeBetweenHelper(this);
            tlfPager.showFilter((Filter) item);
            return true;
        }

        TaskEditFragment.removeExtrasFromIntent(getIntent());
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null)
            onBackPressed();

        boolean result = super.onFilterItemClicked(item);
        filterModeSpec.onFilterItemClickedCallback(item);
        return result;
    }

    @Override
    public void setupActivityFragment(TagData tagData) {
        super.setupActivityFragment(tagData);

        int visibility = (filterModeSpec.showComments() ? View.VISIBLE : View.GONE);

        if (fragmentLayout != LAYOUT_TRIPLE) {
            commentsButton.setVisibility(visibility);
        } else {
            View container = findViewById(R.id.taskedit_fragment_container);
            if (container != null)
                container.setVisibility(visibility);
        }
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

        if (getIntent().hasExtra(NEW_LIST)) {
            Filter newList = getIntent().getParcelableExtra(NEW_LIST);
            getIntent().removeExtra(NEW_LIST);
            onFilterItemClicked(newList);
        }

        if (getIntent().hasExtra(OPEN_TASK)) {
            long id = getIntent().getLongExtra(OPEN_TASK, 0);
            if (id > 0) {
                onTaskListItemClicked(id);
            } else {
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null) {
                    Task result = tlf.quickAddBar.quickAddTask("", true); //$NON-NLS-1$
                    if (result != null)
                        onTaskListItemClicked(result.getId());
                }
            }
            if (fragmentLayout == LAYOUT_SINGLE)
                getIntent().removeExtra(OPEN_TASK);
        }
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        if (fragmentLayout != LAYOUT_SINGLE)
            getIntent().putExtra(OPEN_TASK, taskId);
        TagUpdatesFragment tuf = getTagUpdatesFragment();
        if (tuf != null)
            tuf.getView().setVisibility(View.INVISIBLE);

        super.onTaskListItemClicked(taskId);
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
        AndroidUtilities.tryDismissPopup(this, listsPopover);
        AndroidUtilities.tryDismissPopup(this, mainMenuPopover);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AndroidUtilities.tryUnregisterReceiver(this, tagDeletedReceiver);
    }

    public void setSelectedItem(Filter item) {
        lists.setText(item.title);
    }

    public TaskListFragmentPagerAdapter getFragmentPagerAdapter() {
        return tlfPagerAdapter;
    }

    @Override
    public void onPageSelected(int position) {
        if (tlfPagerAdapter != null) {
            configureIntentAndExtrasWithFilter(getIntent(), tlfPagerAdapter.getFilter(position));
            setListsTitle(tlfPagerAdapter.getPageTitle(position).toString());

            TaskListFragment fragment = getTaskListFragment();
            if (fragment != null) {
                fragment.initiateAutomaticSync();
                fragment.requestCommentCountUpdate();
            }
            if (position != 0)
                Preferences.setBoolean(TaskListFragmentPager.PREF_SHOWED_SWIPE_HELPER, true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) { /* Nothing */ }

    @Override
    public void onPageScrollStateChanged(int state) { /* Nothing */ }

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
                if (!commentsVisible)
                    findViewById(R.id.taskedit_fragment_container).setVisibility(View.GONE);
            }
            Flags.set(Flags.TLA_DISMISSED_FROM_TASK_EDIT);
            onPostResume();

            TagUpdatesFragment tuf = getTagUpdatesFragment();
            if (tuf != null)
                tuf.getView().setVisibility(View.VISIBLE);
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
                getIntent().putExtra(NEW_LIST, newList); // Handle in onPostResume()
                FilterListFragment fla = getFilterListFragment();
                if (fla != null && getFragmentLayout() != LAYOUT_SINGLE)
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

    public void refreshFilterCount(Filter filter) {
        FilterListFragment flf = getFilterListFragment();
        if (flf != null) {
            flf.adapter.refreshFilterCount(filter);
        }
    }

    /**
     * Report who launched this activity
     */
    protected void trackActivitySource() {
        switch (getIntent().getIntExtra(TOKEN_SOURCE,
                Constants.SOURCE_DEFAULT)) {
        case Constants.SOURCE_NOTIFICATION:
            StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_NOTIFICATION);
            break;
        case Constants.SOURCE_OTHER:
            StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_OTHER);
            break;
        case Constants.SOURCE_PPWIDGET:
            StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_PPW);
            break;
        case Constants.SOURCE_WIDGET:
            StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_WIDGET);
            break;
        case Constants.SOURCE_C2DM:
            StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_C2DM);
            break;
        case Constants.SOURCE_REENGAGEMENT:
            StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_REENGAGEMENT);
        }
        getIntent().putExtra(TOKEN_SOURCE, Constants.SOURCE_DEFAULT); // Only report source once
    }

    @Override
    public boolean shouldAddMenuItem(int itemId) {
        return AndroidUtilities.indexOf(filterModeSpec.getForbiddenMenuItems(), itemId) < 0;
    }

    @Override
    public void mainMenuItemSelected(int item, Intent customIntent) {
        TaskListFragment tlf = getTaskListFragment();
        switch (item) {
        case MainMenuPopover.MAIN_MENU_ITEM_LISTS:
            if (filterMode == FILTER_MODE_NORMAL)
                listsNav.performClick();
            else
                setFilterMode(FILTER_MODE_NORMAL);
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_SEARCH:
            onSearchRequested();
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_FEATURED_LISTS:
            setFilterMode(FILTER_MODE_FEATURED);
            return;
        case MainMenuPopover.MAIN_MENU_ITEM_FRIENDS:
            setFilterMode(FILTER_MODE_PEOPLE);
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

    public void setFilterMode(int mode) {
        filterMode = mode;
        updateFilterModeSpec(mode);
        getIntent().putExtra(FILTER_MODE, mode);

        refreshMainMenu();
        if (fragmentLayout == LAYOUT_SINGLE) {
            createListsPopover();
            setupPopoverWithFilterList((FilterListFragment) setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT, 0,
                    filterModeSpec.getFilterListClass(), true, true));
            if (mode == FILTER_MODE_PEOPLE) {
                personImage.setVisibility(View.VISIBLE);
                commentsButton.setVisibility(View.GONE);
                ((PeopleFilterMode) filterModeSpec).setImageView(personImage);
            } else {
                personImage.setVisibility(View.GONE);
                commentsButton.setVisibility(View.VISIBLE);
            }
        } else {
            setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT, R.id.filterlist_fragment_container,
                    filterModeSpec.getFilterListClass(), false, true);
        }

        onFilterItemClicked(getDefaultFilter());
        if (fragmentLayout == LAYOUT_SINGLE)
            listsNav.performClick();
    }

    public void refreshMainMenu() {
        mainMenuPopover.refreshFixedItems();
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(filterModeSpec.getMainMenuIconAttr(), tv, false);
        mainMenu.setImageResource(tv.data);
    }

    private void updateFilterModeSpec(int mode) {
        switch(mode) {
        case FILTER_MODE_PEOPLE:
            filterModeSpec = new PeopleFilterMode();
            break;
        case FILTER_MODE_FEATURED:
            filterModeSpec = new FeaturedListFilterMode();
            break;
        case FILTER_MODE_NORMAL:
        default:
            filterModeSpec = new DefaultFilterMode();
        }
    }

    public MainMenuPopover getMainMenuPopover() {
        return mainMenuPopover;
    }

    private void hideKeyboard() {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf == null)
            return;
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tlf.quickAddBar.getQuickAddBox().getWindowToken(), 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mainMenuPopover.suppressNextKeyEvent();
            mainMenu.performClick();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            TaskEditFragment tef = getTaskEditFragment();
            if (tef != null && tef.onKeyDown(keyCode))
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class TagDeletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deletedTag = intent.getStringExtra(TagViewFragment.EXTRA_TAG_NAME);
            String deletedTagSql = intent.getStringExtra(TagService.TOKEN_TAG_SQL);
            FilterListFragment fl = getFilterListFragment();
            if (deletedTagSql.equals(TagService.SHOW_ACTIVE_TASKS)) {
                fl.switchToActiveTasks();
                fl.clear(); // Should auto refresh
            }
            else if (fl != null) {
                Filter currentlyShowing = getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER);
                if (currentlyShowing != null) {
                    boolean titlesMatch = currentlyShowing.title != null && currentlyShowing.title.equals(deletedTag);
                    boolean sqlMatches = currentlyShowing.getSqlQuery() != null && currentlyShowing.getSqlQuery().equals(deletedTagSql);
                    if (titlesMatch && sqlMatches)
                        fl.switchToActiveTasks();
                }
                fl.clear(); // Should auto refresh
            }
        }

    }
}
