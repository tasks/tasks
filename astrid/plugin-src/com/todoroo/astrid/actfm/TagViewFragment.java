package com.todoroo.astrid.actfm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.welcome.HelpInfoPopover;

public class TagViewFragment extends TaskListFragment {

    private static final String LAST_FETCH_KEY = "tag-fetch-"; //$NON-NLS-1$

    public static final String BROADCAST_TAG_ACTIVITY = AstridApiConstants.PACKAGE + ".TAG_ACTIVITY"; //$NON-NLS-1$

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$
    public static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$

    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SUPPORT_ID + 1;

    private static final int REQUEST_CODE_SETTINGS = 0;

    public static final String TOKEN_START_ACTIVITY = "startActivity"; //$NON-NLS-1$

    protected TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired SyncV2Service syncService;

    protected View taskListView;

    private boolean dataLoaded = false;

    private long currentId;

    private Filter originalFilter;

    //private ImageAdapter galleryAdapter;

    // --- UI initialization

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnKeyListener(null);

        // allow for text field entry, needed for android bug #2516
        OnTouchListener onTouch = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        };

        ((EditText) getView().findViewById(R.id.quickAddText)).setOnTouchListener(onTouch);

        View membersEdit = getView().findViewById(R.id.members_edit);
        membersEdit.setOnClickListener(settingsListener);

        originalFilter = filter;
    }

    private final OnClickListener settingsListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), TagSettingsActivity.class);
            intent.putExtra(EXTRA_TAG_DATA, tagData);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
            if (!AndroidUtilities.isTabletSized(getActivity())) {
                AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
            }
        }
    };

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.TaskListActivity#getListBody(android.view.ViewGroup)
     */
    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.task_list_body_tag, root, false);

        taskListView = super.getListBody(parent);
        parent.addView(taskListView);

        return parent;
    }

    private void showListSettingsPopover() {
        if (!AstridPreferences.canShowPopover())
            return;
        if (!Preferences.getBoolean(R.string.p_showed_list_settings_help, false)) {
            Preferences.setBoolean(R.string.p_showed_list_settings_help, true);
            View tabView = getView().findViewById(R.id.members_edit);
            HelpInfoPopover.showPopover(getActivity(), tabView,
                    R.string.help_popover_list_settings, null);
        }
    }

    @Override
    protected void addSyncRefreshMenuItem(Menu menu, int themeFlags) {
        if(actFmPreferenceService.isLoggedIn()) {
            addMenuItem(menu, R.string.actfm_TVA_menu_refresh,
                    ThemeService.getDrawable(R.drawable.icn_menu_refresh, themeFlags), MENU_REFRESH_ID, true);
        } else {
            super.addSyncRefreshMenuItem(menu, themeFlags);
        }
    }

    // --- data loading

    @Override
    protected void onNewIntent(Intent intent) {
        synchronized(this) {
            if(dataLoaded)
                return;
            dataLoaded = true;
        }

        TaskListActivity activity = (TaskListActivity) getActivity();
        String tag = extras.getString(EXTRA_TAG_NAME);
        long remoteId = extras.getLong(EXTRA_TAG_REMOTE_ID, 0);

        if(tag == null && remoteId == 0)
            return;

        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(Criterion.or(TagData.NAME.eqCaseInsensitive(tag),
                Criterion.and(TagData.REMOTE_ID.gt(0), TagData.REMOTE_ID.eq(remoteId)))));
        try {
            tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, tag);
                tagData.setValue(TagData.REMOTE_ID, remoteId);
                tagDataService.save(tagData);
            } else {
                cursor.moveToFirst();
                tagData.readFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        postLoadTagData();
        setUpMembersGallery();

        super.onNewIntent(intent);

        if (extras.getBoolean(TOKEN_START_ACTIVITY, false)) {
            extras.remove(TOKEN_START_ACTIVITY);
            activity.showComments();
        }
    }

    protected void postLoadTagData() {
        // stub
    }

    @Override
    public TagData getActiveTagData() {
        return tagData;
    }

    @Override
    public void loadTaskListContent(boolean requery) {
        super.loadTaskListContent(requery);
        if(taskAdapter == null || taskAdapter.getCursor() == null)
            return;

        int count = taskAdapter.getCursor().getCount();

        if(tagData != null && sortFlags <= SortHelper.FLAG_REVERSE_SORT &&
                count != tagData.getValue(TagData.TASK_COUNT)) {
            tagData.setValue(TagData.TASK_COUNT, count);
            tagDataService.save(tagData);
        }

        updateCommentCount();
    }

    @Override
    public void requestCommentCountUpdate() {
        updateCommentCount();
    }

    private void updateCommentCount() {
        if (tagData != null) {
            long lastViewedComments = Preferences.getLong(TagUpdatesFragment.UPDATES_LAST_VIEWED + tagData.getValue(TagData.REMOTE_ID), 0);
            int unreadCount = 0;
            TodorooCursor<Update> commentCursor = tagDataService.getUpdatesWithExtraCriteria(tagData, Update.CREATION_DATE.gt(lastViewedComments));
            try {
                unreadCount = commentCursor.getCount();
            } finally {
                commentCursor.close();
            }

            TaskListActivity tla = (TaskListActivity) getActivity();
            tla.setCommentsCount(unreadCount);
        }
    }

    // --------------------------------------------------------- refresh data


    @Override
    public void initiateAutomaticSync() {
        if (!isCurrentTaskListFragment())
            return;
        if (tagData != null) {
            long lastAutoSync = Preferences.getLong(LAST_FETCH_KEY + tagData.getId(), 0);
            if(DateUtilities.now() - lastAutoSync > DateUtilities.ONE_HOUR)
                refreshData(false);
        }
    }

    /** refresh the list with latest data from the web */
    private void refreshData(final boolean manual) {
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.DLG_loading);

        syncService.synchronizeList(tagData, manual, new ProgressBarSyncResultCallback(getActivity(), this,
                R.id.progressBar, new Runnable() {
            @Override
            public void run() {
                ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
            }
        }));
        Preferences.setLong(LAST_FETCH_KEY + tagData.getId(), DateUtilities.now());
    }

    private void setUpMembersGallery() {
        LinearLayout membersView = (LinearLayout)getView().findViewById(R.id.shared_with);
        membersView.setOnClickListener(settingsListener);
        try {
            String membersString = tagData.getValue(TagData.MEMBERS);
            JSONArray members = new JSONArray(membersString);
            if (members.length() > 0) {
                membersView.setOnClickListener(null);
                membersView.removeAllViews();
                for (int i = 0; i < members.length(); i++) {
                    JSONObject member = members.getJSONObject(i);
                    addImageForMember(membersView, member);
                }
                // Handle creator
                JSONObject owner;
                if(tagData.getValue(TagData.USER_ID) != 0) {
                     owner = new JSONObject(tagData.getValue(TagData.USER));
                } else {
                    owner = ActFmPreferenceService.thisUser();
                }
                addImageForMember(membersView, owner);

                JSONObject unassigned = new JSONObject();
                unassigned.put("id", Task.USER_ID_UNASSIGNED); //$NON-NLS-1$
                unassigned.put("name", getActivity().getString(R.string.actfm_EPA_unassigned)); //$NON-NLS-1$
                addImageForMember(membersView, unassigned);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getView().findViewById(R.id.filter_assigned).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAssignedFilter();
            }
        });
    }

    @SuppressWarnings("nls")
    private void addImageForMember(LinearLayout membersView, JSONObject member) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        AsyncImageView image = new AsyncImageView(getActivity());
        image.setLayoutParams(new LinearLayout.LayoutParams((int)(50 * displayMetrics.density),
                (int)(50 * displayMetrics.density)));

        image.setDefaultImageResource(R.drawable.icn_default_person_image);
        if (member.optLong("id", Task.USER_ID_SELF) == Task.USER_ID_UNASSIGNED)
            image.setDefaultImageResource(R.drawable.icn_anyone);

        image.setScaleType(ImageView.ScaleType.FIT_XY);
        try {
            final long id = member.optLong("id", Task.USER_ID_EMAIL);
            if (id == ActFmPreferenceService.userId())
                member = ActFmPreferenceService.thisUser();
            final JSONObject memberToUse = member;

            final String memberName = displayName(memberToUse);
            if (memberToUse.has("picture")) {
                image.setUrl(memberToUse.getString("picture"));
            }
            image.setOnClickListener(listenerForImage(memberToUse, id, memberName));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        membersView.addView(image);
    }

    private OnClickListener listenerForImage(final JSONObject member, final long id, final String displayName) {
        return new OnClickListener() {
            final String email = member.optString("email"); //$NON-NLS-1$
            @Override
            public void onClick(View v) {
                if (currentId == id) {
                    // Back to all
                    resetAssignedFilter();
                } else {
                    // New filter
                    currentId = id;
                    Criterion assignedCriterion;
                    if (currentId == ActFmPreferenceService.userId())
                        assignedCriterion = Criterion.or(Task.USER_ID.eq(0), Task.USER_ID.eq(id));
                    else if (currentId == Task.USER_ID_EMAIL && !TextUtils.isEmpty(email))
                        assignedCriterion = Task.USER.like("%" + email + "%"); //$NON-NLS-1$ //$NON-NLS-2$
                    else
                        assignedCriterion = Task.USER_ID.eq(id);
                    Criterion assigned = Criterion.and(TaskCriteria.activeAndVisible(), assignedCriterion);
                    filter = TagFilterExposer.filterFromTag(getActivity(), new Tag(tagData), assigned);
                    TextView filterByAssigned = (TextView) getView().findViewById(R.id.filter_assigned);
                    filterByAssigned.setVisibility(View.VISIBLE);
                    if (id == Task.USER_ID_UNASSIGNED)
                        filterByAssigned.setText(getString(R.string.actfm_TVA_filter_by_unassigned));
                    else
                        filterByAssigned.setText(getString(R.string.actfm_TVA_filtered_by_assign, displayName));
                    setUpTaskList();
                }
            }
        };
    }

    @Override
    protected Intent getOnClickQuickAddIntent(Task t) {
        Intent intent = super.getOnClickQuickAddIntent(t);
        // Customize extras
        return intent;
    }

    private void resetAssignedFilter() {
        currentId = Task.USER_ID_IGNORE;
        filter = originalFilter;
        getView().findViewById(R.id.filter_assigned).setVisibility(View.GONE);
        setUpTaskList();
    }

    @SuppressWarnings("nls")
    private String displayName(JSONObject user) {
        String name = user.optString("name");
        if (!TextUtils.isEmpty(name) && !"null".equals(name)) {
            name = name.trim();
            int index = name.indexOf(' ');
            if (index > 0) {
                return name.substring(0, index);
            } else {
                return name;
            }
        } else {
            String email = user.optString("email");
            email = email.trim();
            int index = email.indexOf('@');
            if (index > 0) {
                return email.substring(0, index);
            } else {
                return email;
            }
        }
    }

    // --- receivers

    private final BroadcastReceiver notifyReceiver = new BroadcastReceiver() {
        @SuppressWarnings("nls")
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!intent.hasExtra("tag_id"))
                return;
            if(tagData == null || !Long.toString(tagData.getValue(TagData.REMOTE_ID)).equals(intent.getStringExtra("tag_id")))
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //refreshUpdatesList();
                }
            });
            refreshData(false);

            NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
            nm.cancel(tagData.getValue(TagData.REMOTE_ID).intValue());
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BROADCAST_TAG_ACTIVITY);
        getActivity().registerReceiver(notifyReceiver, intentFilter);

        showListSettingsPopover();
        updateCommentCount();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(notifyReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == Activity.RESULT_OK) {
            tagData = tagDataService.fetchById(tagData.getId(), TagData.PROPERTIES); // refetch
            if (tagData == null) // This can happen if a tag has been deleted as part of a sync
                return;
            filter = TagFilterExposer.filterFromTagData(getActivity(), tagData);
            getActivity().getIntent().putExtra(TOKEN_FILTER, filter);
            extras.putParcelable(TOKEN_FILTER, filter);
            Activity activity = getActivity();
            if (activity instanceof TaskListActivity) {
                ((TaskListActivity) activity).setListsTitle(filter.title);
                FilterListFragment flf = ((TaskListActivity) activity).getFilterListFragment();
                if (flf != null)
                    flf.clear();
            }
            taskAdapter = null;
            refresh();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean handleOptionsMenuItemSelected(int id, Intent intent) {
        // handle my own menus
        switch (id) {
        case MENU_REFRESH_ID:
            refreshData(true);
            return true;
        }

        return super.handleOptionsMenuItemSelected(id, intent);
    }

    @Override
    protected boolean hasDraggableOption() {
        return true;
    }

    @Override
    protected void toggleDragDrop(boolean newState) {
        Filter newFilter = TagFilterExposer.filterFromTagData(getActivity(), tagData);

        if(newState)
            ((FilterWithCustomIntent)newFilter).customTaskList =
                new ComponentName(getActivity(), SubtasksTagListFragment.class);
        else
            ((FilterWithCustomIntent)newFilter).customTaskList =
                new ComponentName(getActivity(), TagViewFragment.class);

        ((AstridActivity)getActivity()).onFilterItemClicked(newFilter);
    }

    @Override
    protected void refresh() {
        setUpMembersGallery();
        loadTaskListContent(true);
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
    }

}
