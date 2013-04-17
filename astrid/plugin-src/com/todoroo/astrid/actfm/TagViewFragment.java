/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.SyncMessageCallback;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.dao.TagMetadataDao.TagMetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagMemberMetadata;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.utility.ResourceDrawableCache;
import com.todoroo.astrid.welcome.HelpInfoPopover;

public class TagViewFragment extends TaskListFragment {

    public static final String BROADCAST_TAG_ACTIVITY = AstridApiConstants.API_PACKAGE + ".TAG_ACTIVITY"; //$NON-NLS-1$

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$

    @Deprecated
    private static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$

    public static final String EXTRA_TAG_UUID = "uuid"; //$NON-NLS-1$

    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SUPPORT_ID + 1;
    protected static final int MENU_LIST_SETTINGS_ID = R.string.tag_settings_title;

    private static final int REQUEST_CODE_SETTINGS = 0;

    public static final String TOKEN_START_ACTIVITY = "startActivity"; //$NON-NLS-1$

    protected TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired TagService tagService;

    @Autowired TagDataDao tagDataDao;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired SyncV2Service syncService;

    @Autowired UserDao userDao;

    @Autowired MetadataDao metadataDao;

    @Autowired TagMetadataDao tagMetadataDao;

    @Autowired TaskListMetadataDao taskListMetadataDao;

    protected View taskListView;

    private boolean dataLoaded = false;

    private String currentId = Task.USER_ID_IGNORE;

    protected AtomicBoolean isBeingFiltered = new AtomicBoolean(false);

    private Filter originalFilter;

    protected boolean justDeleted = false;

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
        if (membersEdit != null)
            membersEdit.setOnClickListener(settingsListener);

        originalFilter = filter;
    }

    private final OnClickListener settingsListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Activity activity = getActivity();
            Class<?> settingsClass = AstridPreferences.useTabletLayout(activity) ? TagSettingsActivityTablet.class : TagSettingsActivity.class;
            Intent intent = new Intent(getActivity(), settingsClass);
            intent.putExtra(EXTRA_TAG_DATA, tagData);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);

            if (!AstridPreferences.useTabletLayout(activity)) {
                AndroidUtilities.callOverridePendingTransition(activity, R.anim.slide_left_in, R.anim.slide_left_out);
            }
        }
    };

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.TaskListActivity#getListBody(android.view.ViewGroup)
     */
    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(getTaskListBodyLayout(), root, false);

        taskListView = super.getListBody(parent);
        parent.addView(taskListView);

        return parent;
    }

    protected int getTaskListBodyLayout() {
        return R.layout.task_list_body_tag;
    }

    private void showListSettingsPopover() {
        if (!AstridPreferences.canShowPopover())
            return;
        if (!Preferences.getBoolean(R.string.p_showed_list_settings_help, false)) {
            Preferences.setBoolean(R.string.p_showed_list_settings_help, true);
            View tabView = getView().findViewById(R.id.members_edit);
            if (tabView != null)
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

    @Override
    protected void addMenuItems(Menu menu, Activity activity) {
        super.addMenuItems(menu, activity);
        if (!Preferences.getBoolean(R.string.p_show_list_members, true)) {
            MenuItem item = menu.add(Menu.NONE, MENU_LIST_SETTINGS_ID, 0, R.string.tag_settings_title);
            item.setIcon(ThemeService.getDrawable(R.drawable.list_settings));
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    // --- data loading

    @Override
    protected void initializeData() {
        synchronized(this) {
            if(dataLoaded)
                return;
            dataLoaded = true;
        }

        TaskListActivity activity = (TaskListActivity) getActivity();
        String tag = extras.getString(EXTRA_TAG_NAME);
        String uuid = RemoteModel.NO_UUID;
        if (extras.containsKey(EXTRA_TAG_UUID))
            uuid = extras.getString(EXTRA_TAG_UUID);
        else if (extras.containsKey(EXTRA_TAG_REMOTE_ID)) // For legacy support with shortcuts, widgets, etc.
            uuid = Long.toString(extras.getLong(EXTRA_TAG_REMOTE_ID));


        if(tag == null && RemoteModel.NO_UUID.equals(uuid))
            return;

        TodorooCursor<TagData> cursor;
        if (!RemoteModel.isUuidEmpty(uuid)) {
            cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(TagData.UUID.eq(uuid)));
        } else {
            cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(TagData.NAME.eqCaseInsensitive(tag)));
        }

        try {
            tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, tag);
                tagData.setValue(TagData.UUID, uuid);
                tagDataService.save(tagData);
            } else {
                cursor.moveToFirst();
                tagData.readFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        super.initializeData();

        setUpMembersGallery();

        if (extras.getBoolean(TOKEN_START_ACTIVITY, false)) {
            extras.remove(TOKEN_START_ACTIVITY);
            activity.showComments();
        }
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
            long lastViewedComments = Preferences.getLong(CommentsFragment.UPDATES_LAST_VIEWED + tagData.getValue(TagData.UUID), 0);
            int unreadCount = 0;
            TodorooCursor<UserActivity> commentCursor = tagDataService.getUserActivityWithExtraCriteria(tagData, UserActivity.CREATED_AT.gt(lastViewedComments));
            try {
                unreadCount = commentCursor.getCount();
            } finally {
                commentCursor.close();
            }

            TaskListActivity tla = (TaskListActivity) getActivity();
            if (tla != null)
                tla.setCommentsCount(unreadCount);
        }
    }

    // --------------------------------------------------------- refresh data


    @Override
    protected void initiateAutomaticSyncImpl() {
        if (!isCurrentTaskListFragment())
            return;
        if (tagData != null) {
            long lastAutosync = tagData.getValue(TagData.LAST_AUTOSYNC);
            if(DateUtilities.now() - lastAutosync > AUTOSYNC_INTERVAL) {
                tagData.setValue(TagData.LAST_AUTOSYNC, DateUtilities.now());
                tagDataDao.saveExisting(tagData);
                refreshData();
            }
        }
    }

    /** refresh the list with latest data from the web */
    private void refreshData() {
        if (actFmPreferenceService.isLoggedIn() && tagData != null && !RemoteModel.isUuidEmpty(tagData.getUuid())) {
            ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.DLG_loading);

            SyncMessageCallback callback = new SyncMessageCallback() {
                @Override
                public void runOnSuccess() {
                    synchronized(this) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        reloadTagData(false);
                                        refresh();
                                        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
                                    } catch (Exception e) {
                                        // Can happen when swipe between lists is on
                                    }
                                }
                            });
                        }
                    }
                }
                @Override
                public void runOnErrors(List<JSONArray> errors) {
                    Activity activity = getActivity();
                    if (activity != null && activity instanceof TaskListActivity) {
                        boolean notAuthorized = false;
                        for (JSONArray error : errors) {
                            String errorCode = error.optString(1);
                            if ("not_authorized".equals(errorCode)) { //$NON-NLS-1$
                                notAuthorized = true;
                                break;
                            }
                        }

                        final String tagName = tagData.getValue(TagData.NAME);
                        if (notAuthorized) {
                            final TaskListActivity tla = (TaskListActivity) activity;
                            tla.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DialogUtilities.okCancelCustomDialog(tla,
                                            tla.getString(R.string.actfm_tag_not_authorized_title),
                                            tla.getString(R.string.actfm_tag_not_authorized_body, tagName),
                                            R.string.actfm_tag_not_authorized_new_list,
                                            R.string.actfm_tag_not_authorized_leave_list,
                                            android.R.drawable.ic_dialog_alert,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String oldUuid = tagData.getUuid();
                                                    tagData.setValue(TagData.DELETION_DATE, DateUtilities.now());
                                                    tagData.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                                                    tagDataDao.saveExisting(tagData);

                                                    // TODO: Make this better
                                                    tagData.clearValue(TagData.ID);
                                                    tagData.clearValue(TagData.UUID);
                                                    tagData.clearValue(TagData.USER_ID);
                                                    tagData.clearValue(TagData.DELETION_DATE);
                                                    tagData.setValue(TagData.CREATION_DATE, DateUtilities.now());
                                                    tagDataDao.createNew(tagData);
                                                    String newUuid = tagData.getUuid();

                                                    TodorooCursor<Task> tasks = taskService.fetchFiltered(filter.getSqlQuery(), null, Task.ID, Task.UUID, Task.USER_ID);
                                                    try {
                                                        Task t = new Task();
                                                        for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                                                            t.clear();
                                                            t.readFromCursor(tasks);
                                                            if (Task.USER_ID_SELF.equals(t.getValue(Task.USER_ID))) {
                                                                tagService.createLink(t, tagName, newUuid);
                                                            }
                                                        }
                                                    } finally {
                                                        tasks.close();
                                                    }
                                                    tagService.deleteTagMetadata(oldUuid);

                                                    Filter newFilter = TagFilterExposer.filterFromTagData(tla, tagData);
                                                    tla.onFilterItemClicked(newFilter);
                                                }
                                            },
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String uuid = tagData.getUuid();
                                                    tagDataDao.delete(tagData.getId());
                                                    metadataDao.deleteWhere(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)));
                                                    tagMetadataDao.deleteWhere(TagMetadata.TAG_UUID.eq(uuid));
                                                    tla.switchToActiveTasks();
                                                }
                                            });
                                }
                            });
                        }

                    }
                }
            };

            ActFmSyncThread.getInstance().repopulateQueueFromOutstandingTables();
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<Task>(Task.class, null, tagData.getValue(TagData.TASKS_PUSHED_AT), BriefMe.TAG_ID_KEY, tagData.getUuid()), callback);
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<TagData>(TagData.class, tagData.getUuid(), tagData.getValue(TagData.PUSHED_AT)), callback);
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<TaskListMetadata>(TaskListMetadata.class, null, tagData.getValue(TagData.METADATA_PUSHED_AT), BriefMe.TAG_ID_KEY, tagData.getUuid()), callback);
        }
    }

    protected void setUpMembersGallery() {
        if (!Preferences.getBoolean(R.string.p_show_list_members, true)) {
            getView().findViewById(R.id.members_header).setVisibility(View.GONE);
            return;
        }
        if (tagData == null)
            return;
        LinearLayout membersView = (LinearLayout)getView().findViewById(R.id.shared_with);
        membersView.setOnClickListener(settingsListener);
        boolean addedMembers = false;
        try {
            String membersString = tagData.getValue(TagData.MEMBERS); // OK for legacy compatibility
            if (!TextUtils.isEmpty(membersString)) {
                JSONArray members = new JSONArray(membersString);
                if (members.length() > 0) {
                    addedMembers = true;
                    membersView.setOnClickListener(null);
                    membersView.removeAllViews();
                    for (int i = 0; i < members.length(); i++) {
                        JSONObject member = members.getJSONObject(i);
                        addImageForMember(membersView, member);
                    }
                }
            } else {
                TodorooCursor<User> users = userDao.query(Query.select(User.PROPERTIES)
                        .where(User.UUID.in(Query.select(TagMemberMetadata.USER_UUID)
                                .from(TagMetadata.TABLE)
                                .where(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagData.getUuid(), TagMemberMetadata.KEY), TagMetadata.DELETION_DATE.eq(0))))));
                try {
                    addedMembers = users.getCount() > 0;
                    if (addedMembers) {
                        membersView.setOnClickListener(null);
                        membersView.removeAllViews();
                    }
                    User user = new User();
                    for (users.moveToFirst(); !users.isAfterLast(); users.moveToNext()) {
                        user.clear();
                        user.readFromCursor(users);
                        JSONObject member = new JSONObject();
                        ActFmSyncService.JsonHelper.jsonFromUser(member, user);
                        addImageForMember(membersView, member);
                    }
                } finally {
                    users.close();
                }

                TodorooCursor<TagMetadata> byEmail = tagMetadataDao.query(Query.select(TagMemberMetadata.USER_UUID)
                        .where(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagData.getUuid(), TagMemberMetadata.KEY),
                                TagMemberMetadata.USER_UUID.like("%@%"), TagMetadata.DELETION_DATE.eq(0)))); //$NON-NLS-1$
                try {
                    if (!addedMembers && byEmail.getCount() > 0) {
                        membersView.setOnClickListener(null);
                        membersView.removeAllViews();
                    }
                    addedMembers = addedMembers || byEmail.getCount() > 0;
                    TagMetadata tm = new TagMetadata();
                    for (byEmail.moveToFirst(); !byEmail.isAfterLast(); byEmail.moveToNext()) {
                        tm.clear();
                        tm.readFromCursor(byEmail);
                        String email = tm.getValue(TagMemberMetadata.USER_UUID);
                        if (!TextUtils.isEmpty(email)) {
                            JSONObject member = new JSONObject();
                            member.put("email", email); //$NON-NLS-1$
                            addImageForMember(membersView, member);
                        }
                    }
                } finally {
                    byEmail.close();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (addedMembers) {
            try {
            // Handle creator
                JSONObject owner;
                if(!Task.USER_ID_SELF.equals(tagData.getValue(TagData.USER_ID))) {
                    String userString = tagData.getValue(TagData.USER);
                    if (!TextUtils.isEmpty(userString)) {
                        owner = new JSONObject(tagData.getValue(TagData.USER));
                    } else {
                        User user = userDao.fetch(tagData.getValue(TagData.USER_ID), User.PROPERTIES);
                        if (user != null) {
                            owner = new JSONObject();
                            ActFmSyncService.JsonHelper.jsonFromUser(owner, user);
                        } else {
                            owner = null;
                        }
                    }
                } else {
                    owner = ActFmPreferenceService.thisUser();
                }
                if (owner != null)
                    addImageForMember(membersView, owner);

                JSONObject unassigned = new JSONObject();
                unassigned.put("id", Task.USER_ID_UNASSIGNED); //$NON-NLS-1$
                unassigned.put("name", getActivity().getString(R.string.actfm_EPA_unassigned)); //$NON-NLS-1$
                addImageForMember(membersView, unassigned);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            membersView.removeAllViews();
            membersView.setOnClickListener(settingsListener);
            TextView textView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.no_members_text_view, null);
            membersView.addView(textView);
        }

        View filterAssigned = getView().findViewById(R.id.filter_assigned);
        if (filterAssigned != null)
            filterAssigned.setOnClickListener(new OnClickListener() {
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
        image.setLayoutParams(new LinearLayout.LayoutParams((int)(43 * displayMetrics.density),
                (int)(43 * displayMetrics.density)));


        image.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_default_person_image));
        if (Task.USER_ID_UNASSIGNED.equals(Long.toString(member.optLong("id", 0))))
            image.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_anyone));

        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            final String id = Long.toString(member.optLong("id", -2));
            if (ActFmPreferenceService.userId().equals(id))
                member = ActFmPreferenceService.thisUser();
            final JSONObject memberToUse = member;

            final String memberName = displayName(memberToUse);
            if (memberToUse.has("picture") && !TextUtils.isEmpty(memberToUse.getString("picture"))) {
                image.setUrl(memberToUse.getString("picture"));
            }
            image.setOnClickListener(listenerForImage(memberToUse, id, memberName));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int padding = (int) (3 * displayMetrics.density);
        image.setPadding(padding, padding, padding, padding);
        membersView.addView(image);
    }

    private OnClickListener listenerForImage(final JSONObject member, final String id, final String displayName) {
        return new OnClickListener() {
            final String email = member.optString("email"); //$NON-NLS-1$
            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View v) {
                if (currentId.equals(id)) {
                    // Back to all
                    resetAssignedFilter();
                } else {
                    // New filter
                    currentId = id;
                    Criterion assignedCriterion;
                    if (ActFmPreferenceService.userId().equals(currentId))
                        assignedCriterion = Criterion.or(Task.USER_ID.eq(0), Task.USER_ID.eq(id));
                    else if (Task.userIdIsEmail(currentId) && !TextUtils.isEmpty(email))
                        assignedCriterion = Criterion.or(Task.USER_ID.eq(email), Task.USER.like("%" + email + "%")); //$NON-NLS-1$ //$NON-NLS-2$ // Deprecated field OK for backwards compatibility
                    else
                        assignedCriterion = Task.USER_ID.eq(id);
                    Criterion assigned = Criterion.and(TaskCriteria.activeAndVisible(), assignedCriterion);
                    filter = TagFilterExposer.filterFromTag(getActivity(), new Tag(tagData), assigned);
                    TextView filterByAssigned = (TextView) getView().findViewById(R.id.filter_assigned);
                    if (filterByAssigned != null) {
                        filterByAssigned.setVisibility(View.VISIBLE);
                        if (id == Task.USER_ID_UNASSIGNED)
                            filterByAssigned.setText(getString(R.string.actfm_TVA_filter_by_unassigned));
                        else
                            filterByAssigned.setText(getString(R.string.actfm_TVA_filtered_by_assign, displayName));
                    }
                    isBeingFiltered.set(true);
                    setUpTaskList();
                }
            }
        };
    }

    private void resetAssignedFilter() {
        currentId = Task.USER_ID_IGNORE;
        isBeingFiltered.set(false);
        filter = originalFilter;
        View filterAssigned = getView().findViewById(R.id.filter_assigned);
        if (filterAssigned != null)
            filterAssigned.setVisibility(View.GONE);
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
            if(tagData == null || !tagData.getValue(TagData.UUID).toString().equals(intent.getStringExtra("tag_id")))
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //refreshUpdatesList();
                }
            });
            refreshData();

            NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
            try {
                nm.cancel(Integer.parseInt(tagData.getValue(TagData.UUID)));
            } catch (NumberFormatException e) {
                // Eh
            }
        }
    };

    @Override
    public void onResume() {
        if (justDeleted) {
            parentOnResume();
            // tag was deleted locally in settings
            // go back to active tasks
            AstridActivity activity = ((AstridActivity) getActivity());
            FilterListFragment fl = activity.getFilterListFragment();
            if (fl != null) {
                fl.clear(); // Should auto refresh
                activity.switchToActiveTasks();
            }
            return;
        }
        super.onResume();


        IntentFilter intentFilter = new IntentFilter(BROADCAST_TAG_ACTIVITY);
        getActivity().registerReceiver(notifyReceiver, intentFilter);

        showListSettingsPopover();
        updateCommentCount();
    }

    @Override
    public void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(getActivity(), notifyReceiver);
    }

    protected void reloadTagData(boolean onActivityResult) {
        tagData = tagDataService.fetchById(tagData.getId(), TagData.PROPERTIES); // refetch
        if (tagData == null) {
            // This can happen if a tag has been deleted as part of a sync
            taskListMetadata = null;
            return;
        } else if (tagData.isDeleted()) {
            justDeleted = true;
            return;
        }
        initializeTaskListMetadata();
        filter = TagFilterExposer.filterFromTagData(getActivity(), tagData);
        getActivity().getIntent().putExtra(TOKEN_FILTER, filter);
        extras.putParcelable(TOKEN_FILTER, filter);
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity) {
            ((TaskListActivity) activity).setListsTitle(filter.title);
            FilterListFragment flf = ((TaskListActivity) activity).getFilterListFragment();
            if (flf != null) {
                if (!onActivityResult)
                    flf.refresh();
                else
                    flf.clear();
            }
        }
        taskAdapter = null;
        Flags.set(Flags.REFRESH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == Activity.RESULT_OK) {
            reloadTagData(true);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean handleOptionsMenuItemSelected(int id, Intent intent) {
        // handle my own menus
        switch (id) {
        case MENU_REFRESH_ID:
            refreshData();
            return true;
        case MENU_LIST_SETTINGS_ID:
            settingsListener.onClick(null);
            return true;
        }

        return super.handleOptionsMenuItemSelected(id, intent);
    }

    @Override
    protected boolean hasDraggableOption() {
        return tagData != null && !tagData.getFlag(TagData.FLAGS, TagData.FLAG_FEATURED);
    }

    @Override
    protected void toggleDragDrop(boolean newState) {
        Class<?> customComponent;

        if(newState)
            customComponent = SubtasksTagListFragment.class;
        else {
            filter.setFilterQueryOverride(null);
            customComponent = TagViewFragment.class;
        }

        ((FilterWithCustomIntent) filter).customTaskList = new ComponentName(getActivity(), customComponent);

        extras.putParcelable(TOKEN_FILTER, filter);
        ((AstridActivity)getActivity()).setupTasklistFragmentWithFilterAndCustomTaskList(filter,
                extras, customComponent);
    }

    @Override
    protected void refresh() {
        setUpMembersGallery();
        loadTaskListContent(true);
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
    }

}
