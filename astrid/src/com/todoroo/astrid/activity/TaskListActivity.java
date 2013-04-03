/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
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

import com.actionbarsherlock.app.ActionBar;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.CommentsFragment;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.actfm.sync.SyncUpgradePrompt;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.adapter.TaskListFragmentPagerAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.people.PeopleFilterMode;
import com.todoroo.astrid.people.PersonViewFragment;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.service.abtesting.ABTestEventReportingService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagsPlugin;
import com.todoroo.astrid.tags.reusable.FeaturedListFilterMode;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.ui.MainMenuPopover;
import com.todoroo.astrid.ui.MainMenuPopover.MainMenuListener;
import com.todoroo.astrid.ui.QuickAddBar;
import com.todoroo.astrid.ui.TaskListFragmentPager;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.welcome.tutorial.WelcomeWalkthrough;

public class TaskListActivity extends AstridActivity implements MainMenuListener, OnPageChangeListener {

    public static final String TOKEN_SELECTED_FILTER = "selectedFilter"; //$NON-NLS-1$

    /** token for indicating source of TLA launch */
    public static final String TOKEN_SOURCE = "source"; //$NON-NLS-1$

    public static final String TOKEN_SWITCH_TO_FILTER = "newListCreated"; //$NON-NLS-1$

    /** For indicating the new list screen should be launched at fragment setup time */
    public static final String TOKEN_CREATE_NEW_LIST = "createNewList"; //$NON-NLS-1$
    public static final String TOKEN_CREATE_NEW_LIST_MEMBERS = "newListMembers"; //$NON-NLS-1$
    public static final String TOKEN_CREATE_NEW_LIST_NAME = "newListName"; //$NON-NLS-1$

    public static final String OPEN_TASK = "openTask"; //$NON-NLS-1$

    private static final String FILTER_MODE = "filterMode"; //$NON-NLS-1$

    public static final int FILTER_MODE_NORMAL = 0;
    public static final int FILTER_MODE_PEOPLE = 1;
    public static final int FILTER_MODE_FEATURED = 2;

    public static final int REQUEST_CODE_RESTART = 10;

    @Autowired private ABTestEventReportingService abTestEventReportingService;

    @Autowired private TagMetadataDao tagMetadataDao;

    private View listsNav;
    private ImageView listsNavDisclosure;
    private TextView lists;
    private ImageView mainMenu;
    private TextView personStatus;
    private Button commentsButton;
    private int filterMode;
    private FilterModeSpec filterModeSpec;

    private TaskListFragmentPager tlfPager;
    private TaskListFragmentPagerAdapter tlfPagerAdapter;

    private FragmentPopover listsPopover;
    private MainMenuPopover mainMenuPopover;
    private boolean commentsVisible = false;

    private boolean swipeEnabled = false;

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

    private final OnClickListener friendStatusClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TaskListFragment tlf = getTaskListFragment();
            if (tlf == null || !(tlf instanceof PersonViewFragment))
                return;
            ((PersonViewFragment) tlf).handleStatusButtonClicked();
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
        personStatus = (TextView) actionBar.getCustomView().findViewById(R.id.person_image);
        commentsButton = (Button) actionBar.getCustomView().findViewById(R.id.comments);
        if (ThemeService.getTheme() == R.style.Theme_White_Alt)
            commentsButton.setTextColor(getResources().getColor(R.color.blue_theme_color));

        initializeFragments(actionBar);
        createMainMenuPopover();
        mainMenu.setOnClickListener(mainMenuClickListener);
        commentsButton.setOnClickListener(commentsButtonClickListener);
        personStatus.setOnClickListener(friendStatusClickListener);

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
                    new QueryTemplate().where(Task.TITLE.like(
                            "%" + //$NON-NLS-1$
                                    query + "%")), //$NON-NLS-1$
                    null);
        }

        if (savedFilter == null) {
            savedFilter = getDefaultFilter();
            extras.putAll(configureIntentAndExtrasWithFilter(getIntent(), savedFilter));
        }

        extras.putParcelable(TaskListFragment.TOKEN_FILTER, savedFilter);

        if (swipeIsEnabled()) {
            setupPagerAdapter();
        }

        setupTasklistFragmentWithFilter(savedFilter, extras);

        if (savedFilter != null)
            setListsTitle(savedFilter.title);

        if (getIntent().hasExtra(TOKEN_SOURCE)) {
            trackActivitySource();
        }

        // Have to call this here because sometimes StartupService
        // isn't called (i.e. if the app was silently alive in the background)
        abTestEventReportingService.trackUserRetention(this);
    }

    private void setupPagerAdapter() {
        FilterListFragment flf = getFilterListFragment();
        if (flf == null)
            throw new RuntimeException("Filterlist fragment was null, needs to exist to construct the fragment pager"); //$NON-NLS-1$
        FilterAdapter adapter = flf.adapter;
        tlfPager = (TaskListFragmentPager) findViewById(R.id.pager);
        tlfPagerAdapter = new TaskListFragmentPagerAdapter(getSupportFragmentManager(), adapter);
        tlfPager.setAdapter(tlfPagerAdapter);
        tlfPager.setOnPageChangeListener(this);
    }

    private int getHeaderView() {
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
            tlfPager.showFilter(filter);
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
        boolean isTabletLayout = AstridPreferences.useTabletLayout(this);
        if (isTabletLayout)
            layout = R.layout.main_menu_popover_tablet;
        else if (AndroidUtilities.isTabletSized(this))
            layout = R.layout.main_menu_popover_tablet_phone_layout;
        else
            layout = R.layout.main_menu_popover;

        mainMenuPopover = new MainMenuPopover(this, layout, (fragmentLayout != LAYOUT_SINGLE), this);
        mainMenuPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                mainMenu.setSelected(false);
            }
        });

        if (isTabletLayout)
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

    public void setCommentsButtonVisibility(boolean visible) {
        commentsButton.setVisibility(visible && filterModeSpec.showComments() && fragmentLayout != LAYOUT_TRIPLE ? View.VISIBLE : View.GONE);
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

        if (getIntent().hasExtra(TOKEN_SWITCH_TO_FILTER)) {
            Filter newList = getIntent().getParcelableExtra(TOKEN_SWITCH_TO_FILTER);
            getIntent().removeExtra(TOKEN_SWITCH_TO_FILTER);
            onFilterItemClicked(newList);
        }

        if (getIntent().hasExtra(OPEN_TASK)) {
            long id = getIntent().getLongExtra(OPEN_TASK, 0);
            if (id > 0) {
                onTaskListItemClicked(id, true);
            } else {
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null) {
                    Task result = tlf.quickAddBar.quickAddTask("", true); //$NON-NLS-1$
                    if (result != null)
                        onTaskListItemClicked(result.getId(), true);
                }
            }
            if (fragmentLayout == LAYOUT_SINGLE)
                getIntent().removeExtra(OPEN_TASK);
        }

        if (getIntent().getBooleanExtra(TOKEN_CREATE_NEW_LIST, false)) {
            newListFromLaunch();
        }
    }

    private void newListFromLaunch() {
        Intent thisIntent = getIntent();
        Intent newTagIntent = TagsPlugin.newTagDialog(this);
        newTagIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_MEMBERS, thisIntent.getStringExtra(TOKEN_CREATE_NEW_LIST_MEMBERS));
        newTagIntent.putExtra(TagSettingsActivity.TOKEN_AUTOPOPULATE_NAME, thisIntent.getStringExtra(TOKEN_CREATE_NEW_LIST_NAME));
        thisIntent.removeExtra(TOKEN_CREATE_NEW_LIST_MEMBERS);
        thisIntent.removeExtra(TOKEN_CREATE_NEW_LIST_NAME);
        startActivityForResult(newTagIntent, FilterListFragment.REQUEST_NEW_LIST);
    }

    @Override
    public void onTaskListItemClicked(long taskId, boolean editable) {
        if (fragmentLayout != LAYOUT_SINGLE && editable)
            getIntent().putExtra(OPEN_TASK, taskId);
        CommentsFragment tuf = getTagUpdatesFragment();
        if (tuf != null)
            tuf.getView().setVisibility(View.INVISIBLE);

        super.onTaskListItemClicked(taskId, editable);
    }

    public void setListsTitle(String title) {
        lists.setText(title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Preferences.getBoolean(WelcomeWalkthrough.KEY_SHOWED_WELCOME_LOGIN, false))
            SyncUpgradePrompt.showSyncUpgradePrompt(this);
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

            CommentsFragment tuf = getTagUpdatesFragment();
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
                getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, newList); // Handle in onPostResume()
                FilterListFragment fla = getFilterListFragment();
                if (fla != null && !swipeIsEnabled())
                    fla.clear();
            }
        } else if (requestCode == TaskListFragment.ACTIVITY_EDIT_TASK && resultCode != Activity.RESULT_CANCELED) {
            // Handle switch to assigned filter when it comes from TaskEditActivity finishing
            // For cases when we're in a multi-frame layout, the TaskEditFragment will notify us here directly
            TaskListFragment tlf = getTaskListFragment();
            if (tlf != null) {
                if (data != null) {
                    if (data.getBooleanExtra(TaskEditFragment.TOKEN_TASK_WAS_ASSIGNED, false)) {
                        String assignedTo = data.getStringExtra(TaskEditFragment.TOKEN_ASSIGNED_TO_DISPLAY);
                        String assignedEmail = data.getStringExtra(TaskEditFragment.TOKEN_ASSIGNED_TO_EMAIL);
                        String assignedId = data.getStringExtra(TaskEditFragment.TOKEN_ASSIGNED_TO_ID);
                        taskAssignedTo(assignedTo, assignedEmail, assignedId);
                    } else if (data.getParcelableExtra(TaskEditFragment.TOKEN_NEW_REPEATING_TASK) != null) {
                        Task repeating = data.getParcelableExtra(TaskEditFragment.TOKEN_NEW_REPEATING_TASK);
                        DateChangedAlerts.showRepeatChangedDialog(this, repeating);
                    }
                    if (data.getBooleanExtra(TaskEditFragment.TOKEN_TAGS_CHANGED, false))
                        tagsChanged(true);
                }
                tlf.refresh();
            }
        } else if (requestCode == FilterListFragment.REQUEST_CUSTOM_INTENT && resultCode == RESULT_OK && data != null) {
            // Tag renamed or deleted
            String action = data.getAction();
            String uuid = data.getStringExtra(TagViewFragment.EXTRA_TAG_UUID);

            if (AstridApiConstants.BROADCAST_EVENT_TAG_DELETED.equals(action)) {
                TaskListFragment tlf = getTaskListFragment();
                FilterListFragment fl = getFilterListFragment();
                if (tlf != null) {
                    TagData tagData = tlf.getActiveTagData();
                    String activeUuid = RemoteModel.NO_UUID;
                    if (tagData != null)
                        activeUuid = tagData.getUuid();

                    if (activeUuid.equals(uuid)) {
                        getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, CoreFilterExposer.buildInboxFilter(getResources())); // Handle in onPostResume()
                        fl.clear(); // Should auto refresh
                    } else {
                        tlf.refresh();
                    }
                }

                if (fl != null)
                    fl.refresh();
            } else if (AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED.equals(action)) {
                TaskListFragment tlf = getTaskListFragment();
                if (tlf != null) {
                    TagData td = tlf.getActiveTagData();
                    if (td != null && td.getUuid().equals(uuid)) {
                        td = PluginServices.getTagDataDao().fetch(uuid, TagData.PROPERTIES);
                        if (td != null) {
                            Filter filter = TagFilterExposer.filterFromTagData(this, td);
                            getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, filter);
                        }
                    } else {
                        tlf.refresh();
                    }
                }

                FilterListFragment flf = getFilterListFragment();
                if (flf != null)
                    flf.refresh();
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

    public void taskAssignedTo(final String assignedDisplay, String assignedEmail, final String assignedId) {
        final TaskListFragment tlf = getTaskListFragment();
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
                    getString(R.string.actfm_view_task_text, assignedDisplay),
                    R.string.actfm_view_task_ok, R.string.actfm_view_task_cancel,
                    0, okListener, null);
        } else if (tlf != null && (!TextUtils.isEmpty(assignedEmail) || Task.isRealUserId(assignedId))) {
            checkAddTagMember(tlf, assignedDisplay, assignedEmail, assignedId);
        }
    }

    private void checkAddTagMember(final TaskListFragment tlf, final String assignedDisplay, String assignedEmail, final String assignedId) {
        final TagData td = tlf.getActiveTagData();
        if (td != null) {
            String members = td.getValue(TagData.MEMBERS);

            boolean memberFound = false;
            if (TextUtils.isEmpty(members))
                memberFound = td.getValue(TagData.USER_ID).equals(assignedId) || tagMetadataDao.memberOfTagData(assignedEmail, td.getUuid(), assignedId);
            else {
                JSONObject user = new JSONObject();
                JSONArray membersArray = null;
                try {
                    if (!TextUtils.isEmpty(assignedEmail))
                        user.put("email", assignedEmail); //$NON-NLS-1$
                    if (Task.isRealUserId(assignedId))
                        user.put("id", assignedId); //$NON-NLS-1$
                    membersArray = new JSONArray(members);

                    for (int i = 0; i < membersArray.length(); i++) {
                        JSONObject member = membersArray.getJSONObject(i);
                        String memberId = Long.toString(member.optLong("id", -3)); //$NON-NLS-1$
                        if (Task.isRealUserId(memberId) && memberId.equals(assignedId)) {
                            memberFound = true;
                            break;
                        }
                    }

                    if (!memberFound) {
                        String ownerString = td.getValue(TagData.USER);
                        if (!TextUtils.isEmpty(ownerString)) {
                            JSONObject owner = new JSONObject(ownerString);
                            String ownerId = Long.toString(owner.optLong("id", -3)); //$NON-NLS-1$
                            if (Task.isRealUserId(ownerId) && assignedId.equals(ownerId))
                                memberFound = true;
                        }
                    }
                } catch (JSONException e) {
                    return;
                }
            }

            if (memberFound)
                return;

            DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    tagMetadataDao.createMemberLink(td.getId(), td.getUuid(), assignedId, false);
                    tlf.refresh();
                }
            };
            DialogUtilities.okCancelCustomDialog(this,
                    getString(R.string.actfm_EPA_add_person_to_list_title),
                    getString(R.string.actfm_EPA_add_person_to_list, assignedDisplay, assignedDisplay),
                    R.string.actfm_EPA_add_person_to_list_ok,
                    R.string.actfm_EPA_add_person_to_list_cancel,
                    android.R.drawable.ic_dialog_alert,
                    okListener, null);
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
                personStatus.setVisibility(View.VISIBLE);
                commentsButton.setVisibility(View.GONE);
            } else {
                personStatus.setVisibility(View.GONE);
                commentsButton.setVisibility(View.VISIBLE);
            }

            if (swipeIsEnabled()) {
                setupPagerAdapter();
            }

        } else {
            setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT, R.id.filterlist_fragment_container,
                    filterModeSpec.getFilterListClass(), false, true);
        }

        onFilterItemClicked(getDefaultFilter());
        if (swipeIsEnabled())
            setListsTitle(tlfPagerAdapter.getPageTitle(0).toString());
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
        QuickAddBar qab = tlf.quickAddBar;
        if (qab != null)
            imm.hideSoftInputFromWindow(qab.getQuickAddBox().getWindowToken(), 0);
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
}
