/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.CommentsFragment;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagsPlugin;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.QuickAddBar;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

import net.simonvt.menudrawer.MenuDrawer;

import org.tasks.R;

public class TaskListActivity extends AstridActivity implements OnPageChangeListener {

    MenuDrawer menuDrawer;

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

    private int filterMode;
    private FilterModeSpec filterModeSpec;

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);

        int contentView = getContentView();
        setContentView(contentView);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setLogo(null);

        if(AndroidUtilities.isTabletSized(this)) {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.STATIC);
            menuDrawer.setDropShadowEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        } else {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
            menuDrawer.setDrawerIndicatorEnabled(true);
        }
        menuDrawer.setContentView(contentView);
        // cannot use full screen until next menudrawer release
        // menuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.ic_drawer, typedValue, true);
        menuDrawer.setSlideDrawable(typedValue.resourceId);
        menuDrawer.setHardwareLayerEnabled(true);

        initializeFragments();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            extras = (Bundle) extras.clone();
        }

        if (extras == null) {
            extras = new Bundle();
        }

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

        setupTasklistFragmentWithFilter(savedFilter, extras);

        if (savedFilter != null) {
            setListsTitle(savedFilter.title);
        }

        if (getIntent().hasExtra(TOKEN_SOURCE)) {
            trackActivitySource();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_list_activity, menu);
        TaskListFragment tlf = getTaskListFragment();
        if(tlf instanceof TagViewFragment) {
            menu.findItem(R.id.menu_delete_list).setVisible(true);
            menu.findItem(R.id.menu_rename_list).setVisible(true);
        } else if(tlf instanceof GtasksListFragment) {
            menu.findItem(R.id.menu_sync).setTitle(R.string.actfm_TVA_menu_refresh);
            menu.findItem(R.id.menu_clear_completed).setVisible(true);
            menu.findItem(R.id.menu_sort).setVisible(false);
        }
        if(!Preferences.getBoolean(R.string.p_show_menu_search, true)) {
            menu.findItem(R.id.menu_search).setVisible(false);
        }
        if(!Preferences.getBoolean(R.string.p_show_menu_sort, true)) {
            menu.findItem(R.id.menu_sort).setVisible(false);
        }
        if(!Preferences.getBoolean(R.string.p_show_menu_sync, true)) {
            menu.findItem(R.id.menu_sync).setVisible(false);
        }
        return true;
    }

    protected int getContentView() {
        if (AstridPreferences.useTabletLayout(this)) {
            return R.layout.task_list_wrapper_activity_3pane;
        } else {
            return R.layout.task_list_wrapper_activity_no_swipe;
        }
    }

    protected Filter getDefaultFilter() {
        return filterModeSpec.getDefaultFilter(this);
    }

    @Override
    protected Bundle configureIntentAndExtrasWithFilter(Intent intent,
            Filter filter) {
        Bundle extras = super.configureIntentAndExtrasWithFilter(intent, filter);
        getIntent().putExtra(FILTER_MODE, filterMode);
        return extras;
    }

    protected void initializeFragments() {
        View editFragment = findViewById(R.id.taskedit_fragment_container);
        filterMode = getIntent().getIntExtra(FILTER_MODE, FILTER_MODE_NORMAL);
        updateFilterModeSpec(filterMode);

        if(editFragment != null) {
            if(editFragment.getVisibility() == View.INVISIBLE) {
                fragmentLayout = LAYOUT_TRIPLE;
            } else {
                fragmentLayout = LAYOUT_DOUBLE;
                if (AndroidUtilities.getSdkVersion() >= 11) {
                    setupLayoutTransitions();
                }
            }
        } else {
            fragmentLayout = LAYOUT_SINGLE;
        }

        setupPopoverWithFilterList((FilterListFragment) setupFragment(FilterListFragment.TAG_FILTERLIST_FRAGMENT, 0,
                filterModeSpec.getFilterListClass()));
    }

    private void setupLayoutTransitions() {
        LayoutTransition transition = new LayoutTransition();
        ViewGroup container = (ViewGroup) findViewById(R.id.right_column);
        container.setLayoutTransition(transition);
    }

    private void setupPopoverWithFragment(Fragment frag) {
        View view = frag.getView();
        if (view != null) {
            FrameLayout parent = (FrameLayout) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
            menuDrawer.setMenuView(view);
        }
    }

    public void setupPopoverWithFilterList(FilterListFragment fla) {
        setupPopoverWithFragment(fla);
    }

    @Override
    public boolean onFilterItemClicked(FilterListItem item) {
        TaskEditFragment.removeExtrasFromIntent(getIntent());
        TaskEditFragment tef = getTaskEditFragment();
        if (tef != null) {
            onBackPressed();
        }
        menuDrawer.closeMenu();

        return super.onFilterItemClicked(item);
    }

    @Override
    public void setupActivityFragment(TagData tagData) {
        super.setupActivityFragment(tagData);

        if (fragmentLayout == LAYOUT_TRIPLE) {
            View container = findViewById(R.id.taskedit_fragment_container);
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (!Flags.checkAndClear(Flags.TLA_DISMISSED_FROM_TASK_EDIT)) {
            TaskEditFragment tea = getTaskEditFragment();
            if (tea != null) {
                onBackPressed();
            }
        }

        if (getIntent().hasExtra(TOKEN_SWITCH_TO_FILTER)) {
            Filter newList = getIntent().getParcelableExtra(TOKEN_SWITCH_TO_FILTER);
            getIntent().removeExtra(TOKEN_SWITCH_TO_FILTER);
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
                    if (result != null) {
                        onTaskListItemClicked(result.getId());
                    }
                }
            }
            if (fragmentLayout == LAYOUT_SINGLE) {
                getIntent().removeExtra(OPEN_TASK);
            }
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
    public void onTaskListItemClicked(long taskId) {
        if (fragmentLayout != LAYOUT_SINGLE) {
            getIntent().putExtra(OPEN_TASK, taskId);
        }
        CommentsFragment tuf = getTagUpdatesFragment();
        if (tuf != null) {
            tuf.getView().setVisibility(View.INVISIBLE);
        }

        super.onTaskListItemClicked(taskId);
    }

    public void setListsTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    public void setSelectedItem(Filter item) {
        getSupportActionBar().setTitle(item.title);
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) { /* Nothing */ }

    @Override
    public void onPageScrollStateChanged(int state) { /* Nothing */ }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuDrawer.closeMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        // manage task edit visibility
        View taskeditFragmentContainer = findViewById(R.id.taskedit_fragment_container);
        if(taskeditFragmentContainer != null && taskeditFragmentContainer.getVisibility() == View.VISIBLE) {
            if(fragmentLayout == LAYOUT_DOUBLE) {
                findViewById(R.id.taskedit_fragment_container).setVisibility(View.GONE);
            }
            Flags.set(Flags.TLA_DISMISSED_FROM_TASK_EDIT);
            onPostResume();

            CommentsFragment tuf = getTagUpdatesFragment();
            if (tuf != null) {
                tuf.getView().setVisibility(View.VISIBLE);
            }
        }
        int drawerState = menuDrawer.getDrawerState();
        if(drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            menuDrawer.closeMenu();
            return;
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
            if(data == null) {
                return;
            }

            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, newList); // Handle in onPostResume()
                FilterListFragment fla = getFilterListFragment();
                if (fla != null) {
                    fla.clear();
                }
            }
        } else if (requestCode == TaskListFragment.ACTIVITY_EDIT_TASK && resultCode != Activity.RESULT_CANCELED) {
            // Handle switch to assigned filter when it comes from TaskEditActivity finishing
            // For cases when we're in a multi-frame layout, the TaskEditFragment will notify us here directly
            TaskListFragment tlf = getTaskListFragment();
            if (tlf != null) {
                if (data != null) {
                    if (data.getParcelableExtra(TaskEditFragment.TOKEN_NEW_REPEATING_TASK) != null) {
                        Task repeating = data.getParcelableExtra(TaskEditFragment.TOKEN_NEW_REPEATING_TASK);
                        DateChangedAlerts.showRepeatChangedDialog(this, repeating);
                    }
                    if (data.getBooleanExtra(TaskEditFragment.TOKEN_TAGS_CHANGED, false)) {
                        tagsChanged(true);
                    }
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
                    if (tagData != null) {
                        activeUuid = tagData.getUuid();
                    }

                    if (activeUuid.equals(uuid)) {
                        getIntent().putExtra(TOKEN_SWITCH_TO_FILTER, CoreFilterExposer.buildInboxFilter(getResources())); // Handle in onPostResume()
                        fl.clear(); // Should auto refresh
                    } else {
                        tlf.refresh();
                    }
                }

                if (fl != null) {
                    fl.refresh();
                }
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
                if (flf != null) {
                    flf.refresh();
                }
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
            if (onActivityResult) {
                flf.clear();
            } else {
                flf.refresh();
            }
        }
    }

    protected void refreshTaskList() {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf != null) {
            tlf.refresh();
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
        switch (getIntent().getIntExtra(TOKEN_SOURCE, Constants.SOURCE_DEFAULT)) {
        case Constants.SOURCE_NOTIFICATION:
            break;
        case Constants.SOURCE_OTHER:
            break;
        case Constants.SOURCE_PPWIDGET:
            break;
        case Constants.SOURCE_WIDGET:
            break;
        case Constants.SOURCE_C2DM:
            break;
        }
        getIntent().putExtra(TOKEN_SOURCE, Constants.SOURCE_DEFAULT); // Only report source once
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TaskListFragment tlf = getTaskListFragment();
        switch(item.getItemId()) {
            case android.R.id.home:
                if(menuDrawer.getDrawerState() != MenuDrawer.STATE_CLOSED) {
                    menuDrawer.closeMenu();
                } else {
                    menuDrawer.openMenu();
                }
                hideKeyboard();
                return true;
            case R.id.menu_settings:
                tlf.showSettings();
                return true;
            case R.id.menu_search:
                onSearchRequested();
                return true;
            case R.id.menu_sort:
                AlertDialog dialog = SortSelectionActivity.createDialog(
                        this, tlf.hasDraggableOption(), tlf, tlf.getSortFlags(), tlf.getSort());
                dialog.show();
                return true;
            case R.id.menu_sync:
                tlf.syncActionHelper.performSyncAction();
                return true;
            case R.id.menu_new_filter:
                Intent intent = new Intent(this, CustomFilterActivity.class);
                startActivityForResult(intent, TaskListFragment.ACTIVITY_REQUEST_NEW_FILTER);
                return true;
            case R.id.menu_new_list:
                startActivityForResult(TagsPlugin.newTagDialog(this), FilterListFragment.REQUEST_NEW_LIST);
                if (!AstridPreferences.useTabletLayout(this)) {
                    AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
                }
                return true;
            case R.id.menu_delete_list:
                TagService.Tag deleteTag = new TagService.Tag(tlf.getActiveTagData());
                Intent ret = new Intent(this, TagFilterExposer.DeleteTagActivity.class);
                ret.putExtra("tag", deleteTag.tag);
                ret.putExtra(TagViewFragment.EXTRA_TAG_UUID, deleteTag.uuid);
                startActivityForResult(ret, FilterListFragment.REQUEST_CUSTOM_INTENT);
                return true;
            case R.id.menu_rename_list:
                TagService.Tag renameTag = new TagService.Tag(tlf.getActiveTagData());
                Intent rename = new Intent(this, TagFilterExposer.RenameTagActivity.class);
                rename.putExtra("tag", renameTag.tag);
                rename.putExtra(TagViewFragment.EXTRA_TAG_UUID, renameTag.uuid);
                startActivityForResult(rename, FilterListFragment.REQUEST_CUSTOM_INTENT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateFilterModeSpec(int mode) {
        switch(mode) {
        case FILTER_MODE_NORMAL:
        default:
            filterModeSpec = new DefaultFilterMode();
        }
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            TaskEditFragment tef = getTaskEditFragment();
            if (tef != null && tef.onKeyDown(keyCode)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
